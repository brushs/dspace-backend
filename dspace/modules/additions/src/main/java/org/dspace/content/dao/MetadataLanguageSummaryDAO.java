package org.dspace.content.dao;

import org.dspace.content.MetadataLanguageSummary;
import org.dspace.content.Term;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;

import java.sql.SQLException;
import java.util.List;

public interface MetadataLanguageSummaryDAO extends GenericDAO<MetadataLanguageSummary> {

    public List<MetadataLanguageSummary> getItemsToProcess(Context context, Integer limit) throws SQLException;

}
