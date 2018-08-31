package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
public class ScannableItemTest {

    @Autowired
    private EnvelopeRepository envelopeRepository;

    @Autowired
    private ScannableItemRepository scannableItemRepository;

    @After
    public void cleanUp() {
        envelopeRepository.deleteAll();
        scannableItemRepository.deleteAll();
    }

    @Test
    public void should_update_document_url_of_scannable_item() throws IOException {
        // given
        Envelope envelope = EnvelopeCreator.getEnvelopeFromMetafile();
        envelope.setContainer("container");

        // and
        List<ScannableItem> items = envelopeRepository.save(envelope).getScannableItems();

        // when
        scannableItemRepository.saveAll(items.stream()
            .peek(item -> item.setDocumentUrl("localhost/document/" + item.getId()))
            .collect(Collectors.toList())
        );

        // then
        List<ScannableItem> dbItems = scannableItemRepository.findAllById(
            items.stream().map(ScannableItem::getId).collect(Collectors.toList())
        );

        assertThat(dbItems).hasSize(2);

        // and
        dbItems.forEach(item ->
            assertThat(item.getDocumentUrl()).isEqualTo("localhost/document/" + item.getId())
        );
    }
}
