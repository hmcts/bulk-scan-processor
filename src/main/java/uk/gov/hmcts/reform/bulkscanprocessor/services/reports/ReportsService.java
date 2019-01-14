package uk.gov.hmcts.reform.bulkscanprocessor.services.reports;

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class ReportsService {

    public EnvelopeCountSummary getCountFor(LocalDate date) {
        throw new NotImplementedException("Not yet implemented");
    }
}
