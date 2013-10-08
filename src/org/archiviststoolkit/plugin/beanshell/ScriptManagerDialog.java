/*
 * Created by JFormDesigner on Mon Jul 25 13:52:14 EDT 2011
 */

package org.archiviststoolkit.plugin.beanshell;

import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import com.jgoodies.forms.layout.*;

/**
 * @author Lee Mandell
 */
public class ScriptManagerDialog extends JDialog {
    private ScriptViewerDialog scriptViewDialog = null;

    private BeanShellWorkspace beanShellWorkspace = null;

    private HashMap<String, String> storedScripts = new HashMap<String, String>();

    private String currentScriptName = ""; // the name of the current script loaded

    private String currentScript = ""; // the current script that was loaded

    public ScriptManagerDialog(BeanShellWorkspace beanShellWorkspace) {
        super(beanShellWorkspace);
        initComponents();
        
        this.beanShellWorkspace = beanShellWorkspace;
    }


    /**
     * Method to set the hashmap containing the scripts which are stored
     *
     * @param storedScripts
     */
    public void setStoredScripts(HashMap<String, String> storedScripts) {
        this.storedScripts = storedScripts;
        DefaultListModel model = new DefaultListModel();
        scriptList.setModel(model);

        // iterate through the stored scripts and add the names to JList
        for(String scriptName: storedScripts.keySet()) {
            model.addElement(scriptName);
        }
    }

    /**
     * OK button pressed so just set this hide this window
     */
    private void okButtonActionPerformed() {
        setVisible(false);
    }

    /**
     * Method to show dialog that displays the script
     */
    private void viewButtonActionPerformed() {
        if(scriptViewDialog == null) {
            scriptViewDialog = new ScriptViewerDialog(this, currentScriptName);
        }

        // set the current script and make dialog visible
        scriptViewDialog.setCurrentScript(currentScript);
        scriptViewDialog.setVisible(true);
    }

    /**
     * Method to detect selections in the list that contains names of the
     * stored list
     *
     * @param e
     */
    private void scriptListValueChanged(ListSelectionEvent e) {
        currentScriptName = (String)scriptList.getSelectedValue();

        if (currentScriptName != null) {
            scriptNameTextField.setText(currentScriptName);
            currentScript = storedScripts.get(currentScriptName);
        }
    }

    /**
     * Method to store scripts from the database
     */
    private void storeButtonActionPerformed() {
        beanShellWorkspace.saveScriptsToDatabase();
    }

    /**
     * Method to delete scripts from the database
     */
    private void deleteButtonActionPerformed() {
        if(currentScriptName != null && currentScriptName.length() != 0) {
            storedScripts.remove(currentScriptName);

            // remove from the scriptList JList
            int index = scriptList.getSelectedIndex();
            DefaultListModel model = (DefaultListModel)scriptList.getModel();
            scriptList.clearSelection();
            model.remove(index);

            // now clear the text area
            scriptNameTextField.setText("");
            currentScriptName = "";

            // now save to the database
            beanShellWorkspace.saveScriptsToDatabase();
        }
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner non-commercial license
        dialogPane = new JPanel();
        contentPanel = new JPanel();
        scrollPane1 = new JScrollPane();
        scriptList = new JList();
        panel1 = new JPanel();
        label1 = new JLabel();
        scriptNameTextField = new JTextField();
        buttonBar = new JPanel();
        storeButton = new JButton();
        viewButton = new JButton();
        deleteButton = new JButton();
        okButton = new JButton();

        //======== this ========
        setTitle("Script Manager");
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(new EmptyBorder(12, 12, 12, 12));
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setLayout(new BorderLayout());

                //======== scrollPane1 ========
                {

                    //---- scriptList ----
                    scriptList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                    scriptList.addListSelectionListener(new ListSelectionListener() {
                        public void valueChanged(ListSelectionEvent e) {
                            scriptListValueChanged(e);
                        }
                    });
                    scrollPane1.setViewportView(scriptList);
                }
                contentPanel.add(scrollPane1, BorderLayout.CENTER);

                //======== panel1 ========
                {
                    panel1.setBorder(null);
                    panel1.setLayout(new FlowLayout(FlowLayout.LEFT));

                    //---- label1 ----
                    label1.setText("Script Name ");
                    panel1.add(label1);

                    //---- scriptNameTextField ----
                    scriptNameTextField.setColumns(35);
                    panel1.add(scriptNameTextField);
                }
                contentPanel.add(panel1, BorderLayout.SOUTH);
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(new EmptyBorder(12, 0, 0, 0));
                buttonBar.setLayout(new GridBagLayout());
                ((GridBagLayout)buttonBar.getLayout()).columnWidths = new int[] {0, 0, 0, 0, 85, 80};
                ((GridBagLayout)buttonBar.getLayout()).columnWeights = new double[] {0.0, 0.0, 0.0, 1.0, 0.0, 0.0};

                //---- storeButton ----
                storeButton.setText("Store/Update");
                storeButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        storeButtonActionPerformed();
                    }
                });
                buttonBar.add(storeButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 5), 0, 0));

                //---- viewButton ----
                viewButton.setText("View");
                viewButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        viewButtonActionPerformed();
                    }
                });
                buttonBar.add(viewButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 5), 0, 0));

                //---- deleteButton ----
                deleteButton.setText("Delete");
                deleteButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        deleteButtonActionPerformed();
                    }
                });
                buttonBar.add(deleteButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 5), 0, 0));

                //---- okButton ----
                okButton.setText("OK");
                okButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        okButtonActionPerformed();
                    }
                });
                buttonBar.add(okButton, new GridBagConstraints(5, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 0), 0, 0));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner non-commercial license
    private JPanel dialogPane;
    private JPanel contentPanel;
    private JScrollPane scrollPane1;
    private JList scriptList;
    private JPanel panel1;
    private JLabel label1;
    private JTextField scriptNameTextField;
    private JPanel buttonBar;
    private JButton storeButton;
    private JButton viewButton;
    private JButton deleteButton;
    private JButton okButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
