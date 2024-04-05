package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.DisallowedDocumentTypesException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.MetadataNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.PaymentsDisabledException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.PreviouslyFailedToUploadException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ServiceDisabledException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.services.jms.JmsFileContentProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.services.jms.JmsFileRejector;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.ZipFileContentDetail;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.ZipFileProcessor;

import java.util.List;
import java.util.zip.ZipInputStream;

import static java.time.Instant.now;
import static java.util.Collections.emptyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification.NEW_APPLICATION;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.*;

@ExtendWith(MockitoExtension.class)
class JmsFileContentProcessorTest {
    private static final String FILE_NAME = "file1.zip";
    private static final String CONTAINER_NAME = "container";
    private static final String POBOX = "pobox";
    private static final String BULKSCAN = "bulkscan";
    private static final String CASE_NUMBER = "case_number";
    private static final String CASE_REFERENCE = "case_reference";

    @Mock
    private EnvelopeProcessor envelopeProcessor;

    @Mock
    private ZipFileProcessor zipFileProcessor;

    @Mock
    private JmsFileRejector fileRejector;

    @Mock
    private EnvelopeHandler envelopeHandler;

    @Mock
    private ZipInputStream zis;

    private byte[] metadata = new byte[]{};

    private List<String> pdfs = emptyList();

    private ZipFileContentDetail zipFileContentDetail = new ZipFileContentDetail(new byte[]{}, emptyList());

    private InputEnvelope inputEnvelope;

    private JmsFileContentProcessor fileContentProcessor;

    @BeforeEach
    void setUp() {
        fileContentProcessor = new JmsFileContentProcessor(
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
        given(zipFileProcessor.getZipContentDetail(zis, FILE_NAME)).willReturn(zipFileContentDetail);
        given(envelopeProcessor.parseEnvelope(metadata, FILE_NAME)).willReturn(inputEnvelope);

        // when
        fileContentProcessor.processZipFileContent(
            zis,
            FILE_NAME,
            CONTAINER_NAME
        );

        // then
        verify(envelopeHandler).handleEnvelope(
            CONTAINER_NAME,
            FILE_NAME,
            pdfs,
            inputEnvelope
        );
        verifyNoInteractions(fileRejector);
        verifyNoMoreInteractions(envelopeProcessor);
    }

    @Test
    void should_handle_payments_disabled() throws Exception {
        // given
        given(zipFileProcessor.getZipContentDetail(zis, FILE_NAME)).willReturn(zipFileContentDetail);
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
                inputEnvelope
            );

        // when
        fileContentProcessor.processZipFileContent(
            zis,
            FILE_NAME,
            CONTAINER_NAME
        );

        // then
        verify(fileRejector)
            .handleInvalidBlob(
                1L,
                CONTAINER_NAME,
                FILE_NAME,
                ex
            );
        verifyNoMoreInteractions(fileRejector);
        verifyNoMoreInteractions(envelopeProcessor);
    }

    @Test
    void should_handle_service_disabled() throws Exception {
        // given
        given(zipFileProcessor.getZipContentDetail(zis, FILE_NAME)).willReturn(zipFileContentDetail);
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
                inputEnvelope
            );

        // when
        fileContentProcessor.processZipFileContent(
            zis,
            FILE_NAME,
            CONTAINER_NAME
        );

        // then
        verify(fileRejector)
            .handleInvalidBlob(
                1L,
                CONTAINER_NAME,
                FILE_NAME,
                ex
            );
        verifyNoMoreInteractions(fileRejector);
        verifyNoMoreInteractions(envelopeProcessor);
    }

    @Test
    void should_handle_previously_failed_to_upload_exception() throws Exception {
        // given
        PreviouslyFailedToUploadException ex = new PreviouslyFailedToUploadException("msg");
        given(zipFileProcessor.getZipContentDetail(zis, FILE_NAME)).willThrow(ex);


        // when
        fileContentProcessor.processZipFileContent(
            zis,
            FILE_NAME,
            CONTAINER_NAME
        );

        // then
        verify(envelopeProcessor)
            .createEvent(
                DOC_UPLOAD_FAILURE,
                CONTAINER_NAME,
                FILE_NAME,
                ex.getMessage(),
                null
            );
        verifyNoInteractions(fileRejector);
        verifyNoMoreInteractions(envelopeProcessor);
    }

    @Test
    void should_handle_envelope_rejection_exception() throws Exception {
        // given
        given(zipFileProcessor.getZipContentDetail(zis, FILE_NAME)).willReturn(zipFileContentDetail);
        MetadataNotFoundException ex = new MetadataNotFoundException("msg");
        given(envelopeProcessor.parseEnvelope(metadata, FILE_NAME)).willThrow(ex);
        given(envelopeProcessor
                  .createEvent(FILE_VALIDATION_FAILURE, CONTAINER_NAME, FILE_NAME, "msg", null))
            .willReturn(1L);

        // when
        fileContentProcessor.processZipFileContent(
            zis,
            FILE_NAME,
            CONTAINER_NAME
        );

        // then
        verify(fileRejector)
            .handleInvalidBlob(
                1L,
                CONTAINER_NAME,
                FILE_NAME,
                ex
            );
        verifyNoMoreInteractions(fileRejector);
        verifyNoMoreInteractions(envelopeProcessor);
    }

    @Test
    void should_handle_envelope_disallowed_document_type_exception() throws Exception {
        // given
        given(zipFileProcessor.getZipContentDetail(zis, FILE_NAME)).willReturn(zipFileContentDetail);
        DisallowedDocumentTypesException ex = new DisallowedDocumentTypesException("msg");
        given(envelopeProcessor.parseEnvelope(metadata, FILE_NAME)).willThrow(ex);
        given(envelopeProcessor
                  .createEvent(FILE_VALIDATION_FAILURE, CONTAINER_NAME, FILE_NAME, "msg", null))
            .willReturn(1L);

        // when
        fileContentProcessor.processZipFileContent(
            zis,
            FILE_NAME,
            CONTAINER_NAME
        );

        // then
        verify(fileRejector)
            .handleInvalidBlob(
                1L,
                CONTAINER_NAME,
                FILE_NAME,
                ex
            );
        verifyNoMoreInteractions(fileRejector);
        verifyNoMoreInteractions(envelopeProcessor);
    }

    @Test
    void should_handle_generic_exception() throws Exception {
        // given
        RuntimeException ex = new RuntimeException("msg");
        given(zipFileProcessor.getZipContentDetail(zis, FILE_NAME)).willThrow(ex);


        // when
        fileContentProcessor.processZipFileContent(
            zis,
            FILE_NAME,
            CONTAINER_NAME
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
