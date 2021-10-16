package uk.gov.hmcts.reform.bulkscanprocessor.services.document;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.UnableToUploadDocumentException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

import static com.google.common.io.Resources.getResource;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DocumentManagementServiceTest {

    private static final String AUTH_HEADER = "service-auth-header";

    private DocumentManagementService documentManagementService;

    private ArgumentCaptor<HttpEntity> httpEntityReqEntity;

    @Mock
    private DocumentServiceHelper documentServiceHelper;

    @Mock
    private RestTemplate restTemplate;

    private DocumentUploadCredential documentUploadCredential = new DocumentUploadCredential(
        "s2s-21321231",
        "idam-9943",
        "BULKSCAN_Exception"
    );

    @BeforeEach
    void setUp() {

        this.httpEntityReqEntity = ArgumentCaptor.forClass(HttpEntity.class);

        documentManagementService = new DocumentManagementService(
            documentServiceHelper,
            "http://localhost:8080",
            restTemplate,
            new ObjectMapper().enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        );
    }

    @Test
    void should_return_upload_response_with_document_urls_when_docs_are_successfully_uploaded()
        throws Exception {
        //Given
        File pdf1 = new File(getResource("test1.pdf").toURI());
        File pdf2 = new File(getResource("test2.pdf").toURI());

        given(documentServiceHelper.createDocumentUploadCredential("BULKSCAN","bulkscan"))
            .willReturn(documentUploadCredential);

        given(restTemplate.postForObject(
            eq("http://localhost:8080/cases/documents"),
            httpEntityReqEntity.capture(),
            any())
        ).willReturn(getResponse());

        //when
        Map<String, String> actualUploadResponse =
            documentManagementService.uploadDocuments(asList(pdf1, pdf2));

        //then
        assertThat(actualUploadResponse).containsValues(
            "http://localhost:8080/documents/1971cadc-9f79-4e1d-9033-84543bbbbc1d",
            "http://dm-store:8080/documents/46f068e9-a395-49b3-a819-18e8c1327f11"
        );

        assertThat(actualUploadResponse).containsKeys("test1.pdf", "test2.pdf");

        verify(documentServiceHelper).createDocumentUploadCredential(anyString(), anyString());
        verify(restTemplate).postForObject(
            eq("http://localhost:8080/cases/documents"),
            httpEntityReqEntity.capture(),
            any()
        );
    }

    @Test
    void should_throw_client_exception_when_service_auth_throws_unauthorized_exception() throws Exception {
        //Given
        File pdf1 = new File(getResource("test1.pdf").toURI());
        File pdf2 = new File(getResource("test2.pdf").toURI());

        given(documentServiceHelper.createDocumentUploadCredential(anyString(), anyString()))
            .willThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED));

        //when
        Throwable exc = catchThrowable(() -> documentManagementService.uploadDocuments(asList(pdf1, pdf2)));

        //then
        assertThat(exc)
            .isInstanceOf(HttpClientErrorException.class);

        verify(documentServiceHelper).createDocumentUploadCredential(anyString(), anyString());
    }

    @Test
    void should_throw_unable_to_upload_doc_exception_when_bulk_scan_service_throws_client_exception()
        throws Exception {
        //Given
        File pdf1 = new File(getResource("test1.pdf").toURI());
        File pdf2 = new File(getResource("test2.pdf").toURI());
        given(documentServiceHelper.createDocumentUploadCredential("BULKSCAN","bulkscan"))
            .willReturn(documentUploadCredential);

        given(restTemplate.postForObject(
            eq("http://localhost:8080/cases/documents"),
            httpEntityReqEntity.capture(),
            any())
        ).willThrow(new HttpClientErrorException(HttpStatus.FORBIDDEN));

        //when
        Throwable exc = catchThrowable(() -> documentManagementService.uploadDocuments(asList(pdf1, pdf2)));

        //then
        assertThat(exc)
            .isInstanceOf(UnableToUploadDocumentException.class)
            .hasCauseExactlyInstanceOf(HttpClientErrorException.class);

        verify(documentServiceHelper).createDocumentUploadCredential(anyString(), anyString());
    }

    @Test
    void should_throw_unable_to_upload_document_exception_when_document_storage_is_down() throws Exception {
        //Given
        File pdf1 = new File(getResource("test1.pdf").toURI());
        File pdf2 = new File(getResource("test2.pdf").toURI());
        given(documentServiceHelper.createDocumentUploadCredential("BULKSCAN","bulkscan"))
            .willReturn(documentUploadCredential);

        given(restTemplate.postForObject(
            eq("http://localhost:8080/cases/documents"),
            httpEntityReqEntity.capture(),
            any())
        ).willThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

        //when
        Throwable exc = catchThrowable(() -> documentManagementService.uploadDocuments(asList(pdf1, pdf2)));

        //then
        assertThat(exc)
            .isInstanceOf(UnableToUploadDocumentException.class)
            .hasCauseExactlyInstanceOf(HttpServerErrorException.class);

        verify(documentServiceHelper).createDocumentUploadCredential(anyString(), anyString());
    }

    private String getResponse() throws IOException {
        return Resources.toString(getResource("fileuploadresponse.json"), Charset.defaultCharset());
    }
}
