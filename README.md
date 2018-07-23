# Bulk scan processor

[![Build Status](https://travis-ci.org/hmcts/bulk-scan-processor.svg?branch=master)](https://travis-ci.org/hmcts/bulk-scan-processor)
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

### Setting up API (gateway) tests

Bulk Scan Processor uses an (Azure API Management) API to protect its SAS token dispensing endpoint.
The API allows only HTTPS requests with a valid subscription key.

Jenkins (pipeline) runs the API gateway tests by executing `apiGateway` gradle task. This only happens if
there's a call to `enableApiGatewayTest()` in your Jenkinsfile_CNP/Jenkinsfile_parameterized.

Your API tests rely on the following environment variables:
- `TEST_CLIENT_SUBSCRIPTION_KEY` - subscription key that allows access to the API. Must be set manually in Azure Key Vault.
- `API_GATEWAY_URL` - The URL of the API (gateway) that is the target of tests.
This is provided by Jenkins based on the `api_gateway_url` output variable, defined in Terraform code.

Here's how to set the subscription key in Azure Key Vault:

**Get subscription key**

You can get your subscription key using Azure Portal. In order to do this, perform the following steps:
- Search for the right API Management Service instance (`core-api-mgmt-{environment}`) and navigate to its page
- From the API Management service page, navigate to Developer portal (`Developer portal` link at the top bar)
- In developer portal navigate to `Products` tab and click on `bulk-scan`
- Click on one of the subscriptions from the list (at least `bulk-scan (default)` should be present).
- Click on the `Show` link next to the Primary Key of one of the bulk-scan subscriptions. This will
reveal the key.


**Store subscription key in Azure Key Vault so that API tests can use it**

```
az keyvault secret set --vault-name rpe-bsp-{environment} --name test-client-subscription-key --value {the subscription key}
```

When running the pipeline, Jenkins will read this secret and convert it to `TEST_CLIENT_SUBSCRIPTION_KEY`
environment variable, available to tests.

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

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details
