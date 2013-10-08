/*
 * Created by JFormDesigner on Sun Jul 10 08:59:38 EDT 2011
 */

package org.archiviststoolkit.plugin.beanshell;

import bsh.Interpreter;
import org.archiviststoolkit.plugin.beanshell.BeanShellWorkspace;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 * @author Nathan Stevens
 */
public class ScriptViewerDialog extends JDialog {
    public ScriptViewerDialog(BeanShellWorkspace beanShellWorkspace, String scriptName) {
        super(beanShellWorkspace, "Script Viewer  - " + scriptName);
        initComponents();
    }

    public ScriptViewerDialog(Dialog owner, String scriptName) {
        super(owner, "Script Viewer - " + scriptName);
        initComponents();
    }

    public ScriptViewerDialog(JFrame owner, String scriptName) {
        super(owner, "Script Viewer - " + scriptName);
        initComponents();
    }

    /**
     * Method to set the script that is displayed
     *
     * @param script
     */
    public void setCurrentScript(String script) {
        textArea1.setText(script);
    }

    /**
     * Method to return the current script, for example after it been edited
     *
     * @return The script
     */
    public String getCurrentScript() {
        return textArea1.getText();
    }

    private void cancelButtonActionPerformed() {
        setVisible(false);
    }



    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner non-commercial license
        dialogPane = new JPanel();
        contentPanel = new JPanel();
        scrollPane1 = new JScrollPane();
        textArea1 = new JTextArea();
        buttonBar = new JPanel();
        runScriptButton = new JButton();
        okButton = new JButton();
        cancelButton = new JButton();

        //======== this ========
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
                    scrollPane1.setViewportView(textArea1);
                }
                contentPanel.add(scrollPane1, BorderLayout.CENTER);
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(new EmptyBorder(12, 0, 0, 0));
                buttonBar.setLayout(new GridBagLayout());
                ((GridBagLayout)buttonBar.getLayout()).columnWidths = new int[] {0, 0, 85, 80};
                ((GridBagLayout)buttonBar.getLayout()).columnWeights = new double[] {0.0, 1.0, 0.0, 0.0};

                //---- runScriptButton ----
                runScriptButton.setText("Run Script");
                runScriptButton.setVisible(false);
                buttonBar.add(runScriptButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 5), 0, 0));

                //---- okButton ----
                okButton.setText("Update");
                okButton.setVisible(false);
                buttonBar.add(okButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 5), 0, 0));

                //---- cancelButton ----
                cancelButton.setText("Close");
                cancelButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        cancelButtonActionPerformed();
                    }
                });
                buttonBar.add(cancelButton, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 0), 0, 0));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        setSize(635, 495);
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // Generated using JFormDesigner non-commercial license
    private JPanel dialogPane;
    private JPanel contentPanel;
    private JScrollPane scrollPane1;
    private JTextArea textArea1;
    private JPanel buttonBar;
    private JButton runScriptButton;
    private JButton okButton;
    private JButton cancelButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
