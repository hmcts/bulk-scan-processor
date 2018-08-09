package uk.gov.hmcts.reform.bulkscanprocessor.tasks;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.tasks.processor.FailedDocUploadProcessor;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "scheduling.task.reupload.enabled=true"
})
@RunWith(SpringRunner.class)
public class ReuploadFailedEnvelopeTaskTest {

    @Autowired
    private ReuploadFailedEnvelopeTask task;

    @SpyBean
    private FailedDocUploadProcessor processor;

    @Test
    public void should_return_new_instance_of_processor_when_rerequesting_via_lookup_method() {
        // comparing instance hashes. .hashCode() just returns same one
        assertThat(task.getProcessor().toString()).isNotEqualTo(task.getProcessor().toString());
    }
}
