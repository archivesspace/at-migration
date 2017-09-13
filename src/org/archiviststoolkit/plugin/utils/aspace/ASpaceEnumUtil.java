package org.archiviststoolkit.plugin.utils.aspace;

import org.archiviststoolkit.model.ArchDescriptionDates;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * This is util class used to mapped ASpace enum list to AR lookup list items
 *
 * Created by IntelliJ IDEA.
 * User: nathan
 * Date: 12/7/12
 * Time: 9:52 AM
 */
public class ASpaceEnumUtil {
    private HashMap<String, String> languageCodes;
    private ArrayList<String> aspaceLanguageCodes = new ArrayList<String>();

    private ArrayList<String> aspaceSalutations = new ArrayList<String>();

    private HashMap<String, String> nameLinkCreatorCodes;

    private HashMap<String, JSONObject> dynamicEnums;

    // Hash map that maps AT values to AT codes
    private HashMap<String, String> lookupListValuesToCodes = new HashMap<String, String>();

    // Array list that hold values that are currently in the ASpace backend
    // They is needed, because for some reason, there are values in the AT records
    // which are not in the appropriate LookupListItem table
    private ArrayList<String> validContainerTypes = new ArrayList<String>();
    private ArrayList<String> validResourceTypes = new ArrayList<String>();

    private String[] ASpaceTermTypes = null;
    private String[] ASpaceSubjectSources = null;
    private String[] ASpaceNameSources = null;
    private String[] ASpaceNameRules = null;
    private String[] ASpaceNameDescriptionTypes = null;
    private String[] ASpaceLinkedAgentRoles = null;
    private String[] ASpaceExtentTypes = null;
    private String[] ASpaceDateEnums = null;
    private String[] ASpaceCollectionManagementRecordEnums = null;
    private String[] ASpaceDigitalObjectTypes = null;
    private String[] ASpaceNoteTypes = null;
    private String[] ASpaceIndexItemTypes = null;
    private String[] ASpaceResourceLevels = null;
    private String[] ASpaceFindingAidDescriptionRules = null;
    private String[] ASpaceFindingAidStatus = null;
    private String[] ASpaceInstanceTypes = null;
    private String[] ASpaceInstanceContainerTypes = null;
    private String[] ASpaceAcquisitionTypes = null;
    private String[] ASpaceFileVersionUseStatements = null;
    private String[] ASpaceAccessionResourceTypes = null;

    // A trying that is used to bypass
    public final static String UNMAPPED = "other_unmapped";

    public boolean returnATValue = true; // set this to return the AR value instead of UNMAPPED

    /**
     * Main constructor
     */
    public ASpaceEnumUtil() {
        initASpaceTermTypes();
        initASpaceSubjectSources();
        initASpaceNameRules();
        initASpaceNameSource();
        initASpaceNameDescriptionType();
        initASpaceExtentTypes();
        initASpaceDateEnums();
        initASpaceCollectionManagementRecordEnums();
        initASpaceLinkedAgentRole();
        initASpaceDigitalObjectType();
        initASpaceFileVersionUseStatements();
        initASpaceNoteTypes();
        initASpaceIndexItemTypes();
        initASpaceResourceLevels();
        initASpaceFindingAidDescriptionRules();
        initASpaceFindingAidStatus();
        initASpaceInstanceTypes();
        initASpaceInstanceContainerTypes();
        initASpaceAcquisitionTypes();
        initASpaceAccessionResourceTypes();
    }

    /**
     * Method to set the language code hash map
     */
    public void setLanguageCodes(HashMap<String, String> languageCodes) {
        this.languageCodes = languageCodes;
    }

    /**
     * Method to set the name link creator hash map
     * @param nameLinkCreatorCodes
     */
    public void setNameLinkCreatorCodes(HashMap<String, String> nameLinkCreatorCodes) {
        this.nameLinkCreatorCodes = nameLinkCreatorCodes;
    }

    /**
     * Initialize the array that hold term types
     */
    private void initASpaceTermTypes() {
        ASpaceTermTypes = new String[]{
                "cultural_context",     // 0
                "function",             // 1
                "geographic",           // 2
                "genre_form",           // 3
                "occupation",           // 4
                "style_period",         // 5
                "technique",            // 6
                "temporal",             // 7
                "topical",              // 8
                "uniform_title"         // 9
        };
    }

    /**
     * Method to map the AT subject term type to the ones in ASPace
     * @param atValue
     * @return
     */
    public String getASpaceTermType(String atValue) {
        if(atValue == null || atValue.isEmpty()) return UNMAPPED;

        atValue = atValue.toLowerCase();

        if(atValue.contains("function")) {
            return ASpaceTermTypes[1];
        } else if(atValue.contains("genre")) {
            return ASpaceTermTypes[3];
        } else if(atValue.contains("geographic")) {
            return ASpaceTermTypes[2];
        } else if(atValue.contains("occupation")) {
            return ASpaceTermTypes[4];
        } else if(atValue.contains("topical")) {
            return ASpaceTermTypes[8];
        } else if(atValue.contains("uniform")) {
            return ASpaceTermTypes[9];
        } else { // return others unknown
            return UNMAPPED;
        }
    }

    /**
     * Initialize the array that holds the subject source
     */
    private void initASpaceSubjectSources() {
        ASpaceSubjectSources = new String[] {
                "aat",      // 0
                "rbgenr",   // 1
                "tgn",      // 2
                "lcsh",     // 3
                "local",    // 4
                "mesh",     // 5
                "gmgpc",    // 6
//                "georeft",  // 7
//                "dot"       // 8
        };
    }

