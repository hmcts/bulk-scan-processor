package uk.gov.hmcts.reform.bulkscanprocessor.services.reports;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.ReceivedZipFile;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.ReceivedZipFileRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.Discrepancy;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.ReceivedZipFileData;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.ReconciliationStatement;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.ReportedZipFile;

import java.time.LocalDate;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.DiscrepancyType.PAYMENT_DCNS_MISMATCH;
import static uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.DiscrepancyType.RECEIVED_BUT_NOT_REPORTED;
import static uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.DiscrepancyType.REPORTED_BUT_NOT_RECEIVED;
import static uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.DiscrepancyType.SCANNABLE_DOCUMENT_DCNS_MISMATCH;

@ExtendWith(MockitoExtension.class)
class ReconciliationServiceTest {
    private ReconciliationService reconciliationService;

    @Mock
    private ReceivedZipFileRepository receivedZipFileRepository;

    @Mock
    private ReceivedZipFileConverter receivedZipFileConverter;

    @Mock
    private List<ReceivedZipFile> receivedZipFiles;

    @BeforeEach
    void setUp() {
        reconciliationService = new ReconciliationService(receivedZipFileRepository, receivedZipFileConverter);
    }

    @Test
    void should_return_no_discrepancies_if_no_differences() {
        // given
        LocalDate date = LocalDate.now();

        final List<ReceivedZipFileData> receivedZipFileDataList = asList(
            new ReceivedZipFileData("file1", "c1", null, null),
            new ReceivedZipFileData("file2", "c2", null, null)
        );

        prepareReconciliationServiceBehaviour(date, receivedZipFileDataList);

        List<ReportedZipFile> envelopes = asList(
            new ReportedZipFile("file1", "c1", null, null, null),
            new ReportedZipFile("file2", "c2", null, null, null)
        );
        ReconciliationStatement statement = new ReconciliationStatement(date, envelopes);

        // when
        List<Discrepancy> discrepancies = reconciliationService.getReconciliationReport(statement);

        // then
        assertThat(discrepancies).isEmpty();
    }

