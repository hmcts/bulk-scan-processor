package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.config.ContainerMappings;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.MetadataNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.PaymentsDisabledException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ServiceDisabledException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.ZipFileProcessingResult;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.ZipFileProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.validation.EnvelopeValidator;
import uk.gov.hmcts.reform.bulkscanprocessor.validation.OcrValidator;
import uk.gov.hmcts.reform.bulkscanprocessor.validation.model.OcrValidationWarnings;

import java.util.List;
import java.util.Optional;
import java.util.zip.ZipInputStream;

import static java.time.Instant.now;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
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
    public static final String DCN = "dcn";
    public static final String POBOX = "pobox";
    public static final String BULKSCAN = "bulkscan";
    public static final String CASE_NUMBER = "case_number";
    public static final String CASE_REFERENCE = "case_reference";

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

    @Mock
    private EnvelopeValidator envelopeValidator;

    @Mock
    private ZipInputStream zis;

    @Mock
    private ZipFileProcessingResult result;

    private byte[] metadata = new byte[]{};

    private List<ContainerMappings.Mapping> mappings = emptyList();

    private List<Pdf> pdfs = emptyList();

    private Optional<OcrValidationWarnings> warnings =
        Optional.of(new OcrValidationWarnings(DCN, emptyList()));

    private InputEnvelope inputEnvelope;

    private FileContentProcessor fileContentProcessor;

    private boolean paymentsEnabled = true;

    @BeforeEach
    void setUp() {
        fileContentProcessor = new FileContentProcessor(
            envelopeProcessor,
            zipFileProcessor,
            containerMappings,
            ocrValidator,
            envelopeValidator,
            fileErrorHandler,
            paymentsEnabled
        );
        inputEnvelope = new InputEnvelope(
            POBOX,
            BULKSCAN,
            now(),
            now(),
            now(),
            FILE_NAME,
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
        given(ocrValidator.assertOcrDataIsValid(inputEnvelope)).willReturn(warnings);
        given(result.getPdfs()).willReturn(pdfs);
        given(containerMappings.getMappings()).willReturn(mappings);

        // when
        fileContentProcessor.processZipFileContent(
            zis,
            FILE_NAME,
            CONTAINER_NAME,
            LEASE_ID
        );

        // then
        verify(envelopeValidator).assertZipFilenameMatchesWithMetadata(inputEnvelope, FILE_NAME);
        verify(envelopeValidator).assertContainerMatchesJurisdictionAndPoBox(
            containerMappings.getMappings(), inputEnvelope, CONTAINER_NAME
        );
        verify(envelopeValidator).assertServiceEnabled(inputEnvelope, containerMappings.getMappings());
        verify(envelopeValidator).assertEnvelopeContainsOcrDataIfRequired(inputEnvelope);
        verify(envelopeValidator).assertEnvelopeHasPdfs(inputEnvelope, result.getPdfs());
        verify(envelopeValidator).assertDocumentControlNumbersAreUnique(inputEnvelope);
        verify(envelopeValidator).assertPaymentsEnabledForContainerIfPaymentsArePresent(
            inputEnvelope, paymentsEnabled, containerMappings.getMappings()
        );
        verify(envelopeValidator).assertEnvelopeContainsDocsOfAllowedTypesOnly(inputEnvelope);
        verify(envelopeProcessor).assertDidNotFailToUploadBefore(inputEnvelope.zipFileName, CONTAINER_NAME);

        ArgumentCaptor<Envelope> envelope = ArgumentCaptor.forClass(Envelope.class);
        verify(envelopeProcessor).saveEnvelope(envelope.capture());
        assertThat(envelope.getValue().getPoBox()).isEqualTo(inputEnvelope.poBox);
        assertThat(envelope.getValue().getJurisdiction()).isEqualTo(inputEnvelope.jurisdiction);
        assertThat(envelope.getValue().getDeliveryDate()).isEqualTo(inputEnvelope.deliveryDate);
        assertThat(envelope.getValue().getOpeningDate()).isEqualTo(inputEnvelope.openingDate);
        assertThat(envelope.getValue().getZipFileCreateddate()).isEqualTo(inputEnvelope.zipFileCreateddate);
        assertThat(envelope.getValue().getZipFileName()).isEqualTo(inputEnvelope.zipFileName);
        assertThat(envelope.getValue().getCaseNumber()).isEqualTo(inputEnvelope.caseNumber);
        assertThat(envelope.getValue().getPreviousServiceCaseReference())
            .isEqualTo(inputEnvelope.previousServiceCaseReference);
        assertThat(envelope.getValue().getClassification()).isEqualTo(inputEnvelope.classification);
        assertThat(envelope.getValue().getScannableItems()).isEqualTo(inputEnvelope.scannableItems);
        assertThat(envelope.getValue().getPayments()).isEqualTo(inputEnvelope.payments);
        assertThat(envelope.getValue().getNonScannableItems()).isEqualTo(inputEnvelope.nonScannableItems);
        assertThat(envelope.getValue().getContainer()).isEqualTo(CONTAINER_NAME);

        verifyNoInteractions(fileErrorHandler);
        verifyNoMoreInteractions(envelopeProcessor);
    }

    @Test
    void should_handle_payments_disabled() throws Exception {
        // given
        given(zipFileProcessor.process(zis, FILE_NAME)).willReturn(result);
        given(result.getMetadata()).willReturn(metadata);
        given(envelopeProcessor.parseEnvelope(metadata, FILE_NAME)).willReturn(inputEnvelope);
        given(containerMappings.getMappings()).willReturn(mappings);

        PaymentsDisabledException ex = new PaymentsDisabledException("msg");
        doThrow(ex)
            .when(envelopeValidator)
            .assertPaymentsEnabledForContainerIfPaymentsArePresent(
                inputEnvelope,
                paymentsEnabled,
                mappings
            );

        // when
        fileContentProcessor.processZipFileContent(
            zis,
            FILE_NAME,
            CONTAINER_NAME,
            LEASE_ID
        );

        // then
        verify(fileErrorHandler)
            .handleInvalidFileError(
                FILE_VALIDATION_FAILURE,
                CONTAINER_NAME,
                FILE_NAME,
                LEASE_ID,
                ex
            );
        verifyNoInteractions(ocrValidator);
        verifyNoMoreInteractions(fileErrorHandler);
        verifyNoMoreInteractions(envelopeProcessor);
    }

    @Test
    void should_handle_service_disabled() throws Exception {
        // given
        given(zipFileProcessor.process(zis, FILE_NAME)).willReturn(result);
        given(result.getMetadata()).willReturn(metadata);
        given(envelopeProcessor.parseEnvelope(metadata, FILE_NAME)).willReturn(inputEnvelope);
        given(containerMappings.getMappings()).willReturn(mappings);

        ServiceDisabledException ex = new ServiceDisabledException("msg");
        doThrow(ex)
            .when(envelopeValidator)
            .assertServiceEnabled(
                inputEnvelope,
                mappings
            );

        // when
        fileContentProcessor.processZipFileContent(
            zis,
            FILE_NAME,
            CONTAINER_NAME,
            LEASE_ID
        );

        // then
        verify(fileErrorHandler)
            .handleInvalidFileError(
                DISABLED_SERVICE_FAILURE,
                CONTAINER_NAME,
                FILE_NAME,
                LEASE_ID,
                ex
            );
        verifyNoInteractions(ocrValidator);
        verifyNoMoreInteractions(fileErrorHandler);
        verifyNoMoreInteractions(envelopeProcessor);
    }

    @Test
    void should_handle_envelope_rejection_exception() throws Exception {
        // given
        given(zipFileProcessor.process(zis, FILE_NAME)).willReturn(result);
        given(result.getMetadata()).willReturn(metadata);
        MetadataNotFoundException ex = new MetadataNotFoundException("msg");
        given(envelopeProcessor.parseEnvelope(metadata, FILE_NAME)).willThrow(ex);

        // when
        fileContentProcessor.processZipFileContent(
            zis,
            FILE_NAME,
            CONTAINER_NAME,
            LEASE_ID
        );

        // then
        verify(fileErrorHandler)
            .handleInvalidFileError(
                FILE_VALIDATION_FAILURE,
                CONTAINER_NAME,
                FILE_NAME,
                LEASE_ID,
                ex
            );
        verifyNoInteractions(envelopeValidator);
        verifyNoInteractions(ocrValidator);
        verifyNoMoreInteractions(fileErrorHandler);
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
        verifyNoInteractions(envelopeValidator);
        verifyNoInteractions(ocrValidator);
        verifyNoInteractions(fileErrorHandler);
        verifyNoMoreInteractions(envelopeProcessor);
    }
}
