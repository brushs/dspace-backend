/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.factory;

import org.dspace.content.service.*;
import org.dspace.services.factory.DSpaceServicesFactory;

public abstract class VocabularyServiceFactory {

    public abstract VocabularyService getVocabularyService();

    public static VocabularyServiceFactory getInstance() {
        return DSpaceServicesFactory.getInstance().getServiceManager()
                                    .getServiceByName("vocabularyServiceFactory", VocabularyServiceFactory.class);
    }

}
