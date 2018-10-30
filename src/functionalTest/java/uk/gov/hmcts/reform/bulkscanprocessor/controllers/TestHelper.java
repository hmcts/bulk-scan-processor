package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import com.microsoft.azure.storage.blob.AnonymousCredentials;
import com.microsoft.azure.storage.blob.BlockBlobURL;
import com.microsoft.azure.storage.blob.ContainerURL;
import com.microsoft.azure.storage.blob.PipelineOptions;
import com.microsoft.azure.storage.blob.ServiceURL;
import com.microsoft.azure.storage.blob.SharedKeyCredentials;
import com.microsoft.azure.storage.blob.StorageURL;
import com.microsoft.azure.storage.blob.models.BlockBlobUploadResponse;
import com.microsoft.rest.v2.Context;
import com.microsoft.rest.v2.http.HttpPipeline;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.restassured.RestAssured;
import io.restassured.mapper.ObjectMapperType;
import io.restassured.response.Response;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.EnvelopeListResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.EnvelopeResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.ZipVerifiers;
import uk.gov.hmcts.reform.bulkscanprocessor.util.AzureStorageHelper;
import uk.gov.hmcts.reform.logging.appinsights.SyntheticHeaders;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

public class TestHelper {

    public static final String TEST_PRIVATE_KEY_DER = "test_private_key.der";
    private final ServiceURL serviceURL;

    public TestHelper(final String accountName, final String accountKey, final String accountUrl) throws InvalidKeyException, MalformedURLException {
        SharedKeyCredentials credential = new SharedKeyCredentials(
            accountName,
            accountKey
        );

        HttpPipeline pipeline = StorageURL.createPipeline(credential, new PipelineOptions());

        URL url = new URL(accountUrl);
        serviceURL = new ServiceURL(url, pipeline);
    }

    public ServiceURL getServiceURL() {
        return serviceURL;
    }

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

    public Single<BlockBlobUploadResponse> uploadZipFile(
        ContainerURL container,
        List<String> files,
        String metadataFile,
        final String destZipFilename
    ) throws Exception {
        byte[] zipFile =
            createSignedZipArchiveWithRandomName(files, metadataFile, destZipFilename, TEST_PRIVATE_KEY_DER);

        BlockBlobURL blockBlobURL = container.createBlockBlobURL(destZipFilename);
        ByteBuffer wrapped = ByteBuffer.wrap(zipFile);
        Flowable<ByteBuffer> just = Flowable.just(wrapped);
        return blockBlobURL
            .upload(just, zipFile.length, null, null, null, Context.NONE);
    }

    public String uploadAndLeaseZipFile(
        ContainerURL container,
        List<String> files,
        String metadataFile,
        String destZipFilename
    ) throws Exception {
        BlockBlobURL blockBlobURL = container.createBlockBlobURL(destZipFilename);

        return uploadZipFile(container, files, metadataFile, destZipFilename)
            .flatMap(r -> blockBlobURL.acquireLease(null, -1, null, Context.NONE))
            .flatMap(r -> Single.just(r.headers().leaseId()))
            .blockingGet();
    }

    public boolean storageHasFile(ContainerURL container, String fileName) {
        AzureStorageHelper azureStorageHelper = new AzureStorageHelper(serviceURL);

        return azureStorageHelper.listBlobsLazy(container)
            .any(blobItem -> blobItem.name().contains(fileName))
            .blockingGet();

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

    /**
     * Used for testing with a SAS token
     */
    public ServiceURL getAnonymousAccessServiceURL(String containerUrl) throws Exception {
        URL url = new URL(containerUrl);
        HttpPipeline pipeline = ServiceURL.createPipeline(new AnonymousCredentials(), new PipelineOptions());

        return new ServiceURL(url, pipeline);
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
        List<String> files, String metadataFile, String zipFilename, String privateKeyFilename
    ) throws Exception {
        byte[] zipArchive = createZipArchiveWithRandomName(files, metadataFile, zipFilename);
        byte[] signature = signWithSha256Rsa(zipArchive, privateKeyFilename);
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
    public byte[] signWithSha256Rsa(byte[] input, String privateKeyFilename) throws Exception {
        byte[] keyBytes = Resources.toByteArray(Resources.getResource(privateKeyFilename));
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
