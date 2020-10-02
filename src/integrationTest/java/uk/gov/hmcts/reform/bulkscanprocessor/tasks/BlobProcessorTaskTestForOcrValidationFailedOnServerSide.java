package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.google.common.io.Resources;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.reform.bulkscanprocessor.config.IntegrationTest;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.model.req.FormData;

import java.nio.charset.Charset;
import java.util.List;

import static com.google.common.io.Resources.getResource;
import static com.jayway.jsonpath.JsonPath.parse;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.CREATED;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.DirectoryZipper.zipDir;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.OCR_VALIDATION_SERVER_SIDE_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.ZIPFILE_PROCESSING_STARTED;

@IntegrationTest
@TestPropertySource(properties = {
    "scheduling.task.scan.enabled=true"
})
class BlobProcessorTaskTestForOcrValidationFailedOnServerSide extends ProcessorTestSuiteForOcrValidation {
    @Test
    void should_process_envelope_with_warnings_if_ocr_validation_returns_server_side_error() throws Exception {
        // given
        uploadToBlobStorage(SAMPLE_ZIP_FILE_NAME, zipDir("zipcontents/supplementary_evidence_with_ocr"));

        given(authTokenGenerator.generate()).willReturn("token");
        given(ocrValidationClient.validate(anyString(), any(FormData.class), anyString(), anyString()))
            .willThrow(getServerSideException())
            .willThrow(getServerSideException())
        ;

        // when
        processor.processBlobs();

        // one unsuccessful retry is allowed by ocr-validation-max-retries property ( = 1)
        retryAfterDelay();

        // after second unsuccessful retry causes - no more retries and warning raised
        retryAfterDelay();

        // then
        Envelope actualEnvelope = getSingleEnvelopeFromDb();

        String originalMetaFile = Resources.toString(
            getResource("zipcontents/supplementary_evidence_with_ocr/metadata.json"),
            Charset.defaultCharset()
        );

        assertThat(parse(actualEnvelope)).isEqualToIgnoringGivenFields(
            parse(originalMetaFile),
            "id", "amount", "amount_in_pence", "configuration", "json"
        );
        assertThat(actualEnvelope.getStatus()).isEqualTo(CREATED);
        assertThat(actualEnvelope.getScannableItems()).hasSize(1);
        assertThat(actualEnvelope.getScannableItems().get(0).getOcrValidationWarnings().length).isEqualTo(1);
        assertThat(actualEnvelope.getScannableItems().get(0).getOcrValidationWarnings()[0])
            .isEqualTo("OCR validation was not performed due to errors");

        // and
        List<ProcessEvent> processEvents = processEventRepository.findAll();
        assertThat(processEvents.stream().map(ProcessEvent::getEvent).collect(toList()))
            .containsExactlyInAnyOrder(
                ZIPFILE_PROCESSING_STARTED,
                OCR_VALIDATION_SERVER_SIDE_FAILURE,
                ZIPFILE_PROCESSING_STARTED,
                OCR_VALIDATION_SERVER_SIDE_FAILURE,
                ZIPFILE_PROCESSING_STARTED
            );

        assertThat(processEvents).allMatch(pe -> pe.getReason() == null);
    }
}
