package uk.gov.hmcts.reform.sendletter.model.in;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ManifestBlobInfoTest {

    @Test
    void should_serialize_when_manifest_created_for_parsing()  {
        var serviceName = "test_service";
        var containerName = "testContainer";
        var blobName = "testBlob";

        ManifestBlobInfo blobInfo = new ManifestBlobInfo(serviceName, containerName, blobName);

        assertThat(blobInfo.getBlobName()).isEqualTo(blobName);
        assertThat(blobInfo.getContainerName()).isEqualTo(containerName);
        assertThat(blobInfo.getServiceName()).isEqualTo(serviceName);

    }
}