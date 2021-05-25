package uk.gov.hmcts.reform.sendletter.blob.component;

import com.azure.storage.blob.models.BlobItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sendletter.config.AccessTokenProperties;
import uk.gov.hmcts.reform.sendletter.model.in.ManifestBlobInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Component
public class BlobReader {

    private static final Logger LOG = LoggerFactory.getLogger(BlobReader.class);

    private final BlobManager blobManager;
    private final AccessTokenProperties accessTokenProperties;

    public BlobReader(BlobManager blobManager, AccessTokenProperties accessTokenProperties) {
        this.blobManager =  blobManager;
        this.accessTokenProperties = accessTokenProperties;
    }

    public List<ManifestBlobInfo> retrieveManifestsToProcess() {
        LOG.info("About to read manifests details");
        var containers = new ArrayList<>(accessTokenProperties.getServiceConfig());
        Collections.shuffle(containers);

        for (AccessTokenProperties.TokenConfig config : containers) {
            String serviceName = config.getServiceName();
            String containerName = config.getNewContainerName();

            var containerClient = blobManager.getContainerClient(containerName);

            return containerClient.listBlobs().stream()
                    .map(BlobItem::getName)
                    .filter(fileName -> fileName.startsWith("manifest"))
                    .map(fileName ->
                            new ManifestBlobInfo(
                                    serviceName,
                                    containerName,
                                    fileName)
                    )
                    .collect(toList());
        }
        return Collections.emptyList();
    }
}
