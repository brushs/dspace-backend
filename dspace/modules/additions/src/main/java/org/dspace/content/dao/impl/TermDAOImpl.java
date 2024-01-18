/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao.impl;

import org.dspace.content.Term;
import org.dspace.content.dao.TermDAO;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.sql.SQLException;
import java.util.List;

public class TermDAOImpl extends AbstractHibernateDAO<Term> implements TermDAO {

    protected TermDAOImpl() {
        super();
    }

    @Override
    public List<Term> findByName(Context context, String name, Integer vocabularyId) throws SQLException {
        CriteriaBuilder cb = getCriteriaBuilder(context);
        CriteriaQuery cq = getCriteriaQuery(cb, Term.class);
        Root<Term> root = cq.from(Term.class);

        Predicate nameEn = cb.equal(cb.upper(root.get("nameEn")), name.toUpperCase());
        Predicate nameFr = cb.equal(cb.upper(root.get("nameFr")), name.toUpperCase());

        if (vocabularyId != null) {
            Predicate vocabulary = cb.equal(root.get("vocabularyId"), vocabularyId);
            return list(context, cq.select(root).where(cb.and(cb.or(nameEn, nameFr), vocabulary)), true, Term.class, -1, 0);
        }

        return list(context, cq.select(root).where(cb.or(nameEn, nameFr)), true, Term.class, -1, 0);
    }

    @Override
    public List<Term> getRootTerms(Context context, int vocabularyId) throws SQLException {
        CriteriaBuilder cb = getCriteriaBuilder(context);
        CriteriaQuery cq = getCriteriaQuery(cb, Term.class);
        Root<Term> root = cq.from(Term.class);

        Predicate nullParent = cb.isNull(root.get("parentTermId"));
        Predicate vocabId = cb.equal(root.get("vocabularyId"), vocabularyId);

        return list(context, cq.select(root).where(cb.and(nullParent, vocabId)), true, Term.class, -1, 0);
    }

    @Override
    public List<Term> getChildTerms(Context context, int termId) throws SQLException {
        CriteriaBuilder cb = getCriteriaBuilder(context);
        CriteriaQuery cq = getCriteriaQuery(cb, Term.class);
        Root<Term> root = cq.from(Term.class);

        Predicate children = cb.equal(root.get("parentTermId"), termId);

        return list(context, cq.select(root).where(children), true, Term.class, -1, 0);
    }

}
