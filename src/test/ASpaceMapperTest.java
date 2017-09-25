package test;

import junit.framework.JUnit4TestAdapter;
import org.archiviststoolkit.model.Names;
import org.archiviststoolkit.model.Subjects;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class ASpaceMapperTest extends Testing {

    @Test
    public void convertSubject() throws Exception {
        Subjects tester = new Subjects();
        tester.setSubjectId(1L);
        tester.setSubjectTerm("A test subject");
        tester.setSubjectSource("getty thesaurus of geographic names");
        tester.setSubjectTermType(" Function");
        tester.setVersion(1L);
        TestUtils.addDynamicEnumVal("subject_term_type", "function");
        TestUtils.addDynamicEnumVal("subject_source", "tgn");
        String json = TestUtils.mapper.convertSubject(tester);
        TestUtils.print(json);
        Assert.assertTrue(json.contains("\"publish\":true"));
        Assert.assertTrue(json.contains("\"term_type\":\"function\""));
    }

    @Test
    public void convertName() throws Exception {
        Names tester = new Names();
        tester.setNameId(1L);
        tester.setNameType("Person");
        tester.setSortName("Morrissey, Sarah");
        tester.setContactPhone("555-5555");
        tester.setPersonalPrimaryName("Morrissey");
        tester.setPersonalRestOfName("Sarah Elizabeth");
        tester.setNameSource("nad");
        TestUtils.addDynamicEnumVal("name_source", "nad");
        TestUtils.addDynamicEnumVal("name_rule", "local");
        TestUtils.addDynamicEnumVal("name_person_name_order", "inverted");
        String json = TestUtils.mapper.convertName(tester);
        TestUtils.print(json);
        Assert.assertTrue(json.contains("\"source\":\"nad\""));
    }

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(ASpaceMapperTest.class);
    }


}