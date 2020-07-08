package uk.gov.hmcts.reform.bulkscanprocessor.services;

import com.microsoft.azure.storage.blob.CloudBlockBlob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class EligibilityCheckerTest {
    @Mock
    private EnvelopeProcessor envelopeProcessor;

    @Mock
    private CloudBlockBlob cloudBlockBlob;

    @Mock
    private Envelope envelope;

    private EligibilityChecker eligibilityChecker;

    @BeforeEach
    public void setUp() {
        eligibilityChecker = new EligibilityChecker(envelopeProcessor);
    }

    @Test
    void should_reject_if_envelope_already_exists() throws Exception {
        // given
        given(envelopeProcessor.getEnvelopeByFileAndContainer("cont", "file1.zip")).willReturn(envelope);

        // when
        // then
        assertThat(eligibilityChecker.isEligibleForProcessing(cloudBlockBlob, "cont", "file1.zip")).isFalse();
    }

    @Test
    void should_reject_if_blob_does_not_exist() throws Exception {
        // given
        given(envelopeProcessor.getEnvelopeByFileAndContainer("cont", "file1.zip")).willReturn(null);
        given(cloudBlockBlob.exists()).willReturn(false);

        // when
        // then
        assertThat(eligibilityChecker.isEligibleForProcessing(cloudBlockBlob, "cont", "file1.zip")).isFalse();
    }

    @Test
    void should_accept_otherwise() throws Exception {
        // given
        given(envelopeProcessor.getEnvelopeByFileAndContainer("cont", "file1.zip")).willReturn(null);
        given(cloudBlockBlob.exists()).willReturn(true);

        // when
        // then
        assertThat(eligibilityChecker.isEligibleForProcessing(cloudBlockBlob, "cont", "file1.zip")).isTrue();
    }
}