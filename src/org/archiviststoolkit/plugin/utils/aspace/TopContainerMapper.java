package org.archiviststoolkit.plugin.utils.aspace;

import org.archiviststoolkit.model.Accessions;
import org.archiviststoolkit.model.AccessionsLocations;
import org.archiviststoolkit.model.ArchDescriptionAnalogInstances;
import org.archiviststoolkit.mydomain.DomainObject;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.*;

/**
 * class to add and manage top containers
 *
 * @author sarah morrissey
 * date: 12/2017
 * @version 2.2
 */
public class TopContainerMapper {

    // keys are containers that have been added to ASpace and values are objects that store that container's information
    private static HashMap<MiniContainer, Info> alreadyAdded = new HashMap<MiniContainer, Info>();

    // these are the containers with barcodes that we need to keep track of even if we are done copying the current resoruce
    private static HashMap<MiniContainer, Info> permanentAdded = new HashMap<MiniContainer, Info>();

    // this gets set each time we begin copying a new resource
    // its -1 if copying an accession
    private static long resourceID = -1;

    // used for making unique identifiers for containers without indicators
    private static int unknownCount = 1;

    private ASpaceEnumUtil enumUtil = new ASpaceEnumUtil();
    private static ASpaceCopyUtil aSpaceCopyUtil;

    // analog instance or accession location the container was created for
    private DomainObject instance;
    private Accessions accession;

    // the stuff that is needed to save the container
    private String atID;
    private String parentRepoURI;
    private String indicator;
    private String type;
    private String barcode;

    public DomainObject getInstance() {
        return instance;
    }

    /**
     * method to get the AT identifier
     * @return
     */
    public String getAtID() {
        return atID;
    }

    /**
     * initializes a top container for an analog instance
     * @param analogInstance
     * @param parentRepoURI
     * @throws JSONException
     */
    public TopContainerMapper(ArchDescriptionAnalogInstances analogInstance, String parentRepoURI) throws Exception {
        this.instance = analogInstance;
        indicator = analogInstance.getContainer1Indicator();
        type = (String) enumUtil.getASpaceInstanceContainerType(analogInstance.getContainer1Type())[0];
        barcode = analogInstance.getBarcode();
        this.parentRepoURI = parentRepoURI;
        initContainer();
    }

    /**
     * initializes a top container for an accession location instance
     * @param parentRepoURI
     * @throws JSONException
     */
    public TopContainerMapper(AccessionsLocations accessionLocation, Accessions accession, String parentRepoURI) throws Exception {
        this.accession = accessionLocation.getAccession();
        if (this.accession == null) {this.accession = accession;}
        instance = accessionLocation;

        //pull barcode from the accessionLocation the accession instance housed here was created for
        barcode = accessionLocation.getLocation().getBarcode();
        this.parentRepoURI = parentRepoURI;
        initContainer();
    }

    /**
     * constructor for a top container created from one previously in ASpace for comparison purposes
     * @param containerJS
     * @throws Exception
     */
    public TopContainerMapper(JSONObject containerJS) throws Exception {
        parentRepoURI = containerJS.getJSONObject("repository").get("ref") + "/";
        indicator = containerJS.get("indicator").toString();
        try {
            barcode = containerJS.get("barcode").toString();
        } catch (JSONException e) {
            barcode = null;
        }
        try {
            type = containerJS.get("type").toString();
        } catch (JSONException e) {
            type = null;
        }
        addToExisting(containerJS.get("uri").toString());
        JSONArray locations = containerJS.getJSONArray("container_locations");
        for (int i = 0; i < locations.length(); i++) {
            JSONObject location = locations.getJSONObject(i);
            addLocationURI(location.get("ref").toString());
        }
    }

    public static void setaSpaceCopyUtil(ASpaceCopyUtil copyUtil) {
        aSpaceCopyUtil = copyUtil;
    }

    /**
     * calls the method to add the top container if it does not exist yet
     * @throws JSONException
     */
    private void initContainer() throws Exception {
        atID = instance.getIdentifier().toString();
        if (indicator == null || indicator.isEmpty()) {
            indicator = String.format("Unknown container %d for ", unknownCount);
            if (!aSpaceCopyUtil.getSimulateRESTCalls()) unknownCount++;
            if (instance instanceof AccessionsLocations) indicator += "Accession " + accession.getAccessionNumber();
            else {
                ArchDescriptionAnalogInstances analogInstances = (ArchDescriptionAnalogInstances) instance;
                indicator += "instance of ";
                if (analogInstances.getResource() != null) indicator += analogInstances.getResource().getResourceIdentifier();
                else indicator += analogInstances.getResourceComponent().getTitle();
            }
        }
        if (! alreadyAdded.keySet().contains(new MiniContainer(this))) {
            String uri;
            uri = this.addTopContainer();
            this.addToExisting(uri);
        }
    }

