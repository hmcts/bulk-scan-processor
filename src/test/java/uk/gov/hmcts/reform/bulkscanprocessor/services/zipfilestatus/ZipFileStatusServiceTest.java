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
import uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.services.zipfilestatus.model.ZipFileStatus;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

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
            new ProcessEvent("container_A", "hello.zip", Event.DOC_UPLOADED),
            new ProcessEvent("container_A", "hello.zip", Event.DOC_PROCESSED),
            new ProcessEvent("container_B", "hello.zip", Event.DOC_UPLOADED),
            new ProcessEvent("container_B", "hello.zip", Event.DOC_PROCESSED),
            new ProcessEvent("container_C", "hello.zip", Event.DOC_FAILURE)
        );

        List<Envelope> envelopes = asList(
            EnvelopeCreator.envelope("container_A", Status.PROCESSED),
            EnvelopeCreator.envelope("container_B", Status.PROCESSED)
        );

        given(envelopeRepo.findByZipFileName("hello.zip")).willReturn(envelopes);
        given(eventRepo.findByZipFileName("hello.zip")).willReturn(events);

        // when
        ZipFileStatus result = service.getStatusFor("hello.zip");

        // then
        assertThat(result.envelopes)
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrderElementsOf(envelopes);

        assertThat(result.events)
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrderElementsOf(events);
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
}
