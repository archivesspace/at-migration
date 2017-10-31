/*
 * Created by JFormDesigner on Tue Jul 31 10:12:49 EDT 2012
 */

package org.archiviststoolkit.plugin;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.*;
import org.archiviststoolkit.ApplicationFrame;
import org.archiviststoolkit.importer.ImportExportLogDialog;
import org.archiviststoolkit.plugin.beanshell.ScriptViewerDialog;
import org.archiviststoolkit.plugin.dbdialog.RemoteDBConnectDialogLight;
import org.archiviststoolkit.plugin.utils.CodeViewerDialog;
import org.archiviststoolkit.plugin.utils.ScriptsDAO;
import org.archiviststoolkit.plugin.utils.aspace.ASpaceClient;
import org.archiviststoolkit.plugin.utils.aspace.ASpaceCopyUtil;
import org.archiviststoolkit.plugin.utils.aspace.IntentionalExitException;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.hibernate.Session;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Simple class to test the database transfer code without starting of the AT client application
 *
 * @author Nathan Stevens
 */
public class dbCopyFrame extends JFrame {
    public static final String VERSION = "Archives Space Data Migrator v2.x (10/2017)";
    public static FileOutputStream fw;

    // The application when running within the AT
    private ApplicationFrame mainFrame = null;

    // used for viewing the scripts in pre update 15 AT
    private ScriptViewerDialog svd;

    // used for viewing the scripts in pre update 15 AT
    private CodeViewerDialog cvd;

    // Hash that stores remote connections dialogs
    private HashMap<String, RemoteDBConnectDialogLight> storedRCDS = new HashMap<String, RemoteDBConnectDialogLight>();

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

    private boolean copyStopped = false;

    // used to find and attempt to resolve repository mismatch
    private boolean checkRepositoryMismatch = false;
    private HashMap<String, String> repositoryMismatchMap = null;

    // used to specify that the GUI is in basic mode
    private boolean isBasicUI = false;

    // running in standalone mode
    public dbCopyFrame(boolean basic) {
        initComponents();

        setTitle(VERSION);

        if(basic) {
            hideAdvanceFeatures();
        }
    }

    // running inside the AT
    public dbCopyFrame(ApplicationFrame mainFrame, boolean basic) {
        this.mainFrame = mainFrame;
        initComponents();

        setTitle(VERSION);

        sourceTextField.setText("-2");
        useTracerCheckBox.setSelected(false);

        if(basic) {
            hideAdvanceFeatures();
        }
    }

    public static void saveConsoleText(JTextArea console) {
        if (fw != null && console != null) {
            try {
                fw.write((console.getText() + "\n\n").getBytes());
            } catch (IOException e) {
            }
        }
    }

    /**
     * Method to hide advance UI features to make it easier for users to run the tool
     */
    private void hideAdvanceFeatures() {
        //apiLabel.setVisible(false);
        sourceLabel.setVisible(false);
        sourceTextField.setVisible(false);
        //threadLabel.setVisible(false);
        threadsTextField.setEnabled(false);
        //repositoryCheckButton.setVisible(false);
        //copyRecordCheckBox.setVisible(false);
        //viewRepositoryCheckReportButton.setVisible(false);
        adminLabel.setVisible(false);
        adminTextField.setVisible(false);
        tracerPanel.setVisible(false);
        //useSaveURIMapsCheckBox.setVisible(false);
        simulateCheckBox.setVisible(false);
        useScriptCheckBox.setVisible(false);
        editScriptButton.setVisible(false);
        //ignoreUnlinkedRecordsLabel.setVisible(false);
        //ignoreUnlinkedNamesCheckBox.setVisible(false);
        //ignoreUnlinkedSubjectsCheckBox.setVisible(false);
        //publishPanel.setVisible(false);
        batchImportCheckBox.setVisible(false);
//        deleteResourcesCheckBox.setVisible(false);
        numResourceToCopyLabel.setVisible(false);
        numResourceToCopyTextField.setVisible(false);
//        deleteResourcesCheckBox.setVisible(false);
        //resourcesToCopyTextField.setVisible(false);
        recordURIComboBox.setVisible(false);
        paramsLabel.setVisible(false);
        paramsTextField.setVisible(false);
        viewRecordButton.setVisible(false);
        testRecordButton.setVisible(false);
        basicUIButton.setVisible(false);

        isBasicUI = true;
    }

    /**
     * Close this window, and only exit if we are running in stand alone mode
     */
    private void okButtonActionPerformed() {

        saveConsoleText(consoleTextArea);

        setVisible(false);

        if (mainFrame == null && !isBasicUI) {
            System.exit(0);
        }
    }

    /**
     * Method to copy data from AT to archive space. NO longer Used
     */
    private void CopyToASpaceButtonActionPerformed() {
        // first check that the user is running update 15, or greater
        if (mainFrame != null && !mainFrame.getAtVersionNumber().contains("15") &&
                !mainFrame.getAtVersionNumber().contains("16")) {
            saveConsoleText(consoleTextArea);
            consoleTextArea.setText("You need AT version 2.0 Update 15, or greater for data migration ...");
            return;
        }

        // reset the error count and error messages
        errorCountLabel.setText("N/A");
        migrationErrors = "";

        String sourceTitle = "Source AT Database";

        // get the index of the database url for doing quick connection without displaying
        // the dialog
        int sourceUrlIndex = -1;

        try {
            sourceUrlIndex = Integer.parseInt(sourceTextField.getText());
        } catch (NumberFormatException nfe) { }

        // load the source and destinations database connections
        Session sourceSession = displayRemoteConnectionDialog(sourceTitle, sourceUrlIndex, false);

        if (sourceSession != null) {
            // call the thread that does migration now
            RemoteDBConnectDialogLight sourceRCD = storedRCDS.get(sourceTitle);
            if(checkRepositoryMismatch) {
                startRepositoryCheckProcess(sourceRCD, sourceTitle, sourceUrlIndex);
            } else {
                startASpaceCopyProcess(sourceRCD, sourceTitle, sourceUrlIndex);
            }
        } else {
            System.out.println("source connection doesn't exist ...");
        }
    }

