package uk.gov.hmcts.reform.sendletter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.sendletter.exceptions.ServiceConfigNotFoundException;

import java.util.List;

@Configuration
@ConfigurationProperties("accesstoken")
public class AccessTokenProperties {
    private List<TokenConfig> serviceConfig;

    public List<TokenConfig> getServiceConfig() {
        return serviceConfig;
    }

    public void setServiceConfig(List<TokenConfig> serviceConfig) {
        this.serviceConfig = serviceConfig;
    }

    public String getContainerForGivenService(final String service) {
        return getServiceConfig().stream()
                .filter(tokenConfig -> service.equals(tokenConfig.getServiceName()))
                .map(AccessTokenProperties.TokenConfig::getContainerName)
                .findFirst()
                .orElseThrow(() ->
                        new ServiceConfigNotFoundException(
                                "No container found for service " + service));
    }


    public static class TokenConfig {
        private String serviceName;
        private int validity;
        private String containerName;

        public String getServiceName() {
            return serviceName;
        }

        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
        }

        public int getValidity() {
            return validity;
        }

        public void setValidity(int validity) {
            this.validity = validity;
        }

        public String getContainerName() {
            return containerName;
        }

        public void setContainerName(String containerName) {
            this.containerName = containerName;
        }
    }
}
