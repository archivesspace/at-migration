/*
 * Created by JFormDesigner on Tue Jul 12 15:33:50 EDT 2011
 */

package org.archiviststoolkit.plugin.beanshell;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import com.jgoodies.forms.factories.*;
import com.jgoodies.forms.layout.*;

/**
 * @author Nathan Stevens
 */
public class ScriptInputDialog extends JDialog {
    private JComponent[] inputFields;
    private boolean cancelled = false; // was the cancel button pressed
    
    /**
     * The main constructor
     *
     * @param owner
     * @param title
     * @param options
     */
    public ScriptInputDialog(Dialog owner, String title, String instruction, String[] options) {
        super(owner, title, true);
        initComponents();
        initInputFields(instruction, options);
    }

    public ScriptInputDialog(Frame owner) {
        super(owner);
        initComponents();
    }

    public ScriptInputDialog(Dialog owner) {
        super(owner);
        initComponents();
    }

    /**
     * Method to dynamically generate text boxes and labels
     * @param options
     */
    private void initInputFields(String instruction, String[] options) {
        // set the instruction
        instructionLabel.setText(instruction);

        inputFields = new JComponent[options.length];

        // now for every option add an input text field with the name
        JLabel label;

        for(int i = 0; i < options.length; i++) {
            String option = options[i];

            // check to see if we shouldn't create a jcombobox
            if(option.contains("::")) {
                // first element in the array
                String[] sa1 = option.split("::");
                label = new JLabel(sa1[0]);

                // Now create the combo box and add the choices
                JComboBox comboBox = new JComboBox();

                for(int j = 1; j < sa1.length; j++) {
                    comboBox.addItem(sa1[j]);
                }

                inputFields[i] = comboBox;
            } else if(option.contains(":CB")) { // create a check box
                // first element in the array
                String[] sa1 = option.split(":");
                label = new JLabel(sa1[0]);

                // Now create the combo box and add the choices
                JCheckBox checkBox = new JCheckBox();
                inputFields[i] = checkBox;
            } else { // create a plain text field
                label = new JLabel(option);
                inputFields[i] = new JTextField(30);
            }

            panel2.add(label);
            panel3.add(inputFields[i]);
        }
    }

    /**
     * Method to iterate through the input fields to get
     * the values that were returned.
     * 
     * @return
     */
    public String[] getUserInput() {
        String[] inputs = new String[inputFields.length];

        for(int i = 0; i < inputs.length; i++) {
            JComponent inputField  = inputFields[i];

            if(inputField instanceof JTextField) {
                inputs[i] = ((JTextField)inputField).getText();
            } else if(inputField instanceof JCheckBox) {
                boolean selected = ((JCheckBox)inputField).isSelected();
                if(selected) {
                    inputs[i] = "yes";
                } else {
                    inputs[i] = "no";
                }
            } else { // must be a text field
                inputs[i] = ((JComboBox)inputField).getSelectedItem().toString();
            }
        }

        return inputs;
    }

    private void okButtonActionPerformed() {
        setVisible(false);
        cancelled = false;
    }

    private void cancelButtonActionPerformed() {
        setVisible(false);
        cancelled = true;
    }

    /**Method to see if the cancelled button is pressed
     *
     * @return
     */
    public boolean isCancelled() {
        return cancelled;
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        // Generated using JFormDesigner non-commercial license
        dialogPane = new JPanel();
        contentPanel = new JPanel();
        instructionLabel = new JLabel();
        panel1 = new JPanel();
        panel2 = new JPanel();
        panel3 = new JPanel();
        buttonBar = new JPanel();
        okButton = new JButton();
        cancelButton = new JButton();
        CellConstraints cc = new CellConstraints();

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

                //---- instructionLabel ----
                instructionLabel.setText("Enter Parameters Below");
                contentPanel.add(instructionLabel, BorderLayout.NORTH);

                //======== panel1 ========
                {
                    panel1.setBorder(new EmptyBorder(5, 0, 0, 0));
                    panel1.setLayout(new FormLayout(
                        "default, default:grow",
                        "fill:default:grow"));

                    //======== panel2 ========
                    {
                        panel2.setBorder(new EmptyBorder(0, 0, 0, 5));
                        panel2.setLayout(new GridLayout(0, 1, 5, 5));
                    }
                    panel1.add(panel2, cc.xy(1, 1));

                    //======== panel3 ========
                    {
                        panel3.setBorder(new EmptyBorder(0, 5, 0, 0));
                        panel3.setLayout(new GridLayout(0, 1, 5, 5));
                    }
                    panel1.add(panel3, cc.xy(2, 1));
                }
                contentPanel.add(panel1, BorderLayout.CENTER);
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(new EmptyBorder(12, 0, 0, 0));
                buttonBar.setLayout(new GridBagLayout());
                ((GridBagLayout)buttonBar.getLayout()).columnWidths = new int[] {0, 85, 80};
                ((GridBagLayout)buttonBar.getLayout()).columnWeights = new double[] {1.0, 0.0, 0.0};

                //---- okButton ----
                okButton.setText("OK");
                okButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        okButtonActionPerformed();
                    }
                });
                buttonBar.add(okButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 5), 0, 0));

                //---- cancelButton ----
                cancelButton.setText("Cancel");
                cancelButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        cancelButtonActionPerformed();
                    }
                });
                buttonBar.add(cancelButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
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
    private JLabel instructionLabel;
    private JPanel panel1;
    private JPanel panel2;
    private JPanel panel3;
    private JPanel buttonBar;
    private JButton okButton;
    private JButton cancelButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
