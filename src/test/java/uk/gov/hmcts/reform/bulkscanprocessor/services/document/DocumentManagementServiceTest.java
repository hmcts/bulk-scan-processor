package uk.gov.hmcts.reform.bulkscanprocessor.services.document;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.UnableToUploadDocumentException;
import uk.gov.hmcts.reform.bulkscanprocessor.services.idam.cache.CachedIdamCredential;
import uk.gov.hmcts.reform.bulkscanprocessor.services.idam.cache.IdamCachedClient;
import uk.gov.hmcts.reform.ccd.document.am.feign.CaseDocumentClientApi;
import uk.gov.hmcts.reform.ccd.document.am.model.Document;
import uk.gov.hmcts.reform.ccd.document.am.model.DocumentUploadRequest;
import uk.gov.hmcts.reform.ccd.document.am.model.UploadResponse;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.google.common.io.Resources.getResource;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentManagementServiceTest {

    private static final String AUTH_HEADER = "service-auth-header";

    private DocumentManagementService documentManagementService;

    private ArgumentCaptor<DocumentUploadRequest> documentUploadRequestCaptor;

    @Mock
    private AuthTokenGenerator authTokenGenerator;

    @Mock
    private CaseDocumentClientApi caseDocumentClientApi;
    @Mock
    private IdamCachedClient idamClient;

    private static final String hashToken = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        when(authTokenGenerator.generate()).thenReturn(AUTH_HEADER);

        this.documentUploadRequestCaptor = ArgumentCaptor.forClass(DocumentUploadRequest.class);

        documentManagementService = new DocumentManagementService(
            authTokenGenerator,
            caseDocumentClientApi,
            idamClient
        );
    }

    @Test
    void should_return_upload_response_with_document_urls_when_docs_are_successfully_uploaded()
        throws Exception {
        //Given
        File pdf1 = new File(getResource("test1.pdf").toURI());
        File pdf2 = new File(getResource("test2.pdf").toURI());
        var s2sToken =  "233132";
        var idamToken =  "12321";
        CachedIdamCredential cachedIdamCredential =
            new CachedIdamCredential(idamToken, "user_id_1", 123213);
        given(authTokenGenerator.generate()).willReturn(s2sToken);
        given(idamClient.getIdamCredentials("BULKSCAN")).willReturn(cachedIdamCredential);
        UUID docStoreUuid1 = UUID.randomUUID();
        UUID docStoreUuid2 = UUID.randomUUID();


        given(caseDocumentClientApi.uploadDocuments(
            eq(idamToken),
            eq(s2sToken),
            documentUploadRequestCaptor.capture()
        )).willReturn(getResponse(docStoreUuid1, docStoreUuid2));

        //when
        Map<String, String> actualUploadResponse
            = documentManagementService.uploadDocuments(asList(pdf1, pdf2));

        //then

        assertThat(actualUploadResponse).containsValues(
            "http://localhost:samplefile/" + docStoreUuid1,
            "http://localhost:samplefile/" + docStoreUuid2
        );
        assertThat(actualUploadResponse).containsKeys("template1.pdf", "template2.pdf");
        var docReq = documentUploadRequestCaptor.getValue();
        verify(authTokenGenerator).generate();
        assertThat(docReq.getCaseTypeId()).isEqualTo("Bulk_Scanned");
        assertThat(docReq.getClassification()).isEqualTo("PUBLIC");
        assertThat(docReq.getJurisdictionId()).isEqualTo("BULKSCAN");

    }

    @Test
    void should_throw_client_exception_when_service_auth_throws_unauthorized_exception() throws Exception {
        //Given
        File pdf1 = new File(getResource("test1.pdf").toURI());
        File pdf2 = new File(getResource("test2.pdf").toURI());

        given(authTokenGenerator.generate()).willThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED));

        //when
        Throwable exc = catchThrowable(() -> documentManagementService.uploadDocuments(asList(pdf1, pdf2)));

        //then
        assertThat(exc)
            .isInstanceOf(HttpClientErrorException.class);

        verify(authTokenGenerator).generate();
    }

    @Test
    void should_throw_unable_to_upload_document_exception_when_document_storage_is_down()
        throws Exception {
        //Given
        File pdf1 = new File(getResource("test1.pdf").toURI());
        File pdf2 = new File(getResource("test2.pdf").toURI());

        var s2sToken =  "233132";
        given(authTokenGenerator.generate()).willReturn(s2sToken);
        var idamToken =  "12321";
        CachedIdamCredential cachedIdamCredential =
            new CachedIdamCredential(idamToken, "user_id_1", 123213);
        given(idamClient.getIdamCredentials("BULKSCAN")).willReturn(cachedIdamCredential);

        given(caseDocumentClientApi.uploadDocuments(
            eq(idamToken),
            eq(s2sToken),
            any())
        ).willThrow(new HttpClientErrorException(HttpStatus.FORBIDDEN));

        //when
        Throwable exc = catchThrowable(() -> documentManagementService.uploadDocuments(asList(pdf1, pdf2)));

        //then
        assertThat(exc)
            .isInstanceOf(UnableToUploadDocumentException.class)
            .hasCauseExactlyInstanceOf(HttpClientErrorException.class);

        verify(authTokenGenerator).generate();
        verify(idamClient).getIdamCredentials("BULKSCAN");
    }


    private UploadResponse getResponse(UUID docStoreUuid1, UUID docStoreUuid2) {
        Document testDoc1 = Document.builder().originalDocumentName("template1.pdf")
            .hashToken("token")
            .links(getLinks(docStoreUuid1))
            .build();

        Document testDoc2 = Document.builder().originalDocumentName("template2.pdf")
            .hashToken("token")
            .links(getLinks(docStoreUuid2))
            .build();
        return new UploadResponse(List.of(testDoc1, testDoc2));
    }

    static Document.Links getLinks(UUID docStoreUuid) {
        Document.Links links = new Document.Links();

        Document.Link self = new Document.Link();
        Document.Link binary = new Document.Link();

        var selfLink = "http://localhost:samplefile/" + docStoreUuid;
        var binaryLink = "http://localhost:samplefile/" + docStoreUuid + "/binary";

        self.href = selfLink;
        binary.href = binaryLink;

        links.self = self;
        links.binary = binary;

        return links;
    }
}
