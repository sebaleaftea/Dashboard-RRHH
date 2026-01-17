#!/usr/bin/env python3
"""
ETL script to fetch contracts from Talana and upsert into Cloud SQL Postgres.

Usage (Cloud Run): set the following environment variables (recommended):
  - TALANA_URL: full Talana API URL that returns contracts (supports pagination)
  - INSTANCE_CONNECTION_NAME: GCP Cloud SQL instance connection name (PROJECT:REGION:INSTANCE)
  - DB_NAME: database name (default: grh)
  - SECRET_DB_USER: Secret Manager secret id for DB user (optional)
  - SECRET_DB_PASS: Secret Manager secret id for DB password (optional)
  - SECRET_TALANA_TOKEN: Secret Manager secret id for Talana token (optional)

Or provide DB_USER, DB_PASS, TALANA_TOKEN as plain env vars (less secure).

The script will attempt to use the Cloud SQL Python Connector when
`INSTANCE_CONNECTION_NAME` is provided. If not provided and `DB_HOST` is
present, it will connect directly to the host:port.

You must grant the Cloud Run service account `secretmanager.secretAccessor`
and `cloudsql.client` roles.

IMPORTANT: Adapt the JSON parsing in `process_contract` to match your
Talana response fields. The script includes a conservative mapping that
matches the application's schema (`V1__init.sql`).
"""

import os
import json
import time
import logging
from datetime import datetime
from typing import Any, Dict, Iterable, Optional

import requests

from google.cloud import secretmanager

try:
    # cloud-sql-python-connector and pg8000 are preferred in Cloud Run
    from google.cloud.sql.connector import Connector
    import pg8000
    HAVE_CONNECTOR = True
except Exception:
    Connector = None
    pg8000 = None
    HAVE_CONNECTOR = False

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
logger = logging.getLogger(__name__)


def access_secret(secret_name: str, project_id: Optional[str] = None) -> str:
    """Read the latest value of a secret from Secret Manager.
    `secret_name` may be the resource name or the short id. If only id is
    provided, `project_id` must also be set or the environment project used.
    """
    client = secretmanager.SecretManagerServiceClient()
    if ":" in secret_name or "/" in secret_name:
        name = secret_name
    else:
        project = project_id or os.environ.get("GOOGLE_CLOUD_PROJECT")
        if not project:
            raise RuntimeError("Project id not found in env; provide full secret resource name or set GOOGLE_CLOUD_PROJECT")
        name = f"projects/{project}/secrets/{secret_name}/versions/latest"

    response = client.access_secret_version(request={"name": name})
    payload = response.payload.data.decode("UTF-8")
    return payload


def get_db_credentials(project_id: Optional[str] = None) -> Dict[str, str]:
    # Prefer secrets; fallback to env vars
    secret_db_user = os.environ.get("SECRET_DB_USER")
    secret_db_pass = os.environ.get("SECRET_DB_PASS")
    creds = {}
    if secret_db_user:
        creds["user"] = access_secret(secret_db_user, project_id)
    else:
        creds["user"] = os.environ.get("DB_USER")

    if secret_db_pass:
        creds["password"] = access_secret(secret_db_pass, project_id)
    else:
        creds["password"] = os.environ.get("DB_PASS")

    creds["dbname"] = os.environ.get("DB_NAME", "grh")
    return creds


def get_talana_token(project_id: Optional[str] = None) -> str:
    secret_tal = os.environ.get("SECRET_TALANA_TOKEN")
    if secret_tal:
        return access_secret(secret_tal, project_id)
    token = os.environ.get("TALANA_TOKEN")
    if not token:
        raise RuntimeError("Talana token not found. Set SECRET_TALANA_TOKEN or TALANA_TOKEN env var")
    return token


