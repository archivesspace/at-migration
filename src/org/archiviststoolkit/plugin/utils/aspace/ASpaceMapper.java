package org.archiviststoolkit.plugin.utils.aspace;

import bsh.Interpreter;
import org.archiviststoolkit.model.*;
import org.archiviststoolkit.mydomain.DomainObject;
import org.archiviststoolkit.plugin.utils.RandomString;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * @author nathan
 * Updated by Sarah Morrissey 12/2017
 * @version 2.2
 *
 * Class to map AT data model to ASPace JSON data model
 */
public class ASpaceMapper {
    // String used when mapping AT access class to groups
    public static final String ACCESS_CLASS_PREFIX = "_AccessClass_";

    //default date for use when no other date is available
    public static final Date DEFAULT_DATE = new Date(0, 0, 1);

    // The utility class used to map to ASpace Enums
    private ASpaceEnumUtil enumUtil = new ASpaceEnumUtil();

    // used to map AT vocabularies to ASpace vocabularies
    public String vocabularyURI = "/vocabularies/1";

    // the bean shell interpreter for doing mapping using scripts
    private Interpreter bsi = null;

    // the script mapper script
    private String mapperScript = null;

    // these store the ids of all accessions, resources, and digital objects loaded so we can
    // check for uniqueness before copying them to the ASpace backend
    private HashSet<String> digitalObjectIDs = new HashSet<String>();
    private HashSet<String> accessionIDs = new HashSet<String>();
    private HashSet<String> resourceIDs = new HashSet<String>();
    private HashSet<String> eadIDs = new HashSet<String>();

    /**
     * sets the IDs that are already in use
     * @param iDs
     */
    public void setIDs(HashMap<String, HashSet<String>> iDs) {
        digitalObjectIDs = iDs.get("digital object");
        accessionIDs = iDs.get("accession");
        resourceIDs = iDs.get("resource");
        eadIDs = iDs.get("ead");
    }

    /**
     * gets the IDs that are already in use
     * @return a hash map where the keys are the types of IDs
     */
    public HashMap<String, HashSet<String>> getIDs() {
        HashMap<String, HashSet<String>> iDs = new HashMap<String, HashSet<String>>();
        iDs.put("digital object", digitalObjectIDs);
        iDs.put("accession", accessionIDs);
        iDs.put("resource", resourceIDs);
        iDs.put("ead", eadIDs);
        return iDs;
    }

    // variable names in bean shell script that will indicate whether it can override
    // the default mapping operation with itself
    private static final String SUBJECT_MAPPER = "@subject";
    private static final String NAME_MAPPER = "@name";
    private static final String REPOSITORY_MAPPER = "@repository";
    private static final String LOCATION_MAPPER = "@location";
    private static final String USER_MAPPER = "@user";
    private static final String ACCESSION_MAPPER = "@accession";
    private static final String RESOURCE_MAPPER = "@resource";
    private static final String COMPONENT_MAPPER = "@component";
    private static final String DIGITAL_OBJECT_MAPPER = "@digitalobject";
    private static final String NOTE_MAPPER = "@note";

    // boolean variables to see if to use a mapper script
    public boolean runSubjectMapperScript = false;
    public boolean runNameMapperScript = false;
    public boolean runRepositoryMapperScript = false;
    public boolean runLocationMapperScript = false;
    public boolean runUserMapperScript = false;
    public boolean runAccessionMapperScript = false;
    public boolean runResourceMapperScript = false;
    public boolean runComponentMapperScript = false;
    public boolean runDigitalObjectMapperScript = false;
    public boolean runNoteMapperScript = false;

    // Boolean hash which specify which records which should have publish = true or false
    private HashMap<String, Boolean> publishHashMap;

    // some code used for testing
    private boolean makeUnique = false;
    private boolean allowTruncation = false;

    // used to specify that extent should be all parts if more than 1
    private boolean extentPortionInParts = true;

    // initialize the random string generators for use when unique ids are needed
    private RandomString randomString = new RandomString(3);
    private RandomString randomStringLong = new RandomString(6);

    // used when specifying the external ids
    private String connectionUrl = "";

    // used to store errors
    private ASpaceCopyUtil aspaceCopyUtil;

    // used when generating errors
    private String currentResourceRecordIdentifier;

    // used for date comparison of ISO dates
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    // string builder used for finding bad dates
    private ArrayList<String> datesList = null;
    private boolean checkISODates = false;
    private JSONArray unknownName;

    /**
     *  Main constructor
     */
    public ASpaceMapper() { }

    /**
     * Constructor that takes an aspace copy util object
     * @param aspaceCopyUtil
     */
    public ASpaceMapper(ASpaceCopyUtil aspaceCopyUtil) {
        this.aspaceCopyUtil = aspaceCopyUtil;
    }

    /**
     * Method to set the hash map used for testing when records should be published or not
     * @param publishHashMap
     */
    public void setPublishHashMap(HashMap<String, Boolean> publishHashMap) {
        this.publishHashMap = publishHashMap;
    }

    /**
     * Method to see the bean shell script to override the default mapping action
     *
     * @param script
     */
    public String setMapperScript(String script) {
        // see what mapping functionality the script supports
        if(script.contains(SUBJECT_MAPPER)) {
            runSubjectMapperScript = true;
        }

        if(script.contains(NAME_MAPPER)) {
            runNameMapperScript = true;
        }

        if(script.contains(REPOSITORY_MAPPER)) {
            runRepositoryMapperScript = true;
        }

        if(script.contains(USER_MAPPER)) {
            runUserMapperScript = true;
        }

        if(script.contains(LOCATION_MAPPER)) {
            runLocationMapperScript = true;
        }

        if(script.contains(ACCESSION_MAPPER)) {
            runAccessionMapperScript = true;
        }

        if(script.contains(RESOURCE_MAPPER)) {
            runResourceMapperScript = true;
        }

        if(script.contains(COMPONENT_MAPPER)) {
            runComponentMapperScript = true;
        }

        if(script.contains(DIGITAL_OBJECT_MAPPER)) {
            runDigitalObjectMapperScript = true;
        }

        if(script.contains(NOTE_MAPPER)) {
            runNoteMapperScript = true;
        }

        // initialize the bean shell mapper
        bsi = new Interpreter();
        mapperScript = script;

        return "mapper script set ...";
    }

    /**
     * Method to copy an AT record to ASpace record
     *
     * @param record
     * @return
     * @throws Exception
     */
    public Object convert(DomainObject record) throws Exception {
        if (record instanceof Subjects) {
            return convertSubject((Subjects) record);
        } else if (record instanceof Names) {
            return convertName((Names) record);
        } else if (record instanceof Repositories) {
            return convertRepository((Repositories) record);
        } else if (record instanceof Locations) {
            return convertLocation((Locations) record);
        } else if (record instanceof Users) {
            return convertUser((Users) record);
        } else if (record instanceof Accessions) {
            return convertAccession((Accessions) record);
        } else if (record instanceof Resources) {
            return convertResource((Resources) record);
        } else if (record instanceof ResourcesComponents) {
            return convertResourceComponent((ResourcesComponents) record);
        } else if (record instanceof DigitalObjects) {
            return convertDigitalObject((DigitalObjects) record);
        } else if (record instanceof Assessments) {
            return convertAssessment((Assessments) record);
        } else {
            return null;
        }
    }

    /**
     * Method to run a mapper script on a domain object record and return a boolean indicating
     * weather record should be copied
     *
     * @param record
     * @return
     * @throws Exception
     */
    public Boolean canCopyRecord(DomainObject record) throws Exception {
        bsi = new Interpreter();

        bsi.set("record", record);
        bsi.eval(mapperScript);

        Boolean result = (Boolean)bsi.get("result");

        bsi = null; // set this to null so it can be cleaned up by GC

        return result;
    }

    /**
     * Method to convert an AT subject record to
     *
     * @param record
     * @return
     * @throws Exception
     */
    public String convertSubject(Subjects record) throws Exception {
        // Main json object
        JSONObject json = new JSONObject();

        // add the AT database Id as an external ID
        addExternalId(record, json, "subject");

        json.put("publish", publishHashMap.get("subjects"));

        // set the subject source
        String source = record.getSubjectSource().trim();
        source = (String) enumUtil.getASpaceSubjectSource(source)[0];
        if(source != null && !source.isEmpty()) {
            json.put("source", source);
        } else {
            // source is now required in ASpace v1.1.0
            json.put("source", "local");
        }

        json.put("scope_note", record.getSubjectScopeNote());

        json = addSubjectTerm(record, json);

        json.put("vocabulary", vocabularyURI);

        return json.toString();
    }

    /**
     * adds subject terms to a json object
     * @param record subject from AT
     * @param json json object to add terms to
     * @return
     * @throws Exception
     */
    public JSONObject addSubjectTerm(Subjects record, JSONObject json) throws Exception {
        JSONArray termsJA = new JSONArray();

        // see if to define term type as untyped
        boolean isDefault = aspaceCopyUtil.isTermTypeDefault();

        // set the subject terms and term type
        String terms = record.getSubjectTerm();
        String termTypeAT = record.getSubjectTermType();
        String termType = (String) enumUtil.getASpaceTermType(termTypeAT)[0];

        // check to make sure we have valid term type,
        // otherwise use the default and add warning message
        if(termType == null) {
            String message = record.getSubjectTerm() + " :: Invalid Term Type: \"" + termTypeAT + "\", Changing to topical\n";
            aspaceCopyUtil.addErrorMessage(message);

            // change term type so record can save for now
            termType = "topical";
        }

        String[] sa = terms.split("\\s*--\\s*");

        for(int i = 0; i < sa.length; i++) {
            // check to see if to mark term after the first one as untyped
            if(i > 0 && !isDefault) {
                termType = "untyped";
            }

            String term = sa[i];
            JSONObject termJS = new JSONObject();
            termJS.put("term", term);
            termJS.put("term_type", termType);
            termJS.put("vocabulary", vocabularyURI);

            termsJA.put(termJS);
        }

        json.put("terms", termsJA);
        return json;
    }

    /**
     * Method to convert name record to ASpace agent record
     *
     * @param record
     * @return
     */
    public String convertName(Names record) throws Exception {
        // Main json object, agent_person.rb schema
        JSONObject agentJS = new JSONObject();

        // add the AT database Id as an external ID
        addExternalId(record, agentJS, "name");

        agentJS.put("publish", publishHashMap.get("names"));

        // hold name information
        JSONArray namesJA = new JSONArray();
        JSONObject namesJS = new JSONObject();

        //add the contact information
        JSONArray contactsJA = new JSONArray();
        JSONObject contactsJS = new JSONObject();

        //TODO 7/9/2013 -- There is currently no good way to map salutation
        //TODO so just migrate values that match what already there
        String salutation = enumUtil.getASpaceSalutation(record.getSalutation());
        if(!salutation.equals(ASpaceEnumUtil.UNMAPPED)) {
            contactsJS.put("salutation", salutation);
        }

        //contactsJS.put("publish", publishHashMap.get("names"));
        contactsJS.put("address_1", record.getContactAddress1());
        contactsJS.put("address_2", record.getContactAddress2());
        contactsJS.put("city", record.getContactCity());
        contactsJS.put("region", record.getContactRegion());
        contactsJS.put("country", record.getContactCountry());
        contactsJS.put("post_code", record.getContactMailCode());
        contactsJS.put("email", record.getContactEmail());

        // add the array holding the phone and fax numbers
        addPhoneNumbers(contactsJS, record.getContactPhone(), record.getContactFax());

        // add the contact notes if any. All notes will be concatenated
        addNote(contactsJS, record.getContactNotes());

        contactsJA.put(contactsJS);
        agentJS.put("agent_contacts", contactsJA);

        // add the biog-history note to the agent object
        addNote(agentJS, record);

        // add the agent date
        if(!record.getPersonalDates().trim().isEmpty()) {
            addNameDate(agentJS, "existence", record, "dates_of_existence");
        }

        // get the type of name
        String type = record.getNameType();

        // add basic information to the names record
        String sortName = addBasicNameInformation(namesJS, record);

        if(type.equalsIgnoreCase(Names.PERSON_TYPE)) {
            // set the agent type
            agentJS.put("agent_type", "agent_person");

            String primaryName = addPersonalNameInformation(namesJS, record, sortName);

            // set the name value for the contact information
            contactsJS.put("name", primaryName);
        } else if(type.equalsIgnoreCase(Names.FAMILY_TYPE)) {
            // set the agent type
            agentJS.put("agent_type", "agent_family");

            // set values for name_family.rb schema
            String familyName = fixEmptyString(record.getFamilyName(), sortName);

            namesJS.put("family_name", familyName);
            namesJS.put("prefix", record.getFamilyNamePrefix());

            // set the contact name
            contactsJS.put("name", familyName);
        } else if(type.equalsIgnoreCase(Names.CORPORATE_BODY_TYPE)) {
            // set the agent type
            agentJS.put("agent_type", "agent_corporate_entity");

            String primaryName = fixEmptyString(record.getCorporatePrimaryName(), sortName);

            // set values for name_corporate_entity.rb schema
            namesJS.put("primary_name", primaryName);
            namesJS.put("subordinate_name_1", record.getCorporateSubordinate1());
            namesJS.put("subordinate_name_2", record.getCorporateSubordinate2());
            namesJS.put("number", record.getNumber()); // not sure this is correct

            // set the contact name
            contactsJS.put("name", primaryName);
        } else {
            String message = record.getSortName() + ":: Unknown name type: " + type + "\n";
            aspaceCopyUtil.addErrorMessage(message);
            return null;
        }

        // add the names array and names json objects to main record
        namesJA.put(namesJS);

        // add any alternative forms of the names
        Set<NonPreferredNames> nonPreferredNames = record.getNonPreferredNames();
        if(nonPreferredNames != null && nonPreferredNames.size() != 0) {
            addNonePreferredNames(namesJA, nonPreferredNames);
        }

        agentJS.put("names", namesJA);

        return agentJS.toString();
    }

