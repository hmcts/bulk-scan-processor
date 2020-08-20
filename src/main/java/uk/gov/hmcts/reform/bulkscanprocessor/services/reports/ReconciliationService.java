package uk.gov.hmcts.reform.bulkscanprocessor.services.reports;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.ReceivedZipFile;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.ReceivedZipFileRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.Discrepancy;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.ReconciliationStatement;

import java.util.List;

@Service
public class ReconciliationService {
    private final ReceivedZipFileRepository receivedZipFileRepository;

    public ReconciliationService(ReceivedZipFileRepository receivedZipFileRepository) {
        this.receivedZipFileRepository = receivedZipFileRepository;
    }

    public List<Discrepancy> getReconciliationReport(ReconciliationStatement reconciliationStatement) {
        List<ReceivedZipFile> receivedZipFiles = receivedZipFileRepository.getReceivedZipFilesReportFor(reconciliationStatement.date);
    }
}
