package uk.gov.hmcts.reform.bulkscanprocessor.services.zipfilestatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.NonScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Payment;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEventRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ScannableItemRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.Status;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.zipfilestatus.ZipFileEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.zipfilestatus.ZipFileEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.zipfilestatus.ZipFileStatus;

import java.security.InvalidParameterException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static java.time.Instant.now;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.mapper.EnvelopeResponseMapper.toNonScannableItemsResponse;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.mapper.EnvelopeResponseMapper.toPaymentsResponse;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.mapper.EnvelopeResponseMapper.toScannableItemsResponse;

@ExtendWith(MockitoExtension.class)
public class ZipFileStatusServiceTest {

    @Mock private ProcessEventRepository eventRepo;
    @Mock private EnvelopeRepository envelopeRepo;
    @Mock private ScannableItemRepository scannableItemRepo;

    private ZipFileStatusService service;

    @BeforeEach
    public void setUp() {
        this.service = new ZipFileStatusService(eventRepo, envelopeRepo, scannableItemRepo);
    }

    @Test
    public void should_return_envelopes_and_events_from_db() {
        // given
        List<ProcessEvent> events = asList(
            event(Event.DOC_UPLOADED, "A", now(), null),
            event(Event.DOC_PROCESSED_NOTIFICATION_SENT, "A", now().plusSeconds(1), null),
            event(Event.DOC_FAILURE, "B", now().minusSeconds(2), "reason2")
        );

        List<Envelope> envelopes = asList(envelope("A"), envelope("B"));

        given(envelopeRepo.findByZipFileName("hello.zip")).willReturn(envelopes);
        given(eventRepo.findByZipFileName("hello.zip")).willReturn(events);

        // when
        ZipFileStatus result = service.getStatusFor("hello.zip");

        // then
        assertThat(result.fileName).isEqualTo("hello.zip");

        assertThat(result.envelopes)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new ZipFileEnvelope(
                    envelopes.get(0).getId().toString(),
                    envelopes.get(0).getContainer(),
                    envelopes.get(0).getStatus().name(),
                    envelopes.get(0).getCcdId(),
                    envelopes.get(0).getEnvelopeCcdAction(),
                    envelopes.get(0).isZipDeleted(),
                    envelopes.get(0).getRescanFor(),
                    envelopes.get(0).getClassification(),
                    envelopes.get(0).getJurisdiction(),
                    envelopes.get(0).getCaseNumber(),
                    toScannableItemsResponse(envelopes.get(0).getScannableItems()),
                    toNonScannableItemsResponse(envelopes.get(0).getNonScannableItems()),
                    toPaymentsResponse(envelopes.get(0).getPayments())
                ),
                new ZipFileEnvelope(
                    envelopes.get(1).getId().toString(),
                    envelopes.get(1).getContainer(),
                    envelopes.get(1).getStatus().name(),
                    envelopes.get(1).getCcdId(),
                    envelopes.get(1).getEnvelopeCcdAction(),
                    envelopes.get(1).isZipDeleted(),
                    envelopes.get(1).getRescanFor(),
                    envelopes.get(1).getClassification(),
                    envelopes.get(1).getJurisdiction(),
                    envelopes.get(1).getCaseNumber(),
                    toScannableItemsResponse(envelopes.get(1).getScannableItems()),
                    toNonScannableItemsResponse(envelopes.get(1).getNonScannableItems()),
                    toPaymentsResponse(envelopes.get(1).getPayments())
                )
            );

