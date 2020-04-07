package uk.gov.hmcts.reform.bulkscanprocessor.model.out.reports;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.RejectedFile;

import java.util.List;

public class RejectedEnvelopesResponse {

    @JsonProperty("count")
    public final int count;

    @JsonProperty("rejected_envelopes")
    public final List<RejectedFile> rejectedFiles;

    public RejectedEnvelopesResponse(int count, List<RejectedFile> rejectedFiles) {
        this.count = count;
        this.rejectedFiles = rejectedFiles;
    }
}
