package uk.gov.hmcts.reform.bulkscanprocessor.services;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.bulkscanprocessor.config.EnvelopeAccessProperties;
import uk.gov.hmcts.reform.bulkscanprocessor.config.EnvelopeAccessProperties.Mapping;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.ServiceJuridictionConfigNotFoundException;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("checkstyle:LineLength")
@RunWith(MockitoJUnitRunner.class)
public class EnvelopeAccessServiceTest {

    @Mock
    private EnvelopeAccessProperties accessProps;

    private EnvelopeAccessService service;

    @Before
    public void setUp() throws Exception {
        this.service = new EnvelopeAccessService(accessProps);

        BDDMockito
            .given(accessProps.getMappings())
            .willReturn(asList(
                new Mapping("jur_A", "read_A", "update_A"),
                new Mapping("jur_B", "read_B", "update_B")
            ));
    }

    @Test
    public void getReadJurisdictionForService_should_return_name_of_then_jurisdiction_from_which_service_can_read() {
        String jurisdiction = service.getReadJurisdictionForService("read_A");

        assertThat(jurisdiction).isEqualTo("jur_A");
    }

    @Test
    public void getReadJurisdictionForService_should_throw_an_exception_if_there_is_no_jurisdiction_that_the_service_can_read_from() {
        assertThatThrownBy(() -> service.getReadJurisdictionForService("update_A"))
            .isInstanceOf(ServiceJuridictionConfigNotFoundException.class)
            .hasMessageContaining("update_A");
    }
}
