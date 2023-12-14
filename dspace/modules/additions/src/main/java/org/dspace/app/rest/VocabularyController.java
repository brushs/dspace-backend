package org.dspace.app.rest;

import org.dspace.content.Term;
import org.dspace.content.service.VocabularyService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

import java.util.List;

import static org.dspace.app.rest.utils.ContextUtil.obtainContext;

@Controller
@RequestMapping("/vocabulary")
public class VocabularyController {

    @Autowired
    private VocabularyService vocabularyService;

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
}