def fetch_contracts(talana_url: str, token: str) -> Iterable[Dict[str, Any]]:
    """Fetch contracts from Talana. Supports simple pagination if the API
    returns `next` URL or uses `page` parameter. Adjust as needed for your API.
    """
    headers = {"Authorization": f"Token {token}", "Accept": "application/json"}
    url = talana_url
    page = 1
    per_page = int(os.environ.get("TALANA_PER_PAGE", "100"))

    while True:
        params = {"page": page, "per_page": per_page}
        logger.info("Requesting Talana page %s (url=%s)", page, url)
        resp = requests.get(url, headers=headers, params=params, timeout=60)
        resp.raise_for_status()
        data = resp.json()

        # Extract list of contracts - try common keys
        if isinstance(data, dict):
            if "data" in data and isinstance(data["data"], list):
                items = data["data"]
            elif "contracts" in data:
                items = data["contracts"]
            elif "items" in data:
                items = data["items"]
            else:
                # if the response itself is a list-like structure
                items = data.get("results") if isinstance(data.get("results"), list) else []
        elif isinstance(data, list):
            items = data
        else:
            items = []

        if not items:
            logger.info("No items found on page %s, stopping", page)
            break

        # Log first record structure for debugging
        if page == 1 and items:
            logger.info("Sample contract record structure: %s", json.dumps(items[0], indent=2, default=str))

        for it in items:
            yield it

        # Pagination heuristic: stop if fewer than per_page
        if len(items) < per_page:
            break
        page += 1


def process_contract(rec: Dict[str, Any]) -> Dict[str, Any]:
    """Map Talana contract JSON to DB fields based on actual Talana API structure.
    Talana returns: 
    - rec["empleado"]: empleado ID
    - rec["empleadoDetails"]: {nombre, rut, apellidoPaterno, apellidoMaterno}
    - rec["centroCosto"]: {id, codigo, nombre}
    - rec["sucursal"]: {id, nombre}
    - rec["jefe"]: {nombre} or string
    - rec["cargo"]: string
    - rec["fechaContratacion"] or rec["desde"]: date
    Returns a dict with keys for empleado, centro_costo, sucursal, contrato.
    """
    # Extract empleado info from empleadoDetails nested object
    empleado_id = int(rec.get("empleado") or 0)
    empleado_details = rec.get("empleadoDetails") or {}
    
    empleado = {
        "id": empleado_id,
        "rut": empleado_details.get("rut"),
        "nombre": empleado_details.get("nombre"),
        "ap_paterno": empleado_details.get("apellidoPaterno"),
        "ap_materno": empleado_details.get("apellidoMaterno"),
        "sexo": empleado_details.get("sexo"),  # May not be present
        "fecha_nac": empleado_details.get("fechaNacimiento"),  # May not be present
        "discapacidad": empleado_details.get("discapacidad", False),
    }

    # Extract centro_costo from nested object
    centro_costo_obj = rec.get("centroCosto") or {}
    centro = {
        "id": int(centro_costo_obj.get("id") or 0) if centro_costo_obj.get("id") else None,
        "codigo": centro_costo_obj.get("codigo"),
        "nombre": centro_costo_obj.get("nombre"),
    }

    # Extract sucursal from nested object
    sucursal_obj = rec.get("sucursal") or {}
    sucursal = {
        "id": int(sucursal_obj.get("id") or 0) if sucursal_obj.get("id") else None,
        "nombre": sucursal_obj.get("nombre"),
    }

    # Extract jefe - can be object with nombre or string
    jefe_obj = rec.get("jefe")
    jefe_nombre = None
    if jefe_obj:
        if isinstance(jefe_obj, dict):
            jefe_nombre = jefe_obj.get("nombre")
        else:
            jefe_nombre = str(jefe_obj)

    # Extract contract dates
    fecha_contratacion = rec.get("fechaContratacion") or rec.get("desde")
    
    contrato = {
        "id": int(rec.get("id") or 0),
        "empleado_id": empleado_id,
        "cargo": rec.get("cargo"),
        "cargo_norm": None,  # Will be normalized later if needed
        "centro_costo_id": centro.get("id"),
        "sucursal_id": sucursal.get("id"),
        "jefe_nombre": jefe_nombre,
        "fecha_contratacion": fecha_contratacion,
        "vigente": bool(rec.get("activo", False)),  # Talana uses "activo" field
        "desde": rec.get("desde"),
        "hasta": rec.get("hasta"),
    }

    return {"empleado": empleado, "centro": centro, "sucursal": sucursal, "contrato": contrato}


