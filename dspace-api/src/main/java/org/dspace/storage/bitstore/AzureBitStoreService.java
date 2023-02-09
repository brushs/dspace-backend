/*
Copyright (c) Microsoft Corporation.
        Licensed under the MIT license.
*/
package org.dspace.storage.bitstore;


import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobProperties;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Bitstream;
import org.dspace.core.Utils;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
//import com.azure.storage.common.StorageSharedKeyCredential;

public class AzureBitStoreService implements BitStoreService{

    private static final Logger log = LogManager.getLogger(AzureBitStoreService.class);

    private static final String CSA = "MD5";
    private static final String BLOB_SUFFIX = ".blobbs";

    private String containerName = null;
    private String connectionstring;

    private String subfolder;

    BlobServiceClient blobServiceClient;

    private static final ConfigurationService configurationService
            = DSpaceServicesFactory.getInstance().getConfigurationService();
    @Override
    public void init() throws IOException {

        blobServiceClient = new BlobServiceClientBuilder().connectionString(connectionstring).buildClient();
        // container name
        if (StringUtils.isEmpty(containerName)) {
            // get hostname of DSpace UI to use to name bucket
            String hostname = Utils.getHostName(configurationService.getProperty("dspace.ui.url"));
            containerName = "dspace-asset-" + hostname;
            log.warn("Blob container is not configured, setting default: " + containerName);
        }
        /**

         try {
         if (!blobServiceClient.getBlobContainerClient(containerName);) {
             containerClient = blobServiceClient.createBlobContainer(containerName);
             BlobContainerClient containerClient = blobServiceClient.createBlobContainer(containerName);
             log.info("Creating new container: " + containerName);
         }
         } catch (Exception e) {
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
        String sInternalId = bitstream.getInternalId();
        if (sInternalId.startsWith("-R")) {
            sInternalId = sInternalId.substring(2);
        }

        StringBuilder bufFilename = new StringBuilder();
        bufFilename.append(sInternalId);

        if (bufFilename.indexOf(".") <= 0) {
            bufFilename.append(BLOB_SUFFIX);
        }

        String filename = bufFilename.toString();
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        try {
            BlobClient blobClient = containerClient.getBlobClient(filename);
            ByteArrayInputStream bis = new ByteArrayInputStream(blobClient.downloadContent().toBytes());
            return bis;
        } catch (Exception e) {
            log.error("get(" + bufFilename + ")", e);
            throw new IOException(e);
        }
    }

    @Override
    public void put(Bitstream bitstream, InputStream in) throws IOException {
        //Copy istream to temp file, and send the file, with some metadata
        File scratchFile = getTempFile(bitstream);
        try {

            FileUtils.copyInputStreamToFile(in, scratchFile);

            long contentLength = scratchFile.length();
            BlobContainerClient containerClient;
            containerClient = blobServiceClient.getBlobContainerClient(containerName);

            String fileName = scratchFile.getName();
            BlobClient blobClient = containerClient.getBlobClient(fileName);
            blobClient.uploadFromFile(String.valueOf(scratchFile));
            bitstream.setSizeBytes(contentLength);
            try {
                DigestInputStream dis = new DigestInputStream(in, MessageDigest.getInstance(CSA));
                bitstream.setChecksum(Utils.toHex(dis.getMessageDigest().digest()));
                bitstream.setChecksumAlgorithm(CSA);
                scratchFile.delete();
            } catch (NoSuchAlgorithmException e) {
                // Should never happen
                log.warn("Caught NoSuchAlgorithmException", e);
            }
        } catch (Exception e) {
            log.error("put(" + bitstream.getInternalId() + ", inputstream)", e);
            throw new IOException(e);
        }finally {
            if (scratchFile.exists()) {
                scratchFile.delete();
            }
        }
    }

    @Override
    public Map about(Bitstream bitstream, Map attrs) throws IOException {
        String sInternalId = bitstream.getInternalId();
        if (sInternalId.startsWith("-R")) {
            sInternalId = sInternalId.substring(2);
        }

        StringBuilder bufFilename = new StringBuilder();
        bufFilename.append(sInternalId);

        if (bufFilename.indexOf(".") <= 0) {
            bufFilename.append(BLOB_SUFFIX);
        }

        String filename = bufFilename.toString();
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);

        log.info("Retrieving Blob: " + filename);
        BlobClient blobClient = containerClient.getBlobClient(filename);
        BlobProperties properties = blobClient.getProperties();

        if (properties != null) {
            if (attrs.containsKey("size_bytes")) {
                attrs.put("size_bytes", properties.getBlobSize());
            }
            if (attrs.containsKey("checksum")) {
                attrs.put("checksum", properties.getETag());
                attrs.put("checksum_algorithm", CSA);
            }
            if (attrs.containsKey("modified")) {
                attrs.put("modified", String.valueOf(properties.getLastModified().toEpochSecond()));
            }
            return attrs;
        }

        return null;
    }

    @Override
    public void remove(Bitstream bitstream) throws IOException {
        String sInternalId = bitstream.getInternalId();
        StringBuilder bufFilename = new StringBuilder();
        bufFilename.append(sInternalId);
        bufFilename.append(".blobbs");
        String filename = bufFilename.toString();
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        containerClient.getBlobClient(filename).delete();
    }

    protected File getTempFile(Bitstream bitstream) throws  IOException {
            // Check that bitstream is not null
            if (bitstream == null) {
                return null;
            }

            // turn the internal_id into a file path relative to the assetstore
            // directory
            String sInternalId = bitstream.getInternalId();
            String tempPath = System.getProperty("java.io.tmpdir");
            //String name = sInternalId + ".blobbs";
            StringBuilder bufFilename = new StringBuilder();
            bufFilename.append(sInternalId);
            bufFilename.append(".blobbs");
            String name = bufFilename.toString();


            File f = new File(tempPath, name);
//            if (!name.equals(f.getName()) || f.isInvalid()) {
//                if (System.getSecurityManager() != null)
//                    throw new IOException("Unable to create temporary file");
//                else
//                    throw new IOException("Unable to create temporary file, "
//                            + name);
//            }
            return f;
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
