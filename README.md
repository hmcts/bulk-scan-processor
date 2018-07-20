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
The API allows only HTTPS requests with approved client certificates.

Jenkins (pipeline) runs the API gateway tests by executing `apiGateway` gradle task. This only happens if
there's a call to `enableApiGatewayTest()` in your Jenkinsfile_CNP/Jenkinsfile_parameterized.

Your API tests rely on the following environment variables:
- `TEST_CLIENT_KEY_STORE` - Base64-encoded PKCS12 key store containing private key and certificate.
This piece of information needs to be manually set as a secret in Azure Key Vault.
- `TEST_CLIENT_KEY_STORE_PASSWORD` - Password the key store is protected with.
Like key store, this needs to be set as a secret in Azure Key Vault.
- `API_GATEWAY_URL` - The URL of the API (gateway) that is the target of tests.
This is provided by Jenkins based on the `api_gateway_url` output variable, defined in Terraform code.

Here's how to set secrets for tests in a given environment:

First, generate client private key, a certificate for that key and import both into a key store:

```
# generate private key
openssl genrsa 2048 > private.pem

# generate certificate
openssl req -x509 -new -key private.pem -out cert.pem

# create the key store
# when asked for password, provide one
openssl pkcs12 -export -in cert.pem -inkey private.pem -out cert.pfx -noiter -nomaciter
```

Now, store the content of the key store as a Base64-encoded secret in Azure Key Vault:

```
base64 cert.pfx | perl -pe 'chomp if eof' | xargs az keyvault secret set --vault-name rpe-bsp-{environment} --name test-client-key-store --value $1
```

... along with the password for that key store:

```
az keyvault secret set --vault-name rpe-bsp-{environment} --name test-client-key-store-password --value {the password you've set}
```

For the test certificate to be recognised by the API, set `api_gateway_test_certificate_thumbprint` input variable
with the thumbprint of the certificate for the right environment (in {environment}.tfvars file). In order
to calculate the thumbprint, run the following command:

```
openssl x509 -noout -fingerprint -inform pem -in cert.pem | sed -e s/://g
```

Your {environment}.tfvars file should look similar to this:

```
...
api_gateway_test_certificate_thumbprint = "8D81D05C0154423AE548D709CDDF9549E826C036"
...
```

Having pushed those changes, redeploy the service.


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