    // Method to open the dialog that gets a session
    public Session displayRemoteConnectionDialog(String title, int urlIndex, boolean reuse) {
        if (reuse && storedRCDS.get(title) != null) {
            RemoteDBConnectDialogLight rcd = storedRCDS.get(title);
            return rcd.refreshSession();
        }

        RemoteDBConnectDialogLight rcd = new RemoteDBConnectDialogLight(this);

        // see whether to connect to the particular index
        if(useTracerCheckBox.isSelected()) {
            String databaseName = tracerComboBox.getSelectedItem().toString();

            String databaseType = "MySQL";
            String url = "jdbc:mysql://test.archivesspace.org/at" + databaseName;
            String username = "aspace";
            String password = "clubfoots37@freakiest";

            // see whether we need to connect to the AT sandbox
            if(databaseName.equals("SB")) {
                url  = "jdbc:mysql://dev.archiviststoolkit.org:3306/AT_SANDBOX2_0";
                username = "atuser";
                password = "cr4ckA1t";
            }

            rcd.connectToDatabase(databaseType, url, username, password);
        } else if (urlIndex == -2) {
            rcd.connectToCurrentDatabase();
        } else if (urlIndex == -1) {
            rcd.setModal(true);
            rcd.setTitle(title);
            rcd.pack();
            rcd.setVisible(true);
        } else {
            rcd.connectToDatabase(urlIndex);
        }

        // store this connection for future reference
        storedRCDS.put(title, rcd);

        // return the session which maybe null
        return rcd.getSession();
    }

