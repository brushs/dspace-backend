/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.translationrequest;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * Class representing a translation request.
 *
 * @author [Your Name]
 */
@Entity
@Table(name = "translationrequest")
public class TranslationRequest {

    @Id
    @Column(name = "translationrequest_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "translationrequest_seq")
    @SequenceGenerator(name = "translationrequest_seq", sequenceName = "translationrequest_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "publication_guid", nullable = false, length = 255)
    private String publicationGUID;

    @Column(name = "language", nullable = false, length = 50)
    private String language;

    @Column(name = "status")
    private Integer status;

    @Column(name = "created_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdDate;

    @Column(name = "closed_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date closedDate;

    /**
     * Protected constructor, create object using TranslationRequestService
     */
    protected TranslationRequest() {
    }

    public Integer getId() {
        return id;
    }

    public String getPublicationGUID() {
        return publicationGUID;
    }

    public void setPublicationGUID(String publicationGUID) {
        this.publicationGUID = publicationGUID;
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
}

