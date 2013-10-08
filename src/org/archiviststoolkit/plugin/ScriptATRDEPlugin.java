package org.archiviststoolkit.plugin;

import org.archiviststoolkit.model.*;
import org.archiviststoolkit.plugin.beanshell.BeanShellWorkspace;

import javax.swing.*;

/**
 * Wrapper class which provides RDE plugin functionality to the Beanshell console
 *
 * Created by IntelliJ IDEA.
 * User: nathan
 * Date: Jul 7, 2011
 * Time: 8:04:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class ScriptATRDEPlugin implements RDEPlugin {
    private String scriptName = null; // The script to excute directly
    private Resources parentRecord = null; // The parent resource record
    private ResourcesCommon resourcesCommon = null; // The parent resource component

    // default constructor
    public ScriptATRDEPlugin() { }

    // constructor used when a script is to be executed directly from
    // the RDE drop-down menu
    public ScriptATRDEPlugin(String scriptName) {
        this.scriptName = scriptName;
    }

    /**
     * Method to set the resource record and component record. Only the resource record
     * is used in this class.
     *
     * @param parentRecord
     * @param resourcesCommon
     */
    public void setModel(Resources parentRecord, ResourcesCommon resourcesCommon) {
        this.parentRecord = parentRecord;
        this.resourcesCommon = resourcesCommon;
    }

    /**
     * Method to specify if this plugin display an dialog or just
     * executes a task
     *
     * @return true if it displays a dialog, false otherwise
     */
    public boolean hasDialog() {
        return true;
    }

    /**
     * Method that called to execute task in which no dialogs are displayed.
     * Its not used here
     */
    public void doTask() { }

    /**
     * actually execute the code that cleans up the barcodes. It execute is a thread
     *
     * @param owner The dialog from which this was opened
     * @param title The title of the dialog. not used here
     */
    public void showPlugin(final JDialog owner, String title) {
        try {
            BeanShellWorkspace beanShellWorkspace = new BeanShellWorkspace(owner);

            // see what level the user selected
            if (resourcesCommon instanceof ResourcesComponents) {
                ResourcesComponents component = (ResourcesComponents) resourcesCommon;
                beanShellWorkspace.setResourceComponent(component);
                beanShellWorkspace.setResource(parentRecord);
            } else {
                beanShellWorkspace.setResource(parentRecord);
            }

            // see if to run a particular script, or not
            if(scriptName != null) {
                title = "BeanShell Command (" + scriptName + ")";
                beanShellWorkspace.hideFunctionality(); // hide functionality we don't need
                beanShellWorkspace.setTitle(title);
                beanShellWorkspace.setVisible(true);
                beanShellWorkspace.loadScript(scriptName);
            } else { // just set the main window visible
                beanShellWorkspace.setVisible(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
