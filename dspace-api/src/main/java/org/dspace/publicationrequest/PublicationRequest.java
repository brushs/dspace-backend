/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.publicationrequest;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * Class representing a publication request.
 *
 * @author [Your Name]
 */
@Entity
@Table(name = "publicationrequest")
public class PublicationRequest {

    @Id
    @Column(name = "publicationrequest_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "publicationrequest_seq")
    @SequenceGenerator(name = "publicationrequest_seq", sequenceName = "publicationrequest_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "publication_uuid", nullable = false, length = 255)
    private String publicationUUID;

    @Column(name = "user_email_address", nullable = false, length = 255)
    private String userEmailAddress;

    @Column(name = "language", nullable = false, length = 50)
    private String language;

    @Column(name = "status")
    private Integer status;

    /**
     * Protected constructor, create object using PublicationRequestService
     */
    protected PublicationRequest() {
    }

    public Integer getId() {
        return id;
    }

    public String getPublicationUUID() {
        return publicationUUID;
    }

    public void setPublicationUUID(String publicationUUID) {
        this.publicationUUID = publicationUUID;
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

