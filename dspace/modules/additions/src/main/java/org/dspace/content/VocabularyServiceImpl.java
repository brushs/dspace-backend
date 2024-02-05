package org.dspace.content;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.dao.MetadataLanguageSummaryDAO;
import org.dspace.content.dao.TermDAO;
import org.dspace.content.dao.VocabularyDAO;
import org.dspace.content.service.VocabularyService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class VocabularyServiceImpl implements VocabularyService {

    @Autowired(required = true)
    protected VocabularyDAO vocabularyDAO;

    @Autowired(required = true)
    protected TermDAO termDAO;

    @Autowired(required = true)
    protected MetadataLanguageSummaryDAO metadataLanguageSummaryDAO;

    @Override
    public Vocabulary findByName(Context context, String name)
            throws IOException, SQLException {

        return vocabularyDAO.findByName(context, name);
    }

    @Override
    public Vocabulary findById(Context context, int vocabularyId)
            throws IOException, SQLException {

        return vocabularyDAO.findByID(context, Vocabulary.class, vocabularyId);
    }

    @Override
    public List<Term> findByName(Context context, String termName, Integer vocabularyId)
            throws IOException, SQLException {

        return termDAO.findByName(context, termName, vocabularyId);
    }

    @Override
    public List<Term> getRootTerms(Context context, int vocabularyId) throws IOException, SQLException {

        return termDAO.getRootTerms(context, vocabularyId);
    }

    @Override
    public List<Term> getChildTerms(Context context, int termId) throws IOException, SQLException {

        return termDAO.getChildTerms(context, termId);
    }

    @Override
    public List<MetadataLanguageSummary> getItemsForMetadataProcessing(Context context, int limit) throws IOException, SQLException {
        return metadataLanguageSummaryDAO.getItemsToProcess(context, limit);
    }

}
