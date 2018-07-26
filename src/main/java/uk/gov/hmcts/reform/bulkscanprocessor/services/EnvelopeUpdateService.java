package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeNotFoundException;

import java.util.UUID;

@Service
public class EnvelopeUpdateService {

    private final EnvelopeRepository repo;
    private final EnvelopeAccessService accessService;

    public EnvelopeUpdateService(
        EnvelopeRepository repo,
        EnvelopeAccessService accessService
    ) {
        this.repo = repo;
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
            repo
                .findById(envelopeId)
                .orElseThrow(() -> new EnvelopeNotFoundException());

        accessService.assertCanUpdate(envelope.getJurisdiction(), serviceName);

        envelope.setStatus(Event.DOC_CONSUMED);
        repo.saveAndFlush(envelope);
    }
}
