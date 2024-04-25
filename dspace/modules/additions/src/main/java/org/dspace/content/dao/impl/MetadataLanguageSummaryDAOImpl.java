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
import org.hibernate.procedure.ProcedureCall;

import javax.persistence.ParameterMode;
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

            session.createNativeQuery("CALL refresh_materialized_view()")
                    .executeUpdate();

            // Create a ProcedureCall for the stored procedure
            //ProcedureCall procedureCall = session.createStoredProcedureCall("refresh_materialized_view");

            // Bind the parameter value
            //procedureCall.registerParameter("mv_name", String.class, ParameterMode.IN).bindValue("metadata_language_summary_mv");

            // Execute the stored procedure
            //procedureCall.execute();

            /*
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
            */

            log.info("Committed");
        } catch (Exception e) {
            log.error("Error", e);
        }


    }
}