def upsert_many(conn, items: Iterable[Dict[str, Any]]):
    cur = conn.cursor()
    counts = {"empleado": 0, "centro": 0, "sucursal": 0, "contrato": 0}

    for rec in items:
        mapped = process_contract(rec)

        emp = mapped["empleado"]
        if emp["id"] and emp["rut"]:
            cur.execute(
                """
                INSERT INTO empleado (id, rut, nombre, ap_paterno, ap_materno, sexo, fecha_nac, discapacidad)
                VALUES (%s,%s,%s,%s,%s,%s,%s,%s)
                ON CONFLICT (id) DO UPDATE SET
                  rut=EXCLUDED.rut, nombre=EXCLUDED.nombre, ap_paterno=EXCLUDED.ap_paterno,
                  ap_materno=EXCLUDED.ap_materno, sexo=EXCLUDED.sexo, fecha_nac=EXCLUDED.fecha_nac,
                  discapacidad=EXCLUDED.discapacidad
                """,
                (
                    emp["id"], emp["rut"], emp.get("nombre"), emp.get("ap_paterno"), emp.get("ap_materno"),
                    emp.get("sexo"), emp.get("fecha_nac"), emp.get("discapacidad"),
                ),
            )
            counts["empleado"] += 1

        centro = mapped["centro"]
        if centro.get("id"):
            cur.execute(
                """
                INSERT INTO centro_costo (id, codigo, nombre)
                VALUES (%s,%s,%s)
                ON CONFLICT (id) DO UPDATE SET codigo=EXCLUDED.codigo, nombre=EXCLUDED.nombre
                """,
                (centro["id"], centro.get("codigo"), centro.get("nombre")),
            )
            counts["centro"] += 1

        suc = mapped["sucursal"]
        if suc.get("id"):
            cur.execute(
                """
                INSERT INTO sucursal (id, nombre)
                VALUES (%s,%s)
                ON CONFLICT (id) DO UPDATE SET nombre=EXCLUDED.nombre
                """,
                (suc["id"], suc.get("nombre")),
            )
            counts["sucursal"] += 1

        con = mapped["contrato"]
        if con.get("id"):
            cur.execute(
                """
                INSERT INTO contrato (id, empleado_id, cargo, cargo_norm, centro_costo_id, sucursal_id, jefe_nombre, fecha_contratacion, vigente, desde, hasta)
                VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)
                ON CONFLICT (id) DO UPDATE SET
                  empleado_id=EXCLUDED.empleado_id,
                  cargo=EXCLUDED.cargo,
                  cargo_norm=EXCLUDED.cargo_norm,
                  centro_costo_id=EXCLUDED.centro_costo_id,
                  sucursal_id=EXCLUDED.sucursal_id,
                  jefe_nombre=EXCLUDED.jefe_nombre,
                  fecha_contratacion=EXCLUDED.fecha_contratacion,
                  vigente=EXCLUDED.vigente,
                  desde=EXCLUDED.desde,
                  hasta=EXCLUDED.hasta
                """,
                (
                    con["id"], con.get("empleado_id"), con.get("cargo"), con.get("cargo_norm"),
                    con.get("centro_costo_id"), con.get("sucursal_id"), con.get("jefe_nombre"),
                    con.get("fecha_contratacion"), con.get("vigente"), con.get("desde"), con.get("hasta"),
                ),
            )
            counts["contrato"] += 1

    conn.commit()
    cur.close()
    return counts


