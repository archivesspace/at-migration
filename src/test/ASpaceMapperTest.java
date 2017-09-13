package test;

import junit.framework.JUnit4TestAdapter;
import org.archiviststoolkit.model.Names;
import org.archiviststoolkit.model.Subjects;
import org.junit.Assert;
import org.junit.Test;

public class ASpaceMapperTest {

    @Test
    public void convertSubject() throws Exception {
        Subjects tester = new Subjects();
        tester.setSubjectId(1L);
        tester.setSubjectTerm("A test subject");
        tester.setSubjectSource("getty thesaurus of geographic names");
        tester.setSubjectTermType("function");
        String json = TestUtils.mapper.convertSubject(tester);
        TestUtils.print(json);
        Assert.assertTrue(json.contains("\"publish\":true"));
        Assert.assertTrue(json.contains("\"source\":\"tgn\""));
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
        String json = TestUtils.mapper.convertName(tester);
        Assert.assertTrue(json.contains("\"source\":\"nad\""));
    }

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(ASpaceMapperTest.class);
    }


}