package uk.gov.hmcts.reform.bulkscanprocessor.validation;

import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.ZipFileProcessor;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class ValidPdfDocumentsValidator implements ConstraintValidator<ValidPdfDocuments, ZipFileProcessor> {

    @Override
    public boolean isValid(ZipFileProcessor value, ConstraintValidatorContext context) {
        Set<String> scannedFileNames = value.getEnvelope()
            .getScannableItems()
            .stream()
            .map(ScannableItem::getFileName)
            .collect(Collectors.toSet());
        Set<String> pdfFileNames = value.getPdfs()
            .stream()
            .map(Pdf::getFilename)
            .collect(Collectors.toSet());

        Collection<String> missingScannedFiles = new HashSet<>(scannedFileNames);
        missingScannedFiles.removeAll(pdfFileNames);
        Collection<String> missingPdfFiles = new HashSet<>(pdfFileNames);
        missingPdfFiles.removeAll(scannedFileNames);

        missingScannedFiles.addAll(missingPdfFiles);

        context.unwrap(HibernateConstraintValidatorContext.class)
            .addExpressionVariable("pdfs", String.join(", ", missingScannedFiles));

        return missingScannedFiles.isEmpty();
    }
}
