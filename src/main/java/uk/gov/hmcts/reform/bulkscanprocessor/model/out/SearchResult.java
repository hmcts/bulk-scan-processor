package uk.gov.hmcts.reform.bulkscanprocessor.model.out;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.util.Assert;

import java.util.List;

public class SearchResult {

    @JsonProperty("count")
    public final int count;

    @JsonProperty("data")
    public final List<?> data;

    public SearchResult(List<?> data) {
        Assert.notNull(data, "'Data' should not be null");
        this.data = data;
        count = data.size();
    }
}
