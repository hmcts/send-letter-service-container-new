package uk.gov.hmcts.reform.sendletter.model.in.out;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PrintResponse {
    @JsonProperty("print_job")
    public final PrintJob printJob;

    @JsonProperty("upload")
    public final PrintUploadInfo printUploadInfo;

    private PrintResponse() {
        printJob = null;
        printUploadInfo = null;
    }

    public PrintResponse(PrintJob printJob,
                         PrintUploadInfo printUploadInfo) {
        this.printJob = printJob;
        this.printUploadInfo = printUploadInfo;
    }
}