def fetch_vacaciones(talana_base_url: str, token: str) -> Iterable[Dict[str, Any]]:
    """Fetch vacaciones using Talana resumed endpoint.
    
    API endpoint: /es/api/vacations-resumed
    Uses date range filters from yesterday to today.
    Handles both paginated (dict with results/next) and list responses.
    """
    from datetime import datetime, timedelta
    
    headers = {"Authorization": f"Token {token}"}
    # Normalize to Talana API base: https://talana.com/es/api
    host = talana_base_url.split('/es/api')[0]
    base_api = f"{host}/es/api"
    url = f"{base_api}/vacations-resumed"
    
    # Date range: from yesterday to today
    today = datetime.now().strftime('%Y-%m-%d')
    yesterday = (datetime.now() - timedelta(days=1)).strftime('%Y-%m-%d')
    logger.info("Filtering vacaciones from %s to %s", yesterday, today)
    
    params = {
        "absent_since": yesterday,
        "absent_to": today,
    }
    
    # First request
    next_url = url
    while next_url:
        logger.info("Fetching vacaciones from: %s", next_url)
        resp = requests.get(next_url, headers=headers, params=params if next_url == url else None, timeout=30)
        resp.raise_for_status()
        data = resp.json()
        
        if isinstance(data, dict) and "results" in data:
            results = data.get("results", [])
            logger.info("Page returned %d vacaciones", len(results))
            for item in results:
                yield item
            next_url = data.get("next")
            params = None
        elif isinstance(data, list):
            logger.info("Returned %d vacaciones (non-paginated)", len(data))
            for item in data:
                yield item
            break
        else:
            # Single object
            logger.info("Returned a single vacaciones record")
            yield data
            break


def fetch_licencias(talana_base_url: str, token: str) -> Iterable[Dict[str, Any]]:
    """Fetch all licencias (absences/leaves) from Talana API using personaAusencia endpoint.
    
    API endpoint: /es/api/personaAusencia-paginado/
    Returns paginated absence records including medical leaves, permissions, etc.
    Only fetches records from yesterday onwards (last 24+ hours).
    """
    from datetime import datetime, timedelta
    
    headers = {"Authorization": f"Token {token}"}
    # Extract base URL (e.g., https://talana.com from https://talana.com/es/api/contrato-paginado/)
    base = talana_base_url.split('/es/api/')[0]
    # Use personaAusencia-paginado endpoint for absences/licenses
    url = f"{base}/es/api/personaAusencia-paginado/"
    
    # Calculate yesterday's date (1 day ago)
    yesterday = (datetime.now() - timedelta(days=1)).strftime('%Y-%m-%d')
    logger.info("Filtering licencias from %s onwards", yesterday)
    
    # Filter: absences with start date since yesterday
    params = {
        "page": 1, 
        "page_size": 100,
        "absent_since": yesterday  # Only records with absence start date since yesterday
    }
    
    while url:
        logger.info("Fetching licencias from: %s (page %d)", url, params.get("page", 1))
        resp = requests.get(url, headers=headers, params=params, timeout=30)
        resp.raise_for_status()
        data = resp.json()
        
        results = data.get("results", [])
        logger.info("Page returned %d licencias", len(results))
        
        for item in results:
            yield item
        
        # Check for next page
        url = data.get("next")
        if url:
            params = {}  # Next URL already contains parameters


def process_vacacion(rec: Dict[str, Any]) -> Dict[str, Any]:
    """Map Talana vacaciones JSON to DB fields.
    Talana vacacionesSolicitud structure:
    - rec["id"]: vacacion ID
    - rec["empleado"]["id"]: empleado ID or direct ID
    - rec["vacacionesDesde"]: start date
    - rec["vacacionesHasta"]: end date
    - rec["vacacionesRetorno"]: return date
    - rec["numeroDias"]: number of days
    - rec["mediosDias"]: half days (boolean)
    - rec["fechaAprobacion"]: approval timestamp
    - rec["tipoVacaciones"]: vacation type
    """
    empleado_obj = rec.get("empleado") or {}
    empleado_id = empleado_obj.get("id") if isinstance(empleado_obj, dict) else rec.get("empleado")
    
    return {
        "id": int(rec.get("id") or 0),
        "empleado_id": int(empleado_id or 0) if empleado_id else None,
        "desde": rec.get("vacacionesDesde"),
        "hasta": rec.get("vacacionesHasta"),
        "retorno": rec.get("vacacionesRetorno"),
        "dias": rec.get("numeroDias"),
        "medios_dias": 1 if rec.get("mediosDias") else 0,
        "fecha_aprobacion": rec.get("fechaAprobacion"),
        "tipo": rec.get("tipoVacaciones"),
    }


