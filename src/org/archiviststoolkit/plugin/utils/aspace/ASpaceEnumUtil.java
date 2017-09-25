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

    private static HashMap<String, JSONObject> dynamicEnums;

    // Hash map that maps AT values to AT codes
    private HashMap<String, String> lookupListValuesToCodes = new HashMap<String, String>();

    // Array list that hold values that are currently in the ASpace backend
    // They is needed, because for some reason, there are values in the AT records
    // which are not in the appropriate LookupListItem table
    private ArrayList<String> validContainerTypes = new ArrayList<String>();
    private ArrayList<String> validResourceTypes = new ArrayList<String>();

    private String[] ASpaceSubjectSources = null;

    // A trying that is used to bypass
    public final static String UNMAPPED = "other_unmapped";

    public boolean returnATValue = true; // set this to return the AR value instead of UNMAPPED

//    /**
//     * Main constructor
//     */
//    public ASpaceEnumUtil() {
//        initASpaceSubjectSources();
//    }

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
     * Method to map the AT subject term type to the ones in ASPace
     * @param atValue
     * @return
     */
    public Object[] getASpaceTermType(String atValue) {
        if (atValue.contains("uniform")) {
            atValue = "uniform_title";
        }
        return getASpaceEnumValue("subject_term_type", atValue, false, "topical");
    }

