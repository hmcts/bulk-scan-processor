package uk.gov.hmcts.reform.bulkscanprocessor.services.reports;

import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.ReceivedScannableItemPerDocumentType;

public class ReceivedScannableItemPerDocumentTypeItem implements ReceivedScannableItemPerDocumentType {
    private final String container;
    private final String documentType;
    private final int count;

    public ReceivedScannableItemPerDocumentTypeItem(
            String container,
            String documentType,
            int count
    ) {
        this.container = container;
        this.documentType = documentType;
        this.count = count;
    }

    @Override
    public String getContainer() {
        return container;
    }

    @Override
    public String getDocumentType() {
        return documentType;
    }

    @Override
    public int getCount() {
        return count;
    }
}
