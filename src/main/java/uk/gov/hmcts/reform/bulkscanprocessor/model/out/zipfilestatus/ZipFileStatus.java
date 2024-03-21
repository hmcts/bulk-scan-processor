package uk.gov.hmcts.reform.bulkscanprocessor.model.out.zipfilestatus;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Represents the status of a zip file.
 */
public class ZipFileStatus {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("file_name")
    public final String fileName;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("ccd_id")
    public final String ccdId;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("dcn")
    public final String dcn;

    @JsonProperty("envelopes")
    public final List<ZipFileEnvelope> envelopes;

    @JsonProperty("events")
    public final List<ZipFileEvent> events;

    /**
     * @param fileName name of the zip file
     * @param ccdId CCD ID of the zip file
     * @param dcn DCN of the zip file
     * @param envelopes envelopes in the zip file
     * @param events events that happened to the zip file
     */
    public ZipFileStatus(
        String fileName,
        String ccdId,
        String dcn,
        List<ZipFileEnvelope> envelopes,
        List<ZipFileEvent> events

    ) {
        this.fileName = fileName;
        this.ccdId = ccdId;
        this.dcn = dcn;
        this.envelopes = envelopes;
        this.events = events;
    }
}
