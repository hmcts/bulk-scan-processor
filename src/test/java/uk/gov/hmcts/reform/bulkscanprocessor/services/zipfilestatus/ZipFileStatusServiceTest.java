package uk.gov.hmcts.reform.bulkscanprocessor.services.zipfilestatus;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.zipfilestatus.ZipFileEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.zipfilestatus.ZipFileEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.zipfilestatus.ZipFileStatus;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import static java.time.Instant.now;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class ZipFileStatusServiceTest {

    @Mock private ProcessEventRepository eventRepo;
    @Mock private EnvelopeRepository envelopeRepo;

    private ZipFileStatusService service;

    @Before
    public void setUp() throws Exception {
        this.service = new ZipFileStatusService(eventRepo, envelopeRepo);
    }

    @Test
    public void should_return_envelopes_and_events_from_db() {
        // given
        List<ProcessEvent> events = asList(
            event(Event.DOC_UPLOADED, "A", Timestamp.from(now())),
            event(Event.DOC_PROCESSED, "A", Timestamp.from(now().minusSeconds(1))),
            event(Event.DOC_FAILURE, "B", Timestamp.from(now().minusSeconds(2)))
        );
        
        List<Envelope> envelopes = asList(
            envelope(UUID.randomUUID(), "A", Status.PROCESSED),
            envelope(UUID.randomUUID(), "B", Status.PROCESSED)
        );

        given(envelopeRepo.findByZipFileName("hello.zip")).willReturn(envelopes);
        given(eventRepo.findByZipFileName("hello.zip")).willReturn(events);

        // when
        ZipFileStatus result = service.getStatusFor("hello.zip");

        // then
        assertThat(result.envelopes)
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new ZipFileEnvelope(
                    envelopes.get(0).getId().toString(),
                    envelopes.get(0).getContainer(),
                    envelopes.get(0).getStatus().name()
                ),
                new ZipFileEnvelope(
                    envelopes.get(1).getId().toString(),
                    envelopes.get(1).getContainer(),
                    envelopes.get(1).getStatus().name()
                )
            );

        assertThat(result.events)
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new ZipFileEvent(
                    events.get(0).getEvent().name(),
                    events.get(0).getContainer(),
                    events.get(0).getCreatedAt()
                ),
                new ZipFileEvent(
                    events.get(1).getEvent().name(),
                    events.get(1).getContainer(),
                    events.get(1).getCreatedAt()
                ),
                new ZipFileEvent(
                    events.get(2).getEvent().name(),
                    events.get(2).getContainer(),
                    events.get(2).getCreatedAt()
                )
            );
    }

    @Test
    public void should_return_empty_lists_when_no_data_for_zip_file_was_found() {
        // given
        given(envelopeRepo.findByZipFileName("hello.zip")).willReturn(emptyList());
        given(eventRepo.findByZipFileName("hello.zip")).willReturn(emptyList());

        // when
        ZipFileStatus result = service.getStatusFor("hello.zip");

        // then
        assertThat(result).isNotNull();
        assertThat(result.envelopes).isNotNull().isEmpty();
        assertThat(result.events).isNotNull().isEmpty();
    }

    private Envelope envelope(UUID id, String container, Status status) {
        Envelope envelope = mock(Envelope.class);
        given(envelope.getId()).willReturn(id);
        given(envelope.getContainer()).willReturn(container);
        given(envelope.getStatus()).willReturn(status);
        return envelope;
    }

    private ProcessEvent event(Event eventType, String container, Timestamp createdAt) {
        ProcessEvent event = mock(ProcessEvent.class);
        given(event.getEvent()).willReturn(eventType);
        given(event.getContainer()).willReturn(container);
        given(event.getCreatedAt()).willReturn(createdAt);
        return event;
    }
}