    /**
     * Method to add basic information to the names record
     *
     * @param namesJS
     * @param record
     * @throws Exception
     */
    private String addBasicNameInformation(JSONObject namesJS, BasicNames record) throws Exception {
        namesJS.put("qualifier", record.getQualifier());

        // get the mapped name source and rules
        if(record instanceof Names) {
            String nameSource = (String) enumUtil.getASpaceNameSource(((Names)record).getNameSource())[0];
            String nameRule = (String) enumUtil.getASpaceNameRule(((Names)record).getNameRule())[0];

            // we must have either a source or rules in ASpace so set source to unknown if both are empty in AT
            if ((nameSource == null || nameSource.isEmpty()) && (nameRule == null || nameRule.isEmpty())) {
                nameSource = (String) enumUtil.getASpaceNameSource("unknown")[0];
            }

            namesJS.put("source", nameSource);
            namesJS.put("rules", nameRule);
        }

        // get the sort name and if it empty need to you random string to get
        // record to save correctly
        String sortName = record.getSortName();

        if(sortName.isEmpty()) {
            sortName = "unspecified ##" + randomString.nextString();
        }

        namesJS.put("sort_name", sortName);

        return sortName;
    }

    /**
     * Method to add personal name information to json record
     *
     * @param namesJS
     * @param record
     * @param sortName
     * @throws Exception
     */
    private String addPersonalNameInformation(JSONObject namesJS, BasicNames record, String sortName) throws Exception {
        // set the title to unknown if it is blank
        String title = record.getPersonalTitle();

        String primaryName = fixEmptyString(record.getPersonalPrimaryName(), sortName);

        // set values for name_person.rb schema
        namesJS.put("primary_name", primaryName);
        namesJS.put("title", title);
        namesJS.put("name_order", enumUtil.getASpaceNameOrder(record.getPersonalDirectOrder())[0]);
        namesJS.put("prefix", record.getPersonalPrefix());
        namesJS.put("rest_of_name", record.getPersonalRestOfName());
        namesJS.put("suffix", record.getPersonalSuffix());
        namesJS.put("fuller_form", record.getPersonalFullerForm());
        namesJS.put("number", record.getNumber()); // not sure this is correct

        return primaryName;
    }

    /**
     * Method to add none preferred Names
     * @param namesJA
     * @param nonPreferredNames
     */
    private void addNonePreferredNames(JSONArray namesJA, Set<NonPreferredNames> nonPreferredNames) throws Exception {
        for(NonPreferredNames record: nonPreferredNames) {
            JSONObject namesJS = new JSONObject();

            String sortName = addBasicNameInformation(namesJS, record);
            addPersonalNameInformation(namesJS, record, sortName);

            // now add the date
            if(!record.getPersonalDates().trim().isEmpty()) {
                addNameDate(namesJS, "usage", record, "use_dates");
            }

            // may have to add family name information
            if(!record.getFamilyName().isEmpty()) {
                namesJS.put("family_name", record.getFamilyName());
                namesJS.put("prefix", record.getFamilyNamePrefix());
            }

            namesJA.put(namesJS);
        }
    }

    /**
     * Method to get the corporate agent object from a repository
     *
     * @param repository
     * @return
     */
    public String getCorporateAgent(Repositories repository) throws JSONException {
        // Main json object, agent_person.rb schema
        JSONObject agentJS = new JSONObject();
        agentJS.put("agent_type", "agent_corporate_entity");

        // hold name information
        JSONArray namesJA = new JSONArray();
        JSONObject namesJS = new JSONObject();

        //add the contact information
        JSONArray contactsJA = new JSONArray();
        JSONObject contactsJS = new JSONObject();

        contactsJS.put("name", fixEmptyString(repository.getRepositoryName(), repository.getShortName()));
        contactsJS.put("address_1", repository.getAddress1());
        contactsJS.put("address_2", repository.getAddress2());
        contactsJS.put("address_3", repository.getAddress3());
        contactsJS.put("city", repository.getCity());
        contactsJS.put("region", repository.getRegion());

        // add the country, country code is added to the repository itself
        contactsJS.put("country", repository.getCountry().trim());

        contactsJS.put("post_code", repository.getMailCode());
        contactsJS.put("email", repository.getEmail());

        // add the array holding the phone and fax numbers
        addPhoneNumbers(contactsJS, repository.getTelephone(), repository.getFax());

        contactsJA.put(contactsJS);
        agentJS.put("agent_contacts", contactsJA);

        // make the primary name the shortname since this is what
        // ASpace uses to create the agent record
        String primaryName = repository.getShortName();

        namesJS.put("source", "local");
        namesJS.put("primary_name", primaryName);
        namesJS.put("sort_name", primaryName);

        namesJA.put(namesJS);
        agentJS.put("names", namesJA);

        return agentJS.toString();
    }

    /**
     * Method to add the telephone and fax numbers to an agent contact information
     *
     * @param contactsJS
     * @param telephone
     * @param fax
     */
    private void addPhoneNumbers(JSONObject contactsJS, String telephone, String fax) throws JSONException {
        JSONArray telephonesJA = new JSONArray();

        if(telephone != null && !telephone.isEmpty()) {
            JSONObject phoneJS = new JSONObject();
            phoneJS.put("number", telephone);
            phoneJS.put("number_type", "business");
            telephonesJA.put(phoneJS);
        }

        if (fax != null && !fax.isEmpty()) {
            JSONObject phoneJS = new JSONObject();
            phoneJS.put("number", fax);
            phoneJS.put("number_type", "fax");
            telephonesJA.put(phoneJS);
        }

        contactsJS.put("telephones", telephonesJA);
    }

    /**
     * Method to convert an AT repository record
     *
     * @param record
     * @return
     * @throws Exception
     */
    public String convertRepository(Repositories record) throws Exception {
        // Main json object
        JSONObject json = new JSONObject();

        // add the AT database Id as an external ID
        addExternalId(record, json, "repository");

        // get the repo code
        json.put("repo_code", record.getShortName());
        json.put("name", fixEmptyString(record.getRepositoryName()));
        json.put("org_code", record.getAgencyCode());
        String countryCode = (String) enumUtil.getASpaceCountryID(record.getCountryCode())[0];
        if (countryCode != null) json.put("country", countryCode);
        json.put("parent_institution_name", record.getInstitutionName());
        json.put("url", fixUrl(record.getUrl()));
        json.put("publish", publishHashMap.get("repositories"));
        json.put("hidden", !(publishHashMap.get("repositories")));

        return json.toString();
    }

    /**
     * converts an AT location record
     * @param record
     * @return
     * @throws Exception
     */
    public String convertLocation(Locations record) throws Exception {
        // Main json object
        JSONObject json = new JSONObject();

        // add the AT database Id as an external ID
        addExternalId(record, json, "location");

        // create a json object that represents the location object
        json.put("building", fixEmptyString(record.getBuilding(), "Unknown Building"));
        json.put("floor", record.getFloor());
        json.put("room", record.getRoom());
        json.put("area", record.getArea());
        json.put("barcode", record.getBarcode());
        json.put("classification", record.getClassificationNumber());

        // need to remove the label from the coordinate indicator before sending to ASpace
        String label = record.getCoordinate1Label();
        String coordinate = record.getCoordinate1();
        json.put("coordinate_1_label", label);
        json.put("coordinate_1_indicator", fixEmptyString(coordinate.replace(label, "")));

        label = record.getCoordinate2Label();
        coordinate = record.getCoordinate2();
        json.put("coordinate_2_label", label);
        json.put("coordinate_2_indicator", coordinate.replace(label, ""));

        label = record.getCoordinate3Label();
        coordinate = record.getCoordinate3();
        json.put("coordinate_3_label", label);
        json.put("coordinate_3_indicator", coordinate.replace(label, ""));

        return json.toString();
    }

     /**
     * Method to convert an AT subject record
     *
     * @param record
     * @return
     * @throws Exception
     */
    public String convertUser(Users record) throws Exception {
        // Main json object
        JSONObject json = new JSONObject();

        // add the AT database Id as an external ID
        addExternalId(record, json, "user");

        // get the username replacing spaces with underscores
        String username = record.getUserName().trim();

        json.put("username", username);

        // get the full name, if it doesn't exist then just enter text with random string
        String name = fixEmptyString(record.getFullName(), "User: " + username);
        json.put("name", name);

        json.put("email", record.getEmail());
        json.put("title", record.getTitle());
        json.put("department", record.getDepartment());

        return json.toString();
    }

    /**
     * Method to convert an accession record to json ASpace JSON
     *
     * @param record
     * @return
     * @throws Exception
     */
    public JSONObject convertAccession(Accessions record) throws Exception {
        // Main json object
        JSONObject json = new JSONObject();

        // add the AT database Id as an external ID
        addExternalId(record, json, "accession");

        json.put("publish", publishHashMap.get("accessions"));

        // check to make sure we have a title
        String title = fixEmptyString(record.getTitle(), null);
        Date date = record.getAccessionDate();

        if(date == null) {
            String message = "Invalid Accession Date for" + record.getAccessionNumber() + "\n";
            aspaceCopyUtil.addErrorMessage(message);
            return null;
        }

        json.put("title", title);
        json.put("accession_date", date);

        // get the ids and make them unique if we in DEBUG mode
        String[] cleanIds = cleanUpIds(ASpaceClient.ACCESSION_ENDPOINT, record.getAccessionNumber1().trim(),
                record.getAccessionNumber2().trim(), record.getAccessionNumber3().trim(),
                record.getAccessionNumber4().trim());

        String id_0 = cleanIds[0];
        String id_1 = cleanIds[1];
        String id_2 = cleanIds[2];
        String id_3 = cleanIds[3];

        if (makeUnique) {
            id_0 = randomString.nextString();
            id_1 = randomString.nextString();
            id_2 = randomString.nextString();
            id_3 = randomString.nextString();
        }

        json.put("id_0", id_0);
        json.put("id_1", id_1);
        json.put("id_2", id_2);
        json.put("id_3", id_3);

        json.put("content_description", record.getDescription());
        json.put("condition_description", record.getConditionNote());
        json.put("disposition", record.getAccessionDispositionNote());

        //json.put("disposition", record.?);
        json.put("inventory",record.getInventory());


        // add linked records (extents, dates, rights statement)

        // add the extent array containing one object or many depending if we using multiple extents
        JSONArray extentJA = new JSONArray();
        JSONObject extentJS = new JSONObject();

        // first see to add mulitple extents
        Set<ArchDescriptionPhysicalDescriptions> physicalDescriptions = record.getPhysicalDesctiptions();
        convertPhysicalDescriptions(extentJA, physicalDescriptions);

        // now add an extent if needed
        if(extentJA.length() > 0 && extentPortionInParts) {
            extentJS.put("portion", "part");
        } else {
            extentJS.put("portion", "whole");
        }

        extentJS.put("extent_type", enumUtil.getASpaceExtentType(record.getExtentType())[0]);
        extentJS.put("container_summary", record.getContainerSummary());

        if (record.getExtentNumber() != null) {
            extentJS.put("number", removeTrailingZero(record.getExtentNumber()));
            extentJA.put(extentJS);
        } else if(extentJA.length() == 0) { // add a default number
            extentJS.put("number", "0");
            extentJA.put(extentJS);
        }

        json.put("extents", extentJA);

        // convert and add any accessions related dates here
        JSONArray dateJA = new JSONArray();

        // add the bulk dates
        addDate(dateJA, record, "creation", "Accession: " + record.getAccessionNumber());

        // add the archdescription dates now
        Set<ArchDescriptionDates> archDescriptionDates = record.getArchDescriptionDates();
        convertArchDescriptionDates(dateJA, archDescriptionDates, record.getAccessionNumber());

        // if there are any dates add them to the main json record
        if(dateJA.length() != 0) {
            json.put("dates", dateJA);
        }

        // add external documents
        JSONArray externalDocumentsJA = new JSONArray();
        Set<ExternalReference> externalDocuments = record.getRepeatingData(ExternalReference.class);

        if(externalDocuments != null && externalDocuments.size() != 0) {
            convertExternalDocuments(externalDocumentsJA, externalDocuments);
            json.put("external_documents", externalDocumentsJA);
        }

        // add the deaccessions
        Set<Deaccessions> deaccessions = record.getDeaccessions();
        if(deaccessions != null && deaccessions.size() != 0) {
            JSONArray deaccessionsJA = new JSONArray();
            convertDeaccessions(deaccessionsJA, deaccessions);
            json.put("deaccessions", deaccessionsJA);
        }

        // add a rights statement object
        addRightsStatementRecord(record, json);

        // add the collection management record now
        addCollectionManagementRecord(record, json);

        json.put("suppressed", record.getInternalOnly());

        json.put("acquisition_type", enumUtil.getASpaceAcquisitionType(record.getAcquisitionType())[0]);

        json.put("resource_type", enumUtil.getASpaceAccessionResourceType(record.getResourceType())[0]);

        json.put("restrictions_apply", record.getRestrictionsApply());

        json.put("retention_rule", record.getRetentionRule());

        // add info on who created the accession record to general note field
        String note = "";
        note += record.getGeneralAccessionNote();
        note += "\n\nAT record created by: ";
        note += record.getCreatedBy();
        note += ".\n";

        json.put("general_note", note);

        json.put("access_restrictions", record.getAccessRestrictions());

        json.put("access_restrictions_note", record.getAccessRestrictionsNote());

        json.put("use_restrictions_note", record.getUseRestrictionsNote());

        json.put("use_restrictions", record.getUseRestrictions());

        // add the user defined fields here
        addUserDefinedFields(json, record);

        return json;
    }

