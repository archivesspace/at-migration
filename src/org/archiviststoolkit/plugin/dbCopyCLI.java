package org.archiviststoolkit.plugin;

import org.archiviststoolkit.plugin.dbdialog.RemoteDBConnectDialogLight;
import org.archiviststoolkit.plugin.utils.aspace.ASpaceClient;
import org.archiviststoolkit.plugin.utils.aspace.ASpaceCopyUtil;
import org.archiviststoolkit.plugin.utils.aspace.IntentionalExitException;
import org.hibernate.Session;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

/**
 * A class which allows the migration tool to be run from the command line
 *
 * Created by IntelliJ IDEA.
 * @author nathan
 * Updated by Sarah Morrissey 12/17
 * @version 2.2
 */
public class dbCopyCLI {
    // these fields are read in from the properties file
    private boolean useTracer = false;
    private String tracerDatabase = "";

    private int clientThreads = 1;

    private boolean continueFromURIMaps = false;

    private String resetPassword = "archive";

    private boolean simulateRESTCalls = false;

    private boolean ignoreUnlinkedNames = false;

    private boolean ignoreUnlinkedSubjects = false;

    private boolean copyOnlyResources = false;

    private boolean checkISODates = false;

    private String resourcesToCopy = null;

    private String databaseType = "";
    private String atUrl = "";
    private String atUsername = "";
    private String atPassword = "";

    private String aspaceHost = "http://localhost:8089";
    private String aspaceAdmin = "admin";
    private String aspacePassword = "admin";

    // specify which records to publish
    private boolean publishNames = false;
    private boolean publishSubjects = false;
    private boolean publishAccessions = false;
    private boolean publishDigitalObjects = false;
    private boolean publishResources = true;
    private boolean publishRepos = true;

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
        clientThreads = new Integer(properties.getProperty("clientThreads"));
        checkRepositoryMismatch = new Boolean(properties.getProperty("checkRepositoryMismatch"));
        continueFromURIMaps = new Boolean(properties.getProperty("continueFromURIMaps"));
        resetPassword = properties.getProperty("resetPassword");
        simulateRESTCalls = new Boolean(properties.getProperty("simulateRESTCalls"));
        ignoreUnlinkedNames = new Boolean(properties.getProperty("ignoreUnlinkedNames"));
        ignoreUnlinkedSubjects = new Boolean(properties.getProperty("ignoreUnlinkedSubjects"));
        publishNames = new Boolean(properties.getProperty("publishNames"));
        publishSubjects = new Boolean(properties.getProperty("publishSubjects"));
        publishAccessions = new Boolean(properties.getProperty("publishAccessions"));
        publishDigitalObjects = new Boolean(properties.getProperty("publishDigitalObjects"));
        publishResources = new Boolean(properties.getProperty("publishResources"));
        publishRepos = new Boolean(properties.getProperty("publishRepos"));
        copyOnlyResources = new Boolean(properties.getProperty("copyOnlyResources"));
        checkISODates = new Boolean(properties.getProperty("checkISODates"));
        resourcesToCopy = properties.getProperty("resourcesToCopy");
        databaseType = properties.getProperty("databaseType");
        atUrl = properties.getProperty("atUrl");
        atUsername = properties.getProperty("atUsername");
        atPassword = properties.getProperty("atPassword");
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
            databaseType = "MySQL";
            atUrl = "jdbc:mysql://test.archivesspace.org/at" + tracerDatabase;
            atUsername = "aspace";
            atPassword = "clubfoots37@freakiest";

            // see whether we need to connect to the AT sandbox
            if(tracerDatabase.equals("SB")) {
                atUrl  = "jdbc:mysql://dev.archiviststoolkit.org:3306/AT_SANDBOX2_0";
                atUsername = "atuser";
                atPassword = "cr4ckA1t";
            }
        }

        // try connecting to the T database
        rcd.connectToDatabase(databaseType, atUrl, atUsername, atPassword);

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

            // create the hash map use to see if a certain record should be exported automatically
            HashMap<String, Boolean> publishMap = new HashMap<String, Boolean>();
            publishMap.put("names", publishNames);
            publishMap.put("subjects", publishSubjects);
            publishMap.put("accessions", publishAccessions);
            publishMap.put("digitalObjects", publishDigitalObjects);
            publishMap.put("resources", publishResources);
            publishMap.put("repositories", publishRepos);

            ascopy = new ASpaceCopyUtil(rcd, aspaceHost, aspaceAdmin, aspacePassword);
            ascopy.setPublishHashMap(publishMap);
            ascopy.setRepositoryMismatchMap(repositoryMismatchMap);
            ascopy.setSimulateRESTCalls(simulateRESTCalls);
            ascopy.setCheckISODates(checkISODates);
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

            if (continueFromURIMaps && ascopy.uriMapFileExist()) {
                ascopy.loadURIMaps();
            } else {
                ascopy.loadRepositories();
            }

            ascopy.copyLookupList();
            ascopy.copyRepositoryRecords();
            ascopy.mapRepositoryGroups();
            ascopy.copyLocationRecords();
            //just so it gets removed from recordsToCopy
            ascopy.addAdminUser(null, null, null);
            ascopy.copyUserRecords();
            ascopy.copySubjectRecords();
            ascopy.copyNameRecords();
            ascopy.copyAccessionRecords();
            ascopy.copyDigitalObjectRecords();

            // set the number of resources to copy
            int numberOfResourcesToCopy = 1000000;

            // set the resources to copy. Useful for debugging only
            ascopy.setResourcesToCopyList(getResourcesToCopy());

            ascopy.setUseBatchImport(true);

            ascopy.copyResourceRecords(numberOfResourcesToCopy, clientThreads);

            ascopy.addAssessments();

            // DEBUG code which checks to see that all ISO dates are valid
            if (checkISODates) {
                ascopy.checkISODates();
            }

            ascopy.cleanUp();

            // set the number of errors and message now
            String errorCount = "" + ascopy.getSaveErrorCount();
            migrationErrors = ascopy.getSaveErrorMessages() + "\n\nTotal errors: " + errorCount;

            // now save the migration log
            saveLogFile("migration_log-" + getDatabaseNameFromURL(atUrl) + ".txt", migrationErrors);
        } catch (IntentionalExitException e) {
            System.out.println(e.getMessage());
            if (ascopy != null) ascopy.saveURIMaps();
            else System.out.println("Could not save URI maps ...\nMigration will need to be restarted ...");
        } catch (Exception e) {
            System.out.println("Unrecoverable exception, migration stopped ...\n\n");

            if(ascopy != null) {
                ascopy.saveURIMaps();
                System.out.println(ascopy.getCurrentRecordInfo() + "\n\n");
            } else {
                System.out.println("Could not save URI maps ...\nMigration will need to be restarted ...");
            }
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
     * Return only the name of the database so it can be appended to log file
     *
     * @return
     */
    public String getDatabaseNameFromURL(String databaseURL) {
        int begin = databaseURL.lastIndexOf("/") + 1;
        return databaseURL.substring(begin);
    }

    /**
     * Method to close the rcd connection
     */
    public void closeATConnection() {
        rcd.closeSession();
    }

    private ArrayList<String> getResourcesToCopy() {
        ArrayList<String> resourcesIDsList = new ArrayList<String>();

        if(resourcesToCopy != null) {
            String[] sa = resourcesToCopy.split("\\s*,\\s*");
            for (String id : sa) {
                resourcesIDsList.add(id);
            }
        }

        return resourcesIDsList;
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

        System.exit(0);
    }
}
