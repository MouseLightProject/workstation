package org.janelia.workstation.gui.large_volume_viewer.nb_action;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.*;

import org.janelia.workstation.integration.util.FrameworkAccess;
import org.janelia.workstation.gui.large_volume_viewer.action.CreateTiledMicroscopeSampleAction;
import org.jdesktop.swingx.VerticalLayout;

public class NewTiledMicroscopeSampleDialog extends JDialog {
	
	JTextField nameTextField = new JTextField(40);
	JTextField pathToRenderFolderTextField = new JTextField(40);

	public NewTiledMicroscopeSampleDialog(JFrame owner, String title, boolean modal) {
		super(owner, title, modal);
		try {
			jbInit();
		} catch (Exception e) {
			e.printStackTrace();
		}
		setUpValues();
		SwingUtilities.updateComponentTreeUI(this);
		pack();
		setLocationRelativeTo(FrameworkAccess.getMainFrame());
		setVisible(true);
	}

	private void setUpValues() {
		nameTextField.setText("");
		pathToRenderFolderTextField.setText("");
	}

	private void jbInit() throws Exception {
		setTitle("Add Tiled Microscope Sample");
		setSize(400, 150);

		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new VerticalLayout(5));

		JPanel attrPanel = new JPanel();
		attrPanel.setLayout(new GridBagLayout());

		JLabel sampleNameLabel = new JLabel("Sample Name:");
		sampleNameLabel.setLabelFor(nameTextField);
		GridBagConstraints c = new GridBagConstraints();
		c.ipadx = 5;
		c.gridx = 0;
		c.gridy = 0;
		attrPanel.add(sampleNameLabel, c);
		c.gridx = 1;
		nameTextField.setText("Sample Name Here");
		attrPanel.add(nameTextField, c);

		// Figure out the user path preference
		c.gridx = 0;
		c.gridy = 1;
		JLabel pathLabel = new JLabel("Path To Render Folder:");
		attrPanel.add(pathLabel, c);
		c.gridx = 1;
		pathToRenderFolderTextField.setText("");
		attrPanel.add(pathToRenderFolderTextField, c);

		mainPanel.add(attrPanel);
		add(mainPanel, BorderLayout.CENTER);

		JButton okButton = new JButton("Add Sample");
		okButton.addActionListener(e -> {
                if (nameTextField.getText().isEmpty() || pathToRenderFolderTextField.getText().isEmpty()) {
                    JOptionPane.showMessageDialog(FrameworkAccess.getMainFrame(),
                        "You must specify both a sample name and location!",
                        "Missing values",
                        JOptionPane.WARNING_MESSAGE);
                    return;
                }
			    Action action = new CreateTiledMicroscopeSampleAction(nameTextField.getText(), pathToRenderFolderTextField.getText());
			    action.actionPerformed(e);
				NewTiledMicroscopeSampleDialog.this.dispose();
		});

		JButton cancelButton = new JButton("Cancel");
		cancelButton.setToolTipText("Cancel and close this dialog");
		cancelButton.addActionListener(e -> setVisible(false));

		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
		buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
		buttonPane.add(Box.createHorizontalGlue());
		buttonPane.add(okButton);
		buttonPane.add(cancelButton);

		add(buttonPane, BorderLayout.SOUTH);
	}
	
}