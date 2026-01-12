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
 * The PublicationRequest REST Resource
 *
 * @author [Your Name]
 */
public class PublicationRequestRest extends RestAddressableModel {
    public static final String NAME = "publicationrequest";
    public static final String CATEGORY = RestModel.REQUEST;

    private Integer id;
    private String publicationGUID;
    private String userEmailAddress;
    private String language;
    private Integer status;

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

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getPublicationGUID() {
        return publicationGUID;
    }

    public void setPublicationGUID(String publicationGUID) {
        this.publicationGUID = publicationGUID;
    }

    public String getUserEmailAddress() {
        return userEmailAddress;
    }

    public void setUserEmailAddress(String userEmailAddress) {
        this.userEmailAddress = userEmailAddress;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}

