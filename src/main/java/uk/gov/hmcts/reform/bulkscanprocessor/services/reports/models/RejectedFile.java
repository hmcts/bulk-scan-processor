package uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models;

/**
 * Represents the rejected file.
 */
public class RejectedFile {

    public final String filename;
    public final String container;

    /**
     * Constructor for the RejectedFile.
     * @param filename The filename
     * @param container The container
     */
    public RejectedFile(String filename, String container) {
        this.filename = filename;
        this.container = container;
    }
}