    /**
     * Method to add a right statement object
     *
     * @param record
     * @param json
     */
    private void addRightsStatementRecord(Accessions record, JSONObject json) throws Exception {
        if(record.getRightsTransferred() == null || !record.getRightsTransferred()) return;

        JSONArray rightsStatementJA = new JSONArray();
        JSONObject rightStatementJS = new JSONObject();
        rightStatementJS.put("rights_type", "other");
        rightStatementJS.put("other_rights_basis", enumUtil.getASpaceRightsBasis(null)[0]);

        //add the start date or default date if null
        Date startDate = record.getRightsTransferredDate();
        if (startDate == null) startDate = DEFAULT_DATE;
        rightStatementJS.put("start_date", startDate.toString());

        //add the note if there is one
        JSONArray notesJA = new JSONArray();
        String note = record.getRightsTransferredNote();
        if (note != null && !(note.isEmpty())) {
            JSONObject noteJS = new JSONObject();
            noteJS.put("jsonmodel_type", "note_rights_statement");
            noteJS.put("type", "additional_information");
            JSONArray content = new JSONArray();
            content.put(note);
            noteJS.put("content", content);
            notesJA.put(noteJS);
        }
        rightStatementJS.put("notes", notesJA);

        rightsStatementJA.put(rightStatementJS);
        json.put("rights_statements", rightsStatementJA);
    }

    /**
     * Method to get an event object Accession processed info
     *
     *
     * @param accession
     * @param accessionURI
     * @param agentURI
     * @return
     */
    public ArrayList<JSONObject> getAccessionEvents(Accessions accession, String agentURI, String accessionURI) throws Exception {
        ArrayList<JSONObject> eventsList = new ArrayList<JSONObject>();
        JSONObject eventJS;

        // grab the accession date in case we need a date for an event
        Date accessionDate = accession.getAccessionDate();

        if(accession.getAccessionProcessed() != null && accession.getAccessionProcessed()) {
            eventJS = new JSONObject();
            eventJS.put("event_type", "processed");
            addEventDate(eventJS, accession.getAccessionProcessedDate(), accessionDate, "single", "event");
            addEventLinkedRecordAndAgent(eventJS, agentURI, accessionURI);
            eventsList.add(eventJS);
        }

        if(accession.getAcknowledgementSent() != null && accession.getAcknowledgementSent()) {
            eventJS = new JSONObject();
            eventJS.put("event_type", "acknowledgement_sent");
            addEventDate(eventJS, accession.getAcknowledgementDate(), accessionDate, "single", "event");
            addEventLinkedRecordAndAgent(eventJS, agentURI, accessionURI);
            eventsList.add(eventJS);
        }

        if(accession.getAgreementReceived() != null && accession.getAgreementReceived()) {
            eventJS = new JSONObject();
            eventJS.put("event_type", "agreement_signed");
            addEventDate(eventJS, accession.getAgreementReceivedDate(), accessionDate, "single", "event");
            addEventLinkedRecordAndAgent(eventJS, agentURI, accessionURI);
            eventsList.add(eventJS);
        }

        if(accession.getAgreementSent() != null && accession.getAgreementSent()) {
            eventJS = new JSONObject();
            eventJS.put("event_type", "agreement_sent");
            addEventDate(eventJS, accession.getAgreementSentDate(), accessionDate, "single", "event");
            addEventLinkedRecordAndAgent(eventJS, agentURI, accessionURI);
            eventsList.add(eventJS);
        }

        if(accession.getCataloged() != null && accession.getCataloged()) {
            eventJS = new JSONObject();
            eventJS.put("event_type", "cataloged");
            addEventDate(eventJS, accession.getCatalogedDate(), accessionDate, "single", "event");
            addEventLinkedRecordAndAgent(eventJS, agentURI, accessionURI);
            eventsList.add(eventJS);
        }

        if(accession.getProcessingStartedDate() != null) {
            eventJS = new JSONObject();
            eventJS.put("event_type", "processing_started");
            addEventDate(eventJS, accession.getProcessingStartedDate(), accessionDate, "single", "event");
            addEventLinkedRecordAndAgent(eventJS, agentURI, accessionURI);
            eventsList.add(eventJS);
        }

        if(accession.getRightsTransferred() != null && accession.getRightsTransferred()) {
            eventJS = new JSONObject();
            eventJS.put("event_type", "copyright_transfer");//"rights_transferred");
            eventJS.put("outcome_note", accession.getRightsTransferredNote());
            addEventDate(eventJS, accession.getRightsTransferredDate(), accessionDate, "single", "event");
            addEventLinkedRecordAndAgent(eventJS, agentURI, accessionURI);
            eventsList.add(eventJS);
        }

        return eventsList;
    }

    /**
     * Method to add a date object
     *
     * @param eventJS
     * @param date
     * @param dateType
     * @param dateLabel
     */
    private void addEventDate(JSONObject eventJS, Date date, Date alternativeDate, String dateType, String dateLabel) throws Exception {
        // see if to use the alternative date instead since a date is required
        if(date == null) {
            date = alternativeDate;
        }

        JSONObject dateJS = new JSONObject();
        dateJS.put("date_type", dateType);
        dateJS.put("label", dateLabel);
        dateJS.put("expression", date.toString());

        eventJS.put("date", dateJS);
    }

    /**
     * Method to add the event linked record
     *
     * @param uri
     * @param eventJS
     * @throws Exception
     */
    private void addEventLinkedRecordAndAgent(JSONObject eventJS, String agentURI, String uri) throws Exception {
        // add a dummy linked agent so record can save
        JSONArray linkedAgentsJA = new JSONArray();
        JSONObject linkedAgentJS = new JSONObject();

        linkedAgentJS.put("role", "recipient");
        linkedAgentJS.put("ref", agentURI);
        linkedAgentsJA.put(linkedAgentJS);

        eventJS.put("linked_agents", linkedAgentsJA);

        // add the linked to the record
        JSONArray linkedRecordsJA = new JSONArray();
        JSONObject linkedRecordJS = new JSONObject();

        linkedRecordJS.put("role", "source");
        linkedRecordJS.put("ref", uri);
        linkedRecordsJA.put(linkedRecordJS);

        eventJS.put("linked_records", linkedRecordsJA);
    }

    /**
     * Method to convert physical descriptions object to an extent object
     *
     * @param extentJA
     * @param physicalDescriptions
     * @throws JSONException
     */
    public void convertPhysicalDescriptions(JSONArray extentJA, Set<ArchDescriptionPhysicalDescriptions> physicalDescriptions) throws JSONException {
        if(physicalDescriptions == null || physicalDescriptions.size() == 0) return;

        // keep track of the number of records
        int size = physicalDescriptions.size();

        for (ArchDescriptionPhysicalDescriptions physicalDescription : physicalDescriptions) {
            JSONObject extentJS = new JSONObject();

            if(size == 1 && extentPortionInParts) {
                extentJS.put("portion", "whole");
            } else {
                extentJS.put("portion", "part");
            }

            if(physicalDescription.getExtentNumber() != null) {
                String number = removeTrailingZero(physicalDescription.getExtentNumber());
                extentJS.put("number", number);
            } else {
                extentJS.put("number", "1");
            }

            extentJS.put("extent_type", enumUtil.getASpaceExtentType(physicalDescription.getExtentType())[0]);
            extentJS.put("container_summary", physicalDescription.getContainerSummary());
            extentJS.put("physical_details", physicalDescription.getPhysicalDetail());
            extentJS.put("dimensions", physicalDescription.getDimensions());
            extentJA.put(extentJS);
        }
    }

    /**
     * Method to convert arch description dates to date json objects
     * @param dateJA
     * @param archDescriptionDates
     */
    public void convertArchDescriptionDates(JSONArray dateJA, Set<ArchDescriptionDates> archDescriptionDates,
                                            String recordIdentifier) throws JSONException {
        if(archDescriptionDates == null || archDescriptionDates.size() == 0) return;

        // TODO 12/10/2012 Archivists needs to map this
        for (ArchDescriptionDates archDescriptionDate: archDescriptionDates) {
            JSONObject dateJS = new JSONObject();
            dateJS.put("date_type", enumUtil.getASpaceDateType(archDescriptionDate)[0]);
            dateJS.put("label", enumUtil.getASpaceDateEnum(archDescriptionDate.getDateType())[0]);
            dateJS.put("certainty", enumUtil.getASpaceDateCertainty(archDescriptionDate)[0]);

            String dateExpression = archDescriptionDate.getDateExpression();
            dateJS.put("expression", dateExpression);

            String beginDate = normalizeISODate(archDescriptionDate.getIsoDateBegin(), recordIdentifier);
            String endDate = normalizeISODate(archDescriptionDate.getIsoDateEnd(), recordIdentifier);

            if(beginDate != null) {
                dateJS.put("begin", beginDate);

                if(endDate != null && endDateValid(beginDate, endDate, recordIdentifier)) {
                    dateJS.put("end", endDate);
                } else {
                    dateJS.put("end", beginDate);
                }
            } else if(dateExpression == null || dateExpression.trim().length() < 2) {
                // check that there is a date expression. If we have no expression then
                // we need to add one since we have no start date
                dateJS.put("expression", "unspecified");
            }

            dateJS.put("era", enumUtil.getASpaceDateEra(archDescriptionDate.getEra())[0]);
            dateJS.put("calender", enumUtil.getASpaceDateCalender(archDescriptionDate.getCalendar())[0]);

            dateJA.put(dateJS);

            // DEBUG Code to store all dates then print them put
            if(checkISODates) {
                datesList.add(beginDate + "/" + endDate + "/" + dateExpression + "/" + recordIdentifier);
            }

            // TODO 04/28/2014 -- create a date object for bulk dates as well
        }
    }

    /**
     * Method to convert external documents to the aspace external document object
     *
     * @param externalDocumentsJA
     * @param externalDocuments
     */
    public void convertExternalDocuments(JSONArray externalDocumentsJA, Set<ExternalReference> externalDocuments) throws JSONException {
        for (ExternalReference externalDocument: externalDocuments) {
            JSONObject documentJS = new JSONObject();
            documentJS.put("title", fixEmptyString(externalDocument.getTitle()));
            documentJS.put("location", fixUrl(externalDocument.getHref()));
            externalDocumentsJA.put(documentJS);
        }
    }

    /**
     * Method to convert the deaccessions records to equivalent json records
     *
     * @param deaccessionsJA
     * @param deaccessions
     */
    public void convertDeaccessions(JSONArray deaccessionsJA, Set<Deaccessions> deaccessions) throws JSONException {
        // TODO 12/10/2012 Archivists needs to map this
        for (Deaccessions deaccession: deaccessions) {
            JSONObject deaccessionJS = new JSONObject();
            deaccessionJS.put("scope", "part");
            deaccessionJS.put("description", deaccession.getDescription());
            deaccessionJS.put("reason", deaccession.getReason());
            deaccessionJS.put("disposition", deaccession.getDisposition());
            deaccessionJS.put("notification", deaccession.getNotification());

            // add the date object
            JSONObject dateJS = new JSONObject();

            dateJS.put("date_type", "single");
            dateJS.put("label", "deaccession");
            dateJS.put("expression", deaccession.getDeaccessionDate().toString());
            dateJS.put("begin", deaccession.getDeaccessionDate().toString()); // This should not be needed
            dateJS.put("era", "ce");
            dateJS.put("calender", "gregorian");

            deaccessionJS.put("date", dateJS);

            // add the extent array object
            if(deaccession.getExtent() != null) {
                JSONArray extentJA = new JSONArray();
                JSONObject extentJS = new JSONObject();

                extentJS.put("portion", "whole");
                extentJS.put("number", removeTrailingZero(deaccession.getExtent()));
                extentJS.put("extent_type", enumUtil.getASpaceExtentType(deaccession.getExtentType())[0]);
                extentJS.put("container_summary", deaccession.getDescription());

                extentJA.put(extentJS);
                deaccessionJS.put("extents", extentJA);
            }

            // add this deaccession object to array
            deaccessionsJA.put(deaccessionJS);
        }
    }

