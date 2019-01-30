package uk.gov.hmcts.reform.bulkscanprocessor.services.zipfilestatus;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.services.zipfilestatus.model.ZipFileStatus;

@Service
public class ZipFileStatusService {

    private final ProcessEventRepository eventRepo;
    private final EnvelopeRepository envelopeRepo;

    // region constructor

    public ZipFileStatusService(ProcessEventRepository eventRepo, EnvelopeRepository envelopeRepo) {
        this.eventRepo = eventRepo;
        this.envelopeRepo = envelopeRepo;
    }

    // endregion

    public ZipFileStatus getStatusFor(String zipFileName) {
        return new ZipFileStatus(
            envelopeRepo.findByZipFileName(zipFileName),
            eventRepo.findByZipFileName(zipFileName)
        );
    }
}
