package uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models;

public class RejectedFile {

    public final String filename;
    public final String container;

    public RejectedFile(String filename, String container) {
        this.filename = filename;
        this.container = container;
    }
}
