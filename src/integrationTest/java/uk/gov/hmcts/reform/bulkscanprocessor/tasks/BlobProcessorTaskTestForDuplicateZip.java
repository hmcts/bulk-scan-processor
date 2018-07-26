package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.UnableToUploadDocumentException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Event.DOC_DUPLICATE;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Event.DOC_PROCESSED;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Event.DOC_UPLOADED;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Event.DOC_UPLOAD_FAILURE;

@RunWith(SpringRunner.class)
@SpringBootTest
public class BlobProcessorTaskTestForDuplicateZip extends BlobProcessorTestSuite {

    @Test
    public void should_record_duplicate_when_same_zip_is_uploaded() throws Exception {
        // given
        uploadZipToBlobStore(ZIP_FILE_NAME_SUCCESS);
        given(documentManagementService.uploadDocuments(getUploadResources())).willReturn(getFileUploadResponse());

        // and
        blobProcessorTask.processBlobs();

        assertThat(envelopeRepository.findAll())
            .hasSize(1)
            .first()
            .extracting("status")
            .containsOnly(DOC_PROCESSED);

        // when
        uploadZipToBlobStore(ZIP_FILE_NAME_SUCCESS);
        blobProcessorTask.processBlobs();

        // then
        assertThat(envelopeRepository.findAll())
            .hasSize(1)
            .first()
            .extracting("status")
            .containsOnly(DOC_DUPLICATE);

        // and
        assertThat(processEventRepository.findAll())
            .hasSize(3)
            .extracting("event")
            .containsOnlyElementsOf(ImmutableList.of(DOC_UPLOADED, DOC_PROCESSED, DOC_DUPLICATE));
    }

    @Test
    public void should_retry_uploading_documents_when_same_zip_is_uploaded() throws Exception {
        // given
        uploadZipToBlobStore(ZIP_FILE_NAME_SUCCESS);
        given(documentManagementService.uploadDocuments(getUploadResources()))
            .willThrow(new UnableToUploadDocumentException("oh no", null))
            .willReturn(getFileUploadResponse());

        // and
        blobProcessorTask.processBlobs();

        assertThat(envelopeRepository.findAll())
            .hasSize(1)
            .first()
            .extracting("status")
            .containsOnly(DOC_UPLOAD_FAILURE);

        // when
        uploadZipToBlobStore(ZIP_FILE_NAME_SUCCESS);
        blobProcessorTask.processBlobs();

        // then
        assertThat(envelopeRepository.findAll())
            .hasSize(1)
            .first()
            .extracting("status")
            .containsOnly(DOC_PROCESSED);

        // and
        assertThat(processEventRepository.findAll())
            .hasSize(3)
            .extracting("event")
            .containsOnlyElementsOf(ImmutableList.of(DOC_UPLOAD_FAILURE, DOC_UPLOADED, DOC_PROCESSED));
    }
}
