package uk.gov.hmcts.reform.bulkscanprocessor.model.out.reports;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.RejectedZipFileData;

import java.util.List;

public class RejectedZipFilesResponse {

    @JsonProperty("count")
    public final int count;

    @JsonProperty("rejected_zip_files")
    public final List<RejectedZipFileData> rejectedZipFiles;

    public RejectedZipFilesResponse(int count, List<RejectedZipFileData> rejectedZipFiles) {
        this.count = count;
        this.rejectedZipFiles = rejectedZipFiles;
    }
}