    /**
     * Method to map the subject source
     * ***THIS DOES NOT WORK RIGHT. SHOULD BE THE OTHER WAY AROUND (SHORT NAME TO LONG NAME)***
     * @param atValue
     * @return
     */
    public String getASpaceSubjectSource(String atValue) {

        if(atValue == null || atValue.isEmpty()) return "";

        atValue = atValue.toLowerCase();

        if(atValue.contains("art & architecture thesaurus")) {
            return ASpaceSubjectSources[0];
//        } else if (atValue.contains("dictionary of occupational titles")) {
////            return ASpaceSubjectSources[8];
        } else if (atValue.contains("genre terms: a thesaurus for use in rare book")) {
            return ASpaceSubjectSources[1];
//        } else if (atValue.contains("georef thesaurus")) {
//            return ASpaceSubjectSources[7];
        } else if (atValue.contains("getty thesaurus of geographic names")) {
            return ASpaceSubjectSources[2];
        } else if (atValue.contains("library of congress subject headings")) {
            return ASpaceSubjectSources[3];
        } else if (atValue.contains("local")) {
            return ASpaceSubjectSources[4];
        } else if (atValue.contains("medical subject headings")) {
            return ASpaceSubjectSources[5];
        } else if (atValue.contains("thesaurus for graphic materials")) {
            return ASpaceSubjectSources[6];
        } else if (returnATValue) {
            return lookupListValuesToCodes.get(atValue);
        } else {
            return UNMAPPED;
        }
    }

    /**
     * initialize the name source array
     */
    private void initASpaceNameSource() {
        ASpaceNameSources = new String[]{
                "local",    // 0
                "naf",      // 1
                "nad",      // 2
                "ulan"      // 3
        };

    }

    /**
     * Map the name source
     *
     * @param atValue
     * @return
     */
    public String getASpaceNameSource(String atValue) {
        if(atValue == null || atValue.isEmpty()) return "";

        atValue = atValue.toLowerCase();

        if(atValue.contains("local")) {
            return ASpaceNameSources[0];
        } else if(atValue.contains("naco")) {
            return ASpaceNameSources[1];
        } else if(atValue.contains("nad")) {
            return ASpaceNameSources[2];
        } else if(atValue.contains("union")) {
            return ASpaceNameSources[3];
        } else if(returnATValue) {
            return atValue;
        } else {
            return UNMAPPED;
        }
    }

    /**
     * Method to return if a name is direct or inverted order
     *
     * @param directOrder
     * @return
     */
    public String getASpaceNameOrder(Boolean directOrder) {
        if(directOrder) {
            return "direct";
        } else {
            return "inverted";
        }
    }

    /**
     * Method to initASpaceialize the name rules array
     */
    private void initASpaceNameRules() {
        ASpaceNameRules = new String[] {
                "local",    // 0
                "aacr",     // 1
                "dacs"      // 2
        };
    }

    /**
     * Method to map the name rule
     *
     * @param atValue
     * @return
     */
    public String getASpaceNameRule(String atValue) {
        if(atValue == null || atValue.isEmpty()) return "";

        atValue = atValue.toLowerCase();

        if(atValue.contains("anglo")) {
            return ASpaceNameRules[1];
        } else if(atValue.contains("describing")) {
            return ASpaceNameRules[2];
        } else if(atValue.contains("local")) {
            return ASpaceNameRules[0];
        } else if(returnATValue) {
            return atValue;
        } else {
            return UNMAPPED;
        }
    }

    /**
     * Get the Name description type
     */
    private void initASpaceNameDescriptionType() {
        ASpaceNameDescriptionTypes = new String[]{
                "administrative history",   // 0
                "biographical statement",   // 1
        };
    }

    /**
     * Method to map the name description type
     *
     * @param atValue
     * @return
     */
    public String getASpaceNameDescriptionType(String atValue) {
        if(atValue == null || atValue.isEmpty()) return "";

        atValue = atValue.toLowerCase();

        if(atValue.contains("administrative")) {
            return ASpaceNameDescriptionTypes[0];
        } else if(atValue.contains("biography")) {
            return ASpaceNameDescriptionTypes[1];
        } else if(returnATValue) {
            return atValue;
        } else {
            return UNMAPPED;
        }
    }

    /**
     * Method to return the ASpace salutation. There is currently no way to map this
     *
     * @param atValue
     * @return
     */
    public String getASpaceSalutation(String atValue) {
        if(atValue == null || atValue.isEmpty()) return "";

        atValue = atValue.toLowerCase();
        atValue = atValue.replace(".", "");

        if(aspaceSalutations.contains(atValue)) {
            return atValue;
        } else {
            return UNMAPPED;
        }
    }

    /**
     * Method to init ASpace extent type array
     */
    private void initASpaceExtentTypes() {
        ASpaceExtentTypes = new String[] {
                "cassettes",            // 0
                "cubic_feet",           // 1
                "files",                // 2
                "gigabytes",            // 3
                "leaves",               // 4
                "linear_feet",          // 5
                "megabytes",            // 6
                "photographic_prints",  // 7
                "photographic_slides",  // 8
                "reels",                // 9
                "sheets",               // 10
                "terabytes",            // 11
                "volumes"               // 12
        };
    }

    /**
     * Method to map the extent type
     *
     * @param atValue
     * @return
     */
    public String getASpaceExtentType(String atValue) {
        if(atValue == null || atValue.isEmpty()) return ASpaceExtentTypes[1];

        atValue = atValue.toLowerCase();

        if(atValue.contains("cubic")) {
            return ASpaceExtentTypes[1];
        } else if(atValue.contains("linear")) {
            return ASpaceExtentTypes[5];
        } else if(returnATValue) {
            return atValue;
        } else {
            return UNMAPPED;
        }
    }

    /**
     * Method initiate the array the hold information on dates
     */
    private void initASpaceDateEnums() {
        ASpaceDateEnums = new String[] {
                "single",       // 0
                "bulk",         // 1
                "inclusive",    // 2
                "broadcast",    // 3
                "copyright",    // 4
                "creation",     // 5
                "deaccession",  // 6
                "digitized",    // 7
                "issued",       // 8
                "modified",     // 9
                "publication",  // 10
                "other",        // 11
                "approximate",  // 12
                "inferred",     // 13
                "questionable", // 14
                "ce",           // 15
                "gregorian"     // 16
        };
    }

