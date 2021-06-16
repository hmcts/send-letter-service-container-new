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
        var counter = 0;
        List<ManifestBlobInfo> manifestBlobList = Collections.emptyList();

        while (manifestBlobList.isEmpty()
                && counter < containers.size()) {
            var tokenConfig = containers.get(counter++);
            String serviceName = tokenConfig.getServiceName();
            String containerName = tokenConfig.getContainerName();

            var containerClient = blobManager.getContainerClient(containerName);

            manifestBlobList = containerClient.listBlobs().stream()
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
        return manifestBlobList;
    }
}
