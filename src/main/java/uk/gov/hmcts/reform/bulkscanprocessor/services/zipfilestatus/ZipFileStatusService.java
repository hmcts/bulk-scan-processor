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
import uk.gov.hmcts.reform.bulkscanprocessor.services.email.ReportSender;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.mapper.EnvelopeResponseMapper.toNonScannableItemsResponse;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.mapper.EnvelopeResponseMapper.toPaymentsResponse;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.mapper.EnvelopeResponseMapper.toScannableItemsResponse;

@Service
public class ZipFileStatusService {
    private static final Logger log = LoggerFactory.getLogger(ReportSender.class);
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

        return new ZipFileStatus(
            zipFileName,
            envelopes.stream().map(this::mapEnvelope).collect(toList()),
            events.stream().map(this::mapEvent).collect(toList())
        );
    }

    public List<ZipFileStatus> getStatusByDcn(String documentControllNumber) throws InvalidParameterException {

        if (documentControllNumber.length() < 6) {
            log.error("Exception in Search by DCN error: DCN number specified is less than 6 characters in length.");
            throw new InvalidParameterException("DCN number has to be at least 6 characters long");
        }

        List<String> zipFileNames = scannableItemRepo.findByDcn(documentControllNumber);
        List<ZipFileStatus> zipFileStatusList = new ArrayList<>();

        zipFileNames.stream().forEach(
            zipFileName ->
                zipFileStatusList.add(
                    new ZipFileStatus(
                        zipFileName,
                        envelopeRepo.findByZipFileName(zipFileName).stream().map(this::mapEnvelope).collect(toList()),
                        eventRepo.findByZipFileName(zipFileName).stream().map(this::mapEvent).collect(toList())
                    )
                )
        );

        return zipFileStatusList;
    }

    private ZipFileEnvelope mapEnvelope(Envelope envelope) {
        return new ZipFileEnvelope(
            envelope.getId().toString(),
            envelope.getContainer(),
            envelope.getStatus().name(),
            envelope.getCcdId(),
            envelope.getEnvelopeCcdAction(),
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
