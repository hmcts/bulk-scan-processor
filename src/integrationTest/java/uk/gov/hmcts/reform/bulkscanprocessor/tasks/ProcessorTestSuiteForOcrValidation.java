package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.HttpServerErrorException;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.bulkscanprocessor.config.IntegrationTest;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.OcrValidationClient;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.ServiceBusHelper;
import uk.gov.hmcts.reform.bulkscanprocessor.services.storage.OcrValidationRetryManager;
import uk.gov.hmcts.reform.bulkscanprocessor.validation.OcrPresenceValidator;
import uk.gov.hmcts.reform.bulkscanprocessor.validation.OcrValidator;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@IntegrationTest
@TestPropertySource(properties = {
    "scheduling.task.scan.enabled=true"
})
abstract class ProcessorTestSuiteForOcrValidation extends ProcessorTestSuite {
    @Autowired
    BlobProcessorTask processor;

    @Autowired
    OcrValidationRetryManager ocrValidationRetryManager;

    @Autowired
    OcrPresenceValidator ocrPresenceValidator;

    @MockBean
    AuthTokenGenerator authTokenGenerator;

    @MockBean
    OcrValidationClient ocrValidationClient;

    OcrValidator ocrValidator;

    @BeforeEach
    public void setUp() {
        ocrValidator = new OcrValidator(
            ocrValidationClient,
            ocrPresenceValidator,
            containerMappings,
            authTokenGenerator,
            ocrValidationRetryManager
        );

        super.setUp();
    }

    @NotNull
    HttpServerErrorException getServerSideException() {
        return HttpServerErrorException.create(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "internal server error message",
            HttpHeaders.EMPTY,
            null,
            null
        );
    }

    private void assertNoEnvelopesInDb() {
        // We expect only one envelope which was uploaded
        List<Envelope> envelopes = envelopeRepository.findAll();
        assertThat(envelopes).hasSize(0);
    }

    private void doSleep(long l) {
        try {
            Thread.sleep(l);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    void retryAfterDelay() {
        assertNoEnvelopesInDb();

        doSleep(5000L);

        processor.processBlobs();
    }

    @TestConfiguration
    public static class MockConfig {

        @Bean(name = "notifications-helper")
        public ServiceBusHelper notificationsQueueHelper() {
            return mock(ServiceBusHelper.class);
        }
    }
}
