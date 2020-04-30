package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.config.IntegrationTest;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.InvalidMessageException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.ServiceBusHelper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator.envelope;

@IntegrationTest
@RunWith(SpringRunner.class)
public class OrchestratorNotificationTaskTest {

    @Autowired private EnvelopeRepository envelopeRepo;
    @Autowired private ProcessEventRepository processEventRepo;

    @Mock private ServiceBusHelper serviceBusHelper;

    private OrchestratorNotificationTask task;

    @Before
    public void setUp() throws Exception {
        this.task = new OrchestratorNotificationTask(
            serviceBusHelper,
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

    @After
    public void tearDown() throws Exception {
        processEventRepo.deleteAll();
        envelopeRepo.deleteAll();
    }
}
