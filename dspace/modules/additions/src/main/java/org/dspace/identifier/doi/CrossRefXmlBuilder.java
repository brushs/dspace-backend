package org.dspace.identifier.doi;

import gov.nih.nlm.ncbi.jats1.Abstract;
import org.crossref.accessindicators.Program;
import org.crossref.accessindicators.Program.FreeToRead;
import org.crossref.accessindicators.Program.LicenseRef;
import org.crossref.schema._5_3.DoiBatch;
import org.crossref.schema._5_3.DoiBatch.Head;
import org.crossref.schema._5_3.DoiBatch.Head.Depositor;
import org.crossref.schema._5_3.DoiBatch.Body.ReportPaper;
import org.crossref.schema._5_3.DoiBatch.Body.ReportPaper.ReportPaperMetadata;
import org.crossref.schema._5_3.DoiBatch.Body.ReportPaper.ReportPaperMetadata.DoiData;
import org.crossref.schema._5_3.DoiBatch.Body.ReportPaper.ReportPaperMetadata.PublicationDate;
import org.crossref.schema._5_3.DoiBatch.Body.ReportPaper.ReportPaperMetadata.PublisherItem;
import org.crossref.schema._5_3.DoiBatch.Body.ReportPaper.ReportPaperMetadata.PublisherItem.ItemNumber;
import org.crossref.schema._5_3.DoiBatch.Body.ReportPaper.ReportPaperMetadata.Titles;
import org.crossref.schema._5_3.DoiBatch.Body.ReportPaper.ReportPaperMetadata.Contributors;
import org.crossref.schema._5_3.DoiBatch.Body.ReportPaper.ReportPaperMetadata.Contributors.PersonName;
import org.crossref.schema._5_3.DoiBatch.Body.ReportPaper.ReportPaperMetadata.Contributors.PersonName.ORCID;
import org.crossref.schema._5_3.DoiBatch.Body.ReportPaper.ReportPaperMetadata.Contributors.PersonName.Affiliations;
import org.crossref.schema._5_3.DoiBatch.Body.ReportPaper.ReportPaperMetadata.Contributors.PersonName.Affiliations.Institution;
import org.crossref.schema._5_3.DoiBatch.Body.ReportPaper.ReportPaperMetadata.Contributors.PersonName.Affiliations.Institution.InstitutionId;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.math.BigInteger;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.StringTokenizer;
import java.util.UUID;

@Component
public class CrossRefXmlBuilder {

    private static final Logger log = LoggerFactory.getLogger(CrossRefXmlBuilder.class);

    @Autowired(required = true)
    protected ItemService itemService;

    @Autowired(required = true)
    protected ConfigurationService configurationService;

    protected String version = null;
    protected String registrant = null;
    protected String depositorName = null;
    protected String depositorEmail = null;

    static final String CFG_VERSION = "crossref.version";
    static final String CFG_REGISTRANT = "crossref.registrant";
    static final String CFG_DEPOSITOR_NAME = "crossref.depositor.name";
    static final String CFG_DEPOSITOR_EMAIL = "crossref.depositor.email";

    static final String MDF_LANGUAGE = "dc.lang";
    static final String MDF_TITLE = "dc.title";
    static final String MDF_PUBLICATION_DATE = "dc.date.issued";
    static final String MDF_EDITION = "nrcan.edition";
    static final String MDF_DOI = "dc.identifier.doi";
    static final String MDF_RESOURCE = "dc.identifier.uri";
    static final String MDF_AUTHORS = "relation.isAuthorOfPublication";
    static final String MDF_SERIES_NAME = "nrcan.serial.title";
    static final String MDF_SERIES_NUMBER = "nrcan.volume";
    static final String MDF_EMPLOYEE_ID = "nrcan.identifier.employeeid";
    static final String MDF_ORCID = "person.identifier.orcid";
    static final String MDF_GIVEN_NAME = "person.givenName";
    static final String MDF_SURNAME = "person.familyName";

    protected String getVersion() {
        if (null == this.version) {
            this.version = this.configurationService.getProperty(CFG_VERSION);
            if (null == this.version) {
                throw new RuntimeException("Unable to load url from "
                        + "configuration. Cannot find property " +
                        CFG_VERSION + ".");
            }
        }
        return this.version;
    }

    protected String getRegistrant() {
        if (null == this.registrant) {
            this.registrant = this.configurationService.getProperty(CFG_REGISTRANT);
            if (null == this.registrant) {
                throw new RuntimeException("Unable to load url from "
                        + "configuration. Cannot find property " +
                        CFG_REGISTRANT + ".");
            }
        }
        return this.registrant;
    }

    protected String getDepositorName() {
        if (null == this.depositorName) {
            this.depositorName = this.configurationService.getProperty(CFG_DEPOSITOR_NAME);
            if (null == this.depositorName) {
                throw new RuntimeException("Unable to load url from "
                        + "configuration. Cannot find property " +
                        CFG_DEPOSITOR_NAME + ".");
            }
        }
        return this.depositorName;
    }

