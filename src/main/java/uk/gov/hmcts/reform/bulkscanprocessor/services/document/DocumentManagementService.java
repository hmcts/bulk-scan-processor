package uk.gov.hmcts.reform.bulkscanprocessor.services.document;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.bulkscanprocessor.exceptions.UnableToUploadDocumentException;
import uk.gov.hmcts.reform.ccd.document.am.feign.CaseDocumentClientApi;
import uk.gov.hmcts.reform.ccd.document.am.model.Document;
import uk.gov.hmcts.reform.ccd.document.am.model.DocumentUploadRequest;
import uk.gov.hmcts.reform.ccd.document.am.model.UploadResponse;

import java.io.File;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

@Service
public class DocumentManagementService {

    private static final Logger log = LoggerFactory.getLogger(DocumentManagementService.class);

    private final AuthTokenGenerator authTokenGenerator;
    private CaseDocumentClientApi caseDocumentClientApi;

    private static final String CLASSIFICATION = "classification";
    private static final String CASE_TYPE_ID = "caseTypeId";
    private static final String JURISDICTION_ID = "jurisdictionId";

    private static final String FILES = "files";
    private static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";


    public DocumentManagementService(
        AuthTokenGenerator authTokenGenerator,
        CaseDocumentClientApi caseDocumentClientApi
    ) {
        this.authTokenGenerator = authTokenGenerator;
        this.caseDocumentClientApi = caseDocumentClientApi;
    }

    public Map<String, String> uploadDocuments(List<File> pdfs) {

        String s2sToken = authTokenGenerator.generate();
        try {
            UploadResponse upload = uploadDocs(
                null,
                s2sToken,
                pdfs
            );

            List<Document> documents = upload.getDocuments();
            log.debug("File upload response from Document Storage service is {}", documents);

            return createFileUploadResponse(documents);

        } catch (Exception exception) {
            log.error("Exception occurred while uploading documents ", exception);
            throw new UnableToUploadDocumentException(exception.getMessage(), exception);
        }
    }

    private Map.Entry<String, String> createResponse(Document document) {
        return new AbstractMap.SimpleEntry<>(
            document.originalDocumentName,
            document.links.self.href
        );
    }

    private Map<String, String> createFileUploadResponse(List<Document> documents) {
        return documents.stream()
            .map(this::createResponse)
            .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private UploadResponse uploadDocs(
        String authorisation,
        String serviceAuth,
        List<File> pdfs
    ) {

        var multipartFileList = buildMultipartFileList(pdfs);

        DocumentUploadRequest documentUploadRequest = new DocumentUploadRequest(
            uk.gov.hmcts.reform.ccd.document.am.model.Classification.RESTRICTED.toString(),
            CASE_TYPE_ID,
            JURISDICTION_ID,
            multipartFileList
        );

        UploadResponse uploadResponse =
            caseDocumentClientApi.uploadDocuments(
                authorisation,
                serviceAuth,
                documentUploadRequest
            );
        ;

        return uploadResponse;

    }


    private List<MultipartFile> buildMultipartFileList(List<File> pdfs) {

        List<MultipartFile> multipartFileList = new ArrayList<MultipartFile>();
        for (File pdf : pdfs) {
            try {
                multipartFileList.add(
                    ByteArrayMultipartFile.builder()
                        .content(FileUtils.readFileToByteArray(pdf))
                        .name(pdf.getName())
                        .contentType(MediaType.valueOf("application/pdf"))
                        .build()
                );
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return multipartFileList;
    }
}
