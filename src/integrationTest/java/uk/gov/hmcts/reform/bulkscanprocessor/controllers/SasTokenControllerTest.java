package uk.gov.hmcts.reform.bulkscanprocessor.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.core.PathUtility;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.reform.bulkscanprocessor.config.IntegrationTest;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@IntegrationTest
@RunWith(SpringRunner.class)
public class SasTokenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void should_return_sas_token_when_requested_service_is_configured() throws Exception {
        assertCanRetrieveSasTokenForService("divorce");
        assertCanRetrieveSasTokenForService("probate");
        assertCanRetrieveSasTokenForService("sscs");
    }

    @Test
    public void should_throw_exception_when_requested_service_is_not_configured() throws Exception {
        MvcResult result = this.mockMvc.perform(get("/token/nonexistingservice")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(result.getResolvedException().getMessage())
            .isEqualTo("No service configuration found for service nonexistingservice");
    }

    private void assertCanRetrieveSasTokenForService(String serviceName) throws Exception {
        String tokenResponse = this.mockMvc.perform(get("/token/" + serviceName))
            .andDo(print())
            .andExpect(status().isOk())
            .andReturn()
            .getResponse().getContentAsString();

        verifySasTokenProperties(tokenResponse);
    }

    private void verifySasTokenProperties(String tokenResponse) throws java.io.IOException, StorageException {
        final ObjectNode node = new ObjectMapper().readValue(tokenResponse, ObjectNode.class);

        Map<String, String[]> queryParams = PathUtility.parseQueryString(node.get("sas_token").asText());

        String currentDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

        assertThat(queryParams.get("sig")).isNotNull();//this is a generated hash of the resource string
        assertThat(queryParams.get("se")[0]).startsWith(currentDate);//the expiry date/time for the signature
        assertThat(queryParams.get("sv")).contains("2018-11-09");//azure api version is latest
        assertThat(queryParams.get("sp")).contains("wl");//access permissions(write-w,list-l)
    }
}
