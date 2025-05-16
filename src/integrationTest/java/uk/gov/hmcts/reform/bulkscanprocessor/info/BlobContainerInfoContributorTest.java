package uk.gov.hmcts.reform.bulkscanprocessor.info;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobContainerItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.bulkscanprocessor.config.IntegrationTest;

import java.util.stream.Stream;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@AutoConfigureMockMvc
@IntegrationTest
public class BlobContainerInfoContributorTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BlobServiceClient blobServiceClient;

    @Test
    @SuppressWarnings("unchecked")
    public void should_return_info_about_application() throws Exception {

        PagedIterable pagedIterable = mock(PagedIterable.class);
        given(blobServiceClient.listBlobContainers()).willReturn(pagedIterable);
        given(pagedIterable.stream())
            .willReturn(Stream.of(
                new BlobContainerItem().setName("sscs"),
                new BlobContainerItem().setName("bulkscan"),
                new BlobContainerItem().setName("sscs-rejected")
                )
            );

        this.mockMvc.perform(get("/info"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.containers[0]").value("sscs"))
            .andExpect(jsonPath("$.containers[1]").value("bulkscan"))
            .andExpect(jsonPath("$.containers[2]").value("sscs-rejected"));

    }

}
