package uk.gov.hmcts.reform.bulkscanprocessor.services.wrapper;

import com.microsoft.azure.storage.StorageException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.ErrorHandler;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.BlobDeleteFailureException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.DocFailureGenericException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.DocUploadFailureGenericException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeAwareThrowable;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EventRelatedThrowable;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.LeaseAlreadyPresentException;

@Component
public class ErrorHandlingWrapper {

    private final ErrorHandler errorHandler;

    // custom supplier which marks getter as faulty
    @FunctionalInterface
    public interface Supplier<T> {
        T get() throws Exception;
    }

    public ErrorHandlingWrapper(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    public <T> T wrapDocFailure(String containerName, String zipFileName, Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception exception) {
            if (exception instanceof EventRelatedThrowable) {
                errorHandler.handleError(exception);
            } else {
                errorHandler.handleError(new DocFailureGenericException(containerName, zipFileName, exception));
            }

            return null;
        }
    }

    /**
     * Wraps docs upload to handles related errors.
     *
     * @param envelope for the documents to upload
     * @param supplier upload function
     * @return true if upload successful
     */
    public Boolean wrapDocUploadFailure(Envelope envelope, Supplier<Boolean> supplier) {
        try {
            return supplier.get();
        } catch (Exception exception) {
            if (exception instanceof EnvelopeAwareThrowable) {
                errorHandler.handleError(exception);
            } else {
                errorHandler.handleError(new DocUploadFailureGenericException(envelope, exception));
            }
            return Boolean.FALSE;
        }
    }

    public <T> T wrapAcquireLeaseFailure(
        String containerName,
        String zipFileName,
        Supplier<T> supplier
    ) {
        try {
            return supplier.get();
        } catch (StorageException storageException) {
            if (storageException.getHttpStatusCode() == HttpStatus.CONFLICT.value()) {
                errorHandler.handleError(
                    new LeaseAlreadyPresentException(
                        "Lease already acquired for container " + containerName + " and zip file " + zipFileName,
                        storageException
                    )
                );
            } else {
                errorHandler.handleError(storageException);
            }
            return null;
        } catch (Exception exception) {
            errorHandler.handleError(exception);
            return null;
        }
    }

    public Boolean wrapDeleteBlobFailure(
        Envelope envelope,
        Supplier<Boolean> supplier
    ) {
        try {
            return supplier.get();
        } catch (Exception exception) {
            if (exception instanceof EnvelopeAwareThrowable) {
                errorHandler.handleError(exception);
            } else {
                errorHandler.handleError(new BlobDeleteFailureException(envelope, exception));
            }
            return Boolean.FALSE;
        }
    }

    public Boolean wrapFailure(Supplier<Boolean> supplier) {
        try {
            return supplier.get();
        } catch (Exception exception) {
            errorHandler.handleError(exception);
            return Boolean.FALSE;
        }
    }

}
