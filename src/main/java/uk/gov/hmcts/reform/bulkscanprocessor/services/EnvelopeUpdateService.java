package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeNotFoundException;

import java.util.UUID;

@Service
public class EnvelopeUpdateService {

    private final EnvelopeRepository envelopeRepo;
    private final ProcessEventRepository eventRepo;
    private final EnvelopeAccessService accessService;
    private final EnvelopeStatusChangeValidator statusChangeValidator;

    public EnvelopeUpdateService(
        EnvelopeRepository envelopeRepo,
        ProcessEventRepository eventRepo,
        EnvelopeAccessService accessService,
        EnvelopeStatusChangeValidator statusChangeValidator
    ) {
        this.envelopeRepo = envelopeRepo;
        this.eventRepo = eventRepo;
        this.accessService = accessService;
        this.statusChangeValidator = statusChangeValidator;
    }

    public void updateStatus(UUID envelopeId, Status newStatus, String serviceName) {

        Envelope envelope =
            envelopeRepo
                .findById(envelopeId)
                .orElseThrow(() -> new EnvelopeNotFoundException());

        accessService.assertCanUpdate(envelope.getJurisdiction(), serviceName);
        statusChangeValidator.assertCanUpdate(envelope.getStatus(), newStatus);

        envelope.setStatus(newStatus);
        envelopeRepo.save(envelope);

        eventRepo.save(new ProcessEvent(envelope.getContainer(), envelope.getZipFileName(), Event.DOC_CONSUMED));

    }
}
