package uk.gov.hmcts.reform.sendletter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("HideUtilityClassConstructor") // Spring needs a constructor, its not a utility class
public class Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    public static void main(final String[] args) {
        LOGGER.info("send letter new KEDA container invoked");
    }
}
