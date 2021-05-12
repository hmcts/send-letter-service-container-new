package uk.gov.hmcts.reform.sendletter.model.in;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class PrintJob {
    public final UUID id;

    @JsonProperty("created_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    public final LocalDateTime createdAt;

    @JsonProperty("printed_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    public final LocalDateTime printedAt;

    @JsonProperty("sent_to_print_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    public final LocalDateTime sentToPrintAt;

    public final String service;

    @JsonProperty("status")
    public final PrintStatus printStatus;

    public final List<Document> documents;

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
        printStatus = null;
        documents = null;
        caseId = null;
        caseRef = null;
        letterType = null;
    }

    @SuppressWarnings("squid:S00107")
    public PrintJob(UUID id,
                    LocalDateTime createdAt,
                    LocalDateTime printedAt,
                    LocalDateTime sentToPrintAt,
                    String service,
                    PrintStatus printStatus,
                    List<Document> documents,
                    String caseId,
                    String caseRef,
                    String letterType) {
        this.id = id;
        this.createdAt = createdAt;
        this.printedAt = printedAt;
        this.sentToPrintAt = sentToPrintAt;
        this.service = service;
        this.printStatus = printStatus;
        this.documents = documents;
        this.caseId = caseId;
        this.caseRef = caseRef;
        this.letterType = letterType;
    }
}