    protected String getDepositorEmail() {
        if (null == this.depositorEmail) {
            this.depositorEmail = this.configurationService.getProperty(CFG_DEPOSITOR_EMAIL);
            if (null == this.depositorEmail) {
                throw new RuntimeException("Unable to load url from "
                        + "configuration. Cannot find property " +
                        CFG_DEPOSITOR_EMAIL + ".");
            }
        }
        return this.depositorEmail;
    }

    public DoiBatch buildCrossRefSubmission(Context context, DSpaceObject dso, String doi) throws SQLException, DatatypeConfigurationException {

        if (!isValidForCrossRef(context, dso)) {
            log.info("Not valid for DOI");
            return null;
        }

        Item item = (Item) dso;
        DoiBatch batch = new DoiBatch();
        batch.setVersion(getVersion());
        batch.setHead(buildHead());
        batch.setBody(buildBody(context, item, doi));
        return batch;
    }

    private boolean isValidForCrossRef(Context context, DSpaceObject dso) throws SQLException {
        if (!(dso instanceof Item)) {
            log.info("Not an item");
            return false;
        } else if(itemService.getEntityType(context, (Item) dso).getLabel().equals("Publication")) {
            return true;
        } else {
            log.info("Not a Publication");
            return false;
        }
    }

    private Head buildHead() {
        Head head = new Head();

        String timeStamp = new SimpleDateFormat("yyyyMMddHHmm").format(new java.util.Date());

        head.setDoiBatchId(new BigInteger(timeStamp));
        head.setTimestamp(new BigInteger(timeStamp));
        head.setRegistrant(getRegistrant());

        Depositor depositor = new Depositor();
        depositor.setDepositorName(getDepositorName());
        depositor.setEmailAddress(getDepositorEmail());
        head.setDepositor(depositor);

        return head;
    }

    private DoiBatch.Body buildBody(Context context, Item item, String doi) throws DatatypeConfigurationException, SQLException {
        DoiBatch.Body body = new DoiBatch.Body();

        List<DoiBatch.Body.ReportPaper> reportPapers = body.getReportPaper();

        reportPapers.add(buildReportPaper(context, item, doi));

        return body;
    }

    private ReportPaper buildReportPaper(Context context, Item item, String doi) throws DatatypeConfigurationException, SQLException {
        ReportPaper reportPaper = new ReportPaper();

        reportPaper.setReportPaperMetadata(buildMetadata(context, item, doi));

        return reportPaper;
    }

    private ReportPaperMetadata buildMetadata(Context context, Item item, String doi) throws DatatypeConfigurationException, SQLException {
        ReportPaperMetadata metadata = new ReportPaperMetadata();

        List<MetadataValue> languages = itemService.getMetadataByMetadataString(item, MDF_LANGUAGE);
        if (languages.size() == 0) {
            metadata.setLanguage("en");
        } else {
            metadata.setLanguage(languages.get(0).getValue());
        }

        List<MetadataValue> editions = itemService.getMetadataByMetadataString(item, MDF_EDITION);
        if (editions.size() > 0) {
            metadata.setEditionNumber(editions.get(0).getValue());
        }

        Titles titles = new Titles();
        List<MetadataValue> mdvTitles = itemService.getMetadataByMetadataString(item, MDF_TITLE);
        if (mdvTitles.size() == 0) {
            throw new IllegalArgumentException("Title cannot be null");
        } else {
            titles.setTitle(mdvTitles.get(0).getValue());
        }
        metadata.setTitles(titles);

        PublicationDate publicationDate = new PublicationDate();
        List<MetadataValue> issueDates = itemService.getMetadataByMetadataString(item, MDF_PUBLICATION_DATE);
        if (issueDates.size() == 0) {
            throw new IllegalArgumentException("Publication date cannot be null");
        } else {
            String issueDate = issueDates.get(0).getValue();
            try {
                publicationDate.setYear(Integer.parseInt(issueDate.substring(0, 4)));
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException("Publication date format not valid");
            }
        }
        metadata.setPublicationDate(publicationDate);

        PublisherItem publisherItem = new PublisherItem();
        ItemNumber itemNumber = new ItemNumber();
        List<MetadataValue> serials = itemService.getMetadataByMetadataString(item, MDF_SERIES_NAME);
        if (serials != null && serials.size() > 0) {
            itemNumber.setItemNumberType(serials.get(0).getValue());
        }
        List<MetadataValue> serialNumbers = itemService.getMetadataByMetadataString(item, MDF_SERIES_NUMBER);
        if (serialNumbers != null && serialNumbers.size() > 0) {
            try {
                Integer itemNum = Integer.parseInt(serialNumbers.get(0).getValue());
                if (itemNum > 0) {
                    itemNumber.setValue(itemNum);
                }

            } catch (NumberFormatException nfe) {
                log.info("UUID: " + item.getID() + " Serial Number is not a number, ignoring");
            }
        }
        publisherItem.setItemNumber(itemNumber);
        metadata.setPublisherItem(publisherItem);

        DoiData doiData = new DoiData();
        doiData.setDoi(doi);
        doiData.setResource(itemService.getMetadataByMetadataString(item, MDF_RESOURCE).get(0).getValue());
        metadata.setDoiData(doiData);

        List<Abstract> abstracts = metadata.getAbstract();
        Abstract abstractEn = new Abstract();
        Abstract.P abstractText = new Abstract.P();
        abstractText.setLang("en");
        abstractText.setValue(itemService.getMetadataFirstValue(item, "dc", "description","abstract", "en"));
        abstractEn.setP(abstractText);
        abstracts.add(abstractEn);

        Abstract abstractFr = new Abstract();
        abstractText = new Abstract.P();
        abstractText.setLang("fr");
        abstractText.setValue(itemService.getMetadataFirstValue(item, "dc", "description","abstract", "fr"));
        abstractEn.setP(abstractText);
        abstracts.add(abstractFr);

        metadata.setProgram(buildProgram(context, item));
        metadata.setContributors(buildContributors(context, item));

        return metadata;
    }

