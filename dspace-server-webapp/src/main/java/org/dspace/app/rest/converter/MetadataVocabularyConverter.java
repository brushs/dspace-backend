package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.MetadataVocabularyRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.util.DCMetadataVocabulary;
import org.springframework.stereotype.Component;

@Component
public class MetadataVocabularyConverter implements DSpaceConverter<DCMetadataVocabulary, MetadataVocabularyRest> {
    @Override
    public MetadataVocabularyRest convert(DCMetadataVocabulary modelObject, Projection projection) {
        MetadataVocabularyRest metadataVocabularyRest = new MetadataVocabularyRest();
        metadataVocabularyRest.setMetadataId(modelObject.getMetadataId());
        metadataVocabularyRest.setVocabularyName(modelObject.getVocabularyName());
        metadataVocabularyRest.setFormSource(modelObject.getFormSource());
        return metadataVocabularyRest;
    }

    @Override
    public Class<DCMetadataVocabulary> getModelClass() {
        return DCMetadataVocabulary.class;
    }
}

