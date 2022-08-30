/*
Copyright (c) Microsoft Corporation.
        Licensed under the MIT license.
*/
package org.dspace.storage.bitstore;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Bitstream;
import org.dspace.core.Utils;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import com.azure.storage.blob.specialized.BlockBlobClient;
//import com.azure.storage.common.StorageSharedKeyCredential;

public class AzureBitStoreService implements BitStoreService{

    private static final Logger log = LogManager.getLogger(AzureBitStoreService.class);


    private String containerName = null;
    private String connectionstring;

    private String subfolder;


    private static final ConfigurationService configurationService
            = DSpaceServicesFactory.getInstance().getConfigurationService();
    @Override
    public void init() throws IOException {

        //BlobServiceClient blobServiceClient = new BlobServiceClientBuilder().connectionString(connectStr).buildClient();
        //BlobContainerClient containerClient;

        // container name
        if (StringUtils.isEmpty(containerName)) {
            // get hostname of DSpace UI to use to name bucket
            String hostname = Utils.getHostName(configurationService.getProperty("dspace.ui.url"));
            containerName = "dspace-asset-" + hostname;
            log.warn("Blob container is not configured, setting default: " + containerName);
        }
        /**

         try {
         if (!s3Service.doesBucketExist(containerName)) {
         s3Service.createBucket(bucketName);
         containerClient = blobServiceClient.createBlobContainer(containerName);
         log.info("Creating new S3 Bucket: " + containerName);
         }
         } catch (AmazonClientException e) {
         log.error(e);
         throw new IOException(e);
         }
         */

    }

    @Override
    public String generateId() {
        return Utils.generateKey();
    }

    @Override
    public InputStream get(Bitstream bitstream) throws IOException {
        log.info("Azure mock get");
        return null;
    }

    @Override
    public void put(Bitstream bitstream, InputStream in) throws IOException {
        String key = getFullKey(bitstream.getInternalId());
        //Copy istream to temp file, and send the file, with some metadata
        File scratchFile = File.createTempFile(bitstream.getInternalId(), "blobbs");
        FileUtils.copyInputStreamToFile(in, scratchFile);
        long contentLength = scratchFile.length();

        //bitstream.setSizeBytes(contentLength);
        //bitstream.setChecksum(putObjectResult.getETag());
        //bitstream.setChecksumAlgorithm(CSA);

        log.info("Azure mock put");
    }

    @Override
    public Map about(Bitstream bitstream, Map attrs) throws IOException {
        log.info("Azure mock about");
        return null;
    }

    @Override
    public void remove(Bitstream bitstream) throws IOException {
        log.info("Azure mock remove");
    }

    public void setConnectionstring(String connectionstring) {
        this.connectionstring = connectionstring;
    }

    public void setContainerName(String containerName) {
        this.containerName = containerName;
    }

    public String getContainerName() {
        return containerName;
    }

    public String getFullKey(String id) {
        if (StringUtils.isNotEmpty(subfolder)) {
            return subfolder + "/" + id;
        } else {
            return id;
        }
    }
}
