package org.dspace.content;

import org.apache.solr.common.StringUtils;
import org.dspace.content.service.CitationService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class CitationServiceImpl implements CitationService {

    @Autowired
    private MetadataFieldService metadataFieldService;

    private static MetadataSchema mds = null;
    private static MetadataField mdf = null;

    private static String TYPE_ARTICLE = "Article";
    private static String TYPE_BOOK = "Book";
    private static String TYPE_BOOK_CHAPTER = "Book Chapter";
    private static String TYPE_REPORT = "Report";
    private static String TYPE_MAP = "Map";
    private static String TYPE_CONFERENCE_MATERIAL = "Conference Material";
    private static String TYPE_ABSTRACT = "Abstract";
    private static String TYPE_WEB_RESOURCE = "Web Resource";
    private static String TYPE_THESIS = "Thesis";

    private static String ENTITY_TYPE_PUBLICATION = "Publication";

    private static String FIELD_TYPE = "dc.type_en";
    private static String FIELD_AUTHORS = "dc.contributor.author";
    private static String FIELD_DATE_ISSUED = "dc.date.issued";
    private static String FIELD_TITLE = "dc.title";
    private static String FIELD_JOURNAL = "nrcan.journal.title";
    private static String FIELD_VOLUME = "nrcan.volume";
    private static String FIELD_ISSUE = "nrcan.issue";
    private static String FIELD_ARTICLE_NUMBER = "nrcan.articlenumber";
    private static String FIELD_PAGINATION = "nrcan.pagination.pagerange";
    private static String FIELD_DOI = "dc.identifier.doi";
    private static String FIELD_ENTITY_TYPE = "dspace.entity.type";
    private static String FIELD_EDITION = "nrcan.edition";
    private static String FIELD_SERIAL = "nrcan.serial.title";
    private static String FIELD_REPORT_NUMBER = "nrcan.reportnumber";
    private static String FIELD_MONOGRAPH = "nrcan.monographic.title";
    private static String FIELD_EDITOR = "nrcan.contributor.monographicauthor";

    private static final String DOI_PREFIX = "https://doi.org/";

    protected CitationServiceImpl() {
        if (mds == null) {
            mds = new MetadataSchema();
            mds.setName("dc");
        }

        if (mdf == null) {
            mdf = new MetadataField();
            mdf.setMetadataSchema(mds);
            mdf.setElement("identifier");
            mdf.setQualifier("citation");
        }

    }

    @Override
    public MetadataValue getCitation(List<MetadataValue> mdvs) {
        if (!isValid(mdvs)) {
            return null;
        }

        MetadataValue mdv = new MetadataValue();

        mdv.setMetadataField(mdf);

        List<MetadataValue> typeList = getFilteredList(mdvs, FIELD_TYPE);
        if (typeList == null || typeList.size() == 0) {
            return null;
        }

        String type = typeList.get(0).getValue();
        if (StringUtils.isEmpty((type))) {
            return null;
        }

        if (type.contentEquals(TYPE_ARTICLE)) {
            mdv.setValue(getArticleCitation(mdvs));
        } else if (type.contentEquals(TYPE_REPORT)) {
            mdv.setValue(getReportCitation(mdvs));
        } else if (type.contentEquals(TYPE_BOOK)) {
            mdv.setValue(getReportCitation(mdvs));
        } else if (type.contentEquals(TYPE_MAP)) {
            mdv.setValue(getReportCitation(mdvs));
        } else if (type.contentEquals(TYPE_BOOK_CHAPTER)) {
            mdv.setValue(getChapterCitation(mdvs));
        } else if (type.contentEquals(TYPE_THESIS)) {
            mdv.setValue(getReportCitation(mdvs));
        } else if (type.contentEquals(TYPE_ABSTRACT)) {
            mdv.setValue(getChapterCitation(mdvs));
        } else if (type.contentEquals(TYPE_CONFERENCE_MATERIAL)) {
            mdv.setValue(getChapterCitation(mdvs));
        } else if (type.contentEquals(TYPE_WEB_RESOURCE)) {
            mdv.setValue(getReportCitation(mdvs));
        } else {
            return null;
        }

        return mdv;
    }

    private boolean isValid(List<MetadataValue> mdvs) {
        String entityType = getField(mdvs, FIELD_ENTITY_TYPE, "");

        if (ENTITY_TYPE_PUBLICATION.contentEquals(entityType.trim())) {
            return true;
        }

        return false;
    }

    private String getArticleCitation(List<MetadataValue> mdvs) {

        StringBuilder sb = new StringBuilder();
        sb.append(getAuthors(mdvs));
        sb.append(getYear(mdvs));
        sb.append(getTitle(mdvs));
        sb.append(getJournalName(mdvs));
        sb.append(getField(mdvs, FIELD_VOLUME, ","));
        sb.append(getField(mdvs, FIELD_ISSUE, ","));
        sb.append(getField(mdvs, FIELD_ARTICLE_NUMBER, ","));
        sb.append(getField(mdvs, FIELD_PAGINATION, "."));
        sb.append(getDOI(mdvs));

        return sb.toString();
    }

    private String getBookCitation(List<MetadataValue> mdvs) {
        return "test2";
    }

    private String getChapterCitation(List<MetadataValue> mdvs) {

        StringBuilder sb = new StringBuilder();
        sb.append(getAuthors(mdvs));
        sb.append(getYear(mdvs));
        sb.append(getTitle(mdvs));
        sb.append(getTitleLanguage(mdvs).equals("en") ? "In " : "Dans ");
        sb.append(getEditor(mdvs));
        sb.append(getMonographicName(mdvs));
        sb.append(getField(mdvs, FIELD_EDITION, ","));
        sb.append(getSerialName(mdvs));
        sb.append(getField(mdvs, FIELD_REPORT_NUMBER, ","));
        sb.append(getField(mdvs, FIELD_PAGINATION, "."));
        sb.append(getDOI(mdvs));

        return sb.toString();
    }

    private String getReportCitation(List<MetadataValue> mdvs) {

        StringBuilder sb = new StringBuilder();
        sb.append(getAuthors(mdvs));
        sb.append(getYear(mdvs));
        sb.append(getTitle(mdvs));
        sb.append(getField(mdvs, FIELD_EDITION, ","));
        sb.append(getSerialName(mdvs));
        sb.append(getField(mdvs, FIELD_REPORT_NUMBER, ","));
        sb.append(getField(mdvs, FIELD_PAGINATION, "."));
        sb.append(getDOI(mdvs));

        return sb.toString();
    }

    private String getAuthors(List<MetadataValue> mdvs) {

        List<MetadataValue> fmdvs = getFilteredList(mdvs, FIELD_AUTHORS);

        if (fmdvs.size() == 0) {
            return "";
        } else if (fmdvs.size() == 1) {
            return fmdvs.get(0).getValue() + " ";
        } else {
            String authorsPrefix = fmdvs.subList(0, fmdvs.size() - 1).stream()
                    .map(MetadataValue::getValue)
                    .collect(Collectors.joining(", "));

            return authorsPrefix + " & " + fmdvs.get(fmdvs.size() -1).getValue() + " ";
        }
    }

    private String getYear(List<MetadataValue> mdvs) {
        List<MetadataValue> fmdvs = getFilteredList(mdvs, FIELD_DATE_ISSUED);
        if (fmdvs == null || fmdvs.size() == 0) {
            return "";
        } else {
            return "(" + fmdvs.get(0).getValue().substring(0, 4) + "). ";
        }
    }

    private String getTitle(List<MetadataValue> mdvs) {
        List<MetadataValue> fmdvs = getFilteredList(mdvs, FIELD_TITLE);
        if (fmdvs == null || fmdvs.size() == 0) {
            return "";
        } else {
            return fmdvs.get(0).getValue() + ". ";
        }
    }

    private String getTitleLanguage(List<MetadataValue> mdvs) {
        List<MetadataValue> fmdvs = getFilteredList(mdvs, FIELD_TITLE);
        if (fmdvs == null || fmdvs.size() == 0) {
            return "";
        } else {
            return fmdvs.get(0).getLanguage();
        }
    }

    private String getJournalName(List<MetadataValue> mdvs) {
        List<MetadataValue> fmdvs = getFilteredList(mdvs, FIELD_JOURNAL);
        if (fmdvs == null || fmdvs.size() == 0) {
            return "";
        } else {
            return "<i>" + fmdvs.get(0).getValue() + "</i>, ";
        }
    }

    private String getSerialName(List<MetadataValue> mdvs) {
        List<MetadataValue> fmdvs = getFilteredList(mdvs, FIELD_SERIAL);
        if (fmdvs == null || fmdvs.size() == 0) {
            return "";
        } else {
            return "<i>" + fmdvs.get(0).getValue() + "</i>, ";
        }
    }

    private String getMonographicName(List<MetadataValue> mdvs) {
        List<MetadataValue> fmdvs = getFilteredList(mdvs, FIELD_MONOGRAPH);
        if (fmdvs == null || fmdvs.size() == 0) {
            return "";
        } else {
            return fmdvs.get(0).getValue();
        }
    }

    private String getEditor(List<MetadataValue> mdvs) {
        List<MetadataValue> fmdvs = getFilteredList(mdvs, FIELD_EDITOR);
        if (fmdvs == null || fmdvs.size() == 0) {
            return "";
        } else {
            int indexOfComma = fmdvs.get(0).getValue().indexOf(",");
            return fmdvs.get(0).getValue().substring(indexOfComma + 2) + " " +
                    fmdvs.get(0).getValue().substring(0, indexOfComma - 1) + ", ";
        }
    }

    private String getDOI(List<MetadataValue> mdvs) {
        List<MetadataValue> fmdvs = getFilteredList(mdvs, FIELD_DOI);
        if (fmdvs == null || fmdvs.size() == 0) {
            return "";
        } else {
            return DOI_PREFIX + fmdvs.get(0).getValue();
        }
    }

    private String getField(List<MetadataValue> mdvs, String fieldName, String separator) {
        List<MetadataValue> fmdvs = getFilteredList(mdvs, fieldName);
        if (fmdvs == null || fmdvs.size() == 0) {
            return "";
        } else {
            return fmdvs.get(0).getValue() + separator + " ";
        }
    }

    private List<MetadataValue> getFilteredList(List<MetadataValue> mdvs, String field) {

        String[] splittedKey = field.split("\\.");
        String schema = splittedKey.length > 0 ? splittedKey[0] : null;
        String element = splittedKey.length > 1 ? splittedKey[1] : null;
        String qualifier = splittedKey.length > 2 ? splittedKey[2] : null;

        List<MetadataValue> filteredMdvs = null;

        if (qualifier != null) {
            filteredMdvs =
                    mdvs.stream().filter(m -> m.getMetadataField().getMetadataSchema().getName().equals(schema)
                                    && m.getMetadataField().getElement().equals(element)
                                    && m.getMetadataField().getQualifier().equals(qualifier))
                            .collect(Collectors.toList());
        } else {
            filteredMdvs =
                    mdvs.stream().filter(m -> m.getMetadataField().getMetadataSchema().getName().equals(schema)
                                    && m.getMetadataField().getElement().equals(element)
                                    && StringUtils.isEmpty(m.getMetadataField().getQualifier()))
                            .collect(Collectors.toList());
        }


        return filteredMdvs;
    }
}