        assertThat(result.events)
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new ZipFileEvent(
                    events.get(0).getEvent().name(),
                    events.get(0).getContainer(),
                    events.get(0).getCreatedAt(),
                    events.get(0).getReason()
                ),
                new ZipFileEvent(
                    events.get(1).getEvent().name(),
                    events.get(1).getContainer(),
                    events.get(1).getCreatedAt(),
                    events.get(1).getReason()
                ),
                new ZipFileEvent(
                    events.get(2).getEvent().name(),
                    events.get(2).getContainer(),
                    events.get(2).getCreatedAt(),
                    events.get(2).getReason()
                )
            );
    }

    @Test
    public void should_return_empty_lists_when_no_data_for_zip_file_was_found() {
        // given
        given(envelopeRepo.findByZipFileName("hello.zip")).willReturn(emptyList());
        given(eventRepo.findByZipFileName("hello.zip")).willReturn(emptyList());

        // when
        ZipFileStatus result = service.getStatusFor("hello.zip");

        // then
        assertThat(result).isNotNull();
        assertThat(result.envelopes).isNotNull().isEmpty();
        assertThat(result.events).isNotNull().isEmpty();
    }

    @Test
    // A valid dcn parameter is at least 6 characters long
    public void should_return_list_of_ZipFileStatus_for_valid_dcn_parameter_search() {
        // given
        List<ProcessEvent> events1 = asList(
            event(Event.DOC_UPLOADED, "A", now(), null),
            event(Event.DOC_PROCESSED_NOTIFICATION_SENT, "A", now().plusSeconds(1), null),
            event(Event.DOC_FAILURE, "B", now().minusSeconds(2), "reason2")
        );
        List<ProcessEvent> events2 = asList(
            event(Event.DOC_UPLOADED, "C", now(), null),
            event(Event.DOC_PROCESSED_NOTIFICATION_SENT, "C", now().plusSeconds(1), null),
            event(Event.DOC_FAILURE, "D", now().minusSeconds(2), "reason3")
        );
        List<Envelope> envelopes1 = asList(envelope("A"), envelope("B"));
        List<Envelope> envelopes2 = asList(envelope("C"), envelope("D"));
        var documentControlNumber = "100099";
        var zipFileNames = Arrays.asList("hello1.zip", "hello2.zip");

        given(scannableItemRepo.findByDcn(documentControlNumber)).willReturn(zipFileNames);
        given(envelopeRepo.findByZipFileName("hello1.zip")).willReturn(envelopes1);
        given(eventRepo.findByZipFileName("hello1.zip")).willReturn(events1);
        given(envelopeRepo.findByZipFileName("hello2.zip")).willReturn(envelopes2);
        given(eventRepo.findByZipFileName("hello2.zip")).willReturn(events2);

        // when
        var zipFileStatusList = service.getStatusByDcn(documentControlNumber);

        // then
        assertThat(zipFileStatusList.size() == 2);
        assertThat(zipFileStatusList.get(0).fileName).isEqualTo("hello1.zip");
        assertThat(zipFileStatusList.get(1).fileName).isEqualTo("hello2.zip");
        assertThat(zipFileStatusList.get(0).envelopes)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new ZipFileEnvelope(
                    envelopes1.get(0).getId().toString(),
                    envelopes1.get(0).getContainer(),
                    envelopes1.get(0).getStatus().name(),
                    envelopes1.get(0).getCcdId(),
                    envelopes1.get(0).getEnvelopeCcdAction(),
                    envelopes1.get(0).isZipDeleted(),
                    envelopes1.get(0).getRescanFor(),
                    envelopes1.get(0).getClassification(),
                    envelopes1.get(0).getJurisdiction(),
                    envelopes1.get(0).getCaseNumber(),
                    toScannableItemsResponse(envelopes1.get(0).getScannableItems()),
                    toNonScannableItemsResponse(envelopes1.get(0).getNonScannableItems()),
                    toPaymentsResponse(envelopes1.get(0).getPayments())
                ),
                new ZipFileEnvelope(
                    envelopes1.get(1).getId().toString(),
                    envelopes1.get(1).getContainer(),
                    envelopes1.get(1).getStatus().name(),
                    envelopes1.get(1).getCcdId(),
                    envelopes1.get(1).getEnvelopeCcdAction(),
                    envelopes1.get(1).isZipDeleted(),
                    envelopes1.get(1).getRescanFor(),
                    envelopes1.get(1).getClassification(),
                    envelopes1.get(1).getJurisdiction(),
                    envelopes1.get(1).getCaseNumber(),
                    toScannableItemsResponse(envelopes1.get(1).getScannableItems()),
                    toNonScannableItemsResponse(envelopes1.get(1).getNonScannableItems()),
                    toPaymentsResponse(envelopes1.get(1).getPayments())
                )
            );
        assertThat(zipFileStatusList.get(1).envelopes)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new ZipFileEnvelope(
                    envelopes2.get(0).getId().toString(),
                    envelopes2.get(0).getContainer(),
                    envelopes2.get(0).getStatus().name(),
                    envelopes2.get(0).getCcdId(),
                    envelopes2.get(0).getEnvelopeCcdAction(),
                    envelopes2.get(0).isZipDeleted(),
                    envelopes2.get(0).getRescanFor(),
                    envelopes2.get(0).getClassification(),
                    envelopes2.get(0).getJurisdiction(),
                    envelopes2.get(0).getCaseNumber(),
                    toScannableItemsResponse(envelopes2.get(0).getScannableItems()),
                    toNonScannableItemsResponse(envelopes2.get(0).getNonScannableItems()),
                    toPaymentsResponse(envelopes2.get(0).getPayments())
                ),
                new ZipFileEnvelope(
                    envelopes2.get(1).getId().toString(),
                    envelopes2.get(1).getContainer(),
                    envelopes2.get(1).getStatus().name(),
                    envelopes2.get(1).getCcdId(),
                    envelopes2.get(1).getEnvelopeCcdAction(),
                    envelopes2.get(1).isZipDeleted(),
                    envelopes2.get(1).getRescanFor(),
                    envelopes2.get(1).getClassification(),
                    envelopes2.get(1).getJurisdiction(),
                    envelopes2.get(1).getCaseNumber(),
                    toScannableItemsResponse(envelopes2.get(1).getScannableItems()),
                    toNonScannableItemsResponse(envelopes2.get(1).getNonScannableItems()),
                    toPaymentsResponse(envelopes2.get(1).getPayments())
                )
            );
        assertThat(zipFileStatusList.get(0).events)
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new ZipFileEvent(
                    events1.get(0).getEvent().name(),
                    events1.get(0).getContainer(),
                    events1.get(0).getCreatedAt(),
                    events1.get(0).getReason()
                ),
                new ZipFileEvent(
                    events1.get(1).getEvent().name(),
                    events1.get(1).getContainer(),
                    events1.get(1).getCreatedAt(),
                    events1.get(1).getReason()
                ),
                new ZipFileEvent(
                    events1.get(2).getEvent().name(),
                    events1.get(2).getContainer(),
                    events1.get(2).getCreatedAt(),
                    events1.get(2).getReason()
                )
            );
        assertThat(zipFileStatusList.get(1).events)
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                new ZipFileEvent(
                    events2.get(0).getEvent().name(),
                    events2.get(0).getContainer(),
                    events2.get(0).getCreatedAt(),
                    events2.get(0).getReason()
                ),
                new ZipFileEvent(
                    events2.get(1).getEvent().name(),
                    events2.get(1).getContainer(),
                    events2.get(1).getCreatedAt(),
                    events2.get(1).getReason()
                ),
                new ZipFileEvent(
                    events2.get(2).getEvent().name(),
                    events2.get(2).getContainer(),
                    events2.get(2).getCreatedAt(),
                    events2.get(2).getReason()
                )
            );
    }

    @Test
    public void should_return_invalid_parameter_exception_for_dcn_less_than_6_chars() throws Exception {

        //given
        var documentControllNumber = "1000";

        //when
        //then
        assertThatThrownBy(() -> service.getStatusByDcn("1000"))
            .isInstanceOf(InvalidParameterException.class)
            .hasMessageMatching("DCN number has to be at least 6 characters long");
    }

    private Envelope envelope(String container) {
        Envelope envelope = mock(Envelope.class);
        given(envelope.getId()).willReturn(UUID.randomUUID());
        given(envelope.getContainer()).willReturn(container);
        given(envelope.getStatus()).willReturn(Status.UPLOADED);
        given(envelope.getRescanFor()).willReturn("envelope1.zip");
        given(envelope.getScannableItems())
            .willReturn(asList(mock(ScannableItem.class), mock(ScannableItem.class)));
        given(envelope.getNonScannableItems())
            .willReturn(asList(mock(NonScannableItem.class), mock(NonScannableItem.class)));
        given(envelope.getPayments())
            .willReturn(asList(mock(Payment.class), mock(Payment.class)));
        return envelope;
    }

    private ProcessEvent event(Event eventType, String container, Instant createdAt, String reason) {
        ProcessEvent event = mock(ProcessEvent.class);
        given(event.getEvent()).willReturn(eventType);
        given(event.getContainer()).willReturn(container);
        given(event.getCreatedAt()).willReturn(createdAt);
        given(event.getReason()).willReturn(reason);
        return event;
    }
}
