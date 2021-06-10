package uk.gov.hmcts.reform.bulkscanprocessor.services.reports;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.RejectedZipFile;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.RejectedZipFileRepository;

import java.time.LocalDate;
import java.util.List;

@Service
public class RejectedZipFilesService {

    private static final Logger log = LoggerFactory.getLogger(RejectedZipFilesService.class);

    private final RejectedZipFileRepository rejectedZipFileRepository;

    public RejectedZipFilesService(RejectedZipFileRepository rejectedZipFileRepository) {
        this.rejectedZipFileRepository = rejectedZipFileRepository;
    }

    public List<RejectedZipFile> getRejectedZipFiles(LocalDate date) {
        return rejectedZipFileRepository.getRejectedZipFilesReportFor(date);
    }
}
