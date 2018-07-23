package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Event.DOC_DUPLICATE;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Event.DOC_PROCESSED;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Event.DOC_UPLOADED;

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
}
