package uk.gov.hmcts.reform.bulkscanprocessor.services.payment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.PaymentRepository;
import uk.gov.hmcts.reform.bulkscanprocessor.model.in.PaymentInfo;
import uk.gov.hmcts.reform.bulkscanprocessor.model.in.PaymentRequest;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    @Mock
    private PaymentRepository repository;

    @Captor
    ArgumentCaptor<List<String>> listArgumentCaptor;

    PaymentService paymentService;


    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(repository);
    }

    @Test
    void should_invoke_paymentRepo_update() {
        List<PaymentInfo> paymentInfoList = Arrays.asList(new PaymentInfo("123"),
                                                          new PaymentInfo("234"), new PaymentInfo("567"));
        //Given
        PaymentRequest paymentRequest = new PaymentRequest(paymentInfoList);

        //When
        paymentService.updatePaymentStatus(paymentRequest);

        //Then
        verify(repository).updateStatus(listArgumentCaptor.capture());
        assertThat(listArgumentCaptor.getValue()).containsExactly("123", "234", "567");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void should_not_invoke_paymentRepo_update(List<PaymentInfo> list) {
        //Given
        PaymentRequest paymentRequest = new PaymentRequest(list);

        //When
        paymentService.updatePaymentStatus(paymentRequest);

        //Then
        verify(repository, never()).updateStatus(any());
    }

}
