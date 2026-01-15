/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.translation2publication;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;

/**
 * Class representing a link between a translation request and a publication request.
 *
 * @author [Your Name]
 */
@Entity
@Table(name = "translation2publication")
@IdClass(Translation2PublicationId.class)
public class Translation2Publication {

    @Id
    @Column(name = "translationrequest_id", nullable = false)
    private Integer translationRequestId;

    @Id
    @Column(name = "publicationrequest_id", nullable = false)
    private Integer publicationRequestId;

    /**
     * Protected constructor, create object using Translation2PublicationService
     */
    protected Translation2Publication() {
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

