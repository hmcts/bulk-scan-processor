package uk.gov.hmcts.reform.bulkscanprocessor.entity.reports;

import java.time.Instant;

public interface ZipFileSummary {

    String getZipFileName();

    Instant getCreatedDate();

    Instant getCompletedDate();

    String getContainer();

    String getStatus();
}
