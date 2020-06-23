package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ConfigurationException;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.OcrPresenceException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.EnvelopeProcessor;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DOC_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode.ERR_METAFILE_INVALID;

@ExtendWith(MockitoExtension.class)
class FileErrorHandlerTest {
    private static final long EVENT_ID = 1L;
    private static final String FILE_NAME = "file1.zip";
    private static final Event EVENT = DOC_FAILURE;
    private static final String LEASE_ID = "leaseID";
    private static final String MSG = "msg";
    private static final OcrPresenceException MAPPED_CAUSE = new OcrPresenceException(MSG);
    private static final IOException UNMAPPED_CAUSE = new IOException(MSG);

    private FileErrorHandler fileErrorHandler;

    private static final String CONTAINER = "container";

    @Mock
    private BlobManager blobManager;

    @Mock
    private EnvelopeProcessor envelopeProcessor;

    @Mock
    private ErrorNotificationSender errorNotificationSender;

    @BeforeEach
    void setUp() {
        fileErrorHandler = new FileErrorHandler(
            blobManager,
            envelopeProcessor,
            errorNotificationSender
        );
    }

    @Test
    void should_handle_error_if_error_is_mapped() {
        // given
        given(envelopeProcessor.createEvent(
            EVENT,
            CONTAINER,
            FILE_NAME,
            MSG,
            null
        ))
            .willReturn(EVENT_ID);

        // when
        fileErrorHandler.handleInvalidFileError(
            EVENT,
            CONTAINER,
            FILE_NAME,
            LEASE_ID,
            MAPPED_CAUSE
        );

        // then
        verify(errorNotificationSender)
            .sendErrorNotification(
                FILE_NAME,
                CONTAINER,
                MAPPED_CAUSE,
                EVENT_ID,
                ERR_METAFILE_INVALID
            );
        verify(blobManager)
            .tryMoveFileToRejectedContainer(
                FILE_NAME,
                CONTAINER,
                LEASE_ID
            );
    }

    @Test
    void should_throw_if_error_is_not_mapped() {
        // given
        given(envelopeProcessor.createEvent(
            EVENT,
            CONTAINER,
            FILE_NAME,
            MSG,
            null
        ))
            .willReturn(EVENT_ID);

        // when
        // then
        assertThatThrownBy(() ->
                               fileErrorHandler.handleInvalidFileError(
                                   EVENT,
                                   CONTAINER,
                                   FILE_NAME,
                                   LEASE_ID,
                                   UNMAPPED_CAUSE
                               )
        )
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("Error code mapping not found for " + UNMAPPED_CAUSE.getClass().getName());
    }
}
