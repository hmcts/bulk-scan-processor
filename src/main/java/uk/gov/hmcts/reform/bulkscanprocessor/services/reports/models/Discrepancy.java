package uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models;

/**
 * Represents a discrepancy in the data.
 */
public class Discrepancy {
    public final String zipFileName;
    public final String container;
    public final DiscrepancyType type;
    public final String stated;
    public final String actual;

    /**
     * Constructor for the Discrepancy.
     * @param zipFileName The name of the zip file
     * @param container The container
     * @param type The type of discrepancy
     */
    public Discrepancy(
        String zipFileName,
        String container,
        DiscrepancyType type
    ) {
        this(zipFileName, container, type, null, null);
    }

    /**
     * Constructor for the Discrepancy.
     * @param zipFileName The name of the zip file
     * @param container The container
     * @param type The type of discrepancy
     * @param stated The stated value
     * @param actual The actual value
     */
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
}
