package org.dspace.vocabulary.service;

import org.dspace.core.Context;
import org.dspace.vocabulary.model.xml.Node;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public interface ControlledVocabularyXmlService {

    Node getVocabularyTree(Context context, int vocabularyId, List<String> languages)
            throws SQLException, IOException;
}
