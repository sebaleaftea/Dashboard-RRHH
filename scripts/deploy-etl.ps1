param(
    [string]$ProjectId = 'prd-sfh-it-bi-erbi',
    [string]$Region = 'southamerica-west1',
    [string]$InstanceConnectionName = 'prd-sfh-it-bi-erbi:southamerica-west1:bdd-grh',
    [string]$ImageName = 'gcr.io/prd-sfh-it-bi-erbi/employee-etl:latest',
    [string]$ServiceAccount = 'etl-runner',
    [string]$TalanaSecret = 'talana-api-token',
    [switch]$CreateServiceAccount,
    [switch]$CreateDbSecrets,
    [string]$DbUser = 'grh-bdduser',
    [string]$DbPass = 'c_}2ysUR"6dXEk]o',
    [switch]$UseCloudBuild
)

function Exec([string]$cmd) {
    Write-Host "> $cmd"
    Invoke-Expression $cmd
    if ($LASTEXITCODE -ne 0 -and $LASTEXITCODE -ne $null) {
        Write-Error "Command failed with exit code $LASTEXITCODE : $cmd"
        exit $LASTEXITCODE
    }
}

Write-Host "Deploy ETL: project=$ProjectId region=$Region image=$ImageName"

Exec "gcloud config set project $ProjectId"

if ($CreateServiceAccount) {
    Write-Host "Creating service account $ServiceAccount..."
    Exec "gcloud iam service-accounts create $ServiceAccount --display-name='ETL Runner' --project=$ProjectId"
    $saEmail = "$ServiceAccount@$ProjectId.iam.gserviceaccount.com"
    Exec "gcloud projects add-iam-policy-binding $ProjectId --member=serviceAccount:$saEmail --role=roles/cloudsql.client"
    Exec "gcloud projects add-iam-policy-binding $ProjectId --member=serviceAccount:$saEmail --role=roles/secretmanager.secretAccessor"
    Exec "gcloud projects add-iam-policy-binding $ProjectId --member=serviceAccount:$saEmail --role=roles/logging.logWriter"
} else {
    $saEmail = "$ServiceAccount@$ProjectId.iam.gserviceaccount.com"
    Write-Host "Using existing service account: $saEmail"
}

if ($CreateDbSecrets) {
    Write-Host "Creating DB secrets in Secret Manager..."
    $userCmd = "echo -n '$DbUser' | gcloud secrets create db-user --data-file=- --project=$ProjectId --replication-policy=automatic"
    $passCmd = "echo -n '$DbPass' | gcloud secrets create db-pass --data-file=- --project=$ProjectId --replication-policy=automatic"
    Exec $userCmd
    Exec $passCmd
}

if ($UseCloudBuild) {
    Write-Host "Building image with Cloud Build: $ImageName"
    Push-Location "$PSScriptRoot\.."
    Exec "gcloud builds submit --tag $ImageName --project=$ProjectId .\etl"
    Pop-Location
} else {
    Write-Host "Building image locally (requires docker). If you prefer Cloud Build, re-run with -UseCloudBuild"
    Push-Location "$PSScriptRoot\.."
    Exec "docker build -t $ImageName .\etl"
    Exec "docker push $ImageName"
    Pop-Location
}

# Create or replace job
$jobName = 'employee-etl-job'
$existing = gcloud run jobs describe $jobName --region=$Region --project=$ProjectId 2>$null
if ($LASTEXITCODE -eq 0) {
    Write-Host "Job $jobName exists - deleting to recreate"
    Exec "gcloud run jobs delete $jobName --region=$Region --project=$ProjectId --quiet"
}

Write-Host "Creating Cloud Run Job $jobName"
$setSecrets = "TALANA_TOKEN=$TalanaSecret:latest"
if ($CreateDbSecrets) { 
    $setSecrets += ",DB_USER=db-user:latest,DB_PASS=db-pass:latest" 
}

$createCmd = "gcloud run jobs create $jobName --image=$ImageName --region=$Region --task-timeout=1200s --set-secrets=$setSecrets --add-cloudsql-instances=$InstanceConnectionName --service-account=$saEmail --project=$ProjectId"
Exec $createCmd

Write-Host "Executing job (this will run once now)..."
Exec "gcloud run jobs execute $jobName --region=$Region --project=$ProjectId"

Write-Host "Listing recent executions..."
Exec "gcloud run jobs executions list --job=$jobName --region=$Region --project=$ProjectId --limit=10"

Write-Host ""
Write-Host "SUCCESS! You can view logs with:"
Write-Host "  gcloud logging read `"resource.type=cloud_run_job AND resource.labels.job_name=$jobName`" --project=$ProjectId --limit=200"
Write-Host ""
Write-Host "To schedule this job with Cloud Scheduler (example: every 6 hours):"
Write-Host "  gcloud scheduler jobs create http employee-etl-schedule \"
Write-Host "    --schedule=`"0 */6 * * *`" \"
Write-Host "    --uri=`"https://run.googleapis.com/apis/run.googleapis.com/v1/namespaces/$ProjectId/locations/$Region/jobs/$jobName:run`" \"
Write-Host "    --http-method=POST \"
Write-Host "    --oidc-service-account-email=$saEmail \"
Write-Host "    --oidc-token-audience=`"https://run.googleapis.com`" \"
Write-Host "    --project=$ProjectId \"
Write-Host "    --location=$Region"