def process_licencia(rec: Dict[str, Any]) -> Dict[str, Any]:
    """Map Talana personaAusencia JSON to licencias DB fields.
    Talana personaAusencia structure:
    - rec["id"]: ausencia ID
    - rec["empleado"]["id"]: empleado ID or direct ID
    - rec["ausenciaDesde"]: start date
    - rec["ausenciaHasta"]: end date  
    - rec["numeroDias"]: number of days
    - rec["tipoAusencia"]: absence type (licencia médica, permiso, etc)
    - rec["fechaCreacion"]: creation/request date
    """
    empleado_obj = rec.get("empleado") or {}
    empleado_id = empleado_obj.get("id") if isinstance(empleado_obj, dict) else rec.get("empleado")
    
    return {
        "id": int(rec.get("id") or 0),
        "empleado_id": int(empleado_id or 0) if empleado_id else None,
        "desde": rec.get("ausenciaDesde"),
        "hasta": rec.get("ausenciaHasta"),
        "dias": rec.get("numeroDias"),
        "tipo": rec.get("tipoAusencia"),
        "fecha_solicitud": rec.get("fechaCreacion"),
    }


def ensure_empleado_exists(conn, empleado_id: int, empleado_obj: Dict[str, Any]) -> None:
    """Ensure an empleado exists before inserting FK rows.
    Inserts a minimal empleado record if missing, using available fields."""
    if not empleado_id:
        return

    cur = conn.cursor()
    cur.execute("SELECT 1 FROM empleado WHERE id = %s", (empleado_id,))
    exists = cur.fetchone()
    if exists:
        cur.close()
        return

    # Extract minimal fields from nested empleado object
    rut = None
    nombre = None
    ap_paterno = None
    ap_materno = None
    sexo = None
    fecha_nac = None
    discapacidad = None

    if isinstance(empleado_obj, dict):
        rut = empleado_obj.get("rut")
        nombre = empleado_obj.get("nombre")
        ap_paterno = empleado_obj.get("apellidoPaterno")
        ap_materno = empleado_obj.get("apellidoMaterno")
        sexo = empleado_obj.get("sexo")
        fecha_nac = empleado_obj.get("fechaNacimiento")
        discapacidad = empleado_obj.get("discapacidad")

    logger.warning("Empleado %s missing; inserting minimal record", empleado_id)
    cur.execute(
        """
        INSERT INTO empleado (id, rut, nombre, ap_paterno, ap_materno, sexo, fecha_nac, discapacidad)
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
        ON CONFLICT (id) DO NOTHING
        """,
        (empleado_id, rut, nombre, ap_paterno, ap_materno, sexo, fecha_nac, discapacidad),
    )
    cur.close()


def upsert_vacaciones(conn, items: Iterable[Dict[str, Any]]) -> int:
    """Insert or update vacaciones records."""
    cur = conn.cursor()
    count = 0
    
    for rec in items:
        vac = process_vacacion(rec)
        if vac["id"] and vac["empleado_id"]:
            # Ensure empleado exists to satisfy FK
            ensure_empleado_exists(conn, vac["empleado_id"], rec.get("empleado"))
            cur.execute(
                """
                INSERT INTO vacaciones (id, empleado_id, desde, hasta, retorno, dias, medios_dias, fecha_aprobacion, tipo)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
                ON CONFLICT (id) DO UPDATE SET
                  empleado_id=EXCLUDED.empleado_id,
                  desde=EXCLUDED.desde,
                  hasta=EXCLUDED.hasta,
                  retorno=EXCLUDED.retorno,
                  dias=EXCLUDED.dias,
                  medios_dias=EXCLUDED.medios_dias,
                  fecha_aprobacion=EXCLUDED.fecha_aprobacion,
                  tipo=EXCLUDED.tipo
                """,
                (vac["id"], vac["empleado_id"], vac["desde"], vac["hasta"], vac["retorno"],
                 vac["dias"], vac["medios_dias"], vac["fecha_aprobacion"], vac["tipo"]),
            )
            count += 1
    
    conn.commit()
    cur.close()
    return count


