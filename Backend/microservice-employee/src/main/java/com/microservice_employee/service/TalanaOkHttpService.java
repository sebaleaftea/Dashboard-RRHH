package com.microservice_employee.service;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import java.io.IOException;
import java.util.Map;

@Service
@Profile("talana")
public class TalanaOkHttpService {

    @Value("${talana.api.token}")
    private String apiToken;

    @Value("${talana.api.base-url}")
    private String baseUrl;

    // Talana recomienda 20 requests/min => mínimo ~3000ms entre requests.
    private static final long MIN_INTERVAL_MS = 3_000L;
    private final Object rateLock = new Object();
    private long nextAllowedRequestAtMs = 0L;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(java.time.Duration.ofMinutes(3))
            .readTimeout(java.time.Duration.ofMinutes(3))
            .writeTimeout(java.time.Duration.ofMinutes(3))
            .build();


    public String getPersonas() throws IOException {
        // por defecto page_size 20 (más rápido y menos riesgo de 429)
        return getPersonasPaginadas(1, 20);
    }

    /**
     * Endpoint recomendado por Talana para volúmenes grandes.
     * Algunos entornos lo publican como /personas-paginadas/ y otros como /persona-paginado/.
     */
    public String getPersonasPaginadas(int page, int pageSize) throws IOException {
        if (page < 1) {
            page = 1;
        }
        if (pageSize < 1) {
            pageSize = 20;
        }

        Map<String, String> qp = Map.of(
                "page", String.valueOf(page),
                "page_size", String.valueOf(pageSize)
        );
        return getPersonasPaginadas(qp);
    }

    /**
     * Docs: /es/api/persona-paginado/
     * Acepta query params arbitrarios para búsqueda/filtros (según soporte de la cuenta Talana).
     */
    public String getPersonasPaginadas(Map<String, String> queryParams) throws IOException {
        String pathPrimary = "/persona-paginado/";
        try {
            return getFromTalana(pathPrimary, queryParams);
        } catch (IOException ex) {
            if (isHttpStatus(ex, 404)) {
                return getFromTalana("/personas-paginadas/", queryParams);
            }
            throw ex;
        }
    }

    /**
     * Endpoint oficial para contratos paginados.
     * Docs: /es/api/contrato-paginado/
     */
    public String getContratosPaginados(Map<String, String> queryParams) throws IOException {
        return getFromTalana("/contrato-paginado/", queryParams);
    }

    /**
     * Docs: /es/api/contrato/{id}/
     */
    public String getContratoDetalle(long contratoId) throws IOException {
        return getFromTalana("/contrato/" + contratoId + "/", null);
    }

    /**
     * Legacy (no recomendado) - se mantiene por compatibilidad.
     */
    public String getPersonas(int limit, int offset) throws IOException {
        String path = String.format("/persona/?limit=%d&offset=%d", limit, offset);
        return getFromTalana(path);
    }

    public String getPersonaDetalle(int personaId) throws IOException {
        String path = String.format("/persona/%d/", personaId);
        return getFromTalana(path);
    }

    public String getCentroCostos() throws IOException {
        return getCentroCostos(null, null);
    }

    public String getCentroCostos(Integer limit, Integer offset) throws IOException {
        // Docs suelen usar /centro-costo/; algunas instalaciones usan /centroCosto/
        String primary = "/centro-costo/";
        Map<String, String> qp = null;
        if (limit != null && offset != null) {
            qp = Map.of("limit", String.valueOf(limit), "offset", String.valueOf(offset));
        }
        try {
            return getFromTalana(primary, qp);
        } catch (IOException ex) {
            if (isHttpStatus(ex, 404)) {
                return getFromTalana("/centroCosto/", qp);
            }
            throw ex;
        }
    }

    public String getSucursales() throws IOException {
        return getSucursales(null, null);
    }

    public String getSucursales(Integer limit, Integer offset) throws IOException {
        Map<String, String> qp = null;
        if (limit != null && offset != null) {
            qp = Map.of("limit", String.valueOf(limit), "offset", String.valueOf(offset));
        }
        return getFromTalana("/sucursal/", qp);
    }

    public String getJobTitles() throws IOException {
        return getJobTitles(null, null);
    }

    public String getJobTitles(Integer limit, Integer offset) throws IOException {
        Map<String, String> qp = null;
        if (limit != null && offset != null) {
            qp = Map.of("limit", String.valueOf(limit), "offset", String.valueOf(offset));
        }
        return getFromTalana("/job-title/", qp);
    }

    public String getFromTalana(String path) throws IOException {
        return getFromTalana(path, null);
    }

