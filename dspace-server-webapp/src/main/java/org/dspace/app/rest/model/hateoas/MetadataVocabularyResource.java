package org.dspace.app.rest.model.hateoas;

import org.dspace.app.rest.model.MetadataVocabularyRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;
import org.dspace.app.rest.utils.Utils;

@RelNameDSpaceResource(MetadataVocabularyRest.NAME)
public class MetadataVocabularyResource extends DSpaceResource<MetadataVocabularyRest> {
    public MetadataVocabularyResource(MetadataVocabularyRest data, Utils utils) {
        super(data, utils);
    }
}
