/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier.doi;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.crossref.schema._5_3.DoiBatch;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.*;
import java.net.URISyntaxException;

public class CrossRefConnector implements DOIConnector {

    private static final Logger log = LoggerFactory.getLogger(CrossRefConnector.class);

    // Configuration property names
    static final String CFG_USER = "identifier.doi.user";
    static final String CFG_PASSWORD = "identifier.doi.password";
    static final String CFG_URL = "identifier.doi.url";

    protected String USERNAME = null;
    protected String PASSWORD = null;
    protected String URL = null;

    @Autowired
    protected CrossRefXmlBuilder crossRefXmlBuilder;

    @Autowired
    protected ConfigurationService configurationService;

    protected String getUsername() {
        if (null == this.USERNAME) {
            this.USERNAME = this.configurationService.getProperty(CFG_USER);
            if (null == this.USERNAME) {
                throw new RuntimeException("Unable to load username from "
                                               + "configuration. Cannot find property " +
                                               CFG_USER + ".");
            }
        }
        return this.USERNAME;
    }

    protected String getPassword() {
        if (null == this.PASSWORD) {
            this.PASSWORD = this.configurationService.getProperty(CFG_PASSWORD);
            if (null == this.PASSWORD) {
                throw new RuntimeException("Unable to load password from "
                                               + "configuration. Cannot find property " +
                                               CFG_PASSWORD + ".");
            }
        }
        return this.PASSWORD;
    }

    protected String getUrl() {
        if (null == this.URL) {
            this.URL = this.configurationService.getProperty(CFG_URL);
            if (null == this.URL) {
                throw new RuntimeException("Unable to load url from "
                        + "configuration. Cannot find property " +
                        CFG_URL + ".");
            }
        }
        return this.URL;
    }

    @Override
    public String registerDOI(Context context, DSpaceObject dso, String doi)
        throws DOIIdentifierException {

        log.info("registerDOI: " + doi);

        CrossRefResponse resp = null;
        try {
            resp = this.sendRegisterDOIPostRequest(context, dso, doi);
        } catch (URISyntaxException e) {
            log.error("Caught SQL-Exception while resolving handle to URL: "
                          + e.getMessage());
            throw new RuntimeException(e);
        }

        switch (resp.statusCode) {
            // 200 -> accepted submission
            // This does not mean that the DOI was created, only that they accepted our post request
            // Processing is queued and they will send a callback to the URL we specified
            case (200): {
                return resp.getBatchId();
            }
            // Catch all other http status codes as they indicate an issue.
            default: {
                log.error("While registration of DOI {}, we got a http status code "
                             + "{} and the message \"{}\".",
                        doi, Integer.toString(resp.statusCode), resp.getContent());
                throw new DOIIdentifierException("CrossRef did not accept POST request.",
                                                 DOIIdentifierException.BAD_ANSWER);
            }
        }
    }

    protected CrossRefResponse sendRegisterDOIPostRequest(Context context, DSpaceObject dso, String doi)
            throws DOIIdentifierException, URISyntaxException {

        log.info("sendRegisterDOIPostRequest");

        // assemble request content:
        HttpEntity reqEntity = null;
        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpPost uploadFile = new HttpPost(getUrl());
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addTextBody("operation", "doMDUpload", ContentType.TEXT_PLAIN);
            builder.addTextBody("login_id", getUsername(), ContentType.TEXT_PLAIN);
            builder.addTextBody("login_passwd", getPassword(), ContentType.TEXT_PLAIN);

            log.info("Building XML");
            DoiBatch batch = crossRefXmlBuilder.buildCrossRefSubmission(context, dso, doi);
            log.info("XML ready");

            final JAXBContext jaxbContext = JAXBContext.newInstance(DoiBatch.class);
            final Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, "http://www.crossref.org/schema/5.3.0 http://data.crossref.org/schemas/crossref5.3.0.xsd");
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            marshaller.marshal(batch, output);

            // This attaches the file to the POST:
            builder.addBinaryBody(
                    "fname",
                    output.toByteArray(),
                    ContentType.APPLICATION_OCTET_STREAM,
                    "crossrefupload.xml"
            );

            HttpEntity multipart = builder.build();
            uploadFile.setEntity(multipart);
            CloseableHttpResponse response = httpClient.execute(uploadFile);
            HttpEntity responseEntity = response.getEntity();
            log.info(responseEntity.toString());
            log.info(EntityUtils.toString(response.getEntity()));

            CrossRefResponse crossRefResponse = new CrossRefResponse(response.getStatusLine().getStatusCode(),
                    responseEntity.toString(), String.valueOf(batch.getHead().getDoiBatchId()));

            return crossRefResponse;

        } catch (Exception e) {
            log.error("Caught an Exception while executing HTTP request:"
                    + e.getMessage(), e);
        }  finally {
            // release resources
            try {
                EntityUtils.consume(reqEntity);
            } catch (IOException ioe) {
                log.info("Caught an IOException while releasing a HTTPEntity:"
                             + ioe.getMessage(), ioe);
            }
        }
        return null;
    }

    @Override
    public boolean isDOIReserved(Context context, String doi)
            throws DOIIdentifierException {

        // CrossRef does not support DOI reservation
        return false;
    }

    @Override
    public boolean isDOIRegistered(Context context, String doi)
            throws DOIIdentifierException {

        // CrossRef does not support checking status via API
        return false;
    }

    @Override
    public void updateMetadata(Context context, DSpaceObject dso, String doi)
            throws DOIIdentifierException {
        // TODO
        // CrossRef supports metadata updates by sending in the whole register request again
        // Include all data as empty fields would be treated as deleted metadata
    }

    @Override
    public void reserveDOI(Context context, DSpaceObject dso, String doi)
            throws DOIIdentifierException {

        // Not supported by CrossRef API
    }

    @Override
    public void deleteDOI(Context context, String doi)
            throws DOIIdentifierException {

        // Not supported by CrossRef API
    }

    protected class CrossRefResponse {
        private final int statusCode;
        private final String content;
        private final String batchId;

        protected CrossRefResponse(int statusCode, String content, String batchId) {
            this.statusCode = statusCode;
            this.content = content;
            this.batchId = batchId;
        }

        protected int getStatusCode() {
            return this.statusCode;
        }

        protected String getContent() {
            return this.content;
        }

        protected String getBatchId() {
            return this.batchId;
        }
    }
}
