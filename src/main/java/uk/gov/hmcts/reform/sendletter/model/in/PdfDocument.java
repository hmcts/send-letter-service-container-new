package uk.gov.hmcts.reform.sendletter.model.in;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.Map;
import javax.validation.constraints.NotEmpty;


public class PdfDocument implements Serializable {

    private static final long serialVersionUID = -2374030102074056382L;

    @ApiModelProperty(value = "Template to be used to render PDF", required = true)
    @NotEmpty
    public final String template;

    @ApiModelProperty(value = "Template values for the PDF", required = true)
    @NotEmpty
    public final Map<String, Object> values;

    public PdfDocument(
        @JsonProperty("template") String template,
        @JsonProperty("values") Map<String, Object> values
    ) {
        this.template = template;
        this.values = values;
    }
}
