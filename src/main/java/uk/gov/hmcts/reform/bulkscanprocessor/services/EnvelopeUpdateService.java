package uk.gov.hmcts.reform.bulkscanprocessor.services;

import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;

import java.util.Map;
import java.util.UUID;

@Service
public class EnvelopeUpdateService {

    private static final Logger log = LoggerFactory.getLogger(EnvelopeUpdateService.class);

    public static final Map<Status, Event> eventForStatusChange =
        ImmutableMap.of(
            Status.CONSUMED, Event.DOC_CONSUMED
        );

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
        log.info("Updated envelope {} status from {} to {}.", envelope.getId(), envelope.getStatus(), newStatus);

        Event event = eventForStatusChange.get(newStatus);
        if (event != null) {
            eventRepo.save(new ProcessEvent(
                envelope.getContainer(),
                envelope.getZipFileName(),
                event
            ));
        }
    }
}
