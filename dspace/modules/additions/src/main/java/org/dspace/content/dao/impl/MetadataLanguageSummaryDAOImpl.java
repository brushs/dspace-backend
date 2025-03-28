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

import javax.persistence.criteria.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

public class MetadataLanguageSummaryDAOImpl extends AbstractHibernateDAO<MetadataLanguageSummary>
        implements MetadataLanguageSummaryDAO {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(MetadataLanguageSummaryDAOImpl.class);

    protected MetadataLanguageSummaryDAOImpl() {
        super();
    }

    @Override
    public List<MetadataLanguageSummary> getItemsToProcessByType(Context context, Integer limit) throws SQLException {
        CriteriaBuilder cb = getCriteriaBuilder(context);
        CriteriaQuery cq = getCriteriaQuery(cb, MetadataLanguageSummary.class);
        Root<Term> root = cq.from(MetadataLanguageSummary.class);

        Predicate typeCountMismatch = cb.notEqual(root.get("typeCount"), root.get("typeEnCount"));

        return list(context, cq.select(root).where(typeCountMismatch).orderBy(cb.asc(root.get("lastModified"))), true, MetadataLanguageSummary.class, limit, 0);
    }

    @Override
    public List<MetadataLanguageSummary> getItemsToProcessNoMPD(Context context, Integer limit) throws SQLException {
        CriteriaBuilder cb = getCriteriaBuilder(context);
        CriteriaQuery cq = getCriteriaQuery(cb, MetadataLanguageSummary.class);
        Root<Term> root = cq.from(MetadataLanguageSummary.class);

        Predicate metadataProcessDateNull = cb.isNull(root.get("metadataProcessDate"));

        return list(context, cq.select(root).where(metadataProcessDateNull).orderBy(cb.asc(root.get("lastModified"))), true, MetadataLanguageSummary.class, limit, 0);
    }

    @Override
    public List<MetadataLanguageSummary> getItemsToProcessByMPD(Context context, Integer limit) throws SQLException {
        CriteriaBuilder cb = getCriteriaBuilder(context);
        CriteriaQuery cq = getCriteriaQuery(cb, MetadataLanguageSummary.class);
        Root<Term> root = cq.from(MetadataLanguageSummary.class);

        Expression<Date> lastModifiedMinusOneMinute = cb.function(
                "AGE",
                Date.class,
                root.get("lastModified"),
                cb.literal("1 minute")
        );

        //Predicate metadataProcessDateBeforeUpdateDate = cb.lessThan(root.get("metadataProcessDate"), root.get("lastModified"));
        Predicate metadataProcessDateBeforeUpdateDate = cb.lessThan(root.get("metadataProcessDate"), lastModifiedMinusOneMinute);

        return list(context, cq.select(root).where(metadataProcessDateBeforeUpdateDate).orderBy(cb.asc(root.get("lastModified"))), true, MetadataLanguageSummary.class, limit, 0);
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
