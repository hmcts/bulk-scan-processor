package uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models;

public class Discrepancy {
    public final String zipFileName;
    public final String container;
    public final DiscrepancyType type;
    public final String stated;
    public final String actual;

    // region constructor
    public Discrepancy(
        String zipFileName,
        String container,
        DiscrepancyType type
    ) {
        this(zipFileName, container, type, null, null);
    }
    // endregion

    // region constructor
    public Discrepancy(
        String zipFileName,
        String container,
        DiscrepancyType type,
        String stated,
        String actual
    ) {
        this.zipFileName = zipFileName;
        this.container = container;
        this.type = type;
        this.stated = stated;
        this.actual = actual;
    }
    // endregion
}
