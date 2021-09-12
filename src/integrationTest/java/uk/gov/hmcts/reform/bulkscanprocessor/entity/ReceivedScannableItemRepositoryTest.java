package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.ReceivedScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.ReceivedScannableItemRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.ReceivedScannableItemItem;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification.SUPPLEMENTARY_EVIDENCE;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.DocumentType.OTHER;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
@ExtendWith(SpringExtension.class)
class ReceivedScannableItemRepositoryTest {
    @Autowired
    private ReceivedScannableItemRepository reportRepo;

    @Autowired
    private EnvelopeRepository envelopeRepo;

    @Autowired
    private ScannableItemRepository scannableItemRepo;

    @AfterEach
    void cleanUp() {
        scannableItemRepo.deleteAll();
        envelopeRepo.deleteAll();
    }

    @Test
    void should_return_received_scannable_item_for_date() {
        // given
        Envelope e1 = envelope("A", "file1.zip");
        ScannableItem si11 = scannableItem(e1, Instant.parse("2019-02-15T14:15:23.456Z"), "doc-11");
        ScannableItem si12 = scannableItem(e1, Instant.parse("2019-02-15T14:15:23.456Z"), "doc-12");
        Envelope e2 = envelope("A", "file2.zip");
        ScannableItem si21 = scannableItem(e2, Instant.parse("2019-02-15T14:15:23.456Z"), "doc-21");
        Envelope e3 = envelope("B", "file3.zip");
        ScannableItem si31 = scannableItem(e3, Instant.parse("2019-02-15T14:15:23.456Z"), "doc-31");
        Envelope e4 = envelope("B", "file4.zip");
        ScannableItem si41 = scannableItem(e4, Instant.parse("2019-02-16T14:15:23.456Z"), "doc-41");
        ScannableItem si42 = scannableItem(e4, Instant.parse("2019-02-16T14:15:23.456Z"), "doc-42");

        dbHasEnvelopes(e1, e2, e3, e4);
        dbHasScannableItems(si11, si12, si21, si31, si41, si42);

        // when
        List<ReceivedScannableItem> result = reportRepo.getReceivedScannableItemsFor(LocalDate.of(2019, 2, 15));

        // then
        assertThat(result)
            .usingFieldByFieldElementComparator()
            .containsExactlyElementsOf(
                asList(
                    new ReceivedScannableItemItem("A", 3),
                    new ReceivedScannableItemItem("B", 1)
                )
            );
    }

    @Test
    void should_return_empty_list_if_no_scannable_items() {
        // given
        Envelope e1 = envelope("A", "file1.zip");
        Envelope e2 = envelope("A", "file2.zip");
        Envelope e3 = envelope("B", "file3.zip");
        Envelope e4 = envelope("B", "file4.zip");

        dbHasEnvelopes(e1, e2, e3, e4);

        // when
        List<ReceivedScannableItem> result = reportRepo.getReceivedScannableItemsFor(LocalDate.of(2019, 2, 15));

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void should_return_empty_list_if_no_envelopes() {
        // given
        // no envelopes

        // when
        List<ReceivedScannableItem> result = reportRepo.getReceivedScannableItemsFor(LocalDate.of(2019, 2, 15));

        // then
        assertThat(result).isEmpty();
    }

    private Envelope envelope(String container, String zipFileName) {

        return new Envelope(
            UUID.randomUUID().toString(),
            "jurisdiction1",
            Instant.now(),
            Instant.now(),
            Instant.now(),
            zipFileName,
            "1234432112344321",
            null,
            SUPPLEMENTARY_EVIDENCE,
            emptyList(),
            emptyList(),
            emptyList(),
            container,
            null
        );
    }

    private ScannableItem scannableItem(Envelope envelope, Instant scanningDate, String dcn) {
        ScannableItem scannableItem = new ScannableItem(
            dcn,
            scanningDate,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            OTHER,
            null,
            null
        );
        scannableItem.setEnvelope(envelope);
        return scannableItem;
    }

    private void dbHasEnvelopes(Envelope... envelopes) {
        envelopeRepo.saveAll(asList(envelopes));
        envelopeRepo.flush();
    }

    private void dbHasScannableItems(ScannableItem... scannableItems) {
        scannableItemRepo.saveAll(asList(scannableItems));
    }
}
