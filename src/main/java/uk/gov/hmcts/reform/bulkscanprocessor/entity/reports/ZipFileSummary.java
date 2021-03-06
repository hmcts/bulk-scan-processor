package uk.gov.hmcts.reform.bulkscanprocessor.entity.reports;

import java.time.Instant;

public interface ZipFileSummary {

    String getZipFileName();

    Instant getCreatedDate();

    Instant getCompletedDate();

    String getContainer();

    String getLastEventStatus();

    String getEnvelopeStatus();

    String getClassification();

    String getCcdId();

    String getCcdAction();

    String getEnvelopeId();
}
