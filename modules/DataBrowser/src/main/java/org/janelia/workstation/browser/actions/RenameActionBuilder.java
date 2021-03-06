package org.janelia.workstation.browser.actions;

import javax.swing.Action;
import javax.swing.JOptionPane;

import org.apache.commons.lang3.StringUtils;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.gui.colordepth.ColorDepthMask;
import org.janelia.model.domain.gui.colordepth.ColorDepthSearch;
import org.janelia.model.domain.gui.search.Filter;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.model.domain.workspace.TreeNode;
import org.janelia.model.domain.workspace.Workspace;
import org.janelia.workstation.common.actions.DomainObjectNodeAction;
import org.janelia.workstation.common.gui.util.DomainUIUtils;
import org.janelia.workstation.core.actions.ViewerContext;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.integration.spi.domain.ContextualActionBuilder;
import org.janelia.workstation.integration.spi.domain.ContextualActionUtils;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.util.lookup.ServiceProvider;

/**
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ServiceProvider(service = ContextualActionBuilder.class, position = 120)
public class RenameActionBuilder implements ContextualActionBuilder {

    private static final RenameAction action = new RenameAction();

    private static final String ACTION_NAME = "Rename";

    @Override
    public boolean isCompatible(Object obj) {
        return obj instanceof DomainObject && !(obj instanceof Workspace);
    }

    @Override
    public Action getAction(Object obj) {
        return action;
    }

    @Override
    public Action getNodeAction(Object obj) {
        return action;
    }

    public static class RenameAction extends DomainObjectNodeAction {

        private DomainObject domainObject;

        @Override
        public String getName() {
            return ACTION_NAME;
        }

        @Override
        public void setViewerContext(ViewerContext viewerContext) {
            this.domainObject = DomainUIUtils.getLastSelectedDomainObject(viewerContext);

            // Reset values
            ContextualActionUtils.setVisible(this, false);
            ContextualActionUtils.setEnabled(this, true);

            if (domainObject != null) {
                boolean visible = !viewerContext.isMultiple() && userCanRename(domainObject);
                ContextualActionUtils.setVisible(this, visible);
                ContextualActionUtils.setEnabled(this, ClientDomainUtils.hasWriteAccess(domainObject));
            }
        }

        @Override
        protected void executeAction() {
            if (domainObject == null) {
                throw new IllegalStateException("Rename action invoked with null parameter");
            }
            renameObject(domainObject);
        }
    }

    private static boolean userCanRename(DomainObject domainObject) {
        // TODO: this should be implemented as DomainObjectHandler.supportsRename
        return (domainObject instanceof TreeNode
                || domainObject instanceof TmWorkspace
                || domainObject instanceof TmSample
                || domainObject instanceof ColorDepthMask
                || domainObject instanceof ColorDepthSearch
                || domainObject instanceof Filter);
    }

    private static void renameObject(DomainObject domainObject) {
        String newName = (String) JOptionPane.showInputDialog(FrameworkAccess.getMainFrame(), "Name:\n", "Rename "
                + domainObject.getName(), JOptionPane.PLAIN_MESSAGE, null, null, domainObject.getName());
        if (StringUtils.isBlank(newName)) {
            return;
        }
        try {
            DomainMgr.getDomainMgr().getModel().updateProperty(domainObject, "name", newName);
        }
        catch (Exception ex) {
            FrameworkAccess.handleException(ex);
        }
    }

}