    /**
     * Method to return a collection management record object from an accession
     *
     * @param record
     * @param recordJS
     * @return
     * @throws Exception
     */
    public void addCollectionManagementRecord(Accessions record, JSONObject recordJS) throws Exception {
        // Main json object
        JSONObject json = new JSONObject();

        json.put("cataloged_note", record.getCatalogedNote());
        json.put("processing_plan", record.getProcessingPlan());

        if(record.getProcessingPriority() != null && !record.getProcessingPriority().isEmpty()) {
            json.put("processing_priority", enumUtil.getASpaceCollectionManagementRecordProcessingPriority(record.getProcessingPriority())[0]);
        }

        if(record.getProcessingStatus() != null && !record.getProcessingStatus().isEmpty()) {
            json.put("processing_status", enumUtil.getASpaceCollectionManagementRecordProcessingStatus(record.getProcessingStatus())[0]);
        }

        json.put("processors", record.getProcessors());

        recordJS.put("collection_management", json);
    }

    /**
     * Method to add user defined fields from Accessions, Resource etc to the JSON object
     *
     * @param json
     * @param domainObject
     */
    public void addUserDefinedFields(JSONObject json, DomainObject domainObject) throws Exception {
        JSONObject userDefinedJS = new JSONObject();

        if (domainObject instanceof Accessions) {
            Accessions record = (Accessions) domainObject;

            userDefinedJS.put("boolean_1", record.getUserDefinedBoolean1());
            userDefinedJS.put("boolean_2", record.getUserDefinedBoolean2());

            if(record.getUserDefinedInteger1() != null) userDefinedJS.put("integer_1", record.getUserDefinedInteger1().toString());
            if(record.getUserDefinedInteger2() != null) userDefinedJS.put("integer_2", record.getUserDefinedInteger2().toString());

            if(record.getUserDefinedReal1() != null) userDefinedJS.put("real_1", record.getUserDefinedReal1().toString());
            if(record.getUserDefinedReal2() != null) userDefinedJS.put("real_2", record.getUserDefinedReal2().toString());

            userDefinedJS.put("string_1", record.getUserDefinedString1());
            userDefinedJS.put("string_2", record.getUserDefinedString2());
            userDefinedJS.put("string_3", record.getUserDefinedString3());

            userDefinedJS.put("text_1", record.getUserDefinedText1());
            userDefinedJS.put("text_2", record.getUserDefinedText2());
            userDefinedJS.put("text_3", record.getUserDefinedText3());
            userDefinedJS.put("text_4", record.getUserDefinedText4());

            if(record.getUserDefinedDate1() != null) userDefinedJS.put("date_1", record.getUserDefinedDate1());
            if(record.getUserDefinedDate2() != null) userDefinedJS.put("date_2", record.getUserDefinedDate2());
        } else if (domainObject instanceof Resources) {
            Resources record = (Resources) domainObject;

            userDefinedJS.put("string_1", record.getUserDefinedString1());
            userDefinedJS.put("string_2", record.getUserDefinedString2());
        } else if (domainObject instanceof ArchDescriptionAnalogInstances) {
            ArchDescriptionAnalogInstances record = (ArchDescriptionAnalogInstances) domainObject;

            userDefinedJS.put("boolean_1", record.getUserDefinedBoolean1());
            userDefinedJS.put("boolean_2", record.getUserDefinedBoolean2());
            userDefinedJS.put("string_1", record.getUserDefinedString1());
            userDefinedJS.put("string_2", record.getUserDefinedString2());
        } else {
            return; // Record doesn't have user defined fields so just return
        }

        json.put("user_defined", userDefinedJS);
    }

    /**
     * Method to convert a digital object record
     *
     * @param record
     * @return
     */
    public JSONObject convertDigitalObject(DigitalObjects record) throws Exception {
        // Main json object
        JSONObject json = new JSONObject();

        // add the AT database Id as an external ID
        addExternalId(record, json, "digital_object");

        json.put("publish", publishHashMap.get("digitalObjects"));

        /* add the fields required for abstract_archival_object.rb */

        String title = record.getObjectLabel();
        json.put("title", fixEmptyString(title));

        addLanguageCode(json, record.getLanguageCode());
        addLangMaterials(json, record.getLanguageCode(), false);

        // add the date object
        JSONArray dateJA = new JSONArray();
        addDate(dateJA, record, "creation", "Digital Object: " + record.getMetsIdentifier());

        if(dateJA.length() != 0) {
            json.put("dates", dateJA);
        }

        // add the fields required digital_object.rb

        JSONArray fileVersionsJA = new JSONArray();
        convertFileVersions(fileVersionsJA, record.getFileVersions());
        json.put("file_versions", fileVersionsJA);

        json.put("digital_object_id", getUniqueID(ASpaceClient.DIGITAL_OBJECT_ENDPOINT, record.getMetsIdentifier(), null));

        // set the digital object type
        String type = record.getObjectType();
        if(type != null && !type.isEmpty()) {
            json.put("digital_object_type", enumUtil.getASpaceDigitalObjectType(type)[0]);
        }

        // set the restrictions apply
        json.put("restrictions", record.getRestrictionsApply());

        // add the notes
        JSONArray notesJA = new JSONArray();
        addNotes(notesJA, record);
        json.put("notes", notesJA);

        return json;
    }

    /**
     * Method to convert a digital object record into a aspace digital object component
     *
     * @param record
     * @return
     */
    public JSONObject convertToDigitalObjectComponent(DigitalObjects record) throws Exception {
        // Main json object
        JSONObject json = new JSONObject();

        /* add the fields required for abstract_archival_object.rb */
        String title = record.getObjectLabel();
        json.put("title", fixEmptyString(title));

        json.put("publish", publishHashMap.get("digitalObjects"));

        addLanguageCode(json, record.getLanguageCode());
        addLangMaterials(json, record.getLanguageCode(), false);

        /* add fields required for digital object component*/
        JSONArray fileVersionsJA = new JSONArray();
        convertFileVersions(fileVersionsJA, record.getFileVersions());
        json.put("file_versions", fileVersionsJA);

        String label = record.getLabel();
        json.put("label", label);

        json.put("component_id", getUniqueID(ASpaceClient.DIGITAL_OBJECT_ENDPOINT, record.getComponentId(), null));

        // add the date object
        JSONArray dateJA = new JSONArray();
        addDate(dateJA, record, "creation", "Digital Object Component: " + record.getComponentId());

        if(dateJA.length() != 0) {
            json.put("dates", dateJA);
        }

        // add the notes
        JSONArray notesJA = new JSONArray();
        addNotes(notesJA, record);
        json.put("notes", notesJA);

        return json;
    }

    /**
     * Method to convert external documents to the aspace external document object
     *
     * @param fileVersionsJA
     * @param fileVersionSet
     */
    public void convertFileVersions(JSONArray fileVersionsJA, Set<FileVersions> fileVersionSet) throws JSONException {
        for (FileVersions fileVersion: fileVersionSet) {
            JSONObject fileVersionJS = new JSONObject();

            fileVersionJS.put("file_uri", fileVersion.getUri());
            fileVersionJS.put("use_statement", enumUtil.getASpaceFileVersionUseStatement(fileVersion.getUseStatement())[0]);
            fileVersionJS.put("xlink_actuate_attribute", fileVersion.getEadDaoActuate());
            fileVersionJS.put("xlink_show_attribute", fileVersion.getEadDaoShow().toLowerCase());

            fileVersionsJA.put(fileVersionJS);
        }
    }

    /**
     * Method to convert an resource record to json ASpace JSON
     *
     * @param record
     * @return
     * @throws Exception
     */
    public JSONObject convertResource(Resources record) throws Exception {
        // Main json object
        JSONObject json = new JSONObject();

        // add the AT database Id as an external ID
        addExternalId(record, json, "resource");

        // set the publish, restrictions, processing note, container summary
        json.put("publish", publishHashMap.get("resources"));

        /* Add fields needed for abstract_archival_object.rb */

        // check to make sure we have a title
        String title = fixEmptyString(record.getTitle());
        json.put("title", title);

        // add the language code
        addLanguageCode(json, record.getLanguageCode());
        addLangMaterials(json, record.getLanguageCode(), true);

        // add the extent array containing one object or many depending if we using multiple extents
        JSONArray extentJA = new JSONArray();
        JSONObject extentJS = new JSONObject();

        // first see if there are any multi-part extent
        Set<ArchDescriptionPhysicalDescriptions> physicalDescriptions = record.getPhysicalDesctiptions();
        convertPhysicalDescriptions(extentJA, physicalDescriptions);

        // now add an extent if needed
        if(extentJA.length() > 0 && extentPortionInParts) {
            extentJS.put("portion", "part");
        } else {
            extentJS.put("portion", "whole");
        }

        extentJS.put("extent_type", enumUtil.getASpaceExtentType(record.getExtentType())[0]);
        extentJS.put("container_summary", record.getContainerSummary());

        if (record.getExtentNumber() != null) {
            extentJS.put("number", removeTrailingZero(record.getExtentNumber()));
            extentJA.put(extentJS);
        } else if(extentJA.length() == 0) { // add a default number
            extentJS.put("number", "0");
            extentJA.put(extentJS);
        }

        json.put("extents", extentJA);

        // add the date array containing the dates json objects
        JSONArray dateJA = new JSONArray();

        addDate(dateJA, record, "creation", "Resource: " + currentResourceRecordIdentifier);

        Set<ArchDescriptionDates> archDescriptionDates = record.getArchDescriptionDates();
        convertArchDescriptionDates(dateJA, archDescriptionDates, "Resource: " + currentResourceRecordIdentifier);

        if(dateJA.length() != 0) {
            json.put("dates", dateJA);
        }

        // add external documents
        JSONArray externalDocumentsJA = new JSONArray();
        Set<ExternalReference> externalDocuments = record.getRepeatingData(ExternalReference.class);

        if(externalDocuments != null && externalDocuments.size() != 0) {
            convertExternalDocuments(externalDocumentsJA, externalDocuments);
            json.put("external_documents", externalDocumentsJA);
        }

        // Add fields needed for resource.rb

        // get the ids and make them unique if we in DEBUG mode
        String[] cleanIds = cleanUpIds(ASpaceClient.RESOURCE_ENDPOINT, record.getResourceIdentifier1().trim(),
                record.getResourceIdentifier2().trim(), record.getResourceIdentifier3().trim(),
                record.getResourceIdentifier4().trim());

        String id_0 = cleanIds[0];
        String id_1 = cleanIds[1];
        String id_2 = cleanIds[2];
        String id_3 = cleanIds[3];

        if(makeUnique) {
            id_0 = randomString.nextString();
            id_1 = randomString.nextString();
            id_2 = randomString.nextString();
            id_3 = randomString.nextString();
        }

        json.put("id_0", id_0);
        json.put("id_1", id_1);
        json.put("id_2", id_2);
        json.put("id_3", id_3);

        // get the level
        String level = (String) enumUtil.getASpaceResourceLevel(record.getLevel())[0];
        json.put("level", level);

        if(level.equals("otherlevel")) {
            json.put("other_level", fixEmptyString(record.getOtherLevel()));
        }

        if(record.getRestrictionsApply() != null && record.getRestrictionsApply()) {
            json.put("restrictions", record.getRestrictionsApply());
        }

        json.put("repository_processing_note", record.getRepositoryProcessingNote());//repoProcessingNote);
        json.put("container_summary", record.getContainerSummary());


        // add fields for EAD
        json.put("ead_id", getUniqueID("ead", record.getEadFaUniqueIdentifier(), null));
        json.put("ead_location", record.getEadFaLocation());

        // TODO 5/12/2015 -- breakout finding aid title and subtitle into separate fields in the ASpace record
        json.put("finding_aid_title", record.getFindingAidTitle());
        json.put("finding_aid_subtitle", record.getFindingAidSubtitle());
        json.put("finding_aid_filing_title", record.getFindingAidFilingTitle());
        json.put("finding_aid_date", record.getFindingAidDate());
        json.put("finding_aid_author", record.getAuthor());

        if(record.getDescriptionRules() != null) {
            json.put("finding_aid_description_rules", enumUtil.getASpaceFindingAidDescriptionRule(record.getDescriptionRules())[0]);
        }

        json.put("finding_aid_language", enumUtil.getASpaceLanguageCode(record.getLanguageCode()));
        json.put("finding_aid_script", "Zyyy");
        json.put("finding_aid_language_note", record.getLanguageOfFindingAid());
        json.put("finding_aid_sponsor", record.getSponsorNote());
        json.put("finding_aid_edition_statement", record.getEditionStatement());
        json.put("finding_aid_series_statement", record.getSeries());
        json.put("finding_aid_revision_date", record.getRevisionDate());
        json.put("finding_aid_revision_description", record.getRevisionDescription());

        if(record.getFindingAidStatus() != null) {
            json.put("finding_aid_status", enumUtil.getASpaceFindingAidStatus(record.getFindingAidStatus())[0]);
        }

        json.put("finding_aid_note", record.getFindingAidNote());

        // add the deaccessions
        JSONArray deaccessionsJA = new JSONArray();
        Set<Deaccessions> deaccessions = record.getDeaccessions();

        if(deaccessions != null && deaccessions.size() != 0) {
            convertDeaccessions(deaccessionsJA, deaccessions);
            json.put("deaccessions", deaccessionsJA);
        }

        // add the notes
        JSONArray notesJA = new JSONArray();
        addNotes(notesJA, record);
        json.put("notes", notesJA);

        return json;
    }

