package uk.gov.hmcts.reform.bulkscanprocessor.entity;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class EnvelopeJdbcRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public EnvelopeJdbcRepository(
        NamedParameterJdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void markEnvelopeAsDeleted(UUID envelopeId) {
        jdbcTemplate.update(
            "UPDATE envelopes SET zipdeleted = true "
                + "WHERE id=:envelopeId",
            new MapSqlParameterSource()
                .addValue("envelopeId", envelopeId)
        );
    }
}
