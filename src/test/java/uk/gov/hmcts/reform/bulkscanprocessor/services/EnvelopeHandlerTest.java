package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.config.ContainerMappings;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.PaymentsDisabledException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ServiceDisabledException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.blob.InputEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;
import uk.gov.hmcts.reform.bulkscanprocessor.validation.EnvelopeValidator;
import uk.gov.hmcts.reform.bulkscanprocessor.validation.OcrValidator;
import uk.gov.hmcts.reform.bulkscanprocessor.validation.model.OcrValidationWarnings;

import java.util.List;
import java.util.Optional;

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
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.FILE_VALIDATION_FAILURE;

@ExtendWith(MockitoExtension.class)
class EnvelopeHandlerTest {

    private static final String FILE_NAME = "file1.zip";
    private static final String CONTAINER_NAME = "container";
    private static final String LEASE_ID = "leaseID";
    private static final String DCN = "dcn";
    private static final String POBOX = "pobox";
    private static final String BULKSCAN = "bulkscan";
    private static final String CASE_NUMBER = "case_number";
    private static final String CASE_REFERENCE = "case_reference";

    @Mock
    private EnvelopeValidator envelopeValidator;

    @Mock
    private ContainerMappings containerMappings;

    @Mock
    private EnvelopeProcessor envelopeProcessor;

    private boolean paymentsEnabled;

    @Mock
    private OcrValidator ocrValidator;

    @Mock
    private FileErrorHandler fileErrorHandler;

    private List<ContainerMappings.Mapping> mappings = emptyList();

    private List<Pdf> pdfs = emptyList();

    private Optional<OcrValidationWarnings> warnings =
        Optional.of(new OcrValidationWarnings(DCN, emptyList()));

    private InputEnvelope inputEnvelope;

    private EnvelopeHandler envelopeHandler;

    @BeforeEach
    void setUp() {
        envelopeHandler = new EnvelopeHandler(
            envelopeValidator,
            containerMappings,
            envelopeProcessor,
            ocrValidator,
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
    void should_handle_and_save_envelope() {
        given(ocrValidator.assertOcrDataIsValid(inputEnvelope)).willReturn(warnings);
        given(containerMappings.getMappings()).willReturn(mappings);

        // when
        envelopeHandler.handleEnvelope(
            CONTAINER_NAME,
            FILE_NAME,
            pdfs,
            inputEnvelope,
            LEASE_ID
        );

        // then
        verify(envelopeValidator).assertZipFilenameMatchesWithMetadata(inputEnvelope, FILE_NAME);
        verify(envelopeValidator).assertContainerMatchesJurisdictionAndPoBox(
            containerMappings.getMappings(), inputEnvelope, CONTAINER_NAME
        );
        verify(envelopeValidator).assertServiceEnabled(inputEnvelope, containerMappings.getMappings());
        verify(envelopeValidator).assertEnvelopeContainsOcrDataIfRequired(inputEnvelope);
        verify(envelopeValidator).assertEnvelopeHasPdfs(inputEnvelope, pdfs);
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
    void should_handle_payments_disabled() {
        // given
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
        envelopeHandler.handleEnvelope(
            CONTAINER_NAME,
            FILE_NAME,
            pdfs,
            inputEnvelope,
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
    void should_handle_service_disabled() {
        // given
        given(containerMappings.getMappings()).willReturn(mappings);

        ServiceDisabledException ex = new ServiceDisabledException("msg");
        doThrow(ex)
            .when(envelopeValidator)
            .assertServiceEnabled(
                inputEnvelope,
                mappings
            );

        // when
        envelopeHandler.handleEnvelope(
            CONTAINER_NAME,
            FILE_NAME,
            pdfs,
            inputEnvelope,
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
}