    /**
     * adds a top container to the ASpace database
     * @return the uri of the top container
     * @throws Exception
     */
    private String addTopContainer() throws Exception {
        JSONObject jsonObject = new JSONObject();
        ASpaceMapper.addExternalId(instance, jsonObject, instance.getClass().getSimpleName());
        jsonObject.put("jsonmodel_type", "top_container");
        jsonObject.put("indicator", indicator);
        jsonObject.put("type", type);
        jsonObject.put("barcode", barcode);
        jsonObject.put("container_locations", new JSONArray());
        String id = aSpaceCopyUtil.saveRecord(parentRepoURI + "top_containers", jsonObject.toString(),
                instance.getClass().getSimpleName() + "->" + atID);
        if (!(id.equalsIgnoreCase("no id assigned"))) {
            aSpaceCopyUtil.print("Copied Top Container -- " + this);
        } else {
            aSpaceCopyUtil.print("Fail -- Instance: " + printableInstance());
        }
        return parentRepoURI + "top_containers/" + id;
    }

    /**
     * method that adds a location to a previously saved top container record
     * @param uri of the container
     * @param location json object for the container location to be added
     * @throws Exception
     */
    private void saveTopContainerLocation(String uri, JSONObject location) throws Exception {
        JSONObject json = aSpaceCopyUtil.getRecord(uri);
        JSONArray locationsJA = (JSONArray) json.get("container_locations");
        locationsJA.put(location);
        json.put("container_locations", locationsJA);
        String instanceClassName;
            try {
                instanceClassName = getInstance().getClass().getName();
            } catch (NullPointerException e) {
                instanceClassName = "ArchivesSpace Container";
            }
        aSpaceCopyUtil.saveRecord(uri, json.toString(),instanceClassName + "->" + getAtID());
    }

    /**
     * add a top container to the list of containers that have already been added so that it won't be duplicated     *
     * @param uri of the top container
     */
    public void addToExisting(String uri) {
        if (uri.contains("10000001") || uri.contains("no id assigned")) return;
        MiniContainer mini = new MiniContainer(this);
        Info info = new Info(uri);
        alreadyAdded.put(mini, info);
        if (this.barcode != null && !this.barcode.isEmpty()) permanentAdded.put(mini, info);
    }

    /**
     * gets the uri reference to the matching top container
     * @return uri for top container
     */
    public String getRef() {
        if (!alreadyAdded.containsKey(new MiniContainer(this))) return null;
        return alreadyAdded.get(new MiniContainer(this)).uri;
    }

    /**
     * adds a location for a container if it is not already associated with said container
     * @param uri of the location
     * @param note for accession containers the note from accession location record
     * @throws Exception
     */
    public void addLocationURI(String uri, String note) throws Exception {
        if (uri == null || uri.isEmpty()) {
            return;
        }
        JSONObject json = getLocationJSON(uri, note);

        Info info = alreadyAdded.get(new MiniContainer(this));
        if (info != null) {
            if (info.locationURIs.add(uri)) saveTopContainerLocation(info.uri, json);
        }
    }

    public void addLocationURI(String uri) throws Exception {addLocationURI(uri, "");}

    /**
     * method to get a json object for a containers location
     * @param uri the location's uri
     * @param note for accession containers the note for the accession location, otherwise null
     * @return a json object for the container location
     * @throws JSONException
     */
    private JSONObject getLocationJSON(String uri, String note) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("ref", uri);
        json.put("status", "current");
        json.put("note", note);

