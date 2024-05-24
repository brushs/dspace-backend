package org.dspace.app.rest.model;

import org.dspace.app.rest.RestResourceController;

public class MetadataVocabularyRest extends BaseObjectRest<String> {

    public static final String NAME = "vocabularyMetadata";
    public static final String CATEGORY = RestAddressableModel.SUBMISSION;

    private String VocabularyName;
    private String MetadataId;
    private String FormSource;

    public void setVocabularyName(String name) {
        this.VocabularyName = name;
    }

    public String getVocabularyName() {
        return VocabularyName;
    }

    public void setMetadataId(String id) {
        this.MetadataId = id;
    }

    public String getMetadataId() {
        return MetadataId;
    }

    public void setFormSource(String source) {
        this.FormSource = source;
    }

    public String getFormSource() {
        return FormSource;
    }

    @Override
    public String getId() {
        return MetadataId;
    }
    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public Class getController() {
        return RestResourceController.class;
    }

    @Override
    public String getType() {
        return NAME;
    }

    @Override
    public String getTypePlural() {
        return super.getTypePlural();
    }

}

