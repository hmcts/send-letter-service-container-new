package uk.gov.hmcts.reform.sendletter.model.out;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PrintUploadInfo {
    @JsonProperty("upload_to_container")
    public final String uploadToContainer;

    @JsonProperty("sas")
    public final String sasToken;

    @JsonProperty("manifest_path")
    public final String manifestPath;

    private PrintUploadInfo() {
        uploadToContainer = null;
        sasToken = null;
        manifestPath = null;
    }

    public PrintUploadInfo(String uploadToContainer,
                           String sasToken,
                           String manifestPath) {
        this.uploadToContainer = uploadToContainer;
        this.sasToken = sasToken;
        this.manifestPath = manifestPath;
    }
}