    /**
     * Method to return the date info. Most of these are direct one-to-one mapping
     * but implementing this way for greater flexibility
     *
     * @param atValue
     */
    public String getASpaceDateEnum(String atValue) {
        if(atValue == null || atValue.isEmpty()) return "other";

        atValue = atValue.toLowerCase();

        if(atValue.contains("creation")) {
            return ASpaceDateEnums[5];
        } else if(atValue.contains("recordkeeping")) {
            return ASpaceDateEnums[11];
        } else if(atValue.contains("publication")) {
            return ASpaceDateEnums[10];
        } else if(atValue.contains("broadcast")) {
            return ASpaceDateEnums[3];
        } else if (returnATValue) {
            return atValue;
        } else {
            return UNMAPPED;
        }
    }

    /**
     * Method to return the ASpace date era
     *
     * @param atValue
     * @return
     */
    public String getASpaceDateEra(String atValue) {
        if(atValue == null || atValue.isEmpty()) return "";

        atValue = atValue.toLowerCase();

        if(atValue.contains("ce")) {
            return ASpaceDateEnums[15];
        } else if (returnATValue) {
            return atValue;
        } else {
            return UNMAPPED;
        }
    }

    /**
     * Map the aspace calender
     *
     * @param atValue
     * @return
     */
    public String getASpaceDateCalender(String atValue) {
        if(atValue == null || atValue.isEmpty()) return "";

        atValue = atValue.toLowerCase();

        if(atValue.contains("gregorian")) {
            return ASpaceDateEnums[16];
        } else if (returnATValue) {
            return atValue;
        } else {
            return UNMAPPED;
        }
    }

    /**
     * map the date uncertainty
     *
     * @param archDescriptionDate
     * @return
     */
    public String getASpaceDateUncertainty(ArchDescriptionDates archDescriptionDate) {
        if(archDescriptionDate != null && archDescriptionDate.getCertainty() != null &&  archDescriptionDate.getCertainty()) {
            return ASpaceDateEnums[14];
        } else {
            return ASpaceDateEnums[13];
        }
    }

    /**
     * Method to get the date type
     *
     * @param archDescriptionDate
     * @return
     */
    public String getASpaceDateType(ArchDescriptionDates archDescriptionDate) {
        // TODO 12/10/2012 archivist will need to provide logic for mapping this
        return ASpaceDateEnums[2];
    }

    /**
     * Method to initASpaceialize array that holds enums of collection management records
     */
    private void initASpaceCollectionManagementRecordEnums() {
        ASpaceCollectionManagementRecordEnums = new String[] {
                "high",         // 0
                "medium",       // 1
                "low",          // 2
                "new",          // 3
                "in_progress",  // 4
                "completed"     // 5
        };
    }

    /**
     * Map an AT value to a collection management record enum
     *
     * @param atValue
     * @return
     */
    public String getASpaceCollectionManagementRecordProcessingPriority(String atValue) {
        atValue = atValue.toLowerCase();

        if(atValue.equals("high")) {
            return ASpaceCollectionManagementRecordEnums[0];
        } else if(atValue.equals("medium")) {
            return ASpaceCollectionManagementRecordEnums[1];
        } else if(atValue.equals("low")) {
            return ASpaceCollectionManagementRecordEnums[2];
        } else if(returnATValue) {
            return atValue;
        } else {
            return UNMAPPED;
        }
    }

    /**
     * Map an AT value to a collection management record enum
     * @param atValue
     * @return
     */
    public String getASpaceCollectionManagementRecordProcessingStatus(String atValue) {
        atValue = atValue.toLowerCase();

        if (atValue.equals("new")) {
            return ASpaceCollectionManagementRecordEnums[3];
        } else if(atValue.equals("in progress")) {
            return ASpaceCollectionManagementRecordEnums[4];
        } else if(atValue.equals("processed")) {
            return ASpaceCollectionManagementRecordEnums[5];
        } else if(returnATValue) {
            return atValue;
        } else {
            return UNMAPPED;
        }
    }

    /**
     * Method to initialize array that holds the name linked function roles
     */
    private void initASpaceLinkedAgentRole() {
        ASpaceLinkedAgentRoles = new String[] {
                "creator",  // 0
                "source",   // 1
                "subject"   // 2
        };
    }

    /**
     * Method to map the AT name link function to ASpace linked agent role
     *
     * @param atValue
     * @return
     */
    public String getASpaceLinkedAgentRole(String atValue) {
        if(atValue == null || atValue.isEmpty()) return "";

        atValue = atValue.toLowerCase();

        if (atValue.contains("creator")) {
            return ASpaceLinkedAgentRoles[0];
        } else if(atValue.contains("source")) {
            return ASpaceLinkedAgentRoles[1];
        } else if(atValue.contains("subject")) {
            return ASpaceLinkedAgentRoles[2];
        } else if(returnATValue) {
            return atValue;
        } else {
            return UNMAPPED;
        }
    }

    /**
     * Method to return the link agent relator
     * @param atValue
     * @return
     */
    public String getASpaceLinkedAgentRelator(String atValue) {
        if(nameLinkCreatorCodes.containsKey(atValue)) {
            return nameLinkCreatorCodes.get(atValue);
        } else {
            return atValue.trim();
        }
    }

    /**
     * Method to return the aspace language code
     *
     * @param atValue
     * @return
     */
    public String getASpaceLanguageCode(String atValue) {
        if(atValue == null || atValue.isEmpty()) return "und";

        if(languageCodes.containsKey(atValue)) {
            String code = languageCodes.get(atValue);

            // in the sandbox database the language code is all messed up, so we need
            // to use the default Undetermined code "und" instead
            if(code != null && aspaceLanguageCodes.contains(code)) {
                return code;
            } else {
                return "und";
            }
        } else {
            return UNMAPPED;
        }
    }

    /**
     * Method to initialize the array that holds the digital object types
     */
    private void initASpaceDigitalObjectType() {
        ASpaceDigitalObjectTypes = new String[]{
                "cartographic",                 // 0
                "mixed_materials",              // 1
                "moving_image",                 // 2
                "notated_music",                // 3
                "software_multimedia",          // 4
                "sound_recording",              // 5
                "sound_recording_musical",      // 6
                "sound_recording_nonmusical",   // 7
                "still_image",                  // 8
                "text"                          // 9
        };
    }