        //set start date for location to date record was created
        Date startDate = null;
        try {
            if (instance instanceof AccessionsLocations) {
                startDate = accession.getCreated();
            } else if (instance instanceof ArchDescriptionAnalogInstances) {
                ArchDescriptionAnalogInstances aIn = (ArchDescriptionAnalogInstances) instance;
                if (aIn.getResource() != null) startDate = aIn.getResource().getCreated();
                if (aIn.getResourceComponent() != null) startDate = aIn.getResourceComponent().getCreated();
            }
        } finally {
            if (startDate == null) startDate = ASpaceMapper.DEFAULT_DATE;
        }
        json.put("start_date", startDate.toString());
        return json;
    }

    @Override
    public String toString() {
        return type + " " + indicator + " -- " + printableInstance();
    }

    /**
     * gives information about what AT record the top container was created from
     * @return class and id of item the container was created from
     */
    private String printableInstance() {
        if (instance == null) return "ArchivesSpace Container";
        return instance.getClass().getSimpleName() + " " + atID;
    }

    /**
     * holds only the data needed to determine if containers are equivalent
     * used in the already added map in place of the top container object to minimize memory usage
     */
    private static class MiniContainer {

        private String parentRepoURI;
        private String indicator;
        private String type;
        private String barcode;
        private long resourceID;

        /**
         * constructs a mini container version of a top container object
         * @param container the top container we are creating the mini container for
         */
        MiniContainer(TopContainerMapper container) {
            this.parentRepoURI = container.parentRepoURI;
            this.indicator = container.indicator;
            this.type = container.type;
            this.barcode = container.barcode;
            this.resourceID = TopContainerMapper.resourceID;
        }

        /**
         * reconstructs a mini container from a saved string
         * @param args the string splint into its components
         */
        MiniContainer(String ... args) {
            this.parentRepoURI = args[0];
            this.resourceID = Long.parseLong(args[1]);
            this.indicator = args[2];
            if (args.length > 3) {
                if (args[3].contains("type: ")) {
                    this.type = args[3].substring(6);
                    if (args.length > 4) this.barcode = args[4];
                }
                else this.barcode = args[3];
            }
        }

        @Override
        public String toString() {
            return parentRepoURI + SEPARATOR + resourceID + SEPARATOR + indicator + SEPARATOR + "type: " + type + SEPARATOR + barcode;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || !(o instanceof MiniContainer)) return false;
            MiniContainer other = (MiniContainer) o;

            // records from different repositories can not be in the same top container
            if (!(this.parentRepoURI.equals(other.parentRepoURI))) return false;

            // if two containers have the same non-empty barcode, they are the same
            if (!(this.barcode == null || this.barcode.isEmpty() || other.barcode == null || other.barcode.isEmpty())) {
                if (this.barcode.trim().equalsIgnoreCase(other.barcode.trim())) return true;
            }

            // unless barcode shows otherwise, different resources are assumed to be in different containers
            if (this.resourceID != other.resourceID) return false;

            // finally if they have the same type and indicator, they are the same container
            Boolean sameType = true;
            if (!(this.type == null || other.type == null)) {
                sameType = this.type.equals(other.type);
            }
            return (this.indicator.equalsIgnoreCase(other.indicator) && sameType);
        }

        @Override
        public int hashCode() {
            // can't do too much here - repository is the only thing guaranteed to be the same for equivalent containers
            return parentRepoURI.hashCode();
        }
    }

    /**
     * stores the information for a top container as it was added to ASpace
     */
    private static class Info {
        public String uri;
        private Set<String> locationURIs = new HashSet<String>();

        public Info(String uri) {
            this.uri = uri;
        }

        /**
         * recreates the info from a string
         * @param args string split into its components
         */
        public Info(String ... args) {
            this.uri = args[0];
            for (int i = 1; i < args.length; i++) locationURIs.add(args[i]);
        }

        @Override
        public String toString() {
            StringBuilder value = new StringBuilder(uri);
            for (String uri: locationURIs) {
                value.append(SEPARATOR);
                value.append(uri);
            }
            return value.toString();
        }
    }

    /**
     * splits a saved string for a mini container or info object into its components
     * @param stringForm the saved string
     * @return the saved string split according to the separator
     */
    private static String[] fromString(String stringForm) {
        return stringForm.split(SEPARATOR);
    }

    // the separator that is used when making string forms of thing for saving
    private static final String SEPARATOR = "! . . . !";

    /**
     * turns the already added map into a string to string map that can be saved
     * @return a string to string hashmap that can be saved to a file
     */
    public static HashMap<String, String> getAlreadyAddedStringForm() {
        HashMap<String, String> alreadyAddedStringForm = new HashMap<String, String>();
        for (MiniContainer container : alreadyAdded.keySet()) {
            alreadyAddedStringForm.put(container.toString(), alreadyAdded.get(container).toString());
        }
        return alreadyAddedStringForm;
    }

    /**
     * take a saved string to string map and processes the data to make the already added map
     * @param topContainerURIMap the string to string version of already added
     */
    public static void setAlreadyAdded(HashMap<String, String> topContainerURIMap) {
        for (String key: topContainerURIMap.keySet()) {
            MiniContainer miniContainer = new MiniContainer(fromString(key));
            Info info = new Info(fromString(topContainerURIMap.get(key)));
            alreadyAdded.put(miniContainer, info);
            if (miniContainer.barcode != null && !miniContainer.barcode.isEmpty()) permanentAdded.put(miniContainer, info);
        }
    }

    /**
     * clears everything from the list of containers that have been added
     */
    public static void clearAlreadyAdded() {
        permanentAdded = new HashMap<MiniContainer, Info>();
        resetAlreadyAdded(-1);
    }

    /**
     * clears all the containers that do not have barcodes from already added and sets the current resource ID
     * @param resourceID the resource that is being copied now
     */
    public static void resetAlreadyAdded(long resourceID) {
        TopContainerMapper.resourceID = resourceID;
        alreadyAdded = new HashMap<MiniContainer, Info>();
        alreadyAdded.putAll(permanentAdded);
    }
}
