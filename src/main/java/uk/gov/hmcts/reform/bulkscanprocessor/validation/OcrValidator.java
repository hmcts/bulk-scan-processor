package uk.gov.hmcts.reform.bulkscanprocessor.validation;

import io.vavr.control.Try;
import joptsimple.internal.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.util.Optionals;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException.NotFound;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.bulkscanprocessor.config.ContainerMappings;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.OcrValidationException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputDocumentType;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.OcrValidationClient;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.model.req.FormData;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.model.req.OcrDataField;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.model.res.ValidationResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.validation.model.OcrValidationWarnings;

import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification.EXCEPTION;

/**
 * Validates OCR data of the given envelope.
 */
@Component
@EnableConfigurationProperties(ContainerMappings.class)
public class OcrValidator {

    private static final Logger log = LoggerFactory.getLogger(OcrValidator.class);

    private final OcrValidationClient client;
    private final OcrPresenceValidator presenceValidator;
    private final ContainerMappings containerMappings;
    private final AuthTokenGenerator authTokenGenerator;

    /**
     * Constructor for the OcrValidator.
     * @param client OCR validation client
     * @param presenceValidator OCR presence validator
     * @param containerMappings mappings of PO boxes to OCR validation URLs
     * @param authTokenGenerator auth token generator
     */
    public OcrValidator(
        OcrValidationClient client,
        OcrPresenceValidator presenceValidator,
        ContainerMappings containerMappings,
        AuthTokenGenerator authTokenGenerator
    ) {
        this.client = client;
        this.presenceValidator = presenceValidator;
        this.containerMappings = containerMappings;
        this.authTokenGenerator = authTokenGenerator;
    }

    /**
     * If required, validates the OCR data of the given envelope.
     *
     * @return Warnings for valid OCR data, to be displayed to the caseworker. Empty if
     *         no validation took place.
     * @throws OcrValidationException if the OCR data is invalid
     */
    public Optional<OcrValidationWarnings> assertOcrDataIsValid(
        InputEnvelope envelope
    ) {
        if (envelope.classification == EXCEPTION) {
            return Optional.empty();
        }

        return Optionals.mapIfAllPresent(
            findDocWithOcr(envelope),
            findValidationUrl(envelope.poBox),
            (docWithOcr, validationUrl) ->
                Try
                    .of(() -> client.validate(
                        validationUrl,
                        toFormData(docWithOcr),
                        getFormType(docWithOcr),
                        authTokenGenerator.generate()
                    ))
                    .onSuccess(res -> handleValidationResponse(res, envelope, docWithOcr))
                    .onFailure(exc -> handleRestClientException(exc, validationUrl, envelope, docWithOcr))
                    .map(response -> ocrValidationWarnings(docWithOcr, response.warnings))
                    .getOrElseGet(exc ->
                        ocrValidationWarnings(
                            docWithOcr,
                            singletonList("OCR validation was not performed due to errors")
                        )
                    )
        );
    }

    /**
     * If required, validates the OCR data of the given envelope.
     * @param scannableItem the scannable item to validate
     * @param warnings the warnings
     * @return Warnings for valid OCR data
     * @throws OcrValidationException if the OCR data is invalid
     */
    private OcrValidationWarnings ocrValidationWarnings(InputScannableItem scannableItem, List<String> warnings) {
        log.info("Case validation warnings found: {}", warnings);
        return new OcrValidationWarnings(
            scannableItem.documentControlNumber,
            warnings != null ? warnings : emptyList()
        );
    }

    /**
     * Handles the validation response.
     * @param res the validation response
     * @param envelope the envelope
     * @param docWithOcr the document with OCR
     */
    private void handleValidationResponse(
        ValidationResponse res,
        InputEnvelope envelope,
        InputScannableItem docWithOcr
    ) {
        log.info("Validation result for envelope {}: {}", envelope.zipFileName, res.status);

        if (res.status == null) {
            log.error(
                "Validation ended with null response status. File name: {}",
                envelope.zipFileName
            );
            return;
        }

        switch (res.status) {
            case ERRORS:
                String message = "OCR validation service returned OCR-specific errors. "
                    + "Document control number: " + docWithOcr.documentControlNumber + ". "
                    + "Envelope: " + envelope.zipFileName + ".";
                throw new OcrValidationException(
                    message,
                    "OCR fields validation failed. Envelope: "
                        + envelope.zipFileName + " Validation errors: " + res.errors
                );
            case WARNINGS:
                log.info(
                    "Validation ended with warnings: {}, File name: {}",
                    res.warnings, envelope.zipFileName
                );
                break;
            default:
                break;
        }
    }

    /**
     * Handles the rest client exception.
     * @param exc the exception
     * @param validationUrl the validation URL
     * @param envelope the envelope
     * @param docWithOcr the document with OCR
     * @throws OcrValidationException if the OCR data is invalid
     */
    private void handleRestClientException(
        Throwable exc,
        String validationUrl,
        InputEnvelope envelope,
        InputScannableItem docWithOcr
    ) {
        log.error(
            "Error calling validation endpoint. Url: {}, DCN: {}, doc type: {}, doc subtype: {}, envelope: {}",
            validationUrl,
            docWithOcr.documentControlNumber,
            docWithOcr.documentType,
            docWithOcr.documentSubtype,
            envelope.zipFileName,
            exc
        );

        if (exc instanceof NotFound) {
            throw new OcrValidationException("Unrecognised document subtype " + docWithOcr.documentSubtype);
        }
    }

    /**
     * Finds the OCR validation URL for the given PO box.
     * @param poBox the PO box
     * @return the OCR validation URL
     */
    private Optional<String> findValidationUrl(String poBox) {
        Optional<String> validationUrl = containerMappings
            .getMappings()
            .stream()
            .filter(mapping -> mapping.getPoBoxes().stream().map(String::toLowerCase).collect(toList())
                    .contains(poBox.toLowerCase()))
            .findFirst()
            .map(ContainerMappings.Mapping::getOcrValidationUrl)
            .filter(url -> !Strings.isNullOrEmpty(url));

        if (validationUrl.isEmpty()) {
            log.info("OCR validation URL for po box {} not configured. Skipping validation.", poBox);
        }

        return validationUrl;
    }

    /**
     * Finds the document with OCR data.
     * @param envelope the envelope
     * @return the document with OCR data
     */
    private Optional<InputScannableItem> findDocWithOcr(InputEnvelope envelope) {
        return presenceValidator.assertHasProperlySetOcr(envelope.scannableItems);
    }

    /**
     * Converts the input scannable item to form data.
     * @param doc the input scannable item
     * @return the form data
     */
    private FormData toFormData(InputScannableItem doc) {
        return new FormData(
            doc
                .ocrData
                .getFields()
                .stream()
                .map(it -> new OcrDataField(it.name.asText(), it.value.textValue()))
                .collect(toList())
        );
    }

    /**
     * Gets the form type.
     * @param item the input scannable item
     * @return the form type
     */
    private String getFormType(InputScannableItem item) {
        return item.documentType == InputDocumentType.SSCS1 ? "SSCS1" : item.documentSubtype;
    }
}
