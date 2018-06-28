#!/usr/bin/env bash

set -e

if [ -z "$BULK_SCANNING_DB_PASSWORD" ]; then
  echo "ERROR: Missing environment variable. Set value for 'BULK_SCANNING_DB_PASSWORD'."
  exit 1
fi

psql -v ON_ERROR_STOP=1 --username postgres --set USERNAME=bulkscanner --set PASSWORD=${BULK_SCANNING_DB_PASSWORD} <<-EOSQL
  CREATE USER :USERNAME WITH PASSWORD ':PASSWORD';
  CREATE DATABASE bulkscans
    WITH OWNER = :USERNAME
    ENCODING = 'UTF-8'
    CONNECTION LIMIT = -1;
EOSQL
