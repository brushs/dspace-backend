package org.dspace.app.util;

public class DCMetadataVocabulary {

    private String metadataId = null;
    private String vocabularyName = null;
    private String formSource = null;

    public DCMetadataVocabulary(String metadataId, String vocabularyName, String formSource) {
        this.metadataId = metadataId;
        this.vocabularyName = vocabularyName;
        this.formSource = formSource;
    }

    public String getMetadataId() {
        return metadataId;
    }

    public String getVocabularyName() {
        return vocabularyName;
    }

    public String getFormSource() {
        return formSource;
    }

    public void setFormSource(String formSource) {
        this.formSource = formSource;
    }
}
