/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.factory;

import org.dspace.content.DSpaceObject;
import org.dspace.content.RelationshipMetadataService;
import org.dspace.content.service.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class VocabularyServiceFactoryImpl extends VocabularyServiceFactory {

    @Autowired(required = true)
    private VocabularyService vocabularyService;

    @Override
    public VocabularyService getVocabularyService() {
        return vocabularyService;
    }

}
