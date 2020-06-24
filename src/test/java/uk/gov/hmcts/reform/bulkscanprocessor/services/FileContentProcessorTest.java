package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import uk.gov.hmcts.reform.bulkscanprocessor.config.ContainerMappings;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.ZipFileProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.validation.OcrValidator;

class FileContentProcessorTest {
    @Mock
    private EnvelopeProcessor envelopeProcessor;

    @Mock
    private ZipFileProcessor zipFileProcessor;

    @Mock
    private ContainerMappings containerMappings;

    @Mock
    private OcrValidator ocrValidator;

    @Mock
    private FileErrorHandler fileErrorHandler;

    private FileContentProcessor fileContentProcessor;

    @BeforeEach
    void setUp() {
        fileContentProcessor = new FileContentProcessor(
            envelopeProcessor,
            zipFileProcessor,
            containerMappings,
            ocrValidator,
            fileErrorHandler,
            true
        );
    }

    @Test
    void processZipFileContent() {
    }
}
