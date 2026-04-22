#!/usr/bin/env bash
set -euo pipefail

# NOTE: This script is the current operational path for CORS updates.
# TODO: Move bucket/CORS configuration to IaC (for example Terraform) so
# all environments are versioned and applied automatically in deployment workflows.

# Resolve script-relative defaults. Values can be overridden by ENV_FILE or args.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${ENV_FILE:-${SCRIPT_DIR}/../.env}"
AWS_CLI_IMAGE="amazon/aws-cli"
DEFAULT_ENDPOINT="https://s3.fr-par.scw.cloud"
DEFAULT_REGION="fr-par"
DEFAULT_ORIGINS="http://localhost:5173"

print_usage() {
  # Show help for both CLI args and supported env vars.
  cat <<'EOF'
Usage:
  ./set-bucket-cors.sh [bucket_name] [comma_separated_origins]

Environment:
  ENV_FILE               Optional path to .env file (default: ../.env)
  SCW_ACCESS_KEY         Required
  SCW_SECRET_KEY         Required
  S3_BUCKET              Used when bucket_name argument is not passed
  SCW_ENDPOINT           Optional (default: https://s3.fr-par.scw.cloud)
  SCW_REGION             Optional (default: fr-par)
  CORS_ALLOWED_ORIGINS   Optional (default: http://localhost:5173)
EOF
}

require_command() {
  # Fail fast if a required local command is missing.
  local command_name="$1"
  command -v "$command_name" >/dev/null 2>&1 || {
    echo "$command_name is required but was not found in PATH."
    exit 1
  }
}

require_var() {
  # Fail fast when required environment values are empty.
  local var_name="$1"
  local var_value="${!var_name:-}"
  [[ -n "$var_value" ]] || {
    echo "$var_name must be set."
    exit 1
  }
}

# Simple .env parser:
# - ignores comments/blank lines
# - supports KEY=value
# - strips surrounding single/double quotes
# - tolerates Windows CRLF line endings
load_env_file() {
  # Load .env values into process environment without requiring `source`.
  local file_path="$1"
  [[ -f "$file_path" ]] || return 0

  while IFS= read -r raw_line || [[ -n "$raw_line" ]]; do
    local line="${raw_line%$'\r'}"
    [[ -z "$line" || "$line" =~ ^[[:space:]]*# ]] && continue
    [[ "$line" =~ ^[A-Za-z_][A-Za-z0-9_]*= ]] || continue

    local key="${line%%=*}"
    local value="${line#*=}"
    value="${value%$'\r'}"

    if [[ "$value" =~ ^\".*\"$ || "$value" =~ ^\'.*\'$ ]]; then
      value="${value:1:${#value}-2}"
    fi

    export "$key=$value"
  done < "$file_path"
}

build_origins_json() {
  # Convert comma-separated origins into a JSON-safe quoted list.
  # Example: "http://a.com,http://b.com" -> "\"http://a.com\",\"http://b.com\""
  local origins="$1"
  local result=()

  IFS=',' read -r -a origin_array <<< "$origins"
  for origin in "${origin_array[@]}"; do
    local trimmed
    trimmed="$(echo "$origin" | xargs)"
    [[ -n "$trimmed" ]] && result+=("\"$trimmed\"")
  done

  [[ ${#result[@]} -gt 0 ]] || {
    echo "At least one CORS origin is required."
    exit 1
  }

  (IFS=,; echo "${result[*]}")
}

run_s3api() {
  # Execute AWS CLI in Docker so no local AWS CLI install is required.
  docker run --rm \
    -e "AWS_ACCESS_KEY_ID=${SCW_ACCESS_KEY}" \
    -e "AWS_SECRET_ACCESS_KEY=${SCW_SECRET_KEY}" \
    -e "AWS_DEFAULT_REGION=${REGION}" \
    "$@"
}

main() {
  if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
    print_usage
    exit 0
  fi

  require_command docker
  load_env_file "$ENV_FILE"

  # Value resolution order:
  # 1) CLI arguments
  # 2) Environment variables
  # 3) Script defaults
  BUCKET_NAME="${1:-${S3_BUCKET:-}}"
  ORIGINS="${2:-${CORS_ALLOWED_ORIGINS:-$DEFAULT_ORIGINS}}"
  ENDPOINT="${SCW_ENDPOINT:-$DEFAULT_ENDPOINT}"
  REGION="${SCW_REGION:-$DEFAULT_REGION}"

  require_var SCW_ACCESS_KEY
  require_var SCW_SECRET_KEY

  [[ -n "$BUCKET_NAME" ]] || {
    echo "Bucket name is required. Set S3_BUCKET or pass it as first argument."
    exit 1
  }

  local origins_json
  origins_json="$(build_origins_json "$ORIGINS")"

  # Use a temporary JSON file that can be mounted into Docker as /cors.json.
  local cors_file
  cors_file="$(mktemp /tmp/cors.XXXXXX.json)"
  trap 'rm -f "$cors_file"' EXIT

  # Build the exact CORS policy sent to the bucket.
  cat > "$cors_file" <<EOF
{
  "CORSRules": [
    {
      "AllowedOrigins": [${origins_json}],
      "AllowedHeaders": ["*"],
      "AllowedMethods": ["GET", "HEAD", "POST", "PUT", "DELETE"],
      "MaxAgeSeconds": 3000,
      "ExposeHeaders": ["Etag"]
    }
  ]
}
EOF

  # Apply the CORS policy.
  echo "Applying CORS to bucket '${BUCKET_NAME}' on '${ENDPOINT}'..."
  run_s3api \
    -v "${cors_file}:/cors.json:ro" \
    "$AWS_CLI_IMAGE" \
    s3api put-bucket-cors \
    --bucket "${BUCKET_NAME}" \
    --cors-configuration file:///cors.json \
    --endpoint-url "${ENDPOINT}"

  # Read it back to verify what is currently configured on the bucket.
  echo "Verifying CORS configuration..."
  run_s3api \
    "$AWS_CLI_IMAGE" \
    s3api get-bucket-cors \
    --bucket "${BUCKET_NAME}" \
    --endpoint-url "${ENDPOINT}"

  echo "Done."
}

main "$@"
