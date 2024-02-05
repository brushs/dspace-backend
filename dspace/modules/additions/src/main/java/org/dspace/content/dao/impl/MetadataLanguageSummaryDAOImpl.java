/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao.impl;

import org.dspace.content.MetadataLanguageSummary;
import org.dspace.content.Term;
import org.dspace.content.dao.MetadataLanguageSummaryDAO;
import org.dspace.content.dao.TermDAO;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.sql.SQLException;
import java.util.List;

public class MetadataLanguageSummaryDAOImpl extends AbstractHibernateDAO<MetadataLanguageSummary>
        implements MetadataLanguageSummaryDAO {

    protected MetadataLanguageSummaryDAOImpl() {
        super();
    }

    @Override
    public List<MetadataLanguageSummary> getItemsToProcess(Context context, Integer limit) throws SQLException {
        CriteriaBuilder cb = getCriteriaBuilder(context);
        CriteriaQuery cq = getCriteriaQuery(cb, MetadataLanguageSummary.class);
        Root<Term> root = cq.from(MetadataLanguageSummary.class);

        Predicate typeCountMismatch = cb.notEqual(root.get("typeCount"), root.get("typeEnCount"));
        Predicate subjectCountMismatch = cb.notEqual(root.get("subjectCount"), root.get("subjectEnCount"));

        return list(context, cq.select(root).where(cb.or(typeCountMismatch, subjectCountMismatch)).orderBy(cb.asc(root.get("lastModified"))), true, MetadataLanguageSummary.class, limit, 0);
    }
}
