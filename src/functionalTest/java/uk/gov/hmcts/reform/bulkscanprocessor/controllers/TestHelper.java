package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.core.PathUtility;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import io.restassured.RestAssured;
import io.restassured.mapper.ObjectMapperType;
import io.restassured.response.Response;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.EnvelopeListResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.EnvelopeResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.ZipVerifiers;
import uk.gov.hmcts.reform.logging.appinsights.SyntheticHeaders;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

public class TestHelper {

    public String s2sSignIn(String s2sName, String s2sSecret, String s2sUrl) {
        Map<String, Object> params = ImmutableMap.of(
            "microservice", s2sName,
            "oneTimePassword", new GoogleAuthenticator().getTotpPassword(s2sSecret)
        );

        Response response = RestAssured
            .given()
            .relaxedHTTPSValidation()
            .baseUri(s2sUrl)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(params)
            .when()
            .post("/lease")
            .andReturn();

        assertThat(response.getStatusCode()).isEqualTo(200);

        return response
            .getBody()
            .asString();
    }

    public void uploadZipFile(
        CloudBlobContainer container,
        List<String> files,
        String metadataFile,
        final String destZipFilename,
        String testPrivateKeyDer
    ) throws Exception {
        byte[] zipFile =
            createSignedZipArchiveWithRandomName(files, metadataFile, destZipFilename, testPrivateKeyDer);
        CloudBlockBlob blockBlobReference = container.getBlockBlobReference(destZipFilename);
        blockBlobReference.uploadFromByteArray(zipFile, 0, zipFile.length);
    }

    public CloudBlockBlob uploadAndLeaseZipFile(
        CloudBlobContainer container,
        List<String> files,
        String metadataFile,
        String destZipFilename,
        String testPrivateKeyDer
    ) throws Exception {
        byte[] zipFile =
            createSignedZipArchiveWithRandomName(files, metadataFile, destZipFilename, testPrivateKeyDer);
        CloudBlockBlob blockBlobReference = container.getBlockBlobReference(destZipFilename);
        blockBlobReference.uploadFromByteArray(zipFile, 0, zipFile.length);
        blockBlobReference.acquireLease();
        return blockBlobReference;
    }

    public boolean storageHasFile(CloudBlobContainer container, String fileName) {
        return StreamSupport.stream(container.listBlobs().spliterator(), false)
            .anyMatch(listBlobItem -> listBlobItem.getUri().getPath().contains(fileName));
    }

    public String getSasToken(String containerName, String testUrl) throws Exception {
        Response tokenResponse = RestAssured
            .given()
            .relaxedHTTPSValidation()
            .baseUri(testUrl)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .header(SyntheticHeaders.SYNTHETIC_TEST_SOURCE, "Bulk Scan Processor functional test")
            .when().get("/token/" + containerName)
            .andReturn();

        assertThat(tokenResponse.getStatusCode()).isEqualTo(200);

        final ObjectNode node =
            new ObjectMapper().readValue(tokenResponse.getBody().asString(), ObjectNode.class);
        return node.get("sas_token").asText();
    }

    public CloudBlobContainer getCloudContainer(
        String sasToken, String containerName, String containerUrl
    ) throws Exception {
        URI containerUri = new URI(containerUrl + containerName);
        return new CloudBlobContainer(PathUtility.addToQuery(containerUri, sasToken));
    }

