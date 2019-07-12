package uk.gov.hmcts.reform.bulkscanprocessor.validation;

import joptsimple.internal.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.bulkscanprocessor.config.ContainerMappings;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.OcrValidationException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.OcrValidationClient;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.model.req.FormData;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.model.req.OcrDataField;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.model.res.ValidationResponse;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

@Component
@EnableConfigurationProperties(ContainerMappings.class)
public class OcrValidator {

    private static final Logger log = LoggerFactory.getLogger(OcrValidator.class);

    private final OcrValidationClient client;
    private final ContainerMappings containerMappings;
    private final AuthTokenGenerator authTokenGenerator;

    //region constructor
    public OcrValidator(
        OcrValidationClient client,
        ContainerMappings containerMappings,
        AuthTokenGenerator authTokenGenerator
    ) {
        this.client = client;
        this.containerMappings = containerMappings;
        this.authTokenGenerator = authTokenGenerator;
    }
    //endregion

    public void assertIsValid(InputEnvelope envelope) {
        getValidationUrl(envelope).ifPresent(url -> {
            List<InputScannableItem> docsWithOcr =
                envelope
                    .scannableItems
                    .stream()
                    .filter(it -> it.ocrData != null)
                    .collect(toList());

            if (!docsWithOcr.isEmpty()) {
                if (docsWithOcr.size() > 1) {
                    log.warn(
                        "Multiple documents with OCR found. Envelope: {}, count: {}",
                        envelope.zipFileName,
                        docsWithOcr.size()
                    );
                }

                // TODO: handle exceptions
                ValidationResponse result =
                    client.validate(
                        url,
                        toFormData(docsWithOcr.get(0)),
                        authTokenGenerator.generate()
                    );

                switch (result.status) {
                    case ERRORS:
                        throw new OcrValidationException(Strings.join(result.errors, ", "));
                    case WARNINGS:
                        log.info("OCR succeeded with errors. Envelope: {}", envelope.zipFileName);
                        break;
                    default:
                        break;
                }
            }
        });

    }

    private Optional<String> getValidationUrl(InputEnvelope envelope) {
        return containerMappings
            .getMappings()
            .stream()
            .filter(mapping -> Objects.equals(mapping.getPoBox(), envelope.poBox))
            .findFirst()
            .map(mapping -> mapping.getOcrValidationUrl())
            .filter(target -> !Strings.isNullOrEmpty(target));
    }

    private FormData toFormData(InputScannableItem doc) {
        return new FormData(
            doc.documentSubtype,
            doc
                .ocrData
                .getFields()
                .stream()
                .map(it -> new OcrDataField(
                    it.name.asText(),
                    it.value.textValue()
                ))
                .collect(toList())
        );
    }
}
