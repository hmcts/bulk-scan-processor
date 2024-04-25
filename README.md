# Bulk scan processor

![](https://github.com/hmcts/bulk-scan-processor/workflows/CI/badge.svg)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/958fd3d74a194a0f8b9529cbc012293e)](https://www.codacy.com/app/HMCTS/bulk-scan-processor)
[![codecov](https://codecov.io/gh/hmcts/bulk-scan-processor/branch/master/graph/badge.svg)](https://codecov.io/gh/hmcts/bulk-scan-processor)
[![Known Vulnerabilities](https://snyk.io/test/github/hmcts/bulk-scan-processor/badge.svg)](https://snyk.io/test/github/hmcts/bulk-scan-processor)

## Purpose

Retrieve scanned documents along with information extracted with OCR engine. Store the images and let recipient
services fetch the new data.

## Building and deploying the application

The project uses [Gradle](https://gradle.org) as a build tool. It already contains
`./gradlew` wrapper script, so there's no need to install gradle.

### Building the application

To build the project execute the following command:

```bash
  ./gradlew build
```

### Running the application

You will either need to add a .env file if one does not already exist, or add environment variables to your
Application tasks configuration.

If you choose to add a .env file, you will need to add all the
environment variables listed in the [application.yaml](/src/main/resources/application.yaml) file.

For example, ${BULK_SCANNING_DB_PASSWORD:} in the application.yaml file will need to be
added as BULK_SCANNING_DB_PASSWORD="value from keyvault" in your .env file.

Create the image of the application by executing the following command:

```bash
  ./gradlew assemble
```

Application listens on port `8581` which can be overridden by setting `SERVER_PORT` environment variable or from [.env](/.env) file.

The application depends upon certain components that are already up and running.
Configuration details for each component can be changed by passing values in environment variables:

#### PostgreSQL
 * `BULK_SCANNING_DB_HOST`
 * `BULK_SCANNING_DB_PORT`
 * `BULK_SCANNING_DB_NAME`
 * `BULK_SCANNING_DB_USER_NAME`
 * `BULK_SCANNING_DB_PASSWORD`

#### Azure Blob Storage
 * `STORAGE_ACCOUNT_NAME`
 * `STORAGE_KEY`
 * `SAS_TOKEN_VALIDITY`

#### Document Management Storage
 * `DOCUMENT_MANAGEMENT_URL` working endpoint URL

#### Service to Service Authentication
 * `S2S_URL` working endpoint URL
 * `S2S_NAME` service name
 * `S2S_SECRET` service secret

Please find more details in [infrastructure/main.tf](/infrastructure/main.tf) file.

### Running smoke tests

Smoke tests expect an address of deployed application to be passed in `TEST_URL` environment variable. For example:

```bash
  TEST_URL=http://localhost:8561 ./gradlew smoke
```

By default, it will use `http://localhost:8581` which is defined in [src/smokeTest/resources/application.yaml](/src/smokeTest/resources/application.yaml).

### Running integration tests

```bash
  ./gradlew integration
```

## Migration

To run migration gradle task expects `FLYWAY_URL` to be present. In case db requires username/password: `FLYWAY_USER` and `FLYWAY_PASSWORD`. Once those variables are exported all flyway tasks are available.

```bash
./gradlew flywayMigrate
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
