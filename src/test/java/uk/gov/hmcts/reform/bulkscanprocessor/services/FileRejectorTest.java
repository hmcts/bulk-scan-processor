package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.OcrPresenceException;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.BlobManager;

import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode.ERR_METAFILE_INVALID;

@ExtendWith(MockitoExtension.class)
class FileRejectorTest {
    private static final long EVENT_ID = 1L;
    private static final String FILE_NAME = "file1.zip";
    private static final String LEASE_ID = "leaseID";
    private static final String MSG = "msg";
    private static final OcrPresenceException MAPPED_CAUSE = new OcrPresenceException(MSG);

    private FileRejector fileRejector;

    private static final String CONTAINER = "container";

    @Mock
    private BlobManager blobManager;

    @Mock
    private ErrorNotificationSender errorNotificationSender;

    @BeforeEach
    void setUp() {
        fileRejector = new FileRejector(
            blobManager,
            errorNotificationSender
        );
    }

    @Test
    void should_handle_handle_invalid_blob() {
        // given

        // when
        fileRejector.handleInvalidBlob(
            EVENT_ID,
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
            .newTryMoveFileToRejectedContainer(
                FILE_NAME,
                CONTAINER,
                LEASE_ID
            );
    }
}
