package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.DocumentType;
import uk.gov.hmcts.reform.bulkscanprocessor.model.ocr.OcrData;
import uk.gov.hmcts.reform.bulkscanprocessor.model.ocr.OcrDataField;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.persistence.EntityManager;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator.envelope;

@RunWith(SpringRunner.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
public class ScannableItemRepositoryTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EnvelopeRepository envelopeRepository;

    @Autowired
    private ScannableItemRepository scannableItemRepository;

    @After
    public void cleanUp() {
        scannableItemRepository.deleteAll();
    }

    @Test
    public void clearOcrData_should_remove_ocr_data_from_all_scannable_item_for_given_envelope() {
        // given
        List<ScannableItem> scannableItems = createScannableItemsWithOcrData(3);
        Envelope envelope = envelope("BULKSCAN", Status.CREATED, scannableItems);

        UUID envelopeId = envelopeRepository.saveAndFlush(envelope).getId();

        assertAllScannableItemsHaveOcrData(envelopeId);

        // when
        scannableItemRepository.clearOcrData(envelopeId);

        // then
        assertNoScannableItemsHaveOcrData(envelopeId);
    }

    @Test
    public void clearOcrData_should_not_remove_ocr_data_from_other_envelopes() {
        // given
        Envelope envelopeToClear = envelope("BULKSCAN", Status.CREATED, createScannableItemsWithOcrData(3));
        Envelope envelopeToLeave = envelope("BULKSCAN", Status.CREATED, createScannableItemsWithOcrData(3));

        UUID envelopeToClearId = envelopeRepository.saveAndFlush(envelopeToClear).getId();
        UUID envelopeToLeaveId = envelopeRepository.saveAndFlush(envelopeToLeave).getId();

        assertAllScannableItemsHaveOcrData(envelopeToLeaveId);

        // when
        scannableItemRepository.clearOcrData(envelopeToClearId);

        // then
        assertAllScannableItemsHaveOcrData(envelopeToLeaveId);
    }

    @Test
    public void clearOcrData_should_return_the_number_of_updated_scannable_items() {
        // given
        int numberOfScannableItems = 8;

        List<ScannableItem> scannableItems = createScannableItemsWithOcrData(numberOfScannableItems);
        Envelope envelope = envelope("BULKSCAN", Status.CREATED, scannableItems);

        UUID envelopeId = envelopeRepository.saveAndFlush(envelope).getId();

        // when
        int result = scannableItemRepository.clearOcrData(envelopeId);

        // then
        assertThat(result).isEqualTo(numberOfScannableItems);
    }

    private void assertAllScannableItemsHaveOcrData(UUID envelopeId) {
        assertAllScannableItemsMeetCriteria(
            envelopeId,
            scannableItem -> scannableItem.getOcrData() != null
        );
    }

    private void assertNoScannableItemsHaveOcrData(UUID envelopeId) {
        assertAllScannableItemsMeetCriteria(
            envelopeId,
            scannableItem -> scannableItem.getOcrData() == null
        );
    }

    private void assertAllScannableItemsMeetCriteria(UUID envelopeId, Predicate<ScannableItem> criteria) {
        // make sure there's no outdated cache
        entityManager.clear();

        Optional<Envelope> envelope = envelopeRepository.findById(envelopeId);

        assertThat(envelope).isPresent();
        assertThat(envelope.get().getScannableItems()).allMatch(criteria);
    }

    private List<ScannableItem> createScannableItemsWithOcrData(int count) {
        return Stream.generate(() -> {
            Timestamp timestamp = Timestamp.from(Instant.parse("2018-06-23T12:34:56.123Z"));

            ScannableItem scannableItem = new ScannableItem(
                "1111001",
                timestamp,
                "test",
                "test",
                "return",
                timestamp,
                createOcrData(),
                "1111001.pdf",
                "test",
                DocumentType.CHERISHED,
                null
            );

            scannableItem.setDocumentUrl("http://localhost:8080/documents/0fa1ab60-f836-43aa-8c65-b07cc9bebceb");
            return scannableItem;
        })
            .limit(count)
            .collect(toList());
    }

    private OcrData createOcrData() {
        OcrData ocrData = new OcrData();
        ocrData.setFields(Arrays.asList(
            new OcrDataField(new TextNode("key1"), new TextNode("value1")),
            new OcrDataField(new TextNode("key2"), new TextNode("value2"))
        ));

        return ocrData;
    }
}
