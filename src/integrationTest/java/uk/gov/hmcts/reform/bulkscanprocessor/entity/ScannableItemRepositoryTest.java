package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.COMPLETED;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.CREATED;
import static uk.gov.hmcts.reform.bulkscanprocessor.entity.Status.NOTIFICATION_SENT;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification.EXCEPTION;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.DocumentType.OTHER;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class ScannableItemRepositoryTest {

    @Autowired
    private ScannableItemRepository scannableItemRepo;

    @Autowired
    private EnvelopeRepository envelopeRepo;

    @BeforeEach
    public void cleanUp() {
        scannableItemRepo.deleteAll();
        envelopeRepo.deleteAll();
    }

    @Test
    public void should_find_zip_file_when_dcn_begins_with_search_string() {
        // given
        final String documentControlNumber = "10000023";
        Envelope e1 = envelope("c1", "test1.zip", CREATED, EXCEPTION, "ccd-id-1", "ccd-action-1", null);
        Envelope e2 = envelope("c2", "test2.zip", NOTIFICATION_SENT, EXCEPTION, "ccd-id-2", "ccd-action-2", null);
        Envelope e3 = envelope("c3", "test3.zip", COMPLETED, EXCEPTION, "ccd-id-3", "ccd-action-3", null);
        ScannableItem s1 = scannableItem(e1,"10000023");
        ScannableItem s2 = scannableItem(e2,"1000002322");
        ScannableItem s3 = scannableItem(e3,"20000023322");
        dbHasEnvelope(e1, e2, e3);
        dbHasScannableItems(s1, s2, s3);

        // when
        final List<String> result = scannableItemRepo.findByDcn(documentControlNumber);

        // then
        assertThat(result).containsExactlyInAnyOrder("test1.zip", "test2.zip");
    }

    @Test
    public void should_not_find_zip_file_when_dcn_not_begins_with_search_string() {
        // given
        final String documentControlNumber = "10000023";
        Envelope e1 = envelope("c1", "test1.zip", CREATED, EXCEPTION, "ccd-id-1", "ccd-action-1", null);
        Envelope e2 = envelope("c2", "test2.zip", NOTIFICATION_SENT, EXCEPTION, "ccd-id-2", "ccd-action-2", null);
        ScannableItem s1 = scannableItem(e1,"310000023");
        ScannableItem s2 = scannableItem(e2,"45721000002322");
        dbHasEnvelope(e1, e2);
        dbHasScannableItems(s1, s2);

        // when
        final List<String> result = scannableItemRepo.findByDcn(documentControlNumber);

        // then
        assertThat(result).isEmpty();
    }

    private void dbHasEnvelope(Envelope... envelopes) {
        envelopeRepo.saveAll(asList(envelopes));
    }

    private void dbHasScannableItems(ScannableItem... scannableItems) {
        scannableItemRepo.saveAll(asList(scannableItems));
    }

    private Envelope envelope(
        String container,
        String zipFileName,
        Status status,
        Classification classification,
        String ccdId,
        String ccdAction,
        String rescanFor
    ) {
        Envelope envelope = new Envelope(
            UUID.randomUUID().toString(),
            "jurisdiction1",
            Instant.now(),
            Instant.now(),
            Instant.now(),
            zipFileName,
            "1234432112344321",
            null,
            classification,
            emptyList(),
            emptyList(),
            emptyList(),
            container,
            rescanFor
        );

        envelope.setStatus(status);

        envelope.setCcdId(ccdId);
        envelope.setEnvelopeCcdAction(ccdAction);

        return envelope;
    }

    private ScannableItem scannableItem(Envelope envelope, String dcn) {
        ScannableItem scannableItem = new ScannableItem(
            dcn,
            null,
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
}
