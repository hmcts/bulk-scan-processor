package uk.gov.hmcts.reform.bulkscanprocessor.model.out.msg;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorMsgTest {

    @Test
    void should_serialise_and_deserialise_correctly() throws Exception {
        // given
        ErrorMsg original = new ErrorMsg(
            "id1",
            123L,
            "fileName1",
            "jurisdiction1",
            "poBox1",
            "dcn123",
            ErrorCode.ERR_AV_FAILED,
            "error description 1",
            "service 1",
            "container 1"
        );

        ObjectMapper objectMapper = new ObjectMapper();

        // when
        String serialised = objectMapper.writeValueAsString(original);
        ErrorMsg deserialised = objectMapper.readValue(serialised, ErrorMsg.class);

        // then
        assertThat(deserialised).isEqualToComparingFieldByField(original);
    }
}