    /**
     * Method to return the type of the digital object
     *
     * @param atValue
     * @return
     */
    public String getASpaceDigitalObjectType(String atValue) {
        if(atValue == null || atValue.isEmpty()) return "";

        if (atValue.contains("cartographic")) {
            return ASpaceDigitalObjectTypes[0];
        } else if(atValue.contains("mixed material")) {
            return ASpaceDigitalObjectTypes[1];
        } else if(atValue.contains("moving image")) {
            return ASpaceDigitalObjectTypes[2];
        } else if(atValue.contains("notated music")) {
            return ASpaceDigitalObjectTypes[3];
        } else if(atValue.contains("software, multimedia")) {
            return ASpaceDigitalObjectTypes[4];
        } else if(atValue.equals("sound recording")) {
            return ASpaceDigitalObjectTypes[5];
        } else if(atValue.contains("sound recording-musical")) {
            return ASpaceDigitalObjectTypes[6];
        } else if(atValue.contains("sound recording-nonmusical")) {
            return ASpaceDigitalObjectTypes[7];
        } else if(atValue.contains("still image")) {
            return ASpaceDigitalObjectTypes[8];
        } else if(atValue.contains("text")) {
            return ASpaceDigitalObjectTypes[9];
        } else if(returnATValue) {
            return atValue;
        } else {
            return UNMAPPED;
        }
    }

    /**
     * Method to initialize the use statements
     */
    private void initASpaceFileVersionUseStatements() {
        ASpaceFileVersionUseStatements = new String[] {
                "audio-clip",           // 0
                "audio-master",         // 1
                "audio-master-edited",  // 2
                "audio-service",        // 3
                "audio-streaming",      // 4
                "image-master",         // 5
                "image-master-edited",  // 6
                "image-service",        // 7
                "image-service-edited", // 8
                "image-thumbnail",      // 9
                "text-codebook",        // 10
                "text-data",            // 11
                "text-data_definition", // 12
                "text-georeference",    // 13
                "text-ocr-edited",      // 14
                "text-ocr-unedited",    // 15
                "text-tei-transcripted",    // 16
                "text-tei-translated",      // 17
                "video-clip",               // 18
                "video-master",             // 19
                "video-master-edited",      // 20
                "video-service",            // 21
                "video-streaming"           // 22
        };
    }

    /**
     * Map the AT use statements to ASpace equivalents
     *
     * @param atValue
     * @return
     */
    public String getASpaceFileVersionUseStatement(String atValue) {
        if(atValue == null || atValue.isEmpty()) return "";

        atValue = atValue.toLowerCase();

        if(atValue.equals("audio-clip")) {
            return ASpaceFileVersionUseStatements[0];
        } else if(atValue.equals("audio-master")) {
            return ASpaceFileVersionUseStatements[1];
        } else if(atValue.equals("audio-master-edited")) {
            return ASpaceFileVersionUseStatements[2];
        } else if(atValue.equals("audio-service")) {
            return ASpaceFileVersionUseStatements[3];
        } else if(atValue.equals("audio-streaming")) {
            return ASpaceFileVersionUseStatements[4];
        } else if(atValue.equals("image-master")) {
            return ASpaceFileVersionUseStatements[5];
        } else if(atValue.equals("image-master-edited")) {
            return ASpaceFileVersionUseStatements[6];
        } else if(atValue.equals("image-service")) {
            return ASpaceFileVersionUseStatements[7];
        } else if(atValue.equals("image-service-edited")) {
            return ASpaceFileVersionUseStatements[8];
        } else if(atValue.equals("image-thumbnail")) {
            return ASpaceFileVersionUseStatements[9];
        } else if(atValue.equals("text-codebook")) {
            return ASpaceFileVersionUseStatements[10];
        } else if(atValue.equals("text-data")) {
            return ASpaceFileVersionUseStatements[11];
        } else if(atValue.equals("text-data_definition")) {
            return ASpaceFileVersionUseStatements[12];
        } else if(atValue.equals("text-georeference")) {
            return ASpaceFileVersionUseStatements[13];
        } else if(atValue.equals("text-ocr-edited")) {
            return ASpaceFileVersionUseStatements[14];
        } else if(atValue.equals("text-ocr-unedited")) {
            return ASpaceFileVersionUseStatements[15];
        } else if(atValue.equals("text-tei-transcripted")) {
            return ASpaceFileVersionUseStatements[16];
        } else if(atValue.equals("text-tei-translated")) {
            return ASpaceFileVersionUseStatements[17];
        } else if(atValue.equals("video-clip")) {
            return ASpaceFileVersionUseStatements[18];
        } else if(atValue.equals("video-master")) {
            return ASpaceFileVersionUseStatements[19];
        } else if(atValue.equals("video-master-edited")) {
            return ASpaceFileVersionUseStatements[20];
        } else if(atValue.equals("video-service")) {
            return ASpaceFileVersionUseStatements[21];
        } else if(atValue.equals("video-streaming")) {
            return ASpaceFileVersionUseStatements[22];
        } else if(returnATValue) {
            return atValue;
        } else {
            return UNMAPPED;
        }
    }

    /**
     * Method to initASpaceialize array containing ASpace note types
     */
    private void initASpaceNoteTypes() {
        ASpaceNoteTypes = new String[] {
                "abstract",                             // 0
                "accruals",                             // 1
                "appraisal",                            // 2
                "arrangement",                          // 3
                "bioghist",                             // 4
                "accessrestrict",                       // 5
                "userestrict",                          // 6
                "custodhist",                           // 7
                "dimensions",                           // 8
                "edition",                              // 9
                "extent",                               // 10
                "altformavail",                         // 11
                "originalsloc",                         // 12
                "fileplan",                             // 13
                "note",                                 // 14
                "physdesc",                             // 15
                "acqinfo",                              // 16
                "inscription",                          // 17
                "langmaterial",                         // 18
                "legalstatus",                          // 19
                "physloc",                              // 20
                "materialspec",                         // 21
                "otherfindaid",                         // 22
                "phystech",                             // 23
                "physdesc",                             // 24
                "physfacet",                            // 25
                "prefercite",                           // 26
                "processinfo",                          // 27
                "relatedmaterial",                      // 28
                "relatedmaterial",                      // 29
                "scopecontent",                         // 30
                "separatedmaterial",                    // 31
                "summary",                              // 32
                "odd",                                  // 33
                "bibliography"                          // 34
        };
    }

