package uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.FileNameIrregularitiesException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.MetadataNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.PreviouslyFailedToUploadException;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;
import uk.gov.hmcts.reform.bulkscanprocessor.validation.MetafileJsonValidator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.UPLOAD_FAILURE;

@Component
public class EnvelopeProcessor {
    private static final Logger log = LoggerFactory.getLogger(EnvelopeProcessor.class);

    private final MetafileJsonValidator schemaValidator;
    private final EnvelopeRepository envelopeRepository;
    private final ProcessEventRepository processEventRepository;
    private final int reUploadBatchSize;
    private final int maxReuploadTriesCount;

    public EnvelopeProcessor(
        MetafileJsonValidator schemaValidator,
        EnvelopeRepository envelopeRepository,
        ProcessEventRepository processEventRepository,
        @Value("${scheduling.task.reupload.batch}") int reUploadBatchSize,
        @Value("${scheduling.task.reupload.max_tries}") int maxReuploadTriesCount
    ) {
        this.schemaValidator = schemaValidator;
        this.envelopeRepository = envelopeRepository;
        this.processEventRepository = processEventRepository;
        this.reUploadBatchSize = reUploadBatchSize;
        this.maxReuploadTriesCount = maxReuploadTriesCount;
    }

    public Envelope parseEnvelope(byte[] metadataStream, String zipFileName) throws IOException, ProcessingException {
        if (Objects.isNull(metadataStream)) {
            throw new MetadataNotFoundException("No metadata file found in the zip file");
        }

        schemaValidator.validate(metadataStream, zipFileName);

        return schemaValidator.parseMetafile(metadataStream);
    }

    /**
     * Assert envelope did not fail to be uploaded in the past.
     * Throws exception otherwise.
     *
     * @param envelope details to check against.
     */
    public void assertDidNotFailToUploadBefore(Envelope envelope) throws PreviouslyFailedToUploadException {
        List<Envelope> envelopes = envelopeRepository.findRecentEnvelopes(
            envelope.getContainer(),
            envelope.getZipFileName(),
            UPLOAD_FAILURE,
            PageRequest.of(0, 1)
        );

        if (envelopes.size() == 1) {
            Envelope failedEnvelope = envelopes.get(0);

            throw new PreviouslyFailedToUploadException(
                String.format(
                    "Envelope %s created at %s is already marked as failed to upload. Skipping",
                    failedEnvelope.getId(),
                    failedEnvelope.getCreatedAt()
                )
            );
        }
    }

    /**
     * Check blob did not fail to be deleted before. This means that
     * processing is complete and an envelope has already been created as
     * blob deletion is the last processing step.
     *
     */
    public Envelope getEnvelopeByFileAndContainer(String container, String zipFileName) {
        return envelopeRepository.findEnvelopesByFileAndContainer(
            container,
            zipFileName,
            PageRequest.of(0, 1)
        )
        .stream()
        .findFirst()
        .orElse(null);
    }

    // TODO: move to separate class.
    /**
     * Assert given envelope has scannable items exactly matching
     * the filenames with list of pdfs acquired from zip file.
     * In case there is a mismatch an exception is thrown.
     *
     * @param envelope to assert against
     * @param pdfs     to assert against
     */
    public static void assertEnvelopeHasPdfs(Envelope envelope, List<Pdf> pdfs) throws FileNameIrregularitiesException {
        Set<String> scannedFileNames = envelope
            .getScannableItems()
            .stream()
            .map(ScannableItem::getFileName)
            .collect(Collectors.toSet());

        Set<String> pdfFileNames = pdfs
            .stream()
            .map(Pdf::getFilename)
            .collect(Collectors.toSet());

        Set<String> missingActualPdfFiles = Sets.difference(scannedFileNames, pdfFileNames);
        Set<String> notDeclaredPdfs = Sets.difference(pdfFileNames, scannedFileNames);

        List<String> problems = new ArrayList<>();

        if (!notDeclaredPdfs.isEmpty()) {
            problems.add("Not declared PDFs: " + String.join(", ", notDeclaredPdfs));
        }

        if (!missingActualPdfFiles.isEmpty()) {
            problems.add("Missing PDFs: " + String.join(", ", missingActualPdfFiles));
        }

        if (!problems.isEmpty()) {
            throw new FileNameIrregularitiesException(String.join(". ", problems));
        }
    }

    public Envelope saveEnvelope(Envelope envelope) {
        Envelope dbEnvelope = envelopeRepository.save(envelope);

        log.info("Envelope for jurisdiction {} and zip file name {} successfully saved in database.",
            envelope.getJurisdiction(),
            envelope.getZipFileName()
        );

        return dbEnvelope;
    }

    public List<Envelope> getFailedToUploadEnvelopes(String jurisdiction) {
        return envelopeRepository.findEnvelopesToResend(
            jurisdiction,
            maxReuploadTriesCount,
            reUploadBatchSize > 0 ? PageRequest.of(0, reUploadBatchSize) : null
        );
    }

    public void handleEvent(Envelope envelope, Event event) {
        processEventRepository.save(
            new ProcessEvent(envelope.getContainer(), envelope.getZipFileName(), event)
        );

        Status.fromEvent(event).ifPresent(status -> {
            envelope.setStatus(status);
            envelopeRepository.save(envelope);
        });
    }

}
