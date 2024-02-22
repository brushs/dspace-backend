package org.dspace.app.rest;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.content.service.VocabularyService;
import org.dspace.vocabulary.model.xml.Node;
import org.dspace.vocabulary.service.ControlledVocabularyXmlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.NumberFormat;
import java.util.List;

@Controller
@RequestMapping("/vocabularyxml")
public class VocabularyXmlController {

    @Autowired
    private VocabularyService vocabularyService;

    @Autowired
    private ControlledVocabularyXmlService controlledVocabularyXmlService;

    @RequestMapping(value = "vocab", method = RequestMethod.GET)
    public String test() throws Exception
    {
        // TODO
        // Validate Export XML parameters

        return "test";
    }

    @RequestMapping(value = "exportxml", method = RequestMethod.GET)
    public void exportXml(@RequestParam Integer vocabularyId,
                          @RequestParam List<String> lang,
                          final HttpServletRequest request,
                          final HttpServletResponse response) throws Exception
    {
        // TODO
        // Validate parameters

        final Node node = controlledVocabularyXmlService.getVocabularyTree(
                ContextUtil.obtainContext(request), vocabularyId, lang);
        final JAXBContext jaxbContext = JAXBContext.newInstance(Node.class);
        final Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

        response.setContentType("application/xml");
        response.setHeader("Content-Disposition", "attachment; filename=" +
                node.getLabel() + ".xml");

        marshaller.marshal(node, response.getOutputStream());
    }

    @RequestMapping(value = "memcheck", method = RequestMethod.GET)
    @ResponseBody
    public String memCheck() {
        Runtime runtime = Runtime.getRuntime();

        final NumberFormat format = NumberFormat.getInstance();

        final long maxMemory = runtime.maxMemory();
        final long allocatedMemory = runtime.totalMemory();
        final long freeMemory = runtime.freeMemory();
        final long mb = 1024 * 1024;
        final String mega = " MB";

        StringBuilder sb = new StringBuilder();

        sb.append("========================== Memory Info ==========================\n");
        sb.append(" Free memory: " + format.format(freeMemory / mb) + mega + "\n");
        sb.append(" Allocated memory: " + format.format(allocatedMemory / mb) + mega + "\n");
        sb.append(" Max memory: " + format.format(maxMemory / mb) + mega + "\n");
        sb.append(" Total free memory: " + format.format((freeMemory + (maxMemory - allocatedMemory)) / mb) + mega + "\n");
        sb.append("=================================================================\n");

        return sb.toString();
    }
}
