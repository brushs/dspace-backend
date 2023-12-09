package org.dspace.app.rest;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VocabularyController {

    @RequestMapping(value = "vocab", method = RequestMethod.GET)
    public String test() throws Exception
    {
        // TODO
        // Validate Export XML parameters

        return "test";
    }
}
