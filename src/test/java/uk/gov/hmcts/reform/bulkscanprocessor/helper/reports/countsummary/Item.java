package uk.gov.hmcts.reform.bulkscanprocessor.helper.reports.countsummary;

import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.EnvelopeCountSummaryItem;

import java.time.LocalDate;

public class Item implements EnvelopeCountSummaryItem {

    private final LocalDate date;
    private final String container;
    private final int received;
    private final int rejected;

    public Item(LocalDate date, String container, int received, int rejected) {
        this.date = date;
        this.container = container;
        this.received = received;
        this.rejected = rejected;
    }

    @Override
    public LocalDate getDate() {
        return date;
    }

    @Override
    public String getContainer() {
        return container;
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
