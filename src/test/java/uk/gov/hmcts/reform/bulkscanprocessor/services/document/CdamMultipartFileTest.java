package uk.gov.hmcts.reform.bulkscanprocessor.services.document;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;

class CdamMultipartFileTest {

    private File file = new File("doc.json");
    private CdamMultipartFile cdamMultipartFile = new CdamMultipartFile(
        file,
        "newName.json",
        MediaType.APPLICATION_PDF
    );

    @Test
    void testGetName() {
        assertThat(cdamMultipartFile.getName()).isEqualTo("newName.json");
    }

    @Test
    void testGetOriginalFilename() {
        assertThat(cdamMultipartFile.getOriginalFilename()).isEqualTo("doc.json");
    }

    @Test
    void testGetSize() {
        assertThrows(
            UnsupportedOperationException.class,
            () -> cdamMultipartFile.getSize()
        );
    }

    @Test
    void testTransferToe() {
        assertThrows(
            UnsupportedOperationException.class,
            () -> cdamMultipartFile.transferTo(new File("d:"))
        );
    }

    @Test
    void testGetResource() {
        assertThat(cdamMultipartFile.getResource()).isNotNull();
    }

    @Test
    void testIsEmpty() {
        assertThrows(
            UnsupportedOperationException.class,
            () -> cdamMultipartFile.isEmpty()
        );
    }

    @Test
    void testGetContent() {
        assertThat(cdamMultipartFile.getContent()).isEqualTo(file);
    }

    @Test
    void testGetContentType() {
        assertThat(cdamMultipartFile.getContentType()).isEqualTo(APPLICATION_PDF_VALUE);
    }

}
