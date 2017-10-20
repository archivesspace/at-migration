package org.archiviststoolkit.plugin.utils.aspace;

import org.apache.commons.httpclient.NameValuePair;
import org.archiviststoolkit.ApplicationFrame;
import org.archiviststoolkit.model.*;
import org.archiviststoolkit.mydomain.DomainObject;
import org.archiviststoolkit.plugin.dbCopyFrame;
import org.archiviststoolkit.plugin.dbdialog.RemoteDBConnectDialogLight;
import org.archiviststoolkit.plugin.utils.ScriptDataUtils;
import org.archiviststoolkit.plugin.utils.StopWatch;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.swing.*;
import java.io.*;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: nathan
 * Date: 9/5/12
 * Time: 1:48 PM
 * Utility class for copying data from the AT to Archive Space
 */
public class ASpaceCopyUtil {

    public static final String SUPPORTED_ASPACE_VERSION = "v2.1 & v2.2";

    // used to get session from the source and destination databases
    private RemoteDBConnectDialogLight sourceRCD;

    // Main application frame when running within the AT
    private ApplicationFrame mainFrame = null;

    // The language codes needed for data mapping
    private HashMap<String, String> languageCodes;

    private HashMap<String, String> nameLinkCreatorCodes;

    // String to indicate when no ids where return from aspace backend
    private final String NO_ID = "no id assigned";

    // used to create the Archive Space JSON data
    private ASpaceMapper mapper;

    // The utility class used to map to ASpace Enums
    private ASpaceEnumUtil enumUtil = new ASpaceEnumUtil();

    // used to make REST calls to archive space backend service
    private ASpaceClient aspaceClient = null;

    // used to store information about the archives space backend
    private String aspaceInformation = "Simulator";

    private String aspaceVersion = "";

    // hashmap that maps repository from old database with copy in new database
    private HashMap<String, String> repositoryURIMap = new HashMap<String, String>();

    // hasmap to store the repo agents for use in creating event objects
    private HashMap<String, String> repositoryAgentURIMap = new HashMap<String, String>();

    // hashmap that stores the repository groups from the archive space database
    private HashMap<String, JSONObject> repositoryGroupURIMap = new HashMap<String, JSONObject>();

    // hashmap that maps location from the old database with copy in new database
    private HashMap<Long, String> locationURIMap = new HashMap<Long, String>();

    // hashmap that maps subjects from old database with copy in new database
    private HashMap<Long, String> subjectURIMap = new HashMap<Long, String>();

    // hashmap that maps names from old database with copy in new database
    private HashMap<Long, String> nameURIMap = new HashMap<Long, String>();

    // hashmap that maps accessions from old database with copy in new database
    private HashMap<Long, String> accessionURIMap = new HashMap<Long, String>();

    // hashmap that maps digital objects from old database with copy in new database
    private HashMap<Long, String> digitalObjectURIMap = new HashMap<Long, String>();

    // hashmap that maps resource from old database with copy in new database
    private HashMap<Long, String> resourceURIMap = new HashMap<Long, String>();

    private HashMap<Long, String> assessmentURIMap = new HashMap<Long, String>();

    private HashMap<String, HashMap<String, Integer>> assessmentAttributeDefinitions = new HashMap<String, HashMap<String, Integer>>();

    private Boolean copyAssessments;

    public String getURIMapping(DomainObject record) {

        Long recordIdentifier = record.getIdentifier();
        Class recordClass = record.getClass();

        if (recordClass.toString().contains("Locations")) {
            return locationURIMap.get(recordIdentifier);
        } else if (recordClass.toString().contains("Subjects")) {
            return subjectURIMap.get(recordIdentifier);
        } else if (recordClass.toString().contains("Names")) {
            return nameURIMap.get(recordIdentifier);
        } else if (recordClass.toString().contains("Accessions")) {
            return accessionURIMap.get(recordIdentifier);
        } else if (recordClass.toString().contains("DigitalObjects")) {
            return digitalObjectURIMap.get(recordIdentifier);
        } else if (recordClass.toString().contains("Resources")) {
            return resourceURIMap.get(recordIdentifier);
        } else if (recordClass.toString().contains("Assessments")) {
            return assessmentURIMap.get(recordIdentifier);
        } else {
            return null;
        }
    }

    // stop watch object for keeping tract of time
    private StopWatch stopWatch = null;

    // specify debug the boolean
    private boolean debug = true;

    // specify the current record type and ID in case we have fetal error during migration
    private String currentRecordType = "";
    private String currentRecordIdentifier = "";

    // integer to keep track of # of resource records copied
    private int copyCount = 1;

    private int totalASpaceClients = 0;

    // These fields are used to track of the number of messages posted to the output console
    // in order to prevent memory usage errors
    private int messageCount = 0;
    private final int MAX_MESSAGES = 100;

    // hashmap that maps the lookup list values currently in the destination database
    private HashMap<String, String> lookupListMap = new HashMap<String, String>();

    // keep tract of the number of errors when converting and saving records
    private int saveErrorCount = 0;
    private int aspaceErrorCount = 0;

    private String resetPassword = "password";

    // this is used to output text to user when doing the data transfer
    private JTextArea outputConsole;

    // this are used to give user better feedback on progress
    private JProgressBar progressBar;
    private JLabel errorCountLabel;

    // used to specify the stop the copying process. Only get checked when copying resources
    private boolean stopCopy = false;

    // used to specified the the copying process is running
    private boolean copying = false;

    // variable used to find and attempt to resolve repository mismatches
    private boolean checkRepositoryMismatch = false;
    private HashMap<String, String> repositoryMismatchMap = new HashMap<String, String>();
    private int mismatchesFixed = 0;
    private int mismatchesNotFixed = 0;

    /* Variables used to save URI mapping to file to make data transfer more efficient */

    // file where the uri maps is saved
    private static File uriMapFile = null;

    // keys use to store objects in hash map
    private final String REPOSITORY_KEY = "repositoryURIMap";
    private final String LOCATION_KEY = "locationURIMap";
    private final String USER_KEY = "userURIMap";
    private final String SUBJECT_KEY = "subjectURIMap";
    private final String NAME_KEY = "nameURIMap";
    private final String ACCESSION_KEY = "accessionURIMap";
    private final String DIGITAL_OBJECT_KEY = "digitalObjectURIMap";
    private final String RESOURCE_KEY = "resourceURIMap";
    private final String ASSESSMENT_KEY = "assessmentURIMap";
    private final String TOP_CONTAINER_KEY = "topContainerURIMap";
    private final String REPOSITORY_MISMATCH_KEY = "repositoryMismatchMap";
    private final String REPOSITORY_AGENT_KEY = "repositoryAgentMap";
    private final String REPOSITORY_GROUP_KEY = "repositoryGroupMap";
    private final String RECORD_TOTAL_KEY = "copyProgress";
    private final String RECORD_ATTEMPTED_KEY = "progressBookmark";

    // An Array List for storing the total number of main records transferred
    ArrayList<String> recordTotals = new ArrayList<String>();

    private LinkedList<String> recordsToCopy = new LinkedList<String>();
    private int numAttempted = 0;
    private int numSuccessful = 0;
    private int numUnlinked = 0;

    // Specifies whether or not to simulate the REST calls
    private boolean simulateRESTCalls = false;

    // Specifies whether to use the batch import functionality of aspace
    private boolean useBatchImport = false;

    // Specifies whether to delete the previously saved resource records. Useful for testing purposes
    private boolean deleteSavedResources = false;

    // this list is used to copy a specific resource
    private ArrayList<String> resourcesIDsList;

    // Booleans to specify whether to copy over unlinked Names and Subject records
    private boolean ignoreNames = false;
    private boolean ignoreSubjects = false;

    // used to keep track of the number of records rejected by a mapper script
    // loaded by the user
    private int mapperScriptRejects = 0;

    // A string builder object to track errors
    private StringBuilder errorBuffer = new StringBuilder();

    // String which specifies how ref_ids are to be handled. The options are below
    public static final String REFID_ORIGINAL = "-refid_original";
    public static final String REFID_UNIQUE = "-refid_unique";
    public static final String REFID_NONE = "-refid_none";
    private String refIdOption = REFID_UNIQUE;

    // String which specifies how to handle the default term type
    public static final String TERM_UNTYPED = "-term_untyped";
    public static final String TERM_DEFAULT = "-term_default";
    private String termTypeOption = TERM_DEFAULT;
    private boolean enumsSet = false;

    /**
     * The main constructor, used when running as a stand alone application
     *
     * @param sourceRCD
     */
    public ASpaceCopyUtil(RemoteDBConnectDialogLight sourceRCD, String host, String admin, String adminPassword) {
        this.sourceRCD = sourceRCD;
        this.aspaceClient = new ASpaceClient(host, admin, adminPassword);
        init();
    }

    /**
     * Constructor that only takes source connection. Used when running in the AT
     *
     * @param sourceRCD
     */
    public ASpaceCopyUtil(ApplicationFrame mainFrame, RemoteDBConnectDialogLight sourceRCD, String host, String admin,
                          String adminPassword) {
        this.mainFrame = mainFrame;
        this.sourceRCD = sourceRCD;
        this.aspaceClient = new ASpaceClient(host, admin, adminPassword);
        init();
    }

    /**
     * Method to initiate certain variables that are needed to work
     */
    private void init() {
        print("Starting database copy ... ");

        // set the error buffer for the mapper
        mapper = new ASpaceMapper(this);

        // set the file that contains the record map
        uriMapFile = new File(System.getProperty("user.home") + File.separator + "uriMaps.bin");

        // first add the admin repo to the repository URI map
        repositoryURIMap.put("adminRepo", ASpaceClient.ADMIN_REPOSITORY_ENDPOINT);

        // get the language codes and set it for the enum and mapper utils
        languageCodes = sourceRCD.getLanguageCodes();
        nameLinkCreatorCodes = sourceRCD.getNameLinkCreatorCodes();

        mapper.setLanguageCodes(languageCodes);
        mapper.setNameLinkCreatorCodes(nameLinkCreatorCodes);
        mapper.setConnectionUrl(sourceRCD.getConnectionUrl());

        enumUtil.setLanguageCodes(languageCodes);
        enumUtil.setNameLinkCreatorCodes(nameLinkCreatorCodes);

        // map used when copying lookup list items to the archive space backend
        lookupListMap.put("subject term source", ASpaceClient.VOCABULARY_ENDPOINT);

        TopContainerMapper.setaSpaceCopyUtil(this);

        recordsToCopy = new LinkedList<String>();
        recordsToCopy.add("Lookup List");
        recordsToCopy.add("Repositories");
        recordsToCopy.add("RepositoryGroups");
        recordsToCopy.add("Locations");
        recordsToCopy.add("Admin User");
        recordsToCopy.add("Users");
        recordsToCopy.add("Subjects");
        recordsToCopy.add("Names");
        recordsToCopy.add("Accessions");
        recordsToCopy.add("Digital Objects");
        recordsToCopy.add("Resource Records");
        recordsToCopy.add("Assessments");

        // start the stop watch object so we can see how long this data transfer takes
        startWatch();
    }

    /**
     * Method to set the hash map used for testing when records should be published or not
     * @param publishHashMap
     */
    public void setPublishHashMap(HashMap<String, Boolean> publishHashMap) {
        mapper.setPublishHashMap(publishHashMap);
    }

    /**
     * Method to set a bean script to use for the data mapping operation instead of the default logic
     */
    public void setMapperScript(String script) {
        mapper.setMapperScript(script);
    }

    /**
     * Method to set the reset password when copying user records
     *
     * @param resetPassword
     */
    public void setResetPassword(String resetPassword) {
        this.resetPassword = resetPassword;
    }

    /**
     * Method to set the ref id option
     * @param option
     */
    public void setRefIdOption(String option) {
        refIdOption = option;
    }

