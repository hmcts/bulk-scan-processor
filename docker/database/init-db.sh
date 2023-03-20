#!/usr/bin/env bash

set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    CREATE USER bulk_scan_processor;
    CREATE DATABASE bulk_scan_processor
        WITH OWNER = bulk_scan_processor
        ENCODING ='UTF-8'
        CONNECTION LIMIT = -1;
EOSQL

psql -v ON_ERROR_STOP=1 --dbname=bulk_scan_processor --username "$POSTGRES_USER" <<-EOSQL
    CREATE SCHEMA bulk_scan_processor AUTHORIZATION bulk_scan_processor;
EOSQL

psql -v ON_ERROR_STOP=1 --dbname=bulk_scan_processor --username "$POSTGRES_USER" <<-EOSQL
    CREATE EXTENSION lo;
EOSQL
