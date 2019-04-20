package org.janelia.workstation.common.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.janelia.workstation.core.actions.ViewerContext;
import org.janelia.workstation.core.actions.ViewerContextReceiver;
import org.janelia.workstation.integration.spi.domain.ContextualActionUtils;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class ViewerContextAction extends AbstractAction implements ViewerContextReceiver {

    private ViewerContext viewerContext;

    @Override
    public void setViewerContext(ViewerContext viewerContext) {
        this.viewerContext = viewerContext;
        ContextualActionUtils.setName(this, getName());
        ContextualActionUtils.setVisible(this, isVisible());
    }

    protected ViewerContext getViewerContext() {
        return viewerContext;
    }

    public abstract String getName();

    protected boolean isVisible() {
        return true;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        executeAction();
    }

    protected void executeAction() {
    }
}