package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.reform.bulkscanprocessor.config.IntegrationTest;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.InvalidMessageException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.services.OrchestratorNotificationService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.ServiceBusHelper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator.envelope;

@IntegrationTest
@TestPropertySource(properties = "scheduling.task.notifications_to_orchestrator.enabled=true")
public class OrchestratorNotificationTaskTest {

    @Autowired private EnvelopeRepository envelopeRepo;
    @Autowired private ProcessEventRepository processEventRepo;

    @Autowired
    private OrchestratorNotificationService orchestratorNotificationService;

    @MockBean
    private ServiceBusHelper serviceBusHelper;

    private OrchestratorNotificationTask task;

    @BeforeEach
    public void setUp() throws Exception {

        task = new OrchestratorNotificationTask(
            orchestratorNotificationService,
            envelopeRepo,
            processEventRepo
        );
    }

    @Test
    public void should_update_envelope_status_and_events_after_sending_notification() {
        // given
        Envelope envelopeInDb = envelopeRepo.saveAndFlush(envelope("some_jurisdiction", Status.UPLOADED));

        // when
        task.run();

        // then
        Envelope envelopeAfterTaskRun = envelopeRepo.getOne(envelopeInDb.getId());
        List<ProcessEvent> events = processEventRepo.findAll();

        assertThat(envelopeAfterTaskRun.getStatus()).isEqualTo(Status.NOTIFICATION_SENT);
        assertThat(events)
            .hasOnlyOneElementSatisfying(event -> {
                assertThat(event.getZipFileName()).isEqualTo(envelopeInDb.getZipFileName());
                assertThat(event.getEvent()).isEqualTo(Event.DOC_PROCESSED_NOTIFICATION_SENT);
            });
    }

    @Test
    public void should_not_update_envelope_and_create_an_event_if_sending_notification_failed() {
        // given
        Envelope envelopeInDb = envelopeRepo.saveAndFlush(envelope("some_jurisdiction", Status.UPLOADED));

        doThrow(InvalidMessageException.class)
            .when(serviceBusHelper).sendMessage(any());

        // when
        task.run();

        // then
        Envelope envelopeAfterTaskRun = envelopeRepo.getOne(envelopeInDb.getId());
        List<ProcessEvent> events = processEventRepo.findAll();

        assertThat(envelopeAfterTaskRun.getStatus())
            .isEqualTo(Status.UPLOADED); // status still the same.

        assertThat(events)
            .hasOnlyOneElementSatisfying(event -> {
                assertThat(event.getZipFileName()).isEqualTo(envelopeInDb.getZipFileName());
                assertThat(event.getEvent()).isEqualTo(Event.DOC_PROCESSED_NOTIFICATION_FAILURE);
            });

    }

    @AfterEach
    public void tearDown() {
        processEventRepo.deleteAll();
        envelopeRepo.deleteAll();
    }

    @TestConfiguration
    public static class MockConfig {

        @Bean(name = "envelopes-helper")
        public ServiceBusHelper envelopesQueueHelper() {
            return mock(ServiceBusHelper.class);
        }
    }
}
