package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ScannableItemRepository extends JpaRepository<ScannableItem, UUID> {

    /**
     * Finds envelope with a blob not deleted for a given container and zip file name.
     *
     * @param documentControlNumber of scannable item.
     * @return A list of envelopes.
     */
    @Query("select e.zipFileName from ScannableItem si"
        + " JOIN Envelope e on e.id = si.envelope.id"
        + " where si.documentControlNumber LIKE :documentControlNumber%"
        + " order by si.scanningDate desc"
    )

    List<String> findByDcn(
        @Param("documentControlNumber") String documentControlNumber
    );

}
