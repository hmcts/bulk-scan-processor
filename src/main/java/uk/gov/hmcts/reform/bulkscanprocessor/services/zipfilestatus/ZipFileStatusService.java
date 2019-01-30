package uk.gov.hmcts.reform.bulkscanprocessor.services.zipfilestatus;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.zipfilestatus.ZipFileEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.zipfilestatus.ZipFileEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.zipfilestatus.ZipFileStatus;

import static java.util.stream.Collectors.toList;

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
            envelopeRepo
                .findByZipFileName(zipFileName)
                .stream()
                .map(envelope ->
                    new ZipFileEnvelope(
                        envelope.getId().toString(),
                        envelope.getContainer(),
                        envelope.getStatus().name()
                    )
                )
                .collect(toList()),
            eventRepo
                .findByZipFileName(zipFileName)
                .stream()
                .map(event ->
                    new ZipFileEvent(
                        event.getEvent().name(),
                        event.getContainer(),
                        event.getCreatedAt()
                    )
                )
                .collect(toList())

        );
    }
}
