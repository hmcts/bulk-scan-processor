package uk.gov.hmcts.reform.bulkscanprocessor.services.reports;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.RejectedZipFile;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.RejectedZipFileRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Service to handle rejected zip files.
 */
@Service
public class RejectedZipFilesService {

    private final RejectedZipFileRepository rejectedZipFileRepository;

    /**
     * Constructor for the RejectedZipFilesService.
     * @param rejectedZipFileRepository The repository for rejected zip file
     */
    public RejectedZipFilesService(RejectedZipFileRepository rejectedZipFileRepository) {
        this.rejectedZipFileRepository = rejectedZipFileRepository;
    }

    /**
     * Get the list of rejected zip files for a date.
     * @param date The date
     * @return The list of rejected zip files
     */
    public List<RejectedZipFile> getRejectedZipFiles(LocalDate date) {
        return rejectedZipFileRepository.getRejectedZipFilesReportFor(date);
    }

    /**
     * Get the list of rejected zip files with a specific name.
     * @param name the name the rejected zip files should match
     * @return The list of rejected zip files
     */
    public List<RejectedZipFile> getRejectedZipFiles(String name) {
        return rejectedZipFileRepository.getRejectedZipFilesReportFor(name);
    }
}
