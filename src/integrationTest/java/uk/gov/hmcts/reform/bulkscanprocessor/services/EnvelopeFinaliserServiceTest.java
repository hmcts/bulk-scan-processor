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
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.DocumentType;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.OcrData;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.OcrDataField;

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
            createScannableItems(3, createOcrData(), null)
        );

        UUID envelopeId = envelopeRepository.saveAndFlush(envelope).getId();
        Long ccdId = 312312L;
        String envelopeCcdAction = "EXCEPTION_RECORD";
        // when
        envelopeFinaliserService.finaliseEnvelope(envelopeId, ccdId, envelopeCcdAction);

        // then
        Optional<Envelope> finalisedEnvelope = envelopeRepository.findById(envelopeId);

        assertThat(finalisedEnvelope).isPresent();
        assertThat(finalisedEnvelope.get().getStatus()).isEqualTo(Status.COMPLETED);
        assertThat(finalisedEnvelope.get().getScannableItems())
            .allMatch(item -> item.getOcrData() == null && item.getOcrValidationWarnings() == null);
        assertThat(finalisedEnvelope.get().getCcdId()).isEqualTo(ccdId.toString());
        assertThat(finalisedEnvelope.get().getEnvelopeCcdAction()).isEqualTo(envelopeCcdAction);
    }

    @Test
    public void finaliseEnvelope_should_update_envelope_status_and_clear_all_ocr_data_and_warnings() {
        // given
        Envelope envelope = envelope(
            "JURISDICTION1",
            Status.NOTIFICATION_SENT,
            createScannableItems(3, createOcrData(), createWarnings())
        );

        UUID envelopeId = envelopeRepository.saveAndFlush(envelope).getId();
        Long ccdId = 909033412141414L;
        String envelopeCcdAction = "AUTO_ATTACHED_TO_CASE";
        // when
        envelopeFinaliserService.finaliseEnvelope(envelopeId, ccdId, envelopeCcdAction);

        // then
        Optional<Envelope> finalisedEnvelope = envelopeRepository.findById(envelopeId);

        assertThat(finalisedEnvelope).isPresent();
        assertThat(finalisedEnvelope.get().getStatus()).isEqualTo(Status.COMPLETED);
        assertThat(finalisedEnvelope.get().getScannableItems())
            .allMatch(item -> item.getOcrData() == null && item.getOcrValidationWarnings() == null);
        assertThat(finalisedEnvelope.get().getCcdId()).isEqualTo(ccdId);
        assertThat(finalisedEnvelope.get().getEnvelopeCcdAction()).isEqualTo(envelopeCcdAction);
    }

    @Test
    public void finaliseEnvelope_should_not_update_other_envelopes_with_ocr_data() {
        // given
        Envelope envelope1 = envelope(
            "JURISDICTION1",
            Status.NOTIFICATION_SENT,
            createScannableItems(3, createOcrData(), null)
        );
        Envelope envelope2 = envelope(
            "JURISDICTION1",
            Status.NOTIFICATION_SENT,
            createScannableItems(3, createOcrData(), null)
        );

        UUID envelope1Id = envelopeRepository.saveAndFlush(envelope1).getId();
        UUID envelope2Id = envelopeRepository.saveAndFlush(envelope2).getId();
        Long ccdId = 9843232L;
        String envelopeCcdAction = "EXCEPTION_RECORD";

        // when
        envelopeFinaliserService.finaliseEnvelope(envelope1Id, ccdId, envelopeCcdAction);

        // then
        Optional<Envelope> unaffectedEnvelope = envelopeRepository.findById(envelope2Id);

        assertThat(unaffectedEnvelope).isPresent();
        assertThat(unaffectedEnvelope.get().getStatus()).isEqualTo(Status.NOTIFICATION_SENT);
        assertThat(unaffectedEnvelope.get().getScannableItems())
            .allMatch(item -> item.getOcrData() != null && item.getOcrValidationWarnings() == null);
        assertThat(unaffectedEnvelope.get().getCcdId()).isEqualTo(ccdId);
        assertThat(unaffectedEnvelope.get().getEnvelopeCcdAction()).isEqualTo(envelopeCcdAction);
    }

    @Test
    public void finaliseEnvelope_should_not_update_other_envelopes_with_ocr_data_and_warnings() {
        // given
        Envelope envelope1 = envelope(
            "JURISDICTION1",
            Status.NOTIFICATION_SENT,
            createScannableItems(3, createOcrData(), createWarnings())
        );
        Envelope envelope2 = envelope(
            "JURISDICTION1",
            Status.NOTIFICATION_SENT,
            createScannableItems(3, createOcrData(), createWarnings())
        );

        UUID envelope1Id = envelopeRepository.saveAndFlush(envelope1).getId();
        UUID envelope2Id = envelopeRepository.saveAndFlush(envelope2).getId();

        Long ccdId = 31221321L;
        String envelopeCcdAction = "EXCEPTION_RECORD";
        // when
        envelopeFinaliserService.finaliseEnvelope(envelope1Id, ccdId, envelopeCcdAction);

        // then
        Optional<Envelope> unaffectedEnvelope = envelopeRepository.findById(envelope2Id);

        assertThat(unaffectedEnvelope).isPresent();
        assertThat(unaffectedEnvelope.get().getStatus()).isEqualTo(Status.NOTIFICATION_SENT);
        assertThat(unaffectedEnvelope.get().getScannableItems())
            .allMatch(item -> item.getOcrData() != null && item.getOcrValidationWarnings() != null);
        assertThat(unaffectedEnvelope.get().getCcdId()).isEqualTo(ccdId);
        assertThat(unaffectedEnvelope.get().getEnvelopeCcdAction()).isEqualTo(envelopeCcdAction);
    }

    @Test
    public void finaliseEnvelope_should_create_event_of_type_completed() {
        // given
        Envelope envelope = envelope("JURISDICTION1", Status.NOTIFICATION_SENT, emptyList());
        UUID envelopeId = envelopeRepository.saveAndFlush(envelope).getId();

        // when
        envelopeFinaliserService.finaliseEnvelope(envelopeId, 2321L, "EXCEPTION_RECORD");

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
            envelopeFinaliserService.finaliseEnvelope(nonExistingId, null, null)
        )
            .isInstanceOf(EnvelopeNotFoundException.class)
            .hasMessage(String.format("Envelope with ID %s couldn't be found", nonExistingId));
    }

    private List<ScannableItem> createScannableItems(
        int count,
        OcrData ocrData,
        String[] warnings
    ) {
        return new ArrayList<>(Stream.generate(() -> {
            Instant instant = Instant.parse("2018-06-23T12:34:56.123Z");

            ScannableItem scannableItem = new ScannableItem(
                UUID.randomUUID().toString(),
                instant,
                "test",
                "test",
                "return",
                instant,
                ocrData,
                "1111001.pdf",
                "test",
                DocumentType.CHERISHED,
                null,
                warnings
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

    private String[] createWarnings() {
        return new String[]{"warning1"};
    }
}
