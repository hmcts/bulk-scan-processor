package uk.gov.hmcts.reform.bulkscanprocessor.services.reports;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.ReceivedScannableItem;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.ReceivedScannableItemPerDocumentType;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.ReceivedScannableItemRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Service to handle received scannable items.
 */
@Service
public class ReceivedScannableItemsService {

    private final ReceivedScannableItemRepository receivedScannableItemRepository;

    /**
     * Constructor for the ReceivedScannableItemsService.
     * @param receivedScannableItemRepository The repository for received scannable item
     */
    public ReceivedScannableItemsService(ReceivedScannableItemRepository receivedScannableItemRepository) {
        this.receivedScannableItemRepository = receivedScannableItemRepository;
    }

    /**
     * Get the received scannable items for a date.
     * @param date The date
     * @return The received scannable items
     */
    public List<ReceivedScannableItem> getReceivedScannableItems(LocalDate date) {
        return receivedScannableItemRepository.getReceivedScannableItemsFor(date);
    }

    /**
     * Get the received scannable items per document type for a date.
     * @param date The date
     * @return The received scannable items per document type
     */
    public List<ReceivedScannableItemPerDocumentType> getReceivedScannableItemsPerDocumentType(LocalDate date) {
        return receivedScannableItemRepository.getReceivedScannableItemsPerDocumentTypeFor(date);
    }
}
