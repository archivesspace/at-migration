package test;

import junit.framework.JUnit4TestAdapter;
import org.archiviststoolkit.plugin.dbCopyCLI;
import org.junit.Assert;
import org.junit.Test;

public class dbCopyCLITest extends Testing {

    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(dbCopyCLITest.class);
    }

    @Test
    public void testDBNameFromURL() throws Exception {
        dbCopyCLI tester = new dbCopyCLI(TestUtils.getProperties());
        String result = tester.getDatabaseNameFromURL("http://test.com/test/a.name");
        Assert.assertEquals("a.name", result);
    }
}