    /**
     * Method to start the a thread that actually copied ASpace records
     *
     * @param sourceRCD
     * @param sourceTitle
     * @param sourceUrlIndex
     */
    private void startASpaceCopyProcess(final RemoteDBConnectDialogLight sourceRCD, final String sourceTitle,
                                        final int sourceUrlIndex) {
        Thread performer = new Thread(new Runnable() {
            public void run() {
                // first disable/enable the relevant buttons
                copyToASpaceButton.setEnabled(false);
                //errorLogButton.setEnabled(false);
                stopButton.setEnabled(true);

                // clear text area and show progress bar
                saveConsoleText(consoleTextArea);
                consoleTextArea.setText("");
                copyProgressBar.setStringPainted(true);
                copyProgressBar.setString("Copying Records ...");
                copyProgressBar.setIndeterminate(true);

                try {
                    // print the connection message
                    consoleTextArea.append(sourceRCD.getConnectionMessage());

                    String host = hostTextField.getText().trim();
                    String admin = adminTextField.getText();
                    String adminPassword = adminPasswordTextField.getText();

                    boolean simulateRESTCalls = simulateCheckBox.isSelected();
                    boolean extentPortionInParts = byuExtentRadioButton.isSelected();
                    boolean ignoreUnlinkedNames = ignoreUnlinkedNamesCheckBox.isSelected();
                    boolean ignoreUnlinkedSubjects = ignoreUnlinkedSubjectsCheckBox.isSelected();

                    // create the hash map use to see if a certain record should be exported automatically
                    HashMap<String, Boolean> publishMap = new HashMap<String, Boolean>();
                    publishMap.put("names", publishNamesCheckBox.isSelected());
                    publishMap.put("subjects", publishSubjectsCheckBox.isSelected());
                    publishMap.put("accessions", publishAccessionsCheckBox.isSelected());
                    publishMap.put("digitalObjects", publishDigitalObjectsCheckBox.isSelected());
                    publishMap.put("resources", publishResourcesCheckBox.isSelected());
                    publishMap.put("repositories", publishReposCheckBox.isSelected());

                    ascopy = new ASpaceCopyUtil(sourceRCD, host, admin, adminPassword);
                    ascopy.setPublishHashMap(publishMap);
                    ascopy.setRepositoryMismatchMap(repositoryMismatchMap);
                    ascopy.setSimulateRESTCalls(simulateRESTCalls);
                    ascopy.setExtentPortionInParts(extentPortionInParts);
                    ascopy.setIgnoreUnlinkedRecords(ignoreUnlinkedNames, ignoreUnlinkedSubjects);

                    // load the mapper script if specified
                    if (svd != null && useScriptCheckBox.isSelected()) {
                        String script = svd.getCurrentScript();
                        ascopy.setMapperScript(script);
                    } else if (cvd != null && useScriptCheckBox.isSelected()) {
                        String script = cvd.getCurrentScript();
                        ascopy.setMapperScript(script);
                    }

                    // set the reset password, and output console and progress bar
                    ascopy.setResetPassword(resetPasswordTextField.getText().trim());
                    ascopy.setOutputConsole(consoleTextArea);
                    ascopy.setProgressIndicators(copyProgressBar, errorCountLabel);
                    ascopy.setCopying(true);

                    // try getting the session and only continue if a valid session is return;
                    if (!ascopy.getSession()) {
                        consoleTextArea.append("No session, nothing to do ...\n");
                        reEnableCopyButtons();
                        return;
                    } else {
                        consoleTextArea.append("Administrator authenticated ...\n");
                    }

                    // check the current aspace version to make sure
                    String aspaceVersion = ascopy.getASpaceVersion();

                    //Check if working
                    System.out.println("Version: " + aspaceVersion);

                    if (aspaceVersion.isEmpty()) ascopy.setCopyAssessments();
                    if (!aspaceVersion.isEmpty() && !ASpaceCopyUtil.SUPPORTED_ASPACE_VERSION.contains(aspaceVersion)) {
                        String message = "Unsupported Archivesspace Version\nSupport Versions: " +
                                ASpaceCopyUtil.SUPPORTED_ASPACE_VERSION + " ...\n";

                        consoleTextArea.append(message);
                        reEnableCopyButtons();
                        return;
                    }

                    // process special options here. This could be done better but its the
                    // quickest way to do it for now
                    String ids = resourcesToCopyTextField.getText().trim();
                    ArrayList<String> resourcesIDsList = new ArrayList<String>();

                    if (!ids.isEmpty()) {
                        String[] sa = ids.split("\\s*,\\s*");
                        for (String id : sa) {
                            // check to see if we are dealing with a special command
                            // or an id to copy
                            if (id.startsWith("-")) {
                                processSpecialOption(ascopy, id);
                            } else {
                                resourcesIDsList.add(id);
                            }
                        }
                    }



                    if (useSaveURIMapsCheckBox.isSelected() && ascopy.uriMapFileExist()) {
                        ascopy.loadURIMaps();
                    } else {
                        // first load the notes etc types and resource from the destination database if not using saved ones
                        if (!copyStopped) ascopy.loadRepositories();
                    }

                    // set the progress bar from doing it's thing since the ascopy class is going to take over
                    copyProgressBar.setIndeterminate(false);

                    if (!copyStopped) ascopy.copyLookupList();
                    if (!copyStopped) ascopy.copyRepositoryRecords();
                    if (!copyStopped) ascopy.mapRepositoryGroups();
                    if (!copyStopped) ascopy.copyLocationRecords();
                    if (!copyStopped) ascopy.addAdminUser(admin, "Administrator User", adminPassword);
                    if (!copyStopped) ascopy.copyUserRecords();
                    if (!copyStopped) ascopy.copySubjectRecords();
                    if (!copyStopped) ascopy.copyNameRecords();
                    if (!copyStopped) ascopy.copyAccessionRecords();
                    if (!copyStopped) ascopy.copyDigitalObjectRecords();

                    // get the number of resources to copy here to allow it to be reset while the migration
                    // has been started, but migration of resources has not yet started
                    int resourcesToCopy = 1000000;
                    int threads = 1;

                    try {
                        boolean useBatchImport = batchImportCheckBox.isSelected();
//                        boolean deleteSavedResources = deleteResourcesCheckBox.isSelected();
                        ascopy.setUseBatchImport(useBatchImport);
//                        ascopy.setDeleteSavedResources(deleteSavedResources);

                        // get the number of threads to run the copy process in
                        threads = Integer.parseInt(threadsTextField.getText());

                        // get the number of resource to copy
                        if (resourcesIDsList.isEmpty()) {
                            resourcesToCopy = Integer.parseInt(numResourceToCopyTextField.getText());
                        } else {
                            resourcesToCopy = resourcesIDsList.size();
                        }
                    } catch (NumberFormatException nfe) {}

                    // check to make sure we didn't stop the copy process or resource to copy is
                    // not set to zero. Setting resources to copy to zero is a convenient way
                    // to generate a URI map which contains no resource records for testing purposes
                    if (!copyStopped && resourcesToCopy != 0) {
                        ascopy.setResourcesToCopyList(resourcesIDsList);
                        ascopy.copyResourceRecords(resourcesToCopy, threads);
                    }

                    if (!copyStopped) ascopy.addAssessments();

                    ascopy.cleanUp();

                    // set the number of errors and message now
                    String errorCount = "" + ascopy.getASpaceErrorCount();
                    errorCountLabel.setText(errorCount);
                    migrationErrors = ascopy.getSaveErrorMessages() + "\n\nTotal errors/warnings: " + errorCount;
                } catch (IntentionalExitException e) {
                    saveConsoleText(consoleTextArea);
                    consoleTextArea.setText(e.getMessage());
                    consoleTextArea.append("\nWill attempt to save URI maps ...");
                    if (ascopy != null) ascopy.saveURIMaps();
                    else consoleTextArea.append("\nCould not save URI maps ...\nMigration will need to be restarted ...");
                } catch (Exception e) {
                    saveConsoleText(consoleTextArea);
                    consoleTextArea.setText("Unrecoverable exception, migration stopped ...\n\n");

                    if(ascopy != null) {
                        ascopy.saveURIMaps();
                        consoleTextArea.append(ascopy.getCurrentRecordInfo() + "\n\n");
                    } else {
                        consoleTextArea.append("Could not save URI maps ...\nMigration will need to be restarted ...");
                    }

                    consoleTextArea.append(getStackTrace(e));
                    //e.printStackTrace();
                } finally {
                    sourceRCD.closeSession();
                }

                reEnableCopyButtons();
            }
        });

        performer.start();
    }

    /**
     * Method to process special commands
     */
    private void processSpecialOption(ASpaceCopyUtil ascopy, String option) {
        if(option.contains("-refid_")) {
            ascopy.setRefIdOption(option);
        } else if(option.contains("-term_")) {
            ascopy.setTermTypeOption(option);
        }
    }

