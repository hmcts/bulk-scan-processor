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

@Service
public class ProcessedEventsService {
    private final ProcessEventRepository processEventRepository;

    @Autowired
    public ProcessedEventsService(ProcessEventRepository processEventRepository) {
        this.processEventRepository = processEventRepository;
    }

    @Transactional(readOnly = true)
    public List<ProcessEvent> getProcessedEventsByDcnPrefix(String dcnPrefix, LocalDate fromDate, LocalDate toDate) {
        var events = processEventRepository.findEventsByDcnPrefix(dcnPrefix, fromDate, toDate);
        return events.isEmpty() ? emptyList() : ImmutableList.copyOf(events);
    }
}
