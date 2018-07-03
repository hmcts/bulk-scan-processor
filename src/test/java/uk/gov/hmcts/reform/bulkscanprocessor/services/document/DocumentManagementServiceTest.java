package uk.gov.hmcts.reform.bulkscanprocessor.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.io.Resources;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.UnableToUploadDocumentException;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.DocumentManagementService;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.FileUploadResponse;
import uk.gov.hmcts.reform.bulkscanprocessor.services.document.output.Pdf;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.toByteArray;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DocumentManagementServiceTest {

    private static final String AUTH_HEADER = "service-auth-header";

    private DocumentManagementService documentManagementService;

    private ArgumentCaptor<HttpEntity> httpEntityReqEntity;

    @Mock
    private AuthTokenGenerator authTokenGenerator;

    @Mock
    private RestTemplate restTemplate;

    @Before
    public void setUp() {
        when(authTokenGenerator.generate()).thenReturn(AUTH_HEADER);

        this.httpEntityReqEntity = ArgumentCaptor.forClass(HttpEntity.class);

        documentManagementService = new DocumentManagementService(
            authTokenGenerator,
            restTemplate,
            "http://localhost:8080"
        );
    }

    @Test
    public void should_return_upload_response_with_document_urls_when_docs_are_successfully_uploaded()
        throws Exception {
        //Given
        byte[] test1PdfBytes = toByteArray(getResource("test1.pdf"));
        byte[] test2PdfBytes = toByteArray(getResource("test2.pdf"));

        Pdf pdf1 = new Pdf("test1.pdf", test1PdfBytes);
        Pdf pdf2 = new Pdf("test2.pdf", test2PdfBytes);

        when(restTemplate.postForObject(
            eq("http://localhost:8080/documents"),
            httpEntityReqEntity.capture(),
            any()
        )).thenReturn(getResponse());

        //when
        List<FileUploadResponse> actualUploadResponse = documentManagementService.uploadDocuments(asList(pdf1, pdf2));

        //then
        assertThat(actualUploadResponse)
            .extracting("fileUrl")
            .containsExactly(
                "http://localhost:8080/documents/1971cadc-9f79-4e1d-9033-84543bbbbc1d",
                "http://localhost:8080/documents/0fa1ab60-f836-43aa-8c65-b07cc9bebcbe"
            );

        assertThat(actualUploadResponse)
            .extracting("fileName")
            .containsExactly(
                "test1.pdf", "test2.pdf"
            );

        verify(authTokenGenerator).generate();
        verify(restTemplate).postForObject(
            eq("http://localhost:8080/documents"),
            httpEntityReqEntity.capture(),
            any()
        );
    }

    @Test
    public void should_throw_client_exception_when_service_auth_throws_unauthorized_exception() throws Exception {
        //Given
        byte[] test1PdfBytes = toByteArray(getResource("test1.pdf"));
        byte[] test2PdfBytes = toByteArray(getResource("test2.pdf"));

        Pdf pdf1 = new Pdf("test1.pdf", test1PdfBytes);
        Pdf pdf2 = new Pdf("test2.pdf", test2PdfBytes);

        when(authTokenGenerator.generate()).thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED));

        //when
        Throwable exc = catchThrowable(() -> documentManagementService.uploadDocuments(asList(pdf1, pdf2)));

        //then
        assertThat(exc)
            .isInstanceOf(HttpClientErrorException.class);

        verify(authTokenGenerator).generate();
    }

    @Test
    public void should_throw_unable_to_upload_doc_exception_when_bulk_scan_service_throws_client_exception()
        throws Exception {
        //Given
        byte[] test1PdfBytes = toByteArray(getResource("test1.pdf"));
        byte[] test2PdfBytes = toByteArray(getResource("test2.pdf"));

        Pdf pdf1 = new Pdf("test1.pdf", test1PdfBytes);
        Pdf pdf2 = new Pdf("test2.pdf", test2PdfBytes);

        when(restTemplate.postForObject(
            eq("http://localhost:8080/documents"),
            httpEntityReqEntity.capture(), any())
        ).thenThrow(new HttpClientErrorException(HttpStatus.FORBIDDEN));

        //when
        Throwable exc = catchThrowable(() -> documentManagementService.uploadDocuments(asList(pdf1, pdf2)));

        //then
        assertThat(exc)
            .isInstanceOf(UnableToUploadDocumentException.class)
            .hasCauseExactlyInstanceOf(HttpClientErrorException.class);

        verify(authTokenGenerator).generate();
    }

    @Test
    public void should_throw_unable_to_upload_document_exception_when_document_storage_is_down() throws Exception {
        //Given
        byte[] test1PdfBytes = toByteArray(getResource("test1.pdf"));
        byte[] test2PdfBytes = toByteArray(getResource("test2.pdf"));

        Pdf pdf1 = new Pdf("test1.pdf", test1PdfBytes);
        Pdf pdf2 = new Pdf("test2.pdf", test2PdfBytes);

        when(restTemplate.postForObject(
            eq("http://localhost:8080/documents"),
            httpEntityReqEntity.capture(),
            any())
        ).thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

        //when
        Throwable exc = catchThrowable(() -> documentManagementService.uploadDocuments(asList(pdf1, pdf2)));

        //then
        assertThat(exc)
            .isInstanceOf(UnableToUploadDocumentException.class)
            .hasCauseExactlyInstanceOf(HttpServerErrorException.class);

        verify(authTokenGenerator).generate();
    }

    private ObjectNode getResponse() throws IOException {
        String response = Resources.toString(getResource("fileuploadresponse.json"), Charset.defaultCharset());
        return (ObjectNode) new ObjectMapper().readTree(response);
    }
}
