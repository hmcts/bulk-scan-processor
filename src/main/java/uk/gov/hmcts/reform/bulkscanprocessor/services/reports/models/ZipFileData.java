package uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models;

import java.util.List;
import java.util.Objects;

public abstract class ZipFileData {
    public final String zipFileName;
    public final String container;
    public final List<String> scannableItemDcns;
    public final List<String> paymentDcns;

    public ZipFileData(
        String zipFileName,
        String container,
        List<String> scannableItemDcns,
        List<String> paymentDcns
    ) {
        this.zipFileName = zipFileName;
        this.container = container;
        this.scannableItemDcns = scannableItemDcns;
        this.paymentDcns = paymentDcns;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ZipFileData)) {
            return false;
        }
        ZipFileData that = (ZipFileData) o;
        return zipFileName.equals(that.zipFileName)
            && container.equals(that.container);
    }

    @Override
    public int hashCode() {
        return Objects.hash(zipFileName, container);
    }
}
