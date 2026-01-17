# Script para verificar los resultados del ETL en Cloud SQL
# Ejecutar en Cloud Shell

$query = @"
SELECT 
    'Contratos' as tabla, 
    COUNT(*) as registros,
    MAX(updated_at) as ultima_actualizacion
FROM contrato
UNION ALL
SELECT 
    'Empleados', 
    COUNT(*),
    MAX(updated_at)
FROM empleado
UNION ALL
SELECT 
    'Vacaciones', 
    COUNT(*),
    MAX(created_at)
FROM vacaciones
UNION ALL
SELECT 
    'Licencias', 
    COUNT(*),
    MAX(created_at)
FROM licencias
ORDER BY tabla;
"@

Write-Host "=== VERIFICACIÓN ETL - RESULTADOS ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "Ejecuta este comando en Cloud Shell:" -ForegroundColor Yellow
Write-Host ""
Write-Host "psql `"host=34.176.82.205 port=5432 dbname=grh user=grh-bdduser password='c_}2ysUR`"6dXEk]o' sslmode=require`" -c `"$query`"" -ForegroundColor White
Write-Host ""
Write-Host "O para ver solo los conteos:" -ForegroundColor Yellow
Write-Host ""
Write-Host "psql `"host=34.176.82.205 port=5432 dbname=grh user=grh-bdduser password='c_}2ysUR`"6dXEk]o' sslmode=require`" -c `"SELECT 'Contratos' as tabla, COUNT(*) as registros FROM contrato UNION ALL SELECT 'Empleados', COUNT(*) FROM empleado UNION ALL SELECT 'Vacaciones', COUNT(*) FROM vacaciones UNION ALL SELECT 'Licencias', COUNT(*) FROM licencias ORDER BY tabla;`"" -ForegroundColor White
Write-Host ""
Write-Host "O ver los logs del job:" -ForegroundColor Yellow
Write-Host ""
Write-Host "gcloud logging read `"resource.type=cloud_run_job AND resource.labels.job_name=employee-etl-job AND labels.run.googleapis.com/execution_name=employee-etl-job-d4fxd`" --limit=100 --format=`"value(textPayload)`" --project=prd-sfh-it-bi-erbi | grep -E `"(Loaded|Filtering|successfully|records)`"" -ForegroundColor White
