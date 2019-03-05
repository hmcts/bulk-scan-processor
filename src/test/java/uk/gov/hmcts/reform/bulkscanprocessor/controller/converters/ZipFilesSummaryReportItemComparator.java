package uk.gov.hmcts.reform.bulkscanprocessor.controller.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.reports.ZipFilesSummaryReportItem;

import java.util.Comparator;

class ZipFilesSummaryReportItemComparator implements Comparator<ZipFilesSummaryReportItem> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public int compare(ZipFilesSummaryReportItem o1, ZipFilesSummaryReportItem o2) {
        try {
            return MAPPER.writeValueAsString(o1).compareTo(MAPPER.writeValueAsString(o2));
        } catch (JsonProcessingException exception) {
            throw new RuntimeException("oh no. should not happened", exception);
        }
    }
}
