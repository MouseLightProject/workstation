package org.janelia.it.workstation.gui.browser.gui.dialogs;

import net.miginfocom.swing.MigLayout;
import org.janelia.it.jacs.model.domain.enums.SampleImageType;
import org.janelia.it.jacs.model.domain.sample.DataSet;
import org.janelia.it.jacs.model.entity.cv.NamedEnum;
import org.janelia.it.jacs.model.entity.cv.PipelineProcess;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.gui.dialogs.ModalDialog;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.shared.util.Utils;
import org.janelia.it.workstation.shared.workers.SimpleWorker;
//import org.janelia.it.workstation.gui.browser.gui.DataSetListDialog;
import org.janelia.it.workstation.gui.browser.api.*;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * A dialog for viewing the list of accessible data sets, editing them, and
 * adding new ones.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DataSetDialog extends ModalDialog {

    private static final Font separatorFont = new Font("Sans Serif", Font.BOLD, 12);

    private static final String SLIDE_CODE_PATTERN = "{Slide Code}";
    private static final String DEFAULT_SAMPLE_NAME_PATTERN = "{Line}-" + SLIDE_CODE_PATTERN;

    private final DataSetListDialog parentDialog;

    private JPanel attrPanel;
    private JTextField nameInput;
    private JTextField identifierInput;
    private JTextField sampleNamePatternInput;
    private JComboBox<SampleImageType> sampleImageInput;
    private JCheckBox sageSyncCheckbox;
    private HashMap<String, JCheckBox> processCheckboxes = new LinkedHashMap<>();
    public final String NAME_COL = "Name";
    public final String PIPELINE_PROCESS_COL = "Pipeline Process";
    public final String SAMPLE_NAME_COL = "Sample Name Pattern";
    public final String SAGE_SYNC_COL = "SAGE Sync";

    private DataSet dataSet;

    public DataSetDialog(DataSetListDialog parentDialog) {

        super(parentDialog);
        this.parentDialog = parentDialog;

        setTitle("Data Set Definition");

        attrPanel = new JPanel(new MigLayout("wrap 2, ins 20"));

        add(attrPanel, BorderLayout.CENTER);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setToolTipText("Close without saving changes");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });

        JButton okButton = new JButton("OK");
        okButton.setToolTipText("Close and save changes");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveAndClose();
            }
        });

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(cancelButton);
        buttonPane.add(okButton);

        add(buttonPane, BorderLayout.SOUTH);
    }

    public void showForNewDataSet() {
        showForDataSet(null);
    }

    public void addSeparator(JPanel panel, String text, boolean first) {
        JLabel label = new JLabel(text);
        label.setFont(separatorFont);
        panel.add(label, "split 2, span" + (first ? "" : ", gaptop 10lp"));
        panel.add(new JSeparator(SwingConstants.HORIZONTAL), "growx, wrap, gaptop 10lp");
    }

    private void updateDataSetIdentifier() {
        if (dataSet == null) {
            identifierInput.setText(createDenormIdentifierFromName(SessionMgr.getSubjectKey(), nameInput.getText()));
        }
    }

    public void showForDataSet(final DataSet dataSet) {

        this.dataSet = dataSet;

        attrPanel.removeAll();

        addSeparator(attrPanel, "Data Set Attributes", true);

        final JLabel nameLabel = new JLabel("Data Set Name: ");
        nameInput = new JTextField(40);

        nameInput.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                updateDataSetIdentifier();
            }

            public void removeUpdate(DocumentEvent e) {
                updateDataSetIdentifier();
            }

            public void insertUpdate(DocumentEvent e) {
                updateDataSetIdentifier();
            }
        });

        nameLabel.setLabelFor(nameInput);
        attrPanel.add(nameLabel, "gap para");
        attrPanel.add(nameInput);

        final JLabel identifierLabel = new JLabel("Data Set Identifier: ");
        identifierInput = new JTextField(40);
        identifierInput.setEditable(false);
        identifierLabel.setLabelFor(identifierInput);
        attrPanel.add(identifierLabel, "gap para");
        attrPanel.add(identifierInput);

        final JLabel sampleNamePatternLabel = new JLabel("Sample Name Pattern: ");
        sampleNamePatternInput = new JTextField(40);
        sampleNamePatternInput.setText(DEFAULT_SAMPLE_NAME_PATTERN);
        sampleNamePatternLabel.setLabelFor(sampleNamePatternInput);
        attrPanel.add(sampleNamePatternLabel, "gap para");
        attrPanel.add(sampleNamePatternInput);

        final JLabel sampleImageLabel = new JLabel("Sample Image: ");
        sampleImageInput = new JComboBox<>(SampleImageType.values());
        sampleImageLabel.setLabelFor(sampleImageInput);
        attrPanel.add(sampleImageLabel, "gap para");
        attrPanel.add(sampleImageInput);

        sageSyncCheckbox = new JCheckBox("Synchronize images from SAGE");
        attrPanel.add(sageSyncCheckbox, "gap para, span 2");

        JPanel pipelinesPanel = new JPanel();
        pipelinesPanel.setLayout(new BoxLayout(pipelinesPanel, BoxLayout.PAGE_AXIS));
        addCheckboxes(PipelineProcess.values(), processCheckboxes, pipelinesPanel);

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(pipelinesPanel);
        scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));

        addSeparator(attrPanel, "Pipelines", false);
        attrPanel.add(scrollPane, "span 2, growx");

        if (dataSet != null) {

            nameInput.setText(dataSet.getName());

            identifierInput.setText(dataSet.getIdentifier());
            sampleNamePatternInput.setText(dataSet.getSampleNamePattern());

            SampleImageType sampleImageType = dataSet.getSampleImageType();
            if (sampleImageType != null) {
                sampleImageInput.setSelectedItem(sampleImageType);
            }
            if (dataSet.getSageSync() != null) {
                sageSyncCheckbox.setSelected(dataSet.getSageSync().booleanValue());
            }
            applyCheckboxValues(processCheckboxes, dataSet.getPipelineProcesses().get(0));
        } else {
            nameInput.setText("");
            applyCheckboxValues(processCheckboxes, PipelineProcess.FlyLightUnaligned.toString());
        }

        packAndShow();
    }

    private void saveAndClose() {

        Utils.setWaitingCursor(DataSetDialog.this);

        final String sampleNamePattern = sampleNamePatternInput.getText();
        if (!sampleNamePattern.contains(SLIDE_CODE_PATTERN)) {
            JOptionPane.showMessageDialog(this,
                    "Sample name pattern must contain the unique identifier \"" + SLIDE_CODE_PATTERN + "\"",
                    "Invalid Sample Name Pattern",
                    JOptionPane.ERROR_MESSAGE);
            sampleNamePatternInput.requestFocus();
            return;
        }

        final String sampleImageType = ((SampleImageType) sampleImageInput.getSelectedItem()).name();

        SimpleWorker worker = new SimpleWorker() {

            @Override
            protected void doStuff() throws Exception {

                if (dataSet == null) {
                    dataSet = new DataSet();
                    dataSet.setName(nameInput.getText());
                    dataSet.setIdentifier(identifierInput.getText());
                } else {
                    dataSet.setName(nameInput.getText());
                }

                dataSet.setSampleNamePattern(sampleNamePattern);
                dataSet.setSampleImageType(SampleImageType.valueOf(sampleImageType));
                java.util.List<String> pipelineProcesses = new ArrayList<>();
                pipelineProcesses.add(getCheckboxValues(processCheckboxes));
                dataSet.setPipelineProcesses(pipelineProcesses);
                dataSet.setSageSync(new Boolean(sageSyncCheckbox.isSelected()));
                DomainModel model = DomainMgr.getDomainMgr().getModel();
                model.save(dataSet);
            }

            @Override
            protected void hadSuccess() {
                parentDialog.refresh();
                Utils.setDefaultCursor(DataSetDialog.this);
                setVisible(false);
            }

            @Override
            protected void hadError(Throwable error) {
                SessionMgr.getSessionMgr().handleException(error);
                Utils.setDefaultCursor(DataSetDialog.this);
                setVisible(false);
            }
        };

        worker.execute();
    }

    private void addCheckboxes(final Object[] choices, final HashMap<String, JCheckBox> checkboxes, final JPanel panel) {
        for (Object choice : choices) {
            NamedEnum namedEnum = ((NamedEnum) choice);
            JCheckBox checkBox = new JCheckBox(namedEnum.getName());
            checkboxes.put(namedEnum.toString(), checkBox);
            panel.add(checkBox);
        }
    }

    private void applyCheckboxValues(final HashMap<String, JCheckBox> checkboxes, String selected) {

        for (JCheckBox checkbox : checkboxes.values()) {
            checkbox.setSelected(false);
        }

        if (StringUtils.isEmpty(selected)) {
            return;
        }

        for (String value : selected.split(",")) {
            JCheckBox checkbox = checkboxes.get(value);
            if (checkbox != null) {
                checkbox.setSelected(true);
            }
        }
    }

    private String getCheckboxValues(final HashMap<String, JCheckBox> checkboxes) {

        StringBuilder sb = new StringBuilder();
        for (String key : checkboxes.keySet()) {
            JCheckBox checkbox = checkboxes.get(key);
            if (checkbox != null && checkbox.isSelected()) {
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(key);
            }
        }

        return sb.toString();
    }

    private String createDenormIdentifierFromName (String username, String name) {
        if (username.contains(":")) username = username.split(":")[1];
        return username+"_"+name.toLowerCase().replaceAll("\\W+", "_");
    }
}