    /**
     * Method to convert a resource record into an archival object
     *
     * @param record
     * @return
     */
    private JSONObject convertResourceComponent(ResourcesComponents record) throws Exception {
        // Main json object
        JSONObject json = new JSONObject();

        // add the AT database Id as an external ID
        addExternalId(record, json, "resource_component");

        /* Add fields needed for abstract_archival_object.rb */
        boolean publish = !record.getInternalOnly();
        json.put("publish", publish);

        // check to make sure we have a title
        String title = record.getTitle();
        json.put("title", title);

        json.put("repository_processing_note", record.getRepositoryProcessingNote());

        // add the language code
        addLanguageCode(json, record.getLanguageCode());
        addLangMaterials(json, record.getLanguageCode(), false);

        // add the date array containing the date json objects
        JSONArray dateJA = new JSONArray();

        String recordIdentifier = "Resource Component: " + currentResourceRecordIdentifier + "/"  + record.getPersistentId();
        addDate(dateJA, record, "creation", recordIdentifier);

        Set<ArchDescriptionDates> archDescriptionDates = record.getArchDescriptionDates();
        convertArchDescriptionDates(dateJA, archDescriptionDates, recordIdentifier);

        if(dateJA.length() != 0) {
            json.put("dates", dateJA);
        } else if(title.isEmpty()) {
            json.put("title", "unspecified");
        }

        // add field required for archival_object.rb

        // see if to make the ref id unique, leave blank, or just use the original (default)
        String refId;

        if(aspaceCopyUtil.getRefIdOption().equalsIgnoreCase(ASpaceCopyUtil.REFID_UNIQUE)) {
            refId = record.getPersistentId() + "_" + randomString.nextString();
        } else if(aspaceCopyUtil.getRefIdOption().equalsIgnoreCase(ASpaceCopyUtil.REFID_NONE)) {
            refId = "";
        } else {
            refId = record.getPersistentId();
        }

        json.put("ref_id", refId);

        String level = (String) enumUtil.getASpaceArchivalObjectLevel(record.getLevel())[0];
        json.put("level", level);

        if(level.equals("otherlevel")) {
            json.put("other_level", fixEmptyString(record.getOtherLevel()));
        }

        if(record.getComponentUniqueIdentifier() != null && !record.getComponentUniqueIdentifier().isEmpty()) {
            json.put("component_id", record.getComponentUniqueIdentifier());
        }

        json.put("position", record.getSequenceNumber());

        // add restrictions apply
        if(record.getRestrictionsApply() != null && record.getRestrictionsApply()) {
            json.put("restrictions_apply", record.getRestrictionsApply());
        }

        // add the notes
        JSONArray notesJA = new JSONArray();
        addNotes(notesJA, record);
        json.put("notes", notesJA);

        // add the extent array containing one object or many depending if we using multiple extents
        JSONArray extentJA = new JSONArray();
        JSONObject extentJS = new JSONObject();

        if(!record.getExtentType().isEmpty()) {
            extentJS.put("portion", "whole");

            if(record.getExtentNumber() != null) {
                extentJS.put("number", removeTrailingZero(record.getExtentNumber()));
            } else {
                extentJS.put("number", "0");
            }

            extentJS.put("extent_type", enumUtil.getASpaceExtentType(record.getExtentType())[0]);
            extentJS.put("container_summary", record.getContainerSummary());
            extentJA.put(extentJS);
        } else if(!record.getContainerSummary().isEmpty()) {
            // some groups only put information in the container summary field
            // so place this has a note
            addNoteForContainerSummary(notesJA, record.getContainerSummary(), publish);
        }

        Set<ArchDescriptionPhysicalDescriptions> physicalDescriptions = record.getPhysicalDesctiptions();
        convertPhysicalDescriptions(extentJA, physicalDescriptions);

        if(extentJA.length() > 0) {
            if(extentJA.length() > 1 && extentPortionInParts) {
                extentJS.put("portion", "part");
            }

            json.put("extents", extentJA);
        }

        return json;
    }

    /**
     * method to convert an AT assessment to an AS assessment
     * @param record
     * @return
     * @throws Exception
     */
    private String convertAssessment(Assessments record) throws Exception {

        JSONObject json = new JSONObject();

        addExternalId(record, json, "assessment");

        // add the name for who did the survey. If empty use name of user who created record
        print("Adding agent record for who did survey ...");
        String name = record.getWhoDidSurvey();
        if (name == null || name.isEmpty()) name = record.getCreatedBy();
        json.put("surveyed_by", addAssessmentsAgent(name, record));

        Date surveyBegin = record.getDateOfSurvey();
        //use the date the assessment record was created if the date of survey is not specified
        if (surveyBegin == null) surveyBegin = record.getCreated();
        if (surveyBegin == null) surveyBegin = DEFAULT_DATE;
        String surveyBeginStr = new SimpleDateFormat("yyyy-MM-dd").format(surveyBegin);
        json.put("survey_begin", surveyBeginStr);

        Double duration = record.getAmountOfTimeSurveyTook();
        if (duration != null) json.put("surveyed_duration", duration + " hours");

        Double extent = record.getTotalExtent();
        if (extent != null) json.put("surveyed_extent", extent + " feet");

        json.put("review_required", record.getReviewNeeded());

        String reviewer = record.getWhoNeedsToReview();
        if (reviewer != null && !(reviewer.isEmpty())) json.put("reviewer", addAssessmentsAgent(reviewer, record));

        json.put("review_note", record.getReviewNote());

        json.put("inactive", record.getInactive());

        json.put("general_assessment_note", record.getGeneralNote());
        json.put("special_format_note", record.getSpecialFormatNote());
        json.put("exhibition_value_note", record.getExhibitionValueNote());

        Double monetaryValue = record.getMonetaryValue();
        if (monetaryValue != null) json.put("monetary_value", monetaryValue.toString());
        json.put("monetary_value_note", record.getMonetaryValueNote());

        json.put("conservation_note", record.getConservationNote());

        return json.toString();
    }

    /**
     * method to add the linked records with matching repository to an assessment as well as add the assessment attributes
     * @param jsonText
     * @param record
     * @return
     * @throws Exception
     */
    public String addAssessmentsRepoSpecificInfo(String jsonText, Assessments record, String repoURI) throws Exception {

        JSONObject json = new JSONObject(jsonText);

        JSONArray recordsJA = new JSONArray();

        String recordUri;

        // add linked accessions from the correct repository
        for (AssessmentsAccessions accession : record.getAccessions()) {
            Accessions accessionRecord = accession.getAccession();
            String recordRepo = aspaceCopyUtil.getRemappedRepositoryURI("accession",
                    accessionRecord.getIdentifier(), accessionRecord.getRepository());
            if (recordRepo.equals(repoURI)) {
                recordUri = aspaceCopyUtil.getURIMapping(accessionRecord);
                recordsJA.put(getReferenceObject(recordUri));
            }
        }

        // add linked digital objects from correct repository
        for (AssessmentsDigitalObjects digitalObject: record.getDigitalObjects()) {
            DigitalObjects digitalObjectRecord = digitalObject.getDigitalObject();
            String recordRepo = aspaceCopyUtil.getRemappedRepositoryURI("digitalObject",
                    digitalObjectRecord.getIdentifier(), digitalObjectRecord.getRepository());
            if (recordRepo.equals(repoURI)) {
                recordUri = aspaceCopyUtil.getURIMapping(digitalObjectRecord);
                recordsJA.put(getReferenceObject(recordUri));
            }
        }

        // add resources from the correct repository
        for (AssessmentsResources resource : record.getResources()) {
            Resources resourceRecord = resource.getResource();
            String recordRepo = aspaceCopyUtil.getRemappedRepositoryURI("resource",
                    resourceRecord.getIdentifier(), resourceRecord.getRepository());
            if (recordRepo.equals(repoURI)) {
                recordUri = aspaceCopyUtil.getURIMapping(resourceRecord);
                recordsJA.put(getReferenceObject(recordUri));
            }
        }

        json.put("records", recordsJA);

        // also add the assessment attributes since they also must be scoped by repository
        addAssessmentsAttributes(json, record, repoURI);

        return json.toString();
    }

    /**
     * adds an ASpace agent and gives a reference to it to be used as an assessment agent
     * @param name
     * @param assessment
     * @return
     * @throws Exception
     */
    private JSONArray addAssessmentsAgent(String name, Assessments assessment) throws Exception {

        JSONObject json = new JSONObject();

        JSONArray namesJA = new JSONArray();
        JSONObject nameJSON = new JSONObject();

        name = name.trim();
        boolean unknown = false;
        if (name.isEmpty()) {
            //if a default name 'unknown' has already been added use that instead of adding again
            if (unknownName != null) return unknownName;
            name = "unknown";
            unknown = true;
        }

        JSONObject matchingUserAgentRef = aspaceCopyUtil.getMatchingUserAgent(name);
        if (matchingUserAgentRef != null) {
            return new JSONArray().put(matchingUserAgentRef);
        }

        //map the name to an ASpace agent
        nameJSON.put("primary_name", name);
        nameJSON.put("sort_name", name);
        nameJSON.put("source", enumUtil.getASpaceNameSource("local")[0]);
        nameJSON.put("name_order", enumUtil.getASpaceNameOrder(null)[0]);
        namesJA.put(nameJSON);
        json.put("names", namesJA);

        json.put("publish", false);
        json.put("agent_type", "agent_person");

        //save the agent to ASpace
        String endpoint = "/agents/people";
        String id = aspaceCopyUtil.saveRecord(endpoint , json.toString(), "Assessments->" + assessment.getIdentifier());

        JSONArray ja = new JSONArray();
        if (!id.equals("no id assigned")) {
            //return a JSONArray with a reference to the agent
            String uri = endpoint + "/" + id;
            aspaceCopyUtil.addAssessmentsNameToUserURIMap(name, uri);
            ja.put(getReferenceObject(uri));
            if (unknown) unknownName = ja;
        }
        return ja;
    }

    /**
     * add assessment attributes to an assessment
     * @param json
     * @param record
     * @param repo
     * @throws Exception
     */
    private void addAssessmentsAttributes(JSONObject json, Assessments record, String repo) throws Exception {

        //first add the formats
        JSONArray formatsJA = new JSONArray();
        HashMap<String, Boolean> formats = new HashMap<String, Boolean>();
        formats.put("Architectural Materials", record.getArchitecturalMaterials());
        formats.put("Glass", record.getGlass());
        formats.put("Art Originals", record.getArtOriginals());
        formats.put("Photographs", record.getPhotographs());
        formats.put("Artifacts", record.getArtifacts());
        formats.put("Scrapbooks", record.getScrapbooks());
        formats.put("Audio Materials", record.getAudioMaterials());
        formats.put("Technical Drawings & Schematics", record.getTechnicalDrawingsAndSchematics());
        formats.put("Biological Specimens", record.getBiologicalSpecimens());
        formats.put("Textiles", record.getTextiles());
        formats.put("Botanical Specimens", record.getBotanicalSpecimens());
        formats.put("Vellum & Parchment", record.getVellumAndParchment());
        formats.put("Computer Storage Units", record.getComputerStorageUnits());
        formats.put("Video Materials", record.getVideoMaterials());
        formats.put("Film (negative, slide, or motion picture)", record.getFilm());
        formats.put("Other", record.getOther());
        formats.put("Special Format 1", record.getSpecialFormat1());
        formats.put("Special Format 2", record.getSpecialFormat2());

        for (String format : formats.keySet()) {
            if (formats.get(format)) {
                JSONObject formatJSON = new JSONObject();
                int id = aspaceCopyUtil.getAssessmentAttributeID(repo, format, "format");
                formatJSON.put("definition_id", id);
                formatJSON.put("value", "true");
                formatsJA.put(formatJSON);
            }
        }

        json.put("formats", formatsJA);

        //next the conservation issues
        JSONArray conservationJA = new JSONArray();
        HashMap<String, Boolean> conservationIssues = new HashMap<String, Boolean>();
        conservationIssues.put("Potential Mold or Mold Damage", record.getPotentialMoldOrMoldDamage());
        conservationIssues.put("Recent Pest Damage", record.getRecentPestDamage());
        conservationIssues.put("Deteriorating Film Base", record.getDeterioratingFilmBase());
        conservationIssues.put("Special Conservation Issue 1", record.getSpecialConservationIssue1());
        conservationIssues.put("Special Conservation Issue 2", record.getSpecialConservationIssue2());
        conservationIssues.put("Special Conservation Issue 3", record.getSpecialConservationIssue3());
        conservationIssues.put("Brittle Paper", record.getBrittlePaper());
        conservationIssues.put("Metal Fasteners", record.getMetalFasteners());
        conservationIssues.put("Newspaper", record.getNewspaper());
        conservationIssues.put("Tape", record.getTape());
        conservationIssues.put("Thermofax Paper", record.getThermofaxPaper());
        conservationIssues.put("Other Conservation Issue 1", record.getOtherConservationIssue1());
        conservationIssues.put("Other Conservation Issue 2", record.getOtherConservationIssue2());
        conservationIssues.put("Other Conservation Issue 3", record.getOtherConservationIssue3());

        for (String conservationIssue : conservationIssues.keySet()) {
            if (conservationIssues.get(conservationIssue)) {
                JSONObject conservationJSON = new JSONObject();
                int id = aspaceCopyUtil.getAssessmentAttributeID(repo, conservationIssue, "conservation_issue");
                conservationJSON.put("definition_id", id);
                conservationJSON.put("value", "true");
                conservationJA.put(conservationJSON);
            }
        }

        json.put("conservation_issues", conservationJA);

        //finally the ratings
        JSONArray ratingsJA = new JSONArray();
        HashMap<String, Integer> ratings = new HashMap<String, Integer>();
        ratings.put("Physical Condition", record.getConditionOfMaterial());
        ratings.put("Physical Access (arrangement)", record.getPhysicalAccess());
        ratings.put("Documentation Quality", record.getDocumentationQuality());
        ratings.put("Housing Quality", record.getQualityOfHousing());
        ratings.put("Intellectual Access (description)", record.getIntellectualAccess());
        ratings.put("Interest", record.getInterest());
        ratings.put("Numerical Rating 1", record.getUserNumericalRating1());
        ratings.put("Numerical Rating 2", record.getUserNumericalRating2());

        for (String rating : ratings.keySet()) {
            Integer value = ratings.get(rating);
            if (value != null) {
                JSONObject ratingJSON = new JSONObject();
                int id = aspaceCopyUtil.getAssessmentAttributeID(repo, rating, "rating");
                ratingJSON.put("definition_id", id);
                ratingJSON.put("value", value.toString());
                ratingsJA.put(ratingJSON);
            }
        }

        json.put("ratings", ratingsJA);
    }

