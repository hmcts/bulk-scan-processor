package uk.gov.hmcts.reform.bulkscanprocessor.services.reports;

import com.google.common.collect.ImmutableList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;

import java.time.LocalDate;
import java.util.List;

import static java.util.Collections.emptyList;
/**
 * Service to handle process events.
 */
@Service
public class ProcessEventsService {
    private final ProcessEventRepository processEventRepository;

    /**
     * Constructor for the ProcessEventsService.
     * @param processEventRepository The repository for process event
     */
    @Autowired
    public ProcessEventsService(ProcessEventRepository processEventRepository) {
        this.processEventRepository = processEventRepository;
    }

    /**
     * Get the process events by DCN prefix.
     * @param dcnPrefix The DCN prefix
     * @param fromDate The from date
     * @param toDate The to date
     * @return The process events
     */
    @Transactional(readOnly = true)
    public List<ProcessEvent> getProcessEventsByDcnPrefix(String dcnPrefix, LocalDate fromDate, LocalDate toDate) {
        var events = processEventRepository.findEventsByDcnPrefix(dcnPrefix, fromDate, toDate);
        return events.isEmpty() ? emptyList() : ImmutableList.copyOf(events);
    }
}
