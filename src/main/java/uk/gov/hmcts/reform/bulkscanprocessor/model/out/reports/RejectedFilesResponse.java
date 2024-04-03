package uk.gov.hmcts.reform.bulkscanprocessor.model.out.reports;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.RejectedFile;

import java.util.List;

/**
 * Response for rejected files report.
 */
public class RejectedFilesResponse {

    @JsonProperty("count")
    public final int count;

    @JsonProperty("rejected_files")
    public final List<RejectedFile> rejectedFiles;

    /**
     * Constructor for RejectedFilesResponse.
     * @param count count of rejected files
     * @param rejectedFiles list of rejected files
     */
    public RejectedFilesResponse(int count, List<RejectedFile> rejectedFiles) {
        this.count = count;
        this.rejectedFiles = rejectedFiles;
    }
}
