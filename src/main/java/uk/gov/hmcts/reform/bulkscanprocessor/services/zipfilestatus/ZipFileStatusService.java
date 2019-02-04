package uk.gov.hmcts.reform.bulkscanprocessor.services.zipfilestatus;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.zipfilestatus.ZipFileEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.zipfilestatus.ZipFileEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.zipfilestatus.ZipFileStatus;

import java.util.List;

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
        List<Envelope> envelopes = envelopeRepo.findByZipFileName(zipFileName);
        List<ProcessEvent> events = eventRepo.findByZipFileName(zipFileName);

        return new ZipFileStatus(
            zipFileName,
            envelopes.stream().map(this::mapEnvelope).collect(toList()),
            events.stream().map(this::mapEvent).collect(toList())
        );
    }

    private ZipFileEnvelope mapEnvelope(Envelope envelope) {
        return new ZipFileEnvelope(
            envelope.getId().toString(),
            envelope.getContainer(),
            envelope.getStatus().name()
        );
    }

    private ZipFileEvent mapEvent(ProcessEvent event) {
        return new ZipFileEvent(
            event.getEvent().name(),
            event.getContainer(),
            event.getCreatedAt()
        );
    }
}
