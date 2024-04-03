package uk.gov.hmcts.reform.bulkscanprocessor.model.out.reports;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Response for the zip files summary report.
 */
public class ZipFilesSummaryReportListResponse {

    private static final String ENVELOPE_UPLOAD_FAILURE = "UPLOAD_FAILURE";
    private static final String ENVELOPE_UPLOAD_COMPLETED = "COMPLETED";
    private static final String CCD_ACTION_EXCEPTION_RECORD = "EXCEPTION_RECORD";
    private static final String CCD_ACTION_AUTO_CREATED_CASE = "AUTO_CREATED_CASE";
    private static final String CCD_ACTION_AUTO_ATTACHED_TO_CASE = "AUTO_ATTACHED_TO_CASE";

    @JsonProperty("total")
    public final int total;

    @JsonProperty("total_completed")
    public final int totalCompleted;

    @JsonProperty("total_failed")
    public final int totalFailed;

    @JsonProperty("exception_record")
    public final Long exceptionRecord;

    @JsonProperty("auto_created_case")
    public final Long autoCreatedCase;

    @JsonProperty("auto_attached_to_case")
    public final Long autoAttachedToCase;

    @JsonProperty("data")
    public final List<ZipFilesSummaryReportItem> items;

    /**
     * Constructor for ZipFilesSummaryReportListResponse.
     * @param items list of items
     */
    public ZipFilesSummaryReportListResponse(List<ZipFilesSummaryReportItem> items) {
        this.total = items.size();
        this.items = items;

        Map<String, Long> envelopeStatusCount = items.stream()
            .filter(item -> item.envelopeStatus != null)
            .collect(Collectors.groupingBy(item -> item.envelopeStatus, Collectors.counting()));
        this.totalCompleted = envelopeStatusCount.containsKey(ENVELOPE_UPLOAD_COMPLETED)
            ? Math.toIntExact(envelopeStatusCount.get(ENVELOPE_UPLOAD_COMPLETED)) : 0;
        this.totalFailed = envelopeStatusCount.containsKey(ENVELOPE_UPLOAD_FAILURE)
            ? Math.toIntExact(envelopeStatusCount.get(ENVELOPE_UPLOAD_FAILURE)) : 0;

        Map<String, Long> ccdActionCount = items.stream()
            .filter(envelope -> envelope.ccdAction != null)
            .collect(Collectors.groupingBy(envelope -> envelope.ccdAction, Collectors.counting()));
        this.exceptionRecord = ccdActionCount.containsKey(CCD_ACTION_EXCEPTION_RECORD)
            ? ccdActionCount.get(CCD_ACTION_EXCEPTION_RECORD) : 0;
        this.autoCreatedCase = ccdActionCount.containsKey(CCD_ACTION_AUTO_CREATED_CASE)
            ? ccdActionCount.get(CCD_ACTION_AUTO_CREATED_CASE) : 0;
        this.autoAttachedToCase = ccdActionCount.containsKey(CCD_ACTION_AUTO_ATTACHED_TO_CASE)
            ? ccdActionCount.get(CCD_ACTION_AUTO_ATTACHED_TO_CASE) : 0;
    }
}
