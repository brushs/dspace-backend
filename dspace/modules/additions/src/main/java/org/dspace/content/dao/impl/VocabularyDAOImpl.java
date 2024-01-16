/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao.impl;

import org.dspace.content.MetadataField;
import org.dspace.content.MetadataField_;
import org.dspace.content.MetadataValue;
import org.dspace.content.Vocabulary;
import org.dspace.content.dao.MetadataValueDAO;
import org.dspace.content.dao.VocabularyDAO;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;

import javax.persistence.Query;
import javax.persistence.criteria.*;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

public class VocabularyDAOImpl extends AbstractHibernateDAO<Vocabulary> implements VocabularyDAO {

    protected VocabularyDAOImpl() {
        super();
    }

    @Override
    public Vocabulary findByName(Context context, String name) throws SQLException {
        CriteriaBuilder cb = getCriteriaBuilder(context);
        CriteriaQuery cq = getCriteriaQuery(cb, Vocabulary.class);
        Root<Vocabulary> root = cq.from(Vocabulary.class);

        Predicate nameEn = cb.equal(cb.upper(root.get("nameEn")), name.toUpperCase());
        Predicate nameFr = cb.equal(cb.upper(root.get("nameFr")), name.toUpperCase());

        cq.select(root).where(cb.or(nameEn, nameFr));

        return uniqueResult(context, cq.select(root).where(cb.or(nameEn, nameFr)), true, Vocabulary.class);
    }

}