    /**
     * Method to return the ref id options
     *
     * @return
     */
    public String getRefIdOption() {
        return refIdOption;
    }

    /**
     * Method to set the term type option
     *
     * @param option
     */
    public void setTermTypeOption(String option) {
        termTypeOption = option;
    }

    /**
     * Method to return the term type option
     *
     * @return
     */
    public String getTermType() {
        if (termTypeOption.equals(TERM_DEFAULT)) {
            return "genre_form";
        } else {
            return termTypeOption.replace("-term_", "");
        }
    }

    /**
     * Method to see if to mark a term type as untyped
     *
     * @return
     */
    public boolean isTermTypeDefault() {
        return termTypeOption.equals(ASpaceCopyUtil.TERM_DEFAULT);
    }

    /**
     * Method to set the output console
     *
     * @param outputConsole
     */
    public void setOutputConsole(JTextArea outputConsole) {
        this.outputConsole = outputConsole;
    }

    public void setProgressIndicators(JProgressBar progressBar, JLabel errorCountLabel) {
        this.progressBar = progressBar;
        this.errorCountLabel = errorCountLabel;
    }

    /**
     * Method to start the start the time watch
     */
    private void startWatch() {
        stopWatch = new StopWatch();
        stopWatch.start();
    }

    private String stopWatch() {
        stopWatch.stop();
        return stopWatch.getPrettyTime();
    }

    /**
     * Method to return the status of getting the session needed to create certain records
     *
     * @return
     */
    public boolean getSession() throws IntentionalExitException {
        if(simulateRESTCalls) return true;

        boolean connected = aspaceClient.getSession();

        if(connected) {
            aspaceClient.setASpaceCopyUtil(this);
            aspaceInformation = aspaceClient.getArchivesSpaceInformation();
            setASpaceVersion();
        }

        return connected;
    }

    /**
     * Method to extract the aspace version from the information return from the backend
     */
    private void setASpaceVersion() {
        try {
            JSONObject infoJS = new JSONObject(aspaceInformation);
            String version = infoJS.getString("archivesSpaceVersion");
            int end = version.lastIndexOf(".");
            aspaceVersion = version.substring(0, end);
        } catch (Exception e) { }
    }