    /**
     * Method to start the a thread that will look for and attempt to fix
     * repository mismatches
     *
     * @param sourceRCD
     * @param sourceTitle
     * @param sourceUrlIndex
     */
    private void startRepositoryCheckProcess(final RemoteDBConnectDialogLight sourceRCD, final String sourceTitle,
                                        final int sourceUrlIndex) {
        Thread performer = new Thread(new Runnable() {
            public void run() {
                repositoryMismatchErrors = "";

                // first disable/enable the relevant buttons
                copyToASpaceButton.setEnabled(false);
                repositoryCheckButton.setEnabled(false);
                //errorLogButton.setEnabled(false);
                stopButton.setEnabled(true);

                // clear text area and show progress bar
                saveConsoleText(consoleTextArea);
                consoleTextArea.setText("");
                copyProgressBar.setStringPainted(true);
                copyProgressBar.setString("Checking Repository Mismatches ...");
                copyProgressBar.setIndeterminate(true);

                try {
                    // print the connection message
                    consoleTextArea.append(sourceRCD.getConnectionMessage());

                    String host = hostTextField.getText().trim();
                    String admin = adminTextField.getText();
                    String adminPassword = adminPasswordTextField.getText();

                    // create the hash map use to see if a certain record should be exported automatically
                    // this is needed here otherwise an null pointer error is thrown
                    HashMap <String, Boolean> publishMap = new HashMap<String, Boolean>();
                    publishMap.put("names", publishNamesCheckBox.isSelected());
                    publishMap.put("subjects", publishSubjectsCheckBox.isSelected());
                    publishMap.put("accessions", publishAccessionsCheckBox.isSelected());
                    publishMap.put("digitalObjects", publishDigitalObjectsCheckBox.isSelected());
                    publishMap.put("resources", publishResourcesCheckBox.isSelected());

                    ascopyREC = new ASpaceCopyUtil(sourceRCD, host, admin, adminPassword);
                    ascopyREC.setCheckRepositoryMismatch();
                    ascopyREC.setPublishHashMap(publishMap);

                    // set the reset password, and output console and progress bar
                    ascopyREC.setOutputConsole(consoleTextArea);
                    ascopyREC.setProgressIndicators(copyProgressBar, errorCountLabel);

                    // set the progress bar from doing it's thing since the ascopy class is going to take over
                    copyProgressBar.setIndeterminate(false);

                    // get the number of resources to copy here to allow it to be reset while the migration
                    // has been started, but migration of resources has not yet started
                    int resourcesToCopy = 1000000;
                    int threads = 1;

                    ArrayList<String> resourcesIDsList = new ArrayList<String>();

                    if(!copyStopped) {
                        ascopyREC.setResourcesToCopyList(resourcesIDsList);
                        ascopyREC.copyResourceRecords(resourcesToCopy, threads);
                    }

                    repositoryMismatchMap = ascopyREC.getRepositoryMismatchMap();

                    ascopyREC.cleanUp();

                    // set the number of errors and message now
                    String errorCount = "" + ascopyREC.getASpaceErrorCount();
                    errorCountLabel.setText(errorCount);
                    repositoryMismatchErrors = ascopyREC.getCurrentRecordCheckMessage() + "\n\nTotal errors: " + errorCount;
                } catch (Exception e) {
                    saveConsoleText(consoleTextArea);
                    consoleTextArea.setText("Unrecoverable exception, recording checking stopped ...\n\n");

                    // This is null for some reason so let commit it out.
                    if(ascopyREC != null) {
                        consoleTextArea.append(ascopyREC.getCurrentRecordInfo() + "\n\n");
                    }

                    consoleTextArea.append(getStackTrace(e));
                    e.printStackTrace();
                } finally {
                    sourceRCD.closeSession();
                }

                checkRepositoryMismatch = false;
                ascopyREC = null;

                if(copyRecordCheckBox.isSelected()) {
                    CopyToASpaceButtonActionPerformed();
                } else {
                    reEnableCopyButtons();
                }
            }
        });

        performer.start();
    }

    /**
     * Method to re-enable the copy buttons
     */
    private void reEnableCopyButtons() {
        // re-enable the buttons the relevant buttons
        copyToASpaceButton.setEnabled(true);
        repositoryCheckButton.setEnabled(true);
        //errorLogButton.setEnabled(true);
        copyProgressBar.setValue(0);

        if (copyStopped) {
            if (ascopy != null) ascopy.saveURIMaps();
            copyStopped = false;
            copyProgressBar.setString("Cancelled Copy Process ...");
        } else {
            copyProgressBar.setString("Done");
        }
    }

    /**
     * Method to display the error log dialog
     */
    private void errorLogButtonActionPerformed() {
        ImportExportLogDialog logDialog;

        if(ascopy != null && ascopy.isCopying()) {
            logDialog = new ImportExportLogDialog(null, ImportExportLogDialog.DIALOG_TYPE_IMPORT, ascopy.getCurrentProgressMessage());
            logDialog.setTitle("Current Data Transfer Errors");
        } else {
            logDialog = new ImportExportLogDialog(null, ImportExportLogDialog.DIALOG_TYPE_IMPORT, migrationErrors);
            logDialog.setTitle("Data Transfer Errors");
        }

        logDialog.showDialog();
    }

    /**
     * Method to display dialog to view the repository error check report
     */
    private void viewRepositoryCheckReportButtonActionPerformed() {
        ImportExportLogDialog logDialog = null;

        if(ascopyREC != null && checkRepositoryMismatch) {
            logDialog = new ImportExportLogDialog(null, ImportExportLogDialog.DIALOG_TYPE_IMPORT, ascopyREC.getCurrentRecordCheckMessage());
            logDialog.setTitle("Current Repository Mismatch Errors");
        } else {
            logDialog = new ImportExportLogDialog(null, ImportExportLogDialog.DIALOG_TYPE_IMPORT, repositoryMismatchErrors);
            logDialog.setTitle("Repository Mismatch Errors");
        }

        logDialog.showDialog();
    }

