package uk.gov.hmcts.reform.sendletter.blob.component;

import com.azure.storage.blob.models.BlobItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sendletter.config.AccessTokenProperties;
import uk.gov.hmcts.reform.sendletter.model.in.ManifestBlobInfo;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

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

    public Optional<ManifestBlobInfo> retrieveManifestsToProcess() {
        LOG.info("About to read manifests details");
        var containers = new ArrayList<>(accessTokenProperties.getServiceConfig());
        Collections.shuffle(containers);

        for (AccessTokenProperties.TokenConfig config : containers) {
            String serviceName = config.getServiceName();
            String containerName = config.getNewContainerName();

            var containerClient = blobManager.getContainerClient(containerName);

            var manifestFiles = containerClient.listBlobs().stream()
                    .map(BlobItem::getName)
                    .filter(fileName -> fileName.startsWith("manifest"))
                    .collect(toList());

            if (!manifestFiles.isEmpty()) {
                int index = getRandomIndex(0, manifestFiles.size());
                LOG.info("BlobReader:: ServiceName {}, Container {}, Blob name: {}, index: {}", serviceName,
                        containerName, manifestFiles.get(index), index);
                return Optional.of(new ManifestBlobInfo(serviceName, containerName, manifestFiles.get(index)));
            }
        }

        return Optional.empty();
    }

    private int getRandomIndex(int min, int max) {
        SecureRandom random = new SecureRandom();
        return random.ints(min, max)
                .findFirst()
                .orElse(0);
    }
}
