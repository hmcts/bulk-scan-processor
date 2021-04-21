package uk.gov.hmcts.reform.bulkscanprocessor.model.out.reports;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ZipFilesSummaryReportListResponse {

    @JsonProperty("total")
    public final int total;

    @JsonProperty("total_completed")
    public final int totalCompleted;

    @JsonProperty("total_failed")
    public final int totalFailed;

    @JsonProperty("exception_record")
    public final int exceptionRecord;

    @JsonProperty("auto_case_creation")
    public final int autoCaseCreation;

    @JsonProperty("auto_attached_to_case")
    public final int autoAttachedToCase;

    @JsonProperty("data")
    public final List<ZipFilesSummaryReportItem> items;

    public ZipFilesSummaryReportListResponse(List<ZipFilesSummaryReportItem> items) {
        this.total = items.size();
        this.totalCompleted = (int) items.stream().filter(
            completed -> "COMPLETED".equalsIgnoreCase(completed.envelopeStatus)).count();
        this.totalFailed = (int) items.stream().filter(
            completed -> completed.envelopeStatus != null && completed.envelopeStatus.contains("FAILURE")).count();
        this.exceptionRecord = (int) items.stream()
            .filter(exRecord -> exRecord.ccdAction.equalsIgnoreCase("EXCEPTION_RECORD")).count();
        this.autoCaseCreation = (int) items.stream()
            .filter(autoCreatedCase -> autoCreatedCase.ccdAction.equalsIgnoreCase("AUTO_CREATED_CASE")).count();
        this.autoAttachedToCase = (int) items.stream()
            .filter(autoAttachedCase -> autoAttachedCase.ccdAction.equalsIgnoreCase("AUTO_ATTACHED_TO_CASE")).count();
        this.items = items;
    }
}
