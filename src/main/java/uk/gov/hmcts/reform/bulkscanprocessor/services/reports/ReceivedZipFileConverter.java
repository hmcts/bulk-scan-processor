package uk.gov.hmcts.reform.bulkscanprocessor.services.reports;

import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.ReceivedZipFile;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.ReceivedZipFileData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

@Component
public class ReceivedZipFileConverter {

    /**
     * Converts a list of received zip files to a list of ReceivedZipFileData.
     */
    public List<ReceivedZipFileData> convertReceivedZipFiles(List<ReceivedZipFile> receivedZipFiles) {
        return receivedZipFiles
            .stream()
            .collect(groupingBy(file -> Pair.of(file.getZipFileName(), file.getContainer())))
            .values()
            .stream()
            .map(this::getReceivedZipFileData)
            .collect(toList());
    }

    /**
     * Converts a list of received zip files to a ReceivedZipFileData.
     * @param receivedZipFilesList list of received zip files
     * @return ReceivedZipFileData
     */
    private ReceivedZipFileData getReceivedZipFileData(List<ReceivedZipFile> receivedZipFilesList) {
        Set<String> scannableItemDcns = new HashSet<>();
        Set<String> paymentDcns = new HashSet<>();
        receivedZipFilesList.forEach(
            receivedZipFile -> {
                if (receivedZipFile.getScannableItemDcn() != null) {
                    scannableItemDcns.add(receivedZipFile.getScannableItemDcn());
                }
                if (receivedZipFile.getPaymentDcn() != null) {
                    paymentDcns.add(receivedZipFile.getPaymentDcn());
                }
            }
        );
        return new ReceivedZipFileData(
            receivedZipFilesList.get(0).getZipFileName(),
            receivedZipFilesList.get(0).getContainer(),
            receivedZipFilesList.get(0).getRescanFor(),
            new ArrayList<>(scannableItemDcns),
            new ArrayList<>(paymentDcns),
            receivedZipFilesList.get(0).getEnvelopeId()
        );
    }
}
