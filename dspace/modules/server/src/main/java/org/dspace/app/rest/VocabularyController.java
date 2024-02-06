package org.dspace.app.rest;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.content.Term;
import org.dspace.content.service.VocabularyService;
import org.dspace.core.Context;
import org.dspace.vocabulary.model.xml.Node;
import org.dspace.vocabulary.service.ControlledVocabularyXmlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.List;

import static org.dspace.app.rest.utils.ContextUtil.obtainContext;

@RestController
@RequestMapping("/vocabulary")
public class VocabularyController {

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

    @RequestMapping(value = "name", method = RequestMethod.GET)
    public String getByName(@RequestParam String name,
                            HttpServletRequest request) throws Exception
    {
        Context context = obtainContext(request);

        //Integer num = vocabularyService.findByName(context, name).getId();

        List<Term> terms = vocabularyService.findByName(context, name, null);

        return String.valueOf(1);
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
}