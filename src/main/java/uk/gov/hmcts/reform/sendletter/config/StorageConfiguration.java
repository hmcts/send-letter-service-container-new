package uk.gov.hmcts.reform.sendletter.config;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class StorageConfiguration {

    @Bean
    public BlobServiceClient getStorageClient(@Value("${storage.connection}") String connection) {
        return new BlobServiceClientBuilder()
                .connectionString(connection)
                .buildClient();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