    /**
     * Method to return an ASpace digital object note type
     *
     * @param atValue
     * @return
     */
    public String getASpaceDigitalObjectNoteType(String atValue) {
        if(atValue == null || atValue.isEmpty()) return ASpaceNoteTypes[14];

        atValue = atValue.toLowerCase();

        if(atValue.contains("biographical/historical")) {
            return ASpaceNoteTypes[4];
        } else if(atValue.contains("conditions governing access")) {
            return ASpaceNoteTypes[5];
        } else if(atValue.contains("conditions governing use")) {
            return ASpaceNoteTypes[6];
        } else if(atValue.contains("custodial history")) {
            return ASpaceNoteTypes[7];
        } else if(atValue.contains("dimensions")) {
            return ASpaceNoteTypes[8];
        } else if(atValue.contains("existence and location of copies")) {
            return ASpaceNoteTypes[11];
        } else if(atValue.contains("existence and location of originals")) {
            return ASpaceNoteTypes[12];
        } else if(atValue.contains("general note")) {
            return ASpaceNoteTypes[14];
        } else if(atValue.contains("general physical description")) {
            return ASpaceNoteTypes[24];
        } else if(atValue.contains("immediate source of acquisition")) {
            return ASpaceNoteTypes[16];
        } else if(atValue.contains("language of materials")) {
            return ASpaceNoteTypes[18];
        } else if(atValue.contains("legal status")) {
            return ASpaceNoteTypes[19];
        } else if(atValue.contains("preferred citation")) {
            return ASpaceNoteTypes[26];
        } else if(atValue.contains("processing information")) {
            return ASpaceNoteTypes[27];
        } else if(atValue.contains("related archival materials")) {
            return ASpaceNoteTypes[29];
        } else {
            return ASpaceNoteTypes[14];  // just tag this note as a note type
        }
    }

    /**
     * Method to map the AT multipart note type to the ASpace equivalence
     *
     * @param atValue
     * @return
     */
    public String getASpaceMultiPartNoteType(String atValue) {
        if(atValue == null || atValue.isEmpty()) return "";

        atValue = atValue.toLowerCase();

        if(atValue.contains("accruals")) {
            return ASpaceNoteTypes[1];
        } else if(atValue.contains("appraisal")) {
            return ASpaceNoteTypes[2];
        } else if(atValue.contains("arrangement")) {
            return ASpaceNoteTypes[3];
        } else if(atValue.contains("biographical/historical")) {
            return ASpaceNoteTypes[4];
        } else if(atValue.contains("bibliography")) {
            return ASpaceNoteTypes[34];
        } else if(atValue.contains("conditions governing access")) {
            return ASpaceNoteTypes[5];
        } else if(atValue.contains("conditions governing use")) {
            return ASpaceNoteTypes[6];
        } else if(atValue.contains("custodial history")) {
            return ASpaceNoteTypes[7];
        } else if(atValue.contains("dimensions")) {
            return ASpaceNoteTypes[8];
        } else if(atValue.contains("existence and location of copies")) {
            return ASpaceNoteTypes[11];
        } else if(atValue.contains("existence and location of originals")) {
            return ASpaceNoteTypes[12];
        } else if(atValue.contains("file plan")) {
            return ASpaceNoteTypes[13];
        } else if(atValue.contains("general note")) {
            return ASpaceNoteTypes[33];
        } else if(atValue.contains("immediate source of acquisition")) {
            return ASpaceNoteTypes[16];
        } else if(atValue.contains("legal status")) {
            return ASpaceNoteTypes[19];
        } else if(atValue.contains("other finding aids")) {
            return ASpaceNoteTypes[22];
        } else if(atValue.contains("physical characteristics and technical requirements")) {
            return ASpaceNoteTypes[23];
        } else if(atValue.contains("preferred citation")) {
            return ASpaceNoteTypes[26];
        } else if(atValue.contains("processing information")) {
            return ASpaceNoteTypes[27];
        } else if(atValue.contains("related archival materials")) {
            return ASpaceNoteTypes[28];
        } else if(atValue.contains("scope and contents")) {
            return ASpaceNoteTypes[30];
        } else if(atValue.contains("separated materials")) {
            return ASpaceNoteTypes[31];
        } else {
            return UNMAPPED;
        }
    }

    /**
     * Method to map the AT single part note type to the ASpace equivalence
     *
     * @param atValue
     * @return
     */
    public String getASpaceSinglePartNoteType(String atValue) {
        if(atValue == null || atValue.isEmpty()) return "";

        atValue = atValue.toLowerCase();

        if (atValue.contains("abstract")) {
            return ASpaceNoteTypes[0];
        } else if(atValue.contains("general physical description")) {
            return ASpaceNoteTypes[15];
        } else if(atValue.contains("language of materials")) {
            return ASpaceNoteTypes[18];
        } else if(atValue.contains("location note")) {
            return ASpaceNoteTypes[20];
        } else if(atValue.contains("material specific details")) {
            return ASpaceNoteTypes[21];
        } else if(atValue.contains("physical facet")) {
            return ASpaceNoteTypes[25];
        } else {
            return UNMAPPED;
        }
    }

    /**
     * Method to return the enumeration value for the order list note
     *
     * @param atValue
     * @return
     */
    public String getASpaceOrderedListNoteEnumeration(String atValue) {
        if(atValue == null || atValue.isEmpty()) {
            return "null";
        } else {
            return atValue;
        }
    }

    /**
     * initialize array containing the ASpace index note item type
     */
    private void initASpaceIndexItemTypes() {
        ASpaceIndexItemTypes = new String[] {
                "corporate_entity", // 0
                "family",           // 1
                "function",         // 2
                "geographic_name",  // 3
                "name",             // 4
                "occupation",       // 5
                "person",           // 6
                "subject",          // 7
                "title"             // 8
        };
    }

