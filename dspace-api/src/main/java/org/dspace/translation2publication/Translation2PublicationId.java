/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.translation2publication;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key class for Translation2Publication entity.
 *
 * @author [Your Name]
 */
public class Translation2PublicationId implements Serializable {

    private Integer translationRequestId;
    private Integer publicationRequestId;

    public Translation2PublicationId() {
    }

    public Translation2PublicationId(Integer translationRequestId, Integer publicationRequestId) {
        this.translationRequestId = translationRequestId;
        this.publicationRequestId = publicationRequestId;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Translation2PublicationId that = (Translation2PublicationId) o;
        return Objects.equals(translationRequestId, that.translationRequestId) &&
               Objects.equals(publicationRequestId, that.publicationRequestId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(translationRequestId, publicationRequestId);
    }
}