    /**
     * This will display the edit dialog which will allow loading of mapper scripts
     */
    private void editScriptButtonActionPerformed() {
        // DEBUG CODE load the test script script
        String script = "";

        try {
            script = ScriptsDAO.getTextForBuiltInScript("utils/aspace/", "mapper.bsh");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // if we running in standalone mode in pre update 15
        if (mainFrame == null || mainFrame.getAtVersionNumber().contains("15") || mainFrame.getAtVersionNumber().contains("16")) {
            if (cvd == null) {
                cvd = new CodeViewerDialog(this, SyntaxConstants.SYNTAX_STYLE_JAVA, script, true, false);
            }

            cvd.setTitle("Mapper Script Editor");
            cvd.pack();
            cvd.setVisible(true);
        } else {
            // check to see if we have a viewer dialog, if not initialize one
            if (svd == null) {
                svd = new ScriptViewerDialog(this, "Mapper Script Editor");
            }

            svd.setCurrentScript(script);
            svd.pack();
            svd.setVisible(true);
        }
    }

    /**
     * Method to stop the copy process. Only works when resource are being copied
     */
    private void stopButtonActionPerformed() {
        if(ascopy != null) {
            ascopy.stopCopy();
        }

        if(ascopyREC != null) {
            ascopyREC.stopCopy();
        }

        copyStopped = true;
        stopButton.setEnabled(false);
    }

    /**
     * A convenient method for view the ASpace json records. It meant to be used for development purposes only
     */
    private void viewRecordButtonActionPerformed() {
        String uri = recordURIComboBox.getSelectedItem().toString();
        String recordJSON = "";

        try {
            if(aspaceClient == null) {
                String host = hostTextField.getText().trim();
                String admin = adminTextField.getText();
                String adminPassword = adminPasswordTextField.getText();

                aspaceClient = new ASpaceClient(host, admin, adminPassword);
                aspaceClient.getSession();
            }

            recordJSON = aspaceClient.getRecordAsJSON(uri, paramsTextField.getText());

            if(recordJSON == null || recordJSON.isEmpty()) {
                recordJSON = aspaceClient.getErrorMessages();
            }
        } catch (Exception e) {
            recordJSON = e.toString();
        }

        // see whether to display window with syntax highlighting
        if (mainFrame == null || mainFrame.getAtVersionNumber().contains("15")) {
            CodeViewerDialog codeViewerDialog = new CodeViewerDialog(this, SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT, recordJSON, true, true);
            codeViewerDialog.setTitle("REST ENDPOINT URI: " + uri);
            codeViewerDialog.setASpaceClient(aspaceClient);
            codeViewerDialog.pack();
            codeViewerDialog.setVisible(true);
        } else {
            ImportExportLogDialog logDialog = new ImportExportLogDialog(null, ImportExportLogDialog.DIALOG_TYPE_IMPORT, recordJSON);
            logDialog.setTitle("REST ENDPOINT URI: " + uri);
            logDialog.showDialog();
        }
    }

    /**
     * This will allow multiple records to submit for testing.
     */
    private void testRecordButtonActionPerformed() {
        try {
            if(aspaceClient == null) {
                String host = hostTextField.getText().trim();
                String admin = adminTextField.getText();
                String adminPassword = adminPasswordTextField.getText();

                aspaceClient = new ASpaceClient(host, admin, adminPassword);
                aspaceClient.getSession();
            }

            // see whether to display code editor window
            if (mainFrame == null || mainFrame.getAtVersionNumber().contains("15") || mainFrame.getAtVersionNumber().contains("16")) {
                CodeViewerDialog codeViewerDialog = new CodeViewerDialog(this, SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT, "", true, true);
                codeViewerDialog.setTitle("Record Unit Test");
                codeViewerDialog.setASpaceClient(aspaceClient);
                codeViewerDialog.setUpMultipleRecordTest();
                codeViewerDialog.pack();
                codeViewerDialog.setVisible(true);
            } else {
                saveConsoleText(consoleTextArea);
                consoleTextArea.setText("You need AT version 2.0, and above for this to work");
            }
        } catch (Exception e) {
            saveConsoleText(consoleTextArea);
            consoleTextArea.setText(getStackTrace(e));
        }
    }

    /**
     * Method to display the basic UI.
     */
    private void basicUIButtonActionPerformed() {
        dbCopyFrame basicFrame = new dbCopyFrame(mainFrame, true);
        basicFrame.pack();
        basicFrame.setVisible(true);
    }

    /**
     * Method to check and fix repository mismatches between resource records and linked
     * accessions and digital objects
     */
    private void repositoryCheckButtonActionPerformed() {
        checkRepositoryMismatch = true;
        CopyToASpaceButtonActionPerformed();
    }

    /**
     * Method to get the string from a stack trace
     * @param throwable The exception
     * @return the string representation of the stack trace
     */
    private String getStackTrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner Evaluation license - Sarah Morrissey
        dialogPane = new JPanel();
        contentPanel = new JPanel();
        apiLabel = new JLabel();
        sourceLabel = new JLabel();
        sourceTextField = new JTextField();
        threadLabel = new JLabel();
        threadsTextField = new JTextField();
        copyToASpaceButton = new JButton();
        hostLabel = new JLabel();
        hostTextField = new JTextField();
        repositoryCheckButton = new JButton();
        copyRecordCheckBox = new JCheckBox();
        viewRepositoryCheckReportButton = new JButton();
        tracerPanel = new JPanel();
        useTracerCheckBox = new JCheckBox();
        tracerComboBox = new JComboBox();
        adminLabel = new JLabel();
        adminTextField = new JTextField();
        adminPasswordLabel = new JLabel();
        adminPasswordTextField = new JTextField();
        useSaveURIMapsCheckBox = new JCheckBox();
        resetPassswordLabel = new JLabel();
        resetPasswordTextField = new JTextField();
        typeOfExtentDataLabel = new JLabel();
        normalExtentRadioButton = new JRadioButton();
        byuExtentRadioButton = new JRadioButton();
        ignoreUnlinkedRecordsLabel = new JLabel();
        ignoreUnlinkedNamesCheckBox = new JCheckBox();
        ignoreUnlinkedSubjectsCheckBox = new JCheckBox();
        publishPanel = new JPanel();
        label1 = new JLabel();
        publishReposCheckBox = new JCheckBox();
        publishNamesCheckBox = new JCheckBox();
        publishSubjectsCheckBox = new JCheckBox();
        publishAccessionsCheckBox = new JCheckBox();
        publishDigitalObjectsCheckBox = new JCheckBox();
        publishResourcesCheckBox = new JCheckBox();
        simulateCheckBox = new JCheckBox();
        useScriptCheckBox = new JCheckBox();
        editScriptButton = new JButton();
        batchImportCheckBox = new JCheckBox();
        numResourceToCopyLabel = new JLabel();
        numResourceToCopyTextField = new JTextField();
//        deleteResourcesCheckBox = new JCheckBox();
        resourcesToCopyTextField = new JTextField();
        outputConsoleLabel = new JLabel();
        copyProgressBar = new JProgressBar();
        scrollPane1 = new JScrollPane();
        consoleTextArea = new JTextArea();
        recordURIComboBox = new JComboBox();
        paramsLabel = new JLabel();
        paramsTextField = new JTextField();
        viewRecordButton = new JButton();
        testRecordButton = new JButton();
        buttonBar = new JPanel();
        errorLogButton = new JButton();
        saveErrorsLabel = new JLabel();
        errorCountLabel = new JLabel();
        stopButton = new JButton();
        basicUIButton = new JButton();
        okButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("Archives Space Data Migrator");
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(Borders.DIALOG_BORDER);

            // JFormDesigner evaluation mark
            dialogPane.setBorder(new javax.swing.border.CompoundBorder(
                new javax.swing.border.TitledBorder(new javax.swing.border.EmptyBorder(0, 0, 0, 0),
                    "JFormDesigner Evaluation", javax.swing.border.TitledBorder.CENTER,
                    javax.swing.border.TitledBorder.BOTTOM, new java.awt.Font("Dialog", java.awt.Font.BOLD, 12),
                    java.awt.Color.red), dialogPane.getBorder())); dialogPane.addPropertyChangeListener(new java.beans.PropertyChangeListener(){public void propertyChange(java.beans.PropertyChangeEvent e){if("border".equals(e.getPropertyName()))throw new RuntimeException();}});

            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setLayout(new FormLayout(
                    new ColumnSpec[] {
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        new ColumnSpec(ColumnSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW),
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC
                    },
                    new RowSpec[] {
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.LINE_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.LINE_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.LINE_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.LINE_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.LINE_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.LINE_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.LINE_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.LINE_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.LINE_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.LINE_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.LINE_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.LINE_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC,
                        FormFactory.LINE_GAP_ROWSPEC,
                        new RowSpec(RowSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW),
                        FormFactory.LINE_GAP_ROWSPEC,
                        FormFactory.DEFAULT_ROWSPEC
                    }));

                //---- apiLabel ----
                apiLabel.setText("  Archives Space Version: v2.x");
                apiLabel.setHorizontalTextPosition(SwingConstants.CENTER);
                contentPanel.add(apiLabel, cc.xy(1, 1));

                //---- sourceLabel ----
                sourceLabel.setText("Source");
                contentPanel.add(sourceLabel, cc.xy(3, 1));

                //---- sourceTextField ----
                sourceTextField.setColumns(4);
                sourceTextField.setText("2");
                contentPanel.add(sourceTextField, cc.xy(5, 1));

                //---- threadLabel ----
                threadLabel.setText("Threads");
                contentPanel.add(threadLabel, cc.xy(11, 1));

                //---- threadsTextField ----
                threadsTextField.setColumns(4);
                threadsTextField.setText("1");
                contentPanel.add(threadsTextField, cc.xy(13, 1));

                //---- copyToASpaceButton ----
                copyToASpaceButton.setText("Copy To Archives Space");
                copyToASpaceButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        CopyToASpaceButtonActionPerformed();
                    }
                });
                contentPanel.add(copyToASpaceButton, cc.xy(1, 3));

                //---- hostLabel ----
                hostLabel.setText("Host");
                contentPanel.add(hostLabel, cc.xy(3, 3));

                //---- hostTextField ----
                hostTextField.setText("http://localhost:8089");
                contentPanel.add(hostTextField, cc.xywh(5, 3, 9, 1));

                //---- repositoryCheckButton ----
                repositoryCheckButton.setText("Run Repository Check");
                repositoryCheckButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        repositoryCheckButtonActionPerformed();
                    }
                });
                contentPanel.add(repositoryCheckButton, cc.xy(1, 5));

                //---- copyRecordCheckBox ----
                copyRecordCheckBox.setText("Copy Records When Done");
                copyRecordCheckBox.setSelected(true);
                contentPanel.add(copyRecordCheckBox, cc.xywh(3, 5, 7, 1));

                //---- viewRepositoryCheckReportButton ----
                viewRepositoryCheckReportButton.setText("View Report");
                viewRepositoryCheckReportButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        viewRepositoryCheckReportButtonActionPerformed();
                    }
                });
                contentPanel.add(viewRepositoryCheckReportButton, cc.xywh(11, 5, 3, 1));

                //======== tracerPanel ========
                {
                    tracerPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

                    //---- useTracerCheckBox ----
                    useTracerCheckBox.setText("Use Tracer Database");
                    tracerPanel.add(useTracerCheckBox);

                    //---- tracerComboBox ----
                    tracerComboBox.setModel(new DefaultComboBoxModel(new String[] {
                        "1",
                        "2",
                        "3",
                        "SB"
                    }));
                    tracerPanel.add(tracerComboBox);
                }
                contentPanel.add(tracerPanel, cc.xy(1, 7));

                //---- adminLabel ----
                adminLabel.setText("Admin");
                contentPanel.add(adminLabel, cc.xy(3, 7));

                //---- adminTextField ----
                adminTextField.setText("asadmin");
                contentPanel.add(adminTextField, cc.xy(5, 7));

                //---- adminPasswordLabel ----
                adminPasswordLabel.setText("Password");
                contentPanel.add(adminPasswordLabel, cc.xy(11, 7));

                //---- adminPasswordTextField ----
                adminPasswordTextField.setText("admin");
                contentPanel.add(adminPasswordTextField, cc.xy(13, 7));

                //---- useSaveURIMapsCheckBox ----
                useSaveURIMapsCheckBox.setText("Continue Previous Migration");
                contentPanel.add(useSaveURIMapsCheckBox, cc.xy(1, 9));

                //---- resetPassswordLabel ----
                resetPassswordLabel.setText("Reset Password");
                contentPanel.add(resetPassswordLabel, cc.xywh(3, 9, 5, 1));

                //---- resetPasswordTextField ----
                resetPasswordTextField.setText("archive");
                contentPanel.add(resetPasswordTextField, cc.xywh(9, 9, 5, 1));

                //---- typeOfExtentDataLabel ----
                typeOfExtentDataLabel.setText("  Specify Type of Extent Data");
                contentPanel.add(typeOfExtentDataLabel, cc.xy(1, 11));

                //---- normalExtentRadioButton ----
                normalExtentRadioButton.setText("Normal or Harvard Plugin");
                normalExtentRadioButton.setSelected(true);
                contentPanel.add(normalExtentRadioButton, cc.xywh(3, 11, 5, 1));

                //---- byuExtentRadioButton ----
                byuExtentRadioButton.setText("BYU Plugin");
                contentPanel.add(byuExtentRadioButton, cc.xywh(9, 11, 5, 1));

                //---- ignoreUnlinkedRecordsLabel ----
                ignoreUnlinkedRecordsLabel.setText("  Specify Unlinked Records to NOT Copy");
                contentPanel.add(ignoreUnlinkedRecordsLabel, cc.xy(1, 13));

                //---- ignoreUnlinkedNamesCheckBox ----
                ignoreUnlinkedNamesCheckBox.setText("Name Records");
                contentPanel.add(ignoreUnlinkedNamesCheckBox, cc.xywh(3, 13, 5, 1));

                //---- ignoreUnlinkedSubjectsCheckBox ----
                ignoreUnlinkedSubjectsCheckBox.setText("Subject Records");
                contentPanel.add(ignoreUnlinkedSubjectsCheckBox, cc.xywh(9, 13, 5, 1));

                //======== publishPanel ========
                {
                    publishPanel.setLayout(new FormLayout(
                        new ColumnSpec[] {
                            new ColumnSpec(ColumnSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW),
                            FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                            FormFactory.DEFAULT_COLSPEC,
                            FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                            FormFactory.DEFAULT_COLSPEC,
                            FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                            FormFactory.DEFAULT_COLSPEC,
                            FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                            FormFactory.DEFAULT_COLSPEC,
                            FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                            FormFactory.DEFAULT_COLSPEC,
                            FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                            FormFactory.DEFAULT_COLSPEC
                        },
                        RowSpec.decodeSpecs("default")));

                    //---- label1 ----
                    label1.setText("  Records To Publish?");
                    publishPanel.add(label1, cc.xy(1, 1));

                    //---- publishReposCheckBox ----
                    publishReposCheckBox.setText("Repositories");
                    publishReposCheckBox.setSelected(true);
                    publishPanel.add(publishReposCheckBox, cc.xy(3, 1));

                    //---- publishNamesCheckBox ----
                    publishNamesCheckBox.setText("Names");
                    publishPanel.add(publishNamesCheckBox, cc.xy(5, 1));

                    //---- publishSubjectsCheckBox ----
                    publishSubjectsCheckBox.setText("Subjects");
                    publishPanel.add(publishSubjectsCheckBox, cc.xy(7, 1));

                    //---- publishAccessionsCheckBox ----
                    publishAccessionsCheckBox.setText("Accessions");
                    publishPanel.add(publishAccessionsCheckBox, cc.xy(9, 1));

                    //---- publishDigitalObjectsCheckBox ----
                    publishDigitalObjectsCheckBox.setText("Digital Objects");
                    publishDigitalObjectsCheckBox.setSelected(true);
                    publishPanel.add(publishDigitalObjectsCheckBox, cc.xy(11, 1));

                    //---- publishResourcesCheckBox ----
                    publishResourcesCheckBox.setText("Resources");
                    publishResourcesCheckBox.setSelected(true);
                    publishPanel.add(publishResourcesCheckBox, cc.xy(13, 1));
                }
                contentPanel.add(publishPanel, cc.xywh(1, 15, 13, 1));

                //---- simulateCheckBox ----
                simulateCheckBox.setText("Simulate REST Calls");
                contentPanel.add(simulateCheckBox, cc.xy(1, 17));

                //---- useScriptCheckBox ----
                useScriptCheckBox.setText("Use Mapper Script");
                contentPanel.add(useScriptCheckBox, cc.xywh(3, 17, 5, 1));

                //---- editScriptButton ----
                editScriptButton.setText("Edit or Load Script");
                editScriptButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        editScriptButtonActionPerformed();
                    }
                });
                contentPanel.add(editScriptButton, cc.xywh(9, 17, 5, 1));

                //---- batchImportCheckBox ----
                batchImportCheckBox.setText("Use Batch Import for Resources");
                batchImportCheckBox.setSelected(true);
                contentPanel.add(batchImportCheckBox, cc.xy(1, 19));

                //---- numResourceToCopyLabel ----
                numResourceToCopyLabel.setText("Number of  Resources To Copy");
                contentPanel.add(numResourceToCopyLabel, cc.xywh(3, 19, 5, 1));

                //---- numResourceToCopyTextField ----
                numResourceToCopyTextField.setText("100000");
                contentPanel.add(numResourceToCopyTextField, cc.xywh(9, 19, 5, 1));

