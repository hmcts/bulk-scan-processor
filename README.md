# Bulk scan processor

TEST

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

### Running integration tests

```bash
  ./gradlew integration
```

## Migration

To run migration gradle task expects `FLYWAY_URL` to be present. In case db requires username/password: `FLYWAY_USER` and `FLYWAY_PASSWORD`. Once those variables are exported all flyway tasks are available.

```bash
./gradlew flywayMigrate
```

## API (gateway)

Bulk Scan Processor uses an (Azure API Management) API to protect its SAS token dispensing endpoint.
The API allows only HTTPS requests with approved client certificates and valid subscription keys to reach
the service.

### Calling the API

In order to talk to the SAS dispensing endpoint through the API, you need to have the following pieces
of information:
- a certificate whose thumbprint is known to the API (has to be added to the list of allowed thumbprints in [main.tf](infrastructure/main.tf))
- a valid subscription key
- name of an existing client service (e.g. `test`)

#### Preparing client certificate
First, generate client private key, a certificate for that key and import both into a key store:

```
# generate private key
openssl genrsa 2048 > private.pem

# generate certificate
openssl req -x509 -new -key private.pem -out cert.pem -days 365

# create the key store
# when asked for password, provide one
openssl pkcs12 -export -in cert.pem -inkey private.pem -out cert.pfx -noiter -nomaciter
```

Next, calculate the thumbprint of your certificate:

```
openssl x509 -noout -fingerprint -inform pem -in cert.pem | sed -e s/://g
```

Finally, add this thumbprint to `allowed_client_certificate_thumbprints` input variable (Terraform)
for the target environment (e.g. in `saat.tfvars` file). Your definition may look similar to this:

```
allowed_client_certificate_thumbprints = ["2FC66765E63BB2436F0F9E4F59E951A6D1D20D43"]
```

Once you're run the deployment, the API will recognise your certificate.

#### Retrieving subscription key

You can get your subscription key for the API using Azure Portal. In order to do this, perform the following steps:
- Search for the right API Management service instance (`core-api-mgmt-{environment}`) and navigate to its page
- From the API Management service page, navigate to Developer portal (`Developer portal` link at the top bar)
- In developer portal navigate to `Products` tab and click on `bulk-scan`
- Click on one of the subscriptions from the list (at least `bulk-scan (default)` should be present).
- Click on the `Show` link next to the Primary Key of one of the bulk-scan subscriptions. This will
reveal the key. You will need to provide this value in your request to the API.

#### Getting the token through the API

You can call the API using the following curl command (assuming your current directory contains the private key
and certificate you've created earlier):

```
curl -v --key private.pem --cert cert.pem https://core-api-mgmt-{environment}.azure-api.net/bulk-scan/token/{service name} -H "Ocp-Apim-Subscription-Key:{subscription key}"
```

You should get a response with status 200 and a token in the body.

### API tests

Jenkins (pipeline) runs the API gateway tests by executing `apiGateway` gradle task. This happens because
there's a call to `enableApiGatewayTest()` in your Jenkinsfile_CNP/Jenkinsfile_parameterized. API tests
are located in [apiGatewayTest](src/apiGatewayTest) directory.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details