    /**
     * Method to convert AT index item types to ASpace note index types
     *
     * @param atValue
     * @return
     */
    public String getASpaceIndexItemType(String atValue) {
        if(atValue == null || atValue.isEmpty()) return "";

        atValue = atValue.toLowerCase();

        if (atValue.contains("corporate name")) {
            return ASpaceIndexItemTypes[0];
        } else if(atValue.contains("genre form")) {
            return ASpaceIndexItemTypes[7];
        } else if(atValue.contains("name")) {
            return ASpaceIndexItemTypes[4];
        } else if(atValue.contains("occupation")) {
            return ASpaceIndexItemTypes[5];
        } else if(atValue.contains("personal name")) {
            return ASpaceIndexItemTypes[6];
        } else if(atValue.contains("subject")) {
            return ASpaceIndexItemTypes[7];
        } else if(atValue.contains("family name")) {
            return ASpaceIndexItemTypes[1];
        } else if(atValue.contains("function")) {
            return ASpaceIndexItemTypes[2];
        } else if(atValue.contains("geographic name")) {
            return ASpaceIndexItemTypes[3];
        }else if(atValue.contains("Title")) {
            return ASpaceIndexItemTypes[8];
        } else {
            return ASpaceIndexItemTypes[8];
        }
    }

    /**
     * Initialize the array that stores resource levels
     */
    private void initASpaceResourceLevels() {
        ASpaceResourceLevels = new String[] {
                "class",            // 0
                "collection",       // 1
                "file",             // 2
                "fonds",            // 3
                "item",             // 4
                "otherlevel",       // 5
                "recordgrp",        // 6
                "series",           // 7
                "subfonds",         // 8
                "subgrp",           // 9
                "subseries"         // 10
        };
    }

    /**
     * Method to map the AT resource level to ASpace equivalent
     *
     * @param atValue
     * @return
     */
    public String getASpaceResourceLevel(String atValue) {
        if(atValue == null || atValue.isEmpty()) return "collection";

        atValue = atValue.toLowerCase();

        if (atValue.contains("class")) {
            return ASpaceResourceLevels[0];
        } else if(atValue.contains("collection")) {
            return ASpaceResourceLevels[1];
        } else if(atValue.contains("file")) {
            return ASpaceResourceLevels[2];
        } else if(atValue.contains("fonds")) {
            return ASpaceResourceLevels[3];
        } else if(atValue.contains("item")) {
            return ASpaceResourceLevels[4];
        } else if(atValue.contains("otherlevel")) {
            return ASpaceResourceLevels[5];
        } else if(atValue.contains("recordgrp")) {
            return ASpaceResourceLevels[6];
        } else if(atValue.equals("series")) {
            return ASpaceResourceLevels[7];
        } else if(atValue.contains("subfonds")) {
            return ASpaceResourceLevels[8];
        } else if(atValue.contains("subgrp")) {
            return ASpaceResourceLevels[9];
        } else if(atValue.contains("subseries")) {
            return ASpaceResourceLevels[10];
        } else {
            return UNMAPPED;
        }
    }

    /**
     * Method to map the AT resource component level to ASpace equivalent
     *
     * @param atValue
     * @return
     */
    public String getASpaceArchivalObjectLevel(String atValue) {
        return getASpaceResourceLevel(atValue);
    }

    /**
     * Method to initASpace array that holds the description rules
     */
    private void initASpaceFindingAidDescriptionRules() {
        ASpaceFindingAidDescriptionRules = new String[] {
                "aacr", // 0
                "cco",  // 1
                "dacs", // 2
                "rad",  // 3
                "isadg" // 4
        };
    }

    /**
     * Method to return the map find description
     *
     * @param atValue
     * @return
     */
    public String getASpaceFindingAidDescriptionRule(String atValue) {
        atValue = atValue.toLowerCase();

        if (atValue.contains("anglo-american")) {
            return ASpaceFindingAidDescriptionRules[0];
        } else if(atValue.contains("cataloging cultural")) {
            return ASpaceFindingAidDescriptionRules[1];
        } else if(atValue.contains("describing archives")) {
            return ASpaceFindingAidDescriptionRules[2];
        } else if(atValue.contains("rules for archival description")) {
            return ASpaceFindingAidDescriptionRules[3];
        } else if(returnATValue) {
            return atValue;
        } else {
            return UNMAPPED;
        }
    }

    /**
     * Method to initASpace the finding aid status
     */
    private void initASpaceFindingAidStatus() {
        ASpaceFindingAidStatus = new String[] {
                "completed",        // 0
                "in_progress",      // 1
                "under_revision",   // 2
                "unprocessed"       // 3
        };
    }

    /**
     * Method to return the finding aid status
     *
     * @param atValue
     * @return
     */
    public String getASpaceFindingAidStatus(String atValue) {
        atValue = atValue.toLowerCase();

        if (atValue.contains("completed")) {
            return ASpaceFindingAidStatus[0];
        } else if(atValue.contains("in_process")) {
            return ASpaceFindingAidStatus[1];
        } else if(atValue.contains("under_revision")) {
            return ASpaceFindingAidStatus[2];
        } else if(atValue.contains("unprocessed")) {
            return ASpaceFindingAidStatus[3];
        } else if (returnATValue) {
            return atValue;
        } else {
            return UNMAPPED;
        }
    }

    /**
     * Method to initialize the ASpace instance types
     */
    private void initASpaceInstanceTypes() {
        ASpaceInstanceTypes = new String[] {
                "audio",                // 0
                "books",                // 1
                "computer_disks",       // 2
                "graphic_materials",    // 3
                "maps",                 // 4
                "microform",            // 5
                "mixed_materials",      // 6
                "moving_images",        // 7
                "realia",               // 8
                "text",                 // 9
                "digital_object"        // 10
        };
    }

