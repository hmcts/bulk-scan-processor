package uk.gov.hmcts.reform.bulkscanprocessor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import com.microsoft.azure.storage.OperationContext;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.core.PathUtility;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import io.restassured.RestAssured;
import io.restassured.mapper.ObjectMapperType;
import io.restassured.response.Response;
import org.assertj.core.api.SoftAssertions;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.EnvelopeListResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.EnvelopeResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.ZipExtractor;
import uk.gov.hmcts.reform.logging.appinsights.SyntheticHeaders;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

public class TestHelper {

    private static final String TEST_SOURCE_NAME = "Bulk Scan Processor tests";
    private static final Random RANDOM = new Random();

    private static final DateTimeFormatter FILE_NAME_DATE_TIME_FORMAT =
        DateTimeFormatter.ofPattern("dd-MM-yyyy-HH-mm-ss");

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
        OperationContext operationContext
    ) {
        try {
            byte[] zipFile =
                createWrappedZipArchiveWithRandomName(files, metadataFile, destZipFilename);
            CloudBlockBlob blockBlobReference = container.getBlockBlobReference(destZipFilename);
            blockBlobReference.uploadFromByteArray(
                zipFile,
                0,
                zipFile.length,
                null,
                null,
                operationContext
            );
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    public CloudBlockBlob uploadAndLeaseZipFile(
        CloudBlobContainer container,
        List<String> files,
        String metadataFile,
        String destZipFilename
    ) throws Exception {
        byte[] zipFile = createWrappedZipArchiveWithRandomName(files, metadataFile, destZipFilename);
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
            .header(SyntheticHeaders.SYNTHETIC_TEST_SOURCE, TEST_SOURCE_NAME)
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

    public String getRandomFilename() {
        return String.format(
            "%s_%s.test.zip",
            ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE),
            LocalDateTime.now().format(FILE_NAME_DATE_TIME_FORMAT)
        );
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
                String metadata = metadataTemplate
                    .replace("$$zip_file_name$$", zipFilename)
                    .replace("$$dcn1$$", generateDcnNumber())
                    .replace("$$dcn2$$", generateDcnNumber());
                zos.putNextEntry(new ZipEntry("metadata.json"));

                zos.write(metadata.getBytes());
                zos.closeEntry();
            }
        }
        return outputStream.toByteArray();
    }

    private String generateDcnNumber() {
        return Long.toString(System.currentTimeMillis()) + Math.abs(RANDOM.nextInt());
    }

    public byte[] createWrappedZipArchiveWithRandomName(
        List<String> files, String metadataFile, String zipFilename
    ) throws Exception {
        byte[] zipArchive = createZipArchiveWithRandomName(files, metadataFile, zipFilename);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {
            zos.putNextEntry(new ZipEntry(ZipExtractor.DOCUMENTS_ZIP));
            zos.write(zipArchive);
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
                .header(SyntheticHeaders.SYNTHETIC_TEST_SOURCE, TEST_SOURCE_NAME)
                .when()
                .get(url)
                .andReturn();

        assertSuccessfulEnvelopesResponse(response);

        EnvelopeListResponse deserialisedResponse =
            response.getBody().as(EnvelopeListResponse.class, ObjectMapperType.JACKSON_2);

        assertThat(deserialisedResponse).isNotNull();

        return deserialisedResponse;
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
                .header(SyntheticHeaders.SYNTHETIC_TEST_SOURCE, TEST_SOURCE_NAME)
                .when()
                .get("/envelopes/" + id)
                .andReturn();

        assertThat(response.getStatusCode()).isEqualTo(200);

        return response.getBody().as(EnvelopeResponse.class, ObjectMapperType.JACKSON_2);
    }

    private void assertSuccessfulEnvelopesResponse(Response response) {
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.getStatusCode()).isEqualTo(200);

            try {
                response.getBody().as(EnvelopeListResponse.class, ObjectMapperType.JACKSON_2);
            } catch (Exception exc) {
                softly.fail(
                    "Expected list of envelopes in the body but got\n'%s'\nException:\n%s",
                    response.getBody().print(),
                    exc
                );
            }
        });
    }
}
