/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.PublicationRequestRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;

/**
 * PublicationRequest Rest HAL Resource. The HAL Resource wraps the REST Resource
 * adding support for the links and embedded resources
 *
 * @author [Your Name]
 */
@RelNameDSpaceResource(PublicationRequestRest.NAME)
public class PublicationRequestResource extends DSpaceResource<PublicationRequestRest> {
    public PublicationRequestResource(PublicationRequestRest publicationRequest, Utils utils) {
        super(publicationRequest, utils);
    }
}

