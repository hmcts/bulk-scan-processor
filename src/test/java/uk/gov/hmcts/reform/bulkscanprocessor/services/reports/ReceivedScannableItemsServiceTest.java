package uk.gov.hmcts.reform.bulkscanprocessor.services.reports;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.ReceivedScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.ReceivedScannableItemRepository;

import java.time.LocalDate;
import java.util.List;
import javax.persistence.EntityNotFoundException;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ReceivedScannableItemsServiceTest {

    @Mock
    private ReceivedScannableItemRepository receivedScannableItemRepository;

    private ReceivedScannableItemsService receivedScannableItemsService;

    @BeforeEach
    void setUp() {
        receivedScannableItemsService = new ReceivedScannableItemsService(receivedScannableItemRepository);
    }

    @Test
    void should_return_received_scannable_items() {
        // given
        LocalDate date = LocalDate.now();
        List<ReceivedScannableItem> receivedScannableItems = asList(
                new ReceivedScannableItemItem("c1", 3),
                new ReceivedScannableItemItem("c2", 4)
        );
        given(receivedScannableItemRepository.getReceivedScannableItemsFor(date))
                .willReturn(receivedScannableItems);

        // when
        List<ReceivedScannableItem> res = receivedScannableItemsService.getReceivedScannableItems(date);

        // then
        assertThat(res).isSameAs(receivedScannableItems);
    }

    @Test
    void should_rethrow_exception() {
        // given
        LocalDate date = LocalDate.now();
        given(receivedScannableItemRepository.getReceivedScannableItemsFor(date))
                .willThrow(new EntityNotFoundException("msg"));

        // when
        // then
        assertThatThrownBy(
            () -> receivedScannableItemsService.getReceivedScannableItems(date)
        )
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessage("msg");
    }
}