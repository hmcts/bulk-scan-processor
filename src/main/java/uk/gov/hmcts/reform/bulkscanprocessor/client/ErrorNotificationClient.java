package uk.gov.hmcts.reform.bulkscanprocessor.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import uk.gov.hmcts.reform.bulkscanprocessor.model.in.ErrorNotificationResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.errors.ErrorNotificationRequest;

import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;

@FeignClient(
    name = "error-notifications",
    url = "${error_notifications.url}",
    configuration = ErrorNotificationConfiguration.class
)
public interface ErrorNotificationClient {

    @PostMapping(value = "/notifications",
        consumes = APPLICATION_JSON_VALUE,
        produces = APPLICATION_JSON_VALUE
    )
    ErrorNotificationResponse notify(@RequestBody ErrorNotificationRequest notification);
}
