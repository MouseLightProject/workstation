package org.janelia.it.workstation.gui.large_volume_viewer.action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JOptionPane;

import org.janelia.it.jacs.model.domain.tiledMicroscope.TmSample;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmDirectedSession;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.browser.workers.IndeterminateProgressMonitor;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.it.workstation.gui.large_volume_viewer.api.TiledMicroscopeDomainMgr;
import org.janelia.it.workstation.gui.large_volume_viewer.dialogs.EditWorkspaceNameDialog;
import org.janelia.it.workstation.gui.large_volume_viewer.top_component.LargeVolumeViewerTopComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This action creates a SATA annotation session on top of an existing TM Sample. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public final class NewDirectedSessionActionListener implements ActionListener {

    private static final Logger log = LoggerFactory.getLogger(NewDirectedSessionActionListener.class);
    
    private TmSample sample;

    public NewDirectedSessionActionListener(TmSample sample) {
        this.sample = sample;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        ActivityLogHelper.logUserAction("NewDirectedSessionActionListener.actionPerformed");
        
        if (sample==null) {
            JOptionPane.showMessageDialog(ConsoleApp.getMainFrame(),
                    "No sample selected.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        EditWorkspaceNameDialog dialog = new EditWorkspaceNameDialog("Session Name", "new session");
        final String sessionName = dialog.showForSample(sample);
        
        if (sessionName==null) {
            log.info("Aborting session creation: no valid name was provided by the user");
            return;
        }

        final Long finalSampleId = sample.getId();
        SimpleWorker creator = new SimpleWorker() {
            
            private TmDirectedSession session;
            
            @Override
            protected void doStuff() throws Exception {
                log.info("Creating new session with name '{}' for {}",sessionName,finalSampleId);
                // now we can create the workspace
                this.session = TiledMicroscopeDomainMgr.getDomainMgr().createSession(finalSampleId, sessionName);
                log.info("Created session with id={}",session.getId());
            }

            @Override
            protected void hadSuccess() {
                LargeVolumeViewerTopComponent.getInstance().openLargeVolumeViewer(session);
            }

            @Override
            protected void hadError(Throwable error) {
                ConsoleApp.handleException(error);
            }
        };
        creator.setProgressMonitor(new IndeterminateProgressMonitor(ConsoleApp.getMainFrame(), "Creating new session...", ""));
        creator.execute();
    }
}