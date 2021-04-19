package uk.gov.hmcts.reform.bulkscanprocessor.services.zipfilestatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItemRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.zipfilestatus.ZipFileEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.zipfilestatus.ZipFileEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.zipfilestatus.ZipFileStatus;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.mapper.EnvelopeResponseMapper.toNonScannableItemsResponse;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.mapper.EnvelopeResponseMapper.toPaymentsResponse;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.mapper.EnvelopeResponseMapper.toScannableItemsResponse;

@Service
public class ZipFileStatusService {
    private static final Logger log = LoggerFactory.getLogger(ZipFileStatusService.class);
    private final ProcessEventRepository eventRepo;
    private final EnvelopeRepository envelopeRepo;
    private final ScannableItemRepository scannableItemRepo;

    // region constructor

    public ZipFileStatusService(
        ProcessEventRepository eventRepo,
        EnvelopeRepository envelopeRepo,
        ScannableItemRepository scannableItemRepo
    ) {
        this.eventRepo = eventRepo;
        this.envelopeRepo = envelopeRepo;
        this.scannableItemRepo = scannableItemRepo;
    }

    // endregion

    public ZipFileStatus getStatusFor(String zipFileName) {
        List<Envelope> envelopes = envelopeRepo.findByZipFileName(zipFileName);
        List<ProcessEvent> events = eventRepo.findByZipFileName(zipFileName);
        return getZipFileStatus(zipFileName, null, envelopes, events);
    }

    public List<ZipFileStatus> getStatusByDcn(
        String documentControlNumber,
        int minLength
    ) throws InvalidParameterException {

        if (documentControlNumber.length() < minLength) {
            log.error("Exception in Search by DCN error: DCN number specified is less than"
                          + minLength + " characters in length.");
            throw new InvalidParameterException("DCN number has to be at least " + minLength + " characters long");
        }

        List<String> zipFileNames = scannableItemRepo.findByDcn(documentControlNumber);
        List<ZipFileStatus> zipFileStatusList = new ArrayList<>();

        zipFileNames.stream().forEach(
            zipFileName ->
                zipFileStatusList.add(
                    getZipFileStatus(
                        zipFileName,
                        null,
                        envelopeRepo.findByZipFileName(zipFileName),
                        eventRepo.findByZipFileName(zipFileName)
                    )
                )
        );

        return zipFileStatusList;
    }

    public ZipFileStatus getStatusByCcdId(String ccdId) {
        List<Envelope> envelopes = envelopeRepo.findByCcdId(ccdId);
        if (envelopes.size() > 0) {
            String zipFileName = envelopes.get(0).getZipFileName();
            List<ProcessEvent> events = eventRepo.findByZipFileName(zipFileName);
            return getZipFileStatus(null, ccdId, envelopes, events);
        }
        return getZipFileStatus(null, ccdId, emptyList(), emptyList());
    }

    private ZipFileStatus getZipFileStatus(
        String fileName,
        String ccdId,
        List<Envelope> envelopes,
        List<ProcessEvent> events
    ) {
        return new ZipFileStatus(
            fileName,
            ccdId,
            envelopes.stream().map(this::mapEnvelope).collect(toList()),
            events.stream().map(this::mapEvent).collect(toList())
        );
    }

    private ZipFileEnvelope mapEnvelope(Envelope envelope) {
        return new ZipFileEnvelope(
            envelope.getId().toString(),
            envelope.getContainer(),
            envelope.getStatus().name(),
            envelope.getCcdId(),
            envelope.getEnvelopeCcdAction(),
            envelope.getZipFileName(),
            envelope.isZipDeleted(),
            envelope.getRescanFor(),
            envelope.getClassification(),
            envelope.getJurisdiction(),
            envelope.getCaseNumber(),
            toScannableItemsResponse(envelope.getScannableItems()),
            toNonScannableItemsResponse(envelope.getNonScannableItems()),
            toPaymentsResponse(envelope.getPayments())
        );
    }

    private ZipFileEvent mapEvent(ProcessEvent event) {
        return new ZipFileEvent(
            event.getEvent().name(),
            event.getContainer(),
            event.getCreatedAt(),
            event.getReason()
        );
    }
}
