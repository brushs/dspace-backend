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
import java.io.IOException;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.List;

@Controller
@RequestMapping("/health")
public class HealthCheckController {

    @Autowired
    private VocabularyService vocabularyService;

    @RequestMapping(value = "check", method = RequestMethod.GET)
    @ResponseBody
    public String memCheck(HttpServletRequest request) throws SQLException, IOException {
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

        // Make a non-cached DB call to check connectivity
        vocabularyService.getChildTerms(ContextUtil.obtainContext(request), 1);

        return sb.toString();
    }
}
