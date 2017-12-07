/*
 * Created by JFormDesigner on Wed Jul 06 20:07:37 EDT 2011
 */

package org.archiviststoolkit.plugin;

import org.archiviststoolkit.plugin.beanshell.BeanShellWorkspace;
import org.archiviststoolkit.plugin.dbdialog.RemoteDBConnectDialog;
import org.archiviststoolkit.model.Resources;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 * @author Nathan Stevens
 */
public class ScriptATFrame extends JFrame {
    private RemoteDBConnectDialog dbDialog;

    public ScriptATFrame() {
        initComponents();
    }

    private void okButtonActionPerformed() {
        System.exit(0);
    }

    /**
     * Method to open the database connection dialog
     */
    private void connectButtonActionPerformed() {
        if (dbDialog == null) {
            dbDialog = new RemoteDBConnectDialog(this);
            dbDialog.pack();
        }

        dbDialog.setVisible(true);        
    }

    /**
     * Method to show the Beanshell Workspace Window
     */
    private void showScriptConsoleButtonActionPerformed() {
        BeanShellWorkspace beanShellWorkspace = new BeanShellWorkspace(this);

        try {
            if(dbDialog != null) {
                Resources fullRecord = dbDialog.getResourceRecord();
                beanShellWorkspace.setResource(fullRecord);
                beanShellWorkspace.setDbDialog(dbDialog);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        beanShellWorkspace.setVisible(true);
    }

    /**
     * Generic method to test some functionality
     *
     * @param e
     */
    private void testButtonActionPerformed(ActionEvent e) {
        // todo implement any code that needs testing
    }

    /**
     * Method to open
     */
    private void dbCopyButtonActionPerformed() {

    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner non-commercial license
        dialogPane = new JPanel();
        contentPanel = new JPanel();
        showScriptConsoleButton = new JButton();
        testButton = new JButton();
        buttonBar = new JPanel();
        connectButton = new JButton();
        okButton = new JButton();

        //======== this ========
        setTitle("Script AT 0.5");
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(new EmptyBorder(12, 12, 12, 12));
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setLayout(new GridBagLayout());
                ((GridBagLayout)contentPanel.getLayout()).columnWidths = new int[] {0, 0};
                ((GridBagLayout)contentPanel.getLayout()).rowHeights = new int[] {0, 0, 0};
                ((GridBagLayout)contentPanel.getLayout()).columnWeights = new double[] {0.0, 1.0E-4};
                ((GridBagLayout)contentPanel.getLayout()).rowWeights = new double[] {0.0, 0.0, 1.0E-4};

                //---- showScriptConsoleButton ----
                showScriptConsoleButton.setText("Show Beanshell Workspace");
                showScriptConsoleButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        showScriptConsoleButtonActionPerformed();
                    }
                });
                contentPanel.add(showScriptConsoleButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 5, 0), 0, 0));

                //---- testButton ----
                testButton.setText("Test");
                testButton.setVisible(false);
                testButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        testButtonActionPerformed(e);
                    }
                });
                contentPanel.add(testButton, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 0), 0, 0));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(new EmptyBorder(12, 0, 0, 0));
                buttonBar.setLayout(new GridBagLayout());
                ((GridBagLayout)buttonBar.getLayout()).columnWidths = new int[] {0, 0, 80};
                ((GridBagLayout)buttonBar.getLayout()).columnWeights = new double[] {0.0, 1.0, 0.0};

                //---- connectButton ----
                connectButton.setText("Connect to Database");
                connectButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        connectButtonActionPerformed();
                    }
                });
                buttonBar.add(connectButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 5), 0, 0));

                //---- okButton ----
                okButton.setText("OK");
                okButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        okButtonActionPerformed();
                    }
                });
                buttonBar.add(okButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
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
    private JButton showScriptConsoleButton;
    private JButton testButton;
    private JPanel buttonBar;
    private JButton connectButton;
    private JButton okButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
