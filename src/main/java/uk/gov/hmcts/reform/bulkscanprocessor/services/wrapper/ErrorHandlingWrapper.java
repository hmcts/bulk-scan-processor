package uk.gov.hmcts.reform.bulkscanprocessor.services.wrapper;

import org.springframework.stereotype.Component;
import org.springframework.util.ErrorHandler;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.DocFailureGenericException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.DocUploadFailureGenericException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeAwareThrowable;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EventRelatedThrowable;

import java.util.Optional;

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
            Exception exc = Optional.of(exception)
                .filter(EventRelatedThrowable.class::isInstance)
                .orElse(new DocFailureGenericException(containerName, zipFileName, exception));

            errorHandler.handleError(exc);

            return null;
        }
    }

    public void wrapDocUploadFailure(Envelope envelope, Supplier<Void> supplier) {
        try {
            supplier.get();
        } catch (Exception exception) {
            Exception exc = Optional.of(exception)
                .filter(EnvelopeAwareThrowable.class::isInstance)
                .orElse(new DocUploadFailureGenericException(envelope, exception));

            errorHandler.handleError(exc);
        }
    }
}
