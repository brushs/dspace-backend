/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

/**
 * The TranslationRequest REST Resource
 *
 * @author [Your Name]
 */
public class TranslationRequestRest extends RestAddressableModel {
    public static final String NAME = "translationrequest";
    public static final String CATEGORY = RestModel.REQUEST;

    private Integer id;
    private String publicationUUID;
    private String bitstreamUUID;
    private String language;
    private Integer status;
    private Date createdDate;
    private Date closedDate;
    private String titleEn;
    private String titleFr;
    private String bitstreamName;
    private String notes;

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

    public String getPublicationUUID() {
        return publicationUUID;
    }

    public void setPublicationUUID(String publicationUUID) {
        this.publicationUUID = publicationUUID;
    }

    public String getBitstreamUUID() {
        return bitstreamUUID;
    }

    public void setBitstreamUUID(String bitstreamUUID) {
        this.bitstreamUUID = bitstreamUUID;
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

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Date getClosedDate() {
        return closedDate;
    }

    public void setClosedDate(Date closedDate) {
        this.closedDate = closedDate;
    }

    public String getTitleEn() {
        return titleEn;
    }

    public void setTitleEn(String titleEn) {
        this.titleEn = titleEn;
    }

    public String getTitleFr() {
        return titleFr;
    }

    public void setTitleFr(String titleFr) {
        this.titleFr = titleFr;
    }

    public String getBitstreamName() {
        return bitstreamName;
    }

    public void setBitstreamName(String bitstreamName) {
        this.bitstreamName = bitstreamName;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}

