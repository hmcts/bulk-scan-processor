//package uk.gov.hmcts.reform.bulkscanprocessor.controllers;
//
//import org.assertj.core.util.Strings;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
//
//import java.util.Collections;
//import java.util.List;
//
//import static java.util.Arrays.asList;
//import static org.assertj.core.api.Assertions.assertThat;
//
//public class BlobProcessorTest extends BaseFunctionalTest {
//
//    @BeforeEach
//    public void setUp() throws Exception {
//        super.setUp();
//    }
//
//    @Test
//    public void should_process_zipfile_after_upload_and_set_status() {
//        List<String> files = asList("1111006.pdf", "1111002.pdf");
//        String metadataFile = "exception_with_ocr_metadata.json";
//        String destZipFilename = testHelper.getRandomFilename();
//
//        // valid zip file
//        uploadZipFile(files, metadataFile, destZipFilename);
//        var envelope = waitForEnvelopeToBeInStatus(
//            destZipFilename,
//            asList(Status.NOTIFICATION_SENT, Status.COMPLETED)
//        );
//
//        assertThat(envelope.getScannableItems()).hasSize(2);
//        assertThat(envelope.getScannableItems()).noneMatch(item -> Strings.isNullOrEmpty(item.documentUuid));
//    }
//
//    @Test
//    public void should_process_zipfile_with_supplementary_evidence_with_ocr_classification() {
//        List<String> files = Collections.singletonList("1111006.pdf");
//        String metadataFile = "supplementary_evidence_with_ocr_metadata.json";
//        String destZipFilename = testHelper.getRandomFilename();
//
//        uploadZipFile(files, metadataFile, destZipFilename);
//        var envelope = waitForEnvelopeToBeInStatus(
//            destZipFilename,
//            asList(Status.NOTIFICATION_SENT, Status.COMPLETED)
//        );
//
//        assertThat(envelope.getScannableItems()).hasSize(1);
//        assertThat(envelope.getScannableItems()).noneMatch(item -> Strings.isNullOrEmpty(item.documentUuid));
//    }
//}
