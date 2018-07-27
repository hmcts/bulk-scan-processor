package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeNotFoundException;

import java.util.UUID;

@Service
public class EnvelopeUpdateService {

    private final EnvelopeRepository envelopeRepo;
    private final ProcessEventRepository eventRepo;
    private final EnvelopeAccessService accessService;

    public EnvelopeUpdateService(
        EnvelopeRepository envelopeRepo,
        ProcessEventRepository eventRepo,
        EnvelopeAccessService accessService
    ) {
        this.envelopeRepo = envelopeRepo;
        this.eventRepo = eventRepo;
        this.accessService = accessService;
    }

    /**
     * Changes the status of envelope to DOC_CONSUMED.
     *
     * @param envelopeId  ID of the envelope to update
     * @param serviceName Name of the service that requests to mark the envelope as consumed
     */
    public void markAsConsumed(UUID envelopeId, String serviceName) {
        Envelope envelope =
            envelopeRepo
                .findById(envelopeId)
                .orElseThrow(() -> new EnvelopeNotFoundException());

        accessService.assertCanUpdate(envelope.getJurisdiction(), serviceName);

        envelope.setStatus(Event.DOC_CONSUMED);
        envelopeRepo.save(envelope);

        eventRepo.save(new ProcessEvent(envelope.getContainer(), envelope.getZipFileName(), Event.DOC_CONSUMED));
    }
}