    public String getRandomFilename(String suffix) {
        StringBuilder strBuffer = new StringBuilder();
        strBuffer
            .append(ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE))
            .append(Strings.isNullOrEmpty(suffix) ? "" : "_")
            .append(Strings.isNullOrEmpty(suffix) ? "" : suffix);
        return strBuffer.toString();
    }

    public byte[] createZipArchiveWithRandomName(
        List<String> files, String metadataFile, String zipFilename
    ) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {
            for (String file : files) {
                zos.putNextEntry(new ZipEntry(file));
                zos.write(Resources.toByteArray(Resources.getResource(file)));
                zos.closeEntry();
            }

            if (metadataFile != null) {
                String metadataTemplate =
                    Resources.toString(Resources.getResource(metadataFile), StandardCharsets.UTF_8);
                String metadata = metadataTemplate.replace("$$zip_file_name$$", zipFilename);
                zos.putNextEntry(new ZipEntry("metadata.json"));
                zos.write(metadata.getBytes());
                zos.closeEntry();
            }
        }
        return outputStream.toByteArray();
    }

    public byte[] createSignedZipArchiveWithRandomName(
        List<String> files, String metadataFile, String zipFilename, String privateKeyDer
    ) throws Exception {
        byte[] zipArchive = createZipArchiveWithRandomName(files, metadataFile, zipFilename);
        byte[] signature = signWithSha256Rsa(zipArchive, privateKeyDer);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {
            zos.putNextEntry(new ZipEntry(ZipVerifiers.DOCUMENTS_ZIP));
            zos.write(zipArchive);
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry(ZipVerifiers.SIGNATURE_SIG));
            zos.write(signature);
            zos.closeEntry();
        }
        return outputStream.toByteArray();
    }

    // Create signature using SHA256/RSA.
    public byte[] signWithSha256Rsa(byte[] input, String privateKeyDer) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(privateKeyDer);

        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");

        Signature privateSignature = Signature.getInstance("SHA256withRSA");
        privateSignature.initSign(kf.generatePrivate(spec));
        privateSignature.update(input);
        return privateSignature.sign();
    }

    public EnvelopeListResponse getEnvelopes(String baseUrl, String s2sToken, Status status) {
        String url = status == null ? "/envelopes" : "/envelopes?status=" + status;

        Response response =
            RestAssured
                .given()
                .relaxedHTTPSValidation()
                .baseUri(baseUrl)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header("ServiceAuthorization", "Bearer " + s2sToken)
                .header(SyntheticHeaders.SYNTHETIC_TEST_SOURCE, "Bulk Scan Processor smoke test")
                .when()
                .get(url)
                .andReturn();

        assertThat(response.getStatusCode()).isEqualTo(200);

        return response.getBody().as(EnvelopeListResponse.class, ObjectMapperType.JACKSON_2);
    }

    public Optional<EnvelopeResponse> getEnvelopeByZipFileName(
        String baseUrl,
        String s2sToken,
        String zipFileName
    ) {
        return getEnvelopes(baseUrl, s2sToken, null)
            .envelopes
            .stream()
            .filter(env -> Objects.equals(env.getZipFileName(), zipFileName))
            .findFirst();
    }

    public EnvelopeResponse getEnvelope(String baseUrl, String s2sToken, UUID id) {
        Response response =
            RestAssured
                .given()
                .relaxedHTTPSValidation()
                .baseUri(baseUrl)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header("ServiceAuthorization", "Bearer " + s2sToken)
                .header(SyntheticHeaders.SYNTHETIC_TEST_SOURCE, "Bulk Scan Processor smoke test")
                .when()
                .get("/envelopes/" + id)
                .andReturn();

        assertThat(response.getStatusCode()).isEqualTo(200);

        return response.getBody().as(EnvelopeResponse.class, ObjectMapperType.JACKSON_2);
    }

    public void updateEnvelopeStatus(
        String baseUrl,
        String s2sToken,
        UUID envelopeId,
        Status status
    ) {
        Response resp =
            RestAssured
                .given()
                .relaxedHTTPSValidation()
                .baseUri(baseUrl)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header("ServiceAuthorization", "Bearer " + s2sToken)
                .body("{\"status\": \"" + status + "\"}")
                .when()
                .put("/envelopes/" + envelopeId + "/status")
                .andReturn();

        assertThat(resp.statusCode())
            .as("Should get success response on update")
            .isEqualTo(204);
    }
}
