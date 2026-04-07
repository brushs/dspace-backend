/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.translation2publication.dao.impl;

import java.sql.SQLException;
import java.util.List;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.translation2publication.Translation2Publication;
import org.dspace.translation2publication.Translation2PublicationId;
import org.dspace.translation2publication.dao.Translation2PublicationDAO;

/**
 * Hibernate implementation of the Database Access Object interface class for the Translation2Publication object.
 * This class is responsible for all database calls for the Translation2Publication object and is autowired by Spring.
 * This class should never be accessed directly.
 *
 * @author [Your Name]
 */
public class Translation2PublicationDAOImpl extends AbstractHibernateDAO<Translation2Publication>
    implements Translation2PublicationDAO {

    protected Translation2PublicationDAOImpl() {
        super();
    }

    @Override
    public Translation2Publication findByID(Context context, Translation2PublicationId id) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<Translation2Publication> criteriaQuery = getCriteriaQuery(criteriaBuilder,
            Translation2Publication.class);
        Root<Translation2Publication> root = criteriaQuery.from(Translation2Publication.class);
        criteriaQuery.select(root);
        criteriaQuery.where(
            criteriaBuilder.and(
                criteriaBuilder.equal(root.get("translationRequestId"), id.getTranslationRequestId()),
                criteriaBuilder.equal(root.get("publicationRequestId"), id.getPublicationRequestId())
            )
        );
        return singleResult(context, criteriaQuery);
    }

    @Override
    public List<Translation2Publication> findByTranslationRequestId(Context context, Integer translationRequestId)
        throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<Translation2Publication> criteriaQuery = getCriteriaQuery(criteriaBuilder,
            Translation2Publication.class);
        Root<Translation2Publication> root = criteriaQuery.from(Translation2Publication.class);
        criteriaQuery.select(root);
        criteriaQuery.where(criteriaBuilder.equal(root.get("translationRequestId"), translationRequestId));
        return list(context, criteriaQuery, false, Translation2Publication.class, -1, -1);
    }

    @Override
    public List<Translation2Publication> findByPublicationRequestId(Context context, Integer publicationRequestId)
        throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<Translation2Publication> criteriaQuery = getCriteriaQuery(criteriaBuilder,
            Translation2Publication.class);
        Root<Translation2Publication> root = criteriaQuery.from(Translation2Publication.class);
        criteriaQuery.select(root);
        criteriaQuery.where(criteriaBuilder.equal(root.get("publicationRequestId"), publicationRequestId));
        return list(context, criteriaQuery, false, Translation2Publication.class, -1, -1);
    }

    @Override
    public List<Translation2Publication> findAll(Context context, int offset, int limit) throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<Translation2Publication> criteriaQuery = getCriteriaQuery(criteriaBuilder,
            Translation2Publication.class);
        Root<Translation2Publication> root = criteriaQuery.from(Translation2Publication.class);
        criteriaQuery.select(root);
        criteriaQuery.orderBy(criteriaBuilder.asc(root.get("translationRequestId")));
        return list(context, criteriaQuery, false, Translation2Publication.class, limit, offset);
    }

    @Override
    public int countRows(Context context) throws SQLException {
        Query query = createQuery(context, "SELECT count(*) FROM Translation2Publication");
        return count(query);
    }
}

