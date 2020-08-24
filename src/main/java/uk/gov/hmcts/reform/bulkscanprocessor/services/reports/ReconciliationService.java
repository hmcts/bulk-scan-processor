package uk.gov.hmcts.reform.bulkscanprocessor.services.reports;

import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.ReceivedZipFile;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.ReceivedZipFileRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.Discrepancy;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.DiscrepancyType;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.ReceivedZipFileData;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.ReconciliationStatement;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.ReportedZipFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.DiscrepancyType.PAYMENT_DCNS_MISMATCH;
import static uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.DiscrepancyType.RECEIVED_BUT_NOT_REPORTED;
import static uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.DiscrepancyType.REPORTED_BUT_NOT_RECEIVED;
import static uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.DiscrepancyType.SCANNABLE_DOCUMENT_DCNS_MISMATCH;

@Service
public class ReconciliationService {
    private final ReceivedZipFileRepository receivedZipFileRepository;
    private final ReceivedZipFileConverter receivedZipFileConverter;

    public ReconciliationService(
        ReceivedZipFileRepository receivedZipFileRepository,
        ReceivedZipFileConverter receivedZipFileConverter
    ) {
        this.receivedZipFileRepository = receivedZipFileRepository;
        this.receivedZipFileConverter = receivedZipFileConverter;
    }

    public List<Discrepancy> getReconciliationReport(ReconciliationStatement reconciliationStatement) {
        Map<Pair<String, String>, ReportedZipFile> reportedZipFilesMap =
            reconciliationStatement.envelopes
                .stream()
                .collect(
                    toMap(
                        envelope -> Pair.of(envelope.zipFileName, envelope.container), identity()
                    )
                );

        List<ReceivedZipFile> receivedZipFiles =
            receivedZipFileRepository.getReceivedZipFilesReportFor(reconciliationStatement.date);
        List<ReceivedZipFileData> receivedZipFileDataList =
            receivedZipFileConverter.convertReceivedZipFiles(receivedZipFiles);
        Map<Pair<String, String>, ReceivedZipFileData> receivedZipFilesMap =
            receivedZipFileDataList
                .stream()
                .collect(
                    toMap(
                        receivedZipFile -> Pair.of(receivedZipFile.zipFileName, receivedZipFile.container), identity()
                    )
                );

        List<Discrepancy> discrepancies = new ArrayList<>();

        receivedZipFilesMap.keySet()
            .forEach(receivedZipFileKey -> {
                final ReceivedZipFileData receivedZipFile = receivedZipFilesMap.get(receivedZipFileKey);
                if (reportedZipFilesMap.containsKey(receivedZipFileKey)) {
                    ReportedZipFile reportedZipFile = reportedZipFilesMap.get(receivedZipFileKey);
                    compareLists(
                        discrepancies,
                        receivedZipFile,
                        reportedZipFile.paymentDcns,
                        receivedZipFile.paymentDcns,
                        PAYMENT_DCNS_MISMATCH
                    );
                    compareLists(
                        discrepancies,
                        receivedZipFile,
                        reportedZipFile.scannableItemDcns,
                        receivedZipFile.scannableItemDcns,
                        SCANNABLE_DOCUMENT_DCNS_MISMATCH
                    );
                } else {
                    discrepancies.add(
                        new Discrepancy(
                            receivedZipFile.zipFileName,
                            receivedZipFile.container,
                            RECEIVED_BUT_NOT_REPORTED.text
                        )
                    );
                }
            });
        reportedZipFilesMap.keySet()
            .stream()
            .filter(reportedZipFileKey -> !receivedZipFilesMap.containsKey(reportedZipFileKey))
            .forEach(reportedZipFileKey -> {
                final ReportedZipFile reportedZipFile = reportedZipFilesMap.get(reportedZipFileKey);
                discrepancies.add(
                    new Discrepancy(
                        reportedZipFile.zipFileName,
                        reportedZipFile.container,
                        REPORTED_BUT_NOT_RECEIVED.text
                    )
                );
            });

        return discrepancies;
    }

    private void compareLists(
        List<Discrepancy> discrepancies,
        ReceivedZipFileData receivedZipFile,
        List<String> reportedList,
        List<String> receivedList,
        DiscrepancyType discrepancyType
    ) {
        if (reportedList == null && receivedList != null
            || reportedList != null
            && !reportedList.equals(receivedList)) {
            discrepancies.add(
                new Discrepancy(
                    receivedZipFile.zipFileName,
                    receivedZipFile.container,
                    discrepancyType.text,
                    printList(reportedList),
                    printList(receivedList)
                )
            );
        }
    }

    private String printList(List<String> list) {
        return list == null ? null : list.toString();
    }
}
