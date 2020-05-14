package uk.gov.hmcts.reform.bulkscanprocessor.services.errornotifications;

import com.google.common.collect.ImmutableMap;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ContainerJurisdictionPoBoxMismatchException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.DuplicateDocumentControlNumberException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.DuplicateDocumentControlNumbersInEnvelopeException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.FileNameIrregularitiesException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.InvalidEnvelopeSchemaException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.InvalidZipEntriesException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.MetadataNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.NonPdfFileFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.OcrDataNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.OcrDataParseException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.OcrPresenceException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.OcrValidationException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.PaymentsDisabledException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ServiceDisabledException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ZipNameNotMatchingMetaDataException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode;

import java.util.Map;
import java.util.Optional;

import static uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode.ERR_METAFILE_INVALID;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode.ERR_PAYMENTS_DISABLED;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode.ERR_SERVICE_DISABLED;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode.ERR_ZIP_PROCESSING_FAILED;

/**
 * Stores mapping between exceptions and ErrorCodes.
 */
public final class ErrorMapping {

    private static final Map<Class<?>, ErrorCode> mapping =
        new ImmutableMap.Builder<Class<?>, ErrorCode>()
            .put(InvalidEnvelopeSchemaException.class, ERR_METAFILE_INVALID)
            .put(ZipNameNotMatchingMetaDataException.class, ERR_METAFILE_INVALID)
            .put(FileNameIrregularitiesException.class, ERR_METAFILE_INVALID)
            .put(DuplicateDocumentControlNumberException.class, ERR_ZIP_PROCESSING_FAILED)
            .put(DuplicateDocumentControlNumbersInEnvelopeException.class, ERR_METAFILE_INVALID)
            .put(OcrDataNotFoundException.class, ERR_METAFILE_INVALID)
            .put(NonPdfFileFoundException.class, ERR_ZIP_PROCESSING_FAILED)
            .put(OcrDataParseException.class, ERR_METAFILE_INVALID)
            .put(MetadataNotFoundException.class, ERR_ZIP_PROCESSING_FAILED)
            .put(InvalidZipEntriesException.class, ERR_ZIP_PROCESSING_FAILED)
            .put(ContainerJurisdictionPoBoxMismatchException.class, ERR_METAFILE_INVALID)
            .put(OcrValidationException.class, ERR_METAFILE_INVALID)
            .put(OcrPresenceException.class, ERR_METAFILE_INVALID)
            .put(PaymentsDisabledException.class, ERR_PAYMENTS_DISABLED)
            .put(ServiceDisabledException.class, ERR_SERVICE_DISABLED)
            .build();

    private ErrorMapping() {
        // util class
    }

    public static Optional<ErrorCode> getFor(Class<?> clazz) {
        return Optional.ofNullable(mapping.get(clazz));
    }
}
