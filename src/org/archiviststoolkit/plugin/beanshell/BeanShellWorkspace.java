/*
 * Created by JFormDesigner on Wed Jul 06 13:54:42 EDT 2011
 */

package org.archiviststoolkit.plugin.beanshell;

import java.awt.event.*;

import bsh.EvalError;
import bsh.Interpreter;
import bsh.util.JConsole;

import org.archiviststoolkit.ApplicationFrame;
import org.archiviststoolkit.importer.ImportExportLogDialog;
import org.archiviststoolkit.model.*;
import org.archiviststoolkit.mydomain.*;
import org.archiviststoolkit.plugin.ScriptAT;
import org.archiviststoolkit.plugin.dbdialog.RemoteDBConnectDialog;
import org.archiviststoolkit.plugin.dbdialog.RemoteDBConnectDialogLight;
import org.archiviststoolkit.plugin.utils.MSAccessDatabaseConnection;
import org.archiviststoolkit.plugin.utils.ScriptDataUtils;
import org.archiviststoolkit.plugin.utils.ScriptsDAO;
import org.archiviststoolkit.plugin.utils.StopWatch;
import org.archiviststoolkit.structure.*;
import org.archiviststoolkit.util.*;
import org.hibernate.Session;

import java.awt.*;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 * @author Nathan Stevens
 */
public class BeanShellWorkspace extends JDialog {
    private ApplicationFrame applicationFrame = null;

    private RemoteDBConnectDialog dbDialog = null;

    // A DAO object to load and save the list of scripts from the
    // database when running in stand alone mode
    private ScriptsDAO scriptsDAO = null;

    private DomainTableWorkSurface workSurface = null;

    private ScriptViewerDialog scriptViewDialog = null;

    private ScriptManagerDialog scriptManagerDialog = null;

    private JConsole console; // The console

    private Interpreter interpreter; // the bsh interpreter

    private final JFileChooser fc = new JFileChooser(); // used to open the file

    private File currentScriptFile = null; // the file of the current script

    private String currentScriptName = ""; // the name of the current script

    private String currentScript = ""; // the current script that was loaded

    private Resources currentResourceRecord = null; // the current resource record

    private boolean stopScript = false; // specifies if the script should be stoped

    private StopWatch stopWatch = null;

    // Hash that stores all the loaded scripts
    private HashMap<String, String> storedScripts = new HashMap<String, String>();

    // Hash that stores remote connections dialogs
    private HashMap<String,RemoteDBConnectDialogLight> storedRCDS = new HashMap<String, RemoteDBConnectDialogLight>();

    /**
     * Constructor that is called when running in the main AT window
     * 
     * @param title
     * @param applicationFrame
     */
    public BeanShellWorkspace(String title, ApplicationFrame applicationFrame) {
        super(applicationFrame);
        this.applicationFrame = applicationFrame;
        initComponents();
        initConsole();
        printRecordType();
        loadScriptsFromDatabase();

        // set the title
        setTitle(title);

        // set the current directory to working directory
        fc.setCurrentDirectory(new File("."));
    }

    /**
     * Constructor called when running in stand alone mode
     *
     * @param owner
     */
    public BeanShellWorkspace(Frame owner) {
        super(owner);
        initComponents();
        initConsole();

        // set the current directory to working directory
        fc.setCurrentDirectory(new File("."));
    }

    /**
     * Method called when running as an RDE plugin 
     *
     * @param owner
     */
    public BeanShellWorkspace(Dialog owner) {
        super(owner);
        initComponents();
        initConsole();
        loadScriptsFromDatabase();

        // set the current directory to working directory
        fc.setCurrentDirectory(new File("."));
    }

