package uk.gov.hmcts.reform.sendletter.model.in;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import static com.fasterxml.jackson.annotation.JsonInclude.Include;

public class Document {
    @JsonProperty("file_name")
    public final String fileName;

    @JsonProperty("upload_to_path")
    @JsonInclude(Include.NON_NULL)
    public final String uploadToPath;

    @JsonProperty("copies_required")
    public final Integer copies;

    private Document() {
        fileName = null;
        uploadToPath = null;
        copies = null;
    }

    public Document(
            String fileName,
            String uploadToPath,
            Integer copies
    ) {
        this.fileName = fileName;
        this.uploadToPath = uploadToPath;
        this.copies = copies;
    }
}
