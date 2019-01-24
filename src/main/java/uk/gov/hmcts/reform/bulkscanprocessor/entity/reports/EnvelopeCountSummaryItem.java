package uk.gov.hmcts.reform.bulkscanprocessor.entity.reports;

import java.time.LocalDate;

public interface EnvelopeCountSummaryItem {

    LocalDate getDate();

    String getContainer();

    int getReceived();

    int getRejected();
}
