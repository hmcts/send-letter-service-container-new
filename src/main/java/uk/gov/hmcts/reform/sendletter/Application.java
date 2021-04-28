package uk.gov.hmcts.reform.sendletter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import uk.gov.hmcts.reform.sendletter.blob.BlobProcessor;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("HideUtilityClassConstructor")
@SpringBootApplication
public class Application implements CommandLineRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    @Autowired
    private BlobProcessor retrieve;

    public static void main(final String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(String... args) {
        try {
            LOGGER.info("send letter new KEDA container invoked {}",
                    Optional.ofNullable(args).map(List::of));
            retrieve.read();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
