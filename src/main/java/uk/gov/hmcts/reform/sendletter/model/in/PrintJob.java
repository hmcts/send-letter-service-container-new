package uk.gov.hmcts.reform.sendletter.model.in;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import javax.validation.Valid;

public class PrintJob {
    public final UUID id;

    @JsonProperty("created_at")
    public final ZonedDateTime createdAt;

    @JsonProperty("printed_at")
    public final ZonedDateTime printedAt;

    @JsonProperty("sent_to_print_at")
    public final ZonedDateTime sentToPrintAt;

    public final String service;

    public final String type;

    @JsonProperty("container_name")
    public final String containerName;

    @JsonProperty("status")
    public final PrintStatus printStatus;

    public final List<@Valid Document> documents;

    @JsonProperty("case_id")
    public final String caseId;

    @JsonProperty("case_ref")
    public final String caseRef;

    @JsonProperty("letter_type")
    public final String letterType;


    private PrintJob() {
        id = null;
        createdAt = null;
        printedAt = null;
        sentToPrintAt = null;
        service = null;
        type = null;
        containerName = null;
        printStatus = null;
        documents = null;
        caseId = null;
        caseRef = null;
        letterType = null;
    }

    @SuppressWarnings("squid:S00107")
    public PrintJob(UUID id,
                    ZonedDateTime createdAt,
                    ZonedDateTime printedAt,
                    ZonedDateTime sentToPrintAt,
                    String service,
                    String type,
                    String containerName, PrintStatus printStatus,
                    List<@Valid Document> documents,
                    String caseId,
                    String caseRef,
                    String letterType) {
        this.id = id;
        this.createdAt = createdAt;
        this.printedAt = printedAt;
        this.sentToPrintAt = sentToPrintAt;
        this.service = service;
        this.type = type;
        this.containerName = containerName;
        this.printStatus = printStatus;
        this.documents = documents;
        this.caseId = caseId;
        this.caseRef = caseRef;
        this.letterType = letterType;
    }
}
