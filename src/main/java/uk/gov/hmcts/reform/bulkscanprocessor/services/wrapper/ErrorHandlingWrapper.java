package uk.gov.hmcts.reform.bulkscanprocessor.services.wrapper;

import com.microsoft.azure.storage.StorageException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.ErrorHandler;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
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

    public void wrapDocUploadFailure(Envelope envelope, Supplier<Void> supplier) {
        try {
            supplier.get();
        } catch (Exception exception) {
            if (exception instanceof EnvelopeAwareThrowable) {
                errorHandler.handleError(exception);
            } else {
                errorHandler.handleError(new DocUploadFailureGenericException(envelope, exception));
            }
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
}
