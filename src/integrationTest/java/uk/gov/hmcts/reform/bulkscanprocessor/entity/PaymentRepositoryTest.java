package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.helper.EnvelopeCreator;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class PaymentRepositoryTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private EnvelopeRepository envelopeRepository;

    @AfterEach
    public void cleanUp() {
        envelopeRepository.deleteAll();
    }
    
    @Test
    void should_update_payment_status() {

        List<Payment> payments = asList(
            new Payment("11234"),
            new Payment("22234"),
            new Payment("33234")
        );

        // given
        Envelope envelope = EnvelopeCreator.envelope(payments);
        envelopeRepository.save(envelope);
        List<Payment> paymentsDB = envelope.getPayments();

        assertThat(paymentsDB).as("Successful payment").extracting("status", "lastmodified")
            .containsExactly(tuple(null, null), tuple(null, null), tuple(null, null));


        List<String> paymentDcns = paymentsDB.stream().map(Payment::getDocumentControlNumber)
            .collect(Collectors.toList());

        //When
        int recordCount = paymentRepository.updateStatus(paymentDcns);
        assertThat(recordCount).isEqualTo(3);

        Optional<List<Payment>> successFullPayments = paymentRepository.findByDocumentControlNumberIn(paymentDcns);

        //Then
        assertThat(successFullPayments).isPresent();
        assertThat(successFullPayments.get().size()).isEqualTo(3);
        assertThat(successFullPayments.get()).as("Successful payment").extracting("status")
            .contains("SUBMITTED", "SUBMITTED", "SUBMITTED");

        assertThat(successFullPayments.get()).as("Successful payment")
            .extracting("lastmodified").isNotNull();
    }

    @Test
    void should_update_only_given_records() {

        List<Payment> paymentsNotBeUpdated = asList(
            new Payment("44444"),
            new Payment("55555"),
            new Payment("66666")
        );

        // given
        Envelope envelope = EnvelopeCreator.envelope(paymentsNotBeUpdated);
        envelopeRepository.save(envelope);

        List<Payment> payments = asList(
            new Payment("88234"),
            new Payment("99234"),
            new Payment("77234")
        );

        Envelope envelopeSecond = EnvelopeCreator.envelope(payments);
        envelopeRepository.save(envelopeSecond);
        List<Payment> paymentsDB = envelopeSecond.getPayments();

        List<String> paymentDcns = paymentsDB.stream().map(Payment::getDocumentControlNumber)
            .collect(Collectors.toList());

        //When
        int recordCount = paymentRepository.updateStatus(paymentDcns);
        assertThat(recordCount).isEqualTo(3);
    }
}
