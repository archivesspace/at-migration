package test;

import junit.framework.JUnit4TestAdapter;
import org.archiviststoolkit.model.LookupList;
import org.archiviststoolkit.model.LookupListItems;
import org.archiviststoolkit.plugin.dbdialog.RemoteDBConnectDialogLight;
import org.archiviststoolkit.plugin.utils.aspace.ASpaceClient;
import org.archiviststoolkit.plugin.utils.aspace.ASpaceEnumUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;

public class ASpaceEnumUtilTest extends Testing {

    static JSONObject json =  new JSONObject();

    static {
        try {
            JSONArray arr = new JSONArray();
            arr.put("local");
            json.put("name", "subject_source");
            json.put("values", arr);
            TestUtils.dynamicEnums.put("subject_source", json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void emptyMapsToLocal() throws JSONException {
        ASpaceEnumUtil tester = new ASpaceEnumUtil();
        tester.setASpaceDynamicEnums(TestUtils.dynamicEnums);
        Object[] result = tester.mapsToASpaceEnumValue("subject_source", "", "");
        Assert.assertEquals("local", result[0]);
    }

    @Test
    public void mapAllLookupListItems() throws JSONException {
        ASpaceClient asc = new ASpaceClient("http://localhost:7089", "admin", "admin");
        ASpaceEnumUtil tester = new ASpaceEnumUtil();
        HashMap<String, JSONObject> dynamicEnums = asc.loadDynamicEnums();
        tester.setASpaceDynamicEnums(dynamicEnums);
        RemoteDBConnectDialogLight rcd = new RemoteDBConnectDialogLight();
        rcd.connectToDatabase("MySQL", "jdbc:mysql://localhost:3306/at_container_test", "sem", "sem");
        ArrayList<LookupList> lookupLists = rcd.getLookupLists();
        for (LookupList lookupList : lookupLists) {
            String listName = lookupList.getListName();
            ArrayList<String[]> additional = new ArrayList<String[]>();

            JSONObject dynamicEnum = tester.getDynamicEnum(listName);
            if (dynamicEnum == null) continue;
            String enumName = dynamicEnum.getString("name");
            JSONArray valuesJA = dynamicEnum.getJSONArray("values");
            if(listName.equalsIgnoreCase("Extent type")) {
                valuesJA.put("unknown");
            } else if(listName.equals("Name source")) {
                valuesJA.put("unknown");
            } else if(listName.equals("Container types")) {
                valuesJA.put("unknown_item");
            } else if(listName.equals("Resource type")) {
                valuesJA.put("collection");
            } else if(listName.equals("Date type")) {
                valuesJA.put("other");
            }
            dynamicEnum.put("values", valuesJA);
            for (LookupListItems item :lookupList.getListItems()) {
                additional.add(new String[]{item.getListItem(), item.getCode()});
            }
            additional.add(new String[]{"", null});
            additional.add(new String[]{"other value", null});

            System.out.println("\n\nAT LookupList," + listName + "\nASpace Enum," + enumName +
                    "\n\nAT Value,ASpace Code,ASpace Value,Notes");

            for (String[] item : additional) {
                if (item[1] == null) item[1] = "";
                Object[] value = tester.mapsToASpaceEnumValue(enumName, item[0], item[1]);
                if (item[0].equals("")) {
                    item[0] = "null/empty";
                    if (value[0] == null) continue;
                }
                System.out.println(item[0] + "," + value[0] + "," + value[1]);
            }
        }


    }

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(ASpaceEnumUtilTest.class);
    }
}
