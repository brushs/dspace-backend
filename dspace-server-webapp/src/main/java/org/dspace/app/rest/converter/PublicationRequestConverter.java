/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.PublicationRequestRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.publicationrequest.PublicationRequest;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the PublicationRequest in the DSpace API data model and the
 * REST data model
 *
 * @author [Your Name]
 */
@Component
public class PublicationRequestConverter implements DSpaceConverter<PublicationRequest, PublicationRequestRest> {

    @Override
    public PublicationRequestRest convert(PublicationRequest obj, Projection projection) {
        PublicationRequestRest rest = new PublicationRequestRest();
        rest.setProjection(projection);
        rest.setId(obj.getId());
        rest.setPublicationGUID(obj.getPublicationGUID());
        rest.setUserEmailAddress(obj.getUserEmailAddress());
        rest.setLanguage(obj.getLanguage());
        rest.setStatus(obj.getStatus());
        return rest;
    }

    @Override
    public Class<PublicationRequest> getModelClass() {
        return PublicationRequest.class;
    }
}

