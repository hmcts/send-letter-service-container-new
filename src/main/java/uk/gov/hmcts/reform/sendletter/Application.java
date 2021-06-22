package uk.gov.hmcts.reform.sendletter;

import com.microsoft.applicationinsights.TelemetryClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import uk.gov.hmcts.reform.sendletter.blob.BlobProcessor;

@SuppressWarnings("HideUtilityClassConstructor")
@SpringBootApplication
public class Application implements CommandLineRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    @Autowired
    private BlobProcessor retrieve;
    @Autowired
    private TelemetryClient telemetryClient;

    public static void main(final String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(String... args) {
        try {
            telemetryClient.trackEvent("send letter new KEDA container invoked");
            LOGGER.info("send letter new KEDA container invoked");
            retrieve.read();
            LOGGER.info("send letter new KEDA container finished");
        } catch (Exception e) {
            LOGGER.info("Exception occured while KEDA container invoked", e);
        } finally {
            // Initiate flush and give it some time to finish.
            telemetryClient.flush();
            try {
                Thread.sleep(10000);
            } catch (InterruptedException ie) {
                LOGGER.error("Exception in thread sleep", ie);
                Thread.currentThread().interrupt();
            }
        }
    }
}
