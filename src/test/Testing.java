package test;

import org.archiviststoolkit.plugin.dbCopyFrame;
import org.junit.Test;

import java.util.ArrayList;

/**
 * a class for setup to make testing easier
 */
public class Testing {

    private static ArrayList<String> testsToRun = new ArrayList<String>();

    /**
     * Adds a test to the list of tests to be run by main method
     * call this function from each test you want to run
     * @param test the name of the class containing the test. This class must be in the test package
     */
    public static void addTest(Class test) {
        testsToRun.add(test.getName());
    }

    public Testing() {
        addTest(this.getClass());
    }


    /**
     * Run tests from here. It is recommended to start by clearing the AS database
     * @param args
     */
    public static void main(String[] args) throws Exception {
        TestUtils.resetDatabase();
        //Call constructors for any tests you want to run here
        new dbCopyCLITest();
        new ASpaceMapperTest();
        org.junit.runner.JUnitCore.main(testsToRun.toArray(new String[0]));
    }
}
