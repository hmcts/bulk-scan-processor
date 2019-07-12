package uk.gov.hmcts.reform.bulkscanprocessor.validation;

import joptsimple.internal.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.OcrValidationException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.OcrValidationClient;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.model.req.FormData;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.model.req.OcrDataField;
import uk.gov.hmcts.reform.bulkscanprocessor.ocrvalidation.client.model.res.ValidationResponse;

import java.util.List;

import static java.util.stream.Collectors.toList;

@Service
public class OcrValidator {

    private static final Logger log = LoggerFactory.getLogger(OcrValidator.class);

    private final OcrValidationClient client;

    public OcrValidator(OcrValidationClient client) {
        this.client = client;
    }

    public void assertIsValid(InputEnvelope envelope) {
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

            ValidationResponse result =
                client.validate(
                    "url", // TODO
                    toFormData(docsWithOcr.get(0)),
                    "token" // TODO
                );

            switch (result.status) {
                case ERRORS:
                    throw new OcrValidationException(Strings.join(result.errors, ", "));
                case WARNINGS:
                    log.info("OCR succeeded with errors. Envelope: {}", envelope.zipFileName);
                    break;
                case SUCCESS:
                    break;
            }
        }
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
