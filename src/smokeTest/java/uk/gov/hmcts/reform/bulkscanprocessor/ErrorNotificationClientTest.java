package uk.gov.hmcts.reform.bulkscanprocessor;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.client.ErrorNotificationClient;
import uk.gov.hmcts.reform.bulkscanprocessor.client.NotificationClientException;
import uk.gov.hmcts.reform.bulkscanprocessor.model.out.errors.ErrorNotificationRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RunWith(SpringRunner.class)
@TestPropertySource("classpath:application.yaml")
@EnableFeignClients(clients = ErrorNotificationClient.class)
@ImportAutoConfiguration({
    FeignAutoConfiguration.class,
    HttpMessageConvertersAutoConfiguration.class
})
public class ErrorNotificationClientTest {

    @Autowired
    private ErrorNotificationClient client;

    /*
    NOTICE.
    These are smoke tests so will NOT include any happy scenarios.
    They will be covered with functional tests once everything is plugged in.
     */

    @Test
    public void should_return_NotificationClientException_with_multiple_missing_required_fields_info() {
        Throwable throwable = catchThrowable(() ->
            System.out.println(client.notify(new ErrorNotificationRequest(
                "test_zip_file_name", null, "test_error_code", null
            )))
        );

        assertThat(throwable).isInstanceOf(NotificationClientException.class);

        NotificationClientException exception = (NotificationClientException) throwable;

        assertThat(exception.getStatus()).isEqualTo(BAD_REQUEST);
        assertThat(exception.getResponse().getMessage()).isEqualTo(
            "po_box is required | error_description is required"
        );
    }

    @Test
    public void should_return_NotificationClientException_with_single_missing_required_field_info() {
        Throwable throwable = catchThrowable(() ->
            System.out.println(client.notify(new ErrorNotificationRequest(
                null, "test_po_box", "test_error_code", "test_error_description"
            )))
        );

        assertThat(throwable).isInstanceOf(NotificationClientException.class);

        NotificationClientException exception = (NotificationClientException) throwable;

        assertThat(exception.getStatus()).isEqualTo(BAD_REQUEST);
        assertThat(exception.getResponse().getMessage()).isEqualTo("zip_file_name is required");
    }

    @Test
    public void should_return_NotificationClientException_with_invalid_error_code_info() {
        Throwable throwable = catchThrowable(() ->
            System.out.println(client.notify(new ErrorNotificationRequest(
                "test_zip_file_name", "test_po_box", "test_error_code", "test_error_description"
            )))
        );

        assertThat(throwable).isInstanceOf(NotificationClientException.class);

        NotificationClientException exception = (NotificationClientException) throwable;

        assertThat(exception.getStatus()).isEqualTo(BAD_REQUEST);
        assertThat(exception.getResponse().getMessage()).isEqualTo("Invalid error_code.");
    }
}
