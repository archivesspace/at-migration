package org.archiviststoolkit.plugin;

import org.archiviststoolkit.plugin.dbdialog.RemoteDBConnectDialogLight;
import org.archiviststoolkit.plugin.utils.aspace.ASpaceClient;
import org.archiviststoolkit.plugin.utils.aspace.ASpaceCopyUtil;
import org.hibernate.Session;

import java.io.*;
import java.util.HashMap;
import java.util.Properties;

/**
 * A class which allows the migration tool to be run from the command line
 *
 * Created by IntelliJ IDEA.
 * User: nathan
 * Date: 4/21/14
 * Time: 2:58 PM
 */
public class dbCopyCLI {
    // these fields are read in from the properties file
    private boolean useTracer = false;
    private String tracerDatabase = "";

    private int databaseURLIndex = -2;

    private int clientThreads = 1;

    private boolean continueFromResources = false;

    private String resetPassword = "archive";

    private boolean simulateRESTCalls = false;

    private boolean ignoreUnlinkedNames = false;

    private boolean ignoreUnlinkedSubjects = false;

    private String aspaceHost = "http://localhost:8089";

    private String aspaceAdmin = "admin";

    private String aspacePassword = "admin";

    // this is used to connect to the AT database
    private RemoteDBConnectDialogLight rcd;

    // stores any migration errors
    private String migrationErrors = "";

    // store any mismatch errors
    private String repositoryMismatchErrors = "";

    // the database copy util for AT to archives space
    private ASpaceCopyUtil ascopy;

    // the database copy until for check for repository mismatches
    private ASpaceCopyUtil ascopyREC;

    // used to connect connect to apace backend for testing
    private ASpaceClient aspaceClient;

    // used to find and attempt to resolve repository mismatch
    private boolean checkRepositoryMismatch = false;
    private HashMap<String, String> repositoryMismatchMap = null;

    /**
     * The main constructor which takes a properties object
     *
     * @param properties
     */
    public dbCopyCLI(Properties properties) throws Exception {
        useTracer = new Boolean(properties.getProperty("useTracer"));
        tracerDatabase = properties.getProperty("tracerDatabase");
        databaseURLIndex = new Integer(properties.getProperty("databaseURLIndex"));
        clientThreads = new Integer(properties.getProperty("clientThreads"));
        checkRepositoryMismatch = new Boolean(properties.getProperty("checkRepositoryMismatch"));
        continueFromResources = new Boolean(properties.getProperty("continueFromResources"));
        resetPassword = properties.getProperty("resetPassword");
        simulateRESTCalls = new Boolean(properties.getProperty("simulateRESTCalls"));
        ignoreUnlinkedNames = new Boolean(properties.getProperty("ignoreUnlinkedNames"));
        ignoreUnlinkedSubjects = new Boolean(properties.getProperty("ignoreUnlinkedSubjects"));
        aspaceHost = properties.getProperty("aspaceHost");
        aspaceAdmin = properties.getProperty("aspaceAdmin");
        aspacePassword = properties.getProperty("aspacePassword");
    }

    /**
     * Method to load an AT database connection
     *
     * @return
     */
    public Session getDatabaseSession() {
        rcd = new RemoteDBConnectDialogLight();

        // see whether to connect to the particular index
        if(useTracer) {
            String databaseType = "MySQL";
            String url = "jdbc:mysql://tracerdb.cyo37z0ucix8.us-east-1.rds.amazonaws.com/at" + tracerDatabase;
            String username = "aspace";
            String password = "clubfoots37@freakiest";

            // see whether we need to connect to the AT sandbox
            if(tracerDatabase.equals("SB")) {
                url  = "jdbc:mysql://dev.archiviststoolkit.org:3306/AT_SANDBOX2_0";
                username = "atuser";
                password = "cr4ckA1t";
            }

            rcd.connectToDatabase(databaseType, url, username, password);
        } else if (databaseURLIndex == -2) {
            rcd.connectToCurrentDatabase();
        } else {
            rcd.connectToDatabase(databaseURLIndex);
        }

        // return the session which maybe null
        return rcd.getSession();
    }

    /**
     * Method to start the database copy process
     */
    private void copyToASpace() {
        Session sourceSession = getDatabaseSession();
        if (sourceSession != null) {
            if(checkRepositoryMismatch) {
                startRepositoryCheckProcess();
            }

            // copy the AT records now
            startASpaceCopyProcess();
        } else {
            System.out.println("Source AT connection doesn't exist ...");
        }
    }

