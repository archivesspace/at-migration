/*
 * Created by JFormDesigner on Thu Jan 03 10:26:44 EST 2013
 */

package org.archiviststoolkit.plugin.utils;

import java.awt.event.*;
import com.jgoodies.forms.factories.*;
import com.jgoodies.forms.layout.*;
import org.archiviststoolkit.plugin.utils.aspace.ASpaceClient;
import org.archiviststoolkit.plugin.utils.aspace.RecordTestServletClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.json.JSONObject;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 * Simple dialog for viewing or editing source code with syntax highlighting
 *
 * @author Nathan Stevens
 */
public class CodeViewerDialog extends JDialog {
    private RSyntaxTextArea textArea;
    private boolean editable = false;

    private RecordTestServletClient recordTestServletClient;
    private boolean testMultipleRecords = false;

    private ASpaceClient aspaceClient;

    /**
     * Constructor which code is past in
     *
     * @param owner
     * @param code
     * @param syntaxStyle
     */
    public CodeViewerDialog(Frame owner, String syntaxStyle,  String code, boolean editable, boolean allowRecordTest) {
        super(owner);
        initComponents();

        this.editable = editable;

        // add the syntax area now
        textArea = new RSyntaxTextArea(30, 100);
        textArea.setSyntaxEditingStyle(syntaxStyle);
        textArea.setCodeFoldingEnabled(true);
        textArea.setAntiAliasingEnabled(true);
        textArea.setEditable(editable);
        textArea.setText(code);

        RTextScrollPane sp = new RTextScrollPane(textArea);
        sp.setFoldIndicatorEnabled(true);

        contentPanel.add(sp, BorderLayout.CENTER);

        // Make the components for unit testing a json using the RecordTestServlet
        if(allowRecordTest) {
            scrollPane1.setVisible(true);
            recordTestPanel.setVisible(true);
            testHostUrlTextField.setText(RecordTestServletClient.DEFAULT_URL);

            recordTestServletClient = new RecordTestServletClient();
        }

        // make sure we open this window somewhere that make sense
        setLocation(owner.getLocationOnScreen());
    }



    /**
     * Method to set the script that is displayed
     *
     * @param script
     */
    public void setCurrentScript(String script) {
        textArea.setText(script);
    }

    /**
     * Method to return the current script, for example after it been edited
     *
     * @return The script
     */
    public String getCurrentScript() {
        return textArea.getText();
    }

    /**
     * Close the dialog when the window is closed
     */
    private void okButtonActionPerformed() {
        setVisible(false);

        if(!editable) {
            dispose();
        }
    }

    /**
     * Method to store a json record to test against
     */
    private void storeButtonActionPerformed() {
        String testHost = testHostUrlTextField.getText();
        String jsonText = textArea.getText();

        try {
            recordTestServletClient.setHost(testHost);

            String message = "";

            // see whether we note posting to the aspace backend by seeing if we have http
            if(testHost.toLowerCase().contains("http")) {
                if(!testMultipleRecords) {
                    message = recordTestServletClient.storeRecord(jsonText);
                } else {
                    message = recordTestServletClient.storeMultipleRecords(jsonText);
                }
            } else {
                message = aspaceClient.post(testHost, jsonText, null, "Test Record");
            }

            messageTextArea.append(message + "\n");
        } catch(Exception e) {
            if(e instanceof JSONException) {
                messageTextArea.setText("Invalid JSON Record");
            } else {
                e.printStackTrace();
            }
        }
    }

    /**
     * Method to test a json record against a stored record
     */
    private void testButtonActionPerformed() {
        String testHost = testHostUrlTextField.getText();
        String jsonText = textArea.getText();

        try {
            recordTestServletClient.setHost(testHost);

            String message = "";

            if(!testMultipleRecords) {
                message = recordTestServletClient.testRecord(jsonText);
            } else {
                message = recordTestServletClient.testMultipleRecords(jsonText);
            }

            messageTextArea.append(message + "\n");
        } catch(Exception e) {
            if(e instanceof JSONException) {
                messageTextArea.setText("Invalid JSON Record");
            } else {
                e.printStackTrace();
            }
        }
    }

