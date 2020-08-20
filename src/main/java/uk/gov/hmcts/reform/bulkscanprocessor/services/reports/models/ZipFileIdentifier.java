package uk.gov.hmcts.reform.bulkscanprocessor.services.reports.models;

import java.util.Objects;

public class ZipFileIdentifier {
    private final String zipFileName;
    private final String container;

    public ZipFileIdentifier(String zipFileName, String container) {
        this.zipFileName = zipFileName;
        this.container = container;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ZipFileIdentifier that = (ZipFileIdentifier) o;
        return Objects.equals(zipFileName, that.zipFileName)
            && Objects.equals(container, that.container);
    }

    @Override
    public int hashCode() {
        return Objects.hash(zipFileName, container);
    }
}
