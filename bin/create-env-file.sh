#!/usr/bin/env bash

# Script to create a .localenv file
# Format of command: sudo ./create-env-file.sh <key vault> <service name (in the chart yaml)> <env>
# Example of use: sudo ./create-env-file.sh bulk-scan bulk-scan-orchestrator aat
# Author/contact for updating: Adam Stevenson

# Refresh env file by removing one if it currently exists
rm ../.localenv

KEY_VAULT="${1}"
SERVICE_NAME="${2}"
ENV="${3}"

function fetch_secret_from_keyvault() {
    local SECRET_NAME=$1
    az keyvault secret show --vault-name "${KEY_VAULT}" --name "${SECRET_NAME}" --query "value"
}

function store_secret_from_keyvault() {
    local SECRET_VAR=$1
    local SECRET_NAME=$2
    SECRET_VALUE=$(fetch_secret_from_keyvault "${SECRET_NAME}")
    store_secret "${SECRET_VAR}" "${SECRET_VALUE}"
}

function store_secret() {
    local SECRET_VAR=$1
    local SECRET_VALUE=$2
    local SECRET_TO_WRITE="${SECRET_VAR}=${SECRET_VALUE}"
    SECRET_TO_WRITE=$(echo "${SECRET_TO_WRITE}" | tr -d '"' )
    echo "${SECRET_TO_WRITE}"
    echo "${SECRET_TO_WRITE}" >> ../.localenv
}

echo "# ----------------------- "
echo "# Populating substitutions to localenv file on ""$(date)"

SUBS_KEYS_JSON=$(jq -r '' substitutions.json)
IFS=$'\n'; set -f; SUBS_KEYS_ARRAY=($(<substitutions.json))

for ((i=0; i <= "${#SUBS_KEYS_ARRAY[@]}"-3; i+=1)) do
  SUB_PLACEHOLDER="${SUBS_KEYS_ARRAY[$((i+1))]}"
  SUB_NAME=${SUB_PLACEHOLDER%%:*}
  SUB_NAME=$(echo "${SUB_NAME}" | sed 's/ //g')
  SUB_NAME=$(echo "${SUB_NAME}" | sed 's/"//g')
  SUB_VALUE="$( cut -d ':' -f 2- <<< "${SUB_PLACEHOLDER}" )";
  SUB_VALUE=$(echo "${SUB_VALUE}" | sed 's/,//g')
  SUB_VALUE=$(echo "${SUB_VALUE}" | sed 's/"//g')
  SUB_VALUE="${SUB_VALUE:1}"
  SUB_COMBINED="${SUB_NAME}=${SUB_VALUE}"
  echo "${SUB_COMBINED}"
  echo "${SUB_COMBINED}" >> ../.localenv
done
echo "# End of substitutions "
echo "# ----------------------- "
echo "# ----------------------- "
echo "# Populating secrets to localenv file from ${KEY_VAULT} on ""$(date)"

# Secrets from Azure listed in chart, excluding substitutions
SECRETS=$(yq eval ".java.keyVaults.${KEY_VAULT}.secrets[]" ../charts/"${SERVICE_NAME}"/values.yaml)
SECRETS=${SECRETS//alias: /}
SECRETS=${SECRETS//name: /}
SECRETS_AS_ARRAY=("${x//\n/}")
readarray -t SECRETS_AS_ARRAY <<<"$SECRETS"

KEY_VAULT="${KEY_VAULT}-${ENV}"
LENGTH="${#SECRETS_AS_ARRAY[@]}"
for ((i=0; i <= LENGTH-1; i+=2)) do
  ENV_NAME="${SECRETS_AS_ARRAY[$((i+1))]}"
  ENV_NAME=${ENV_NAME^^}
  ENV_NAME=$(echo "${ENV_NAME}" | tr . _)
  ENV_NAME=$(echo "${ENV_NAME}" | tr - _)

  if [[ ! " ${SUBS_KEYS_ARRAY[*]} " =~ ${ENV_NAME} ]]; then
    ENV_VALUE=${SECRETS_AS_ARRAY[${i}]}
    store_secret_from_keyvault "${ENV_NAME}" "${ENV_VALUE}"
  else
    echo "Ignoring ${ENV_NAME} as it is listed within substitutions.json"
  fi
done
echo "# End of fetched secrets. "
echo "# ----------------------- "

echo "# ----------------------- "
echo "# Populating environment variables from chart to localenv file from ${KEY_VAULT} on ""$(date)"

# Get environment var list from chart, and save to file. Loop through as we need to exclude substitutions
ENVIRONMENT_LIST=$(yq eval ".java.environment" ../charts/"${SERVICE_NAME}"/values.yaml)
ENVIRONMENT_LIST=$(echo "${ENVIRONMENT_LIST}" | tr -d '"' )
ENVIRONMENT_LIST=$(echo "${ENVIRONMENT_LIST}" | tr -d '{{')
ENVIRONMENT_LIST=$(echo "${ENVIRONMENT_LIST}" | tr -d '}}')
ENVIRONMENT_LIST=$(echo "${ENVIRONMENT_LIST}" | tr -d '}}')
ENVIRONMENT_LIST=${ENVIRONMENT_LIST// /}
ENVIRONMENT_LIST_AS_ARRAY=("${x//\n/}")
readarray -t ENVIRONMENT_LIST_AS_ARRAY <<<"${ENVIRONMENT_LIST}"

ENV_LENGTH="${#ENVIRONMENT_LIST_AS_ARRAY[@]}"
for ((i=0; i <= ENV_LENGTH-1; i++)) do
  ENV_NAME_AND_VALUE_PLACEHOLDER=${ENVIRONMENT_LIST_AS_ARRAY[i]}
  ENV_NAME_AND_VALUE_PLACEHOLDER="$(sed "s/.Values.global.environment/${ENV}/g" <<<${ENV_NAME_AND_VALUE_PLACEHOLDER})"
  ENV_NAME=${ENV_NAME_AND_VALUE_PLACEHOLDER%%:*}
  ENV_VALUE="$( cut -d ':' -f 2- <<< "${ENV_NAME_AND_VALUE_PLACEHOLDER}" )";
  ENV_NAME_AND_VALUE="${ENV_NAME}=${ENV_VALUE}"
  if [[ ! " ${SUBS_KEYS_ARRAY[*]} " =~ ${ENV_NAME} ]]; then
    echo "${ENV_NAME_AND_VALUE}"
    echo "${ENV_NAME_AND_VALUE}" >> ../.localenv
  else
    echo "Ignoring ${ENV_NAME} as it is listed within substitutions.json"
  fi
done

# Give ownership of file to allow editing if needed
chmod 775 ../.localenv

echo "# End of fetched environment variables. "
echo "# ----------------------- "