    private Program buildProgram(Context context, Item item) throws DatatypeConfigurationException {
        Program program = new Program();
        program.setName("AccessIndicators");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd");

        String dateString = getFirstMetadataValueByMetadataString(item, MDF_PUBLICATION_DATE, null);

        FreeToRead freeToRead = new FreeToRead();
        freeToRead.setStartDate(DatatypeFactory.newInstance().newXMLGregorianCalendar(
                LocalDate.parse(dateString, formatter).toString()));
        program.setFreeToRead(freeToRead);

        LicenseRef licenseRef = new LicenseRef();
        licenseRef.setAppliesTo("vor");
        licenseRef.setStartDate(DatatypeFactory.newInstance().newXMLGregorianCalendar(
                LocalDate.parse(dateString, formatter).toString()));
        licenseRef.setValue("https://open.canada.ca/en/open-government-licence-canada");
        program.setLicenseRef(licenseRef);

        return program;
    }

    private Contributors buildContributors(Context context, Item item) throws SQLException {
        // Need to handle Orgs and Persons here
        Contributors contributors = new Contributors();

        List<PersonName> authors = contributors.getPersonName();

        List<MetadataValue> mdvAuthors = itemService.getMetadataByMetadataString(item, MDF_AUTHORS);

        for (MetadataValue author : mdvAuthors) {
            authors.add(buildAuthor(context, author));
        }

        return contributors;
    }

    private PersonName buildAuthor(Context context, MetadataValue mdv) throws SQLException {
        PersonName author = new PersonName();

        Item authorEntity = itemService.find(context, UUID.fromString(mdv.getValue()));

        if (mdv.getPlace() == 0) {
            author.setSequence("first");
        } else {
            author.setSequence("additional");
        }

        author.setContributorRole("author");
        author.setGivenName(getFirstMetadataValueByMetadataString(authorEntity, MDF_GIVEN_NAME, null));
        author.setSurname(getFirstMetadataValueByMetadataString(authorEntity, MDF_SURNAME, null));

        // Set affiliation to NRCan for NRCan employees
        if (getFirstMetadataValueByMetadataString(authorEntity, MDF_EMPLOYEE_ID, null) != null) {
            author.setAffiliations(buildAffiliations());
        }

        if (getFirstMetadataValueByMetadataString(authorEntity, MDF_ORCID, null) != null) {
            ORCID orcid = new ORCID();
            orcid.setValue("https://orcid.org/" + getFirstMetadataValueByMetadataString(authorEntity, MDF_ORCID, null));
            author.setORCID(orcid);
        }


        return author;
    }

    private Affiliations  buildAffiliations() {
        Affiliations affiliations = new Affiliations();

        Institution institution = new Institution();

        institution.setInstitutionName("Natural Resources Canada");

        InstitutionId institutionId = new InstitutionId();

        institutionId.setType("ror");
        institutionId.setValue("https://ror.org/05hepy730");

        institution.setInstitutionId(institutionId);

        affiliations.setInstitution(institution);

        return affiliations;
    }

    public String getFirstMetadataValueByMetadataString(Item item, String mdString, String language) {
        StringTokenizer dcf = new StringTokenizer(mdString, ".");

        String[] tokens = {"", "", ""};
        int i = 0;
        while (dcf.hasMoreTokens()) {
            tokens[i] = dcf.nextToken().trim();
            i++;
        }
        String schema = tokens[0];
        String element = tokens[1];
        String qualifier = tokens[2];

        String value = itemService.getMetadataFirstValue(item, schema, element, qualifier, language);
        if (value == null) {
            value = itemService.getMetadataFirstValue(item, schema, element, qualifier, language == null ? "" : null);
        }

        return value;
    }
}
