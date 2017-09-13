package test;

/**
 * a class for setup to make testing easier
 */
public class Testing {


    /**
     * Run tests from here. It is recommended to start by clearing the AS database
     * @param args
     */
    public static void main(String[] args) throws Exception {
//        TestUtils.resetDatabase();
        org.junit.runner.JUnitCore.main("test.ASpaceMapperTest", "test.dbCopyCLITests");
    }
}
