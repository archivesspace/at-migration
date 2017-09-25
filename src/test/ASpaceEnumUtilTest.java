package test;

import junit.framework.JUnit4TestAdapter;
import org.archiviststoolkit.plugin.utils.aspace.ASpaceEnumUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

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

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(ASpaceEnumUtilTest.class);
    }
}
