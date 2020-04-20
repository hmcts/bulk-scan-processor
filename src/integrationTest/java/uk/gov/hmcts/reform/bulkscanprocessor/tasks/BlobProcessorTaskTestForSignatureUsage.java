package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.config.IntegrationTest;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg.ErrorCode;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;

import java.nio.charset.Charset;
import java.util.List;

import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.toByteArray;
import static com.jayway.jsonpath.JsonPath.parse;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.PROCESSED;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.DirectoryZipper.zipAndSignDir;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DOC_PROCESSED;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DOC_SIGNATURE_FAILURE;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.DOC_UPLOADED;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event.ZIPFILE_PROCESSING_STARTED;

@IntegrationTest
@RunWith(SpringRunner.class)
@SpringBootTest(properties = {
    "storage.public_key_der_file=signing/test_public_key.der",
    "storage.signature_algorithm=sha256withrsa"
})
public class BlobProcessorTaskTestForSignatureUsage extends ProcessorTestSuite<BlobProcessorTask> {

    @Before
    public void setUp() throws Exception {
        super.setUp();

        processor = new BlobProcessorTask(
            blobManager,
            documentProcessor,
            envelopeProcessor,
            zipFileProcessor,
            envelopeRepository,
            processEventRepository,
            containerMappings,
            ocrValidator,
            serviceBusHelper,
            paymentsEnabled
        );
    }

    @Test
    public void should_read_blob_check_signature_and_save_metadata_in_database_when_zip_contains_metadata_and_pdfs()
        throws Exception {
        // given
        uploadToBlobStorage(SAMPLE_ZIP_FILE_NAME, zipAndSignDir("zipcontents/ok", "signing/test_private_key.der"));

        // the following is copy from BlobProcessorTaskTest::testBlobFileProcessed
        // needs a better refinement but currently solving deeper rooted issue with constructors
        // leaving as TODO

        // and
        Pdf pdf = new Pdf("1111002.pdf", toByteArray(getResource("zipcontents/ok/1111002.pdf")));

        given(documentManagementService.uploadDocuments(ImmutableList.of(pdf)))
            .willReturn(ImmutableMap.of(
                "1111002.pdf", DOCUMENT_URL2
            ));

        // when
        processor.processBlobs();

        // then
        Envelope actualEnvelope = getSingleEnvelopeFromDb();

        String originalMetaFile = Resources.toString(
            getResource("zipcontents/ok/metadata.json"),
            Charset.defaultCharset()
        );

        assertThat(parse(actualEnvelope)).isEqualToIgnoringGivenFields(
            parse(originalMetaFile),
            "id", "amount", "amount_in_pence", "configuration", "json"
        );
        assertThat(actualEnvelope.getStatus()).isEqualTo(PROCESSED);

        assertThat(actualEnvelope.getScannableItems())
            .extracting(ScannableItem::getDocumentUuid)
            .hasSameElementsAs(ImmutableList.of(DOCUMENT_UUID2));
        assertThat(actualEnvelope.isZipDeleted()).isTrue();

        // This verifies pdf file objects were created from the zip file
        verify(documentManagementService).uploadDocuments(ImmutableList.of(pdf));

        // and
        List<ProcessEvent> processEvents = processEventRepository.findAll();
        assertThat(processEvents.stream().map(ProcessEvent::getEvent).collect(toList()))
            .containsExactlyInAnyOrder(
                ZIPFILE_PROCESSING_STARTED,
                DOC_UPLOADED,
                DOC_PROCESSED
            );

        assertThat(processEvents).allMatch(pe -> pe.getReason() == null);
    }

    @Test
    public void should_record_signature_failure_when_zip_contains_invalid_signature() throws Exception {
        // when
        uploadToBlobStorage(
            SAMPLE_ZIP_FILE_NAME,
            zipAndSignDir(
                "zipcontents/ok",
                "signing/some_other_private_key.der" // not matching the public key used for validation!
            )
        );

        // when
        processor.processBlobs();

        // then
        envelopeWasNotCreated();
        eventsWereCreated(ZIPFILE_PROCESSING_STARTED, DOC_SIGNATURE_FAILURE);
        fileWasDeleted(SAMPLE_ZIP_FILE_NAME);
        errorWasSent(SAMPLE_ZIP_FILE_NAME, ErrorCode.ERR_SIG_VERIFY_FAILED);
    }
}
