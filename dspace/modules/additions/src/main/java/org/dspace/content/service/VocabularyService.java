package org.dspace.content.service;

import org.dspace.content.MetadataLanguageSummary;
import org.dspace.content.Term;
import org.dspace.content.Vocabulary;
import org.dspace.core.Context;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public interface VocabularyService {

    Vocabulary findByName(Context context, String name)
            throws IOException, SQLException;

    Vocabulary findById(Context context, int vocabularyId)
            throws IOException, SQLException;

    List<Term> findByName(Context context, String termName, Integer vocabularyId)
            throws IOException, SQLException;

    List<Term> getRootTerms(Context context, int vocabularyId)
            throws IOException, SQLException;

    List<Term> getChildTerms(Context context, int termId)
            throws IOException, SQLException;

    List<MetadataLanguageSummary> getItemsForMetadataProcessing(Context context, int limit)
            throws IOException, SQLException;

}
