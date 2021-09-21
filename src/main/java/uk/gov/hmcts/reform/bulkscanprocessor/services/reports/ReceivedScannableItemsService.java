package uk.gov.hmcts.reform.bulkscanprocessor.services.reports;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.ReceivedScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.ReceivedScannableItemPerDocumentType;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.ReceivedScannableItemRepository;

import java.time.LocalDate;
import java.util.List;

@Service
public class ReceivedScannableItemsService {

    private final ReceivedScannableItemRepository receivedScannableItemRepository;

    public ReceivedScannableItemsService(ReceivedScannableItemRepository receivedScannableItemRepository) {
        this.receivedScannableItemRepository = receivedScannableItemRepository;
    }

    public List<ReceivedScannableItem> getReceivedScannableItems(LocalDate date) {
        return receivedScannableItemRepository.getReceivedScannableItemsFor(date);
    }

    public List<ReceivedScannableItemPerDocumentType> getReceivedScannableItemsPerDocumentType(LocalDate date) {
        return receivedScannableItemRepository.getReceivedScannableItemsPerDocumentTypeFor(date);
    }
}
