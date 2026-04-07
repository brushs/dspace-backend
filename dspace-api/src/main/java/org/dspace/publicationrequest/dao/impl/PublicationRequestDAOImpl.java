/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.publicationrequest.dao.impl;

import java.sql.SQLException;
import java.util.List;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.publicationrequest.PublicationRequest;
import org.dspace.publicationrequest.dao.PublicationRequestDAO;

/**
 * Hibernate implementation of the Database Access Object interface class for the PublicationRequest object.
 * This class is responsible for all database calls for the PublicationRequest object and is autowired by Spring.
 * This class should never be accessed directly.
 *
 * @author [Your Name]
 */
public class PublicationRequestDAOImpl extends AbstractHibernateDAO<PublicationRequest>
    implements PublicationRequestDAO {

    protected PublicationRequestDAOImpl() {
        super();
    }

    @Override
    public PublicationRequest findByID(Context context, Integer id) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<PublicationRequest> criteriaQuery = getCriteriaQuery(criteriaBuilder, PublicationRequest.class);
        Root<PublicationRequest> root = criteriaQuery.from(PublicationRequest.class);
        criteriaQuery.select(root);
        criteriaQuery.where(criteriaBuilder.equal(root.get("id"), id));
        return singleResult(context, criteriaQuery);
    }

    @Override
    public List<PublicationRequest> findByPublicationUUID(Context context, String publicationUUID)
        throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<PublicationRequest> criteriaQuery = getCriteriaQuery(criteriaBuilder, PublicationRequest.class);
        Root<PublicationRequest> root = criteriaQuery.from(PublicationRequest.class);
        criteriaQuery.select(root);
        criteriaQuery.where(criteriaBuilder.equal(root.get("publicationUUID"), publicationUUID));
        return list(context, criteriaQuery, false, PublicationRequest.class, -1, -1);
    }

    @Override
    public List<PublicationRequest> findByUserEmailAddress(Context context, String userEmailAddress)
        throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<PublicationRequest> criteriaQuery = getCriteriaQuery(criteriaBuilder, PublicationRequest.class);
        Root<PublicationRequest> root = criteriaQuery.from(PublicationRequest.class);
        criteriaQuery.select(root);
        criteriaQuery.where(criteriaBuilder.equal(root.get("userEmailAddress"), userEmailAddress));
        return list(context, criteriaQuery, false, PublicationRequest.class, -1, -1);
    }

    @Override
    public List<PublicationRequest> findAll(Context context, int offset, int limit) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<PublicationRequest> criteriaQuery = getCriteriaQuery(criteriaBuilder, PublicationRequest.class);
        Root<PublicationRequest> root = criteriaQuery.from(PublicationRequest.class);
        criteriaQuery.select(root);
        criteriaQuery.orderBy(criteriaBuilder.desc(root.get("id")));
        return list(context, criteriaQuery, false, PublicationRequest.class, limit, offset);
    }

    @Override
    public int countRows(Context context) throws SQLException {
        Query query = createQuery(context, "SELECT count(*) FROM PublicationRequest");
        return count(query);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<PublicationRequest> findByTitle(Context context, String title, int offset, int limit)
        throws SQLException {
        // Use native SQL to join with metadatavalue table
        // Join twice (LEFT JOIN) to get both English and French titles without duplicating rows
        String sql = "SELECT DISTINCT pr.* FROM publicationrequest pr " +
                     "LEFT JOIN metadatavalue mv_en ON CAST(pr.publication_uuid AS UUID) = mv_en.dspace_object_id " +
                     "  AND mv_en.metadata_field_id = 73 AND mv_en.text_lang = 'en' " +
                     "LEFT JOIN metadatavalue mv_fr ON CAST(pr.publication_uuid AS UUID) = mv_fr.dspace_object_id " +
                     "  AND mv_fr.metadata_field_id = 73 AND mv_fr.text_lang = 'fr' " +
                     "WHERE (LOWER(mv_en.text_value) LIKE LOWER(:title) " +
                     "   OR LOWER(mv_fr.text_value) LIKE LOWER(:title)) " +
                     "ORDER BY pr.publicationrequest_id DESC";

        Query query = getHibernateSession(context).createNativeQuery(sql, PublicationRequest.class);
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
    public int countByTitle(Context context, String title) throws SQLException {
        // Use native SQL to count with the same join logic
        String sql = "SELECT COUNT(DISTINCT pr.publicationrequest_id) FROM publicationrequest pr " +
                     "LEFT JOIN metadatavalue mv_en ON CAST(pr.publication_uuid AS UUID) = mv_en.dspace_object_id " +
                     "  AND mv_en.metadata_field_id = 73 AND mv_en.text_lang = 'en' " +
                     "LEFT JOIN metadatavalue mv_fr ON CAST(pr.publication_uuid AS UUID) = mv_fr.dspace_object_id " +
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
    public List<PublicationRequest> findByTranslationRequestId(Context context, Integer translationRequestId,
                                                                 int offset, int limit) throws SQLException {
        // Use native SQL to join with translation2publication table
        String sql = "SELECT pr.* FROM publicationrequest pr " +
                     "INNER JOIN translation2publication t2p ON pr.publicationrequest_id = t2p.publicationrequest_id " +
                     "WHERE t2p.translationrequest_id = :translationRequestId " +
                     "ORDER BY pr.publicationrequest_id DESC";

        Query query = getHibernateSession(context).createNativeQuery(sql, PublicationRequest.class);
        query.setParameter("translationRequestId", translationRequestId);

        if (limit > 0) {
            query.setMaxResults(limit);
        }
        if (offset > 0) {
            query.setFirstResult(offset);
        }

        return query.getResultList();
    }

    @Override
    public int countByTranslationRequestId(Context context, Integer translationRequestId) throws SQLException {
        // Use native SQL to count with the same join logic
        String sql = "SELECT COUNT(pr.publicationrequest_id) FROM publicationrequest pr " +
                     "INNER JOIN translation2publication t2p ON pr.publicationrequest_id = t2p.publicationrequest_id " +
                     "WHERE t2p.translationrequest_id = :translationRequestId";

        Query query = getHibernateSession(context).createNativeQuery(sql);
        query.setParameter("translationRequestId", translationRequestId);

        Number result = (Number) query.getSingleResult();
        return result != null ? result.intValue() : 0;
    }

    @Override
    public List<PublicationRequest> findByStatus(Context context, int status, int offset, int limit)
        throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<PublicationRequest> criteriaQuery = getCriteriaQuery(criteriaBuilder, PublicationRequest.class);
        Root<PublicationRequest> root = criteriaQuery.from(PublicationRequest.class);
        criteriaQuery.select(root);
        criteriaQuery.where(criteriaBuilder.equal(root.get("status"), status));
        criteriaQuery.orderBy(criteriaBuilder.desc(root.get("id")));
        return list(context, criteriaQuery, false, PublicationRequest.class, limit, offset);
    }

    @Override
    public int countByStatus(Context context, int status) throws SQLException {
        Query query = createQuery(context, "SELECT count(*) FROM PublicationRequest WHERE status = :status");
        query.setParameter("status", status);
        return count(query);
    }
}

