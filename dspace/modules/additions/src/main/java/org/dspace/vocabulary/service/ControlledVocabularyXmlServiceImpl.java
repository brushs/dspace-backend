package org.dspace.vocabulary.service;

import org.dspace.content.Term;
import org.dspace.content.Vocabulary;
import org.dspace.content.service.VocabularyService;
import org.dspace.core.Context;
import org.dspace.vocabulary.model.xml.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@Service
public class ControlledVocabularyXmlServiceImpl implements ControlledVocabularyXmlService {

    @Autowired
    private VocabularyService vocabularyService;

    public Node getVocabularyTree(Context context, int vocabularyId, List<String> languages)
            throws SQLException, IOException {

        Vocabulary vocabulary = vocabularyService.findById(context, vocabularyId);

        Node root = new Node();
        root.setId(vocabulary.getNameEn());
        root.setLabel(vocabulary.getNameEn());
        root.setIsComposedBy(new Node.IsComposedBy());

        List<Term> rootTerms = vocabularyService.getRootTerms(context, vocabularyId);

        for (Term t : rootTerms) {
            Node node = new Node();
            node.setId(String.valueOf(t.getId()));
            node.setLabel(languages.contains("en") ? t.getNameEn() : t.getNameFr());

            node.setIsComposedBy(getChildTree(context, node, languages));

            root.getIsComposedBy().getNode().add(node);

            if (languages.size() == 2) {
                Node bilingualNode = new Node();
                bilingualNode.setId(String.valueOf(t.getId() + "fr"));
                bilingualNode.setLabel(t.getNameFr());

                root.getIsComposedBy().getNode().add(bilingualNode);
            }
        }

        return root;
    }

    private Node.IsComposedBy getChildTree(Context context, Node parent, List<String> languages)
            throws SQLException, IOException {
        List<Term> childTerms = vocabularyService.getChildTerms(context, Integer.parseInt(parent.getId()));

        Node.IsComposedBy children = new Node.IsComposedBy();

        for (Term t : childTerms) {
            Node node = new Node();
            node.setId(String.valueOf(t.getId()));
            node.setLabel(languages.contains("en") ? t.getNameEn() : t.getNameFr());

            node.setIsComposedBy(getChildTree(context, node, languages));

            children.getNode().add(node);

            if (languages.size() == 2) {
                Node bilingualNode = new Node();
                bilingualNode.setId(String.valueOf(t.getId() + "fr"));
                bilingualNode.setLabel(t.getNameFr());

                children.getNode().add(bilingualNode);
            }
        }

        return children;
    }
}
