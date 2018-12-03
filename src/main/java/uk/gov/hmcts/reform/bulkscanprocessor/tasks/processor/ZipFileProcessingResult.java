package uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor;

import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;

import java.util.List;

public class ZipFileProcessingResult {

    private final byte[] metadata;
    private final List<Pdf> pdfs;
    @SuppressWarnings("squid:S1135") // todo comments
    private Envelope envelope; // TODO: make final

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

    public Envelope getEnvelope() {
        return envelope;
    }

    public void setEnvelope(Envelope envelope) {
        this.envelope = envelope;
    }
}
