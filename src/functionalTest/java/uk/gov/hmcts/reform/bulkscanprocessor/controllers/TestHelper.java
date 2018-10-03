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
import uk.gov.hmcts.reform.logging.appinsights.SyntheticHeaders;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
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
            .print();
    }

    public void uploadZipFile(
        CloudBlobContainer container, String srcZipFilename, String destZipFilename
    ) throws Exception {
        byte[] zipFile = Resources.toByteArray(Resources.getResource(srcZipFilename));
        CloudBlockBlob blockBlobReference = container.getBlockBlobReference(destZipFilename);
        blockBlobReference.uploadFromByteArray(zipFile, 0, zipFile.length);
    }

    public void uploadZipFile(
        CloudBlobContainer container,
        List<String> files,
        String metadataFile,
        final String destZipFilename
    ) throws Exception {
        byte[] zipFile = createZipArchiveWithRandomName(files, metadataFile, destZipFilename);
        CloudBlockBlob blockBlobReference = container.getBlockBlobReference(destZipFilename);
        blockBlobReference.uploadFromByteArray(zipFile, 0, zipFile.length);
    }

    public void uploadAndLeaseZipFile(
        CloudBlobContainer container, String srcZipFilename, String destZipFilename
    ) throws Exception {
        byte[] zipFile = Resources.toByteArray(Resources.getResource(srcZipFilename));
        CloudBlockBlob blockBlobReference = container.getBlockBlobReference(destZipFilename);
        blockBlobReference.uploadFromByteArray(zipFile, 0, zipFile.length);
        blockBlobReference.acquireLease();
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
            String metadataTemplate =
                Resources.toString(Resources.getResource(metadataFile), StandardCharsets.UTF_8);
            String metadata = metadataTemplate.replace("$$zip_file_name$$", zipFilename);
            zos.putNextEntry(new ZipEntry("metadata.json"));
            zos.write(metadata.getBytes());
            zos.closeEntry();
        }
        return outputStream.toByteArray();
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

    public static boolean isMasterBranch() {
        String branch = System.getenv("BRANCH_NAME");
        if (Strings.isNullOrEmpty(branch)) {
            branch = System.getenv("CHANGE_BRANCH");
        }
        return "master".equalsIgnoreCase(branch);
    }

}