def upsert_licencias(conn, items: Iterable[Dict[str, Any]]) -> int:
    """Insert or update licencias records."""
    cur = conn.cursor()
    count = 0
    
    for rec in items:
        lic = process_licencia(rec)
        if lic["id"] and lic["empleado_id"]:
            # Ensure empleado exists to satisfy FK
            ensure_empleado_exists(conn, lic["empleado_id"], rec.get("empleado"))
            cur.execute(
                """
                INSERT INTO licencias (id, empleado_id, desde, hasta, dias, tipo, fecha_solicitud)
                VALUES (%s, %s, %s, %s, %s, %s, %s)
                ON CONFLICT (id) DO UPDATE SET
                  empleado_id=EXCLUDED.empleado_id,
                  desde=EXCLUDED.desde,
                  hasta=EXCLUDED.hasta,
                  dias=EXCLUDED.dias,
                  tipo=EXCLUDED.tipo,
                  fecha_solicitud=EXCLUDED.fecha_solicitud
                """,
                (lic["id"], lic["empleado_id"], lic["desde"], lic["hasta"],
                 lic["dias"], lic["tipo"], lic["fecha_solicitud"]),
            )
            count += 1
    
    conn.commit()
    cur.close()
    return count


def run():
    project = os.environ.get("GOOGLE_CLOUD_PROJECT")
    talana_url = os.environ.get("TALANA_URL")
    if not talana_url:
        raise RuntimeError("TALANA_URL env var must be set to Talana API endpoint")

    logger.info("Getting credentials and token")
    creds = get_db_credentials(project)
    talana_token = get_talana_token(project)

    instance_conn = os.environ.get("INSTANCE_CONNECTION_NAME")
    db_host = os.environ.get("DB_HOST")
    db_port = int(os.environ.get("DB_PORT", "5432"))

    # Build connection function
    conn = None
    connector = None
    try:
        if instance_conn and HAVE_CONNECTOR:
            logger.info("Using Cloud SQL Python Connector to connect to %s", instance_conn)
            connector = Connector()
            conn = connector.connect(
                instance_conn,
                "pg8000",
                user=creds["user"],
                password=creds["password"],
                db=creds["dbname"],
            )
        elif db_host:
            logger.info("Connecting directly to host %s:%s", db_host, db_port)
            import pg8000 as _pg

            conn = _pg.connect(host=db_host, port=db_port, user=creds["user"], password=creds["password"], database=creds["dbname"])
        else:
            raise RuntimeError("No connection method available: set INSTANCE_CONNECTION_NAME or DB_HOST env vars")

        logger.info("Fetching contracts from Talana: %s", talana_url)
        items = list(fetch_contracts(talana_url, talana_token))
        logger.info("Fetched %d contracts", len(items))

        counts = upsert_many(conn, items)
        logger.info("Upsert contract counts: %s", counts)
        
        # Load vacaciones (vacation requests) - with error handling
        try:
            logger.info("Fetching vacaciones from Talana")
            vacaciones_items = list(fetch_vacaciones(talana_url, talana_token))
            logger.info("Fetched %d vacaciones", len(vacaciones_items))
            if vacaciones_items:
                vac_count = upsert_vacaciones(conn, vacaciones_items)
                logger.info("Upserted %d vacaciones records", vac_count)
            else:
                logger.warning("No vacaciones fetched from API")
        except Exception as e:
            logger.error("Failed to load vacaciones: %s", e, exc_info=True)
            logger.warning("Continuing without vacaciones data")
        
        # Load licencias (absences/leaves) - with error handling
        try:
            logger.info("Fetching licencias from Talana")
            licencias_items = list(fetch_licencias(talana_url, talana_token))
            logger.info("Fetched %d licencias", len(licencias_items))
            if licencias_items:
                lic_count = upsert_licencias(conn, licencias_items)
                logger.info("Upserted %d licencias records", lic_count)
            else:
                logger.warning("No licencias fetched from API")
        except Exception as e:
            logger.error("Failed to load licencias: %s", e, exc_info=True)
            logger.warning("Continuing without licencias data")

    finally:
        try:
            if conn:
                conn.close()
        except Exception:
            pass
        try:
            if connector:
                connector.close()
        except Exception:
            pass


if __name__ == "__main__":
    start = time.time()
    try:
        run()
    except Exception as e:
        logger.exception("ETL failed: %s", e)
        raise
    finally:
        logger.info("ETL finished in %.2fs", time.time() - start)
