package uk.gov.hmcts.reform.bulkscanprocessor.model.mapper.zipfilestatus;

import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.zipfilestatus.ZipFileEvent;

public final class ZipFileEventMapper {

    private ZipFileEventMapper() {
        // util class
    }

    public static ZipFileEvent fromEvent(ProcessEvent event) {
        if (event == null) {
            return null;
        } else {
            return new ZipFileEvent(
                event.getEvent().name(),
                event.getContainer(),
                event.getCreatedAt()
            );
        }
    }
}
