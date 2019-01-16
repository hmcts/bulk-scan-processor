package uk.gov.hmcts.reform.bulkscanprocessor.helper.reports.countsummary;

import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.EnvelopeCountSummaryItem;

import java.time.LocalDate;

public class Item implements EnvelopeCountSummaryItem {

    private final LocalDate date;
    private final String jurisdiction;
    private final int received;
    private final int rejected;

    public Item(LocalDate date, String jurisdiction, int received, int rejected) {
        this.date = date;
        this.jurisdiction = jurisdiction;
        this.received = received;
        this.rejected = rejected;
    }

    @Override
    public LocalDate getDate() {
        return date;
    }

    @Override
    public String getJurisdiction() {
        return jurisdiction;
    }

    @Override
    public int getReceived() {
        return received;
    }

    @Override
    public int getRejected() {
        return rejected;
    }
}
