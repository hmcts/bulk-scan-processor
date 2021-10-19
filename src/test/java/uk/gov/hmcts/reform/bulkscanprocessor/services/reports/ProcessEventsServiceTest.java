package uk.gov.hmcts.reform.bulkscanprocessor.services.reports;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;

import java.time.LocalDate;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ProcessEventsServiceTest {
    private ProcessEventsService processEventsService;

    @Mock
    private ProcessEventRepository processEventRepository;

    @BeforeEach
    public void setUp() {
        processEventsService = new ProcessEventsService(processEventRepository);
    }

    @Test
    void should_return_emptyList_when_no_events_exist_for_dcn_for_given_dates() {
        // given
        LocalDate fromDate = LocalDate.now().minusDays(3);
        LocalDate toDate = LocalDate.now();
        String dcnPrefix = "21034040";
        given(processEventRepository.findEventsByDcnPrefix(dcnPrefix, fromDate, toDate)).willReturn(emptyList());

        // when
        List<ProcessEvent> events = processEventsService.getProcessEventsByDcnPrefix(dcnPrefix, fromDate, toDate);

        // then
        assertThat(events).isEmpty();
    }

    @Test
    void should_return_envelopeList_when_envelope_exists_for_dcn_for_given_dates() {
        // given
        String dcnPrefix = "21034040";
        var e1 = new ProcessEvent(
                "A",
                "2103404021051_2021-02-03-13-11-17.zip",
                Event.FILE_VALIDATION_FAILURE
        );
        var e2 = new ProcessEvent(
                "A",
                "2103404021051_2021-02-03-13-11-17.zip",
                Event.ZIPFILE_PROCESSING_STARTED
        );
        LocalDate fromDate = LocalDate.now().minusDays(3);
        LocalDate toDate = LocalDate.now();
        given(processEventRepository.findEventsByDcnPrefix(dcnPrefix, fromDate, toDate))
                .willReturn(asList(e2, e1));

        // when
        List<ProcessEvent> events = processEventsService.getProcessEventsByDcnPrefix(dcnPrefix, fromDate, toDate);

        // then
        assertThat(events).isEqualTo(asList(e2, e1));
    }
}