    /**
     * Method to set the language code for a json record
     *
     * @param json
     * @param languageCode
     * @throws Exception
     */
    public void addLanguageCode(JSONObject json, String languageCode) throws Exception {
        if(languageCode != null && !languageCode.isEmpty()) {
            json.put("language", enumUtil.getASpaceLanguageCode(languageCode));
        }
    }

    /**
     * Method to set the lang_materials for a json record if it is required
     * or applicable.
     *
     * @param json
     * @param languageCode
     * @param required
     * @throws Exception
     */
    public void addLangMaterials(JSONObject json, String languageCode, boolean required) throws Exception {
        if (required || (languageCode != null && !languageCode.isEmpty())) {
            JSONArray langMaterialsJA = new JSONArray();
            JSONObject langMaterialsJS = new JSONObject();
            JSONObject languageAndScriptJS = new JSONObject();

            languageAndScriptJS.put("language", enumUtil.getASpaceLanguageCode(languageCode));
            langMaterialsJS.put("language_and_script", languageAndScriptJS);
            langMaterialsJA.put(langMaterialsJS);
            json.put("lang_materials", langMaterialsJA);
        }
    }

    /**
     * Method to add a date json object
     *
     * @param dateJA
     * @param record
     */
    public void addDate(JSONArray dateJA, ArchDescription record, String dateLabel, String recordIdentifier) throws Exception {
        JSONObject dateJS = new JSONObject();

        dateJS.put("date_type", "single");

        dateJS.put("label", dateLabel);

        String dateExpression = record.getDateExpression();
        dateJS.put("expression", dateExpression);

        Integer dateBegin = record.getDateBegin();
        Integer dateEnd = record.getDateEnd();

        if (dateBegin == null && record instanceof Resources) {dateBegin = 0;}

        if (dateBegin != null) {
            dateJS.put("date_type", "inclusive");

            dateJS.put("begin", dateBegin.toString());

            if (dateEnd != null) {
                if(dateEnd >= dateBegin) {
                    dateJS.put("end", dateEnd.toString());
                } else {
                    dateJS.put("end", dateBegin.toString());

                    String message = "End date: " + dateEnd + " before begin date: " + dateBegin + ", ignoring end date\nRecord:: " + recordIdentifier + "\n";
                    aspaceCopyUtil.addErrorMessage(message);
                }
            } else {
                dateJS.put("end", dateBegin.toString());
            }

            // DEBUG Code to store all dates then print them put
            if(checkISODates) {
                String begin = normalizeISODate(dateJS.getString("begin"), recordIdentifier);
                String end = normalizeISODate(dateJS.getString("end"), recordIdentifier);
                datesList.add(begin + "/" + end + "/" + recordIdentifier);
            }
        }

        // see if to add this date now
        if((dateExpression != null && !dateExpression.isEmpty()) || dateBegin != null) {
            dateJA.put(dateJS);
        }

        // add the bulk dates begin and end if resource or resource component
        if(record instanceof AccessionsResourcesCommon) {
            AccessionsResourcesCommon resourcesCommon = (AccessionsResourcesCommon)record;
            Integer bulkDateBegin = resourcesCommon.getBulkDateBegin();
            Integer bulkDateEnd = resourcesCommon.getBulkDateEnd();

            if(bulkDateBegin != null) {
                dateJS = new JSONObject();
                dateJS.put("date_type", "bulk");
                dateJS.put("label", dateLabel);

                dateJS.put("begin", bulkDateBegin.toString());

                if (bulkDateEnd != null) {
                    if(bulkDateEnd >= bulkDateBegin) {
                        dateJS.put("end", bulkDateEnd.toString());
                    } else {
                        dateJS.put("end", bulkDateBegin.toString());

                        String message = "Bulk end date: " + bulkDateEnd + " before bulk begin date: " + bulkDateBegin + ", ignoring end date\n" + recordIdentifier;
                        aspaceCopyUtil.addErrorMessage(message);
                    }
                }

                dateJA.put(dateJS);

                // Debug code
                if(checkISODates) {
                    String begin = normalizeISODate(dateJS.getString("begin"), recordIdentifier);
                    String end = normalizeISODate(dateJS.getString("end"), recordIdentifier);
                    datesList.add(begin + "/" + end + "/" + recordIdentifier);
                }
            }
        }
    }

    /**
     * Method to add a date array to an agent record
     *
     * @param recordJS
     * @param record
     */
    public void addNameDate(JSONObject recordJS, String label, BasicNames record, String dateField) throws Exception {
        JSONArray dateJA = new JSONArray();
        JSONObject dateJS = new JSONObject();

        // decide what date type we have
        dateJS.put("date_type", "single");
        dateJS.put("label", label);

        String dateExpression = record.getPersonalDates().trim();

        // we may have a date range so check for that. yyyy-yyyy
        if (dateExpression.matches("\\d{4}\\s*-\\s*\\d{4}")) {
            String[] sa = dateExpression.split("\\s*-\\s*");
            Integer dateBegin = Integer.parseInt(sa[0]);
            Integer dateEnd = Integer.parseInt(sa[1]);

            dateJS.put("date_type", "range");

            dateJS.put("begin", dateBegin.toString());

            if (dateEnd >= dateBegin) {
                dateJS.put("end", dateEnd.toString());
            } else {
                dateJS.put("begin", dateEnd.toString());
                dateJS.put("end", dateBegin.toString());

                String message = "End date: " + dateEnd + " before begin date: " + dateBegin
                        + ", swapping begin and end.\nRecord:: " + record.getSortName() + "\n";
                aspaceCopyUtil.addErrorMessage(message);
            }
        } else {
            dateJS.put("expression", dateExpression);
        }

        dateJA.put(dateJS);

        recordJS.put(dateField, dateJA);
    }

    /**
     * Method to concat all the contact notes into a single note and add it to the
     * contactJS object.  This has the potential to throw a truncation error
     *
     * @param contactJS
     * @param contactNotes
     */
    public void addNote(JSONObject contactJS, Set<NameContactNotes> contactNotes) throws JSONException {
         if(contactNotes != null && contactNotes.size() > 0) {
             StringBuilder sb = new StringBuilder();

             for(NameContactNotes contactNote: contactNotes) {
                 String label = contactNote.getLabel();
                 String content = contactNote.getNoteText();

                 if(!label.isEmpty()) {
                     sb.append("Label: ").append(label).append("\n");
                 }

                 sb.append("Content: \n").append(content).append("\n\n");
             }

             contactJS.put("note", sb.toString());
         }
    }

    /**
     * Method to add a bioghist note agent object
     *
     * @param agentJS
     * @param record
     * @throws Exception
     */
    public void addNote(JSONObject agentJS, Names record) throws Exception {
        if (record.getDescriptionNote().isEmpty()) return;

        JSONArray notesJA = new JSONArray();
        JSONObject noteJS = new JSONObject();

        noteJS.put("jsonmodel_type", "note_bioghist");

        JSONArray subnotesJA = new JSONArray();

        JSONObject textNoteJS = new JSONObject();
        addTextNote(textNoteJS, record.getDescriptionNote());
        subnotesJA.put(textNoteJS);

        // add a subnote which holds the citation information
        if(record.getCitation() != null && !record.getCitation().isEmpty()) {
            JSONObject citationJS = new JSONObject();
            citationJS.put("jsonmodel_type", "note_citation");
            citationJS.put("publish", publishHashMap.get("names"));
            JSONArray contentJA = new JSONArray();
            contentJA.put(record.getCitation());
            citationJS.put("content", contentJA);
            subnotesJA.put(citationJS);
        }

        noteJS.put("subnotes", subnotesJA);
        notesJA.put(noteJS);
        agentJS.put("notes", notesJA);
    }

    /**
     * Method to add Notes to a json array
     *
     * @param notesJA
     * @param record
     */
    public void addNotes(JSONArray notesJA, ArchDescription record) throws Exception {
        // split the repeating data into the different note types
        Set<ArchDescriptionNotes> notes = new TreeSet<ArchDescriptionNotes>();
        Set<ArchDescriptionStructuredData> structuredNotes = new TreeSet<ArchDescriptionStructuredData>();

        Set<ArchDescriptionRepeatingData> repeatingDataSet = record.getRepeatingData();

        for(ArchDescriptionRepeatingData repeatingData: repeatingDataSet) {
            if(repeatingData instanceof ArchDescriptionNotes) {
                notes.add((ArchDescriptionNotes)repeatingData);
            } else {
                structuredNotes.add((ArchDescriptionStructuredData)repeatingData);
            }
        }

        // process the none structured notes
        for(ArchDescriptionNotes note: notes) {
            // check to see if we are using a mapper script to filter some records
            if(runNoteMapperScript && !canCopyRecord(note)) {
                print("Mapper Script -- Not Copying Note: " + note);
                continue;
            }

            // add the content for abstract_note.rb
            JSONObject noteJS = new JSONObject();

            noteJS.put("label", note.getTitle());
            noteJS.put("publish", !note.getInternalOnly());

            // based on the note and record type, add the correct note
            String noteType = "";
            if(note.getNotesEtcType() != null) {
                noteType = note.getNotesEtcType().getNotesEtcName();
            }

            // create a content array in case we need need it for a note
            JSONArray contentJA = new JSONArray();
            contentJA.put(fixEmptyString(note.getContent(), "no content"));

            if(record instanceof DigitalObjects) {
                noteJS.put("jsonmodel_type", "note_digital_object");
                noteType = (String) enumUtil.getASpaceDigitalObjectNoteType(noteType)[0];
                noteJS.put("type", noteType);
                noteJS.put("content", contentJA);
            } else if(note.getMultiPart() != null && note.getMultiPart()) {
                addMultiPartNote(noteJS, note);
            } else {
                // even though it could be a single part note, based on the type it
                // needs to be a multi part note in ASpace
                String noteTypeMapped = (String) enumUtil.getASpaceSinglePartNoteType(noteType)[0];

                if(noteTypeMapped.equals("abstract") && !noteType.toLowerCase().contains("abstract")) {
                    addMultiPartNote(noteJS, note);
                } else {
                    noteJS.put("jsonmodel_type", "note_singlepart");
                    noteJS.put("type", noteTypeMapped);
                    noteJS.put("content", contentJA);
                }
            }

            notesJA.put(noteJS);
        }

        // process the structured notes
        for(ArchDescriptionStructuredData note: structuredNotes) {
            // check to see if we are using a mapper script to filter some records
            if(runNoteMapperScript && !canCopyRecord(note)) {
                print("Mapper Script -- Not Copying Structured Note: " + note);
                continue;
            }

            // add the content for abstract_note.rb
            JSONObject noteJS = new JSONObject();

            noteJS.put("label", note.getTitle());

            if(note.getInternalOnly() != null) {
                noteJS.put("publish", !note.getInternalOnly());
            } else {
                noteJS.put("publish", true);
            }

            // se if to add any content
            if(note.getContent() != null && !note.getContent().isEmpty()) {
                JSONArray contentJA = new JSONArray();
                contentJA.put(note.getContent());
                noteJS.put("content", contentJA);
            }

            if(note instanceof Bibliography) {
                addBibliographyNote(noteJS, (Bibliography)note);
            } else if(note instanceof Index) {
                addIndexNote(noteJS, (Index)note);
            }

            notesJA.put(noteJS);
        }
    }