    /**
     * Method to return the equivalent ASpace instance type
     *
     * @param atValue
     * @return
     */
    public String getASpaceInstanceType(String atValue) {
        if(atValue == null || atValue.isEmpty()) return "";

        atValue = atValue.toLowerCase().trim();

        if(atValue.contains("audio")) {
            return ASpaceInstanceTypes[0];
        } else if(atValue.contains("books")) {
            return ASpaceInstanceTypes[1];
        } else if(atValue.contains("computer disks")) {
            return ASpaceInstanceTypes[2];
        } else if(atValue.contains("digital object")) {
            return ASpaceInstanceTypes[10];
        } else if(atValue.contains("graphic materials")) {
            return ASpaceInstanceTypes[3];
        } else if(atValue.contains("maps")) {
            return ASpaceInstanceTypes[4];
        } else if(atValue.contains("microform")) {
            return ASpaceInstanceTypes[5];
        } else if(atValue.contains("mixed materials")) {
            return ASpaceInstanceTypes[6];
        } else if(atValue.contains("moving Images")) {
            return ASpaceInstanceTypes[7];
        } else if(atValue.contains("realia")) {
            return ASpaceInstanceTypes[8];
        } else if(atValue.contains("text")) {
            return ASpaceInstanceTypes[9];
        } else if(returnATValue) {
            return atValue;
        } else {
            return UNMAPPED;
        }
    }

    /**
     * Method to initASpace array holding the ASpace container types
     */
    private void initASpaceInstanceContainerTypes() {
        ASpaceInstanceContainerTypes = new String[] {
                "box",      // 0
                "carton",   // 1
                "case",     // 2
                "folder",   // 3
                "frame",    // 4
                "object",   // 5
                "page",     // 6
                "reel",     // 7
                "volume"    // 8
        };
    }

    /**
     * Method to return the equivalent ASpace instance container type
     *
     * if statements with "&& returnATValue" should really be removed, but depending on if the
     * enum is ASpace is expanded then this will save some work
     *
     * @param atValue
     * @return
     */
    public String getASpaceInstanceContainerType(String atValue) {
        if(atValue == null || atValue.trim().isEmpty()) {
            return "item";
        }

        atValue = atValue.toLowerCase().trim();

        if(atValue.equals("bin") && returnATValue) {
            return atValue;
        } else if(atValue.equals("box")) {
            return ASpaceInstanceContainerTypes[0];
        } else if(atValue.equals("box-folder") && returnATValue) {
            return atValue;
        } else if(atValue.equals("carton")) {
            return ASpaceInstanceContainerTypes[1];
        } else if(atValue.equals("cassette") && returnATValue) {
            return atValue;
        } else if(atValue.equals("disk") && returnATValue) {
            return atValue;
        } else if(atValue.equals("drawer") && returnATValue) {
            return atValue;
        } else if(atValue.equals("folder")) {
            return ASpaceInstanceContainerTypes[3];
        } else if(atValue.equals("frame")) {
            return ASpaceInstanceContainerTypes[4];
        } else if(atValue.equals("map-case") && returnATValue) {
            return atValue;
        } else if(atValue.equals("object")) {
            return ASpaceInstanceContainerTypes[5];
        } else if(atValue.equals("oversize") && returnATValue) {
            return atValue;
        } else if(atValue.equals("page")) {
            return ASpaceInstanceContainerTypes[6];
        } else if(atValue.equals("reel")) {
            return ASpaceInstanceContainerTypes[7];
        } else if(atValue.equals("reel-frame") && returnATValue) {
            return atValue;
        } else if(atValue.equals("volume")) {
            return ASpaceInstanceContainerTypes[8];
        } else if(returnATValue) {
            if(validContainerTypes.contains(atValue)) {
                return atValue;
            } else {
                return "item";
            }
        } else {
            return UNMAPPED;
        }
    }

    /**
     * Method to init the aspace term types
     */
    private void initASpaceAcquisitionTypes() {
        ASpaceAcquisitionTypes = new String[] {
                "deposit",  // 0
                "gift",     // 1
                "purchase", // 2
                "transfer"  // 3
        };
    }

    /**
     * Method to get the ASpace type
     *
     * @param atValue
     * @return
     */
    public String getASpaceAcquisitionType(String atValue) {
        if(atValue == null || atValue.isEmpty()) return "";

        atValue = atValue.toLowerCase();

        if (atValue.contains("deposit")) {
            return ASpaceAcquisitionTypes[0];
        } else if(atValue.contains("gift")) {
            return ASpaceAcquisitionTypes[1];
        } else if(atValue.contains("purchase")) {
            return ASpaceAcquisitionTypes[2];
        } else if(atValue.contains("transfer")) {
            return ASpaceAcquisitionTypes[3];
        } else if(returnATValue) {
            return atValue;
        } else {
            return UNMAPPED;
        }
    }

    /**
     * Method to init the ASpace accession resource type
     */
    private void initASpaceAccessionResourceTypes() {
        ASpaceAccessionResourceTypes = new String[] {
            "collection",
            "papers",
            "publications",
            "records"
        };
    }

    /**
     * Method to return the AccessionResourceType
     *
     * @param atValue
     * @return
     */
    public String getASpaceAccessionResourceType(String atValue) {
        if(atValue == null || atValue.isEmpty()) return "collection";

        atValue = atValue.toLowerCase();

        if (atValue.equals("collection")) {
            return ASpaceAccessionResourceTypes[0];
        } else if(atValue.equals("papers")) {
            return ASpaceAccessionResourceTypes[1];
        } else if(atValue.equals("records")) {
            return ASpaceAccessionResourceTypes[3];
        } else if(returnATValue) {
            if(validResourceTypes.contains(atValue)) {
                return atValue;
            } else {
                return "collection";
            }
        } else {
            return UNMAPPED;
        }
    }

