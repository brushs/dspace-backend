/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

/**
 * The Translation2Publication REST Resource
 *
 * @author [Your Name]
 */
public class Translation2PublicationRest extends BaseObjectRest<String> {
    public static final String NAME = "translation2publication";
    public static final String CATEGORY = RestModel.REQUEST;

    private Integer translationRequestId;
    private Integer publicationRequestId;

    @Override
    @JsonProperty(access = Access.READ_ONLY)
    public String getId() {
        // Composite ID in format: "translationRequestId_publicationRequestId"
        if (translationRequestId != null && publicationRequestId != null) {
            return translationRequestId + "_" + publicationRequestId;
        }
        return null;
    }

    @Override
    @JsonProperty(access = Access.READ_ONLY)
    public String getType() {
        return NAME;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Class getController() {
        return org.dspace.app.rest.RestResourceController.class;
    }

    public Integer getTranslationRequestId() {
        return translationRequestId;
    }

    public void setTranslationRequestId(Integer translationRequestId) {
        this.translationRequestId = translationRequestId;
    }

    public Integer getPublicationRequestId() {
        return publicationRequestId;
    }

    public void setPublicationRequestId(Integer publicationRequestId) {
        this.publicationRequestId = publicationRequestId;
    }
}

