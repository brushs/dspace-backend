package org.dspace.content;

import org.apache.commons.lang3.StringUtils;
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

    @Override
    public Vocabulary findByName(Context context, String name)
            throws IOException, SQLException {
        return vocabularyDAO.findByName(context, name);
    }

    @Override
    public List<Term> findByName(Context context, String termName, String vocabularyName)
            throws IOException, SQLException {

        Vocabulary vocabulary = null;
        if (!StringUtils.isEmpty(vocabularyName)) {
            vocabulary = vocabularyDAO.findByName(context, vocabularyName);
        }

        return termDAO.findByName(context, termName, vocabulary == null ? null : vocabulary.getId());
    }
}
