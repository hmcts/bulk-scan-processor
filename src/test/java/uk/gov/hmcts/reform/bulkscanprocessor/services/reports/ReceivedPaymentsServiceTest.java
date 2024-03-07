package uk.gov.hmcts.reform.bulkscanprocessor.services.reports;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.ReceivedPayment;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.ReceivedPaymentRepository;

import java.time.LocalDate;
import java.util.List;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ReceivedPaymentsServiceTest {

    @Mock
    private ReceivedPaymentRepository receivedPaymentRepository;

    private ReceivedPaymentsService receivedPaymentsService;

    @BeforeEach
    void setUp() {
        receivedPaymentsService = new ReceivedPaymentsService(receivedPaymentRepository);
    }

    @Test
    void should_return_received_scannable_items() {
        // given
        LocalDate date = LocalDate.now();
        List<ReceivedPayment> receivedPayments = asList(
                new ReceivedPaymentItem("c1", 3),
                new ReceivedPaymentItem("c2", 4)
        );
        given(receivedPaymentRepository.getReceivedPaymentsFor(date))
                .willReturn(receivedPayments);

        // when
        List<ReceivedPayment> res = receivedPaymentsService.getReceivedPayments(date);

        // then
        assertThat(res).isSameAs(receivedPayments);
    }

    @Test
    void should_rethrow_exception() {
        // given
        LocalDate date = LocalDate.now();
        given(receivedPaymentRepository.getReceivedPaymentsFor(date))
                .willThrow(new EntityNotFoundException("msg"));

        // when
        // then
        assertThatThrownBy(
            () -> receivedPaymentsService.getReceivedPayments(date)
        )
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessage("msg");
    }
}
