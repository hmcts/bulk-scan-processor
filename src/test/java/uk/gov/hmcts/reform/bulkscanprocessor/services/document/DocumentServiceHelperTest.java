package uk.gov.hmcts.reform.bulkscanprocessor.services.document;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.bulkscanprocessor.services.idam.cache.CachedIdamCredential;
import uk.gov.hmcts.reform.bulkscanprocessor.services.idam.cache.IdamCachedClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class DocumentServiceHelperTest {

    @Mock
    private AuthTokenGenerator authTokenGenerator;

    @Mock
    private IdamCachedClient idamCachedClient;

    @InjectMocks
    private DocumentServiceHelper documentServiceHelper;

    private static final String JURISDICTION = "divorce";

    @Test
    void getDocumentUploadCredential() {

        given(authTokenGenerator.generate()).willReturn("1233");
        var idamCredential = new CachedIdamCredential("QWE-123", "User-id", 132131232);
        given(idamCachedClient.getIdamCredentials(JURISDICTION)).willReturn(idamCredential);
        DocumentUploadCredential cr
            = documentServiceHelper.createDocumentUploadCredential(JURISDICTION, "finrem");
        assertThat(cr.caseTypeId).isEqualTo("FINREM_ExceptionRecord");
        assertThat(cr.idamAccessToken).isEqualTo("QWE-123");
        assertThat(cr.s2sToken).isEqualTo("1233");
    }
}
