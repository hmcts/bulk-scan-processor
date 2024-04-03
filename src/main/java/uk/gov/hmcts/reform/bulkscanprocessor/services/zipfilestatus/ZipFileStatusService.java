package uk.gov.hmcts.reform.bulkscanprocessor.services.zipfilestatus;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItemRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.zipfilestatus.ZipFileEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.zipfilestatus.ZipFileEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.zipfilestatus.ZipFileStatus;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.mapper.EnvelopeResponseMapper.toNonScannableItemsResponse;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.mapper.EnvelopeResponseMapper.toPaymentsResponse;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.mapper.EnvelopeResponseMapper.toScannableItemsResponse;

/**
 * Service to get the status of a zip file.
 */
@Service
public class ZipFileStatusService {
    private final ProcessEventRepository eventRepo;
    private final EnvelopeRepository envelopeRepo;
    private final ScannableItemRepository scannableItemRepo;

    /**
     * Constructor for the ZipFileStatusService.
     * @param eventRepo The repository for process event
     * @param envelopeRepo The repository for envelope
     * @param scannableItemRepo The repository for scannable item
     */
    public ZipFileStatusService(
        ProcessEventRepository eventRepo,
        EnvelopeRepository envelopeRepo,
        ScannableItemRepository scannableItemRepo
    ) {
        this.eventRepo = eventRepo;
        this.envelopeRepo = envelopeRepo;
        this.scannableItemRepo = scannableItemRepo;
    }

    /**
     * Get the status of a zip file by file name.
     * @param zipFileName The zip file name
     * @return The status of the zip file
     */
    public ZipFileStatus getStatusByFileName(String zipFileName) {
        List<Envelope> envelopes = envelopeRepo.findByZipFileName(zipFileName);
        List<ProcessEvent> events = eventRepo.findByZipFileNameOrderByCreatedAtDesc(zipFileName);
        return getZipFileStatus(zipFileName, null, null, envelopes, events);
    }

    /**
     * Get the status of a zip file by document control number.
     * @param documentControlNumber The document control number
     * @return List of statuses of the zip file
     */
    public List<ZipFileStatus> getStatusByDcn(String documentControlNumber) {

        List<String> zipFileNames = scannableItemRepo.findByDcn(documentControlNumber);
        List<ZipFileStatus> zipFileStatusList = new ArrayList<>();
        if (!zipFileNames.isEmpty()) {
            zipFileNames.forEach(
                zipFileName ->
                zipFileStatusList.add(
                    getZipFileStatus(
                        null,
                        null,
                        documentControlNumber,
                        envelopeRepo.findByZipFileName(zipFileName),
                        eventRepo.findByZipFileNameOrderByCreatedAtDesc(zipFileName)
                    )
                )
            );
            return zipFileStatusList;
        }
        return singletonList(
                new ZipFileStatus(null, null, documentControlNumber, emptyList(), emptyList())
        );
    }

    /**
     * Get the status of a zip file by ccd id.
     * @param ccdId The ccd id
     * @return The status of the zip file
     */
    public ZipFileStatus getStatusByCcdId(String ccdId) {
        List<Envelope> envelopes = envelopeRepo.findByCcdId(ccdId);
        if (!envelopes.isEmpty()) {
            String zipFileName = envelopes.get(0).getZipFileName();
            List<ProcessEvent> events = eventRepo.findByZipFileNameOrderByCreatedAtDesc(zipFileName);
            return getZipFileStatus(null, ccdId, null, envelopes, events);
        }
        return getZipFileStatus(null, ccdId, null, emptyList(), emptyList());
    }

    /**
     * Get the status of a zip file by container.
     * @param fileName The file name
     * @param ccdId The ccd id
     * @param dcn The document control number
     * @param envelopes The envelopes
     * @param events The events
     * @return The status of the zip file
     */
    private ZipFileStatus getZipFileStatus(
        String fileName,
        String ccdId,
        String dcn,
        List<Envelope> envelopes,
        List<ProcessEvent> events
    ) {
        return new ZipFileStatus(
            fileName,
            ccdId,
            dcn,
            envelopes.stream().map(this::mapEnvelope).collect(toList()),
            events.stream().map(this::mapEvent).collect(toList())
        );
    }

    /**
     * Map the envelope to the zip file envelope.
     * @param envelope The envelope
     * @return The zip file envelope
     */
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
            envelope.getCreatedAt(),
            envelope.getDeliveryDate(),
            envelope.getOpeningDate(),
            toScannableItemsResponse(envelope.getScannableItems()),
            toNonScannableItemsResponse(envelope.getNonScannableItems()),
            toPaymentsResponse(envelope.getPayments())
        );
    }

    /**
     * Map the process event to the response.
     * @param event The process event
     * @return The zip file event
     */
    private ZipFileEvent mapEvent(ProcessEvent event) {
        return new ZipFileEvent(
            event.getEvent().name(),
            event.getContainer(),
            event.getCreatedAt(),
            event.getReason()
        );
    }
}
