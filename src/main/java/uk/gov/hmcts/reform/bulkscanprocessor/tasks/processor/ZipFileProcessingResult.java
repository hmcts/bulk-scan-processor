package uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor;

import uk.gov.hmcts.reform.bulkscanprocessor.model.db.DbEnvelope;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;

import java.util.List;

public class ZipFileProcessingResult {

    private final byte[] metadata;
    private final List<Pdf> pdfs;
    @SuppressWarnings("squid:S1135") // todo comments
    private DbEnvelope envelope; // TODO: make final

    public ZipFileProcessingResult(byte[] metadata, List<Pdf> pdfs) {
        this.metadata = metadata;
        this.pdfs = pdfs;
    }

    public byte[] getMetadata() {
        return metadata;
    }

    public List<Pdf> getPdfs() {
        return pdfs;
    }

    public DbEnvelope getEnvelope() {
        return envelope;
    }

    public void setEnvelope(DbEnvelope envelope) {
        this.envelope = envelope;
    }
}
