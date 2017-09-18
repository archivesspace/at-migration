package test;


import org.archiviststoolkit.plugin.dbdialog.RemoteDBConnectDialogLight;
import org.archiviststoolkit.plugin.utils.aspace.ASpaceCopyUtil;
import org.archiviststoolkit.plugin.utils.aspace.ASpaceMapper;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.Scanner;

/**
 * A simple class that is used to reset the AS database. Meant mainly for running tests
 * Note that for the AS UI to reset you will have to also delete the AS data directory's contents
 */
public class TestUtils {

    public static final String[] empty = new String[0];
    public static boolean trimData = true;
    public static int startNum = 0;
    public static int numToCopy = 50;
    private static String propertiesUrl = "C:/Users/morrissey/Desktop/at-mig-6/dbcopy.properties";
    private static String atURL;
    private static String at_username;
    private static String at_password;
    private static String asURL = "jdbc:mysql://localhost:3306/";
    private static String as_username = "root";
    private static String as_password = "cinnamon";
    private static String schemaSetup = "C:/Users/morrissey/Documents/dumps/as_21_empty.sql";
    private static String asDirectory = "C:/Users/morrissey/Desktop/archivesspace";
    private static String host;
    public static Properties getProperties() {
        return properties;
    }

    private static Properties properties = new Properties();

    public static void setProperties() throws IOException {
        InputStream file = new FileInputStream(propertiesUrl);
        properties.load(file);
        if (atURL == null) atURL = properties.getProperty("atUrl");
        if (at_username == null) at_username = properties.getProperty("atUsername");
        if (at_password == null) at_password = properties.getProperty("atPassword");
        if (host == null) host = properties.getProperty("aspaceHost");
    }
    static {
        try {
            setProperties();
        } catch (IOException e) {
            System.out.println("Error loading properties:");
            e.printStackTrace();
        }
    }
    protected static RemoteDBConnectDialogLight rcd = new RemoteDBConnectDialogLight();
    static {
        rcd.connectToDatabase(properties.getProperty("databaseType"), properties.getProperty("atUrl"),
                properties.getProperty("atUsername"), properties.getProperty("atPassword"));
    }

    protected static ASpaceCopyUtil copyUtil = new ASpaceCopyUtil(rcd, properties.getProperty("aspaceHost"),
            properties.getProperty("aspaceAdmin"), properties.getProperty("aspacePassword"));
    protected static ASpaceMapper mapper = new ASpaceMapper(copyUtil);
    static {
        HashMap<String, Boolean> publish = new HashMap<String, Boolean>();
        publish.put("subjects", true);
        publish.put("names", true);
        mapper.setPublishHashMap(publish);
    }

    /**
     * reverts back to the original AS database state so data can be remigrated
     * @throws SQLException
     * @throws FileNotFoundException
     */
    public static void resetDatabase() throws SQLException, FileNotFoundException {
        System.out.println("Resetting AS database");
        Connection conn = DriverManager.getConnection(asURL, as_username, as_password);
        Statement st = conn.createStatement();
        System.out.println("Dropping old schema");
        try {
            st.executeUpdate("DROP SCHEMA archivesspace;");
        } catch (SQLException e) {
            print(e.getMessage());
        }
        System.out.println("Creating new schema");
        st.executeUpdate("create SCHEMA archivesspace");
        st.executeUpdate("GRANT all on archivesspace.* to 'AS_user'@'localhost'");
        st.close();
        conn = DriverManager.getConnection(asURL + "archivesspace", as_username, as_password);
        st = conn.createStatement();
        File sql = new File(schemaSetup);
        String query = "";
        Scanner sc = new Scanner(sql);
        while (sc.hasNext()) {
            query += sc.nextLine() + "\n";
        }
        String[] commands = query.split("\n" +
                "\n" +
                "--\n" +
                "-- Dumping events for database 'archivesspace'\n" +
                "--\n" +
                "\n" +
                "--\n" +
                "-- Dumping routines for database 'archivesspace'\n" +
                "--");
        System.out.println("Adding tables to schema");
        for (String command: commands[0].split(";")) {
            try {
                if (command.trim() != "") st.executeUpdate(command + ";");
            } catch (SQLException e) {
                System.out.println("Problem with:\n" + command);
                e.printStackTrace();
            }
        }
        System.out.println("Adding functions to schema");
        for (String command: commands[1].split(";;\n" +
                "DELIMITER ;\n")) {
            try {
                String[] ar = command.split("DELIMITER ;;");
                if(ar.length == 2 && ar[1].trim() != "") st.executeUpdate(ar[1]);
            } catch (SQLException e) {
                e.printStackTrace();
                System.out.println("Problem with:\n" + command);
            }
        }
        st.close();
        System.out.println("Database reset");
    }

    /**
    * use to get records down to a manageable amount for testing
    * @param original the full list of records
    * @param <E> some type of record
    * @param numToCopy how many records you want to test with
    * @return a trimmed list of only a few records
    */
   public static <E> ArrayList<E> trimRecords(ArrayList<E> original, int numToCopy) {
       if (trimData) {
           ArrayList<E> newList = new ArrayList<E>();
           for (int i = startNum; i < startNum + numToCopy; i++) {
               int j = i;
               if (i >= original.size()) {
                   j = i % original.size();
               }
               newList.add(original.get(j));
           }
           return newList;
       } else {return original;}
   }

    public static <E> ArrayList<E> trimRecords(ArrayList<E> original) {
       return trimRecords(original, numToCopy);
    }

    public static Statement dbConnect() throws SQLException {
        Connection conn = DriverManager.getConnection(asURL, as_username, as_password);
        Statement st = conn.createStatement();
        return st;
    }

    public static void printStackTrace() {
       System.out.println("Stack Trace:\n");
       for (StackTraceElement e : Thread.currentThread().getStackTrace()) System.out.println(e);
    }

    public static void print(Object o) {System.out.println(o);}
}