    @Test
    void should_return_reported_but_not_received_discrepancy() {
        // given
        LocalDate date = LocalDate.now();

        final List<ReceivedZipFileData> receivedZipFileDataList = singletonList(
            new ReceivedZipFileData("file1", "c1", null, null)
        );

        prepareReconciliationServiceBehaviour(date, receivedZipFileDataList);

        List<ReportedZipFile> envelopes = asList(
            new ReportedZipFile("file1", "c1", null, null, null),
            new ReportedZipFile("file2", "c2", null, null, null)
        );
        ReconciliationStatement statement = new ReconciliationStatement(date, envelopes);

        // when
        List<Discrepancy> discrepancies = reconciliationService.getReconciliationReport(statement);

        // then
        assertThat(discrepancies)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new Discrepancy("file2", "c2", REPORTED_BUT_NOT_RECEIVED.text)
            );
    }

    @Test
    void should_return_received_but_not_reported_discrepancy() {
        // given
        LocalDate date = LocalDate.now();

        final List<ReceivedZipFileData> receivedZipFileDataList = asList(
            new ReceivedZipFileData("file1", "c1", null, null),
            new ReceivedZipFileData("file2", "c2", null, null)
        );

        prepareReconciliationServiceBehaviour(date, receivedZipFileDataList);

        List<ReportedZipFile> envelopes = singletonList(
            new ReportedZipFile("file1", "c1", null, null, null)
        );
        ReconciliationStatement statement = new ReconciliationStatement(date, envelopes);

        // when
        List<Discrepancy> discrepancies = reconciliationService.getReconciliationReport(statement);

        // then
        assertThat(discrepancies)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new Discrepancy("file2", "c2", RECEIVED_BUT_NOT_REPORTED.text)
            );
    }

    @Test
    void should_return_reported_but_not_received_and_received_but_not_reported_discrepancies() {
        // given
        LocalDate date = LocalDate.now();

        final List<ReceivedZipFileData> receivedZipFileDataList = asList(
            new ReceivedZipFileData("file2", "c2", null, null),
            new ReceivedZipFileData("file3", "c3", null, null)
        );

        prepareReconciliationServiceBehaviour(date, receivedZipFileDataList);

        List<ReportedZipFile> envelopes = asList(
            new ReportedZipFile("file1", "c1", null, null, null),
            new ReportedZipFile("file2", "c2", null, null, null)
        );
        ReconciliationStatement statement = new ReconciliationStatement(date, envelopes);

        // when
        List<Discrepancy> discrepancies = reconciliationService.getReconciliationReport(statement);

        // then
        assertThat(discrepancies)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new Discrepancy("file1", "c1", REPORTED_BUT_NOT_RECEIVED.text),
                new Discrepancy("file3", "c3", RECEIVED_BUT_NOT_REPORTED.text)
            );
    }

    @Test
    void should_return_no_discrepancies_if_no_differences_in_dcns() {
        // given
        LocalDate date = LocalDate.now();

        final List<ReceivedZipFileData> receivedZipFileDataList = asList(
            new ReceivedZipFileData("file1", "c1", asList("doc-1", "doc-2"), asList("pay-1", "pay-2")),
            new ReceivedZipFileData("file2", "c2", asList("doc-3", "doc-4"), asList("pay-3", "pay-4"))
        );

        prepareReconciliationServiceBehaviour(date, receivedZipFileDataList);

        List<ReportedZipFile> envelopes = asList(
            new ReportedZipFile("file1", "c1", null, asList("doc-1", "doc-2"), asList("pay-1", "pay-2")),
            new ReportedZipFile("file2", "c2", null, asList("doc-3", "doc-4"), asList("pay-3", "pay-4"))
        );
        ReconciliationStatement statement = new ReconciliationStatement(date, envelopes);

        // when
        List<Discrepancy> discrepancies = reconciliationService.getReconciliationReport(statement);

        // then
        assertThat(discrepancies).isEmpty();
    }

    @Test
    void should_return_no_discrepancies_if_no_differences_in_dcns_with_different_order() {
        // given
        LocalDate date = LocalDate.now();

        final List<ReceivedZipFileData> receivedZipFileDataList = asList(
            new ReceivedZipFileData("file1", "c1", asList("doc-1", "doc-2"), asList("pay-1", "pay-2")),
            new ReceivedZipFileData("file2", "c2", asList("doc-3", "doc-4"), asList("pay-3", "pay-4"))
        );

        prepareReconciliationServiceBehaviour(date, receivedZipFileDataList);

        List<ReportedZipFile> envelopes = asList(
            new ReportedZipFile("file1", "c1", null, asList("doc-2", "doc-1"), asList("pay-2", "pay-1")),
            new ReportedZipFile("file2", "c2", null, asList("doc-4", "doc-3"), asList("pay-4", "pay-3"))
        );
        ReconciliationStatement statement = new ReconciliationStatement(date, envelopes);

        // when
        List<Discrepancy> discrepancies = reconciliationService.getReconciliationReport(statement);

        // then
        assertThat(discrepancies).isEmpty();
    }

    @Test
    void should_return_no_discrepancies_if_different_scannable_document_dcns() {
        // given
        LocalDate date = LocalDate.now();

        final List<ReceivedZipFileData> receivedZipFileDataList = asList(
            new ReceivedZipFileData("file1", "c1", asList("doc-1", "doc-2"), asList("pay-1", "pay-2")),
            new ReceivedZipFileData("file2", "c2", asList("doc-3", "doc-4"), asList("pay-3", "pay-4"))
        );

        prepareReconciliationServiceBehaviour(date, receivedZipFileDataList);

        List<ReportedZipFile> envelopes = asList(
            new ReportedZipFile("file1", "c1", null, singletonList("doc-1"), asList("pay-1", "pay-2")),
            new ReportedZipFile("file2", "c2", null, asList("doc-3", "doc-4"), asList("pay-3", "pay-4"))
        );
        ReconciliationStatement statement = new ReconciliationStatement(date, envelopes);

        // when
        List<Discrepancy> discrepancies = reconciliationService.getReconciliationReport(statement);

        // then
        assertThat(discrepancies)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new Discrepancy("file1", "c1", SCANNABLE_DOCUMENT_DCNS_MISMATCH.text, "[doc-1]", "[doc-1, doc-2]")
            );
    }

    @Test
    void should_return_no_discrepancies_if_different_payment_dcns() {
        // given
        LocalDate date = LocalDate.now();

        final List<ReceivedZipFileData> receivedZipFileDataList = asList(
            new ReceivedZipFileData("file1", "c1", asList("doc-1", "doc-2"), asList("pay-1", "pay-2")),
            new ReceivedZipFileData("file2", "c2", asList("doc-3", "doc-4"), asList("pay-3", "pay-4"))
        );

        prepareReconciliationServiceBehaviour(date, receivedZipFileDataList);

        List<ReportedZipFile> envelopes = asList(
            new ReportedZipFile("file1", "c1", null, asList("doc-1", "doc-2"), singletonList("pay-1")),
            new ReportedZipFile("file2", "c2", null, asList("doc-3", "doc-4"), asList("pay-3", "pay-4"))
        );
        ReconciliationStatement statement = new ReconciliationStatement(date, envelopes);

        // when
        List<Discrepancy> discrepancies = reconciliationService.getReconciliationReport(statement);

        // then
        assertThat(discrepancies)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new Discrepancy("file1", "c1", PAYMENT_DCNS_MISMATCH.text, "[pay-1]", "[pay-1, pay-2]")
            );
    }

    @Test
    void should_handle_null_reported_scannable_document_dcns() {
        // given
        LocalDate date = LocalDate.now();

        final List<ReceivedZipFileData> receivedZipFileDataList = asList(
            new ReceivedZipFileData("file1", "c1", asList("doc-1", "doc-2"), asList("pay-1", "pay-2")),
            new ReceivedZipFileData("file2", "c2", asList("doc-3", "doc-4"), asList("pay-3", "pay-4"))
        );

        prepareReconciliationServiceBehaviour(date, receivedZipFileDataList);

        List<ReportedZipFile> envelopes = asList(
            new ReportedZipFile("file1", "c1", null, null, asList("pay-1", "pay-2")),
            new ReportedZipFile("file2", "c2", null, asList("doc-3", "doc-4"), asList("pay-3", "pay-4"))
        );
        ReconciliationStatement statement = new ReconciliationStatement(date, envelopes);

        // when
        List<Discrepancy> discrepancies = reconciliationService.getReconciliationReport(statement);

        // then
        assertThat(discrepancies)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new Discrepancy("file1", "c1", SCANNABLE_DOCUMENT_DCNS_MISMATCH.text, null, "[doc-1, doc-2]")
            );
    }

    @Test
    void should_handle_empty_reported_scannable_document_dcns() {
        // given
        LocalDate date = LocalDate.now();

        final List<ReceivedZipFileData> receivedZipFileDataList = asList(
            new ReceivedZipFileData("file1", "c1", asList("doc-1", "doc-2"), asList("pay-1", "pay-2")),
            new ReceivedZipFileData("file2", "c2", asList("doc-3", "doc-4"), asList("pay-3", "pay-4"))
        );

        prepareReconciliationServiceBehaviour(date, receivedZipFileDataList);

        List<ReportedZipFile> envelopes = asList(
            new ReportedZipFile("file1", "c1", null, emptyList(), asList("pay-1", "pay-2")),
            new ReportedZipFile("file2", "c2", null, asList("doc-3", "doc-4"), asList("pay-3", "pay-4"))
        );
        ReconciliationStatement statement = new ReconciliationStatement(date, envelopes);

        // when
        List<Discrepancy> discrepancies = reconciliationService.getReconciliationReport(statement);

        // then
        assertThat(discrepancies)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new Discrepancy("file1", "c1", SCANNABLE_DOCUMENT_DCNS_MISMATCH.text, "[]", "[doc-1, doc-2]")
            );
    }

    @Test
    void should_handle_empty_received_scannable_document_dcns() {
        // given
        LocalDate date = LocalDate.now();

        final List<ReceivedZipFileData> receivedZipFileDataList = asList(
            new ReceivedZipFileData("file1", "c1", emptyList(), asList("pay-1", "pay-2")),
            new ReceivedZipFileData("file2", "c2", asList("doc-3", "doc-4"), asList("pay-3", "pay-4"))
        );

        prepareReconciliationServiceBehaviour(date, receivedZipFileDataList);

        List<ReportedZipFile> envelopes = asList(
            new ReportedZipFile("file1", "c1", null, asList("doc-1", "doc-2"), asList("pay-1", "pay-2")),
            new ReportedZipFile("file2", "c2", null, asList("doc-3", "doc-4"), asList("pay-3", "pay-4"))
        );
        ReconciliationStatement statement = new ReconciliationStatement(date, envelopes);

        // when
        List<Discrepancy> discrepancies = reconciliationService.getReconciliationReport(statement);

        // then
        assertThat(discrepancies)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new Discrepancy("file1", "c1", SCANNABLE_DOCUMENT_DCNS_MISMATCH.text, "[doc-1, doc-2]", "[]")
            );
    }

    @Test
    void should_handle_null_reported_payment_dcns() {
        // given
        LocalDate date = LocalDate.now();

        final List<ReceivedZipFileData> receivedZipFileDataList = asList(
            new ReceivedZipFileData("file1", "c1", asList("doc-1", "doc-2"), asList("pay-1", "pay-2")),
            new ReceivedZipFileData("file2", "c2", asList("doc-3", "doc-4"), asList("pay-3", "pay-4"))
        );

        prepareReconciliationServiceBehaviour(date, receivedZipFileDataList);

        List<ReportedZipFile> envelopes = asList(
            new ReportedZipFile("file1", "c1", null, asList("doc-1", "doc-2"), null),
            new ReportedZipFile("file2", "c2", null, asList("doc-3", "doc-4"), asList("pay-3", "pay-4"))
        );
        ReconciliationStatement statement = new ReconciliationStatement(date, envelopes);

        // when
        List<Discrepancy> discrepancies = reconciliationService.getReconciliationReport(statement);

        // then
        assertThat(discrepancies)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new Discrepancy("file1", "c1", PAYMENT_DCNS_MISMATCH.text, null, "[pay-1, pay-2]")
            );
    }

    @Test
    void should_handle_empty_reported_payment_dcns() {
        // given
        LocalDate date = LocalDate.now();

        final List<ReceivedZipFileData> receivedZipFileDataList = asList(
            new ReceivedZipFileData("file1", "c1", asList("doc-1", "doc-2"), asList("pay-1", "pay-2")),
            new ReceivedZipFileData("file2", "c2", asList("doc-3", "doc-4"), asList("pay-3", "pay-4"))
        );

        prepareReconciliationServiceBehaviour(date, receivedZipFileDataList);

        List<ReportedZipFile> envelopes = asList(
            new ReportedZipFile("file1", "c1", null, asList("doc-1", "doc-2"), emptyList()),
            new ReportedZipFile("file2", "c2", null, asList("doc-3", "doc-4"), asList("pay-3", "pay-4"))
        );
        ReconciliationStatement statement = new ReconciliationStatement(date, envelopes);

        // when
        List<Discrepancy> discrepancies = reconciliationService.getReconciliationReport(statement);

        // then
        assertThat(discrepancies)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new Discrepancy("file1", "c1", PAYMENT_DCNS_MISMATCH.text, "[]", "[pay-1, pay-2]")
            );
    }

    @Test
    void should_handle_empty_received_payment_dcns() {
        // given
        LocalDate date = LocalDate.now();

        final List<ReceivedZipFileData> receivedZipFileDataList = asList(
            new ReceivedZipFileData("file1", "c1", asList("doc-1", "doc-2"), emptyList()),
            new ReceivedZipFileData("file2", "c2", asList("doc-3", "doc-4"), asList("pay-3", "pay-4"))
        );
        prepareReconciliationServiceBehaviour(date, receivedZipFileDataList);

        List<ReportedZipFile> envelopes = asList(
            new ReportedZipFile("file1", "c1", null, asList("doc-1", "doc-2"), asList("pay-1", "pay-2")),
            new ReportedZipFile("file2", "c2", null, asList("doc-3", "doc-4"), asList("pay-3", "pay-4"))
        );
        ReconciliationStatement statement = new ReconciliationStatement(date, envelopes);

        // when
        List<Discrepancy> discrepancies = reconciliationService.getReconciliationReport(statement);

        // then
        assertThat(discrepancies)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new Discrepancy("file1", "c1", PAYMENT_DCNS_MISMATCH.text, "[pay-1, pay-2]", "[]")
            );
    }

    private void prepareReconciliationServiceBehaviour(
        LocalDate date,
        List<ReceivedZipFileData> receivedZipFileDataList
    ) {
        given(receivedZipFileRepository.getReceivedZipFilesReportFor(date)).willReturn(receivedZipFiles);
        given(receivedZipFileConverter.convertReceivedZipFiles(receivedZipFiles)).willReturn(receivedZipFileDataList);
    }
}
