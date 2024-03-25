package uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents the rejected zip file data.
 */
public class RejectedZipFileData {
    @JsonProperty("zip_file_name")
    private final String zipFileName;

    @JsonProperty("container")
    private final String container;

    @JsonProperty("processing_started_date_time")
    private final LocalDateTime processingStartedDateTime;

    @JsonProperty("envelope_id")
    private final UUID envelopeId;

    @JsonProperty("event")
    private final String event;

    /**
     * Constructor for the RejectedZipFileData.
     * @param zipFileName The zip file name
     * @param container The container
     * @param processingStartedDateTime The processing started date time
     * @param envelopeId The envelope id
     * @param event The event
     */
    public RejectedZipFileData(
            String zipFileName,
            String container,
            LocalDateTime processingStartedDateTime,
            UUID envelopeId,
            String event
    ) {
        this.zipFileName = zipFileName;
        this.container = container;
        this.processingStartedDateTime = processingStartedDateTime;
        this.envelopeId = envelopeId;
        this.event = event;
    }
}
