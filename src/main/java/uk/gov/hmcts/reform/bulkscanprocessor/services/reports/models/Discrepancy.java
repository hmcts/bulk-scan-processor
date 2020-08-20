package uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Discrepancy {
    @JsonProperty("zip_file_name")
    public final String zipFileName;

    @JsonProperty("container")
    public final String container;

    @JsonProperty("type")
    public final String type;

    @JsonProperty("stated")
    public final String stated;

    @JsonProperty("actual")
    public final String actual;

    public Discrepancy(
        String zipFileName,
        String container,
        String type,
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
