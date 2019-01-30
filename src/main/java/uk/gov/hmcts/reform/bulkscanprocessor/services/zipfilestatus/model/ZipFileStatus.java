package uk.gov.hmcts.reform.bulkscanprocessor.services.zipfilestatus.model;

import uk.gov.hmcts.reform.bulkscanprocessor.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;

import java.util.List;

public class ZipFileStatus {

    public final List<Envelope> envelopes;
    public final List<ProcessEvent> events;

    public ZipFileStatus(List<Envelope> envelopes, List<ProcessEvent> events) {
        this.envelopes = envelopes;
        this.events = events;
    }
}
