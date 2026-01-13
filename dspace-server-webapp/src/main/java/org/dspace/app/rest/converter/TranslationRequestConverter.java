/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.TranslationRequestRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.translationrequest.TranslationRequest;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the TranslationRequest in the DSpace API data model and the
 * REST data model
 *
 * @author [Your Name]
 */
@Component
public class TranslationRequestConverter implements DSpaceConverter<TranslationRequest, TranslationRequestRest> {

    @Override
    public TranslationRequestRest convert(TranslationRequest obj, Projection projection) {
        TranslationRequestRest rest = new TranslationRequestRest();
        rest.setProjection(projection);
        rest.setId(obj.getId());
        rest.setPublicationGUID(obj.getPublicationGUID());
        rest.setLanguage(obj.getLanguage());
        rest.setStatus(obj.getStatus());
        rest.setCreatedDate(obj.getCreatedDate());
        rest.setClosedDate(obj.getClosedDate());
        return rest;
    }

    @Override
    public Class<TranslationRequest> getModelClass() {
        return TranslationRequest.class;
    }
}

