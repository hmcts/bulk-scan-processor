package uk.gov.hmcts.reform.bulkscanprocessor.services.errornotifications;

import com.google.common.collect.ImmutableMap;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.InvalidMetafileException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.OcrDataParseException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.PaymentsDisabledException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ServiceDisabledException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ZipFileProcessingFailedException;
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
            .put(InvalidMetafileException.class, ERR_METAFILE_INVALID)
            .put(ZipFileProcessingFailedException.class, ERR_ZIP_PROCESSING_FAILED)
            .put(OcrDataParseException.class, ERR_METAFILE_INVALID)
            .put(PaymentsDisabledException.class, ERR_PAYMENTS_DISABLED)
            .put(ServiceDisabledException.class, ERR_SERVICE_DISABLED)
            .build();

    private ErrorMapping() {
        // util class
    }

    public static Optional<ErrorCode> getFor(Class<?> clazz) {
        return mapping.entrySet()
            .stream()
            .filter(entry -> clazz.isInstance(entry.getKey()))
            .map(Map.Entry::getValue)
            .findFirst();
    }
}
