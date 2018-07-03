package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.util.EntityParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
public class EnvelopeTest {

    @Autowired
    private EnvelopeRepository repository;

    @Test
    public void asd() throws IOException {
        Envelope envelope = EntityParser.parseEnvelopeMetadata(getMetaFile());

        UUID envelopeId = repository.save(envelope).getId();

        Envelope dbEnvelope = repository.getOne(envelopeId);

        try (InputStream stream = getMetaFile()) {
            String originalMetaFile = IOUtils.toString(stream);
            String actualEnvelope = new ObjectMapper().writeValueAsString(dbEnvelope);

            assertThat(JsonPath.parse(actualEnvelope))
                .isEqualToComparingFieldByFieldRecursively(JsonPath.parse(originalMetaFile));
        }
    }

    private InputStream getMetaFile() {
        return getClass().getResourceAsStream("/metafile.json");
    }
}