     /**
     * Add a multipart note
     *
     * @param noteJS
     * @param note
     * @throws Exception
     */
    private void addMultiPartNote(JSONObject noteJS, ArchDescriptionNotes note) throws Exception {
        // get the note type
        String noteType = "";
        if(note.getNotesEtcType() != null) {
            noteType = note.getNotesEtcType().getNotesEtcName();
        }

        // create the parent json object of this note
        noteJS.put("jsonmodel_type", "note_multipart");
        noteJS.put("type", enumUtil.getASpaceMultiPartNoteType(noteType)[0]);

        JSONArray subnotesJA = new JSONArray();

        // if there is note content add it has a text note
        if(note.getContent() != null && !note.getContent().isEmpty()) {
            JSONObject textNoteJS = new JSONObject();
            textNoteJS.put("publish", noteJS.get("publish"));
            addTextNote(textNoteJS, note.getContent());
            subnotesJA.put(textNoteJS);
        }

        // add the sub notes now
        for(ArchDescriptionRepeatingData childNote: note.getChildren()) {
            JSONObject subnoteJS = new JSONObject();
            subnoteJS.put("publish", noteJS.get("publish"));

            if(childNote instanceof Bibliography) {
                addBibliographyNote(subnoteJS, (Bibliography)childNote);
            } else if(childNote instanceof ChronologyList) {
                addChronologyNote(subnoteJS, (ChronologyList)childNote);
            } else if(childNote instanceof Index) {
                addIndexNote(subnoteJS, (Index)childNote);
            } else if(childNote instanceof ListOrdered) {
                addOrderedListNote(subnoteJS, (ListOrdered)childNote);
            } else if(childNote instanceof ListDefinition) {
                addDefinedListNote(subnoteJS, (ListDefinition)childNote);
            } else { // must be text note
                // This is a check to address AR-1150 in which empty notes were being copied over and
                // causing the parent resource record no migrate
                if(childNote.getContent() != null && !childNote.getContent().isEmpty()) {
                    addTextNote(subnoteJS, childNote.getContent());
                } else {
                    String message = "Empty sub-note with database ID: " + childNote.getIdentifier() + "\n";
                    aspaceCopyUtil.addErrorMessage(message);
                    continue;
                }
            }

            subnotesJA.put(subnoteJS);
        }

        // if there are no subnotes add a dummy note so record can save
        if(subnotesJA.length() == 0) {
            JSONObject textNoteJS = new JSONObject();
            textNoteJS.put("publish", noteJS.get("publish"));
            addTextNote(textNoteJS, "No Subnote Content");
            subnotesJA.put(textNoteJS);
        }

        noteJS.put("subnotes", subnotesJA);
    }

    /**
     * Method to add an ordered list note
     *
     * @param noteJS
     * @param listOrdered
     * @throws Exception
     */
    private void addOrderedListNote(JSONObject noteJS, ListOrdered listOrdered) throws Exception {
        noteJS.put("jsonmodel_type", "note_orderedlist");
        noteJS.put("title", fixEmptyString(listOrdered.getTitle(), "Missing Title"));
        noteJS.put("enumeration", enumUtil.getASpaceOrderedListNoteEnumeration(listOrdered.getNumeration()));

        JSONArray itemsJA = new JSONArray();

        for(ArchDescriptionStructuredDataItems item: listOrdered.getListItems()) {
            ListOrderedItems listItem = (ListOrderedItems)item;
            itemsJA.put(listItem.getItemValue());
        }

        noteJS.put("items", itemsJA);
    }

    /**
     * Method to add a defined list note
     *
     * @param noteJS
     * @param listDefinition
     * @throws Exception
     */
    private void addDefinedListNote(JSONObject noteJS, ListDefinition listDefinition) throws Exception {
        noteJS.put("jsonmodel_type", "note_definedlist");
        noteJS.put("title", fixEmptyString(listDefinition.getTitle(), "Missing Title"));

        JSONArray itemsJA = new JSONArray();

        for(ArchDescriptionStructuredDataItems item: listDefinition.getListItems()) {
            ListDefinitionItems listItem = (ListDefinitionItems)item;
            JSONObject itemJS = new JSONObject();

            itemJS.put("label", listItem.getLabel());
            itemJS.put("value", listItem.getItemValue());

            itemsJA.put(itemJS);
        }

        noteJS.put("items", itemsJA);
    }

    /**
     * Method to add a chronology note
     * @param noteJS
     * @throws Exception
     */
    private void addChronologyNote(JSONObject noteJS, ChronologyList chronologyList) throws Exception {
        noteJS.put("jsonmodel_type", "note_chronology");
        noteJS.put("title", fixEmptyString(chronologyList.getTitle(), "Missing Title"));

        noteJS.put("ingest_problem", chronologyList.getEadIngestProblem());

        JSONArray itemsJA = new JSONArray();

        for(ArchDescriptionStructuredDataItems item: chronologyList.getChronologyItems()) {
            ChronologyItems chronologyItem = (ChronologyItems)item;
            JSONObject itemJS = new JSONObject();

            itemJS.put("event_date", chronologyItem.getEventDate());

            // add the individual events now
            JSONArray eventsJA = new JSONArray();
            for(Events event: chronologyItem.getEvents()) {
                eventsJA.put(event.getEventDescription());
            }

            itemJS.put("events", eventsJA);
            itemsJA.put(itemJS);
        }

        noteJS.put("items", itemsJA);
    }

    /**
     * Method to add a bibliography note
     *
     * @param noteJS
     * @param note
     * @throws Exception
     */
    private void addBibliographyNote(JSONObject noteJS, Bibliography note) throws Exception {
        noteJS.put("jsonmodel_type", "note_bibliography");
        noteJS.put("type", "bibliography");

        // add the note items
        JSONArray itemsJA = new JSONArray();

        for(ArchDescriptionStructuredDataItems item: note.getBibItems()) {
            BibItems bibItems = (BibItems)item;
            itemsJA.put(bibItems.getItemValue());
        }

        noteJS.put("items", itemsJA);
    }

    /**
     * Method to add an index note
     *
     * @param noteJS
     * @param index
     */
    private void addIndexNote(JSONObject noteJS, Index index) throws Exception {
        noteJS.put("jsonmodel_type", "note_index");
        noteJS.put("type", "index");

        JSONArray itemsJA = new JSONArray();

        for(ArchDescriptionStructuredDataItems item: index.getIndexItems()) {
            IndexItems indexItem = (IndexItems)item;
            JSONObject itemJS = new JSONObject();
            itemJS.put("jsonmodel_type", "note_index_item");

            itemJS.put("value", indexItem.getItemValue());
            itemJS.put("type", enumUtil.getASpaceIndexItemType(indexItem.getItemType())[0]);
            itemJS.put("reference", indexItem.getReference());
            itemJS.put("reference_text", indexItem.getReferenceText());

            itemsJA.put(itemJS);
        }

        noteJS.put("items", itemsJA);
    }

    /**
     * Method to add a text note
     *
     * @param noteJS
     * @param content
     * @throws Exception
     */
    private void addTextNote(JSONObject noteJS, String content) throws Exception {
        noteJS.put("jsonmodel_type", "note_text");
        noteJS.put("content", content);
    }

    /**
     * Method to add a physical description note for container summary
     *
     * @param notesJA
     * @param containerSummary
     */
    private void addNoteForContainerSummary(JSONArray notesJA, String containerSummary, boolean publish) throws Exception {
        JSONObject noteJS = new JSONObject();
        noteJS.put("publish", publish);

        noteJS.put("label", "Container Summary");


        JSONArray contentJA = new JSONArray();
        contentJA.put(containerSummary);

        noteJS.put("jsonmodel_type", "note_singlepart");
        noteJS.put("type", "physdesc");
        noteJS.put("content", contentJA);

        notesJA.put(noteJS);
    }

    /**
     * Method to convert an analog instance to an equivalent ASpace instance
     *
     * @param analogInstance
     * @param locationURI
     * @return
     * @throws Exception
     */
    public JSONObject convertAnalogInstance(ArchDescriptionAnalogInstances analogInstance, String locationURI,
                                            String parentRepoURI) throws Exception {
        // check to see if you have an empty instance
        if(analogInstance.getInstanceType() == null || analogInstance.getInstanceType().trim().isEmpty()) {
            return null;
        }

        JSONObject instanceJS = new JSONObject();

        // add the AT database Id as an external ID
        addExternalId(analogInstance, instanceJS, "analog_instance");

        // set the type
        String type = (String) enumUtil.getASpaceInstanceType(analogInstance.getInstanceType())[0];
        instanceJS.put("instance_type", type);

        // add the container now
        JSONObject containerJS = new JSONObject();

        //add the top container
        TopContainerMapper topContainer = new TopContainerMapper(analogInstance, parentRepoURI);
        topContainer.addLocationURI(locationURI);
        containerJS.put("top_container", getReferenceObject(topContainer.getRef()));

        //now for the sub-container
        String type2 = analogInstance.getContainer2Type();
        String indicator2 = analogInstance.getContainer2Indicator();
        String type3 = analogInstance.getContainer3Type();
        String indicator3 = analogInstance.getContainer3Indicator();

        boolean have2 = !(type2 == null || type2.isEmpty()) || !(indicator2 == null || indicator2.isEmpty());
        boolean have3 = !(type3 == null || type3.isEmpty()) || !(indicator3 == null || indicator3.isEmpty());

        //add container 2 type and indicator - must be done if there is a container 3 because 2 is required to create 3
        if(have2 || have3) {
            containerJS.put("type_2", enumUtil.getASpaceSubContainerType(type2)[0]);
            if (indicator2 == null || indicator2.isEmpty()) indicator2 = "unknown container";
            containerJS.put("indicator_2", indicator2);
        }

        //add container 3
        if(have3) {
            containerJS.put("type_3", enumUtil.getASpaceSubContainerType(type3)[0]);
            if (indicator3 == null || indicator3.isEmpty()) indicator3 = "unknown container";
            containerJS.put("indicator_3", indicator3);
        }

        // TODO 4/16/2013 add the user defined fields
        //addUserDefinedFields(containerJS, analogInstance);

        instanceJS.put("sub_container", containerJS);

        return instanceJS;
    }

    /**
     * Method to convert a digital instance to a json record
     *
     * @param digitalObjectURI
     * @return
     * @throws Exception
     */
    public JSONObject convertDigitalInstance(String digitalObjectURI) throws Exception {
        JSONObject instanceJS = new JSONObject();

        if(digitalObjectURI == null || digitalObjectURI.isEmpty()) return null;

        instanceJS.put("instance_type", "digital_object");
        instanceJS.put("digital_object", getReferenceObject(digitalObjectURI));

        return instanceJS;
    }

    /**
     * Method to create a dummy instance to hold the location information for an accession
     *
     * @param locationNote
     * @return
     * @throws Exception
     */
    public JSONObject createAccessionInstance(Accessions accession, String locationURI, String locationNote,
                                              String parentRepoURI, AccessionsLocations location) throws Exception {
        JSONObject instanceJS = new JSONObject();

        // set the type
        instanceJS.put("instance_type", "accession");

        // add the container now
        JSONObject containerJS = new JSONObject();

        // create a top container or get it if a equivalent one exists
        TopContainerMapper topContainer = new TopContainerMapper(location, accession, parentRepoURI);
        topContainer.addLocationURI(locationURI, locationNote);
        containerJS.put("top_container", getReferenceObject(topContainer.getRef()));

        instanceJS.put("sub_container", containerJS);

        return instanceJS;
    }

    /**
     * Method to get a reference object which points to another URI
     *
     * @param recordURI
     * @return
     * @throws Exception
     */
    public static JSONObject getReferenceObject(String recordURI) throws Exception {
        JSONObject referenceJS = new JSONObject();
        referenceJS.put("ref", recordURI);
        return referenceJS;
    }

    /**
     * Method to add the AT internal database ID as an external ID for the ASpace object
     *
     * @param record
     * @param source
     */
    public static void addExternalId(DomainObject record, JSONObject recordJS, String source) throws Exception {
        source = "Archivists Toolkit Database::" + source.toUpperCase();

        JSONArray externalIdsJA = new JSONArray();
        JSONObject externalIdJS = new JSONObject();

        externalIdJS.put("external_id", record.getIdentifier().toString());
        externalIdJS.put("source", source);

        externalIdsJA.put(externalIdJS);

        recordJS.put("external_ids", externalIdsJA);
    }

    /**
     * Method to set the hash map that holds the dynamic enums
     *
     * @param dynamicEnums
     */
    public void setASpaceDynamicEnums(HashMap<String, JSONObject> dynamicEnums) {
        enumUtil.setASpaceDynamicEnums(dynamicEnums);
    }

    /**
     * set whether to return the AT value or UNMAPPED default
     *
     * @param value
     */
    public void setReturnATValue(boolean value) {
    }

