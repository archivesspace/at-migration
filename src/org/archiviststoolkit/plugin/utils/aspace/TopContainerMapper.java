package org.archiviststoolkit.plugin.utils.aspace;

import org.archiviststoolkit.model.Accessions;
import org.archiviststoolkit.model.ArchDescriptionAnalogInstances;
import org.archiviststoolkit.mydomain.DomainObject;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

/**
 * class to add and manage top containers
 */
public class TopContainerMapper {

    public DomainObject getInstance() {
        return instance;
    }

    private class Info {
        public String uri;
        private Set<LocationJSONObject> locationURIs = new HashSet<LocationJSONObject>();

        public Info(String uri) {
            this.uri = uri;
        }
    }

    public static Map<TopContainerMapper, Info> getAlreadyAdded() {
        return alreadyAdded;
    }

    private static Map<TopContainerMapper, Info> alreadyAdded = new HashMap<TopContainerMapper, Info>();

    private static int unknownCount = 1;

    private ASpaceEnumUtil enumUtil = new ASpaceEnumUtil();
    private ASpaceCopyUtil aSpaceCopyUtil;

    public String getAtID() {
        return atID;
    }

    private DomainObject instance;
    private String atID;
    private String parentRepoURI; //***
    private String indicator; //***
    private String type; //dynamic enum "container_type"
    private String barcode;

    /**
     * initializes a top container for an analog instance
     * @param analogInstance
     * @param parentRepoURI
     * @param aSpaceCopyUtil
     * @throws JSONException
     */
    public TopContainerMapper(ArchDescriptionAnalogInstances analogInstance, String parentRepoURI,
                              ASpaceCopyUtil aSpaceCopyUtil) throws Exception {
        this.instance = analogInstance;
        indicator = analogInstance.getContainer1Indicator();
        type = (String) enumUtil.getASpaceInstanceContainerType(analogInstance.getContainer1Type())[0];
        barcode = analogInstance.getBarcode();
        this.parentRepoURI = parentRepoURI;
        this.aSpaceCopyUtil = aSpaceCopyUtil;
        atID = analogInstance.getArchDescriptionInstancesId().toString();
        initContainer();
    }

    /**
     * initializes a top container for an accession instance
     * @param accession
     * @param parentRepoURI
     * @param aSpaceCopyUtil
     * @throws JSONException
     */
    public TopContainerMapper(Accessions accession, String parentRepoURI, ASpaceCopyUtil aSpaceCopyUtil) throws Exception {
        this.instance = accession;
        type = "item";
        this.parentRepoURI = parentRepoURI;
        this.aSpaceCopyUtil = aSpaceCopyUtil;
        atID = accession.getAccessionId().toString();
        initContainer();
    }

    /**
     * calls the method to add the top container if it does not exist yet
     * @throws JSONException
     */
    private void initContainer() throws Exception {
        if (indicator == null || indicator.isEmpty()) {
            indicator = String.format("Unknown container %d for ", unknownCount);
            if (!aSpaceCopyUtil.getSimulateRESTCalls()) unknownCount++;
            if (instance instanceof Accessions) indicator += "Accession " + ((Accessions) instance).getAccessionNumber();
            else {
                ArchDescriptionAnalogInstances analogInstances = (ArchDescriptionAnalogInstances) instance;
                indicator += "instance of ";
                if (analogInstances.getResource() != null) indicator += analogInstances.getResource().getResourceIdentifier();
                else indicator += analogInstances.getResourceComponent().getTitle();
            }
        }
        if (! alreadyAdded.keySet().contains(this)) {
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
        String id = aSpaceCopyUtil.saveRecord(parentRepoURI + "top_containers", jsonObject.toString(),
                instance.getClass().getSimpleName() + "->" + atID);
        if (!(id.equalsIgnoreCase("no id assigned"))) {
            aSpaceCopyUtil.print("Copied Top Container -- " + this);
        } else {
            aSpaceCopyUtil.print("Fail -- Instance: " + printableInstance());
        }
        return parentRepoURI + "top_containers/" + id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof TopContainerMapper)) return false;
        TopContainerMapper other = (TopContainerMapper) o;
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

    /**
     * add a top container to the list of containers that have already been added so that it won't be duplicated
     */
    public void addToExisting(String uri) {
        if (uri.contains("10000001") || uri.contains("no id assigned")) return;
        alreadyAdded.put(this, new Info(uri));
    }

    /**
     * gets the reference to the matching top container
     * @return uri for top container
     */
    public String getRef() {
        if (!alreadyAdded.containsKey(this)) return null;
        return alreadyAdded.get(this).uri;
    }

    public void addLocationURI(String uri, String note) throws Exception {
        if (uri == null || uri.isEmpty()) {
            return;
        }
        LocationJSONObject json = new LocationJSONObject(uri, note);

        alreadyAdded.get(this).locationURIs.add(json);
    }

    public void addLocationURI(String uri) throws Exception {addLocationURI(uri, "");}

    private class LocationJSONObject extends JSONObject {

        String uri;

        public LocationJSONObject(String uri, String note) throws JSONException {
            this.uri = uri;
            put("ref", uri);
            put("status", "current");
            put("note", note);
            //TODO find out if this info is stored in AT
            JSONObject dateJSON = new JSONObject();
            dateJSON.put("type", "single");
            dateJSON.put("date_label", enumUtil.getASpaceDateEnum(null)[0]);
            dateJSON.put("begin", new Date());
            put("start_date", "before " + dateJSON);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || !(other instanceof LocationJSONObject)) return false;
            return this.uri.equals(((LocationJSONObject) other).uri);
        }

        @Override
        public int hashCode() {
            return uri.hashCode();
        }
    }

    public JSONArray getContainerLocations() throws Exception {
        Set<LocationJSONObject> containerLocs = alreadyAdded.get(this).locationURIs;
        if (containerLocs.size() == 0) {
            this.aSpaceCopyUtil.addErrorMessage("No locations exist for top container " + this + "\n");
            this.aSpaceCopyUtil.print("No locations exist for " + this + ". Skipping.\n");
            return null;
        } else {
            JSONArray locations = new JSONArray(containerLocs);
            return locations;
        }
    }

    @Override
    public String toString() {
        return type + " " + indicator + " -- " + printableInstance();
    }

    public String printableInstance() {
        return instance.getClass().getSimpleName() + " " + atID;
    }

}
