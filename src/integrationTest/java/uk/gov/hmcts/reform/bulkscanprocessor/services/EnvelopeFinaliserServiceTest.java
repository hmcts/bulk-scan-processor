package uk.gov.hmcts.reform.bulkscanprocessor.services;

import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.OcrData;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.OcrDataField;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.DocumentType;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator.envelope;

@RunWith(SpringRunner.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
public class EnvelopeFinaliserServiceTest {

    @Autowired
    private EnvelopeRepository envelopeRepository;

    @Autowired
    private ProcessEventRepository processEventRepository;

    private EnvelopeFinaliserService envelopeFinaliserService;

    @Before
    public void setUp() {
        envelopeFinaliserService = new EnvelopeFinaliserService(
            envelopeRepository,
            processEventRepository
        );
    }

    @After
    public void tearDown() {
        envelopeRepository.deleteAll();
        processEventRepository.deleteAll();
    }

    @Test
    public void finaliseEnvelope_should_update_envelope_status_and_clear_all_ocr_data() {
        // given
        Envelope envelope = envelope(
            "JURISDICTION1",
            Status.NOTIFICATION_SENT,
            createScannableItemsWithOcrData(3)
        );

        UUID envelopeId = envelopeRepository.saveAndFlush(envelope).getId();

        // when
        envelopeFinaliserService.finaliseEnvelope(envelopeId);

        // then
        Optional<Envelope> finalisedEnvelope = envelopeRepository.findById(envelopeId);

        assertThat(finalisedEnvelope).isPresent();
        assertThat(finalisedEnvelope.get().getStatus()).isEqualTo(Status.COMPLETED);
        assertThat(finalisedEnvelope.get().getScannableItems()).allMatch(item -> item.getOcrData() == null);
    }

    @Test
    public void finaliseEnvelope_should_not_update_other_envelopes() {
        // given
        Envelope envelope1 = envelope("JURISDICTION1", Status.NOTIFICATION_SENT, createScannableItemsWithOcrData(3));
        Envelope envelope2 = envelope("JURISDICTION1", Status.NOTIFICATION_SENT, createScannableItemsWithOcrData(3));

        UUID envelope1Id = envelopeRepository.saveAndFlush(envelope1).getId();
        UUID envelope2Id = envelopeRepository.saveAndFlush(envelope2).getId();

        // when
        envelopeFinaliserService.finaliseEnvelope(envelope1Id);

        // then
        Optional<Envelope> unaffectedEnvelope = envelopeRepository.findById(envelope2Id);

        assertThat(unaffectedEnvelope).isPresent();
        assertThat(unaffectedEnvelope.get().getStatus()).isEqualTo(Status.NOTIFICATION_SENT);
        assertThat(unaffectedEnvelope.get().getScannableItems()).allMatch(item -> item.getOcrData() != null);
    }

    @Test
    public void finaliseEnvelope_should_create_event_of_type_completed() {
        // given
        Envelope envelope = envelope("JURISDICTION1", Status.NOTIFICATION_SENT, emptyList());
        UUID envelopeId = envelopeRepository.saveAndFlush(envelope).getId();

        // when
        envelopeFinaliserService.finaliseEnvelope(envelopeId);

        // then
        List<ProcessEvent> savedEvents = processEventRepository.findByZipFileName(envelope.getZipFileName());
        assertThat(savedEvents.size()).isOne();

        ProcessEvent savedEvent = savedEvents.get(0);
        assertThat(savedEvent.getContainer()).isEqualTo(envelope.getContainer());
        assertThat(savedEvent.getEvent()).isEqualTo(Event.COMPLETED);
        assertThat(savedEvent.getZipFileName()).isEqualTo(envelope.getZipFileName());
    }

    @Test
    public void finaliseEnvelope_should_throw_exception_when_envelope_is_not_found() {
        UUID nonExistingId = UUID.fromString("ef2565fd-74f5-418e-9d8c-7bf847edde80");

        assertThatThrownBy(() ->
            envelopeFinaliserService.finaliseEnvelope(nonExistingId)
        )
            .isInstanceOf(EnvelopeNotFoundException.class)
            .hasMessage(String.format("Envelope with ID %s couldn't be found", nonExistingId));
    }

    private List<ScannableItem> createScannableItemsWithOcrData(int count) {
        return new ArrayList<>(Stream.generate(() -> {
            Instant instant = Instant.parse("2018-06-23T12:34:56.123Z");

            ScannableItem scannableItem = new ScannableItem(
                "1111001",
                instant,
                "test",
                "test",
                "return",
                instant,
                createOcrData(),
                "1111001.pdf",
                "test",
                DocumentType.CHERISHED,
                null
            );

            scannableItem.setDocumentUuid("0fa1ab60-f836-43aa-8c65-b07cc9bebceb");
            return scannableItem;
        })
            .limit(count)
            .collect(toList()));
    }

    private OcrData createOcrData() {
        return new OcrData(Arrays.asList(
            new OcrDataField(new TextNode("key1"), new TextNode("value1")),
            new OcrDataField(new TextNode("key2"), new TextNode("value2"))
        ));
    }
}
