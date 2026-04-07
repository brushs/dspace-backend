/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.Translation2PublicationRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.translation2publication.Translation2Publication;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the Translation2Publication in the DSpace API data model and the
 * REST data model
 *
 * @author [Your Name]
 */
@Component
public class Translation2PublicationConverter
    implements DSpaceConverter<Translation2Publication, Translation2PublicationRest> {

    @Override
    public Translation2PublicationRest convert(Translation2Publication obj, Projection projection) {
        Translation2PublicationRest rest = new Translation2PublicationRest();
        rest.setProjection(projection);
        rest.setTranslationRequestId(obj.getTranslationRequestId());
        rest.setPublicationRequestId(obj.getPublicationRequestId());
        return rest;
    }

    @Override
    public Class<Translation2Publication> getModelClass() {
        return Translation2Publication.class;
    }
}

