/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.translationrequest.dao.impl;

import java.sql.SQLException;
import java.util.List;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.translationrequest.TranslationRequest;
import org.dspace.translationrequest.dao.TranslationRequestDAO;

/**
 * Hibernate implementation of the Database Access Object interface class for the TranslationRequest object.
 * This class is responsible for all database calls for the TranslationRequest object and is autowired by Spring.
 * This class should never be accessed directly.
 *
 * @author [Your Name]
 */
public class TranslationRequestDAOImpl extends AbstractHibernateDAO<TranslationRequest>
    implements TranslationRequestDAO {

    protected TranslationRequestDAOImpl() {
        super();
    }

    @Override
    public TranslationRequest findByID(Context context, Integer id) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<TranslationRequest> criteriaQuery = getCriteriaQuery(criteriaBuilder, TranslationRequest.class);
        Root<TranslationRequest> root = criteriaQuery.from(TranslationRequest.class);
        criteriaQuery.select(root);
        criteriaQuery.where(criteriaBuilder.equal(root.get("id"), id));
        return singleResult(context, criteriaQuery);
    }

    @Override
    public List<TranslationRequest> findByPublicationUUID(Context context, String publicationUUID)
        throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<TranslationRequest> criteriaQuery = getCriteriaQuery(criteriaBuilder, TranslationRequest.class);
        Root<TranslationRequest> root = criteriaQuery.from(TranslationRequest.class);
        criteriaQuery.select(root);
        criteriaQuery.where(criteriaBuilder.equal(root.get("publicationUUID"), publicationUUID));
        return list(context, criteriaQuery, false, TranslationRequest.class, -1, -1);
    }

    @Override
    public TranslationRequest findByBitstreamUUIDAndPublicationUUID(Context context, String bitstreamUUID,
                                                                      String publicationUUID) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<TranslationRequest> criteriaQuery = getCriteriaQuery(criteriaBuilder, TranslationRequest.class);
        Root<TranslationRequest> root = criteriaQuery.from(TranslationRequest.class);
        criteriaQuery.select(root);
        criteriaQuery.where(
            criteriaBuilder.and(
                criteriaBuilder.equal(root.get("bitstreamUUID"), bitstreamUUID),
                criteriaBuilder.equal(root.get("publicationUUID"), publicationUUID)
            )
        );
        return singleResult(context, criteriaQuery);
    }

    @Override
    public List<TranslationRequest> findAll(Context context, int offset, int limit) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<TranslationRequest> criteriaQuery = getCriteriaQuery(criteriaBuilder, TranslationRequest.class);
        Root<TranslationRequest> root = criteriaQuery.from(TranslationRequest.class);
        criteriaQuery.select(root);
        criteriaQuery.orderBy(criteriaBuilder.desc(root.get("id")));
        return list(context, criteriaQuery, false, TranslationRequest.class, limit, offset);
    }

    @Override
    public int countRows(Context context) throws SQLException {
        Query query = createQuery(context, "SELECT count(*) FROM TranslationRequest");
        return count(query);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<TranslationRequest> findByPublicationTitle(Context context, String title, int offset, int limit)
        throws SQLException {
        // Use native SQL to join with metadatavalue table via publication_uuid
        // Join twice (LEFT JOIN) to get both English and French titles without duplicating rows
        String sql = "SELECT DISTINCT tr.* FROM translationrequest tr " +
                     "LEFT JOIN metadatavalue mv_en ON CAST(tr.publication_uuid AS UUID) = mv_en.dspace_object_id " +
                     "  AND mv_en.metadata_field_id = 73 AND mv_en.text_lang = 'en' " +
                     "LEFT JOIN metadatavalue mv_fr ON CAST(tr.publication_uuid AS UUID) = mv_fr.dspace_object_id " +
                     "  AND mv_fr.metadata_field_id = 73 AND mv_fr.text_lang = 'fr' " +
                     "WHERE (LOWER(mv_en.text_value) LIKE LOWER(:title) " +
                     "   OR LOWER(mv_fr.text_value) LIKE LOWER(:title)) " +
                     "ORDER BY tr.translationrequest_id DESC";

        Query query = getHibernateSession(context).createNativeQuery(sql, TranslationRequest.class);
        query.setParameter("title", "%" + title + "%");

        if (limit > 0) {
            query.setMaxResults(limit);
        }
        if (offset > 0) {
            query.setFirstResult(offset);
        }

        return query.getResultList();
    }

    @Override
    public int countByPublicationTitle(Context context, String title) throws SQLException {
        // Use native SQL to count with the same join logic
        String sql = "SELECT COUNT(DISTINCT tr.translationrequest_id) FROM translationrequest tr " +
                     "LEFT JOIN metadatavalue mv_en ON CAST(tr.publication_uuid AS UUID) = mv_en.dspace_object_id " +
                     "  AND mv_en.metadata_field_id = 73 AND mv_en.text_lang = 'en' " +
                     "LEFT JOIN metadatavalue mv_fr ON CAST(tr.publication_uuid AS UUID) = mv_fr.dspace_object_id " +
                     "  AND mv_fr.metadata_field_id = 73 AND mv_fr.text_lang = 'fr' " +
                     "WHERE (LOWER(mv_en.text_value) LIKE LOWER(:title) " +
                     "   OR LOWER(mv_fr.text_value) LIKE LOWER(:title))";

        Query query = getHibernateSession(context).createNativeQuery(sql);
        query.setParameter("title", "%" + title + "%");

        Number result = (Number) query.getSingleResult();
        return result != null ? result.intValue() : 0;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<TranslationRequest> findByUserEmail(Context context, String email, int offset, int limit)
        throws SQLException {
        // Use native SQL to join with publicationrequest table
        // Join via publication_uuid to get the user email from the publication request
        String sql = "SELECT DISTINCT tr.* FROM translationrequest tr " +
                     "INNER JOIN publicationrequest pr ON tr.publication_uuid = pr.publication_uuid " +
                     "WHERE LOWER(pr.user_email) LIKE LOWER(:email) " +
                     "ORDER BY tr.translationrequest_id DESC";

        Query query = getHibernateSession(context).createNativeQuery(sql, TranslationRequest.class);
        query.setParameter("email", "%" + email + "%");

        if (limit > 0) {
            query.setMaxResults(limit);
        }
        if (offset > 0) {
            query.setFirstResult(offset);
        }

        return query.getResultList();
    }

    @Override
    public int countByUserEmail(Context context, String email) throws SQLException {
        // Use native SQL to count with the same join logic
        String sql = "SELECT COUNT(DISTINCT tr.translationrequest_id) FROM translationrequest tr " +
                     "INNER JOIN publicationrequest pr ON tr.publication_uuid = pr.publication_uuid " +
                     "WHERE LOWER(pr.user_email) LIKE LOWER(:email)";

        Query query = getHibernateSession(context).createNativeQuery(sql);
        query.setParameter("email", "%" + email + "%");

        Number result = (Number) query.getSingleResult();
        return result != null ? result.intValue() : 0;
    }

    @Override
    public List<TranslationRequest> findByStatus(Context context, int status, int offset, int limit)
        throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<TranslationRequest> criteriaQuery = getCriteriaQuery(criteriaBuilder, TranslationRequest.class);
        Root<TranslationRequest> root = criteriaQuery.from(TranslationRequest.class);
        criteriaQuery.select(root);
        criteriaQuery.where(criteriaBuilder.equal(root.get("status"), status));
        criteriaQuery.orderBy(criteriaBuilder.desc(root.get("id")));
        return list(context, criteriaQuery, false, TranslationRequest.class, limit, offset);
    }

    @Override
    public int countByStatus(Context context, int status) throws SQLException {
        Query query = createQuery(context, "SELECT count(*) FROM TranslationRequest WHERE status = :status");
        query.setParameter("status", status);
        return count(query);
    }
}

