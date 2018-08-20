package uk.gov.hmcts.reform.bulkscanprocessor.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({ TYPE, ANNOTATION_TYPE })
@Retention(RUNTIME)
@Constraint(validatedBy = { ValidPdfDocumentsValidator.class })
@Documented
public @interface ValidPdfDocuments {

    String message() default "Missing PDFs: ${pdfs}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
