package uk.gov.hmcts.reform.bulkscanprocessor.model.out;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.zipfilestatus.ZipFileStatus;

public class CcdIdStatus {

    @JsonProperty("ccdid")
    public final String ccdId;

    @JsonProperty("zip_file_status")
    public final ZipFileStatus zipFileStatus;

    // region constructor
    public CcdIdStatus(String ccdId, ZipFileStatus zipFileStatus) {
        this.ccdId = ccdId;
        this.zipFileStatus = zipFileStatus;
    }
    // endregion
}
