package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import com.google.common.io.Resources;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.bulkscanprocessor.config.TestClockProvider;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.reports.EnvelopeCountSummaryReportItem;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.reports.EnvelopeCountSummaryReportListResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.ReceivedPaymentItem;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.ReceivedPaymentsService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.ReceivedScannableItemItem;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.ReceivedScannableItemPerDocumentTypeItem;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.ReceivedScannableItemsService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.ReconciliationService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.RejectedFilesReportService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.RejectedZipFileItem;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.RejectedZipFilesService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.ReportsService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.Discrepancy;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.DiscrepancyType;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.EnvelopeCountSummary;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.ReconciliationStatement;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.RejectedFile;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.ZipFileSummaryResponse;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import javax.validation.ClockProvider;

import static com.google.common.io.Resources.getResource;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.COMPLETED;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.CREATED;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.NOTIFICATION_SENT;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.UPLOADED;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.UPLOAD_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification.EXCEPTION;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification.NEW_APPLICATION;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification.SUPPLEMENTARY_EVIDENCE;
import static uk.gov.hmcts.reform.bulkscanprocessor.util.TimeZones.EUROPE_LONDON_ZONE_ID;

@WebMvcTest(ReportsController.class)
@Import(TestClockProvider.class)
class ReportsControllerTest {

    @MockBean
    private ReportsService reportsService;

    @MockBean
    private RejectedFilesReportService rejectedFilesReportService;

    @MockBean
    private ReconciliationService reconciliationService;

    @MockBean
    private RejectedZipFilesService rejectedZipFilesService;

    @MockBean
    private ReceivedScannableItemsService receivedScannableItemsService;

    @MockBean
    private ReceivedPaymentsService receivedPaymentsService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClockProvider clockProvider;

    @Test
    void should_return_result_generated_by_the_service() throws Exception {

        Instant fiveAM = ZonedDateTime.now(EUROPE_LONDON_ZONE_ID).withHour(5).toInstant();
        givenTheRequestWasMadeAt(fiveAM);

        final EnvelopeCountSummary countSummaryOne = new EnvelopeCountSummary(
            152, 11, "container1", LocalDate.of(2021, 3, 4)
        );
        final EnvelopeCountSummary countSummaryTwo = new EnvelopeCountSummary(
            178, 13, "container2", LocalDate.of(2021, 3, 4)
        );
        List<EnvelopeCountSummary> envelopeCountSummaryList = new ArrayList<>();
        envelopeCountSummaryList.add(countSummaryOne);
        envelopeCountSummaryList.add(countSummaryTwo);
        given(reportsService.getCountFor(LocalDate.of(2021, 3, 4), false))
            .willReturn(envelopeCountSummaryList);

        EnvelopeCountSummaryReportListResponse response = new EnvelopeCountSummaryReportListResponse(
            envelopeCountSummaryList.stream()
                .map(item -> new EnvelopeCountSummaryReportItem(
                    item.received,
                    item.rejected,
                    item.container,
                    item.date
                ))
                .collect(toList()),
            clockProvider
        );

        String expectedTimestamp = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(EUROPE_LONDON_ZONE_ID)
            .format(fiveAM);

        assertThat(response.timeStamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                       .equals(expectedTimestamp));
        mockMvc
            .perform(get("/reports/count-summary?date=2021-03-04"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total_received").value(response.totalReceived))
            .andExpect(jsonPath("$.total_rejected").value(response.totalRejected))
            .andExpect(jsonPath("$.time_stamp").value(expectedTimestamp))
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].received").value(response.items.get(0).received))
            .andExpect(jsonPath("$.data[0].rejected").value(response.items.get(0).rejected))
            .andExpect(jsonPath("$.data[0].container").value(response.items.get(0).container))
            .andExpect(jsonPath("$.data[0].date").value(response.items.get(0).date.toString()));
    }

    @Test
    void should_not_include_test_container_by_default() throws Exception {
        mockMvc.perform(get("/reports/count-summary?date=2019-01-14"));
        verify(reportsService).getCountFor(LocalDate.of(2019, 1, 14), false);
    }

    @Test
    void should_include_test_container_if_requested_by_the_client() throws Exception {
        mockMvc.perform(get("/reports/count-summary?date=2019-01-14&include-test=true"));

        verify(reportsService).getCountFor(LocalDate.of(2019, 1, 14), true);
    }

    @Test
    void should_not_include_test_container_if_exlicitly_not_requested_by_the_client() throws Exception {
        mockMvc.perform(get("/reports/count-summary?date=2019-01-14&include-test=false"));

        verify(reportsService).getCountFor(LocalDate.of(2019, 1, 14), false);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/reports/count-summary?date=",
        "/reports/envelopes-count-summary?date=",
        "/reports/count-summary-report?date=",
        "/reports/zip-files-summary?date="
    })
    void should_return_400_if_date_is_invalid(String uri) throws Exception {
        final String invalidDate = "2019-14-14";

        mockMvc
            .perform(get(uri + invalidDate))
            .andExpect(status().isBadRequest());
    }

