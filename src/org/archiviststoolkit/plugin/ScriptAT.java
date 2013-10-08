package org.archiviststoolkit.plugin;

import bsh.Interpreter;
import org.archiviststoolkit.ApplicationFrame;
import org.archiviststoolkit.editor.ArchDescriptionFields;
import org.archiviststoolkit.mydomain.DomainEditorFields;
import org.archiviststoolkit.mydomain.DomainObject;
import org.archiviststoolkit.plugin.beanshell.BeanShellWorkspace;
import org.archiviststoolkit.plugin.utils.ScriptsDAO;
import org.archiviststoolkit.swing.InfiniteProgressPanel;
import org.java.plugin.Plugin;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 *
 * This is a very simple plugin that adds scripting abilities to the AT
 * or it's derivative.  Currently this plugin is accessed through the RDE
 * or tools menu.
 *
 * @author: Nathan Stevens
 * Date: July 06, 2011
 * Time: 12:10:04 PM
 */
public class ScriptAT extends Plugin implements ATPlugin {
    public static final String PLUGIN_NAME = "ScriptAT";
    public static final String DATA_NAME = "storedScripts";
    public static final String DATABASE_COPY_MENU = "Archives Space Data Migrator Advance";
    public static final String DATABASE_COPY_MENU_BASIC = "Archives Space Data Migrator";

    public static final String TOOL_CATEGORY = "@TOOL";
    public static final String RDE_CATEGORY = "@RDE";

    protected ApplicationFrame mainFrame;
    protected HashMap rdePlugins = new HashMap();

    /**
     * Get the category this plugin belongs to
     *
     * @return The category of this plugin
     */
    public String getCategory() {
        return ATPlugin.TOOL_CATEGORY + " " +
               ATPlugin.EMBEDDED_EDITOR_CATEGORY + " " +
               ATPlugin.CLI_CATEGORY;
    }

    /**
     * Get the name of the plugin
     *
     * @return The name of this plugin
     */
    public String getName() {
        return "Script Runtime v1.0";
    }

