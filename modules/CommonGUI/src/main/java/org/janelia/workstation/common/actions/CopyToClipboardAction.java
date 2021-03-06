package org.janelia.workstation.common.actions;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.janelia.workstation.core.activity_logging.ActivityLogHelper;
import org.janelia.workstation.integration.util.FrameworkAccess;

/**
 * Action to copy a named string to the clipboard.
 *
 * @deprecated use one of CopyGUIDToClipboardActionBuilder/CopyNameToClipboardActionBuilder
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class CopyToClipboardAction extends AbstractAction {

    private final String value;

    public CopyToClipboardAction(String name, String value) {
        super("Copy "+name+" To Clipboard");
        this.value = value;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        try {
            ActivityLogHelper.logUserAction("CopyToClipboardAction.doAction", value);
            Transferable t = new StringSelection(value);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(t, null);
        }
        catch (Exception e) {
            FrameworkAccess.handleException(e);
        }
    }
}
