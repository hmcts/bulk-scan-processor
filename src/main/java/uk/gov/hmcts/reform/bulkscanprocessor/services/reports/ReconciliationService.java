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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
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

        List<Discrepancy> discrepancies = new ArrayList<>();

        receivedZipFileDataList
            .forEach(receivedZipFile -> {
                Pair<String, String> receivedZipFileKey =
                    Pair.of(receivedZipFile.zipFileName, receivedZipFile.container);
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

        findReportedButNotReceived(reportedZipFilesMap, receivedZipFileDataList, discrepancies);

        return discrepancies;
    }

    private void findReportedButNotReceived(Map<Pair<String, String>, ReportedZipFile> reportedZipFilesMap, List<ReceivedZipFileData> receivedZipFileDataList, List<Discrepancy> discrepancies) {
        Set<Pair<String, String>> receivedZipFilesSet =
            receivedZipFileDataList
                .stream()
                .map(receivedZipFile -> Pair.of(receivedZipFile.zipFileName, receivedZipFile.container))
                .collect(toSet());
        reportedZipFilesMap.keySet()
            .stream()
            .filter(reportedZipFileKey -> !receivedZipFilesSet.contains(reportedZipFileKey))
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
            && !new HashSet<>(reportedList).equals(new HashSet<>(receivedList))) {
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