    // Method to set the AT main frame
    public void setApplicationFrame(ApplicationFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    // Method to display the full beanshell workspace which has access
    // to all the functionality
    public void showPlugin() {
        BeanShellWorkspace beanShellWorkspace = new BeanShellWorkspace(getName(), mainFrame);
        beanShellWorkspace.setVisible(true);
    }

    // Method to display the beanshell work space which just executes a particular
    // command
    public void showPlugin(String scriptName) {
        String title = getName() + " (" + scriptName + ")";
        BeanShellWorkspace beanShellWorkspace = new BeanShellWorkspace(title, mainFrame);
        beanShellWorkspace.hideFunctionality(); // hide functionality we don't need
        beanShellWorkspace.setVisible(true);
        beanShellWorkspace.loadScript(scriptName); // now run the script
    }

    // Display the database copy tool
    private void showDatabaseCopyTool(boolean basic) {
        dbCopyFrame frame = new dbCopyFrame(mainFrame, basic);
        frame.pack();
        frame.setVisible(true);
    }

    // method to display a plugin that needs a parent frame
    public void showPlugin(Frame owner) { }

    // method to display a plugin that needs a parent dialog
    public void showPlugin(Dialog owner) { }

    // Method to return any embedded plugins. This can be any thing
    public HashMap getEmbeddedPanels() {
        return null;  
    }

    // Method to return hashmap containing list of RDE plugins
    public HashMap getRapidDataEntryPlugins() {
        rdePlugins.put(PLUGIN_NAME, new ScriptATRDEPlugin());

        // get the stored scripts from the database and if they
        // are designated as RDE, load them
        String[] scriptNames = getScriptsForCategory(RDE_CATEGORY);
        for(int i = 1; i < scriptNames.length; i++) {
            String scriptName = scriptNames[i];
            rdePlugins.put(scriptName, new ScriptATRDEPlugin(scriptName));    
        }

        return rdePlugins;
    }

    // Method to set the editor field. This is not used here
    public void setEditorField(ArchDescriptionFields editorField) {  }

    // Method to set the editor field. This is not used here
    public void setEditorField(DomainEditorFields editorField) {  }

    /**
     * Method to set the domain object for this plugin
     */
    public void setModel(DomainObject domainObject, InfiniteProgressPanel monitor) { }

    /**
     * Method to get the table from which the record was selected
     * @param callingTable The table containing the record
     */
    public void setCallingTable(JTable callingTable) { }

    /**
     * Method to set the selected row of the calling table
     * @param selectedRow
     */
    public void setSelectedRow(int selectedRow) { }

    /**
     * Method to set the current record number along with the total number of records
     *
     * @param recordNumber The current record number
     * @param totalRecords The total number of records
     */
    public void setRecordPositionText(int recordNumber, int totalRecords) { }

    /**
     * Method to do a specific task in the plugin while running in command line mode
     * current task is ignored since only thing we do is just execute script that
     * was specified
     *
     * @param task
     */
    public void doTask(String task) {
        // check to see if we not running in the AT if we then, just display
        // the WorkWorkspace. This is a work around for support running in the AT
        // and at the command line
        if(mainFrame != null) {
            if(task.equals(PLUGIN_NAME)) {
                showPlugin();
            } else if(task.equals(DATABASE_COPY_MENU)) {
                showDatabaseCopyTool(false);
            } else if(task.equals(DATABASE_COPY_MENU_BASIC)) {
                showDatabaseCopyTool(true);
            } else {
                showPlugin(task);
            }

            // return now since we are not running in command line mode
            return;
        }

        // we are running at the command line parameters
        String[] params = org.archiviststoolkit.plugin.ATPluginFactory.getInstance().getCliParameters();

        for(int i = 0; i < params.length; i++) {
            System.out.println("Parameter " + (i+1) + " = " + params[i]);
        }

        try {
            // now initiate the BeanShell interpreter
            Interpreter interpreter = new Interpreter();
            interpreter.set("params", params);

            // import key AT classes which  by default
            interpreter.eval("import org.archiviststoolkit.model.*;");
            interpreter.eval("import org.archiviststoolkit.mydomain.*;");
            interpreter.eval("import org.archiviststoolkit.util.*;");
            interpreter.eval("import org.archiviststoolkit.importer.*;");

            // now get the script file and load it into the interpreter
            File file = new File(params[2]);
            interpreter.source(file.getAbsolutePath());
        } catch(Exception e) {
            System.out.println("Problem running script ...");
            e.printStackTrace();
        }
    }

    // Method to specify a specific task passing in parameters
    public boolean doTask(String task, String[] taskParams) {
        return false;
    }

    // Method to get the list of specific task the plugin can perform
    // In this case the list of commands are just script commands
    public String[] getTaskList() {
        return getScriptsForCategory(TOOL_CATEGORY);
    }

    // Method to get a string array of scripts from the database
    public String[] getScriptsForCategory(String category) {
        try {
            ScriptsDAO scriptDAO = new ScriptsDAO();
            ArrayList<String> foundScripts = scriptDAO.loadScriptsByCategory(category);
            String[] scriptNames = new String[foundScripts.size() + 3];

            scriptNames[0] = DATABASE_COPY_MENU_BASIC;
            scriptNames[1] = DATABASE_COPY_MENU;
            scriptNames[2] = PLUGIN_NAME;

            // now add all the scripts that were found to the array
            int i = 3;
            for(String scriptName: foundScripts) {
                scriptNames[i] = scriptName;
                i++;
            }

            return scriptNames;
        } catch(Exception e) {
            e.printStackTrace();
            return new String[]{PLUGIN_NAME};
        }
    }

    // Method to return the editor type for this plugin
    public String getEditorType() {
        return ATPlugin.RAPID_DATA_ENTRY_EDITOR;
    }

    // code that is executed when plugin starts.
    protected void doStart()  { }

    // code that is executed after plugin stops. not used here
    protected void doStop()  { }

    // main method for running this plugin as a stand alone application
    public static void main(String[] args) {
        ScriptATFrame frame = new ScriptATFrame();
        frame.setVisible(true);
    }
}