    @Test
    void summary_report_should_return_result_generated_by_the_service() throws Exception {

        final EnvelopeCountSummary countSummaryOne = new EnvelopeCountSummary(
            152, 11, "container1", LocalDate.of(2021, 3, 4)
        );
        final EnvelopeCountSummary countSummaryTwo = new EnvelopeCountSummary(
            178, 13, "container2", LocalDate.of(2021, 3, 4)
        );
        List<EnvelopeCountSummary> envelopeCountSummaryList = new ArrayList<>();
        envelopeCountSummaryList.add(countSummaryOne);
        envelopeCountSummaryList.add(countSummaryTwo);
        given(reportsService.getSummaryCountFor(LocalDate.of(2021, 3, 4), false))
            .willReturn(envelopeCountSummaryList);

        EnvelopeCountSummaryReportListResponse response = new EnvelopeCountSummaryReportListResponse(
            envelopeCountSummaryList.stream()
                .map(item -> new EnvelopeCountSummaryReportItem(
                    item.received,
                    item.rejected,
                    item.container,
                    item.date
                ))
                .collect(toList()),
            clockProvider
        );

        mockMvc
            .perform(get("/reports/count-summary-report?date=2021-03-04"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total_received").value(response.totalReceived))
            .andExpect(jsonPath("$.total_rejected").value(response.totalRejected))
            .andExpect(jsonPath("$.time_stamp").value(
                response.timeStamp.format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))))
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].received").value(response.items.get(0).received))
            .andExpect(jsonPath("$.data[0].rejected").value(response.items.get(0).rejected))
            .andExpect(jsonPath("$.data[0].container").value(response.items.get(0).container))
            .andExpect(jsonPath("$.data[0].date").value(response.items.get(0).date.toString()));
    }

    @Test
    void summary_report_should_not_include_test_container_by_default() throws Exception {
        mockMvc.perform(get("/reports/count-summary-report?date=2019-01-14"));
        verify(reportsService).getSummaryCountFor(LocalDate.of(2019, 1, 14), false);
    }

    @Test
    void summary_report_should_include_test_container_if_requested_by_the_client() throws Exception {
        mockMvc.perform(get("/reports/count-summary-report?date=2019-01-14&include-test=true"));

        verify(reportsService).getSummaryCountFor(LocalDate.of(2019, 1, 14), true);
    }

    @Test
    void summary_report_should_not_include_test_container_if_exlicitly_not_requested_by_the_client()
            throws Exception {
        mockMvc.perform(get("/reports/count-summary-report?date=2019-01-14&include-test=false"));

        verify(reportsService).getSummaryCountFor(LocalDate.of(2019, 1, 14), false);
    }

    @Test
    void envelopes_count_summary_report_should_return_result_generated_by_the_service() throws Exception {

        final EnvelopeCountSummary countSummaryOne = new EnvelopeCountSummary(
                152, 11, "container1", LocalDate.of(2021, 3, 4)
        );
        final EnvelopeCountSummary countSummaryTwo = new EnvelopeCountSummary(
                178, 13, "container2", LocalDate.of(2021, 3, 4)
        );
        List<EnvelopeCountSummary> envelopeCountSummaryList = new ArrayList<>();
        envelopeCountSummaryList.add(countSummaryOne);
        envelopeCountSummaryList.add(countSummaryTwo);
        given(reportsService.getEnvelopeSummaryCountFor(LocalDate.of(2021, 3, 4), false))
                .willReturn(envelopeCountSummaryList);

        EnvelopeCountSummaryReportListResponse response = new EnvelopeCountSummaryReportListResponse(
                envelopeCountSummaryList.stream()
                        .map(item -> new EnvelopeCountSummaryReportItem(
                                item.received,
                                item.rejected,
                                item.container,
                                item.date
                        ))
                        .collect(toList()),
                clockProvider
        );

        mockMvc
                .perform(get("/reports/envelopes-count-summary?date=2021-03-04"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_received").value(response.totalReceived))
                .andExpect(jsonPath("$.total_rejected").value(response.totalRejected))
                .andExpect(jsonPath("$.time_stamp").value(
                        response.timeStamp.format(
                                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].received").value(response.items.get(0).received))
                .andExpect(jsonPath("$.data[0].rejected").value(response.items.get(0).rejected))
                .andExpect(jsonPath("$.data[0].container").value(response.items.get(0).container))
                .andExpect(jsonPath("$.data[0].date").value(response.items.get(0).date.toString()));
    }

    @Test
    void envelopes_count_summary_report_should_not_include_test_container_by_default() throws Exception {
        mockMvc.perform(get("/reports/envelopes-count-summary?date=2019-01-14"));
        verify(reportsService).getEnvelopeSummaryCountFor(LocalDate.of(2019, 1, 14), false);
    }

    @Test
    void envelopes_count_summary_report_should_include_test_container_if_requested_by_the_client() throws Exception {
        mockMvc.perform(get("/reports/envelopes-count-summary?date=2019-01-14&include-test=true"));

        verify(reportsService).getEnvelopeSummaryCountFor(LocalDate.of(2019, 1, 14), true);
    }

    @Test
    void envelopes_count_summary_report_should_not_include_test_container_if_explicitly_not_requested_by_the_client()
            throws Exception {
        mockMvc.perform(get("/reports/envelopes-count-summary?date=2019-01-14&include-test=false"));

        verify(reportsService).getEnvelopeSummaryCountFor(LocalDate.of(2019, 1, 14), false);
    }

    @Test
    void should_return_zipfiles_summary_result_in_csv_format() throws Exception {
        LocalDate localDate = LocalDate.of(2019, 1, 14);
        LocalTime localTime = LocalTime.of(12, 30, 10, 0);

        ZipFileSummaryResponse zipFileSummaryResponse = new ZipFileSummaryResponse(
            "test.zip",
            localDate,
            localTime,
            localDate,
            localTime.plusHours(1),
            "bulkscan",
            NOTIFICATION_SENT.toString(),
            COMPLETED.toString(),
            EXCEPTION.name(),
            "ccd-id",
            "AUTO_CREATED_CASE"
        );

        given(reportsService.getZipFilesSummary(localDate, "bulkscan", null))
            .willReturn(singletonList(zipFileSummaryResponse));

        String expectedContent = String.format(
            "Container,Zip File Name,Date Received,Time Received,Date Processed,Time Processed,"
                + "Status,Classification,CCD Action,CCD ID\r\n"
                + "bulkscan,test.zip,%s,%s,%s,%s,NOTIFICATION_SENT,EXCEPTION,AUTO_CREATED_CASE,ccd-id\r\n",
            localDate.toString(), "12:30:10",
            localDate.toString(), "13:30:10"
        );

        mockMvc
            .perform(get("/reports/zip-files-summary?date=2019-01-14&container=bulkscan")
                         .accept(APPLICATION_OCTET_STREAM))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=zip-files-summary.csv"))
            .andExpect(content().contentType(APPLICATION_OCTET_STREAM))
            .andExpect(content().string(expectedContent));
    }

    @Test
    void should_return_empty_zipfiles_summary_in_csv_format_when_no_data_exists() throws Exception {
        LocalDate localDate = LocalDate.of(2019, 1, 14);

        given(reportsService.getZipFilesSummary(localDate, "bulkscan", null))
            .willReturn(emptyList());

        mockMvc
            .perform(get("/reports/zip-files-summary?date=2019-01-14")
                         .accept(APPLICATION_OCTET_STREAM))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=zip-files-summary.csv"))
            .andExpect(content().contentType(APPLICATION_OCTET_STREAM))
            .andExpect(content().string(
                "Container,Zip File Name,Date Received,Time Received,Date Processed,Time Processed,"
                    + "Status,Classification,CCD Action,CCD ID\r\n"
            ));
    }

    @Test
    void should_return_zipfiles_summary_result_in_json_format() throws Exception {
        LocalDate localDate = LocalDate.of(2019, 1, 14);
        LocalTime localTime = LocalTime.of(12, 30, 10, 0);

        ZipFileSummaryResponse response = new ZipFileSummaryResponse(
            "test.zip",
            localDate,
            localTime,
            localDate,
            localTime.plusHours(1),
            "bulkscan",
            NOTIFICATION_SENT.toString(),
            COMPLETED.toString(),
            SUPPLEMENTARY_EVIDENCE.name(),
            "ccd-id",
            "AUTO_CREATED_CASE"
        );

        given(reportsService.getZipFilesSummary(localDate, "bulkscan", NEW_APPLICATION))
            .willReturn(singletonList(response));

        mockMvc
            .perform(get("/reports/zip-files-summary?date=2019-01-14&container=bulkscan"
                             + "&classification=NEW_APPLICATION"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].file_name").value(response.fileName))
            .andExpect(jsonPath("$.data[0].date_received").value("2019-01-14"))
            .andExpect(jsonPath("$.data[0].time_received").value("12:30:10"))
            .andExpect(jsonPath("$.data[0].date_processed").value("2019-01-14"))
            .andExpect(jsonPath("$.data[0].time_processed").value("13:30:10"))
            .andExpect(jsonPath("$.data[0].container").value(response.container))
            .andExpect(jsonPath("$.data[0].last_event_status").value(response.lastEventStatus))
            .andExpect(jsonPath("$.data[0].envelope_status").value(response.envelopeStatus))
            .andExpect(jsonPath("$.data[0].classification").value(response.classification))
            .andExpect(jsonPath("$.data[0].ccd_id").value(response.ccdId))
            .andExpect(jsonPath("$.data[0].ccd_action").value(response.ccdAction));
    }

    @Test
    void should_return_total_count_summary_result() throws Exception {
        LocalDate localDate = LocalDate.of(2021, 4, 8);
        LocalTime localTime = LocalTime.of(12, 30, 10, 0);

        ZipFileSummaryResponse response0 = new ZipFileSummaryResponse(
            "test1.zip",
            localDate,
            localTime,
            localDate,
            localTime.plusHours(1),
            "bulkscan",
            NOTIFICATION_SENT.toString(),
            COMPLETED.toString(),
            SUPPLEMENTARY_EVIDENCE.name(),
            "ccd-id", null
        );

        ZipFileSummaryResponse response1 = new ZipFileSummaryResponse(
            "test1.zip",
            localDate,
            localTime,
            localDate,
            localTime.plusHours(1),
            "bulkscan",
            NOTIFICATION_SENT.toString(),
            COMPLETED.toString(),
            SUPPLEMENTARY_EVIDENCE.name(),
            "ccd-id",
            "AUTO_CREATED_CASE"
        );

        ZipFileSummaryResponse response2 = new ZipFileSummaryResponse(
            "test2.zip",
            localDate,
            localTime,
            localDate,
            localTime.plusHours(2),
            "bulkscan",
            UPLOADED.toString(),
            UPLOAD_FAILURE.toString(),
            SUPPLEMENTARY_EVIDENCE.name(),
            "ccd-id",
            "EXCEPTION_RECORD"
        );

        ZipFileSummaryResponse response3 = new ZipFileSummaryResponse(
            "test3.zip",
            localDate,
            localTime,
            localDate,
            localTime.plusHours(3),
            "bulkscan",
            NOTIFICATION_SENT.toString(),
            CREATED.toString(),
            SUPPLEMENTARY_EVIDENCE.name(),
            "ccd-id",
            null
        );

        List<ZipFileSummaryResponse> response = Arrays.asList(response0, response1, response2, response3);
        given(reportsService.getZipFilesSummary(localDate, "bulkscan", SUPPLEMENTARY_EVIDENCE))
            .willReturn(response);

        mockMvc
            .perform(get("/reports/zip-files-summary?date=2021-04-08&container=bulkscan"
                             + "&classification=SUPPLEMENTARY_EVIDENCE"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.total").value(4))
            .andExpect(jsonPath("$.total_completed").value(2))
            .andExpect(jsonPath("$.total_failed").value(1))
            .andExpect(jsonPath("$.exception_record").value(1))
            .andExpect(jsonPath("$.auto_created_case").value(1))
            .andExpect(jsonPath("$.auto_attached_to_case").value(0));
    }

    @Test
    void should_return_empty_zipfiles_summary_in_json_format_when_no_data_exists() throws Exception {
        LocalDate localDate = LocalDate.of(2019, 1, 14);

        given(reportsService.getZipFilesSummary(localDate, "bulkscan", null))
            .willReturn(emptyList());

        mockMvc
            .perform(get("/reports/zip-files-summary?date=2019-01-14"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void should_return_rejected_files() throws Exception {
        given(rejectedFilesReportService.getRejectedFiles())
            .willReturn(asList(
                new RejectedFile("a.zip", "A"),
                new RejectedFile("b.zip", "B")
            ));

        mockMvc
            .perform(get("/reports/rejected"))
            .andExpect(status().isOk())
            .andExpect(content().json(
                "{"
                    + "'count': 2,"
                    + "'rejected_files': ["
                    + "  {"
                    + "    'filename': 'a.zip',"
                    + "    'container': 'A'"
                    + "  },"
                    + "  {"
                    + "    'filename': 'b.zip',"
                    + "    'container': 'B'"
                    + "  }"
                    + "]"
                    + "}"
            ));
    }

    @Test
    void should_return_proper_response_when_there_are_no_rejected_files() throws Exception {
        given(rejectedFilesReportService.getRejectedFiles())
            .willReturn(emptyList());

        mockMvc
            .perform(get("/reports/rejected"))
            .andExpect(status().isOk())
            .andExpect(content().json(
                "{'count': 0, 'rejected_files': []}"
            ));
    }

    @Test
    void should_return_discrepancies_for_a_given_date() throws Exception {
        // given
        given(reconciliationService.getReconciliationReport(any(ReconciliationStatement.class)))
            .willReturn(
                singletonList(
                    new Discrepancy(
                        "file1",
                        "c1",
                        DiscrepancyType.PAYMENT_DCNS_MISMATCH,
                        "[12345, 23456]",
                        "[12345]"
                    )
                )
            );

        // when
        mockMvc
            .perform(
                post("/reports/reconciliation")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .content(Resources.toString(getResource("reconciliation/envelopes.json"), UTF_8))
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andExpect(content().json(Resources.toString(getResource("reconciliation/discrepancies.json"), UTF_8)));

        // then
        var argument = ArgumentCaptor.forClass(ReconciliationStatement.class);
        verify(reconciliationService).getReconciliationReport(argument.capture());
        var reconciliationStatement = argument.getValue();

        assertThat(reconciliationStatement.date).isEqualTo(LocalDate.of(2020, 8, 20));
    }

    @Test
    void should_return_rejected_zip_files() throws Exception {
        LocalDateTime dateTime = LocalDateTime.parse("2021-04-16T09:01:43.029000");
        UUID uuid1 = randomUUID();
        UUID uuid2 = randomUUID();
        given(rejectedZipFilesService.getRejectedZipFiles(LocalDate.parse("2021-04-16")))
                .willReturn(asList(
                        new RejectedZipFileItem(
                                "a.zip",
                                "A",
                                LocalDateTime.parse("2021-04-16T09:01:43.029000").toInstant(ZoneOffset.UTC),
                                uuid1,
                                "FILE_VALIDATION_FAILURE"
                        ),
                        new RejectedZipFileItem(
                                "b.zip",
                                "B",
                                LocalDateTime.parse("2021-04-16T09:01:44.029000").toInstant(ZoneOffset.UTC),
                                uuid2,
                                "DOC_SIGNATURE_FAILURE"
                        )
                ));

        mockMvc
                .perform(get("/reports/rejected-zip-files?date=2021-04-16"))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        "{"
                                + "'count': 2,"
                                + "'rejected_zip_files': ["
                                + "  {"
                                + "    'zip_file_name': 'a.zip',"
                                + "    'container': 'A',"
                                + "    'processing_started_date_time': '2021-04-16T09:01:43.029',"
                                + "    'envelope_id': '" + uuid1 + "',"
                                + "    'event': 'FILE_VALIDATION_FAILURE'"
                                + "  },"
                                + "  {"
                                + "    'zip_file_name': 'b.zip',"
                                + "    'container': 'B',"
                                + "    'processing_started_date_time': '2021-04-16T09:01:44.029',"
                                + "    'envelope_id': '" + uuid2 + "',"
                                + "    'event': 'DOC_SIGNATURE_FAILURE'"
                                + "  }"
                                + "]"
                                + "}"
                ));
    }

    @Test
    void should_return_received_scannable_items() throws Exception {
        given(receivedScannableItemsService.getReceivedScannableItems(LocalDate.parse("2021-04-16")))
                .willReturn(asList(
                        new ReceivedScannableItemItem(
                                "A",
                                3
                        ),
                        new ReceivedScannableItemItem(
                                "B",
                                5
                        )
                ));

        mockMvc
                .perform(get("/reports/received-scannable-items?date=2021-04-16"))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        "{"
                                + "'total': 8,"
                                + "'received_scannable_items': ["
                                + "  {"
                                + "    'container': 'A',"
                                + "    'count': 3"
                                + "  },"
                                + "  {"
                                + "    'container': 'B',"
                                + "    'count': 5"
                                + "  }"
                                + "]"
                                + "}"
                ));
    }

    @Test
    void should_handle_no_received_scannable_items() throws Exception {
        given(receivedScannableItemsService.getReceivedScannableItems(LocalDate.parse("2021-04-16")))
                .willReturn(emptyList());

        mockMvc
                .perform(get("/reports/received-scannable-items?date=2021-04-16"))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        "{"
                                + "'total': 0,"
                                + "'received_scannable_items': []"
                                + "}"
                ));
    }

    @Test
    void should_return_received_scannable_items_per_document_type() throws Exception {
        given(receivedScannableItemsService.getReceivedScannableItemsPerDocumentType(LocalDate.parse("2021-04-16")))
                .willReturn(asList(
                        new ReceivedScannableItemPerDocumentTypeItem(
                                "A",
                                "other",
                                3
                        ),
                        new ReceivedScannableItemPerDocumentTypeItem(
                                "A",
                                "form",
                                1
                        ),
                        new ReceivedScannableItemPerDocumentTypeItem(
                                "B",
                                "cherished",
                                5
                        )
                ));

        mockMvc
                .perform(get("/reports/received-scannable-items?date=2021-04-16&per_document_type=true"))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        "{"
                                + "'total': 9,"
                                + "'received_scannable_items': ["
                                + "  {"
                                + "    'container': 'A',"
                                + "    'document_type': 'other',"
                                + "    'count': 3"
                                + "  },"
                                + "  {"
                                + "    'container': 'A',"
                                + "    'document_type': 'form',"
                                + "    'count': 1"
                                + "  },"
                                + "  {"
                                + "    'container': 'B',"
                                + "    'document_type': 'cherished',"
                                + "    'count': 5"
                                + "  }"
                                + "]"
                                + "}"
                ));
    }

    @Test
    void should_return_received_scannable_items_per_document_type_false() throws Exception {
        given(receivedScannableItemsService.getReceivedScannableItems(LocalDate.parse("2021-04-16")))
                .willReturn(asList(
                        new ReceivedScannableItemItem(
                                "A",
                                3
                        ),
                        new ReceivedScannableItemItem(
                                "B",
                                5
                        )
                ));

        mockMvc
                .perform(get("/reports/received-scannable-items?date=2021-04-16"))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        "{"
                                + "'total': 8,"
                                + "'received_scannable_items': ["
                                + "  {"
                                + "    'container': 'A',"
                                + "    'count': 3"
                                + "  },"
                                + "  {"
                                + "    'container': 'B',"
                                + "    'count': 5"
                                + "  }"
                                + "]"
                                + "}"
                ));
    }

    @Test
    void should_handle_no_received_scannable_items_per_document_type() throws Exception {
        given(receivedScannableItemsService.getReceivedScannableItems(LocalDate.parse("2021-04-16")))
                .willReturn(emptyList());

        mockMvc
                .perform(get("/reports/received-scannable-items?date=2021-04-16&per_document_type=true"))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        "{"
                                + "'total': 0,"
                                + "'received_scannable_items': []"
                                + "}"
                ));
    }

    @Test
    void should_return_received_payments() throws Exception {
        given(receivedPaymentsService.getReceivedPayments(LocalDate.parse("2021-04-16")))
                .willReturn(asList(
                        new ReceivedPaymentItem(
                                "A",
                                3
                        ),
                        new ReceivedPaymentItem(
                                "B",
                                5
                        )
                ));

        mockMvc
                .perform(get("/reports/received-payments?date=2021-04-16"))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        "{"
                                + "'total': 8,"
                                + "'received_payments': ["
                                + "  {"
                                + "    'container': 'A',"
                                + "    'count': 3"
                                + "  },"
                                + "  {"
                                + "    'container': 'B',"
                                + "    'count': 5"
                                + "  }"
                                + "]"
                                + "}"
                ));
    }

    @Test
    void should_handle_no_payments() throws Exception {
        given(receivedPaymentsService.getReceivedPayments(LocalDate.parse("2021-04-16")))
                .willReturn(emptyList());

        mockMvc
                .perform(get("/reports/received-payments?date=2021-04-16"))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        "{"
                                + "'total': 0,"
                                + "'received_payments': []"
                                + "}"
                ));
    }

    private void givenTheRequestWasMadeAt(Instant time) {
        TestClockProvider.stoppedInstant = time;
    }

}