    /**
     * Method to display the bean shell console
     */
    private void initConsole() {
        try {
            console = new JConsole();
            contentPanel.add(console);

            interpreter = new Interpreter( console );
            interpreter.set("workspaceDialog", this);
            
            // if application frame doesn't equals null then set the workspace
            if(applicationFrame != null) {
                // set the interpreter class loader so it know where to find things
                interpreter.setClassLoader(ScriptAT.class.getClassLoader());

                workSurface = applicationFrame.getWorkSurfaceContainer().getCurrentWorkSurface();
                interpreter.set("workSurface", workSurface);



                // if the work surface is for resources, then load the resource
                // record in long session to make it easier for scripts to use
                if(workSurface instanceof ResourceTableWorkSurface) {
                    currentResourceRecord = (Resources)workSurface.getCurrentDomainObjectFromDatabase();
                    interpreter.set("resource", currentResourceRecord);
                }
            }

            // import classes into the interpreter so we don't have to in the script
            importClassesIntoInterpreter();

            new Thread( interpreter ).start(); // start a thread to call the run() method
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to hide certain buttons when a particular script is
     * going to be directly executed
     */
    public void hideFunctionality() {
        loadButton.setVisible(false);
        //viewScriptButton.setVisible(false);
        manageButton.setVisible(false);
        commandComboBox.setVisible(false);
    }

    /**
     * Method to set the database dialog
     * @param dbDialog
     */
    public void setDbDialog(RemoteDBConnectDialog dbDialog) {
        this.dbDialog = dbDialog;
        scriptsDAO = new ScriptsDAO(dbDialog.getSession());
        loadScriptsFromDatabase();
    }

    /**
     * Method to set the resource record
     * 
     * @param record
     * @throws Exception
     */
    public void setResource(Resources record) throws Exception {
        if(record != null) {
            importClassesIntoInterpreter();
            interpreter.set("resource", record);
            interpreter.eval("print (\"Resource Record: \" + resource);");
            currentResourceRecord = record;
        }
    }

    /**
     * Method to set the parent resource component
     *
     * @param component
     * @throws Exception
     */
    public void setResourceComponent(ResourcesComponents component) throws Exception {
        if(component != null) {
            importClassesIntoInterpreter();
            interpreter.set("parentComponent", component);
            interpreter.eval("print (\"Parent Component: \" + component);");
        }
    }

    /**
     * Method to import certain AT classes into the beanshell interpreter
     */
    private void importClassesIntoInterpreter() {
        try {
            interpreter.eval("import org.archiviststoolkit.model.*;");
            interpreter.eval("import org.archiviststoolkit.mydomain.*;");
            interpreter.eval("import org.archiviststoolkit.util.*;");
            interpreter.eval("import org.archiviststoolkit.structure.*;");
            interpreter.eval("import org.archiviststoolkit.exceptions.*;");
            interpreter.eval("import org.archiviststoolkit.plugin.utils.*");
        } catch(EvalError e) {
            System.out.println("Problem importing AT classes ...");
            e.printStackTrace();
        }
    }

    /**
     * Method to print the type of record(s) being processed
     */
    public void printRecordType() {
        try {
            if (workSurface.getClazz() == Names.class) {
                interpreter.eval("print (\"Name Records\");");
            } else if (workSurface.getClazz() == Subjects.class) {
                interpreter.eval("print (\"Subject Records\");");
            } else if (workSurface.getClazz() == Accessions.class) {
                interpreter.eval("print (\"Accession Records\");");
            } else if (workSurface.getClazz() == Resources.class) {
                interpreter.eval("print (\"Resource Records\");");
            } else if (workSurface.getClazz() == DigitalObjects.class) {
                interpreter.eval("print (\"Digital Object Records\");");
            } else {
                interpreter.eval("print (\"Unknown Record Type\");");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Close dialog and dispose of it when OK hit
     */
    private void okButtonActionPerformed() {
        setVisible(false);
        dispose();
    }

    /**
     *  Method to open the file chooser to load a bean shell script from the command
     * line.
     */
    private void loadButtonActionPerformed() {
        int returnVal = fc.showOpenDialog(this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            setCurrentScript(file);
            loadScript(file);
        }
    }

    /**
     * Method to load the current file into the interpreter
     *
     * @param file
     */
    private void loadScript(final File file) {
        Thread performer = new Thread(new Runnable() {
            public void run() {
                try {
                    // set stop script boolean to true
                    stopScript = false;

                    loadButton.setEnabled(false);
                    reloadButton.setEnabled(false);

                    interpreter.source(file.getAbsolutePath());

                    loadButton.setEnabled(true);
                    reloadButton.setEnabled(true);
                } catch (Exception e) {
                    interpreter.println(e.getMessage());
                    loadButton.setEnabled(true);
                    reloadButton.setEnabled(true);
                }
            }
        });
        performer.start();
    }

    /**
     * Method to run the current script
     */
    private void loadScript() {
        Thread performer = new Thread(new Runnable() {
            public void run() {
                try {
                    // set stop script boolean to true
                    stopScript = false;

                    loadButton.setEnabled(false);
                    reloadButton.setEnabled(false);

                    interpreter.eval(currentScript);

                    loadButton.setEnabled(true);
                    reloadButton.setEnabled(true);
                } catch (Exception e) {
                    interpreter.println(e.getMessage());
                    loadButton.setEnabled(true);
                    reloadButton.setEnabled(true);
                }
            }
        });
        performer.start();
    }

    /**
     * Method to load a script that already in the stored scripts
     * HashMap
     *
     * @param scriptName
     */
    public void loadScript(String scriptName) {
        if(storedScripts.containsKey(scriptName)) {
            currentScriptName = scriptName;
            currentScript = storedScripts.get(scriptName);
            loadScript();
        } else {
            System.out.println("Script not found ... + " + scriptName);
        }
    }

    /**
     * Method to set the current file and read the contents of file and place it in a file
     *
     * @param file The file containing the beanshell script
     */
    private void setCurrentScript(File file) {
        // set the current file
        currentScriptFile = file;

        // read the file and convert the contents to a string
        byte[] buffer = new byte[(int) file.length()];
        BufferedInputStream f = null;
        try {
            f = new BufferedInputStream(new FileInputStream(file));
            f.read(buffer);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (f != null) try {
                f.close();
            } catch (IOException ignored) { }
        }

        // set the current script and it's name
        currentScript = new String(buffer);
        currentScriptName = file.getName();

        // add the script to storedScripts hashmap
        storeScript(currentScriptName, currentScript);
    }

    /**
     * Method to add a script to the storedScripts Hashmap
     *
     * @param scriptName
     * @param script
     */
    public void storeScript(String scriptName, String script) {
        if(!storedScripts.containsKey(scriptName)) {
            // add it to the command combo box
            commandComboBox.addItem(scriptName);
        }

        storedScripts.put(scriptName, script);

        System.out.println("Number of stored scripts: " + storedScripts.size());
    }

    /**
     * Method to load stored scripts from the database
     */
    public void loadScriptsFromDatabase() {
        HashMap<String, String> scripts = null;
        try {
            if(scriptsDAO == null) {
                scriptsDAO = new ScriptsDAO();
            }

            scripts = scriptsDAO.loadScripts();

            if(scripts != null && scripts.size() > 0) {
                storedScripts = scripts;
                ArrayList<String> sortedNames = new ArrayList<String>(storedScripts.keySet());
                Collections.sort(sortedNames);

                for(String scriptName: sortedNames) {
                    commandComboBox.addItem(scriptName);
                }
            }
        } catch(Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Failed to load scripts from database ...",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Method to save any stored scripts to the database
     */
    public void saveScriptsToDatabase() {
        try {
            if(scriptsDAO != null) {
                scriptsDAO.saveScripts(storedScripts);
            }
        } catch(Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Failed to saved scripts to database ...",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void viewScriptButtonActionPerformed() {
        if(scriptViewDialog == null) {
            scriptViewDialog = new ScriptViewerDialog(this, currentScriptName);
        }

        // set the current script and make dialog visible
        scriptViewDialog.setCurrentScript(currentScript);
        scriptViewDialog.setVisible(true);
    }

    private void reloadButtonActionPerformed() {
        if(currentScriptFile != null) {
            loadScript(currentScriptFile);
        } else {
            loadScript();
        }
    }

    public JConsole getConsole() {
        return console;
    }

    public Interpreter getInterpreter() {
        return interpreter;
    }

    /**
     * Method to display the manage dialog which allows a user to store
     * rename, and delete scripts in the database, and add to the run
     * command drop-box
     */
    private void manageButtonActionPerformed() {
        if(scriptManagerDialog == null) {
            scriptManagerDialog = new ScriptManagerDialog(this);
            scriptManagerDialog.pack();
        }

        scriptManagerDialog.setStoredScripts(storedScripts);
        scriptManagerDialog.setVisible(true);
    }

    private void commandComboBoxActionPerformed() {
        if(commandComboBox.getSelectedIndex() != 0) {
            currentScriptName = (String)commandComboBox.getSelectedItem();
            currentScript = storedScripts.get(currentScriptName);
        }
    }

    /**
     * Method to set the stop variable that will cause any scripts that check
     * for this to stop their current processing task
     */
    private void stopButtonActionPerformed() {
        stopScript = true;
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner non-commercial license
        dialogPane = new JPanel();
        contentPanel = new JPanel();
        buttonBar = new JPanel();
        loadButton = new JButton();
        reloadButton = new JButton();
        stopButton = new JButton();
        viewScriptButton = new JButton();
        manageButton = new JButton();
        commandComboBox = new JComboBox();
        okButton = new JButton();

        //======== this ========
        setTitle("BeanShell Workspace v0.44");
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(new EmptyBorder(12, 12, 12, 12));
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setLayout(new BorderLayout());
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(new EmptyBorder(12, 0, 0, 0));
                buttonBar.setLayout(new GridBagLayout());
                ((GridBagLayout)buttonBar.getLayout()).columnWidths = new int[] {0, 0, 0, 0, 0, 0, 85, 80};
                ((GridBagLayout)buttonBar.getLayout()).columnWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0};

                //---- loadButton ----
                loadButton.setText("Load");
                loadButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        loadButtonActionPerformed();
                    }
                });
                buttonBar.add(loadButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 5), 0, 0));

                //---- reloadButton ----
                reloadButton.setText("Run");
                reloadButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        reloadButtonActionPerformed();
                    }
                });
                buttonBar.add(reloadButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 5), 0, 0));

                //---- stopButton ----
                stopButton.setText("Stop");
                stopButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        stopButtonActionPerformed();
                    }
                });
                buttonBar.add(stopButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 5), 0, 0));

                //---- viewScriptButton ----
                viewScriptButton.setText("View");
                viewScriptButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        viewScriptButtonActionPerformed();
                    }
                });
                buttonBar.add(viewScriptButton, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 5), 0, 0));

                //---- manageButton ----
                manageButton.setText("Manage");
                manageButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        manageButtonActionPerformed();
                    }
                });
                buttonBar.add(manageButton, new GridBagConstraints(4, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 5), 0, 0));

                //---- commandComboBox ----
                commandComboBox.setModel(new DefaultComboBoxModel(new String[] {
                    "Select Script To Run"
                }));
                commandComboBox.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        commandComboBoxActionPerformed();
                    }
                });
                buttonBar.add(commandComboBox, new GridBagConstraints(5, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 5), 0, 0));

                //---- okButton ----
                okButton.setText("OK");
                okButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        okButtonActionPerformed();
                    }
                });
                buttonBar.add(okButton, new GridBagConstraints(7, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 0), 0, 0));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        setSize(770, 325);
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner non-commercial license
    private JPanel dialogPane;
    private JPanel contentPanel;
    private JPanel buttonBar;
    private JButton loadButton;
    private JButton reloadButton;
    private JButton stopButton;
    private JButton viewScriptButton;
    private JButton manageButton;
    private JComboBox commandComboBox;
    private JButton okButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables

    /* Method below this point are convenience methods used by scripts */

    /**
     * Method to display the AT log window for showing text the user can copy, save, or print out
     *
     * @param title
     * @param text
     */
    public void displayLogDialog(String title, String text) {
        ImportExportLogDialog logDialog = new ImportExportLogDialog(this, ImportExportLogDialog.DIALOG_TYPE_IMPORT, text);
        logDialog.setTitle(title);
        logDialog.showDialog();
    }


    /**
     * Method to return the selected records from the current work surface
     *
     * @return  ArrayList containing the selected records
     */
    public ArrayList<DomainObject> getSelectedRecords() {
        ArrayList<DomainObject> records = new ArrayList<DomainObject>();

        if(workSurface != null) {
            DomainSortableTable table = (DomainSortableTable)workSurface.getTable();
            int[] selectedRows = table.getSelectedRows();
            for (int i = 0; i < selectedRows.length; i++) {
                DomainObject domainObject = table.getFilteredList().get(selectedRows[i]);
                records.add(domainObject);
            }
        }

        return records;
    }

    /**
     * Method display the file chooser dialog
     *
     * @return
     */
    public File displayFileChooser(File currentDirectory) {
        if(currentDirectory != null) {
            fc.setCurrentDirectory(currentDirectory);
        }

        int returnVal = fc.showOpenDialog(this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            return file;
        } else {
            return null;
        }
    }

    /**
     * Method to display a dialog that lets users input data in a simple
     * dialog
     *
     * @param title
     * @param options
     * @return
     */
    public String[] displayInputDialog(String title, String instruction, String[] options) {
        ScriptInputDialog inputDialog = new ScriptInputDialog(this, title, instruction, options);
        inputDialog.pack();
        inputDialog.setVisible(true);

        if(inputDialog.isCancelled()) {
            return null;
        } else {
            return inputDialog.getUserInput();
        }
    }

    /**
     * Method to save the "currentResourceRecord" resource record to
     * the database
     *
     */
    public void saveCurrentResourceRecord() {
        saveResourceRecord(currentResourceRecord);
    }

    /**
     * Method to save a resource record
     * 
     * @param record
     */
    public boolean saveResourceRecord(Resources record) {
        try {
            setRepository(record);
            String s = new String();

            if(dbDialog != null) {


                dbDialog.saveRecord(record);
            } else {
                ResourcesDAO access = new ResourcesDAO();
                access.getLongSession();
                access.updateLongSession(record, false);
            }
            
            return true;
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Method to set the audit info. This is only used when running in
     * stand alone mode outside of the AT
     *
     * @param record The domain object to set the audit info for
     */
    public void setAuditInfo(DomainObject record) {
        if(dbDialog != null) {
            dbDialog.setAuditInfo(record);
        }
    }

    /**
     * Method to set the current repository
     * @param record
     */
    public void setRepository(Resources record) {
        if(record.getRepository() == null) {
            if(dbDialog != null) {
                record.setRepository(dbDialog.getSelectedUser().getRepository());
            } else {
                record.setRepository(ApplicationFrame.getInstance().getCurrentUserRepository());
            }
        }
    }

    /**
     * Method to get the current repository
     *
     * @return  The current users repository
     */
    public Repositories getRepository() {
        if (dbDialog != null) {
            return dbDialog.getSelectedUser().getRepository();
        } else {
            return applicationFrame.getCurrentUserRepository();
        }
    }

    /**
     * Method to return a Notes Etc Type by the canonical name
     *
     * @param name The canonical name of the Notes ETC type
     * @return
     */
    public NotesEtcTypes lookupNoteEtcTypeByName(String name) {
        if(dbDialog != null) {
            return dbDialog.lookupNoteEtcTypeByName(name);
        } else {
            try {
                NotesEtcTypes notesEtcType = NoteEtcTypesUtils.lookupNoteEtcTypeByCannonicalName(name);

                return notesEtcType;
            } catch(Exception e) {
                return null;
            }
        }
    }

    /**
     * Convenience method for adding a note to a archDescriptionModel
     *
     * @param archDescriptionModel
     * @param noteName
     * @param noteContent
     * @param sequenceNumber
     */
    public ArchDescriptionNotes addNote(ArchDescription archDescriptionModel, String noteName, String noteContent, int sequenceNumber) {
        // get the notes etc type
        NotesEtcTypes noteType = lookupNoteEtcTypeByName(noteName);

        // create a new notes etc type
        ArchDescriptionNotes archDescriptionNote = new ArchDescriptionNotes(archDescriptionModel, NotesEtcTypes.DATA_TYPE_NOTE, noteType);
        archDescriptionNote.setNoteContent(noteContent);
        archDescriptionNote.setSequenceNumber(sequenceNumber);

        // now add this note to the parent object
        archDescriptionModel.addRepeatingData(archDescriptionNote);

        // return the note incase anything needs to be done to it
        return archDescriptionNote;
    }

    /**
     * Method to return an object that allows connecting to an AccessDatabase
     * Really useful for doing data transfer from access database to AT
     *
     * @param odbcName
     * @return
     */
    public MSAccessDatabaseConnection getAccessDatabaseConnection(String odbcName) {
        MSAccessDatabaseConnection MSAccessDatabaseConnection = new MSAccessDatabaseConnection(odbcName, interpreter);
        return MSAccessDatabaseConnection;
    }

    /**
     * Method to return the variable that specifies if the script should be stoped
     *
     * @return true if script is to be stoped or false otherwise
     */
    public boolean isStopScript() {
        return stopScript;
    }

    /**
     * Method to save script data to the database using the ATplugni data model
     *
     * @param dataName
     * @param scriptData
     */
    public void saveOrUpdateScriptData(String dataName, Object scriptData) {
        try {
            ScriptDataUtils.saveOrUpdateScriptData(dataName, scriptData);
        } catch(Exception e) {
            interpreter.print("Error saving script data, " + dataName + " ...");
        }
    }

    /**
     * Method to return script data that was saved to the database before
     *
     * @param dataName
     * @return
     */
    public Object getScriptData(String dataName) {
        return ScriptDataUtils.getScriptData(dataName);
    }

    /**
     * Method to save the script data t a binary file
     * 
     * @param file
     * @param scriptData
     */
    public void saveScriptData(File file, Object scriptData) {
        try {
            ScriptDataUtils.saveScriptData(file, scriptData);
        } catch(Exception e) {
            interpreter.print("Error saving script data to file, " + file + " ...");
            e.printStackTrace();
        }
    }

    /**
     * Method to return script data that was saved to a binary file
     * 
     * @param file
     * @return
     */
    public Object getScriptData(File file) {
        return ScriptDataUtils.getScriptData(file);
    }

    /**
     * Method to delete script data that was saved to the database
     * 
     * @param dataName
     */
    public void deleteScriptData(String dataName) {
        try {
            ScriptDataUtils.deleteData(dataName);
        } catch(Exception e) {
            interpreter.print("Error deleting script data, " + dataName + " ...");
        }
    }

    /**
     * Method to set the status of the record dirty so the user can be prompted to
     * save the record properly
     */
    public void setRecordDirty() {
        applicationFrame = ApplicationFrame.getInstance();

        if(applicationFrame != null) {
            applicationFrame.setRecordDirty();
        }
    }

    // Method to open the dialog that gets a session
    public Session displayRemoteConnectionDialog(String title, boolean reuse) {
        if(reuse && storedRCDS.get(title) != null) {
            RemoteDBConnectDialogLight rcd = storedRCDS.get(title);
            return rcd.getSession();
        }

        RemoteDBConnectDialogLight rcd = new RemoteDBConnectDialogLight(this);
        rcd.setModal(true);
        rcd.setTitle(title);
        rcd.pack();
        rcd.setVisible(true);

        // store this connection for future reference
        storedRCDS.put(title, rcd);

        // return the session which maybe null
        return  rcd.getSession();
    }

    /**
     * Return the store remote procedure dialog
     * @param name
     * @return
     */
    public RemoteDBConnectDialogLight getRCD(String name) {
        return storedRCDS.get(name);
    }

    /**
     * Method to start the start the time watch
     */
    public void startWatch() {
        stopWatch = new StopWatch();
        stopWatch.start();
    }

    public String stopWatch() {
        stopWatch.stop();
        return stopWatch.getPrettyTime();
    }
}
