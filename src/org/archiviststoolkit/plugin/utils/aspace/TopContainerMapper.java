package org.archiviststoolkit.plugin.utils.aspace;

import org.archiviststoolkit.model.Accessions;
import org.archiviststoolkit.model.ArchDescriptionAnalogInstances;
import org.archiviststoolkit.mydomain.DomainAccessObject;
import org.archiviststoolkit.mydomain.DomainObject;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.Warning;
import test.TestUtils;

import java.util.*;

/**
 * class to add and manage top containers
 */
public class TopContainerMapper {

    private class Info {
        public String uri;
        private Set<JSONObject> locationURIs = new HashSet<JSONObject>();

        public Info(String uri) {
            this.uri = uri;
        }
    }

    public static Map<TopContainerMapper, Info> getAlreadyAdded() {
        return alreadyAdded;
    }

    private static Map<TopContainerMapper, Info> alreadyAdded = new HashMap<TopContainerMapper, Info>();

    private ASpaceEnumUtil enumUtil = new ASpaceEnumUtil();
    private ASpaceCopyUtil aSpaceCopyUtil;
    private DomainObject instance;

    public String getAtID() {
        return atID;
    }

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
                              ASpaceCopyUtil aSpaceCopyUtil) throws JSONException {
        this.instance = analogInstance;
        indicator = analogInstance.getContainer1Indicator();
        type = enumUtil.getASpaceInstanceContainerType(analogInstance.getContainer1Type());
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
    public TopContainerMapper(Accessions accession, String parentRepoURI, ASpaceCopyUtil aSpaceCopyUtil) throws JSONException {
        this.instance = accession;
        indicator = accession.getAccessionNumber();
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
    private void initContainer() throws JSONException {
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
    private String addTopContainer() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("jsonmodel_type", "top_container");
        jsonObject.put("indicator", indicator);
        jsonObject.put("type", type);
        jsonObject.put("barcode", barcode);
        String id = aSpaceCopyUtil.saveRecord(parentRepoURI + "top_containers", jsonObject.toString(),
                "TopContainer->" + atID);
        if (!(id.equalsIgnoreCase("no id assigned"))) {
            aSpaceCopyUtil.print("Copied Top Container: " + this);
        } else {
            aSpaceCopyUtil.print("Could not copy Top Container: " + this);
        }
        return parentRepoURI + "top_containers/" + id;
    }

    public void setType(String type) {
        this.type = type;
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
        alreadyAdded.put(this, new Info(uri));
    }

    /**
     * gets the reference to the matching top container
     * @return uri for top container
     */
    public String getRef() {
        return alreadyAdded.get(this).uri;
    }

    public void addLocationURI(String uri) throws Exception {
        if (uri == null || uri.isEmpty()) {
            return;
        }
        JSONObject json = new JSONObject();
        json.put("ref", uri);
        json.put("status", "current");
        //TODO find out if this info is stored in AT
        JSONObject dateJSON = new JSONObject();
        dateJSON.put("type", "single");
        dateJSON.put("date_label", "record_keeping");
        dateJSON.put("begin", new Date());
        json.put("start_date", "before " + dateJSON);
        alreadyAdded.get(this).locationURIs.add(json);
    }

    public JSONArray getContainerLocations() throws Exception {
        Set<JSONObject> containerLocs = alreadyAdded.get(this).locationURIs;
        if (containerLocs.size() == 0) {
            this.aSpaceCopyUtil.addErrorMessage("No locations exist for top container " + this + "\n");
            this.aSpaceCopyUtil.print("No locations exist for " + this + ". Skipping.");
            return null;
        } else {
            JSONArray locations = new JSONArray(containerLocs);
            return locations;
        }
    }

    @Override
    public String toString() {
        return type + " " + indicator;
    }

}
