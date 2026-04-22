param(
    # Optional path to environment file used for local defaults.
    [string]$EnvFile = (Join-Path $PSScriptRoot "..\.env"),
    # Bucket to configure; falls back to S3_BUCKET env var.
    [string]$BucketName = $env:S3_BUCKET,
    # Comma-separated allowed origins; falls back to CORS_ALLOWED_ORIGINS env var.
    [string]$Origins = $env:CORS_ALLOWED_ORIGINS,
    # S3-compatible endpoint (Scaleway object storage endpoint by default).
    [string]$Endpoint = $env:SCW_ENDPOINT,
    # Region for AWS CLI signing (Scaleway region by default).
    [string]$Region = $env:SCW_REGION
)

# NOTE: This script is the current operational path for CORS updates.
# TODO: Move bucket/CORS configuration to IaC (for example Terraform) so
# all environments are versioned and applied automatically in deployment workflows.

$ErrorActionPreference = "Stop"
$AwsCliImage = "amazon/aws-cli"
$DefaultEndpoint = "https://s3.fr-par.scw.cloud"
$DefaultRegion = "fr-par"
$DefaultOrigins = "http://localhost:5173"

function Show-Usage {
    Write-Host @"
Usage:
  .\set-bucket-cors.ps1 [-EnvFile path] [-BucketName name] [-Origins csv] [-Endpoint url] [-Region name]

Environment:
  SCW_ACCESS_KEY         Required
  SCW_SECRET_KEY         Required
  S3_BUCKET              Used when -BucketName is not passed
  SCW_ENDPOINT           Optional (default: https://s3.fr-par.scw.cloud)
  SCW_REGION             Optional (default: fr-par)
  CORS_ALLOWED_ORIGINS   Optional (default: http://localhost:5173)
"@
}

function Require-Command([string]$CommandName) {
    if (-not (Get-Command $CommandName -ErrorAction SilentlyContinue)) {
        throw "$CommandName is required but was not found in PATH."
    }
}

function Read-EnvFile([string]$Path) {
    # Load KEY=VALUE lines from .env into process environment.
    # Existing process env vars are preserved (caller overrides file values).
    if (-not (Test-Path $Path)) { return }

    Get-Content $Path | ForEach-Object {
        $line = $_.Trim()
        if ([string]::IsNullOrWhiteSpace($line) -or $line.StartsWith("#")) { return }
        $parts = $line.Split("=", 2)
        if ($parts.Count -ne 2) { return }

        $key = $parts[0].Trim()
        $value = $parts[1].Trim().Trim("`"").Trim("'")
        if ([string]::IsNullOrWhiteSpace($key)) { return }

        # Respect explicit process env if already set by caller.
        if (-not (Test-Path "Env:$key")) {
            Set-Item -Path "Env:$key" -Value $value
        }
    }
}

function Require-EnvVar([string]$Name) {
    # Fail fast when required credentials are missing.
    if ([string]::IsNullOrWhiteSpace((Get-Item "Env:$Name" -ErrorAction SilentlyContinue).Value)) {
        throw "$Name must be set in your environment."
    }
}

if ($args -contains "-h" -or $args -contains "--help") {
    Show-Usage
    exit 0
}

Require-Command "docker"
Read-EnvFile $EnvFile

# Resolve values from parameters first, then env vars, then defaults.
if ([string]::IsNullOrWhiteSpace($BucketName)) { $BucketName = $env:S3_BUCKET }
if ([string]::IsNullOrWhiteSpace($Origins)) { $Origins = $env:CORS_ALLOWED_ORIGINS }
if ([string]::IsNullOrWhiteSpace($Endpoint)) { $Endpoint = $env:SCW_ENDPOINT }
if ([string]::IsNullOrWhiteSpace($Region)) { $Region = $env:SCW_REGION }

Require-EnvVar "SCW_ACCESS_KEY"
Require-EnvVar "SCW_SECRET_KEY"

if ([string]::IsNullOrWhiteSpace($BucketName)) { throw "S3_BUCKET must be set (or pass -BucketName)." }
if ([string]::IsNullOrWhiteSpace($Endpoint)) { $Endpoint = $DefaultEndpoint }
if ([string]::IsNullOrWhiteSpace($Region)) { $Region = $DefaultRegion }
if ([string]::IsNullOrWhiteSpace($Origins)) { $Origins = $DefaultOrigins }

# Convert comma-separated origins into a clean array for JSON.
$originList = $Origins.Split(",") |
    ForEach-Object { $_.Trim() } |
    Where-Object { -not [string]::IsNullOrWhiteSpace($_) }

if ($originList.Count -eq 0) {
    throw "At least one origin is required. Set CORS_ALLOWED_ORIGINS or pass -Origins."
}

$corsConfig = @{
    CORSRules = @(
        @{
            AllowedOrigins = $originList
            AllowedHeaders = @("*")
            AllowedMethods = @("GET", "HEAD", "POST", "PUT", "DELETE")
            MaxAgeSeconds = 3000
            ExposeHeaders = @("Etag")
        }
    )
}

# Use a temporary JSON file so Dockerized AWS CLI can read it as file:///cors.json.
$tempFile = [System.IO.Path]::GetTempFileName()
$corsFile = [System.IO.Path]::ChangeExtension($tempFile, ".json")
Move-Item -Path $tempFile -Destination $corsFile -Force

try {
    # Write CORS configuration JSON to temporary file.
    $corsConfig | ConvertTo-Json -Depth 10 | Set-Content -Path $corsFile -Encoding UTF8

    Write-Host "Applying CORS to bucket '$BucketName' on endpoint '$Endpoint'..."

    # Apply CORS with AWS CLI running in Docker (no local AWS CLI install required).
    $putArgs = @(
        "run", "--rm",
        "-e", "AWS_ACCESS_KEY_ID=$env:SCW_ACCESS_KEY",
        "-e", "AWS_SECRET_ACCESS_KEY=$env:SCW_SECRET_KEY",
        "-e", "AWS_DEFAULT_REGION=$Region",
        "-v", "${corsFile}:/cors.json:ro",
        $AwsCliImage,
        "s3api", "put-bucket-cors",
        "--bucket", $BucketName,
        "--cors-configuration", "file:///cors.json",
        "--endpoint-url", $Endpoint
    )

    & docker @putArgs

    Write-Host "Verifying CORS configuration..."

    # Read CORS back from bucket to confirm what was applied.
    $getArgs = @(
        "run", "--rm",
        "-e", "AWS_ACCESS_KEY_ID=$env:SCW_ACCESS_KEY",
        "-e", "AWS_SECRET_ACCESS_KEY=$env:SCW_SECRET_KEY",
        "-e", "AWS_DEFAULT_REGION=$Region",
        $AwsCliImage,
        "s3api", "get-bucket-cors",
        "--bucket", $BucketName,
        "--endpoint-url", $Endpoint
    )

    & docker @getArgs
    Write-Host "CORS configuration applied successfully."
}
finally {
    # Always clean up temporary file, even if apply/verify fails.
    if (Test-Path $corsFile) {
        Remove-Item -Path $corsFile -Force
    }
}
