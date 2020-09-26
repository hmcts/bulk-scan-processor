package uk.gov.hmcts.reform.bulkscanprocessor.services;

import com.azure.storage.blob.BlobClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;
import uk.gov.hmcts.reform.bulkscanprocessor.config.ContainerMappings;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.DisallowedDocumentTypesException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.MetadataNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.OcrValidationServerSideException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.PaymentsDisabledException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ServiceDisabledException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.ZipFileProcessingResult;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.ZipFileProcessor;

import java.util.List;
import java.util.zip.ZipInputStream;

import static java.time.Instant.now;
import static java.util.Collections.emptyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification.NEW_APPLICATION;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DISABLED_SERVICE_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DOC_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.FILE_VALIDATION_FAILURE;

@ExtendWith(MockitoExtension.class)
class FileContentProcessorTest {
    private static final String FILE_NAME = "file1.zip";
    private static final String CONTAINER_NAME = "container";
    private static final String LEASE_ID = "leaseID";
    private static final String POBOX = "pobox";
    private static final String BULKSCAN = "bulkscan";
    private static final String CASE_NUMBER = "case_number";
    private static final String CASE_REFERENCE = "case_reference";

    @Mock
    private EnvelopeProcessor envelopeProcessor;

    @Mock
    private ZipFileProcessor zipFileProcessor;

    @Mock
    private FileRejector fileRejector;

    @Mock
    private EnvelopeHandler envelopeHandler;

    @Mock
    private ZipInputStream zis;

    @Mock
    private ZipFileProcessingResult result;

    @Mock
    private BlobClient blobClient;

    private byte[] metadata = new byte[]{};

    private List<ContainerMappings.Mapping> mappings = emptyList();

    private List<Pdf> pdfs = emptyList();

    private InputEnvelope inputEnvelope;

    private FileContentProcessor fileContentProcessor;

    @BeforeEach
    void setUp() {
        fileContentProcessor = new FileContentProcessor(
            zipFileProcessor,
            envelopeProcessor,
            envelopeHandler,
            fileRejector
        );
        inputEnvelope = new InputEnvelope(
            POBOX,
            BULKSCAN,
            now(),
            now(),
            now(),
            FILE_NAME,
            null,
            CASE_NUMBER,
            CASE_REFERENCE,
            NEW_APPLICATION,
            emptyList(),
            emptyList(),
            emptyList()
        );
    }

    @Test
    void should_process_file_content_and_save_envelope() throws Exception {
        // given
        given(zipFileProcessor.process(zis, FILE_NAME)).willReturn(result);
        given(result.getMetadata()).willReturn(metadata);
        given(envelopeProcessor.parseEnvelope(metadata, FILE_NAME)).willReturn(inputEnvelope);
        given(result.getPdfs()).willReturn(pdfs);

        // when
        fileContentProcessor.processZipFileContent(
            zis,
            FILE_NAME,
            CONTAINER_NAME,
            blobClient,
            LEASE_ID
        );

        // then
        verify(envelopeHandler).handleEnvelope(
            CONTAINER_NAME,
            FILE_NAME,
            pdfs,
            inputEnvelope,
            blobClient,
            LEASE_ID
        );
        verifyNoInteractions(fileRejector);
        verifyNoMoreInteractions(envelopeProcessor);
    }

    @Test
    void should_handle_payments_disabled() throws Exception {
        // given
        given(zipFileProcessor.process(zis, FILE_NAME)).willReturn(result);
        given(result.getMetadata()).willReturn(metadata);
        given(envelopeProcessor.parseEnvelope(metadata, FILE_NAME)).willReturn(inputEnvelope);
        given(envelopeProcessor
                  .createEvent(FILE_VALIDATION_FAILURE, CONTAINER_NAME, FILE_NAME, "msg", null))
            .willReturn(1L);

        PaymentsDisabledException ex = new PaymentsDisabledException("msg");
        doThrow(ex)
            .when(envelopeHandler)
            .handleEnvelope(
                CONTAINER_NAME,
                FILE_NAME,
                pdfs,
                inputEnvelope,
                blobClient,
                LEASE_ID
            );

        // when
        fileContentProcessor.processZipFileContent(
            zis,
            FILE_NAME,
            CONTAINER_NAME,
            blobClient,
            LEASE_ID
        );

        // then
        verify(fileRejector)
            .handleInvalidBlob(
                1L,
                CONTAINER_NAME,
                FILE_NAME,
                LEASE_ID,
                ex
            );
        verifyNoMoreInteractions(fileRejector);
        verifyNoMoreInteractions(envelopeProcessor);
    }

