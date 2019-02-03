package uk.gov.hmcts.reform.bulkscanprocessor.services;

import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.EnvelopeNotFoundException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.DocumentType;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.model.ocr.OcrData;
import uk.gov.hmcts.reform.bulkscanprocessor.model.ocr.OcrDataField;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator.envelope;

@RunWith(MockitoJUnitRunner.class)
public class EnvelopeFinaliserServiceTest {

    @Mock
    private EnvelopeRepository envelopeRepository;

    @Mock
    private ProcessEventRepository processEventRepository;

    private EnvelopeFinaliserService envelopeFinaliserService;

    @Before
    public void setUp() {
        envelopeFinaliserService = new EnvelopeFinaliserService(
            envelopeRepository,
            processEventRepository
        );
    }

    @Test
    public void finaliseEnvelope_should_update_envelope_status_clear_all_ocr_data() {
        // given
        UUID envelopeId = UUID.randomUUID();

        Envelope envelope = envelope(
            "JURISDICTION1",
            Status.NOTIFICATION_SENT,
            createScannableItemsWithOcrData(3)
        );

        given(envelopeRepository.findById(envelopeId)).willReturn(Optional.of(envelope));

        // when
        envelopeFinaliserService.finaliseEnvelope(envelopeId);

        // then
        verify(envelopeRepository).findById(envelopeId);
        ArgumentCaptor<Envelope> envelopeCaptor = ArgumentCaptor.forClass(Envelope.class);
        verify(envelopeRepository).save(envelopeCaptor.capture());
        verifyNoMoreInteractions(envelopeRepository);

        Envelope savedEnvelope = envelopeCaptor.getValue();
        assertThat(savedEnvelope.getStatus()).isEqualTo(Status.COMPLETED);

        assertThat(savedEnvelope.getScannableItems())
            .extracting("ocrData")
            .containsOnlyNulls();
    }

    @Test
    public void finaliseEnvelope_should_create_event_of_type_completed() {
        // given
        Envelope envelope = envelope("JURISDICTION1", Status.NOTIFICATION_SENT, emptyList());

        given(envelopeRepository.findById(any())).willReturn(Optional.of(envelope));

        // when
        envelopeFinaliserService.finaliseEnvelope(UUID.randomUUID());

        // then
        ArgumentCaptor<ProcessEvent> processEventCaptor = ArgumentCaptor.forClass(ProcessEvent.class);
        verify(processEventRepository).save(processEventCaptor.capture());
        verifyNoMoreInteractions(processEventRepository);

        ProcessEvent savedEvent = processEventCaptor.getValue();
        assertThat(savedEvent.getContainer()).isEqualTo(envelope.getContainer());
        assertThat(savedEvent.getEvent()).isEqualTo(Event.COMPLETED);
        assertThat(savedEvent.getZipFileName()).isEqualTo(envelope.getZipFileName());
    }

    @Test
    public void finaliseEnvelope_should_throw_exception_when_envelope_is_not_found() {
        given(envelopeRepository.findById(any())).willReturn(Optional.empty());

        assertThatThrownBy(() ->
            envelopeFinaliserService.finaliseEnvelope(
                UUID.fromString("ef2565fd-74f5-418e-9d8c-7bf847edde80")
            )
        )
            .isInstanceOf(EnvelopeNotFoundException.class)
            .hasMessage("Envelope with ID ef2565fd-74f5-418e-9d8c-7bf847edde80 couldn't be found");
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
