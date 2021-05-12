package uk.gov.hmcts.reform.sendletter.blob.component;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sendletter.config.AccessTokenProperties;
import uk.gov.hmcts.reform.sendletter.model.in.ManifestBlobInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

@Component
public class BlobReader {

    private static final Logger LOG = LoggerFactory.getLogger(BlobReader.class);

    private final BlobManager blobManager;
    private final AccessTokenProperties accessTokenProperties;

    public BlobReader(BlobManager blobManager, AccessTokenProperties accessTokenProperties) {
        this.blobManager =  blobManager;
        this.accessTokenProperties = accessTokenProperties;
    }

    public Optional<ManifestBlobInfo> retrieveManifestsToProcess() {
        LOG.info("About to read manifests details");
        var containers = new ArrayList<>(accessTokenProperties.getServiceConfig());
        Collections.shuffle(containers);

        for (AccessTokenProperties.TokenConfig config : containers) {
            String serviceName = config.getServiceName();
            String containerName = config.getNewContainerName();

            BlobContainerClient containerClient = blobManager.getContainerClient(containerName);
            Optional<String> blobName  = containerClient.listBlobs().stream().filter(obj -> obj.getName().startsWith(
                    "manifest")).findFirst().map(BlobItem::getName);


            if (blobName.isPresent()) {
                LOG.info("BlobReader:: ServiceName {}, Container {}, Blob name: {}", serviceName,
                        containerName, blobName.get());
                return Optional.of(new ManifestBlobInfo(serviceName, containerName, getCleanName(blobName.get())));
            }
        }

        return Optional.empty();
    }

    private String getCleanName(String filename) {
        if (filename.contains("/")) {
            return filename.substring(filename.lastIndexOf("/") + 1);
        }
        return filename;
    }
}
