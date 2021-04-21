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
        this.items = items;
        int totalCompletedCount = 0, totalFailedCount = 0, exceptionRecordCount = 0;
        int autoCaseCreationCount = 0, autoAttachedToCaseCount = 0;

        for (var item : items)
        {
            if("COMPLETED".equalsIgnoreCase(item.envelopeStatus)){
                totalCompletedCount += totalCompletedCount;
            }
            if(item.envelopeStatus != null && item.envelopeStatus.contains("FAILURE")){
                totalFailedCount += totalFailedCount;
            }
            if(item.ccdAction.equalsIgnoreCase("EXCEPTION_RECORD")){
                exceptionRecordCount += exceptionRecordCount;
            }
            if(item.ccdAction.equalsIgnoreCase("AUTO_CREATED_CASE")){
                autoCaseCreationCount += autoCaseCreationCount;
            }
            if(item.ccdAction.equalsIgnoreCase("AUTO_ATTACHED_TO_CASE")){
                autoAttachedToCaseCount += autoAttachedToCaseCount;
            }
        }

        this.totalCompleted = totalCompletedCount;
        this.totalFailed = totalFailedCount;
        this.exceptionRecord = exceptionRecordCount;
        this.autoCaseCreation = autoCaseCreationCount;
        this.autoAttachedToCase = autoAttachedToCaseCount;
    }
}
