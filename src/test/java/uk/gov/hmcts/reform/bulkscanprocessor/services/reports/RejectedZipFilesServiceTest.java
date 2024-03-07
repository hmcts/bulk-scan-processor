package uk.gov.hmcts.reform.bulkscanprocessor.services.reports;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.RejectedZipFile;
import uk.gov.hmcts.reform.bulkscanprocessor.entity.reports.RejectedZipFileRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RejectedZipFilesServiceTest {

    @Mock
    private RejectedZipFileRepository rejectedZipFileRepository;

    private RejectedZipFilesService rejectedZipFilesService;

    @BeforeEach
    void setUp() {
        rejectedZipFilesService = new RejectedZipFilesService(rejectedZipFileRepository);
    }

    @Test
    void should_return_rejected_files() {
        // given
        LocalDate date = LocalDate.now();
        List<RejectedZipFile> rejectedFiles = asList(
                new RejectedZipFileItem(
                        "test1.zip",
                        "c1",
                        Instant.now(),
                        null,
                        "FILE_VALIDATION_FAILURE"
                ),
                new RejectedZipFileItem(
                        "test2.zip",
                        "c2",
                        Instant.now(),
                        null,
                        "FILE_VALIDATION_FAILURE"
                )
        );
        given(rejectedZipFileRepository.getRejectedZipFilesReportFor(date))
                .willReturn(rejectedFiles);

        // when
        List<RejectedZipFile> res = rejectedZipFilesService.getRejectedZipFiles(date);

        // then
        assertThat(res).isSameAs(rejectedFiles);
    }

    @Test
    void should_rethrow_exception() {
        // given
        LocalDate date = LocalDate.now();
        given(rejectedZipFileRepository.getRejectedZipFilesReportFor(date))
                .willThrow(new EntityNotFoundException("msg"));

        // when
        // then
        assertThatThrownBy(
            () -> rejectedZipFilesService.getRejectedZipFiles(date)
        )
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessage("msg");
    }
}
