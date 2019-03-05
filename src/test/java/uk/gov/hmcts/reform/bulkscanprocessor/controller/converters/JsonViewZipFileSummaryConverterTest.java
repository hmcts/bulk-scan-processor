package uk.gov.hmcts.reform.bulkscanprocessor.controller.converters;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.reports.ZipFilesSummaryReportItem;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.reports.ZipFilesSummaryReportListResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.ZipFileSummaryResponse;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.bulkscanprocessor.controllers.converters.JsonViewZipFileSummaryConverter.convert;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.CONSUMED;

public class JsonViewZipFileSummaryConverterTest {

    private static final ZipFilesSummaryReportItemComparator COMPARATOR = new ZipFilesSummaryReportItemComparator();

    @Test
    public void should_return_json_response_entity() {
        // given
        LocalDate localDate = LocalDate.of(2019, 1, 14);
        LocalTime localTime = LocalTime.of(12, 30, 10, 0);

        ZipFileSummaryResponse response = new ZipFileSummaryResponse(
            "test.zip",
            localDate,
            localTime,
            localDate,
            localTime.plusHours(1),
            "BULKSCAN",
            CONSUMED.toString()
        );

        // when
        ResponseEntity<?> entity = convert(ImmutableList.of(response));

        // then
        assertThat(entity.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON_UTF8);
        assertThat(((ZipFilesSummaryReportListResponse) entity.getBody()).items)
            .usingElementComparator(COMPARATOR)
            .contains(new ZipFilesSummaryReportItem(
                response.fileName,
                response.dateReceived,
                response.timeReceived,
                response.dateProcessed,
                response.timeProcessed,
                response.jurisdiction,
                response.status
            ));
    }
}
