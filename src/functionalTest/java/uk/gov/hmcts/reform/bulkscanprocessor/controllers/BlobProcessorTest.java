package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import org.assertj.core.util.Strings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.EnvelopeResponse;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class BlobProcessorTest extends BaseFunctionalTest {

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void should_process_zipfile_after_upload_and_set_status() {
        List<String> files = Arrays.asList("1111006.pdf", "1111002.pdf");
        String metadataFile = "exception_with_ocr_metadata.json";
        String destZipFilename = testHelper.getRandomFilename("24-06-2018-00-00-00.test.zip");

        // valid zip file
        EnvelopeResponse envelope = uploadZipFile(files, metadataFile, destZipFilename);

        assertThat(envelope.getStatus()).isIn(Status.NOTIFICATION_SENT, Status.COMPLETED);
        assertThat(envelope.getScannableItems()).hasSize(2);
        assertThat(envelope.getScannableItems()).noneMatch(item -> Strings.isNullOrEmpty(item.documentUuid));
    }

    @Test
    public void should_process_zipfile_with_supplementary_evidence_with_oce_classification() {
        List<String> files = Collections.singletonList("1111006.pdf");
        String metadataFile = "supplementary_evidence_with_ocr_metadata.json";
        String destZipFilename = testHelper.getRandomFilename("24-06-2018-00-00-00.test.zip");
        EnvelopeResponse envelope = uploadZipFile(files, metadataFile, destZipFilename);

        assertThat(envelope.getStatus()).isIn(Status.NOTIFICATION_SENT, Status.COMPLETED);
        assertThat(envelope.getScannableItems()).hasSize(1);
        assertThat(envelope.getScannableItems()).noneMatch(item -> Strings.isNullOrEmpty(item.documentUuid));
    }

    private EnvelopeResponse uploadZipFile(List<String> files, String metadataFile, String destZipFilename) {
        // valid zip file
        testHelper.uploadZipFile(
            inputContainer,
            files,
            metadataFile,
            destZipFilename,
            testPrivateKeyDer,
            operationContext
        );

        String s2sToken = testHelper.s2sSignIn(this.s2sName, this.s2sSecret, this.s2sUrl);

        waitForFileToBeProcessed(destZipFilename, s2sToken);

        return testHelper.getEnvelopeByZipFileName(testUrl, s2sToken, destZipFilename).get();
    }
}
