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
import java.util.Objects;
import java.util.Set;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.DiscrepancyType.PAYMENT_DCNS_MISMATCH;
import static uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.DiscrepancyType.RECEIVED_BUT_NOT_REPORTED;
import static uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.DiscrepancyType.REPORTED_BUT_NOT_RECEIVED;
import static uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.DiscrepancyType.SCANNABLE_DOCUMENT_DCNS_MISMATCH;

/**
 * Service to handle reconciliation.
 */
@Service
public class ReconciliationService {
    private final ReceivedZipFileRepository receivedZipFileRepository;
    private final ReceivedZipFileConverter receivedZipFileConverter;

    /**
     * Constructor for the ReconciliationService.
     * @param receivedZipFileRepository The repository for received zip file
     * @param receivedZipFileConverter The converter for received zip file
     */
    public ReconciliationService(
        ReceivedZipFileRepository receivedZipFileRepository,
        ReceivedZipFileConverter receivedZipFileConverter
    ) {
        this.receivedZipFileRepository = receivedZipFileRepository;
        this.receivedZipFileConverter = receivedZipFileConverter;
    }

    /**
     * Get the reconciliation report for a reconciliation statement.
     * @param reconciliationStatement The reconciliation statement
     * @return The reconciliation report
     */
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

                    if (Objects.isNull(receivedZipFile.envelopeId)) {
                        discrepancies.add(
                            new Discrepancy(
                                receivedZipFile.zipFileName,
                                receivedZipFile.container,
                                DiscrepancyType.REJECTED_ENVELOPE
                            )
                        );
                    } else {
                        compareForNonRejectedEnvelope(
                            discrepancies,
                            receivedZipFile,
                            reportedZipFile
                        );
                    }
                } else {
                    discrepancies.add(
                        new Discrepancy(
                            receivedZipFile.zipFileName,
                            receivedZipFile.container,
                            RECEIVED_BUT_NOT_REPORTED
                        )
                    );
                }
            });

        findReportedButNotReceived(reportedZipFilesMap, receivedZipFileDataList, discrepancies);

        return discrepancies;
    }

    /**
     * Find the reported but not received.
     * @param reportedZipFilesMap The reported zip files map
     * @param receivedZipFileDataList The received zip file data list
     * @param discrepancies The discrepancies
     */
    private void findReportedButNotReceived(
        Map<Pair<String, String>, ReportedZipFile> reportedZipFilesMap,
        List<ReceivedZipFileData> receivedZipFileDataList,
        List<Discrepancy> discrepancies
    ) {
        Set<Pair<String, String>> receivedZipFilesSet =
            receivedZipFileDataList
                .stream()
                .map(receivedZipFile -> Pair.of(receivedZipFile.zipFileName, receivedZipFile.container))
                .collect(toSet());
        reportedZipFilesMap.keySet()
            .stream()
            .filter(reportedZipFileKey -> !receivedZipFilesSet.contains(reportedZipFileKey))
            .forEach(reportedZipFileKey -> discrepancies.add(
                new Discrepancy(
                    reportedZipFileKey.getFirst(),
                    reportedZipFileKey.getSecond(),
                    REPORTED_BUT_NOT_RECEIVED
                )
            ));
    }

    /**
     * Compare for non rejected envelope.
     * @param discrepancies The discrepancies
     * @param receivedZipFile The received zip file
     * @param reportedZipFile The reported zip file
     */
    private void compareForNonRejectedEnvelope(
        List<Discrepancy> discrepancies,
        ReceivedZipFileData receivedZipFile,
        ReportedZipFile reportedZipFile
    ) {
        compareRescanFor(discrepancies, reportedZipFile, receivedZipFile);
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
    }

    /**
     * Compare lists.
     * @param discrepancies The discrepancies
     * @param receivedZipFile The received zip file
     * @param reportedList The reported list
     * @param receivedList The received list
     * @param discrepancyType The discrepancy type
     */
    private void compareLists(
        List<Discrepancy> discrepancies,
        ReceivedZipFileData receivedZipFile,
        List<String> reportedList,
        List<String> receivedList,
        DiscrepancyType discrepancyType
    ) {
        if ((receivedList == null || receivedList.isEmpty())
            && (reportedList == null || reportedList.isEmpty())) {
            // both list empty or null
            return;
        }

        if ((reportedList == null)
            || (receivedList == null || !new HashSet<>(reportedList).equals(new HashSet<>(receivedList)))
        ) {
            discrepancies.add(
                new Discrepancy(
                    receivedZipFile.zipFileName,
                    receivedZipFile.container,
                    discrepancyType,
                    printList(reportedList),
                    printList(receivedList)
                )
            );
        }
    }

    /**
     * Compare rescan for.
     * @param discrepancies The discrepancies
     * @param reportedZipFile The reported zip file
     * @param receivedZipFile The received zip file
     */
    private void compareRescanFor(
        List<Discrepancy> discrepancies,
        ReportedZipFile reportedZipFile,
        ReceivedZipFileData receivedZipFile
    ) {
        if (!equalStrEmptyInsensitive(reportedZipFile.rescanFor, receivedZipFile.rescanFor)) {
            discrepancies.add(
                new Discrepancy(
                    receivedZipFile.zipFileName,
                    receivedZipFile.container,
                    DiscrepancyType.RESCAN_FOR_MISMATCH,
                    reportedZipFile.rescanFor,
                    receivedZipFile.rescanFor
                )
            );
        }
    }

    /**
     * Check if two strings are equal ignoring case and empty strings.
     * @param str1 The first string
     * @param str2 The second string
     * @return True if the strings are equal ignoring case and empty strings
     */
    private static boolean equalStrEmptyInsensitive(String str1, String str2) {
        return (str1 == null || str1.isEmpty())
            ? (str2 == null || str2.isEmpty())
            : str1.equals(str2);
    }

    /**
     * Print list.
     * @param list The list
     * @return The string representation of the list
     */
    private String printList(List<String> list) {
        return list == null ? null : list.toString();
    }
}
