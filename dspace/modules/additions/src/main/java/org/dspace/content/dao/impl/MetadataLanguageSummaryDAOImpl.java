/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao.impl;

import org.apache.logging.log4j.Logger;
import org.dspace.content.MetadataLanguageSummary;
import org.dspace.content.Term;
import org.dspace.content.dao.MetadataLanguageSummaryDAO;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.sql.SQLException;
import java.util.List;

public class MetadataLanguageSummaryDAOImpl extends AbstractHibernateDAO<MetadataLanguageSummary>
        implements MetadataLanguageSummaryDAO {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(MetadataLanguageSummaryDAOImpl.class);

    protected MetadataLanguageSummaryDAOImpl() {
        super();
    }

    @Override
    public List<MetadataLanguageSummary> getItemsToProcess(Context context, Integer limit) throws SQLException {
        CriteriaBuilder cb = getCriteriaBuilder(context);
        CriteriaQuery cq = getCriteriaQuery(cb, MetadataLanguageSummary.class);
        Root<Term> root = cq.from(MetadataLanguageSummary.class);

        Predicate typeCountMismatch = cb.notEqual(root.get("typeCount"), root.get("typeEnCount"));
        Predicate subjectCountMismatch = cb.notEqual(root.get("subjectRawCount"), root.get("subjectCuratedCount"));

        return list(context, cq.select(root).where(cb.or(typeCountMismatch, subjectCountMismatch)).orderBy(cb.asc(root.get("lastModified"))), true, MetadataLanguageSummary.class, limit, 0);
    }

    @Override
    public void refreshMaterializedView(Context context) {
        try (Session session = getHibernateSession(context)) {
            session.doWork(new Work() {
                @Override
                public void execute(java.sql.Connection connection) throws java.sql.SQLException {
                    // Execute a native SQL query to refresh the materialized view
                    try (java.sql.Statement statement = connection.createStatement()) {
                        String sql = "REFRESH MATERIALIZED VIEW " + "metadata_language_summary_mv";
                        log.info("Refreshing MV");
                        statement.execute(sql);
                        log.info("Refreshed MV");
                    }
                }
            });
        } catch (Exception e) {
            log.error("Error", e);
        }


    }
}
