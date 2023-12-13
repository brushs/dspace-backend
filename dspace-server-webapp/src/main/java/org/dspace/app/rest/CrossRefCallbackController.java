package org.dspace.app.rest;

import jakarta.xml.bind.JAXBException;
import org.crossref.schema._5_3.DoiBatch;
import org.dspace.content.DSpaceObject;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.identifier.DOIIdentifierProvider;
import org.dspace.identifier.doi.CrossRefXmlBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;

import javax.xml.datatype.DatatypeConfigurationException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;

import static org.dspace.app.rest.utils.ContextUtil.obtainContext;

@Controller
@RequestMapping("/crossref")
public class CrossRefCallbackController {

    private static final Logger log = LoggerFactory.getLogger(CrossRefCallbackController.class);

    private static final String HEADER_BATCH_ID = "CROSSREF-EXTERNAL-ID";
    private static final String HEADER_RETRIEVE_URL = "CROSSREF-RETRIEVE-URL";

    @Autowired
    private DOIIdentifierProvider doiIdentifierProvider;

    @Autowired
    private CrossRefXmlBuilder xmlBuilder;

    @Autowired
    private ItemService itemService;

    @RequestMapping ("/callback")
    public void processCallback(HttpServletRequest request,
                       HttpServletResponse response) {
        log.info("Callback Received");
        Context context = obtainContext(request);

        try {
            String batchId = request.getHeader(HEADER_BATCH_ID);
            log.info("Batch ID: " + batchId);

            String retrieveUrl = request.getHeader(HEADER_RETRIEVE_URL);
            log.info("Retrieve URL: " + retrieveUrl);

            doiIdentifierProvider.processCallback(context, batchId, retrieveUrl);

            context.commit();
        } catch (Exception e) {
            log.error("Error processing callback notification", e);
        }
    }

    @RequestMapping ("/testbuild/{uuid}")
    public void test(@PathVariable UUID uuid,
                     HttpServletRequest request,
                     HttpServletResponse response
                     ) throws SQLException, IOException, DatatypeConfigurationException, JAXBException {
        log.info("CrossRef Test Build Call Received");

        Context context = obtainContext(request);

        DSpaceObject dso = itemService.find(context, uuid);
        DoiBatch batch = xmlBuilder.buildCrossRefSubmission(context, dso, "10.4905/123456");

        response.setContentType("application/xml");
        response.setHeader("Content-Disposition", "attachment; filename=" + "temp" + ".xml");
        final JAXBContext jaxbContext = JAXBContext.newInstance(DoiBatch.class);
        final Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, "http://www.crossref.org/schema/5.3.0 http://data.crossref.org/schemas/crossref5.3.0.xsd");
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(batch, response.getOutputStream());
    }
}