    /**
     * Method to set the hash map that holds the dynamic enums
     *
     * @param dynamicEnums
     */
    public void setASpaceDynamicEnums(HashMap<String, JSONObject> dynamicEnums) {
        this.dynamicEnums = dynamicEnums;

        // now extract the language codes, and salutations so we can compare with the AT values
        JSONObject languageCodeEnumJS = dynamicEnums.get("language_iso639_2");
        JSONObject salutationEnumJS = dynamicEnums.get("agent_contact_salutation");

        try {
            JSONArray valuesJA = languageCodeEnumJS.getJSONArray("values");
            for(int i = 0; i < valuesJA.length(); i++) {
                aspaceLanguageCodes.add(valuesJA.getString(i));
            }

            valuesJA = salutationEnumJS.getJSONArray("values");
            for(int i = 0; i < valuesJA.length(); i++) {
                aspaceSalutations.add(valuesJA.getString(i));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to return the correct enum list based on the AT list name. Only editable list in the
     * AT are returned
     *
     * @param listName
     * @return
     */
    public JSONObject getDynamicEnum(String listName) {

        if (listName.equalsIgnoreCase("Name link function")) {
            return  dynamicEnums.get("linked_agent_archival_record_roles");
        } else if (listName.equalsIgnoreCase("Name source")) {
            return  dynamicEnums.get("name_source");
        } else if (listName.equalsIgnoreCase("Name rules")) {
            return dynamicEnums.get("name_rule");
        } else if (listName.equalsIgnoreCase("Name description type")) {
            return dynamicEnums.get("name_description_type");
        } else if (listName.equalsIgnoreCase("Acquisition type")) {
            return dynamicEnums.get("accession_acquisition_type");
        } else if (listName.equalsIgnoreCase("Resource type")) {
            return dynamicEnums.get("accession_resource_type");
        } else if (listName.equalsIgnoreCase("Processing Priorities")) {
            return dynamicEnums.get("collection_management_processing_priority");
        } else if (listName.equalsIgnoreCase("Processing Statuses")) {
            return dynamicEnums.get("collection_management_processing_status");
        } else if (listName.equalsIgnoreCase("Era")) {
            return dynamicEnums.get("date_era");
        } else if (listName.equalsIgnoreCase("Calendar")) {
            return dynamicEnums.get("date_calendar");
        } else if (listName.equalsIgnoreCase("Digital object types")) {
            return dynamicEnums.get("digital_object_digital_object_type");
        } else if (listName.equalsIgnoreCase("Events")) {
            return dynamicEnums.get("event_event_type");
        } else if (listName.equalsIgnoreCase("Extent type")) {
            return dynamicEnums.get("extent_extent_type");
        } else if (listName.equalsIgnoreCase("Container types")) {
            return dynamicEnums.get("container_type");
        } else if (listName.equalsIgnoreCase("Description rules")) {
            return dynamicEnums.get("resource_finding_aid_description_rules");
        } else if (listName.equalsIgnoreCase("Finding aid status")) {
            return dynamicEnums.get("resource_finding_aid_status");
        } else if (listName.equalsIgnoreCase("Instance types")) {
            return dynamicEnums.get("instance_instance_type");
        } else if (listName.equalsIgnoreCase("Subject term source")) {
            return dynamicEnums.get("subject_source");
        } else if (listName.equalsIgnoreCase("File use attributes")) {
            return dynamicEnums.get("file_version_use_statement");
        } else {
            return null;
        }
    }

    /**
     * Method to see if an AT value maps to an ASpace enum value
     *
     * @param enumListName
     * @param atValue
     */
    public boolean mapsToASpaceEnumValue(String enumListName, String atValue, String code) {
        // if code is not empty then store it in hash so we can lookup later
        if(!code.isEmpty()) {
            lookupListValuesToCodes.put(atValue, code);
        }

        String mappedValue = "";

        if (enumListName.equals("linked_agent_archival_record_roles")) {
            mappedValue = getASpaceLinkedAgentRole(atValue);
        } else if(enumListName.equals("name_source")) {
            mappedValue = getASpaceNameSource(atValue);
        } else if(enumListName.equals("name_rule")) {
            mappedValue = getASpaceNameRule(atValue);
        } else if(enumListName.equals("name_description_type")) {
            mappedValue = getASpaceNameDescriptionType(atValue);
        } else if(enumListName.equals("accession_acquisition_type")) {
            mappedValue = getASpaceAcquisitionType(atValue);
        } else if(enumListName.equals("accession_resource_type")) {
            validResourceTypes.add(atValue.toLowerCase());
            mappedValue = getASpaceAccessionResourceType(atValue);
        } else if(enumListName.equals("collection_management_processing_priority")) {
            mappedValue = getASpaceCollectionManagementRecordProcessingPriority(atValue);
        } else if(enumListName.equals("collection_management_processing_status")) {
            mappedValue = getASpaceCollectionManagementRecordProcessingStatus(atValue);
        } else if(enumListName.equals("date_era")) {
            mappedValue = getASpaceDateEra(atValue);
        } else if(enumListName.equals("date_calendar")) {
            mappedValue = getASpaceDateCalender(atValue);
        } else if(enumListName.equals("digital_object_digital_object_type")) {
            mappedValue = getASpaceDigitalObjectType(atValue);
        } else if(enumListName.equals("extent_extent_type")) {
            mappedValue = getASpaceExtentType(atValue);
        } else if(enumListName.equals("container_type")) {
            validContainerTypes.add(atValue.toLowerCase());
            mappedValue = getASpaceInstanceContainerType(atValue);
        } else if(enumListName.equals("resource_finding_aid_description_rules")) {
            mappedValue = getASpaceFindingAidDescriptionRule(atValue);
        } else if(enumListName.equals("resource_finding_aid_status")) {
            mappedValue = getASpaceFindingAidStatus(atValue);
        } else if(enumListName.equals("instance_instance_type")) {
            mappedValue = getASpaceInstanceType(atValue);
        } else if(enumListName.equals("subject_source")) {
            mappedValue = getASpaceSubjectSource(atValue);
        } else if(enumListName.equals("file_version_use_statement")) {
            mappedValue = getASpaceFileVersionUseStatement(atValue);
        } else {
            mappedValue = UNMAPPED;
        }

        // see if there is a valid mapped value
        if (mappedValue.equals(UNMAPPED)) {
            return false;
        } else {
            return true;
        }
    }


}
