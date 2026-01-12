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
    public List<PublicationRequest> findByPublicationGUID(Context context, String publicationGUID)
        throws SQLException {
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery<PublicationRequest> criteriaQuery = getCriteriaQuery(criteriaBuilder, PublicationRequest.class);
        Root<PublicationRequest> root = criteriaQuery.from(PublicationRequest.class);
        criteriaQuery.select(root);
        criteriaQuery.where(criteriaBuilder.equal(root.get("publicationGUID"), publicationGUID));
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
}

