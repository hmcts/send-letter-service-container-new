package uk.gov.hmcts.reform.sendletter.blob.component;

import com.azure.storage.blob.BlobContainerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sendletter.config.AccessTokenProperties;
import uk.gov.hmcts.reform.sendletter.model.in.ManifestBlobInfo;

import java.util.ArrayList;
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
        Optional<ManifestBlobInfo> manifest = Optional.empty();

        for (AccessTokenProperties.TokenConfig config : containers) {

            BlobContainerClient containerClient = blobManager.getContainerClient(config.getNewContainerName());
            manifest = containerClient.listBlobs().stream().filter(obj -> obj.getName().startsWith("manifest"))
                    .findFirst().map(blobItem -> new ManifestBlobInfo(config.getServiceName(),
                            config.getNewContainerName(), blobItem.getName()));


            if (manifest.isPresent())   {
                break;
            }
        }

        return manifest;
    }
}
