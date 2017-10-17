package org.archiviststoolkit.plugin.utils.aspace;

import org.archiviststoolkit.model.Accessions;
import org.archiviststoolkit.model.AccessionsLocations;
import org.archiviststoolkit.model.ArchDescriptionAnalogInstances;
import org.archiviststoolkit.mydomain.DomainObject;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

/**
 * class to add and manage top containers
 *
 * @author sarah morrissey
 * date: 9/2017
 */
public class TopContainerMapper {

    //keys are containers that have been added to ASpace and values are objects that store that container's information
    private static Map<MiniContainer, Info> alreadyAdded = new HashMap<MiniContainer, Info>();

    //used for making unique identifiers for containers without indicators
    private static int unknownCount = 1;

    private ASpaceEnumUtil enumUtil = new ASpaceEnumUtil();
    private static ASpaceCopyUtil aSpaceCopyUtil;

    //analog instance or accession location the container was created for
    private DomainObject instance;
    private String atID;
    private String parentRepoURI;
    private String indicator;
    private String type;
    private String barcode;
    private Accessions accession;

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
     * TODO add the foreign key
     * @return the uri of the top container
     * @throws JSONException
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
     * method that resaves a top container record. Mainly for adding additional locations.
     * @param uri
     * @throws Exception
     */
    private void resaveTopContainer(String uri, JSONObject location) throws Exception {
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
     * add a top container to the list of containers that have already been added so that it won't be duplicated
     */
    public void addToExisting(String uri) {
        if (uri.contains("10000001") || uri.contains("no id assigned")) return;
        alreadyAdded.put(new MiniContainer(this), new Info(uri));
    }

    /**
     * gets the reference to the matching top container
     * @return uri for top container
     */
    public String getRef() {
        if (!alreadyAdded.containsKey(new MiniContainer(this))) return null;
        return alreadyAdded.get(new MiniContainer(this)).uri;
    }

    public void addLocationURI(String uri, String note) throws Exception {
        if (uri == null || uri.isEmpty()) {
            return;
        }
        JSONObject json = getLocationJSON(uri, note);

        Info info = alreadyAdded.get(new MiniContainer(this));
        if (info != null) {
            if (info.locationURIs.add(uri)) resaveTopContainer(info.uri, json);
        }
    }

    public void addLocationURI(String uri) throws Exception {addLocationURI(uri, "");}

    private static JSONObject getLocationJSON(String uri, String note) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("ref", uri);
        json.put("status", "current");
        json.put("note", note);
        //TODO find out if this info is stored in AT
        json.put("start_date", ASpaceMapper.DEFAULT_DATE.toString());
        return json;
    }

    @Override
    public String toString() {
        return type + " " + indicator + " -- " + printableInstance();
    }

    private String printableInstance() {
        if (instance == null) return "ArchivesSpace Container";
        return instance.getClass().getSimpleName() + " " + atID;
    }

    /**
     * holds only the data needed to determine if containers are equivalent
     * used in the already added map in place of the top container object to minimize memory usage
     */
    private class MiniContainer {

        private String parentRepoURI;
        private String indicator;
        private String type;
        private String barcode;

        MiniContainer(TopContainerMapper container) {
            this.parentRepoURI = container.parentRepoURI;
            this.indicator = container.indicator;
            this.type = container.type;
            this.barcode = container.barcode;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || !(o instanceof MiniContainer)) return false;
            MiniContainer other = (MiniContainer) o;
            if (!(this.parentRepoURI.equals(other.parentRepoURI))) return false;
            if (!(this.barcode == null || this.barcode.isEmpty() || other.barcode == null || other.barcode.isEmpty())) {
                if (this.barcode.trim().equalsIgnoreCase(other.barcode.trim())) return true;
            }
            Boolean sameType = true;
            if (!(this.type == null || this.type.isEmpty() || other.type == null || other.type.isEmpty())) {
                sameType = this.type.equals(other.type);
            }
            return (this.indicator.equalsIgnoreCase(other.indicator) && sameType);
        }

        @Override
        public int hashCode() {
            return parentRepoURI.hashCode();
        }
    }

    /**
     * stores the information for a top container as it was added to ASpace
     */
    private class Info {
        public String uri;
        private Set<String> locationURIs = new HashSet<String>();

        public Info(String uri) {
            this.uri = uri;
        }
    }
}
