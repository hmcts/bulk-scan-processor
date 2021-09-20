package uk.gov.hmcts.reform.bulkscanprocessor.entity.reports;

public interface ReceivedScannableItemPerDocumentType {
    String getContainer();

    String getDocumentType();

    int getCount();
}
