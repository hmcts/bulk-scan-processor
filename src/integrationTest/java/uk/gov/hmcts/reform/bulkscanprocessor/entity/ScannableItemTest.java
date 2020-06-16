package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
@ExtendWith(SpringExtension.class)
public class ScannableItemTest {

    @Autowired
    private EnvelopeRepository envelopeRepository;

    @Autowired
    private ScannableItemRepository scannableItemRepository;

    @AfterEach
    public void cleanUp() {
        envelopeRepository.deleteAll();
        scannableItemRepository.deleteAll();
    }

    @Test
    public void should_update_document_uuid_of_scannable_item() throws IOException {
        // given
        Envelope envelope = EnvelopeCreator.envelope();
        envelope.setContainer("container");

        // and
        List<ScannableItem> items = envelopeRepository.saveAndFlush(envelope).getScannableItems();

        // when
        scannableItemRepository.saveAll(items.stream()
            .peek(item -> item.setDocumentUuid(item.getId().toString()))
            .collect(Collectors.toList())
        );

        // then
        List<ScannableItem> dbItems = scannableItemRepository.findAllById(
            items.stream().map(ScannableItem::getId).collect(Collectors.toList())
        );

        assertThat(dbItems).hasSize(2);

        // and
        dbItems.forEach(item ->
            assertThat(item.getDocumentUuid()).isEqualTo(item.getId().toString())
        );
    }
}
