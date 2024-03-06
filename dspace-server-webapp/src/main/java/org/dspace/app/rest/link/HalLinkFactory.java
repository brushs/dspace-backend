/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.link;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.model.hateoas.HALResource;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * This is the factory that will be called to add the links to the hal resources.
 */
@Component
public abstract class HalLinkFactory<RESOURCE, CONTROLLER> {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(HalLinkFactory.class);

    @Autowired
    private ConfigurationService configurationService;

    public boolean supports(Class clazz) {
        if (getResourceClass().isAssignableFrom(clazz)) {
            return true;
        } else {
            return false;
        }
    }

    public List<Link> getLinksFor(HALResource halResource, Pageable pageable) throws Exception {
        LinkedList<Link> list = new LinkedList<>();

        if (halResource != null && supports(halResource.getClass())) {
            addLinks((RESOURCE) halResource, pageable, list);
        }

        return list;
    }

    /**
     * Please note that this method could lead to double encoding.
     * See: https://github.com/DSpace/DSpace/issues/8333
     */
    protected <T> Link buildLink(String rel, T data) {
        UriComponentsBuilder uriComponentsBuilder = uriBuilder(data);
        if (uriComponentsBuilder.toUriString().contains("dspace")) {
            uriComponentsBuilder.host(configurationService.getProperty("dspace.server.url"));
        }
        return buildLink(rel, uriComponentsBuilder.build().toUriString());
    }

    protected <T> UriComponentsBuilder uriBuilder(T data) {
        return linkTo(data)
            .toUriComponentsBuilder();
    }

    protected Link buildLink(String rel, String href) {
        //if (rel.contentEquals("self")) {
            href = configurationService.getProperty("dspace.server.url")
                    .substring(0,configurationService.getProperty("dspace.server.url").indexOf("/server"))
                            + href.substring(href.indexOf("/server"));
        //}
        return Link.of(href, rel);
    }

    protected CONTROLLER getMethodOn(Object... parameters) {
        return methodOn(getControllerClass(), parameters);
    }

    protected <C> C getMethodOn(Class<C> clazz) {
        return methodOn(clazz);
    }

    protected abstract void addLinks(RESOURCE halResource, Pageable pageable, LinkedList<Link> list) throws Exception;

    protected abstract Class<CONTROLLER> getControllerClass();

    protected abstract Class<RESOURCE> getResourceClass();

}