//    /**
//     * Initialize the array that holds the subject source
//     */
//    private void initASpaceSubjectSources() {
//        ASpaceSubjectSources = new String[] {
//                "aat",      // 0
//                "rbgenr",   // 1
//                "tgn",      // 2
//                "lcsh",     // 3
//                "local",    // 4
//                "mesh",     // 5
//                "gmgpc",    // 6
////                "georeft",  // 7
////                "dot"       // 8
//        };
//    }

    /**
     * Method to map the subject source
     * @param atValue
     * @return
     */
    public Object[] getASpaceSubjectSource(String atValue) {

        String code;

//        if(atValue == null || atValue.isEmpty()) return "";

        atValue = atValue.toLowerCase();

        if(atValue.contains("art & architecture thesaurus")) {
            code = "aat";
//            return ASpaceSubjectSources[0];
//        } else if (atValue.contains("dictionary of occupational titles")) {
////            return ASpaceSubjectSources[8];
        } else if (atValue.contains("genre terms: a thesaurus for use in rare book")) {
            code = "rbgenr";
//            return ASpaceSubjectSources[1];
        } else if (atValue.contains("getty thesaurus of geographic names")) {
            code = "tgn";
//            return ASpaceSubjectSources[2];
        } else if (atValue.contains("library of congress subject headings")) {
            code = "lcsh";
//            return ASpaceSubjectSources[3];
        } else if (atValue.contains("local")) {
            code = "local";
//            return ASpaceSubjectSources[4];
        } else if (atValue.contains("medical subject headings")) {
            code = "mesh";
//            return ASpaceSubjectSources[5];
        } else if (atValue.contains("thesaurus for graphic materials")) {
            code =  "gmgpc";
//        } else if (returnATValue) {
//            return lookupListValuesToCodes.get(atValue);
        } else {
            code = lookupListValuesToCodes.get(atValue);
//            return UNMAPPED;
        }
        return getASpaceEnumValue("subject_source", code);
    }

    /**
     * Map the name source
     *
     * @param atValue
     * @return
     */
    public Object[] getASpaceNameSource(String atValue) {

        atValue = atValue.toLowerCase();
        if (atValue == null || atValue.trim().isEmpty()) atValue = "local";

        if(atValue.contains("naco")) {
            atValue = "naf";
        } else if(atValue.contains("nad")) {
            atValue = "nad";
        } else if(atValue.contains("union")) {
            atValue = "ulan";
        }
        return getASpaceEnumValue("name_source", atValue);
    }

    /**
     * Method to return if a name is direct or inverted order
     *
     * @param directOrder
     * @return
     */
    public Object[] getASpaceNameOrder(Boolean directOrder) {
        String atValue;
        if(directOrder) {
            atValue = "direct";
        } else {
            atValue = "inverted";
        }
        return getASpaceEnumValue("name_person_name_order", atValue, false, "direct");
    }

    /**
     * Method to map the name rule
     *
     * @param atValue
     * @return
     */
    public Object[] getASpaceNameRule(String atValue) {
        if(atValue == null || atValue.isEmpty()) atValue = "local";

        atValue = atValue.toLowerCase();

        if(atValue.contains("anglo")) {
            atValue = "aacr";
        } else if(atValue.contains("describing")) {
            atValue = "dacs";
        }
        return getASpaceEnumValue("name_rule", atValue);
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
     * Method to map the extent type
     *
     * @param atValue
     * @return
     */
    public Object[] getASpaceExtentType(String atValue) {
        if(atValue == null || atValue.isEmpty()) atValue = "unknown";
        return getASpaceEnumValue("extent_extent_type", atValue);
    }

    /**
     * Method to return the date info. Most of these are direct one-to-one mapping
     * but implementing this way for greater flexibility
     *
     * @param atValue
     */
    public Object[] getASpaceDateEnum(String atValue) {
        if(atValue == null || atValue.isEmpty() || atValue.contains("recordkeeping")) atValue = "other";
        return getASpaceEnumValue("date_label", atValue);
    }

    /**
     * Method to return the ASpace date era
     *
     * @param atValue
     * @return
     */
    public Object[] getASpaceDateEra(String atValue) {
        return getASpaceEnumValue("date_era", atValue);
    }

    /**
     * Map the aspace calender
     *
     * @param atValue
     * @return
     */
    public Object[] getASpaceDateCalender(String atValue) {
        return getASpaceEnumValue("date_calendar", atValue);
    }

    /**
     * map the date uncertainty
     *
     * @param archDescriptionDate
     * @return
     */
    public Object[] getASpaceDateCertainty(ArchDescriptionDates archDescriptionDate) {
        String atValue;
        if(archDescriptionDate != null && archDescriptionDate.getCertainty() != null &&  archDescriptionDate.getCertainty()) {
            atValue = "inferred";
        } else {
            atValue = "questionable";
        }
        return getASpaceEnumValue("date_certainty", atValue, false, "questionable");
    }

    /**
     * Method to get the date type
     *
     * @param archDescriptionDate
     * @return
     */
    public Object[] getASpaceDateType(ArchDescriptionDates archDescriptionDate) {
        // TODO 12/10/2012 archivist will need to provide logic for mapping this
        return getASpaceEnumValue("date_type", "inclusive", false, "inclusive");
    }

    /**
     * Map an AT value to a collection management record enum
     *
     * @param atValue
     * @return
     */
    public Object[] getASpaceCollectionManagementRecordProcessingPriority(String atValue) {
        return getASpaceEnumValue("collection_management_processing_priority", atValue);
    }

    /**
     * Map an AT value to a collection management record enum
     * @param atValue
     * @return
     */
    public Object[] getASpaceCollectionManagementRecordProcessingStatus(String atValue) {
        atValue = atValue.toLowerCase();

        if(atValue.equals("processed")) {
            atValue = "completed";
        }
        return getASpaceEnumValue("collection_management_processing_status", atValue);
    }

    /**
     *
     * @param atValue
     * @return
     */
    public Object[] getASpaceLinkedAgentRole(String atValue) {
        return getASpaceEnumValue("linked_agent_role", atValue, false, null);
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
     * Method to return the type of the digital object
     *
     * @param atValue
     * @return
     */
    public Object[] getASpaceDigitalObjectType(String atValue) {
        if(atValue.contains("mixed material")) {
            atValue = "mixed_materials";
        } else if(atValue.contains("software, multimedia")) {
            atValue = "software_multimedia";
        } else if(atValue.contains("sound recording-musical")) {
            atValue = "sound_recording_musical";
        } else if(atValue.contains("sound recording-nonmusical")) {
            atValue = "sound_recording_nonmusical";
        }
        return getASpaceEnumValue("digital_object_digital_object_type", atValue);
    }

    /**
     * Map the AT use statements to ASpace equivalents
     *
     * @param atValue
     * @return
     */
    public Object[] getASpaceFileVersionUseStatement(String atValue) {
        return getASpaceEnumValue("file_version_use_statement", atValue);
    }

    /**
     * Method to return an ASpace digital object note type
     *
     * @param atValue
     * @return
     */
    public Object[] getASpaceDigitalObjectNoteType(String atValue) {

        String enumName = "note_digital_object_type";
        String defaultValue = "note";

        atValue = atValue.toLowerCase();

        if(atValue.contains("biographical/historical")) {
            atValue = "bioghist";
        } else if(atValue.contains("conditions governing access")) {
            atValue = "accessrestrict";
        } else if(atValue.contains("conditions governing use")) {
            atValue = "userestrict";
        } else if(atValue.contains("custodial history")) {
            atValue = "custodhist";
        } else if(atValue.contains("existence and location of copies")) {
            atValue = "altformavail";
        } else if(atValue.contains("existence and location of originals")) {
            atValue = "originalsloc";
        } else if(atValue.contains("general physical description")) {
            atValue = "physdesc";
        } else if(atValue.contains("immediate source of acquisition")) {
            atValue = "acqinfo";
        } else if(atValue.contains("language of materials")) {
            atValue = "langmaterial";
        } else if(atValue.contains("legal status")) {
            atValue = "legalstatus";
        } else if(atValue.contains("preferred citation")) {
            atValue = "prefercite";
        } else if(atValue.contains("processing information")) {
            atValue = "processinfo";
        } else if(atValue.contains("related archival materials")) {
            atValue = "relatedmaterial";
        }
        return getASpaceEnumValue(enumName, atValue, false, defaultValue);
    }

    /**
     * Method to map the AT multipart note type to the ASpace equivalence
     *
     * @param atValue
     * @return
     */
    public Object[] getASpaceMultiPartNoteType(String atValue) {

        String enumName = "note_multipart_type";
        String defaultValue = "odd";

        atValue = atValue.toLowerCase();

        if(atValue.contains("biographical/historical")) {
            atValue = "bioghist";
        } else if(atValue.contains("conditions governing access")) {
            atValue = "accessrestrict";
        } else if(atValue.contains("conditions governing use")) {
            atValue = "userestrict";
        } else if(atValue.contains("custodial history")) {
            atValue = "custodhist";
        } else if(atValue.contains("existence and location of copies")) {
            atValue = "altformavail";
        } else if(atValue.contains("existence and location of originals")) {
            atValue = "originalsloc";
        } else if(atValue.contains("file plan")) {
            atValue = "fileplan";
        } else if(atValue.contains("immediate source of acquisition")) {
            atValue = "acqinfo";
        } else if(atValue.contains("legal status")) {
            atValue = "legalstatus";
        } else if(atValue.contains("other finding aids")) {
            atValue = "otherfindaid";
        } else if(atValue.contains("physical characteristics and technical requirements")) {
            atValue = "phystech";
        } else if(atValue.contains("preferred citation")) {
            atValue = "prefercite";
        } else if(atValue.contains("processing information")) {
            atValue = "processinfo";
        } else if(atValue.contains("related archival materials")) {
            atValue = "relatedmaterial";
        } else if(atValue.contains("scope and contents")) {
            atValue = "scopecontent";
        } else if(atValue.contains("separated materials")) {
            atValue = "separatedmaterial";
        }
        return getASpaceEnumValue(enumName, atValue, false, defaultValue);
    }

    /**
     * Method to map the AT single part note type to the ASpace equivalence
     *
     * @param atValue
     * @return
     */
    public Object[] getASpaceSinglePartNoteType(String atValue) {

        atValue = atValue.toLowerCase();

        String enumName = "note_singlepart_type";
        String defaultValue = "abstract";


        if(atValue.contains("general physical description")) {
           atValue = "physdesc";
        } else if(atValue.contains("language of materials")) {
            atValue = "langmaterial";
        } else if(atValue.contains("location note")) {
            atValue = "physloc";
        } else if(atValue.contains("material specific details")) {
            atValue = "materialspec";
        } else if(atValue.contains("physical facet")) {
            atValue = "physfacet";
        }
        return getASpaceEnumValue(enumName, atValue, false, defaultValue);
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
     * Method to convert AT index item types to ASpace note index types
     *
     * @param atValue
     * @return
     */
    public Object[] getASpaceIndexItemType(String atValue) {

        atValue = atValue.toLowerCase();
        String enumName = "note_index_item_type";

        if (atValue.contains("corporate name")) {
            atValue = "corporate_entity";
        } else if(atValue.contains("genre form")) {
            atValue = "subject";
        } else if(atValue.contains("personal name")) {
            atValue = "person";
        } else if(atValue.contains("family name")) {
            atValue = "family";
        }
        return getASpaceEnumValue(enumName, atValue, false, "title");

    }

    /**
     * Method to map the AT resource level to ASpace equivalent
     *
     * @param atValue
     * @return
     */
    public Object[] getASpaceResourceLevel(String atValue) {
        return getASpaceEnumValue("archival_record_level", atValue, false, "collection");
    }

    /**
     * Method to map the AT resource component level to ASpace equivalent
     *
     * @param atValue
     * @return
     */
    public Object[] getASpaceArchivalObjectLevel(String atValue) {
        return getASpaceResourceLevel(atValue);
    }

    /**
     * Method to return the map find description
     *
     * @param atValue
     * @return
     */
    public Object[] getASpaceFindingAidDescriptionRule(String atValue) {
        atValue = atValue.toLowerCase();
        String enumName = "resource_finding_aid_description_rules";

        if (atValue.contains("anglo-american")) {
            atValue = "aacr";
        } else if(atValue.contains("cataloging cultural")) {
            atValue = "cco";
        } else if(atValue.contains("describing archives")) {
            atValue = "dacs";
        } else if(atValue.contains("rules for archival description")) {
            atValue = "rad";
        }
        return getASpaceEnumValue(enumName, atValue);
    }

    /**
     * Method to return the finding aid status
     *
     * @param atValue
     * @return
     */
    public Object[] getASpaceFindingAidStatus(String atValue) {
        return getASpaceEnumValue("resource_finding_aid_status", atValue);
    }

    /**
     * Method to return the equivalent ASpace instance type
     *
     * @param atValue
     * @return
     */
    public Object[] getASpaceInstanceType(String atValue) {

        return getASpaceEnumValue("instance_instance_type", atValue);
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
    public Object[] getASpaceInstanceContainerType(String atValue) {
        if(atValue == null || atValue.trim().isEmpty()) {
            atValue = "item";
        }
        return getASpaceEnumValue("container_type", atValue);
    }

    /**
     * Method to get the ASpace type
     *
     * @param atValue
     * @return
     */
    public Object[] getASpaceAcquisitionType(String atValue) {
        return getASpaceEnumValue("accession_acquisition_type", atValue);
    }

    /**
     * Method to return the AccessionResourceType
     *
     * @param atValue
     * @return
     */
    public Object[] getASpaceAccessionResourceType(String atValue) {
        if(atValue == null || atValue.isEmpty()) atValue = "collection";
        return getASpaceEnumValue("accession_resource_type", atValue);
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

    public Object[] getASpaceEnumValue(String enumListName, String atValue) {
        return getASpaceEnumValue(enumListName, atValue, true, null);
    }

    public Object[] getASpaceEnumValue(String enumListName, String atValue, boolean returnATValue, String defaultValue) {
        if (dynamicEnums == null) {
            System.out.println("dynamicEnums is null");
            return new Object[]{null, false};
            }
        if (atValue == null) {
            System.out.println("AT value is null in list " + enumListName);
            return new Object[]{null, false};
        }
        atValue = atValue.trim().toLowerCase();
        if (atValue.contains(" ")) {
            StringBuilder newAtValue = new StringBuilder();
            for (int i = 0; i < atValue.length(); i++) {
                char ch = atValue.charAt(i);
                if (ch == ' ') {
                    ch = '_';
                }
                newAtValue.append(ch);
            }
            atValue = newAtValue.toString();
        }
        System.out.println(atValue);
        try {
            JSONArray enumValues = dynamicEnums.get(enumListName).getJSONArray("values");
            for (int i = 0; i < enumValues.length(); i++) {
                String value = enumValues.getString(i);
                if (value.equalsIgnoreCase(atValue)) return new Object[]{value, true};
            }
            if (returnATValue) {
                System.out.println("AT value not found but returning it anyway " + atValue);
                return new Object[]{atValue, false};
            }
            return getASpaceEnumValue(enumListName, defaultValue, returnATValue, null);
        } catch (JSONException e) {
            System.out.println("Caught a JSON exception: returning null");
            e.printStackTrace();
            return new Object[]{null, false};
        }
    }

    /**
     * method to get the enum value a lookup list item will map to
     * @param enumListName
     * @param atValue
     * @param code
     * @return an array with 2 items: a string with the mapped value and a boolean to tell if it is in AS already
     */
    public Object[] mapsToASpaceEnumValue(String enumListName, String atValue, String code) {
        // if code is not empty then store it in hash so we can lookup later
        if(!code.isEmpty()) {
            lookupListValuesToCodes.put(atValue, code);
        }

        Object[] mappedValue;

        if (enumListName.equals("linked_agent_archival_record_roles")) {
            mappedValue = getASpaceLinkedAgentRole(atValue);
        } else if(enumListName.equals("name_source")) {
            mappedValue = getASpaceNameSource(atValue);
        } else if(enumListName.equals("name_rule")) {
            mappedValue = getASpaceNameRule(atValue);
//        } else if(enumListName.equals("name_description_type")) {
//            mappedValue = getASpaceNameDescriptionType(atValue);
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
            mappedValue = new Object[]{null, false};
        }

        return mappedValue;
    }


}
