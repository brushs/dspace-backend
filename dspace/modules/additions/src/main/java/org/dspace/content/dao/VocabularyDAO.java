package org.dspace.content.dao;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.Vocabulary;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;

public interface VocabularyDAO extends GenericDAO<Vocabulary> {

    public Vocabulary findByName(Context context, String name) throws SQLException;

}