    /**
     * Method to run the repository check process
     */
    private void startRepositoryCheckProcess() {
        try {
            // print the connection message
            System.out.println("Starting repository check\n\n");

            ascopyREC = new ASpaceCopyUtil(rcd, aspaceHost, aspaceAdmin, aspacePassword);
            ascopyREC.setCheckRepositoryMismatch();

            // get the number of resources to copy here to allow it to be reset while the migration
            // has been started, but migration of resources has not yet started
            int resourcesToCopy = 1000000;
            int threads = 1;

            ascopyREC.copyResourceRecords(resourcesToCopy, threads);

            repositoryMismatchMap = ascopyREC.getRepositoryMismatchMap();

            ascopyREC.cleanUp();

            // set the number of errors and message now
            String errorCount = "" + ascopyREC.getSaveErrorCount();
            repositoryMismatchErrors = ascopyREC.getCurrentRecordCheckMessage() + "\n\nTotal errors: " + errorCount;

            // now save the repocheck log
            saveLogFile("repocheck_log.txt", repositoryMismatchErrors);
        } catch (Exception e) {
            System.out.println("Unrecoverable exception, recording checking stopped ...\n\n");

            // This is null for some reason so let's check it.
            if (ascopyREC != null) {
                System.out.println(ascopyREC.getCurrentRecordInfo() + "\n\n");
            }

            e.printStackTrace();
        }
    }

    /**
     * Method to start copy the AT records into ASpace
     */
    private void startASpaceCopyProcess() {
        try {
            // print the connection message
            System.out.println("Starting record copy\n\n");

            ascopy = new ASpaceCopyUtil(rcd, aspaceHost, aspaceAdmin, aspacePassword);
            ascopy.setRepositoryMismatchMap(repositoryMismatchMap);
            ascopy.setSimulateRESTCalls(simulateRESTCalls);
            ascopy.setExtentPortionInParts(false);
            ascopy.setIgnoreUnlinkedRecords(ignoreUnlinkedNames, ignoreUnlinkedSubjects);

            // set the reset password, and output console and progress bar
            ascopy.setResetPassword(resetPassword);
            ascopy.setCopying(true);

            // try getting the session and only continue if a valid session is return;
            if (!ascopy.getSession()) {
                System.out.println("No session, nothing to do ...\n");
                return;
            } else {
                System.out.println("Administrator authenticated ...\n");
            }

            // first load the notes etc types and resource from the destination database
            ascopy.loadRepositories();

            if (continueFromResources && ascopy.uriMapFileExist()) {
                ascopy.loadURIMaps();
            } else {
                ascopy.copyLookupList();
                ascopy.copyRepositoryRecords();
                ascopy.mapRepositoryGroups();
                ascopy.copyLocationRecords();
                ascopy.copyUserRecords();
                ascopy.copySubjectRecords();
                ascopy.copyNameRecords();
                ascopy.copyAccessionRecords();
                ascopy.copyDigitalObjectRecords();

                // save the record maps for possible future use
                ascopy.saveURIMaps();
            }

            // get the number of resources to copy here to allow it to be reset while the migration
            // has been started, but migration of resources has not yet started
            int resourcesToCopy = 1000000;

            ascopy.setUseBatchImport(true);
            ascopy.copyResourceRecords(resourcesToCopy, clientThreads);

            ascopy.cleanUp();

            // set the number of errors and message now
            String errorCount = "" + ascopy.getSaveErrorCount();
            migrationErrors = ascopy.getSaveErrorMessages() + "\n\nTotal errors: " + errorCount;

            // now save the migration log
            saveLogFile("migration_log.txt", migrationErrors);
        } catch (Exception e) {
            System.out.println("Unrecoverable exception, migration stopped ...\n\n");

            if (ascopy != null) {
                System.out.println(ascopy.getCurrentRecordInfo() + "\n\n");
            }
            e.printStackTrace();
        }
    }

    /**
     * Method to save the log file to the log directory of the AT
     *
     * @param logfileName
     * @param logText
     */
    private void saveLogFile(String logfileName, String logText) {
        File selectedFile = new File(System.getProperty("user.dir") + "/logs/" + logfileName);

        try {
            FileWriter fileWriter = new FileWriter(selectedFile);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

            bufferedWriter.write(logText);

            bufferedWriter.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     * Method to close the rcd connection
     */
    public void closeATConnection() {
        rcd.closeSession();
    }

    /**
     * Main method for testing in stand alone mode
     */
    public static void main(String[] args) {
        System.out.println("Starting " + dbCopyFrame.VERSION + "\n");

        // load the properties from the file which contains connection inf
        String propertyFilename = System.getProperty("user.dir") + "/dbcopy.properties";
        if(args.length == 1) {
            propertyFilename = args[0];
        }

        Properties prop = new Properties();
	    InputStream input = null;

	    try {
		    input = new FileInputStream(propertyFilename);

		    // try loading the properties file
		    prop.load(input);

            // now create the main dbcopy object
            dbCopyCLI dbcopyCLI = new dbCopyCLI(prop);

            // connect to the AT database and start the copy process
            dbcopyCLI.copyToASpace();

            // close the AT connection
            dbcopyCLI.closeATConnection();
	    } catch (IOException e) {
		    e.printStackTrace();
	    } catch (Exception e) {
            e.printStackTrace();
        } finally {
		    if (input != null) {
			    try {
				    input.close();
			    } catch (IOException e) {
				    e.printStackTrace();
			    }
		    }
	    }
    }
}
