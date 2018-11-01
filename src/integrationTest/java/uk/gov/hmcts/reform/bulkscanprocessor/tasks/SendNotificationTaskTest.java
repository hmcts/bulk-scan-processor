package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.services.servicebus.ServiceBusHelper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator.envelope;

@SpringBootTest
@RunWith(SpringRunner.class)
public class SendNotificationTaskTest {

    @Autowired private EnvelopeRepository envelopeRepo;
    @Autowired private ProcessEventRepository processEventRepo;

    @Mock private ServiceBusHelper serviceBusHelper;

    private SendNotificationTask task;

    @Before
    public void setUp() throws Exception {
        this.task = new SendNotificationTask(
            serviceBusHelper,
            envelopeRepo,
            processEventRepo
        );
    }

    @Test
    public void should_update_envelope_status_and_events_after_sending_notification() {
        // given
        Envelope envelopeInDb = envelopeRepo.save(envelope("some_jurisdiction", Status.PROCESSED));

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
}
