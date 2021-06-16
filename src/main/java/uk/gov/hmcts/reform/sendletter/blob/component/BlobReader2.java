package uk.gov.hmcts.reform.sendletter.blob.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sendletter.blob.storage.LeaseClientProvider;
import uk.gov.hmcts.reform.sendletter.config.AccessTokenProperties;
import uk.gov.hmcts.reform.sendletter.model.in.BlobInfo;

import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class BlobReader2 {

    private static final Logger LOG = LoggerFactory.getLogger(BlobReader2.class);

    private final BlobManager blobManager;
    private final AccessTokenProperties accessTokenProperties;
    private final LeaseClientProvider leaseClientProvider;
    private final int leaseTime;

    public BlobReader2(BlobManager blobManager,
            AccessTokenProperties accessTokenProperties,
            LeaseClientProvider leaseClientProvider,
            @Value("${storage.leaseTime}") int leaseTime) {
        this.blobManager =  blobManager;
        this.accessTokenProperties = accessTokenProperties;
        this.leaseClientProvider = leaseClientProvider;
        this.leaseTime = leaseTime;
    }

    public Optional<BlobInfo> retrieveBlobToProcess() {
        LOG.info("About to read manifests details from new container");
        var containers = accessTokenProperties.getServiceConfig().stream().
                filter(t -> t.getContainerName().startsWith("new-")).
                collect(Collectors.toList());
        Collections.shuffle(containers);
        var counter = 0;

        Optional<BlobInfo> manifest = Optional.empty();
        while (manifest.isEmpty() && counter < containers.size()) {
            var tokenConfig = containers.get(counter++);
            var containerName = tokenConfig.getContainerName();
            var serviceName = tokenConfig.getServiceName();
            var containerClient = blobManager.getContainerClient(containerName);

            manifest = containerClient.listBlobs().stream()
                    .filter(blobItem -> blobItem.getName().startsWith("manifest"))
                    .map(blobItem ->
                            new BlobInfo(
                                    containerClient.getBlobClient(blobItem.getName())
                            )
                    )
                    .filter(blobInfo -> {
                        this.acquireLease(blobInfo, containerName, serviceName);
                        return blobInfo.isLeased();
                    })
                    .findFirst();
        }
        return manifest;
    }

    private void acquireLease(BlobInfo blobInfo, String containerName, String serviceName) {
        try {
            var blobLeaseClient = leaseClientProvider.get(blobInfo.getBlobClient());
            var leaseId = blobLeaseClient.acquireLease(leaseTime);
            blobInfo.setLeaseId(leaseId);
            blobInfo.setContainerName(containerName);
            blobInfo.setServiceName(serviceName);
        } catch (Exception e) {
            LOG.error("Unable to acquire lease for blob {}",
                    blobInfo.getBlobClient().getBlobName(),
                    e);
        }
    }
}
