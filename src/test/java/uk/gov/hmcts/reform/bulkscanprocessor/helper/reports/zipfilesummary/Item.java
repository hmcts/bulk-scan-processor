package uk.gov.hmcts.reform.bulkscanprocessor.helper.reports.zipfilesummary;

import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.ZipFileSummaryItem;

import java.time.Instant;

public class Item implements ZipFileSummaryItem {

    private final String zipFileName;
    private final Instant createdDate;
    private final Instant completedDate;
    private final String container;
    private final String status;

    public Item(String zipFileName,
                Instant createdDate,
                Instant completedDate,
                String container,
                String status
    ) {
        this.zipFileName = zipFileName;
        this.createdDate = createdDate;
        this.completedDate = completedDate;
        this.container = container;
        this.status = status;
    }

    @Override
    public String getZipFileName() {
        return zipFileName;
    }

    @Override
    public Instant getCreatedDate() {
        return createdDate;
    }

    @Override
    public Instant getCompletedDate() {
        return completedDate;
    }

    @Override
    public String getContainer() {
        return container;
    }

    @Override
    public String getStatus() {
        return status;
    }
}
