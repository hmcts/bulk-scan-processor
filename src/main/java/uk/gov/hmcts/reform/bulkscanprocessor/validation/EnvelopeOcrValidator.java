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
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.OcrValidationException;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.OcrValidationClient;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.model.req.FormData;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.model.req.OcrDataField;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.model.res.ValidationResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.validation.model.OcrValidationWarnings;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification.EXCEPTION;

@Component
@EnableConfigurationProperties(ContainerMappings.class)
public class EnvelopeOcrValidator {

    private static final Logger log = LoggerFactory.getLogger(EnvelopeOcrValidator.class);

    private final OcrValidationClient client;
    private final ContainerMappings containerMappings;
    private final AuthTokenGenerator authTokenGenerator;

    //region constructor
    public EnvelopeOcrValidator(
        OcrValidationClient client,
        ContainerMappings containerMappings,
        AuthTokenGenerator authTokenGenerator
    ) {
        this.client = client;
        this.containerMappings = containerMappings;
        this.authTokenGenerator = authTokenGenerator;
    }
    //endregion

    /**
     * If required, validates the OCR data of the given envelope.
     *
     * @return Warnings for valid OCR data, to be displayed to the caseworker. Empty if
     *         no validation took place.
     * @throws OcrValidationException if the OCR data is invalid
     */
    public Optional<OcrValidationWarnings> assertOcrDataIsValid(Envelope envelope) {
        if (envelope.getClassification() == EXCEPTION) {
            return Optional.empty();
        }

        return Optionals.mapIfAllPresent(
            findDocWithOcr(envelope.getScannableItems()),
            findValidationUrl(envelope.getPoBox()),
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

    private OcrValidationWarnings ocrValidationWarnings(ScannableItem scannableItem, List<String> warnings) {
        return new OcrValidationWarnings(
            scannableItem.getDocumentControlNumber(),
            warnings != null ? warnings : emptyList()
        );
    }

    private void handleValidationResponse(
        ValidationResponse res,
        Envelope envelope,
        ScannableItem docWithOcr
    ) {
        log.info("Validation result for envelope {}: {}", envelope.getZipFileName(), res.status);
        switch (res.status) {
            case ERRORS:
                String message = "OCR validation service returned OCR-specific errors. "
                    + "Document control number: " + docWithOcr.getDocumentControlNumber() + ". "
                    + "Envelope: " + envelope.getZipFileName() + ".";
                throw new OcrValidationException(
                    message,
                    "OCR fields validation failed. Validation errors: " + res.errors
                );
            case WARNINGS:
                log.info(
                    "Validation ended with warnings. File name: {}",
                    envelope.getZipFileName()
                );
                break;
            default:
                break;
        }
    }

    private void handleRestClientException(
        Throwable exc,
        String validationUrl,
        Envelope envelope,
        ScannableItem docWithOcr
    ) {
        log.error(
            "Error calling validation endpoint. Url: {}, DCN: {}, doc type: {}, doc subtype: {}, envelope: {}",
            validationUrl,
            docWithOcr.getDocumentControlNumber(),
            docWithOcr.getDocumentType(),
            docWithOcr.getDocumentSubtype(),
            envelope.getZipFileName(),
            exc
        );

        if (exc instanceof NotFound) {
            throw new OcrValidationException("Unrecognised document subtype " + docWithOcr.getDocumentSubtype());
        }
    }

    private Optional<String> findValidationUrl(String poBox) {
        Optional<String> validationUrl = containerMappings
            .getMappings()
            .stream()
            .filter(mapping -> Objects.equals(mapping.getPoBox(), poBox))
            .findFirst()
            .map(ContainerMappings.Mapping::getOcrValidationUrl)
            .filter(url -> !Strings.isNullOrEmpty(url));

        if (validationUrl.isEmpty()) {
            log.info("OCR validation URL for po box {} not configured. Skipping validation.", poBox);
        }

        return validationUrl;
    }

    private Optional<ScannableItem> findDocWithOcr(List<ScannableItem> docs) {
        return docs
            .stream()
            .filter(it -> it.getOcrData() != null)
            .findFirst();
    }

    private FormData toFormData(ScannableItem doc) {
        return new FormData(
            doc
                .getOcrData()
                .fields
                .stream()
                .map(it -> new OcrDataField(it.name.asText(), it.value.textValue()))
                .collect(toList())
        );
    }

    private String getFormType(ScannableItem item) {
        return item.getDocumentSubtype();
    }
}
