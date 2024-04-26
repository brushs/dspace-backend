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
import org.hibernate.Transaction;
import org.hibernate.jdbc.Work;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.sql.Connection;
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

            Transaction transaction = session.getTransaction();

            SessionAutoCommitDisabler autoCommitDisabler = new SessionAutoCommitDisabler();
            autoCommitDisabler.disableAutoCommitForSession(session);

            session.createNativeQuery("REFRESH MATERIALIZED VIEW CONCURRENTLY metadata_language_summary_mv")
                    .executeUpdate();

            transaction.commit();

            log.info("Committed MV Refresh");
        } catch (Throwable t) {
            log.error("Error", t);
        }
    }

    public class SessionAutoCommitDisabler {

        public void disableAutoCommitForSession(Session session) {
            // Disable auto-commit for the underlying JDBC connection
            session.doWork(new Work() {
                @Override
                public void execute(Connection connection) throws SQLException {
                    connection.setAutoCommit(false);
                }
            });
        }
    }
}
