package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.google.common.io.Resources;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.reform.bulkscanprocessor.config.IntegrationTest;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.model.req.FormData;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.model.res.ValidationResponse;

import java.nio.charset.Charset;
import java.util.List;

import static com.google.common.io.Resources.getResource;
import static com.jayway.jsonpath.JsonPath.parse;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.CREATED;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.DirectoryZipper.zipDir;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.OCR_VALIDATION_SERVER_SIDE_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.ZIPFILE_PROCESSING_STARTED;
import static uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.model.res.Status.SUCCESS;

@IntegrationTest
@TestPropertySource(properties = {
    "scheduling.task.scan.enabled=true"
})
class BlobProcessorTaskTestForFailedAndRecoveredOcrValidation extends ProcessorTestSuiteForOcrValidation {
    @Test
    void should_process_envelope_without_warnings_if_ocr_validation_returns_server_side_error_and_then_passes()
        throws Exception {
        // given
        uploadToBlobStorage(SAMPLE_ZIP_FILE_NAME, zipDir("zipcontents/supplementary_evidence_with_ocr"));

        given(authTokenGenerator.generate()).willReturn("token");
        given(ocrValidationClient.validate(anyString(), any(FormData.class), anyString(), anyString()))
            .willThrow(getServerSideException())
            .willReturn(new ValidationResponse(SUCCESS, emptyList(), emptyList()))
        ;

        // when
        processor.processBlobs();

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
        assertThat(actualEnvelope.getScannableItems().get(0).getOcrValidationWarnings().length).isEqualTo(0);

        // and
        List<ProcessEvent> processEvents = processEventRepository.findAll();
        assertThat(processEvents.stream().map(ProcessEvent::getEvent).collect(toList()))
            .containsExactlyInAnyOrder(
                ZIPFILE_PROCESSING_STARTED,
                OCR_VALIDATION_SERVER_SIDE_FAILURE,
                ZIPFILE_PROCESSING_STARTED
            );
    }
}
