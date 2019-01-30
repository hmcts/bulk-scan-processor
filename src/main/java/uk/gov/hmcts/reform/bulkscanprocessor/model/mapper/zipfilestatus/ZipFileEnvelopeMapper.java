package uk.gov.hmcts.reform.bulkscanprocessor.model.mapper.zipfilestatus;

import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.zipfilestatus.ZipFileEnvelope;

public final class ZipFileEnvelopeMapper {

    private ZipFileEnvelopeMapper() {
        // util class
    }

    public static ZipFileEnvelope fromEnvelope(Envelope envelope) {
        if (envelope == null) {
            return null;
        } else {
            return new ZipFileEnvelope(
                envelope.getId().toString(),
                envelope.getContainer(),
                envelope.getStatus().name()
            );
        }
    }
}
