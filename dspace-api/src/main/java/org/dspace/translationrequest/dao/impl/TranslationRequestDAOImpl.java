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
}

