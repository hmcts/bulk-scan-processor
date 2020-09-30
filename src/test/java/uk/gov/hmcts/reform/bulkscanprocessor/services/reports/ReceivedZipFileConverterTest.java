package uk.gov.hmcts.reform.bulkscanprocessor.services.reports;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.ReceivedZipFile;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.ReceivedZipFileData;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class ReceivedZipFileConverterTest {
    private ReceivedZipFileConverter receivedZipFileConverter;

    @BeforeEach
    void setUp() {
        receivedZipFileConverter = new ReceivedZipFileConverter();
    }

    @Test
    void should_convert_files_without_scannable_documents_and_payments() {
        // given
        ReceivedZipFile file1 =
            new ReceivedZipFileItem("file1", "c1", Instant.now(), null, null,  "file3", UUID.randomUUID());
        ReceivedZipFile file2 = new ReceivedZipFileItem("file2", "c2", Instant.now(), null, null,  null, null);

        // when
        List<ReceivedZipFileData> res = receivedZipFileConverter.convertReceivedZipFiles(asList(file1, file2));

        // then
        assertThat(res)
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new ReceivedZipFileData("file1", "c1", "file3", emptyList(), emptyList(), file1.getEnvelopeId()),
                new ReceivedZipFileData("file2", "c2", null, emptyList(), emptyList(), null)
            );
    }

    @Test
    void should_convert_files_with_scannable_documents() {
        // given
        var envelopeId2 = UUID.randomUUID();
        ReceivedZipFile file1 = new ReceivedZipFileItem("file1", "c1", Instant.now(), "doc-1", null,  "file3", null);
        ReceivedZipFile file2 = new ReceivedZipFileItem("file1", "c1", Instant.now(), "doc-2", null,  "file3", null);
        ReceivedZipFile file3
            = new ReceivedZipFileItem("file2", "c2", Instant.now(), "doc-3", null,  null, envelopeId2);
        ReceivedZipFile file4
            = new ReceivedZipFileItem("file2", "c2", Instant.now(), "doc-4", null,  null, envelopeId2);

        // when
        List<ReceivedZipFileData> res =
            receivedZipFileConverter.convertReceivedZipFiles(asList(file1, file2, file3, file4));

        // then
        assertThat(res)
            .extracting(file ->
                            tuple(
                                file.envelopeId,
                                file.zipFileName,
                                file.container,
                                file.rescanFor,
                                file.scannableItemDcns.stream().sorted().collect(toList()),
                                file.paymentDcns.stream().sorted().collect(toList())
                            )
            )
            .containsExactlyInAnyOrder(
                tuple(null, "file1", "c1", "file3", asList("doc-1", "doc-2"), emptyList()),
                tuple(envelopeId2, "file2", "c2", null, asList("doc-3", "doc-4"), emptyList())
            );
    }

    @Test
    void should_convert_files_with_payments() {
        // given
        var envelopeId1 = UUID.randomUUID();
        ReceivedZipFile file1
            = new ReceivedZipFileItem("file1", "c1", Instant.now(), null, "pay-1",  null, envelopeId1);
        ReceivedZipFile file2
            = new ReceivedZipFileItem("file1", "c1", Instant.now(), null, "pay-2",  null, envelopeId1);
        ReceivedZipFile file3
            = new ReceivedZipFileItem("file2", "c2", Instant.now(), null, "pay-3",  "file5", null);
        ReceivedZipFile file4
            = new ReceivedZipFileItem("file2", "c2", Instant.now(), null, "pay-4",  "file5", null);

        // when
        List<ReceivedZipFileData> res =
            receivedZipFileConverter.convertReceivedZipFiles(asList(file1, file2, file3, file4));

        // then
        assertThat(res)
            .extracting(file ->
                            tuple(
                                file.envelopeId,
                                file.zipFileName,
                                file.container,
                                file.rescanFor,
                                file.scannableItemDcns.stream().sorted().collect(toList()),
                                file.paymentDcns.stream().sorted().collect(toList())
                            )
            )
            .containsExactlyInAnyOrder(
                tuple(envelopeId1, "file1", "c1", null, emptyList(), asList("pay-1", "pay-2")),
                tuple(null, "file2", "c2", "file5", emptyList(), asList("pay-3", "pay-4"))
            );
    }

    @Test
    void should_convert_files_with_scannable_documents_and_payments() {
        // given
        var envelopeId1 = UUID.randomUUID();
        var envelopeId2 = UUID.randomUUID();
        ReceivedZipFile file1
            = new ReceivedZipFileItem("file1", "c1", Instant.now(), "doc-1", "pay-1",  null, envelopeId1);
        ReceivedZipFile file2
            = new ReceivedZipFileItem("file1", "c1", Instant.now(), "doc-1", "pay-2",  null, envelopeId1);
        ReceivedZipFile file3
            = new ReceivedZipFileItem("file1", "c1", Instant.now(), "doc-2", "pay-2",  null, envelopeId1);
        ReceivedZipFile file4
            = new ReceivedZipFileItem("file1", "c1", Instant.now(), "doc-2", "pay-2",  null, envelopeId1);
        ReceivedZipFile file5
            = new ReceivedZipFileItem("file2", "c2", Instant.now(), "doc-3", "pay-3",  null, envelopeId2);
        ReceivedZipFile file6
            = new ReceivedZipFileItem("file2", "c2", Instant.now(), "doc-3", "pay-3",  null, envelopeId2);
        ReceivedZipFile file7
            = new ReceivedZipFileItem("file2", "c2", Instant.now(), "doc-4", "pay-4",  null, envelopeId2);
        ReceivedZipFile file8
            = new ReceivedZipFileItem("file2", "c2", Instant.now(), "doc-4", "pay-4",  null, envelopeId2);

        // when
        List<ReceivedZipFileData> res =
            receivedZipFileConverter.convertReceivedZipFiles(
                asList(file1, file2, file3, file4, file5, file6, file7, file8)
            );

        // then
        assertThat(res)
            .extracting(file ->
                            tuple(
                                file.envelopeId,
                                file.zipFileName,
                                file.container,
                                file.scannableItemDcns.stream().sorted().collect(toList()),
                                file.paymentDcns.stream().sorted().collect(toList())
                            )
            )
            .containsExactlyInAnyOrder(
                tuple(envelopeId1, "file1", "c1", asList("doc-1", "doc-2"), asList("pay-1", "pay-2")),
                tuple(envelopeId2, "file2", "c2", asList("doc-3", "doc-4"), asList("pay-3", "pay-4"))
            );
    }
}
