package org.dspace.content.dao;

import org.dspace.content.Term;
import org.dspace.content.Vocabulary;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;

import java.sql.SQLException;
import java.util.List;

public interface TermDAO extends GenericDAO<Term> {

    public List<Term> findByName(Context context, String name, Integer vocabularyId) throws SQLException;

    List<Term> getRootTerms(Context context, int vocabularyId) throws SQLException;

    List<Term> getChildTerms(Context context, int termId) throws SQLException;
}
