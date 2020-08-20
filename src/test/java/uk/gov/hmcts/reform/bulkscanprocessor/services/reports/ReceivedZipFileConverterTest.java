package uk.gov.hmcts.reform.bulkscanprocessor.services.reports;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.ReceivedZipFile;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.ReceivedZipFileData;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models.ZipFileIdentifier;

import java.time.Instant;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class ReceivedZipFileConverterTest {
    private ReceivedZipFileConverter receivedZipFileConverter;

    @BeforeEach
    public void setUp() {
        receivedZipFileConverter = new ReceivedZipFileConverter();
    }

    @Test
    void should_convert_files_without_scannable_documents_and_payments() {
        // given
        ReceivedZipFile file1 = new ReceivedZipFileItem("file1", "c1", Instant.now(), null, null);
        ReceivedZipFile file2 = new ReceivedZipFileItem("file2", "c2", Instant.now(), null, null);

        // when
        List<ReceivedZipFileData> res = receivedZipFileConverter.convertReceivedZipFiles(asList(file1, file2));

        // then
        assertThat(res)
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new ReceivedZipFileData(new ZipFileIdentifier("file1", "c1"), emptyList(), emptyList()),
                new ReceivedZipFileData(new ZipFileIdentifier("file2", "c2"), emptyList(), emptyList())
            );
    }

    @Test
    void should_convert_files_with_scannable_documents() {
        // given
        ReceivedZipFile file1 = new ReceivedZipFileItem("file1", "c1", Instant.now(), "doc-1", null);
        ReceivedZipFile file2 = new ReceivedZipFileItem("file1", "c1", Instant.now(), "doc-2", null);
        ReceivedZipFile file3 = new ReceivedZipFileItem("file2", "c2", Instant.now(), "doc-3", null);
        ReceivedZipFile file4 = new ReceivedZipFileItem("file2", "c2", Instant.now(), "doc-4", null);

        // when
        List<ReceivedZipFileData> res =
            receivedZipFileConverter.convertReceivedZipFiles(asList(file1, file2, file3, file4));

        // then
        assertThat(res)
            .extracting(file ->
                            tuple(
                                file.zipFileIdentifier,
                                file.scannableItemDcns.stream().sorted().collect(toList()),
                                file.paymentDcns.stream().sorted().collect(toList())
                            )
            )
            .containsExactlyInAnyOrder(
                tuple(
                    new ZipFileIdentifier("file1", "c1"),
                    asList("doc-1", "doc-2"),
                    emptyList()
                ),
                tuple(
                    new ZipFileIdentifier("file2", "c2"),
                    asList("doc-3", "doc-4"),
                    emptyList()
                )
            );
    }

    @Test
    void should_convert_files_with_payments() {
        // given
        ReceivedZipFile file1 = new ReceivedZipFileItem("file1", "c1", Instant.now(), null, "pay-1");
        ReceivedZipFile file2 = new ReceivedZipFileItem("file1", "c1", Instant.now(), null, "pay-2");
        ReceivedZipFile file3 = new ReceivedZipFileItem("file2", "c2", Instant.now(), null, "pay-3");
        ReceivedZipFile file4 = new ReceivedZipFileItem("file2", "c2", Instant.now(), null, "pay-4");

        // when
        List<ReceivedZipFileData> res =
            receivedZipFileConverter.convertReceivedZipFiles(asList(file1, file2, file3, file4));

        // then
        assertThat(res)
            .extracting(file ->
                            tuple(
                                file.zipFileIdentifier,
                                file.scannableItemDcns.stream().sorted().collect(toList()),
                                file.paymentDcns.stream().sorted().collect(toList())
                            )
            )
            .containsExactlyInAnyOrder(
                tuple(
                    new ZipFileIdentifier("file1", "c1"),
                    emptyList(),
                    asList("pay-1", "pay-2")
                ),
                tuple(
                    new ZipFileIdentifier("file2", "c2"),
                    emptyList(),
                    asList("pay-3", "pay-4")
                )
            );
    }

    @Test
    void should_convert_files_with_scannable_documents_and_payments() {
        // given
        ReceivedZipFile file1 = new ReceivedZipFileItem("file1", "c1", Instant.now(), "doc-1", "pay-1");
        ReceivedZipFile file2 = new ReceivedZipFileItem("file1", "c1", Instant.now(), "doc-1", "pay-2");
        ReceivedZipFile file3 = new ReceivedZipFileItem("file1", "c1", Instant.now(), "doc-2", "pay-2");
        ReceivedZipFile file4 = new ReceivedZipFileItem("file1", "c1", Instant.now(), "doc-2", "pay-2");
        ReceivedZipFile file5 = new ReceivedZipFileItem("file2", "c2", Instant.now(), "doc-3", "pay-3");
        ReceivedZipFile file6 = new ReceivedZipFileItem("file2", "c2", Instant.now(), "doc-3", "pay-3");
        ReceivedZipFile file7 = new ReceivedZipFileItem("file2", "c2", Instant.now(), "doc-4", "pay-4");
        ReceivedZipFile file8 = new ReceivedZipFileItem("file2", "c2", Instant.now(), "doc-4", "pay-4");

        // when
        List<ReceivedZipFileData> res =
            receivedZipFileConverter.convertReceivedZipFiles(
                asList(file1, file2, file3, file4, file5, file6, file7, file8)
            );

        // then
        assertThat(res.get(1).scannableItemDcns).containsExactlyInAnyOrder("doc-1", "doc-2");
        assertThat(res.get(0).scannableItemDcns).containsExactlyInAnyOrder("doc-3", "doc-4");
        assertThat(res.get(1).paymentDcns).containsExactlyInAnyOrder("pay-1", "pay-2");
        assertThat(res.get(0).paymentDcns).containsExactlyInAnyOrder("pay-3", "pay-4");
        assertThat(res)
            .extracting(file ->
                            tuple(
                                file.zipFileIdentifier,
                                file.scannableItemDcns.stream().sorted().collect(toList()),
                                file.paymentDcns.stream().sorted().collect(toList())
                            )
            )
            .containsExactlyInAnyOrder(
                tuple(
                    new ZipFileIdentifier("file1", "c1"),
                    asList("doc-1", "doc-2"),
                    asList("pay-1", "pay-2")
                ),
                tuple(
                    new ZipFileIdentifier("file2", "c2"),
                    asList("doc-3", "doc-4"),
                    asList("pay-3", "pay-4")
                )
            );
    }
}
