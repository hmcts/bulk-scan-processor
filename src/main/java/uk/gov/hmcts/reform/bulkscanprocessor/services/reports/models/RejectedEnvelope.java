package uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models;

public class RejectedEnvelope {

    public final String filename;
    public final String container;

    public RejectedEnvelope(String filename, String container) {
        this.filename = filename;
        this.container = container;
    }
}
