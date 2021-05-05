package uk.gov.hmcts.reform.sendletter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties("accesstoken")
public class AccessTokenProperties {
    private List<TokenConfig> serviceConfig;

    public List<TokenConfig> getServiceConfig() {
        return serviceConfig;
    }

    public void setServiceConfig(List<TokenConfig> serviceConfig) {
        this.serviceConfig = serviceConfig;
    }

    public static class TokenConfig {
        private String containerName;
        private int validity;

        public String getContainerName() {
            return containerName;
        }

        public void setContainerName(String containerName) {
            this.containerName = containerName;
        }

        public int getValidity() {
            return validity;
        }

        public void setValidity(int validity) {
            this.validity = validity;
        }
    }
}
