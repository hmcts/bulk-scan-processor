package uk.gov.hmcts.reform.bulkscanprocessor.model.mapper.zipfilestatus;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.ProcessEvent;
import uk.gov.hmcts.reform.bulkscanprocessor.model.common.Event;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.zipfilestatus.ZipFileEvent;

import static org.assertj.core.api.Assertions.assertThat;

public class ZipFileEventMapperTest {

    @Test
    public void should_map_event_properly() {
        // given
        ProcessEvent input = new ProcessEvent("container", "hello.zip", Event.DOC_PROCESSED);

        // when
        ZipFileEvent output = ZipFileEventMapper.fromEvent(input);

        // then
        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(output.container).as("container").isEqualTo(input.getContainer());
        softly.assertThat(output.createdAt).as("createdAt").isEqualTo(input.getCreatedAt());
        softly.assertThat(output.eventType).as("eventType").isEqualTo(input.getEvent().name());

        softly.assertAll();
    }

    @Test
    public void should_map_null_properly() {
        // given
        ProcessEvent input = null;

        // when
        ZipFileEvent output = ZipFileEventMapper.fromEvent(input);

        // then
        assertThat(output).isNull();
    }
}
