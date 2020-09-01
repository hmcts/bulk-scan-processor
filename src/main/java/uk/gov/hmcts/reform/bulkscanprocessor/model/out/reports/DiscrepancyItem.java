package uk.gov.hmcts.reform.bulkscanprocessor.model.out.reports;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.DiscrepancyType;

public class DiscrepancyItem {
    @JsonProperty("zip_file_name")
    public final String zipFileName;

    @JsonProperty("container")
    public final String container;

    @JsonProperty("type")
    public final DiscrepancyType type;

    @JsonProperty("stated")
    public final String stated;

    @JsonProperty("actual")
    public final String actual;

    // region constructor
    public DiscrepancyItem(
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
