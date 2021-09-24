package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.ReceivedPayment;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.ReceivedPaymentRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.services.reports.ReceivedPaymentItem;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.bulkscanprocessor.model.common.Classification.SUPPLEMENTARY_EVIDENCE;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
@ExtendWith(SpringExtension.class)
class ReceivedPaymentRepositoryTest {
    @Autowired
    private ReceivedPaymentRepository reportRepo;

    @Autowired
    private EnvelopeRepository envelopeRepo;

    @Autowired
    private PaymentRepository paymentRepo;

    @AfterEach
    void cleanUp() {
        paymentRepo.deleteAll();
        envelopeRepo.deleteAll();
    }

    @Test
    void should_return_received_payments_for_date() {
        // given
        Envelope e1 = envelope("A", Instant.parse("2019-02-15T14:15:23.456Z"), "file1.zip");
        Payment p11 = payment(e1, "pay-11");
        Payment p12 = payment(e1, "pay-12");
        Envelope e2 = envelope("A", Instant.parse("2019-02-15T14:15:23.456Z"), "file2.zip");
        Payment p21 = payment(e2, "pay-31");
        Envelope e3 = envelope("B", Instant.parse("2019-02-15T14:15:23.456Z"), "file3.zip");
        Payment p31 = payment(e3, "pay-31");
        Envelope e4 = envelope("B", Instant.parse("2019-02-16T14:15:23.456Z"), "file4.zip");
        Payment p41 = payment(e4, "pay-41");
        Payment p42 = payment(e4, "pay-42");

        dbHasEnvelopes(e1, e2, e3, e4);
        dbHasPayments(p11, p12, p21, p31, p41, p42);

        // when
        List<ReceivedPayment> result = reportRepo.getReceivedPaymentsFor(LocalDate.of(2019, 2, 15));

        // then
        assertThat(result)
            .usingFieldByFieldElementComparator()
            .containsExactlyElementsOf(
                asList(
                    new ReceivedPaymentItem("A", 3),
                    new ReceivedPaymentItem("B", 1)
                )
            );
    }

    @Test
    void should_return_empty_list_if_no_payments() {
        // given
        Envelope e1 = envelope("A", Instant.parse("2019-02-15T14:15:23.456Z"), "file1.zip");
        Envelope e2 = envelope("A", Instant.parse("2019-02-15T14:15:23.456Z"), "file2.zip");
        Envelope e3 = envelope("B", Instant.parse("2019-02-15T14:15:23.456Z"), "file3.zip");
        Envelope e4 = envelope("B", Instant.parse("2019-02-16T14:15:23.456Z"), "file4.zip");

        dbHasEnvelopes(e1, e2, e3, e4);

        // when
        List<ReceivedPayment> result = reportRepo.getReceivedPaymentsFor(LocalDate.of(2019, 2, 15));

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void should_return_empty_list_if_no_envelopes() {
        // given
        // no envelopes

        // when
        List<ReceivedPayment> result = reportRepo.getReceivedPaymentsFor(LocalDate.of(2019, 2, 15));

        // then
        assertThat(result).isEmpty();
    }

    private Envelope envelope(String container, Instant date, String zipFileName) {

        return new Envelope(
            UUID.randomUUID().toString(),
            "jurisdiction1",
            date,
            date,
            date,
            zipFileName,
            "1234432112344321",
            null,
            SUPPLEMENTARY_EVIDENCE,
            emptyList(),
            emptyList(),
            emptyList(),
            container,
            null
        );
    }

    private Payment payment(Envelope envelope, String dcn) {
        Payment payment = new Payment(dcn);
        payment.setEnvelope(envelope);
        return payment;
    }

    private void dbHasEnvelopes(Envelope... envelopes) {
        envelopeRepo.saveAll(asList(envelopes));
        envelopeRepo.flush();
    }

    private void dbHasPayments(Payment... payments) {
        paymentRepo.saveAll(asList(payments));
    }
}