    /**
     * Method to delete a record on the ASpace backend.  This is really for development purposes
     */
    private void deleteButtonActionPerformed() {
        try {
            JSONObject jsonObject = new JSONObject(textArea.getText());
            String uri = jsonObject.getString("uri");
            String message = aspaceClient.deleteRecord(uri);
            messageTextArea.setText(message);
        } catch (JSONException e) {
            messageTextArea.setText("Invalid JSON record");
        } catch (Exception e) {
            messageTextArea.setText("Error deleting JSON record");
            e.printStackTrace();
        }
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner non-commercial license
        dialogPane = new JPanel();
        contentPanel = new JPanel();
        scrollPane1 = new JScrollPane();
        messageTextArea = new JTextArea();
        buttonBar = new JPanel();
        recordTestPanel = new JPanel();
        storeButton = new JButton();
        testButton = new JButton();
        label1 = new JLabel();
        testHostUrlTextField = new JTextField();
        deleteButton = new JButton();
        okButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("Code Viewer");
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

                    //---- messageTextArea ----
                    messageTextArea.setRows(4);
                    messageTextArea.setEditable(false);
                    scrollPane1.setViewportView(messageTextArea);
                }
                contentPanel.add(scrollPane1, BorderLayout.SOUTH);
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(new EmptyBorder(12, 0, 0, 0));
                buttonBar.setLayout(new GridBagLayout());
                ((GridBagLayout)buttonBar.getLayout()).columnWidths = new int[] {0, 80};
                ((GridBagLayout)buttonBar.getLayout()).columnWeights = new double[] {1.0, 0.0};

                //======== recordTestPanel ========
                {
                    recordTestPanel.setLayout(new FormLayout(
                        new ColumnSpec[] {
                            FormFactory.DEFAULT_COLSPEC,
                            FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                            FormFactory.DEFAULT_COLSPEC,
                            FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                            FormFactory.DEFAULT_COLSPEC,
                            FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                            new ColumnSpec(ColumnSpec.FILL, Sizes.DEFAULT, FormSpec.DEFAULT_GROW),
                            FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                            FormFactory.DEFAULT_COLSPEC
                        },
                        RowSpec.decodeSpecs("default")));

                    //---- storeButton ----
                    storeButton.setText("Store");
                    storeButton.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            storeButtonActionPerformed();
                        }
                    });
                    recordTestPanel.add(storeButton, cc.xy(1, 1));

                    //---- testButton ----
                    testButton.setText("Test");
                    testButton.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            testButtonActionPerformed();
                        }
                    });
                    recordTestPanel.add(testButton, cc.xy(3, 1));

                    //---- label1 ----
                    label1.setText("Url");
                    recordTestPanel.add(label1, cc.xy(5, 1));
                    recordTestPanel.add(testHostUrlTextField, cc.xy(7, 1));

                    //---- deleteButton ----
                    deleteButton.setText("Delete");
                    deleteButton.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            deleteButtonActionPerformed();
                        }
                    });
                    recordTestPanel.add(deleteButton, cc.xy(9, 1));
                }
                buttonBar.add(recordTestPanel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 5), 0, 0));

                //---- okButton ----
                okButton.setText("OK");
                okButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        okButtonActionPerformed();
                    }
                });
                buttonBar.add(okButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
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
    private JTextArea messageTextArea;
    private JPanel buttonBar;
    private JPanel recordTestPanel;
    private JButton storeButton;
    private JButton testButton;
    private JLabel label1;
    private JTextField testHostUrlTextField;
    private JButton deleteButton;
    private JButton okButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables

    /**
     * Method to set the ASpace client for running test on multiple records
     *
     */
    public void setUpMultipleRecordTest() {
        testMultipleRecords = true;

        // now create a json object which has a list of record URI for testing
        // We could just use a string, but first creating a json array makes
        // formatting the string easier and of course assures we are creating
        // a valid json array.
        JSONArray recordsJA = new JSONArray();

        recordsJA.put("/repositories/2");
        recordsJA.put("/users/4");
        recordsJA.put("/subjects/1");
        recordsJA.put("/agents/families/1");
        recordsJA.put("/agents/people/1");
        recordsJA.put("/agents/corporate_entities/1");
        recordsJA.put("/repositories/2/accessions/1");
        recordsJA.put("/repositories/2/resources/1");
        recordsJA.put("/repositories/2/archival_objects/1");

        try {
            textArea.setText(recordsJA.toString(2));
        } catch(Exception e) {
            textArea.setText("Problem ");
        }
    }

    /**
     * Method to setup the ASpace client
     *
     * @param aspaceClient
     */
    public void setASpaceClient(ASpaceClient aspaceClient) {
        this.aspaceClient = aspaceClient;
        if(recordTestServletClient != null) {
            recordTestServletClient.setASpaceClient(aspaceClient);
        }
    }
}
