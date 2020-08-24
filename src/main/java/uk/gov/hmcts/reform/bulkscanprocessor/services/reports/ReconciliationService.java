package uk.gov.hmcts.reform.bulkscanprocessor.services.reports;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.ReceivedZipFile;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.ReceivedZipFileRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.Discrepancy;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.DiscrepancyType;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.ReconciliationStatement;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.ZipFileData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        List<ReceivedZipFile> receivedZipFiles =
            receivedZipFileRepository.getReceivedZipFilesReportFor(reconciliationStatement.date);
        List<ZipFileData> receivedZipFileDataList =
            new ArrayList<>(receivedZipFileConverter.convertReceivedZipFiles(receivedZipFiles));

        List<Discrepancy> discrepancies = new ArrayList<>();

        Map<ZipFileData, ZipFileData> reportedZipFilesMap =
            reconciliationStatement.envelopes.stream().collect(toMap(envelope -> envelope, envelope -> envelope));
        receivedZipFileDataList
            .forEach(receivedZipFile -> {
                if (reportedZipFilesMap.containsKey(receivedZipFile)) {
                    ZipFileData reportedZipFile = reportedZipFilesMap.get(receivedZipFile);
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
            }
        );
        reconciliationStatement.envelopes
            .stream()
            .filter(file -> !receivedZipFileDataList.contains(file))
            .forEach(
                file -> discrepancies.add(
                    new Discrepancy(
                        file.zipFileName,
                        file.container,
                        REPORTED_BUT_NOT_RECEIVED.text
                    )
                )
            );

        return discrepancies;
    }

    private void compareLists(
        List<Discrepancy> discrepancies,
        ZipFileData file,
        List<String> reportedList,
        List<String> receivedList,
        DiscrepancyType discrepancyType
    ) {
        if (reportedList == null && receivedList != null
            || reportedList != null
            && !reportedList.equals(receivedList)) {
            discrepancies.add(
                new Discrepancy(
                    file.zipFileName,
                    file.container,
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