    public String getFromTalana(String path, Map<String, String> queryParams) throws IOException {
        if (apiToken == null || apiToken.isBlank()) {
            throw new IOException("HTTP 401 calling Talana: missing token (set TALANA_API_TOKEN env var or talana.api.token)");
        }
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String full = base + (path.startsWith("/") ? path : "/" + path);

        HttpUrl parsed = HttpUrl.parse(full);
        if (parsed == null) {
            throw new IOException("Invalid Talana URL: " + full);
        }
        HttpUrl.Builder urlBuilder = parsed.newBuilder();
        if (queryParams != null && !queryParams.isEmpty()) {
            for (Map.Entry<String, String> e : queryParams.entrySet()) {
                if (e.getKey() == null || e.getKey().isBlank()) continue;
                if (e.getValue() == null) continue;
                urlBuilder.setQueryParameter(e.getKey(), e.getValue());
            }
        }
        String url = urlBuilder.build().toString();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("accept", "application/json")
                .addHeader("Authorization", "Token " + apiToken)
                .build();

        return executeWithRetryAllowAuthFallback(request, url);
    }

    public String postToTalana(String path, String jsonBody) throws IOException {
        return sendBodyToTalana("POST", path, jsonBody);
    }

    public String patchToTalana(String path, String jsonBody) throws IOException {
        return sendBodyToTalana("PATCH", path, jsonBody);
    }

    private String sendBodyToTalana(String method, String path, String jsonBody) throws IOException {
        if (apiToken == null || apiToken.isBlank()) {
            throw new IOException("HTTP 401 calling Talana: missing token (set TALANA_API_TOKEN env var or talana.api.token)");
        }
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String full = base + (path.startsWith("/") ? path : "/" + path);
        HttpUrl parsed = HttpUrl.parse(full);
        if (parsed == null) {
            throw new IOException("Invalid Talana URL: " + full);
        }
        String url = parsed.toString();

        MediaType json = MediaType.get("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(jsonBody == null ? "{}" : jsonBody, json);

        Request.Builder b = new Request.Builder()
                .url(url)
                .addHeader("accept", "application/json")
                .addHeader("content-type", "application/json")
                .addHeader("Authorization", "Token " + apiToken);

        if ("POST".equalsIgnoreCase(method)) {
            b.post(body);
        } else if ("PATCH".equalsIgnoreCase(method)) {
            b.patch(body);
        } else {
            throw new IllegalArgumentException("Unsupported method: " + method);
        }

        return executeWithRetryAllowAuthFallback(b.build(), url);
    }

    private String executeWithRetry(Request request, String urlForError) throws IOException {
        enforceRateLimit();

        int attempts = 0;
        int maxAttempts = 5;
        while (true) {
            try (Response response = client.newCall(request).execute()) {
                int code = response.code();
                if (code == 429 && attempts < maxAttempts) {
                    attempts++;

                    long backoffMs = parseRetryAfterMs(response);
                    if (backoffMs <= 0) {
                        long baseMs = 1000L;
                        long expMs = baseMs * (1L << (attempts - 1));
                        long jitterMs = (long) (Math.random() * 250L);
                        backoffMs = Math.min(30_000L, expMs + jitterMs);
                    }

                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    continue;
                }
                if (!response.isSuccessful()) {
                    String body = response.body() != null ? response.body().string() : "";
                    String snippet = body == null ? "" : body;
                    if (snippet.length() > 500) {
                        snippet = snippet.substring(0, 500) + "...";
                    }
                    throw new IOException(
                            "HTTP " + code + " calling " + urlForError + " response=" + snippet
                    );
                }
                return response.body() != null ? response.body().string() : null;
            }
        }
    }

    /**
     * Ejecuta la petición con reintentos y si recibe 401 intenta un esquema alternativo de Authorization.
     * Algunos entornos de Talana requieren "Bearer <token>" en lugar de "Token <token>".
     */
    private String executeWithRetryAllowAuthFallback(Request request, String urlForError) throws IOException {
        try {
            return executeWithRetry(request, urlForError);
        } catch (IOException ex) {
            // Si fue 401 y el header actual es "Token ...", probamos con "Bearer ..."
            if (isHttpStatus(ex, 401)) {
                String currentAuth = request.header("Authorization");
                if (currentAuth != null && currentAuth.startsWith("Token ")) {
                    Request.Builder alt = request.newBuilder();
                    alt.removeHeader("Authorization");
                    alt.addHeader("Authorization", "Bearer " + apiToken);
                    return executeWithRetry(alt.build(), urlForError);
                }
            }
            throw ex;
        }
    }

    private void enforceRateLimit() {
        long sleepMs = 0L;
        synchronized (rateLock) {
            long now = System.currentTimeMillis();
            if (now < nextAllowedRequestAtMs) {
                sleepMs = nextAllowedRequestAtMs - now;
            }
            nextAllowedRequestAtMs = Math.max(nextAllowedRequestAtMs, now) + MIN_INTERVAL_MS;
        }
        if (sleepMs > 0) {
            try {
                Thread.sleep(sleepMs);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static long parseRetryAfterMs(Response response) {
        String retryAfter = response.header("Retry-After");
        if (retryAfter == null || retryAfter.isBlank()) {
            return -1;
        }
        try {
            long seconds = Long.parseLong(retryAfter.trim());
            if (seconds < 0) {
                return -1;
            }
            return Math.min(60_000L, seconds * 1000L);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static boolean isHttpStatus(IOException ex, int statusCode) {
        if (ex == null || ex.getMessage() == null) {
            return false;
        }
        return ex.getMessage().startsWith("HTTP " + statusCode + " ");
    }
}
