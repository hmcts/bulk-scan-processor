#!/usr/bin/env bash

# Main script for setting up environment
# Format of command: sudo ./setup-env.sh <key vault> <service name (in the chart yaml)> <env>
# Example of use: sudo ./setup-env.sh bulk-scan bulk-scan-orchestrator aat
# Author/contact for updating: Adam Stevenson
KEY_VAULT="${1}"
SERVICE_NAME="${2}"
ENV="${3}"

docker-compose down -v
docker-compose build
docker-compose up -d

sudo ./create-env-file.sh "${KEY_VAULT}" "${SERVICE_NAME}" "${ENV}"

echo "Setup complete! Next step is to add the .localenv file through the ENV plugin and run the application afterwards"
