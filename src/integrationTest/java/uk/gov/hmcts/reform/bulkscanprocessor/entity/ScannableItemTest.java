package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.validation.MetafileJsonValidator;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
public class ScannableItemTest {

    private MetafileJsonValidator validator;

    @Autowired
    private EnvelopeRepository envelopeRepository;

    @Autowired
    private ScannableItemRepository scannableItemRepository;

    @Before
    public void setUp() throws IOException, ProcessingException {
        validator = new MetafileJsonValidator();
    }

    @After
    public void cleanUp() {
        envelopeRepository.deleteAll();
        scannableItemRepository.deleteAll();
    }

    @Test
    public void should_update_document_url_of_scannable_item() throws IOException {
        // given
        Envelope envelope = getEnvelope();
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

    private Envelope getEnvelope() throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream("/metafile.json")) {
            return validator.parseMetafile(IOUtils.toByteArray(inputStream));
        }
    }
}