    /**
     * This method is used to map AT lookup list values into a dynamic enum.
     *
     * @param lookupList
     * @return
     */
    public JSONObject mapLookList(LookupList lookupList, ArrayList<String> additional) throws Exception {
        // first we get the correct dynamic enum based on list. If it null then we just return null
        JSONObject dynamicEnumJS = enumUtil.getDynamicEnum(lookupList.getListName());

        if(dynamicEnumJS == null) return null;

        // add any values to this list if needed
        String enumListName = dynamicEnumJS.getString("name");
        JSONArray valuesJA = dynamicEnumJS.getJSONArray("values");

        HashSet<String> values = new HashSet<String>();

        for (LookupListItems lookupListItem: lookupList.getListItems()) {
            String atValue = lookupListItem.getListItem();
            String code = lookupListItem.getCode();

            // get the value it maps to and whether or not that value is already in ASpace
            Object[] mapped = enumUtil.mapsToASpaceEnumValue(enumListName, atValue, code);

            // if its not already in ASpace, add it
            if(!(Boolean) mapped[1]) {
                values.add((String) mapped[0]);
            }
        }

        // we may have additional items we want in ASpace as defaults that aren't in AT
        // add these values as needed
        for (String value: additional) {
            Object[] mapped = enumUtil.mapsToASpaceEnumValue(enumListName, value, "");
            if (!(Boolean) mapped[1]) {
                values.add((String) mapped[0]);
            }
        }

        for (String value: values) valuesJA.put(value);

        return dynamicEnumJS;
    }

    /**
     * Method to map the ASpace group to one or more AT access classes
     *
     * @param groupJS
     * @return
     */
    public void mapAccessClass(HashMap<String, JSONObject> repositoryGroupURIMap,
                               JSONObject groupJS, String repoURI) {
        try {
            String groupCode = groupJS.getString("group_code");
            String key = "";

            if (groupCode.equals("administrators")) { // map to access class 5
                key = repoURI + ACCESS_CLASS_PREFIX + "5";
                repositoryGroupURIMap.put(key, groupJS);
            } else if(groupCode.equals("repository-managers")) { // map to access class 4
                key = repoURI + ACCESS_CLASS_PREFIX + "4";
                repositoryGroupURIMap.put(key, groupJS);
            } else if(groupCode.equals("repository-archivists")) { // map to access class 3
                key = repoURI + ACCESS_CLASS_PREFIX + "3";
                repositoryGroupURIMap.put(key, groupJS);
            } else if(groupCode.equals("repository-advanced-data-entry")) { // map to access class 2
                key = repoURI + ACCESS_CLASS_PREFIX + "2";
                repositoryGroupURIMap.put(key, groupJS);
            } else if(groupCode.equals("repository-basic-data-entry")) { // map to access class 1
                key = repoURI + ACCESS_CLASS_PREFIX + "1";
                repositoryGroupURIMap.put(key, groupJS);
            } else if (groupCode.equals("repository-viewers")) { // map access class to access class 0 for now
                key = repoURI + ACCESS_CLASS_PREFIX + "0";
                repositoryGroupURIMap.put(key, groupJS);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to prepend http:// to a url to prevent ASpace from complaining
     *
     * @param url
     * @return
     */
    private String fixUrl(String url) {
        if(url.isEmpty()) return null;//"http://url.unspecified";

        String lowercaseUrl = url.toLowerCase();
        url = url.trim();

        // check to see if its a proper uri format
        if(lowercaseUrl.contains("://")) {
            return url;
        } else if(lowercaseUrl.startsWith("/")) {
            url = "file://" + url;
            return url;
        } else if(lowercaseUrl.contains(":\\")) {
            url = "file:///" + url;
            return url;
        } else {
            url = "http://" + url;
            return  url;
        }
    }

    /**
     * Method to set a string that's empty to "unspecified"
     * @param text
     * @return
     */
    public String fixEmptyString(String text) {
        return fixEmptyString(text, null);
    }

    /**
     * Method to set a string that empty to "not set"
     * @param text
     * @return
     */
    private String fixEmptyString(String text, String useInstead) {
        if(text == null || text.trim().isEmpty()) {
            if(useInstead == null) {
                return "unspecified";
            } else {
                return useInstead;
            }
        } else {
            return text;
        }
    }

    /**
     * Method to truncate a string to a certain length
     *
     * @param text
     * @param maxLength
     * @return
     */
    private String truncateString(String text, int maxLength) {
        if(!allowTruncation) {
            return text;
        } else if(text.length() <= maxLength) {
            return text;
        } else {
            return text.substring(0, (maxLength -3)) + "...";
        }
    }

    /**
     * Method to set the language codes
     *
     * @param languageCodes
     */
    public void setLanguageCodes(HashMap<String, String> languageCodes) {
        enumUtil.setLanguageCodes(languageCodes);
    }

    /**
     * Method to set the name link creator codes
     *
     * @param nameLinkCreatorCodes
     */
    public void setNameLinkCreatorCodes(HashMap<String, String> nameLinkCreatorCodes) {
        enumUtil.setNameLinkCreatorCodes(nameLinkCreatorCodes);
    }

    /**
     * This functions shift the ids so that we don't have any blanks. The ASpace backend
     * throws an error if there are any blanks.
     *
     * @param id1
     * @param id2
     * @param id3
     * @param id4
     * @return
     */
    private String[] cleanUpIds(String recordType, String id1, String id2, String id3, String id4) {
        String[] ids = new String[]{"","","",""};
        int index = 0;

        // keeps track of whether an ID was shifted
        boolean shifted = false;

        if(!id1.trim().isEmpty()) {
            ids[index] = id1;
            index++;
        }

        if(!id2.trim().isEmpty()) {
            if(index < 1) { shifted = true; }
            ids[index] = id2;
            index++;
        }

        if(!id3.trim().isEmpty()) {
            if(index < 2) { shifted = true; }
            ids[index] = id3;
            index++;
        }

        if(!id4.trim().isEmpty()) {
            if(index < 3) { shifted = true; }
            ids[index] = id4;
        }

        // check to see if this id is unique, if it isn't then make it so
        getUniqueID(recordType, concatIdParts(ids), ids);

        // report any corrections
        if(shifted) {
            String message;
            String fullId = concatIdParts(ids);

            if(recordType.equals(ASpaceClient.ACCESSION_ENDPOINT)) {
                message = "Accession Id Cleaned Up: " + fullId + "\n";
            } else { // must be a resource record
                message = "Resource Id Cleaned Up: " + fullId + "\n";
            }

            aspaceCopyUtil.addErrorMessage(message);
        }

        return ids;
    }

    /**
     * Method to concat the id parts in a string array into a full id delimited by "."
     *
     * @param ids
     * @return
     */
    private String concatIdParts(String[] ids) {
        String fullId = "";
        for(int i = 0; i < ids.length; i++) {
            if(!ids[i].isEmpty() && i == 0) {
                fullId += ids[0];
            } else if(!ids[i].isEmpty()) {
                fullId += "."  + ids[i];
            }
        }

        return fullId;
    }

    /**
     * Method to return a unique id, in cases where ASpace needs a unique id but AT doesn't
     *
     * @param endpoint
     * @param id
     * @return
     */
    private String getUniqueID(String endpoint, String id, String[] idParts) {
        // must check to make sure ID is not null
        if(id != null) {
            id = id.trim();
            id = id.toLowerCase();
        } else {
            id = "";
        }

        if(endpoint.equals(ASpaceClient.DIGITAL_OBJECT_ENDPOINT)) {
            // if id is empty add text
            if(id.isEmpty()) {
                id = "Digital Object ID ##"+ randomStringLong.nextString();
            }

            if(!digitalObjectIDs.contains(id)) {
                digitalObjectIDs.add(id);
            } else {
                String oldId = id;
                id += " ##" + randomStringLong.nextString();
                digitalObjectIDs.add(id);

                String message = "Duplicate Digital Object Id: "  + oldId  + " Changed to: " + id + "\n";
                aspaceCopyUtil.addErrorMessage(message);
            }

            return id;
        } else if(endpoint.equals(ASpaceClient.ACCESSION_ENDPOINT)) {
            String message = null;

            if(!accessionIDs.contains(id)) {
                accessionIDs.add(id);
            } else {
                String fullId = "";

                do {
                    idParts[0] += " ##" + randomString.nextString();
                    fullId = concatIdParts(idParts);
                } while(accessionIDs.contains(fullId));

                accessionIDs.add(fullId);

                message = "Duplicate Accession Id: "  + id  + " Changed to: " + fullId + "\n";
                aspaceCopyUtil.addErrorMessage(message);
            }

            // we don't need to return the new id here, since the idParts array
            // is being used to to store the new id
            return "not used";
        } else if(endpoint.equals(ASpaceClient.RESOURCE_ENDPOINT)) {

            resourceIDs.add(id);

            // we don't need to return the new id here, since the idParts array
            // is being used to to store the new id
            return "not used";
        } else if(endpoint.equals("ead")) {
            if(id.isEmpty()) {
                return "";
            }

            if(!eadIDs.contains(id)) {
                eadIDs.add(id);
            } else {
                String nid = "";

                do {
                    nid = id + " ##" + randomString.nextString();
                } while(eadIDs.contains(nid));

                eadIDs.add(nid);

                String message = "Duplicate EAD Id: "  + id  + " Changed to: " + nid + "\n";
                aspaceCopyUtil.addErrorMessage(message);

                // assign id to new id
                id = nid;
            }

            return id;
        } else {
            return id;
        }
    }

    /**
     * Method that takes an ISO date and normalizes it into the format yyyy-mm-dd
     *
     * @param date
     * @return
     */
    private String normalizeISODate(String date, String recordIdentifier) {
        // trim the date before testing
        date = date.trim();

        if(date.length() < 4) return null;

        if(date.matches("\\d{4}")) { // matches yyyy
            return date + "-01-01";
        } else if (date.matches("\\d{4}-\\d{2}")) { // matches yyyy-mm format
            String[] sa = date.split("-");
            return sa[0] + "-" + sa[1] + "-01";
        } else if (date.matches("\\d{4}-\\d{2}-\\d{2}")) {// matches yyyy-mm-dd
            return date;
        } else if(date.matches("\\d{8}")) { // match yyyymmdd
            return date.substring(0,4) + "-" + date.substring(4,6) + "-" + date.substring(6,8);
        } else { // return blank
            String message = "Invalid ISO date " + date + "\n Record:: " + recordIdentifier + "\n";
            aspaceCopyUtil.addErrorMessage(message);
            return null;
        }
    }

    /**
     * Method to check and see if the end date does not come before the begin date
     * by looking only the year, month, and day
     *
     *
     * @param begin
     * @param end
     * @param recordIdentifier
     * @return
     */
    private boolean endDateValid(String begin, String end, String recordIdentifier) {
        try {
            Date beginDate = sdf.parse(begin);
            Date endDate = sdf.parse(end);

            if(endDate.before(beginDate)) {
                String message = "End date: " + end + " before begin date: " + begin + ", ignoring end date.\nRecord:: " + recordIdentifier + "\n";
                aspaceCopyUtil.addErrorMessage(message);
            } else {
                return true;
            }
        } catch (ParseException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        return false;
    }

    /**
     * Method to set the current resource record identifier. useful for error
     * message generation
     *
     * @param identifier
     */
    public void setCurrentResourceRecordIdentifier(String identifier) {
        this.currentResourceRecordIdentifier = identifier;
    }

    /**
     * Method to set the current connection url
     *
     * @param connectionUrl
     */
    public void setConnectionUrl(String connectionUrl) {
        this.connectionUrl = connectionUrl;
    }

    /**
     * Method to see whether to set the extent in parts for BYU plugin
     *
     * @param b
     */
    public void setExtentPortionInParts(boolean b) {
        extentPortionInParts = b;
    }

    /**
     * Method to specify that all ISO dates should be checked
     *
     * @param checkISODates
     */
    public void setCheckISODates(boolean checkISODates) {
        this.checkISODates = checkISODates;
        datesList = new ArrayList<String>();

        // force the date formater to not to try an correct bad dates
        sdf.setLenient(false);
    }

    /**
     * Method to check all ISO dates
     */
    public void checkISODates() {
        System.out.println("\n\nChecking " + datesList.size() + " ISO Dates ...\n");

        int badDates = 0;

        for (String date : datesList) {
            String[] sa = date.split("/");
            try {
                if (!sa[0].equals("null")) {
                    sdf.parse(sa[0]);

                    if (!sa[1].equals("null")) {
                        sdf.parse(sa[1]);
                    }
                }
            } catch (ParseException e) {
                System.out.println("Invalid ISO date: " + date);
                badDates++;
            }
        }

        System.out.println("\nFinished checking for bad ISO dates. Found: " + badDates);
    }

    /**
     * Method to remove trailing zero from a double
     *
     * @param doubleNumber
     * @return
     */
    public String removeTrailingZero(Double doubleNumber) {
        String number = doubleNumber.toString();

        if(number.endsWith(".0")) {
            number = number.replace(".0", "");
        }

        return number;
    }

    /**
     * Method to print messages to the user
     * @param message
     */
    private void print(String message) {
        aspaceCopyUtil.print(message);
    }

    /**
     * Main method for testing
     *
     * @param args
     */
    public static void main(String[] args) {
        ASpaceMapper mapper = new ASpaceMapper();
        Double number = new Double("1.00");
        System.out.println("Number " + number + ", " + mapper.removeTrailingZero(number));

        number = new Double("10.5");
        System.out.println(mapper.removeTrailingZero(number));

        number = new Double("0.5");
        System.out.println(mapper.removeTrailingZero(number));
    }
}