    public void setCopyAssessments() {
         if (aspaceVersion.isEmpty()) {
             String message = "Cannot determine your version of ArchivesSpace. Do you want to attempt to\n" +
                     "copy assessments to ArchivesSpace? (This will only work with version 2.2.)";
             while (copyAssessments == null) {
                 int result = JOptionPane.showConfirmDialog(null, message, "Copy assessments?",
                         JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                 if (result == JOptionPane.NO_OPTION) {
                     copyAssessments = false;
                 } else if (result == JOptionPane.YES_OPTION) {
                     copyAssessments = true;
                 }
             }
         }
    }

    /**
     * Method to return the aspace version
     */
    public String getASpaceVersion() {
        return aspaceVersion;
    }

    /**
     * Method to copy some of the look list items from AT to ASpace. This is needed to allow
     * user defined values to be migrated to ASpace.
     */
    public void copyLookupList() throws Exception {
        if(simulateRESTCalls) return;

        // first load the dynamic enums current is ASpace
        HashMap<String, JSONObject> dynamicEnums = aspaceClient.loadDynamicEnums();
        mapper.setASpaceDynamicEnums(dynamicEnums);
        enumsSet = true;
        mapper.setReturnATValue(false);

        if(dynamicEnums == null) {
            System.out.println("Can't load ASpace dynamic enums ...");
            return;
        }

        if (!recordsToCopy.contains("Lookup List")) return;

        ArrayList<LookupList> records = sourceRCD.getLookupLists();

        /* add a dummy lookup list to hold new event fields */
        LookupList eventList = new LookupList();
        eventList.setListName("Events");
        eventList.addListItem("processing_started");
        eventList.addListItem("processing_completed");
        eventList.addListItem("rights_transferred");
        records.add(eventList);

        LookupList rightsBasisList = new LookupList();
        rightsBasisList.setListName("Rights Basis");
        rightsBasisList.addListItem("archivists_toolkit");
        records.add(rightsBasisList);

        // these are used to update the progress bar
        int total = records.size();
        int count = numAttempted;
        int success = numSuccessful;

//        if (recordsToCopy.peek().equals("Lookup List")) {
        records = new ArrayList<LookupList>(records.subList(numAttempted, total));
//        }

        for (LookupList lookupList: records) {
            String listName = lookupList.getListName();

            // we may need to add additional values to some lookup list now
            if(listName.equalsIgnoreCase("Extent type")) {
                sourceRCD.addExtentTypes(lookupList);
                lookupList.addListItem("unknown");
            } else if(listName.equals("Salutation")) {
                sourceRCD.addSalutations(lookupList);
            } else if(listName.equals("Name source")) {
                lookupList.addListItem("ingest");
                lookupList.addListItem("local");
            } else if(listName.equals("Container types")) {
                lookupList.addListItem("unknown_item");
            } else if(listName.equals("Resource type")) {
                lookupList.addListItem("collection");
            } else if(listName.equals("Date type")) {
                lookupList.addListItem("other");
            }

            JSONObject updatedEnumJS = mapper.mapLookList(lookupList);

            if (updatedEnumJS != null) {
                String endpoint = updatedEnumJS.getString("uri");
                String jsonText = updatedEnumJS.toString();
                String id = saveRecord(endpoint, jsonText, "LookupList->" + lookupList.getListName());

                if (!id.equalsIgnoreCase(NO_ID)) {
                    print("Copied Lookup List Values: " + lookupList.getListName() + " :: " + id);
                    success++;
                    numSuccessful++;
                }
            }

            count++;
            numAttempted++;
            updateProgress("Lookup List", total, count);
        }

        recordsToCopy.remove();
        numAttempted = 0;
        numSuccessful = 0;
        saveURIMaps();

        // set this to be 100 percent since not all lookup will need to be migrated
        updateRecordTotals("Lookup List", total, total);

        // set this to true so we can just return the AT value
        mapper.setReturnATValue(true);
    }

    /**
     * Method to load the vocabularies from ASpace backend
     *
     * @return
     */
    public void loadRepositories() {
        if(simulateRESTCalls) return;

        HashMap<String, String> repos = aspaceClient.loadRepositories();

        if (repos != null) {
            repositoryURIMap = repos;
        }
    }

    /**
     * Method to copy the repository records
     *
     * @throws Exception
     */
    public void copyRepositoryRecords() throws Exception {

        if (!recordsToCopy.contains("Repositories")) return;

        print("Copying repository records ...");

        ArrayList<Repositories> records = sourceRCD.getRepositories();

//        if (recordsToCopy.peek().equals("Repositories")) {
//            records = new ArrayList<Repositories>(records.subList(numAttempted, records.size()));
//        }

        // these are used to update the progress bar
        int total = records.size();
        int count = numAttempted;//0;
        int success = numSuccessful;

        records = new ArrayList<Repositories>(records.subList(numAttempted, total));

        for (Repositories repository : records) {
            if(stopCopy) return;

            String shortName = repository.getShortName();

            if (!repositoryURIMap.containsKey(shortName)) {
                String jsonText;
                String id;

                // get and save the agent object for the repository
                String agentURI = null;
                jsonText = mapper.getCorporateAgent(repository);
                id = saveRecord(ASpaceClient.AGENT_CORPORATE_ENTITY_ENDPOINT, jsonText, "Repository_Name_Corporate->" + repository.getShortName());

                if (!id.equalsIgnoreCase(NO_ID)) {
                    agentURI = ASpaceClient.AGENT_CORPORATE_ENTITY_ENDPOINT + "/" + id;
                }

                jsonText = mapper.convertRepository(repository);
                id = saveRecord(ASpaceClient.REPOSITORY_ENDPOINT, jsonText, "Repository->" + shortName);

                if (!id.equalsIgnoreCase(NO_ID)) {
                    String uri = ASpaceClient.REPOSITORY_ENDPOINT + "/" + id;
                    repositoryURIMap.put(shortName, uri);
                    repositoryAgentURIMap.put(uri, agentURI);

                    print("Copied Repository: " + repository.getShortName() + " :: " + id);
//                    print("Mapping repository user group records for " + repository.getShortName() + "...");
//                    mapRepositoryGroups(uri);
                    success++;
                    numSuccessful++;
                } else {
                    print("Fail -- Repository: " + repository.getShortName());
                }
            } else {
                print("Repository already in database " + shortName);
                success++;
                numSuccessful++;
            }

            count++;
            numAttempted++;
            updateProgress("Repositories", total, count);
        }

        recordsToCopy.remove();
        numAttempted = 0;
        numSuccessful = 0;
        saveURIMaps();

        updateRecordTotals("Repositories", total, success);
    }

    /**
     * Method to load the repository groups, map them to access classes
     * then place them in a hashmap
     */
    public void mapRepositoryGroups() {
        if(simulateRESTCalls) return;

        if (!recordsToCopy.contains("RepositoryGroups")) return;

        print("Mapping repository user group records ...");

        // these are used to update the progress bar
        int total = repositoryURIMap.size();
        int count = 0;

        // now load the groups for all repositories
        for(String repoURI: repositoryURIMap.values()) {
            JSONArray groupsJA = aspaceClient.loadRepositoryGroups(repoURI);

            // process each group and map it to an AT user class
            for(int i = 0; i < groupsJA.length(); i++) {
                try {
                    JSONObject groupJS = groupsJA.getJSONObject(i);

                    // if no members array exist add one
                    if(groupJS.isNull("member_usernames")) {
                        JSONArray membersJA = new JSONArray();
                        groupJS.put("member_usernames", membersJA);
                    }

                    // map this to this group to an AT access class
                    mapper.mapAccessClass(repositoryGroupURIMap, groupJS, repoURI);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                count++;
                updateProgress("Repository Groups", total, count);
            }
        }

        recordsToCopy.remove();
        saveURIMaps();

        if(debug) {
            print("Number of groups mapped: " + repositoryGroupURIMap.size());
        }
    }

    /**
     * Method to copy the location records
     */
    public void copyLocationRecords() throws Exception {

        if (!recordsToCopy.contains("Locations")) return;

        print("Copying locations records ...");

        ArrayList<Locations> records = sourceRCD.getLocations();

//        if (recordsToCopy.peek().equals("Locations")) {
//            records = new ArrayList<Locations>(records.subList(numAttempted, records.size()));
//        }

        // these are used to update the progress bar and import log
        int total = records.size();
        int count = numAttempted;
        int success = numSuccessful;

        records = new ArrayList<Locations>(records.subList(numAttempted, total));

        updateProgress("Locations", total, count);

        for (Locations location : records) {
            if(stopCopy) return;

            // check to see if we are using a mapper script to filter some records
            if(mapper.runLocationMapperScript && !mapper.canCopyRecord(location)) {
                print("Mapper Script -- Not Copying Location: " + location);
                continue;
            }

            String jsonText = (String) mapper.convert(location);
            if (jsonText != null) {
                String uri = ASpaceClient.LOCATION_ENDPOINT;
                String id = saveRecord(uri, jsonText, "Location->" + location.getSortString());

                if (!id.equalsIgnoreCase(NO_ID)) {
                    uri = uri + "/" + id;
                    locationURIMap.put(location.getIdentifier(), uri);
                    print("Copied Location: " + location.getSortString() + " :: " + id);
                    success++;
                    numSuccessful++;
                } else {
                    print("Fail -- Location: " + location.getSortString());
                }
            } else {
                print("Fail -- Location to JSON: " + location.getSortString());
            }

            count++;
            numAttempted++;
            updateProgress("Locations", total, count);
        }

        recordsToCopy.remove();
        numAttempted = 0;
        numSuccessful = 0;
        saveURIMaps();

        updateRecordTotals("Locations", total, success);

        // refresh the database connection to prevent heap space error
        freeMemory();
    }

    /**
     * Copy the user records, remapping the repository
     *
     * @throws Exception
     */
    public void copyUserRecords() throws Exception {

        if (!recordsToCopy.contains("Users")) return;

        print("Copying User records ...");

        ArrayList<Users> records = sourceRCD.getUsers();

//        if (recordsToCopy.peek().equals("Users")) {
//            records = new ArrayList<Users>(records.subList(numAttempted, records.size()));
//        }

        // these are used to update the progress bar
        int total = records.size();
        int count = numAttempted;
        int success = numSuccessful;

        records = new ArrayList<Users>(records.subList(numAttempted, total));

        for (Users user : records) {
            if(stopCopy) return;

            // check to see if we are using a mapper script to filter some records
            if(mapper.runUserMapperScript && !mapper.canCopyRecord(user)) {
                print("Mapper Script -- Not Copying User: " + user);
                continue;
            }

            // first get the group the user belongs too
            ArrayList<String> groupURIs = getUserGroupURIs(user);

            NameValuePair[] params = new NameValuePair[1 + groupURIs.size()];
            params[0] = new NameValuePair("password", resetPassword);

            for (int i = 0; i < groupURIs.size(); i++) {
                params[i + 1] = new NameValuePair("groups[]", groupURIs.get(i));
            }

            String jsonText = (String) mapper.convert(user);
            String id = saveRecord(ASpaceClient.USER_ENDPOINT, jsonText, params, "User->" + user.getUserName());

            if (!id.equalsIgnoreCase(NO_ID)) {
                print("Copied User: " + user.toString() + " :: " + id);
                success++;
                numSuccessful++;
            } else {
                print("Fail -- User: " + user.toString());
            }

            count++;
            numAttempted++;
            updateProgress("Users", total, count);
        }

        recordsToCopy.remove();
        numAttempted = 0;
        numSuccessful = 0;
        saveURIMaps();

        updateRecordTotals("Users", total, success);

        // refresh the database connection to prevent heap space error
        freeMemory();
    }

    /**
     * Method to copy a test user to the ASpace backend
     *
     * @param username
     * @param name
     * @param password
     */
    public void addAdminUser(String username, String name, String password) throws Exception {

        if (username == null) recordsToCopy.remove();

        if (!recordsToCopy.contains("Admin User")) return;

        // get the administrator group uri
        String groupURI = "";

        String key = ASpaceClient.ADMIN_REPOSITORY_ENDPOINT  + ASpaceMapper.ACCESS_CLASS_PREFIX + 5;
        JSONObject groupJS = repositoryGroupURIMap.get(key);

        if(groupJS != null) {
            groupURI = groupJS.getString("uri");
        }

        // create the json object for the user
        JSONObject json = new JSONObject();
        json.put("username", username);
        json.put("name", name);

        NameValuePair[] params = new NameValuePair[2];
        params[0] = new NameValuePair("password", password);
        params[1] = new NameValuePair("groups[]", groupURI);

        String jsonText = json.toString();
        String id = saveRecord(ASpaceClient.USER_ENDPOINT, jsonText, params, "N/A");

        print("Added Admin User: " + username + " :: " + id);

        recordsToCopy.remove();
        saveURIMaps();
    }

    /**
     * Method to return the URI's of ASpace group(s) a user belongs to based on repository and access class
     *
     * It currently assigns users to a single group, but is designed to multiple groups if needed
     */
    public ArrayList<String> getUserGroupURIs(Users user) {
        ArrayList<String> groupURIs = new ArrayList<String>();

        // get the repository uri
        String repoURI = getRepositoryURI(user.getRepository());

        // construct the key base on access class
        String key = "";
        JSONObject groupJS;

        int accessClass = user.getAccessClass();

        try {
            if(accessClass == 5) {
                key = ASpaceClient.ADMIN_REPOSITORY_ENDPOINT  + ASpaceMapper.ACCESS_CLASS_PREFIX + 5;
                groupJS = repositoryGroupURIMap.get(key);
            } else {
                key = repoURI + ASpaceMapper.ACCESS_CLASS_PREFIX + accessClass;
                groupJS = repositoryGroupURIMap.get(key);
            }

            if(groupJS != null) {
                groupURIs.add(groupJS.getString("uri"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return groupURIs;
    }

    /**
     * Copy the name records
     *
     * @throws Exception
     */
    public void copyNameRecords() throws Exception {

        if (!recordsToCopy.contains("Names")) return;

        print("Copying Name records ...");

        ArrayList<Names> records = sourceRCD.getNames();

//        if (recordsToCopy.peek().equals("Names")) {
//            records = new ArrayList<Names>(records.subList(numAttempted, records.size()));
//        }

        // these are used to update the progress bar
        int total = records.size();
        int count = numAttempted;
        int success = numSuccessful;
        int unlinkedCount = numUnlinked;

        records = new ArrayList<Names>(records.subList(numAttempted, total));

        for (Names name : records) {
            if(stopCopy) return;

            // check to see if to ignore this record if it has no links
            if(ignoreNames && name.getArchDescriptionNames().size() == 0) {
                unlinkedCount++;
                numUnlinked++;
                print("Not Copying Unlinked Name: " + name);
                continue;
            }

            // check to see if we are using a mapper script to filter some records
            if(mapper.runNameMapperScript && !mapper.canCopyRecord(name)) {
                print("Mapper Script -- Not Copying Name: " + name);
                continue;
            }

            String type = name.getNameType();
            String jsonText = (String) mapper.convert(name);
            if (jsonText != null) {
                // based on the type of name copy to the correct location
                String id = "";
                String uri = "";

                if(type.equals(Names.PERSON_TYPE)) {
                    id = saveRecord(ASpaceClient.AGENT_PEOPLE_ENDPOINT, jsonText, "Name_Person->" + name.getSortName());
                    uri = ASpaceClient.AGENT_PEOPLE_ENDPOINT + "/" + id;
                } else if(type.equals(Names.FAMILY_TYPE)) {
                    id = saveRecord(ASpaceClient.AGENT_FAMILY_ENDPOINT, jsonText, "Name_Family->" + name.getSortName());
                    uri = ASpaceClient.AGENT_FAMILY_ENDPOINT + "/" + id;
                } else { // must be a corporate name
                    id = saveRecord(ASpaceClient.AGENT_CORPORATE_ENTITY_ENDPOINT, jsonText, "Name_Corporate->" + name.getSortName());
                    uri = ASpaceClient.AGENT_CORPORATE_ENTITY_ENDPOINT + "/" + id;
                }

                if(!id.equalsIgnoreCase(NO_ID)) {
                    nameURIMap.put(name.getIdentifier(), uri);
                    print("Copied Name: " + name + " :: " + id);
                    success++;
                    numSuccessful++;
                } else {
                    print("Failed -- Name: " + name);
                }
            } else {
                print("null json object when copying name: " + name);
            }

            count++;
            numAttempted++;
            updateProgress("Names", total, count);
        }

        recordsToCopy.remove();
        numAttempted = 0;
        numSuccessful = 0;

        updateRecordTotals("Names", total, success);

        // add error message indicating any records that were not copied because they
        // were not linked to any other records
        if(unlinkedCount > 0) {
            String unlinkMessage = "Did Not Copy " + unlinkedCount + " Unlinked Name Record(s)\n";
            addErrorMessage(unlinkMessage);
        }
        numUnlinked = 0;
        saveURIMaps();

        // refresh the database connection to prevent heap space error
        freeMemory();
    }

    public void addAssessments() throws Exception {

        if (!recordsToCopy.contains("Assessments")) return;

        if (aspaceVersion.contains("2.2")) {
            System.out.println("Copying assessments ...");
        } else if (aspaceVersion.isEmpty()) {
            if (copyAssessments == null) setCopyAssessments();
            if (!copyAssessments) {
                recordsToCopy.remove();
                return;
            }
            System.out.println("Unknown version of ASpace.\nAttempting to copy assessments ...");
            print("Unknown version of ASpace ...");
            print("Attempting to copy assessments ...");
        } else {
            System.out.println("ASpace version does not support assessments. Can not copy.");
            print("ASpace version " + aspaceVersion + " does not support assessments.\nSkipping copy assessments.");
            addErrorMessage("Can not copy assessments. ASpace version " + aspaceVersion + " does not support.");
            recordsToCopy.remove();
            return;
        }

        loadAssessmentAtributeDefinitions();

        ArrayList<Assessments> records = sourceRCD.getAssessments();

//        if (recordsToCopy.peek().equals("Assessments")) {
//            records = new ArrayList<Assessments>(records.subList(numAttempted, records.size()));
//        }

        // these are used to update the progress bar
        int total = records.size();
        int count = numAttempted;
        int success = numSuccessful;

        records = new ArrayList<Assessments>(records.subList(numAttempted, total));

        for (Assessments assessment : records) {
            if (stopCopy) return;
            
            String repoURI = getRemappedRepositoryURI("assessment", assessment.getIdentifier(), assessment.getRepository());
            String uri = repoURI + ASpaceClient.ASSESSMENT_ENDPOINT;

            String jsonText = (String) mapper.convert(assessment);
            if (jsonText != null) {
                String id = saveRecord(uri, jsonText, "Assessment->" + assessment.getAssessmentId());

                if(!id.equalsIgnoreCase(NO_ID)) {
                    uri = uri + "/" + id;
                    assessmentURIMap.put(assessment.getIdentifier(), uri);
                    print("Copied Assessment: " + assessment + " :: " + id);
                    success++;
                    numSuccessful++;
                } else {
                    print("Fail -- Assessment: " + assessment);
                }
            } else {
                print("Fail -- Assessment to JSON: " + assessment);
            }

            count++;
            numAttempted++;
            updateProgress("Assessments", total, count);
        }

        recordsToCopy.remove();
        numAttempted = 0;
        numSuccessful = 0;
        saveURIMaps();

        updateRecordTotals("Assessments", total, success);

        // refresh the database connection to prevent heap space error
        freeMemory();
    }

    // when an assessment attribute definition is added this is how to know what id it was assigned
    private int nextAssesAttrDefnID = 1;

    /**
     * loads the default assessment attribute definitions from ASpace (as well as any other existing to be safe)
     * @throws Exception
     */
    private void loadAssessmentAtributeDefinitions() throws Exception {
        boolean globalSet = false;

        //first add all the repo URIs to an array list so that the admin repo is first in the loop
        ArrayList<String> repos = new ArrayList<String>();
        repos.add(ASpaceClient.ADMIN_REPOSITORY_ENDPOINT);
        repos.addAll(repositoryURIMap.values());

        for (String repoURI: repos) {

            //admin repo will be in repos twice so skip it the second time
            if (repoURI.equals(ASpaceClient.ADMIN_REPOSITORY_ENDPOINT)) {
                if (globalSet) continue;
                globalSet = true;
            }

            //add the repository and have it map to an empty hash map that will store its definitions
            assessmentAttributeDefinitions.put(repoURI, new HashMap<String, Integer>());

            //get the definitions from ASpace and add each one to that repo's hashmap (label and type map to id)
            JSONObject result = new JSONObject(aspaceClient.get(repoURI + ASpaceClient.ASSESSMENT_ATTR_DEFNS_ENDPOINT, null));
            JSONArray definitionsJA = (JSONArray) result.get("definitions");
            for (int i = 0; i < definitionsJA.length(); i++) {
                JSONObject definitionJS = (JSONObject) definitionsJA.get(i);
                String key = definitionJS.get("label").toString() + " - " + definitionJS.get("type").toString();
                //don't need to add it if its already in the global map
                if (!assessmentAttributeDefinitions.get(ASpaceClient.ADMIN_REPOSITORY_ENDPOINT).containsKey(key)) {
                    assessmentAttributeDefinitions.get(repoURI).put(key, (Integer) definitionJS.get("id"));
                    nextAssesAttrDefnID++;
                }
            }
        }
    }

    /**
     * gets the mapping for a assessment attribute definition if it already exists. Otherwise adds the definition
     * @param repo
     * @param label
     * @param type
     * @return
     */
    public Integer getAssessmentAttributeID(Repositories repo, String label, String type) throws Exception {
        Integer id;
        String key = label + " - " + type;

        //first see if it is in the global assessment attributes list
        id = assessmentAttributeDefinitions.get(aspaceClient.ADMIN_REPOSITORY_ENDPOINT).get(key);
        if (id != null) return id;

        //next see if it is in the repository specific assessment attributes list
        String repoURI = repositoryURIMap.get(repo.getShortName());
        id = assessmentAttributeDefinitions.get(repoURI).get(key);
        if (id != null) return id;

        //otherwise add it to the repository assessment attributes list
        JSONObject defn = new JSONObject();
        defn.put("label", label);
        defn.put("type", type);
        JSONObject previous = new JSONObject(aspaceClient.get(repoURI + aspaceClient.ASSESSMENT_ATTR_DEFNS_ENDPOINT,
                null));
        JSONArray previousJA = (JSONArray) previous.get("definitions");
        id = nextAssesAttrDefnID++;
        previousJA.put(defn);
        JSONObject json = new JSONObject();
        json.put("definitions", previousJA);
        saveRecord(repoURI + aspaceClient.ASSESSMENT_ATTR_DEFNS_ENDPOINT, json.toString(),
                "Assessment Attributes->" + type);
        assessmentAttributeDefinitions.get(repoURI).put(key, id);
        return id;
    }

    /**
     * Method to copy the subject records
     *
     * @throws Exception
     */
    public void copySubjectRecords() throws Exception {

        if (!recordsToCopy.contains("Subjects")) return;

        print("Copying Subject records ...");

        ArrayList<Subjects> records = sourceRCD.getSubjects();

//        if (recordsToCopy.peek().equals("Subjects")) {
//            records = new ArrayList<Subjects>(records.subList(numAttempted, records.size()));
//        }

        // these are used to update the progress bar
        int total = records.size();
        int count = numAttempted;
        int success = numSuccessful;
        int unlinkedCount = numUnlinked;

        records = new ArrayList<Subjects>(records.subList(numAttempted, total));

        for (Subjects subject : records) {
            if(stopCopy) return;

            // check to see if to ignore this record if it has no links
            if(ignoreSubjects && subject.getArchDescriptionSubjects().size() == 0) {
                unlinkedCount++;
                numUnlinked++;
                print("Not Copying Unlinked Subject: " + subject);
                continue;
            }

            // check to see if we are using a mapper script to filter some records
            if(mapper.runSubjectMapperScript && !mapper.canCopyRecord(subject)) {
                print("Mapper Script -- Not Copying Subject: " + subject);
                continue;
            }

            String jsonText = (String) mapper.convert(subject);
            if (jsonText != null) {
                String id = saveRecord(ASpaceClient.SUBJECT_ENDPOINT, jsonText, "Subject->" + subject.getSubjectTerm());

                if(!id.equalsIgnoreCase(NO_ID)) {
                    String uri = ASpaceClient.SUBJECT_ENDPOINT + "/" + id;
                    subjectURIMap.put(subject.getIdentifier(), uri);
                    print("Copied Subject: " + subject + " :: " + id);
                    success++;
                    numSuccessful++;
                } else {
                    print("Fail -- Subject: " + subject);
                }
            } else {
                print("Fail -- Subject to JSON: " + subject);
            }

            count++;
            numAttempted++;
            updateProgress("Subjects", total, count);
        }

        recordsToCopy.remove();
        numAttempted = 0;
        numSuccessful = 0;

        updateRecordTotals("Subjects", total, success);

        // add error message indicating any records that were not copied because they
        // were not linked to any other records
        if(unlinkedCount > 0) {
            String unlinkMessage = "Did Not Copy " + unlinkedCount + " Unlinked Subject Record(s)\n";
            addErrorMessage(unlinkMessage);
        }
        numUnlinked = 0;
        saveURIMaps();

        // refresh the database connection to prevent heap space error
        freeMemory();
    }

    /**
     * Method to copy accession records
     *
     * @throws Exception
     */
    public void copyAccessionRecords() throws Exception {

        if (!recordsToCopy.contains("Accessions")) return;

        print("Copying Accession records ...");

        ArrayList<Accessions> records = sourceRCD.getAccessions();

//        if (recordsToCopy.peek().equals("Accessions")) {
//            records = new ArrayList<Accessions>(records.subList(numAttempted, records.size()));
//        }

        // these are used to update the progress bar
        int total = records.size();
        int count = numAttempted;
        int success = numSuccessful;

        records = new ArrayList<Accessions>(records.subList(numAttempted, total));

//        updateProgress("Accessions", total, count);

        for (Accessions accession : records) {
            if(stopCopy) return;

            // check to see if we are using a mapper script to filter some records
            if(mapper.runAccessionMapperScript && !mapper.canCopyRecord(accession)) {
                print("Mapper Script -- Not Copying Accession: " + accession);
                continue;
            }

            JSONObject accessionJS = (JSONObject) mapper.convert(accession);

            if (accessionJS != null) {
                // add the subjects
                addSubjects(accessionJS, accession);

                // add the linked agents aka Names records
                addNames(accessionJS, accession);

                // add an instance that holds the location information
                TopContainerMapper.setaSpaceCopyUtil(this);
                addInstance(accession, accessionJS);

                String repoURI = getRemappedRepositoryURI("accession", accession.getIdentifier(), accession.getRepository());
                String uri = repoURI + ASpaceClient.ACCESSION_ENDPOINT;
                String id = saveRecord(uri, accessionJS.toString(), "Accession->" + accession.getAccessionNumber());

                if (!id.equalsIgnoreCase(NO_ID)) {
                    uri = uri + "/" + id;

                    // now add the event objects
                    addEvents(accession, repoURI, uri);

                    accessionURIMap.put(accession.getIdentifier(), uri);
                    print("Copied Accession: " + accession.getTitle() + " :: " + id);
                    success++;
                    numSuccessful++;
                } else {
                    print("Fail -- Accession: " + accession.getTitle());
                }
            } else {
                print("Fail -- Accession to JSON: " + accession.getTitle());
            }

            count++;
            numAttempted++;
            updateProgress("Accessions", total, count);
        }

        recordsToCopy.remove();
        numAttempted = 0;
        numSuccessful = 0;
        saveURIMaps();

        updateRecordTotals("Accessions", total, success);

        // refresh the database connection to prevent heap space error
        freeMemory();
    }

    /**
     * Method to add events object to an accession object
     *
     * @param accession
     * @param repoURI
     * @param accessionURI
     */
    private void addEvents(Accessions accession, String repoURI, String accessionURI) throws Exception {
        String uri = repoURI + ASpaceClient.EVENT_ENDPOINT;
        String agentURI = repositoryAgentURIMap.get(repoURI);

        ArrayList<JSONObject> eventList = mapper.getAccessionEvents(accession, agentURI, accessionURI);

        for (JSONObject eventJS: eventList) {
            String id = saveRecord(uri, eventJS.toString(), "Accession Event->" + accession.getAccessionNumber());
            //System.out.println("Aspace Event ID:" + id);
        }
    }

    /**
     * Method to add a dummy instance to the accession json object in order to store
     * the location information
     *
     * @param json
     * @param accession
     * @throws Exception
     */
    public void addInstance(Accessions accession, JSONObject json) throws Exception {
        String parentRepoURI = getRemappedRepositoryURI("accession", accession.getIdentifier(), accession.getRepository()) + "/";

        Set<AccessionsLocations> locations = accession.getLocations();
        if(locations == null || locations.size() == 0) return;

        // now add a dummy instance record to store location
        JSONArray instancesJA = new JSONArray();

        // get all the locations URIs
        for(AccessionsLocations location: locations) {
            String locationURI = locationURIMap.get(location.getLocation().getIdentifier());
            if(locationURI != null) {
                String locationNote = location.getNote();
                JSONObject instanceJS = mapper.createAccessionInstance(accession, locationURI, locationNote, parentRepoURI, location);
                instancesJA.put(instanceJS);
            }
        }

        json.put("instances", instancesJA);
    }

    /**
     * Method to copy the digital object records
     *
     * @throws Exception
     */
    public void copyDigitalObjectRecords() throws Exception {

        if (!recordsToCopy.contains("Digital Objects")) return;

        print("Copying Digital Object records ...");

        ArrayList<DigitalObjects> records = sourceRCD.getDigitalObjects();

//        if (recordsToCopy.peek().equals("Digital Objects")) {
//            records = new ArrayList<DigitalObjects>(records.subList(numAttempted, records.size()));
//        }

        // these are used to update the progress bar
        int total = records.size();
        int count = numAttempted;
        int success = numSuccessful;

        records = new ArrayList<DigitalObjects>(records.subList(numAttempted, total));

        for (DigitalObjects digitalObject : records) {
            if(stopCopy) return;

            // check to see if we are using a mapper script to filter some records
            if(mapper.runDigitalObjectMapperScript && !mapper.canCopyRecord(digitalObject)) {
                print("Mapper Script -- Not Copying Digital Object: " + digitalObject);
                continue;
            }

            String atId = digitalObject.getMetsIdentifier(); // used to see what record an error occurred for

            JSONObject digitalObjectJS = mapper.convertDigitalObject(digitalObject);

            if(digitalObjectJS != null) {
                // add the subjects
                addSubjects(digitalObjectJS, digitalObject);

                // add the linked agents aka Names records
                addNames(digitalObjectJS, digitalObject);

                String repoURI = getRemappedRepositoryURI("digitalObject", digitalObject.getIdentifier(), digitalObject.getRepository());
                String uri = repoURI + ASpaceClient.DIGITAL_OBJECT_ENDPOINT;
                String id = saveRecord(uri, digitalObjectJS.toString(), "DigitalObject->" + digitalObject.getMetsIdentifier());

                if (!id.equalsIgnoreCase(NO_ID)) {
                    String digitalObjectURI = uri + "/" + id;
                    digitalObjectURIMap.put(digitalObject.getDigitalObjectId(), digitalObjectURI);

                    // add all the child records now
                    // set the resource URI and archival object endpoint
                    String docEndpoint = repoURI + ASpaceClient.DIGITAL_OBJECT_COMPONENT_ENDPOINT;

                    // add any archival objects here
                    Set<DigitalObjects> digitalObjectChildren = digitalObject.getDigitalObjectChildren();
                    for (DigitalObjects digitalObjectChild : digitalObjectChildren) {
                        if (stopCopy) return;

                        JSONObject digitalObjectChildJS = mapper.convertToDigitalObjectComponent(digitalObjectChild);

                        if (digitalObjectChildJS != null) {
                            digitalObjectChildJS.put("digital_object", mapper.getReferenceObject(digitalObjectURI));

                            // set the position
                            digitalObjectChildJS.put("position", digitalObjectChild.getObjectOrder());

                            // add the subjects now
                            addSubjects(digitalObjectChildJS, digitalObjectChild);

                            // add the linked agents aka Names records
                            addNames(digitalObjectChildJS, digitalObjectChild);

                            // save this json record now to get the URI
                            String cid = saveRecord(docEndpoint, digitalObjectChildJS.toString(), "DigitalObject->" + atId);

                            if (!cid.equalsIgnoreCase(NO_ID)) {
                                String digitalObjectChildURI = docEndpoint + "/" + cid;

                                print("Copied Digital Object Child: " + digitalObjectChild.getTitle() + " :: " + cid + "\n");

                                // call the recursive method to add child components
                                copyDigitalObjectChildren(docEndpoint, digitalObjectURI, digitalObjectChildURI, digitalObjectChild, atId);
                            } else {
                                print("Fail -- Digital Object Child: " + digitalObjectChild.getTitle() + "\n");
                            }
                        } else {
                            print("Fail -- Digital Object Child to JSON: " + digitalObjectChild.getTitle());
                        }
                    }

                    print("Copied Digital Object: " + digitalObject.getTitle() + " :: " + id);
                    success++;
                    numSuccessful++;
                } else {
                    print("Fail -- Digital Object: " + digitalObject.getTitle());
                }
            } else {
                print("Fail -- Digital Object to JSON: " + digitalObject.getTitle());
            }

            count++;
            numAttempted++;
            updateProgress("Digital Objects", total, count);
        }

        recordsToCopy.remove();
        numAttempted = 0;
        numSuccessful = 0;
        saveURIMaps();

        updateRecordTotals("Digital Objects", total, success);

        // refresh the database connection to prevent heap space error
        freeMemory();
    }

    /**
     * Method to do a recursion to add all the digital object children
     *
     * @param digitalObjectURI
     * @param parentURI
     * @param digitalObject
     */
    public void copyDigitalObjectChildren(String endpoint, String digitalObjectURI, String parentURI, DigitalObjects digitalObject, String atId) throws Exception {
        if (stopCopy) return;

        Set<DigitalObjects> digitalObjectChildren = digitalObject.getDigitalObjectChildren();

        if (digitalObjectChildren != null && digitalObjectChildren.size() != 0) {
            for (DigitalObjects digitalObjectChild : digitalObjectChildren) {
                if (stopCopy) return;

                JSONObject digitalObjectChildJS = mapper.convertToDigitalObjectComponent(digitalObjectChild);

                if (digitalObjectChildJS != null) {
                    digitalObjectChildJS.put("digital_object", mapper.getReferenceObject(digitalObjectURI));
                    digitalObjectChildJS.put("parent", mapper.getReferenceObject(parentURI));

                    // set the position
                    digitalObjectChildJS.put("position", digitalObjectChild.getObjectOrder());

                    // add the subjects now
                    addSubjects(digitalObjectChildJS, digitalObjectChild);

                    // add the linked agents aka Names records
                    addNames(digitalObjectChildJS, digitalObjectChild);

                    // save this json record now to get the URI
                    String cid = saveRecord(endpoint, digitalObjectChildJS.toString(), "DigitalObjectChild->" + atId);

                    if (!cid.equals(NO_ID)) {
                        String digitalObjectChildURI = endpoint + "/" + cid;

                        print("Copied Digital Object Child: " + digitalObjectChild.getTitle() + " :: " + cid + "\n");

                        // call the recursive method to add child components
                        copyDigitalObjectChildren(endpoint, digitalObjectURI, digitalObjectChildURI, digitalObjectChild, atId);
                    } else {
                        print("Fail -- Digital Object Child: " + digitalObjectChild.getTitle() + "\n");
                    }
                } else {
                    print("Fail -- Digital Object Child to JSON: " + digitalObjectChild.getTitle());
                }
            }
        }
    }

    /**
     * Method to copy resource records from one database to the next
     *
     * @throws Exception
     * @param max Used to specify the maximum amout of records that should be copied
     * @param threads
     */
    public void copyResourceRecords(int max, int threads) throws Exception {

        if (!enumsSet) mapper.setASpaceDynamicEnums(aspaceClient.loadDynamicEnums());
        enumsSet = true;

        currentRecordType = "Resource Record";

        // first delete previously saved resource records if that option was selected by user
//        if(deleteSavedResources) {
//            deleteSavedResources();
//        }

        if (!recordsToCopy.contains("Resource Records")) return;

        ArrayList<Resources> records = sourceRCD.getResources();

//        if (recordsToCopy.peek().equals("Resource Records")) {
//            records = new ArrayList<Resources>(records.subList(numAttempted, records.size()));
//        }

//        print("Copying " + records.size() + " Resource records ...");

        copyCount = 0; // keep track of the number of resource records copied

        // these are used to update the progress bar
        int total = records.size();
        int count = numAttempted;

        records = new ArrayList<Resources>(records.subList(numAttempted, total));

        print("Copying " + records.size() + " Resource records ...");

        // if we in debug mode, then set total to max
        if(debug && max < total) total = max;

        for (Resources resource : records) {
            // we need to update the progress bar here
            updateProgress("Resource Records", total, count);

            count++;

            // check if to stop copy process
            if(stopCopy) {
                updateRecordTotals("Resource Records", total, copyCount);
                return;
            }

            // check to see if we are using a mapper script to filter some records
            if(mapper.runResourceMapperScript && !mapper.canCopyRecord(resource)) {
                print("Mapper Script -- Not Copying Resource: " + resource.getTitle());
                continue;
            }

            // get the parent repository
            Repositories repository = resource.getRepository();

            // get the at resource identifier to see if to only copy a specified resource
            // and to use for trouble shooting purposes
            String atId = resource.getResourceIdentifier();

            currentRecordIdentifier = "DB ID: " + resource.getResourceId() + "\nAT ID: " + atId;

            // set the atId in the mapper object
            mapper.setCurrentResourceRecordIdentifier(atId);

            // check to see if we are not just copy a single resource
            if(resourcesIDsList != null && !resourcesIDsList.contains(atId)) {
                print("Not Copied: Resource not in list: " + resource);
                continue;
            }

            if (resourceURIMap.containsKey(resource.getIdentifier())) {
                incrementCopyCount();
                print("Not Copied: Resource already in database " + resource);
                updateProgress("Resource Records", total, count);
                continue;
            }

            // create the batch import JSON array in case we need it
            JSONArray batchJA = new JSONArray();

            // indicate we are copying the resource record
            print("Copying Resource: " + resource.getTitle());

            // grab the resource in a new session since we going to be doing a
            // recursion to get all the resource components
            resource = sourceRCD.getResource(resource.getIdentifier());

            // get the main json object
            JSONObject resourceJS = (JSONObject) mapper.convert(resource);

            if (resourceJS != null) {
                String repoURI = getRepositoryURI(resource.getRepository());
                String endpoint = repoURI + ASpaceClient.RESOURCE_ENDPOINT;
                String batchEndpoint = repoURI + ASpaceClient.BATCH_IMPORT_ENDPOINT;

                // add the subjects
                addSubjects(resourceJS, resource);

                // add the linked agents aka Names records
                addNames(resourceJS, resource);

                // add the instances
                addInstances(resourceJS, resource, repository, repoURI + "/");

                // add the linked accessions
                addRelatedAccessions(resourceJS, resource, repoURI + "/");

                // if we using batch import then we not not going to
                String id = resource.getIdentifier().toString();
                if(useBatchImport) {
                    resourceJS.put("uri", endpoint + "/" + id);
                    resourceJS.put("jsonmodel_type", "resource");
                    batchJA.put(resourceJS);
                } else {
                    id = saveRecord(endpoint, resourceJS.toString(4), "Resource->" + atId);
                }

                if (!id.equalsIgnoreCase(NO_ID)) {
                    // set the resource URI and archival object endpoint
                    String resourceURI = endpoint + "/" + id;
                    String aoEndpoint = repoURI + ASpaceClient.ARCHIVAL_OBJECT_ENDPOINT;

                    // add any archival objects here
                    Set<ResourcesComponents> resourceComponents = resource.getResourcesComponents();
                    for (ResourcesComponents component : resourceComponents) {
                        // check to see if we are using a mapper script to filter some records
                        if(mapper.runComponentMapperScript && !mapper.canCopyRecord(component)) {
                            print("Mapper Script -- Not Copying Resource Component: " + component);
                            continue;
                        }

                        JSONObject componentJS = (JSONObject) mapper.convert(component);

                        if (componentJS != null) {
                            componentJS.put("resource", mapper.getReferenceObject(resourceURI));

                            // add the subjects now
                            addSubjects(componentJS, component);

                            // add the linked agents aka Names records
                            addNames(componentJS, component);

                            // add the instances
                            addInstances(componentJS, component, repository, repoURI + "/");

                            // save this json record now to get the URI
                            String cid = component.getIdentifier().toString();
                            if(useBatchImport) {
                                componentJS.put("uri", aoEndpoint + "/" + cid);
                                componentJS.put("jsonmodel_type", "archival_object");
                                batchJA.put(componentJS);
                            } else {
                                cid = saveRecord(aoEndpoint, componentJS.toString(), atId);
                            }

                            if (!cid.equals(NO_ID)) {
                                String componentURI = aoEndpoint + "/" + cid;

                                print("Copied Resource Component: " + component.getTitle() + " :: " + cid + "\n");

                                // call the recursive method to add child components
                                copyResourceComponents(aoEndpoint, resourceURI, componentURI, component, batchJA, "ResourceComponent->" + atId + " :: " + component.getPersistentId(), repository, repoURI + "/");
                            } else {
                                print("Fail -- Resource Component: " + component.getTitle());
                            }
                        } else {
                            print("Fail -- Resource Component to JSON: " + component.getTitle());
                        }
                    }

                    // update the batch record now
                    if(useBatchImport) {
                        String resourceTitle = resource.getTitle();
                        Long dbId = resource.getIdentifier();

                        // release the hibernate connection now since we no longer need it
                        // in order to free the memory
                        freeMemory();

                        print("Batch Copying Resource # " + count + " || Title: " + resourceTitle);

                        if(threads == 1) {
                            String bids = saveRecord(batchEndpoint, batchJA.toString(2), atId);

                            if(!bids.equals(NO_ID) && bids.length() != 0) {
                                if(!simulateRESTCalls) {
                                    System.out.println("bids: " + bids);
                                    JSONObject bidsJS = new JSONObject(bids);
                                    resourceURI = (new JSONArray(bidsJS.getString(resourceURI))).getString(0);

                                }

                                print("Batch Copied Resource: " + resourceTitle + " :: " + resourceURI);

                            } else {
                                print("Batch Copy Fail -- Resource: " + resourceTitle);
                                continue;
                            }
                        } else {
                            // copy this in a separate thread
                            copyResourceRecordInThread(batchEndpoint, resourceURI, resourceTitle, batchJA.toString(2), atId, dbId, threads, total);
                        }
                    } else {
                        print("Copied Resource: " + resource.getTitle() + " :: " + id);
                    }

                    // save the record information to the URI map
                    if(threads == 1) {
                        updateResourceURIMap(resource.getIdentifier(), resourceURI);
                        incrementCopyCount();
                        updateRecordTotals("Resource Records", total, copyCount);
                    }
                } else {
                    print("Fail -- Resource: " + resource.getTitle());
                }
            } else {
                print("Fail -- Resource to JSON: " + resource.getTitle());
            }

            if (debug && copyCount >= max) break;

            // refresh the database connection to prevent heap space error now, if we not doing batch
            // processing of records
            if(!useBatchImport) {
                freeMemory();
            }

            if (!simulateRESTCalls) numAttempted++;
        }

        if (!simulateRESTCalls) recordsToCopy.remove();
        numAttempted = 0;
        numSuccessful = 0;
        if (!simulateRESTCalls) saveURIMaps();

        // wait for any threads to finish before returning if we running more than one
        // thread to copy
        while(getTotalASpaceClients() != 0 && !stopCopy) {
            print("Waiting for last set of records to be copied ...");
            Thread.sleep(60000); //wait 60 seconds before checking again
        }

        // update the number of resource actually copied
//        updateRecordTotals("Resource Records", total, copyCount);
    }

    /**
     * Method to do recursive copy of resource components
     *
     * @param parentURI
     * @param component
     * @throws Exception
     */
    private void copyResourceComponents(String endpoint, String resourceURI, String parentURI,
                                        ResourcesComponents component, JSONArray batchJA, String atId, Repositories parentRepository, String repoURI) throws Exception {
        if(stopCopy) return;

        if (component.isHasChild()) {
            for (ResourcesComponents childComponent : component.getResourcesComponents()) {
                if(stopCopy) return;

                // check to see if we are using a mapper script to filter some records
                if(mapper.runComponentMapperScript && !mapper.canCopyRecord(childComponent)) {
                    print("Mapper Script -- Not Copying Child Resource Component: " + childComponent);
                    continue;
                }

                JSONObject componentJS = (JSONObject) mapper.convert(childComponent);

                if (componentJS != null) {
                    componentJS.put("resource", mapper.getReferenceObject(resourceURI));
                    componentJS.put("parent", mapper.getReferenceObject(parentURI));

                    // add the subjects now
                    addSubjects(componentJS, childComponent);

                    // add the linked agents aka Names records
                    addNames(componentJS, childComponent);

                    // add the instances
                    addInstances(componentJS, childComponent, parentRepository, repoURI);

                    // save this json record now to get the URI
                    String id = childComponent.getIdentifier().toString();
                    if(useBatchImport) {
                        componentJS.put("uri", endpoint + "/" + id);
                        componentJS.put("jsonmodel_type", "archival_object");
                        batchJA.put(componentJS);
                    } else {
                        id = saveRecord(endpoint, componentJS.toString(), atId);
                    }

                    if(!id.equals(NO_ID)) {
                        String componentURI = endpoint + "/" + id;

                        print("Copied Resource Component: " + childComponent.getTitle() + " :: " + id  + "\n");

                        // call the recursive method to add child components
                        copyResourceComponents(endpoint, resourceURI, componentURI, childComponent, batchJA, "ResourceComponent->" + atId + " :: " + childComponent.getPersistentId(), parentRepository, repoURI);
                    } else {
                        print("Fail -- Resource Component: " + childComponent.getTitle());
                    }
                } else {
                    print("Fail -- Resource Component to JSON: " + childComponent.getTitle());
                }
            }
        }
    }

    /**
     * Add the subjects to the json resource, or resource component record
     *
     * @param json   The json representation of the AT record
     * @param record
     * @throws Exception
     */
    private synchronized void addSubjects(JSONObject json, ArchDescription record) throws Exception {
        Set<ArchDescriptionSubjects> asubjects = record.getSubjects();
        JSONArray subjectsJA = new JSONArray();

        for (ArchDescriptionSubjects asubject : asubjects) {
            Subjects subject = asubject.getSubject();
            String subjectURI = subjectURIMap.get(subject.getIdentifier());

            if (subjectURI != null) {
                subjectsJA.put(mapper.getReferenceObject(subjectURI));

                if (debug) print("Added subject to " + record.getTitle());
            } else {
                print("No mapped subject found ...");
            }
        }

        // if we had any subjects add them parent json record
        if (subjectsJA.length() != 0) {
            json.put("subjects", subjectsJA);
        }
    }

    /**
     * Add the names to the resource or resource component record
     *
     * @param json object record
     * @param record
     * @throws Exception
     */
    private synchronized void addNames(JSONObject json, ArchDescription record) throws Exception {
        Set<ArchDescriptionNames> anames = record.getNames();
        JSONArray linkedAgentsJA = new JSONArray();

        for (ArchDescriptionNames aname : anames) {
            Names name = aname.getName();
            String nameURI = nameURIMap.get(name.getIdentifier());

            if(nameURI != null) {
                JSONObject linkedAgentJS = new JSONObject();

                linkedAgentJS.put("role", enumUtil.getASpaceLinkedAgentRole(aname.getNameLinkFunction())[0]);
                linkedAgentJS.put("relator", enumUtil.getASpaceLinkedAgentRelator(aname.getRole()));

                // add the name form as a term
                if(!aname.getForm().isEmpty()) {
                    // get the term type
                    String termType = getTermType();

                    JSONArray termsJA = new JSONArray();

                    JSONObject termJS = new JSONObject();
                    termJS.put("term", aname.getForm());
                    termJS.put("term_type", termType);
                    termJS.put("vocabulary", mapper.vocabularyURI);

                    termsJA.put(termJS);
                    linkedAgentJS.put("terms", termsJA);
                }

                linkedAgentJS.put("ref", nameURI);
                linkedAgentsJA.put(linkedAgentJS);
            } else {
                print("No mapped name found ...");
            }
        }

        // if we had any subjects add them parent json record
        if (linkedAgentsJA.length() != 0) {
            json.put("linked_agents", linkedAgentsJA);
        }
    }

    /**
     * Method to add the instances from Resource and Resource components
     *
     * @param json
     * @param record
     * @throws Exception
     */
    private synchronized void addInstances(JSONObject json, ResourcesCommon record, Repositories parentRepository, String parentRepoURI) throws Exception {
        addInstances(json, record.getInstances(), record.getTitle(), parentRepository, parentRepoURI);
    }

    /**
     * Method to add an instance to resource, or resource component record
     *
     * @param json
     * @param recordTitle the title of the record
     * @throws Exception
     */
    private synchronized void addInstances(JSONObject json, Set<ArchDescriptionInstances> ainstances,
                                           String recordTitle, Repositories parentRepository, String parentRepoURI) throws Exception {
        JSONArray instancesJA = new JSONArray();

        String resourceRepo = parentRepository.getShortName();

        TopContainerMapper.setaSpaceCopyUtil(this);
        for (ArchDescriptionInstances ainstance : ainstances) {
            if (ainstance instanceof ArchDescriptionAnalogInstances) {
                ArchDescriptionAnalogInstances analogInstance = (ArchDescriptionAnalogInstances)ainstance;

                String locationURI = null;

                Locations location = analogInstance.getLocation();
                if(location != null) {
                    locationURI = locationURIMap.get(location.getIdentifier());

                    /*if(locationURI != null && !locationURI.contains(parentRepoURI)) {
                        // if there is a mismatch between the resource record repo and
                        // the location record repo then don't store location
                        System.out.println("Resource/Location Repo Mismatch");
                        locationURI = null;
                    }*/
                }

                JSONObject instanceJS = mapper.convertAnalogInstance(analogInstance, locationURI, parentRepoURI);

                if(instanceJS != null) {
                    instancesJA.put(instanceJS);
                    if (debug) print("Added analog instance to " + recordTitle);
                } else if(debug) {
                    print("Failed to add analog instance to " + recordTitle);
                }
            } else {
                // must be a digital instance
                ArchDescriptionDigitalInstances digitalInstance = (ArchDescriptionDigitalInstances)ainstance;
                DigitalObjects digitalObject = digitalInstance.getDigitalObject();

                // for some reason it possible that we have a digital instance with no link digital
                // onject so check for it
                if (digitalObject != null) {
                    String digitalObjectURI = digitalObjectURIMap.get(digitalObject.getDigitalObjectId());
                    JSONObject instanceJS = mapper.convertDigitalInstance(digitalObjectURI);

                    String digitalObjectRepo = digitalObject.getRepository().getShortName();

                    if (checkRepositoryMismatch) {
                        if (!resourceRepo.equals(digitalObjectRepo)) {
                            repositoryMismatchMap.put("digitalObject_" + digitalObject.getIdentifier(), resourceRepo);

                            String message = "Repository Mismatch between Resource Record -- Digital Object Instance: " +
                                    recordTitle + " [ " + resourceRepo + " ] / " +
                                    digitalObject.getMetsIdentifier() + " [ " + digitalObjectRepo + " ]\nMismatch corrected ...\n";

                            mismatchesFixed++;
                            addErrorMessage(message);
                        }
                    } else if (instanceJS != null) {
                        if (digitalObjectURI.contains(parentRepoURI)) {
                            instancesJA.put(instanceJS);
                            if (debug) print("Added Digital Object Instance to " + recordTitle);
                        } else {
                            String message = "Repository Mismatch between Resource Record -- Digital Object Instance: " +
                                    recordTitle + " [ " + resourceRepo + " ] / " +
                                    digitalObject.getMetsIdentifier() + " [ " + digitalObjectRepo + " ]\n";

                            addErrorMessage(message);
                        }
                    } else {
                        String message = "Linked Digital Object instance: " + digitalObject.getMetsIdentifier() + " Not Found For: " + recordTitle;
                        addErrorMessage(message);
                    }
                } else {
                    System.out.println("No Linked Digital Object found for Digital Instance: " + recordTitle);
                }
            }
        }

        if (instancesJA.length() != 0) {
            json.put("instances", instancesJA);
        }
    }

    /**
     * Method to add a related accessions to a resource record
     *
     * @param json
     * @param record
     */
    private synchronized void addRelatedAccessions(JSONObject json, Resources record, String recordRepoURI) throws Exception {
        Set<AccessionsResources> accessionsResources = record.getAccessions();
        JSONArray accessionsJA = new JSONArray();
        String message;

        for(AccessionsResources accessionsResource: accessionsResources) {
            Accessions accession = accessionsResource.getAccession();
            Long accessionId = accession.getAccessionId();
            String accessionURI = accessionURIMap.get(accessionId);

            // get the repository
            String resourceRepo = record.getRepository().getShortName();
            String accessionRepo = accession.getRepository().getShortName();

            if(checkRepositoryMismatch) {
                if(!resourceRepo.equals(accessionRepo)) {
                    // check to see if this is not already in the mismatch map
                    String key = "accession_" + accession.getIdentifier();
                    String fixMessage = "\nMismatch corrected ...\n";

                    if(repositoryURIMap.containsKey(key) &&
                            !repositoryMismatchMap.get(key).equals(resourceRepo)) {
                        fixMessage = "\nMismatch correction failed ...\n";
                        mismatchesNotFixed++;
                    } else {
                        repositoryMismatchMap.put(key, resourceRepo);
                        mismatchesFixed++;
                    }

                    message = "Repository Mismatch Between Resource -- Accession: " +
                            record.getResourceIdentifier() + " [ "+ resourceRepo + " ] / " +
                            accession.getAccessionNumber() + " [ " + accessionRepo + " ]" + fixMessage;
                    addErrorMessage(message);
                }
            } else if(accessionURI != null) {
                if(accessionURI.contains(recordRepoURI)) {
                    accessionsJA.put(mapper.getReferenceObject(accessionURI));
                    if (debug) print("Added Accession to Resource: " + record.getResourceIdentifier());
                } else {
                    message = "Repository Mismatch Between Resource -- Accession: " +
                            record.getResourceIdentifier() + " [ "+ resourceRepo + " ] / " +
                            accession.getAccessionNumber() + " [ " + accessionRepo + " ]\n";
                    addErrorMessage(message);
                }
            } else {
                message = "Linked Accession Not Found: " + accession.getAccessionNumber() + "\n";
                addErrorMessage(message);
            }
        }

        if (accessionsJA.length() != 0) {
            json.put("related_accessions", accessionsJA);
        }
    }

    /**
     * Method to copy resource records in a different thread
     * in order to increase performance?
     *
     * @throws Exception
     * @param endpoint
     * @param jsonText
     * @param atId
     * @param dbId
     */
    public void copyResourceRecordInThread(final String endpoint, final String tempResourceURI,
                                           final String resourceTitle, final String jsonText,
                                           final String atId, final Long dbId, int maxClients, final int totalRecords) throws Exception {

        // start the controller thread that goe through list of records
        Thread performer = new Thread(new Runnable() {
            public void run() {
                ASpaceClient asc = aspaceClient.getAuthenticatedClient();
                int clientNumber = getTotalASpaceClients();

                String bids = "";
                try {
                    print("Route: " + endpoint + "\nBatch Record Length: " + jsonText.length() + " bytes");

                    if(simulateRESTCalls) {
                        bids = "/repositories/2/resource/10001";
                        Thread.sleep(2);
                    } else {
                        bids = asc.post(endpoint, jsonText, null, atId);
                    }
                } catch (Exception e) {
                    print("Error saving batch import record: " + atId);

                    // get the error message and add it to the parent aspace client object
                    aspaceClient.appendToErrorBuffer(asc.getErrorMessages());

                    incrementSaveErrorCount();
                    incrementASpaceErrorCount();
                }

                if(!bids.equals(NO_ID)) {
                    try {
                        String resourceURI;

                        if(simulateRESTCalls) {
                            resourceURI = bids;
                        } else {
                            JSONObject bidsJS = new JSONObject(bids);
                            resourceURI = (new JSONArray(bidsJS.getString(tempResourceURI))).getString(0);
                        }

                        updateResourceURIMap(dbId, resourceURI);
                        incrementCopyCount();
                        updateRecordTotals("Resource Records", totalRecords, copyCount);

                        print("Thread Client # " + clientNumber + " -- Batch Copied Resource: " + resourceTitle + " :: " + resourceURI);
                    } catch(Exception e) {
                        System.out.println("Batch IDS JSON Object: "  + bids);
                        e.printStackTrace();
                    }
                } else {
                    print("Thread Client # " + clientNumber + " -- Batch Copy Fail -- Resource: " + resourceTitle);
                }

                // reduce the number of clients and set
                decrementTotalASpaceClients();
            }
        });

        // wait for the client count to be less than max before trying to copy
        int timeCount = 0;
        try {
            while (totalASpaceClients >= maxClients && !stopCopy) {
                timeCount++;

                if(timeCount <= 30) {
                    print("Waiting on response from backend to copy: " + atId + "\n");
                } else {
                    // if waiting more than ten minuets then inform user to
                    // check ASpace backend
                    print("Waiting over 10 minutes for response from backend to copy: " + atId + "\n");
                    print("Make sure the backend has not crashed ...\n");
                }

                Thread.sleep(20000); // wait 20 seconds before checking again
            }

            // start the thread now
            incrementTotalASpaceClients();
            performer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * increment the number of aspace clients
     */
    private synchronized void incrementTotalASpaceClients() {
        totalASpaceClients++;
    }

    /**
     * Method to decrement the total number of asapce client
     */
    private synchronized void decrementTotalASpaceClients() {
        totalASpaceClients--;
    }

    /**
     * Method to get the current aspace client
     *
     * @return current number of aspace client
     */
    private synchronized int getTotalASpaceClients() {
        return totalASpaceClients;
    }

    /**
     * Method to add to resource map in a thread safe manner
     *
     * @param oldIdentifier
     * @param uri
     */
    private synchronized void updateResourceURIMap(Long oldIdentifier, String uri) {
        if(!checkRepositoryMismatch) {
            resourceURIMap.put(oldIdentifier, uri);
            saveURIMaps();
        }
    }

    /**
     * Method to increment the number of resource records copied
     */
    private synchronized void incrementCopyCount() {
        copyCount++;
        if (!simulateRESTCalls) numSuccessful++;
    }

    /**
     * Method to delete any previously saved resource records on the ASpace backend.
     * Useful for testing purposes, but not much else
     */
    private void deleteSavedResources() {
        print("\nNumber of Resources to Delete: " + resourceURIMap.size());
        ArrayList<Long> deletedKeys = new ArrayList<Long>();

        // initialize the progress bar
        updateProgress("Resource", resourceURIMap.size(), -1);

        int count = 1;
        for(Long key: resourceURIMap.keySet()) {
            try {
                String uri = resourceURIMap.get(key);
                print("Deleting Resource: " + uri);

                String message = aspaceClient.deleteRecord(uri);
                print(message + "\n");
                deletedKeys.add(key);

                // update the progress bar
                updateProgress("Resource", 0, count);
                count++;
            } catch (Exception e) {
                e.printStackTrace();
                print("Error deleting ... \n" + e.getMessage());
            }
        }

        // remove the deleted keys now, since we can't modify a
        // hashmap while we still iterating over it
        for(Long key: deletedKeys) {
            resourceURIMap.remove(key);
        }

    }

    /**
     * Method to return the new repository for a given domain object.
     *
     * @param oldRepository
     * @return The URI of the new repository
     */
    private synchronized String getRepositoryURI(Repositories oldRepository) {
        // check to see if old repo is not null. If it is then we need to just return anyone
        // since this should never occur in a properly formatted AT database
        if(oldRepository != null && repositoryURIMap.containsKey(oldRepository.getShortName())) {
            String shortName = oldRepository.getShortName();
            return repositoryURIMap.get(shortName);
        } else {
            return "/repositories/2";
        }
    }

    /**
     * Method to remapped the repository if either an accession or digital object type to that
     * of the resource record they are linked to.  This is needed because the AT allows linking
     * of Accession and Digital Objects to Resource from a different repository while Aspace throws
     * an error.
     *
     * @param recordType
     * @param atId
     * @param oldRepository
     * @return
     */
    private synchronized String getRemappedRepositoryURI(String recordType, Long atId, Repositories oldRepository) {
        if(repositoryMismatchMap == null) {
            return getRepositoryURI(oldRepository);
        } else {
            String key = recordType + "_" + atId;
            String shortName = oldRepository.getShortName();

            // see if we are remapping this repository
            if(repositoryMismatchMap.containsKey(key)) {
                shortName = repositoryMismatchMap.get(key);
                System.out.println("Remapping repo for " + recordType);
            }

            return repositoryURIMap.get(shortName);
        }
    }

    /**
     * Method to save the record that takes into account running in stand alone
     * or within the AT
     *
     * @param endpoint to make post to
     * @param jsonText record
     */
    public synchronized String saveRecord(String endpoint, String jsonText, String atId) throws IntentionalExitException {
        return saveRecord(endpoint, jsonText, null, atId);
    }

    /**
     * Method to save the record that takes into account running in stand alone
     * or within the AT
     *
     * @param endpoint to make post to
     * @param jsonText record
     * @param params   parameters to pass to service
     */
    public synchronized String saveRecord(String endpoint, String jsonText, NameValuePair[] params, String atId) throws IntentionalExitException {

        String id = NO_ID;

        try {
            // Make sure we don't try to print out a batch import record since they can
            // be thousands of lines long
            if (endpoint.contains(ASpaceClient.BATCH_IMPORT_ENDPOINT)) {
                print("Route: " + endpoint + "\nBatch Record Length: " + jsonText.length() + " bytes");
            } else {
                print("Route: " + endpoint + "\n" + jsonText);
            }

            if (simulateRESTCalls) {
                id = "10000001";
                Thread.sleep(2);
            } else {
                id = aspaceClient.post(endpoint, jsonText, params, atId);
            }
        } catch (IntentionalExitException e) {
            throw e;
        } catch (Exception e) {
            if(endpoint.contains(ASpaceClient.BATCH_IMPORT_ENDPOINT)) {
                print("Error saving batch import record ...");
            } else {
                print("Error saving record " + jsonText);
            }

            incrementSaveErrorCount();
            incrementASpaceErrorCount();
        }

        return id;
    }

    /**
     * Method to increment the error count
     */
    public synchronized void incrementSaveErrorCount() {
        saveErrorCount++;
    }

    /**
     * Method to increment the aspace error count that occur when saving to the
     * backend
     */
    public synchronized void incrementASpaceErrorCount() {
        aspaceErrorCount++;
        updateErrorCountLabel();
    }

    private void updateErrorCountLabel() {
        if (errorCountLabel != null) {
            errorCountLabel.setText(aspaceErrorCount + " and counting ...");
        }
    }

    /**
     * Convenient print method for printing string in the text console in the future
     *
     * @param message
     */
    public synchronized void print(String message) {
        if(outputConsole != null) {
            messageCount++;

            if(messageCount < MAX_MESSAGES) {
                outputConsole.append(message + "\n");
            } else {
                messageCount = 0;
                outputConsole.setText(message + "\n");
            }
        } else {
            System.out.println(message);
        }

        // now see if to increment the count on the number of records that were rejects
        if(message.contains("Mapper Script --")) {
            mapperScriptRejects++;
        }
    }

    /**
     * Method to update the progress bar if not running in command line mode
     *
     * @param recordType
     * @param total
     * @param count
     */
    private synchronized void updateProgress(String recordType, int total, int count) {
        if(progressBar == null) return;

        if(count == -1) {
            progressBar.setMinimum(0);
            progressBar.setMaximum(total);
            progressBar.setString("Deleting " + total + " " + recordType);
        } else if(count == 0) {
            progressBar.setMinimum(0);
            progressBar.setMaximum(1);
            progressBar.setString("Loading " + recordType);
        } else if(count >= 1) {
            progressBar.setMinimum(0);
            progressBar.setMaximum(total);

            if(checkRepositoryMismatch) {
                progressBar.setString("Checking " + total + " " + recordType);
            } else {
                progressBar.setString("Copying " + total + " " + recordType);
            }
        }

        progressBar.setValue(count);
    }

    /**
     * Method to update the record totals
     *
     * @param recordType
     * @param total
     * @param success
     */
    private synchronized void updateRecordTotals(String recordType, int total, int success) {
        float percent = (new Float(success)/new Float(total))*100.0f;
        String info = recordType + " : " + success + " / " + total + " (" + String.format("%.2f", percent) + "%)";

        if (recordType.equals("Resource Records") && recordTotals.size() > 8) {
            recordTotals.set(8, info);
        } else {
            recordTotals.add(info);
        }
//        if(recordTotals.size() <= 8) {
//            recordTotals.add(info);
//        } else {
//            recordTotals.set(8, info);
//        }
    }

    /**
     * Method to return the number of errors when saving records
     *
     * @return
     */
    public int getSaveErrorCount() {
        return saveErrorCount;
    }

    /**
     * Get the total error count
     *
     * @return
     */
    public int getASpaceErrorCount() {
        return aspaceErrorCount;
    }

    /**
     * Method to add an error message to the buffer
     *
     * @param message
     */
    public synchronized void addErrorMessage(String message) {
        if(checkRepositoryMismatch) {
            if(message.contains("Repository Mismatch")) {
                errorBuffer.append(message).append("\n");
                incrementASpaceErrorCount();
            }
        } else {
            errorBuffer.append(message).append("\n");
            incrementASpaceErrorCount();
        }
    }

    /**
     * Method to return the error messages that occurred during the transfer process
     *
     * @return
     */
    public String getSaveErrorMessages() {
        int errorsAndWarnings = Math.abs(saveErrorCount - aspaceErrorCount);

        String mapperScriptMessage = "";
        if(mapperScriptRejects > 0) {
            mapperScriptMessage = "\n\nMapper Script -- Rejected " + mapperScriptRejects + " records";
        }

        String errorMessage = "RECORD CONVERSION ERRORS/WARNINGS ( " + errorsAndWarnings + " ) ::\n\n" + errorBuffer.toString() + mapperScriptMessage +
                "\n\n\nRECORD SAVE ERRORS ( " + saveErrorCount + " ) ::\n\n" + aspaceClient.getErrorMessages() +
                "\n\nTOTAL COPY TIME: " + stopWatch.getPrettyTime() +
                "\n\nNUMBER OF RECORDS COPIED: \n" + getTotalRecordsCopiedMessage() +
                "\n\n" + getSystemInformation();

        return errorMessage;
    }

    /**
     * Method to return the current status of the migration
     *
     * @return
     */
    public String getCurrentProgressMessage() {
        int errorsAndWarnings = Math.abs(aspaceErrorCount - saveErrorCount);

        String mapperScriptMessage = "";
        if(mapperScriptRejects > 0) {
            mapperScriptMessage = "\n\nMapper Script -- Rejected " + mapperScriptRejects + " records";
        }

        String totalRecordsCopied = getTotalRecordsCopiedMessage();

        String errorMessages = "RECORD CONVERSION ERRORS/WARNINGS ( " + errorsAndWarnings + " ) ::\n\n" + errorBuffer.toString() + mapperScriptMessage +
                "\n\n\nRECORD SAVE ERRORS ( " + saveErrorCount + " ) ::\n\n" + aspaceClient.getErrorMessages();

        String message = errorMessages +
                "\n\nRunning for: " + stopWatch.getPrettyTime() +
                "\n\nCurrent # of Records Copied: \n" + totalRecordsCopied +
                "\n\n" + getSystemInformation();

        return message;
    }

    /**
     * Method to return string with total records copied
     * @return
     */
    private String getTotalRecordsCopiedMessage() {
        String totalRecordsCopied = "";

        for(String entry: recordTotals) {
            totalRecordsCopied += entry + "\n";
        }

        return totalRecordsCopied;
    }

    /**
     * Method to return the current status of the repository check process
     *
     * @return
     */
    public String getCurrentRecordCheckMessage() {
        int errorsAndWarnings = Math.abs(aspaceErrorCount - saveErrorCount);

        String errorMessages = "REPOSITORY MISMATCH ERRORS ( " + errorsAndWarnings + " ) ::\n\n" + errorBuffer.toString();

        if(errorsAndWarnings == 0) {
            errorMessages = "No errors/warnings ...";
        }

        String message = "";
        if(copying) {
            message = errorMessages +
                    "\n\nRunning for: " + stopWatch.getPrettyTime() +
                    "\n\nCurrent # of Resource Records Checked: " + copyCount;
        } else {
            message = errorMessages +
                    "\n\nFinish checking records ... Total time: " + stopWatch.getPrettyTime() + "\n" +
                    "\nNumber of Resource Records checked: " + copyCount;
        }

        System.out.println("\nMismatches fixed: " + mismatchesFixed);
        System.out.println("Mismatches NOT fixed: " + mismatchesNotFixed);

        return message;
    }

    /**
     * Method to do certain task after the copy has completed
     */
    public void cleanUp() {

        saveURIMaps();

        copying = false;

        aspaceClient.startIndexer();

        String totalRecordsCopied = getTotalRecordsCopiedMessage();

        if(checkRepositoryMismatch) {
            print("\n\nFinish checking records ... Total time: " + stopWatch.getPrettyTime());
            print("\nNumber of Resource Records checked: " + copyCount);
        } else {
            print("\n\nFinish coping data ... Total time: " + stopWatch.getPrettyTime());
            print("\nNumber of Records copied: \n" + totalRecordsCopied);
        }

        print("\nNumber of errors/warnings: " + aspaceErrorCount);
    }

    /**
     * Method to return information about the ASpace and Migration tool version
     *
     * @return
     */
    public String getSystemInformation() {
        return dbCopyFrame.VERSION + "\n" + aspaceInformation;
    }

    /**
     * Method to set the boolean which specifies whether to stop copying the resources
     */
    public void stopCopy() {
        stopCopy = true;
    }

    /**
     * Method to check if the copying process is running
     *
     * @return
     */
    public boolean isCopying() {
        return copying;
    }

    /**
     * Method to set the whether the copying process is running
     *
     * @param copying
     */
    public void setCopying(boolean copying) {
        this.copying = copying;
    }

    /**
     * Method to try and free some memory by refreshing the hibernate session and running GC
     */
    private void freeMemory() {
        sourceRCD.refreshSession();

        if(outputConsole != null) {
            outputConsole.setText("");
        }

        // we may need to reset the the pause setting on the ASpace Indexer
        aspaceClient.pauseIndexer();

        Runtime runtime = Runtime.getRuntime();

        long freeMem = runtime.freeMemory();
        System.out.println("\nFree memory before GC: " + freeMem/1048576L + "MB");

        runtime.gc();

        freeMem = runtime.freeMemory();
        System.out.println("Free memory after GC:  " + freeMem/1048576L + "MB");

        System.out.println("Number of client threads: "  + getTotalASpaceClients() + "\n");
    }

    /**
     * Method to save the URI maps to a binary file
     */
    public void saveURIMaps() {
        HashMap uriMap = new HashMap();

        uriMap.put(REPOSITORY_KEY, repositoryURIMap);
        uriMap.put(REPOSITORY_MISMATCH_KEY, repositoryMismatchMap);
        uriMap.put(REPOSITORY_AGENT_KEY, repositoryAgentURIMap);

        HashMap<String, String> repoGroups = new HashMap<String, String>();
        for (String key: repositoryGroupURIMap.keySet()) {
            repoGroups.put(key, repositoryGroupURIMap.get(key).toString());
        }
        uriMap.put(REPOSITORY_GROUP_KEY, repoGroups);

        // only save maps we are going to need,
        // or we not generating from ASpace backend data
        uriMap.put(LOCATION_KEY, locationURIMap);
        uriMap.put(SUBJECT_KEY, subjectURIMap);
        uriMap.put(NAME_KEY, nameURIMap);
        uriMap.put(ACCESSION_KEY, accessionURIMap);
        uriMap.put(DIGITAL_OBJECT_KEY, digitalObjectURIMap);
        uriMap.put(RESOURCE_KEY, resourceURIMap);
        uriMap.put(ASSESSMENT_KEY, assessmentURIMap);

        uriMap.put(TOP_CONTAINER_KEY, TopContainerMapper.getAlreadyAddedStringForm());

        // store the record totals array list here also
        uriMap.put(RECORD_TOTAL_KEY, recordTotals);

        HashMap<String, Serializable> progress = new HashMap<String, Serializable>();
        progress.put("records_to_copy", recordsToCopy);
        progress.put("number_attempted", numAttempted);
        progress.put("number_successful", numSuccessful);
        progress.put("number_unlinked", numUnlinked);
        uriMap.put(RECORD_ATTEMPTED_KEY, progress);

        if(repositoryMismatchMap != null) {
            uriMap.put(REPOSITORY_MISMATCH_KEY, repositoryMismatchMap);
        }

        // save to file system now
        print("\nSaving URI Maps ...");

        try {
            ScriptDataUtils.saveScriptData(uriMapFile, uriMap);
        } catch (Exception e) {
            print("Unable to save URI map file " + uriMapFile.getName());
            System.out.println(e.getMessage());
        }
    }

    /**
     * Method to load the save URI maps
     */
    public void loadURIMaps() {
        try {
            HashMap uriMap  = (HashMap) ScriptDataUtils.getScriptData(uriMapFile);

            locationURIMap = (HashMap<Long,String>)uriMap.get(LOCATION_KEY);
            subjectURIMap = (HashMap<Long,String>)uriMap.get(SUBJECT_KEY);
            nameURIMap = (HashMap<Long,String>)uriMap.get(NAME_KEY);
            accessionURIMap = (HashMap<Long,String>)uriMap.get(ACCESSION_KEY);
            digitalObjectURIMap = (HashMap<Long,String>)uriMap.get(DIGITAL_OBJECT_KEY);
            resourceURIMap = (HashMap<Long,String>)uriMap.get(RESOURCE_KEY);
            assessmentURIMap = (HashMap<Long,String>)uriMap.get(ASSESSMENT_KEY);
            repositoryURIMap = (HashMap<String, String>)uriMap.get(REPOSITORY_KEY);
            repositoryAgentURIMap = (HashMap<String, String>)uriMap.get(REPOSITORY_AGENT_KEY);

            HashMap<String, String> repoGroupMap = (HashMap<String, String>)uriMap.get(REPOSITORY_GROUP_KEY);
            for (String key: repoGroupMap.keySet()) {
                repositoryGroupURIMap.put(key, new JSONObject(repoGroupMap.get(key)));
            }

            TopContainerMapper.setAlreadyAdded((HashMap<String, String>) uriMap.get(TOP_CONTAINER_KEY));

            HashMap<String, Serializable> progress = (HashMap<String, Serializable>)uriMap.get(RECORD_ATTEMPTED_KEY);
            LinkedList<String> saved = (LinkedList<String>) progress.get("records_to_copy");
            if (saved != null) recordsToCopy = saved;
            Integer numAttemptedInteger = (Integer) progress.get("number_attempted");
            if (numAttemptedInteger != null) numAttempted = numAttemptedInteger;
            Integer numSuccessfulInteger = (Integer) progress.get("number_successful");
            if (numSuccessfulInteger != null) numSuccessful = numSuccessfulInteger;
            Integer numUnlinkedInteger = (Integer) progress.get("number_unlinked");
            if (numUnlinkedInteger != null) numUnlinked = numUnlinkedInteger;

            // load the repository mismatch map if its not null
            if(uriMap.containsKey(REPOSITORY_MISMATCH_KEY)) {
                repositoryMismatchMap = (HashMap<String,String>)uriMap.get(REPOSITORY_MISMATCH_KEY);
            }

            // load the record totals so far
            if(uriMap.containsKey(RECORD_TOTAL_KEY)) {
                recordTotals = (ArrayList<String>)uriMap.get(RECORD_TOTAL_KEY);
            }

            print("Loaded URI Maps");
        } catch (Exception e) {
            print("Unable to load URI map file: " + uriMapFile.getName());
        }
    }

    /**
     * Method to see if the URI map file exist
     *
     * @return
     */
    public boolean uriMapFileExist() {
        return uriMapFile.exists();
    }

    /**
     * Method used to simulate the REST calls. Useful for testing memory usage and for setting baseline
     * data transfer time
     *
     * @param simulateRESTCalls
     */
    public void setSimulateRESTCalls(boolean simulateRESTCalls) {
        this.simulateRESTCalls = simulateRESTCalls;
    }

    /**
     * Method to see whether to check for repository mismatches, or copy the records
     */
    public void setCheckRepositoryMismatch() {
        checkRepositoryMismatch = true;
        simulateRESTCalls = true;
        useBatchImport = true;
        copying = true;
        repositoryMismatchMap = new HashMap<String, String>();
    }

    /**
     * Method to get the repository mismatch map
     *
     * @return
     */
    public HashMap<String, String> getRepositoryMismatchMap() {
        return repositoryMismatchMap;
    }

    /**
     * Method to set the repository mismatch map
     * @param repositoryMismatchMap
     */
    public void setRepositoryMismatchMap(HashMap<String, String> repositoryMismatchMap) {
        this.repositoryMismatchMap = repositoryMismatchMap;
    }

    /**
     * Method to set whether to use the batch import functionality. It only use
     * for transferring resource records for now.
     *
     * @param useBatchImport
     */
    public void setUseBatchImport(boolean useBatchImport) {
        this.useBatchImport = useBatchImport;
    }

    /**
     * Method to specify whether to delete the save resources.
     *
     * @param deleteSavedResources
     */
    public void setDeleteSavedResources(boolean deleteSavedResources) {
        this.deleteSavedResources = deleteSavedResources;
    }

    /**
     * Method to set the resources to copy
     *
     * @param resourcesIDsList
     */
    public void setResourcesToCopyList(ArrayList<String> resourcesIDsList) {
        if(resourcesIDsList.size() != 0) {
            this.resourcesIDsList = resourcesIDsList;
        } else {
            this.resourcesIDsList = null;
        }
    }

    /**
     * Method to see whether to set the extent in parts for BYU plugin
     *
     * @param b
     */
    public void setExtentPortionInParts(boolean b) {
        mapper.setExtentPortionInParts(b);
    }

    /**
     * Method to specify whether un-linked names and subject records should be copied
     *
     * @param ignoreNames
     * @param ignoreSubjects
     */
    public void setIgnoreUnlinkedRecords(boolean ignoreNames, boolean ignoreSubjects) {
        this.ignoreNames = ignoreNames;
        this.ignoreSubjects = ignoreSubjects;
    }

    /**
     * Method to get the current
     * @return
     */
    public String getCurrentRecordInfo()  {
        String info = "Current Record Type: " + currentRecordType +
                "\nRecord Identifier : " + currentRecordIdentifier;

        return info;
    }

    /**
     * A debug method for checking all ISO dates
     *
     * @param checkISODates
     */
    public void setCheckISODates(boolean checkISODates) {
        mapper.setCheckISODates(checkISODates);
    }

    /**
     * A debug method to process all ISO dates, making sure they are valid.
     */
    public void checkISODates() {
        mapper.checkISODates();
    }

    public JSONObject getRecord(String uri) throws Exception {
        String data = aspaceClient.get(uri,null);
        return new JSONObject(data);
    }

    public boolean getSimulateRESTCalls() {
        return simulateRESTCalls;
    }
}