//                //---- deleteResourcesCheckBox ----
//                deleteResourcesCheckBox.setText("Delete Previously Saved Resources");
//                contentPanel.add(deleteResourcesCheckBox, cc.xy(1, 21));

                //---- resourcesToCopyTextField ----
                resourcesToCopyTextField.setText("-refid_unique, -term_default");
                contentPanel.add(resourcesToCopyTextField, cc.xywh(3, 21, 11, 1));

                //---- outputConsoleLabel ----
                outputConsoleLabel.setText("Output Console:");
                contentPanel.add(outputConsoleLabel, cc.xy(1, 23));
                contentPanel.add(copyProgressBar, cc.xywh(3, 23, 11, 1));

                //======== scrollPane1 ========
                {

                    //---- consoleTextArea ----
                    consoleTextArea.setRows(12);
                    consoleTextArea.setLineWrap(true);
                    scrollPane1.setViewportView(consoleTextArea);
                }
                contentPanel.add(scrollPane1, cc.xywh(1, 25, 13, 1));

                //---- recordURIComboBox ----
                recordURIComboBox.setModel(new DefaultComboBoxModel(new String[] {
                    "/repositories",
                    "/users",
                    "/subjects",
                    "/agents/families/1",
                    "/agents/people/1",
                    "/agents/corporate_entities/1",
                    "/repositories/2/accessions/1",
                    "/repositories/2/resources/1",
                    "/repositories/2/archival_objects/1",
                    "/config/enumerations"
                }));
                recordURIComboBox.setEditable(true);
                contentPanel.add(recordURIComboBox, cc.xy(1, 27));

                //---- paramsLabel ----
                paramsLabel.setText("Params");
                contentPanel.add(paramsLabel, cc.xy(3, 27));

                //---- paramsTextField ----
                paramsTextField.setText("page=1");
                contentPanel.add(paramsTextField, cc.xywh(5, 27, 3, 1));

                //---- viewRecordButton ----
                viewRecordButton.setText("View");
                viewRecordButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        viewRecordButtonActionPerformed();
                    }
                });
                contentPanel.add(viewRecordButton, cc.xy(11, 27));

                //---- testRecordButton ----
                testRecordButton.setText("Test");
                testRecordButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        testRecordButtonActionPerformed();
                    }
                });
                contentPanel.add(testRecordButton, cc.xy(13, 27));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
                buttonBar.setLayout(new FormLayout(
                    new ColumnSpec[] {
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.GLUE_COLSPEC,
                        FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.DEFAULT_COLSPEC,
                        FormFactory.BUTTON_COLSPEC
                    },
                    RowSpec.decodeSpecs("pref")));

                //---- errorLogButton ----
                errorLogButton.setText("View Error Log");
                errorLogButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        errorLogButtonActionPerformed();
                    }
                });
                buttonBar.add(errorLogButton, cc.xy(2, 1));

                //---- saveErrorsLabel ----
                saveErrorsLabel.setText(" Errors/Warnings: ");
                buttonBar.add(saveErrorsLabel, cc.xy(4, 1));

                //---- errorCountLabel ----
                errorCountLabel.setText("N/A ");
                errorCountLabel.setForeground(Color.red);
                errorCountLabel.setFont(new Font("Lucida Grande", Font.BOLD, 13));
                buttonBar.add(errorCountLabel, cc.xy(6, 1));

                //---- stopButton ----
                stopButton.setText("Cancel Copy");
                stopButton.setEnabled(false);
                stopButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        stopButtonActionPerformed();
                        stopButtonActionPerformed();
                    }
                });
                buttonBar.add(stopButton, cc.xy(9, 1));

                //---- basicUIButton ----
                basicUIButton.setText("Basic UI");
                basicUIButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        basicUIButtonActionPerformed();
                    }
                });
                buttonBar.add(basicUIButton, cc.xy(10, 1));

                //---- okButton ----
                okButton.setText("Close");
                okButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        okButtonActionPerformed();
                    }
                });
                buttonBar.add(okButton, cc.xy(12, 1));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());

        //---- buttonGroup1 ----
        ButtonGroup buttonGroup1 = new ButtonGroup();
        buttonGroup1.add(normalExtentRadioButton);
        buttonGroup1.add(byuExtentRadioButton);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }


    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner Evaluation license - Sarah Morrissey
    private JPanel dialogPane;
    private JPanel contentPanel;
    private JLabel apiLabel;
    private JLabel sourceLabel;
    private JTextField sourceTextField;
    private JLabel threadLabel;
    private JTextField threadsTextField;
    private JButton copyToASpaceButton;
    private JLabel hostLabel;
    private JTextField hostTextField;
    private JButton repositoryCheckButton;
    private JCheckBox copyRecordCheckBox;
    private JButton viewRepositoryCheckReportButton;
    private JPanel tracerPanel;
    private JCheckBox useTracerCheckBox;
    private JComboBox tracerComboBox;
    private JLabel adminLabel;
    private JTextField adminTextField;
    private JLabel adminPasswordLabel;
    private JTextField adminPasswordTextField;
    private JCheckBox useSaveURIMapsCheckBox;
    private JLabel resetPassswordLabel;
    private JTextField resetPasswordTextField;
    private JLabel typeOfExtentDataLabel;
    private JRadioButton normalExtentRadioButton;
    private JRadioButton byuExtentRadioButton;
    private JLabel ignoreUnlinkedRecordsLabel;
    private JCheckBox ignoreUnlinkedNamesCheckBox;
    private JCheckBox ignoreUnlinkedSubjectsCheckBox;
    private JPanel publishPanel;
    private JLabel label1;
    private JCheckBox publishReposCheckBox;
    private JCheckBox publishNamesCheckBox;
    private JCheckBox publishSubjectsCheckBox;
    private JCheckBox publishAccessionsCheckBox;
    private JCheckBox publishDigitalObjectsCheckBox;
    private JCheckBox publishResourcesCheckBox;
    private JCheckBox simulateCheckBox;
    private JCheckBox useScriptCheckBox;
    private JButton editScriptButton;
    private JCheckBox batchImportCheckBox;
    private JLabel numResourceToCopyLabel;
    private JTextField numResourceToCopyTextField;
//    private JCheckBox deleteResourcesCheckBox;
    private JTextField resourcesToCopyTextField;
    private JLabel outputConsoleLabel;
    private JProgressBar copyProgressBar;
    private JScrollPane scrollPane1;
    private JTextArea consoleTextArea;
    private JComboBox recordURIComboBox;
    private JLabel paramsLabel;
    private JTextField paramsTextField;
    private JButton viewRecordButton;
    private JButton testRecordButton;
    private JPanel buttonBar;
    private JButton errorLogButton;
    private JLabel saveErrorsLabel;
    private JLabel errorCountLabel;
    private JButton stopButton;
    private JButton basicUIButton;
    private JButton okButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables

    /**
     * Main method for testing in stand alone mode
     */
    public static void main(String[] args) {
        dbCopyFrame frame = new dbCopyFrame(false);
        frame.pack();
        frame.setVisible(true);
    }

}
