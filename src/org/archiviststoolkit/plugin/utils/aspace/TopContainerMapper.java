package org.archiviststoolkit.plugin.utils.aspace;

import org.archiviststoolkit.model.Accessions;
import org.archiviststoolkit.model.ArchDescriptionAnalogInstances;
import org.json.JSONException;
import org.json.JSONObject;
import test.TestUtils;

import java.util.*;

public class TopContainerMapper {

    private static Map<TopContainerMapper, String> alreadyAdded = new HashMap<TopContainerMapper, String>();

    private ASpaceEnumUtil enumUtil = new ASpaceEnumUtil();
    private ASpaceCopyUtil aSpaceCopyUtil;
    private String atID;
    private String parentRepoURI; //***
    private String indicator; //***
    private String type; //dynamic enum "container_type"
    private String barcode;

    public TopContainerMapper(ArchDescriptionAnalogInstances analogInstance, String parentRepoURI,
                              ASpaceCopyUtil aSpaceCopyUtil) throws JSONException {
        indicator = analogInstance.getContainer1Indicator();
        type = enumUtil.getASpaceInstanceContainerType(analogInstance.getContainer1Type());
        barcode = analogInstance.getBarcode();
        this.parentRepoURI = parentRepoURI;
        this.aSpaceCopyUtil = aSpaceCopyUtil;
        atID = analogInstance.getArchDescriptionInstancesId().toString();
        initContainer();
    }

    public TopContainerMapper(Accessions accession, String parentRepoURI, ASpaceCopyUtil aSpaceCopyUtil) throws JSONException {
        indicator = accession.getAccessionNumber();
        type = "item";
        this.parentRepoURI = parentRepoURI;
        TestUtils.print(parentRepoURI);
        this.aSpaceCopyUtil = aSpaceCopyUtil;
        atID = accession.getAccessionId().toString();
        initContainer();
    }

    private void initContainer() throws JSONException {
        if (! alreadyAdded.keySet().contains(this)) {
            String uri;
            uri = this.addTopContainer();
            this.addToExisting(uri);
        }
    }

    /**
     * adds a top container to the ASpace database
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
            aSpaceCopyUtil.print("Copied Top Container: " + type + " " + indicator);
        } else {
            aSpaceCopyUtil.print("Could not copy Top Container: " + type + " " + indicator);
        }
        return parentRepoURI + "top_containers/" + id;
    }

    public void setIndicator(String indicator) {
        this.indicator = indicator;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
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
        alreadyAdded.put(this, uri);
    }

    public String getRef() {
        return alreadyAdded.get(this);
    }

}