    @Test
    void should_handle_ocr_validation_server_side_exception() throws Exception {
        // given
        given(zipFileProcessor.process(zis, FILE_NAME)).willReturn(result);
        given(result.getMetadata()).willReturn(metadata);
        given(envelopeProcessor.parseEnvelope(metadata, FILE_NAME)).willReturn(inputEnvelope);

        HttpServerErrorException cause = HttpServerErrorException.create(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "internal server error message",
            HttpHeaders.EMPTY,
            null,
            null
        );
        OcrValidationServerSideException ex = new OcrValidationServerSideException("msg", cause);
        doThrow(ex)
            .when(envelopeHandler)
            .handleEnvelope(
                CONTAINER_NAME,
                FILE_NAME,
                pdfs,
                inputEnvelope,
                blobClient,
                LEASE_ID
            );

        // when
        fileContentProcessor.processZipFileContent(
            zis,
            FILE_NAME,
            CONTAINER_NAME,
            blobClient,
            LEASE_ID
        );

        // then
        verifyNoInteractions(fileRejector);
        verifyNoMoreInteractions(envelopeProcessor);
    }

    @Test
    void should_handle_service_disabled() throws Exception {
        // given
        given(zipFileProcessor.process(zis, FILE_NAME)).willReturn(result);
        given(result.getMetadata()).willReturn(metadata);
        given(envelopeProcessor.parseEnvelope(metadata, FILE_NAME)).willReturn(inputEnvelope);
        given(envelopeProcessor
                  .createEvent(DISABLED_SERVICE_FAILURE, CONTAINER_NAME, FILE_NAME, "msg", null))
            .willReturn(1L);

        ServiceDisabledException ex = new ServiceDisabledException("msg");
        doThrow(ex)
            .when(envelopeHandler)
            .handleEnvelope(
                CONTAINER_NAME,
                FILE_NAME,
                pdfs,
                inputEnvelope,
                blobClient,
                LEASE_ID
            );

        // when
        fileContentProcessor.processZipFileContent(
            zis,
            FILE_NAME,
            CONTAINER_NAME,
            blobClient,
            LEASE_ID
        );

        // then
        verify(fileRejector)
            .handleInvalidBlob(
                1L,
                CONTAINER_NAME,
                FILE_NAME,
                LEASE_ID,
                ex
            );
        verifyNoMoreInteractions(fileRejector);
        verifyNoMoreInteractions(envelopeProcessor);
    }

    @Test
    void should_handle_envelope_rejection_exception() throws Exception {
        // given
        given(zipFileProcessor.process(zis, FILE_NAME)).willReturn(result);
        given(result.getMetadata()).willReturn(metadata);
        MetadataNotFoundException ex = new MetadataNotFoundException("msg");
        given(envelopeProcessor.parseEnvelope(metadata, FILE_NAME)).willThrow(ex);
        given(envelopeProcessor
                  .createEvent(FILE_VALIDATION_FAILURE, CONTAINER_NAME, FILE_NAME, "msg", null))
            .willReturn(1L);

        // when
        fileContentProcessor.processZipFileContent(
            zis,
            FILE_NAME,
            CONTAINER_NAME,
            blobClient,
            LEASE_ID
        );

        // then
        verify(fileRejector)
            .handleInvalidBlob(
                1L,
                CONTAINER_NAME,
                FILE_NAME,
                LEASE_ID,
                ex
            );
        verifyNoMoreInteractions(fileRejector);
        verifyNoMoreInteractions(envelopeProcessor);
    }

    @Test
    void should_handle_envelope_disallowed_document_type_exception() throws Exception {
        // given
        given(zipFileProcessor.process(zis, FILE_NAME)).willReturn(result);
        given(result.getMetadata()).willReturn(metadata);
        DisallowedDocumentTypesException ex = new DisallowedDocumentTypesException("msg");
        given(envelopeProcessor.parseEnvelope(metadata, FILE_NAME)).willThrow(ex);
        given(envelopeProcessor
                  .createEvent(FILE_VALIDATION_FAILURE, CONTAINER_NAME, FILE_NAME, "msg", null))
            .willReturn(1L);

        // when
        fileContentProcessor.processZipFileContent(
            zis,
            FILE_NAME,
            CONTAINER_NAME,
            blobClient,
            LEASE_ID
        );

        // then
        verify(fileRejector)
            .handleInvalidBlob(
                1L,
                CONTAINER_NAME,
                FILE_NAME,
                LEASE_ID,
                ex
            );
        verifyNoMoreInteractions(fileRejector);
        verifyNoMoreInteractions(envelopeProcessor);
    }

    @Test
    void should_handle_generic_exception() throws Exception {
        // given
        RuntimeException ex = new RuntimeException("msg");
        given(zipFileProcessor.process(zis, FILE_NAME)).willThrow(ex);


        // when
        fileContentProcessor.processZipFileContent(
            zis,
            FILE_NAME,
            CONTAINER_NAME,
            blobClient,
            LEASE_ID
        );

        // then
        verify(envelopeProcessor)
            .createEvent(
                DOC_FAILURE,
                CONTAINER_NAME,
                FILE_NAME,
                ex.getMessage(),
                null
            );
        verifyNoInteractions(fileRejector);
        verifyNoMoreInteractions(envelopeProcessor);
    }
}
