package org.janelia.workstation.gui.passive_3d;

import org.janelia.workstation.gui.large_volume_viewer.ColorButtonPanel;
import org.janelia.workstation.gui.viewer3d.Mip3d;
import org.janelia.workstation.gui.viewer3d.VolumeModel;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.janelia.workstation.gui.viewer3d.texture.TextureDataI;

import java.beans.PropertyChangeEvent;
import java.util.Collection;
import java.util.ArrayList;
import java.awt.Dimension;
import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.Container;
import java.util.Collections;
import java.util.Comparator;
import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.janelia.workstation.core.workers.IndeterminateNoteProgressMonitor;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.console.viewerapi.model.ImageColorModel;

/**
 * Created with IntelliJ IDEA. User: fosterl Date: 7/31/13 Time: 3:25 PM
 *
 * This popup will give users a snapshot volume. Very simple viewer, relatively
 * speaking.
 */
public class Snapshot3d extends JPanel {

    // Chosing dimensions of same aspect ratio as expected data.
    private static final Dimension WIDGET_SIZE = new Dimension(400, 800);
    private VolumeSource.VolumeAcceptor volumeAcceptor;
    private Collection<Component> locallyAddedComponents = new ArrayList<>();
    private ImageColorModel independentImageColorModel;
    private ImageColorModel sharedImageColorModel;
    private IndeterminateNoteProgressMonitor monitor;
    private String labelText;
    private Snapshot3dControls controls;
    private boolean hasBeenFiltered = false;

    private static Snapshot3d snapshotInstance;

    public static Snapshot3d getInstance() {
        if (snapshotInstance == null) {
            snapshotInstance = new Snapshot3d();
        }
        return snapshotInstance;
    }

    private Snapshot3d() {
        super();
    }

    public void setIndependentImageColorModel(ImageColorModel imageColorModel) {
        this.independentImageColorModel = imageColorModel;
    }

    public void setSharedImageColorModel(ImageColorModel imageColorModel) {
        this.sharedImageColorModel = imageColorModel;
    }

    public void setLoadProgressMonitor(IndeterminateNoteProgressMonitor monitor) {
        this.monitor = monitor;
    }

    public String getLabelText() {
        return this.labelText;
    }
    
    public void setLabelText(String labelText) {
        this.labelText = labelText;
    }

    public IndeterminateNoteProgressMonitor getMonitor() {
        return monitor;
    }

    /**
     * @return the hasBeenFiltered
     */
    public boolean isHasBeenFiltered() {
        return hasBeenFiltered;
    }

    /**
     * @param hasBeenFiltered the hasBeenFiltered to set
     */
    public void setHasBeenFiltered(boolean hasBeenFiltered) {
        this.hasBeenFiltered = hasBeenFiltered;
    }

    /**
     * Launching consists of making a load worker, and then executing that.
     *
     * @param volumeSource for getting the data.
     */
    public void launch(MonitoredVolumeSource volumeSource) {
        setHasBeenFiltered( false );
        SnapshotWorker loadWorker = new SnapshotWorker(volumeSource);
        if (getMonitor() == null) {
            setLoadProgressMonitor(new IndeterminateNoteProgressMonitor(FrameworkAccess.getMainFrame(), "Fetching tiles", volumeSource.getInfo()));
        }
        loadWorker.setProgressMonitor(getMonitor());
        volumeSource.setProgressMonitor(getMonitor());
        loadWorker.execute();
    }
    
    public void reLaunch(Collection<TextureDataI> textureDatas) {
        launch( textureDatas );        
    }

    private void launch(Collection<TextureDataI> textureDatas) {
        cleanup();

        Mip3d mip3d = new Mip3d();
        mip3d.clear();

        Comparator<TextureDataI> comparator = new Comparator<TextureDataI>() {
            @Override
            public int compare(TextureDataI o1, TextureDataI o2) {
                return o1.getHeader().compareTo(o2.getHeader());
            }
        };
        Collections.sort(new ArrayList<>(textureDatas), comparator);

        VolumeModel volumeModel = mip3d.getVolumeModel();
        volumeModel.removeAllListeners();
        volumeModel.resetToDefaults();
        SnapshotVolumeBrick brick = new SnapshotVolumeBrick(volumeModel);
        controls = new Snapshot3dControls(textureDatas, independentImageColorModel, sharedImageColorModel, this, brick);
        if (textureDatas.size() == 1) {
            brick.setPrimaryTextureData(textureDatas.iterator().next());
        } else {
            brick.setTextureDatas(textureDatas);
        }
        if (controls.getFilterActions().size() > 0) {
            for (Action action : controls.getFilterActions()) {
                mip3d.addMenuAction(action);
            }
        }
        mip3d.addActor(brick);

        locallyAddedComponents.add(mip3d);
        this.setPreferredSize(WIDGET_SIZE);
        this.setMinimumSize(WIDGET_SIZE);
        this.setLayout(new BorderLayout());
        JPanel southPanel = new JPanel();
        southPanel.setLayout(new BorderLayout());
        Component sliderPanel = controls.getSliderPanel();
        if (labelText != null) {
            final JLabel label = new JLabel(labelText);
            locallyAddedComponents.add(label);
            southPanel.add(label, BorderLayout.SOUTH);
        }
        JPanel southWestPanel = new JPanel();
        ColorButtonPanel addsubs = controls.getColorButtonPanel();
        // One more button to allow the user to 'synchronize' the two color
        // models.
        addsubs.addButton(controls.getSharedColorModelButton());
        southWestPanel.setLayout(new BorderLayout());        
        southWestPanel.add(addsubs, BorderLayout.CENTER);

        southPanel.add(sliderPanel, BorderLayout.CENTER);
        southPanel.add(southWestPanel, BorderLayout.EAST);
        locallyAddedComponents.add(southPanel);
        this.add(southPanel, BorderLayout.SOUTH);
        this.add(mip3d, BorderLayout.CENTER);
        controls.setFilterActiveState( hasBeenFiltered );
    }

    private void cleanup() {
        // Cleanup old widgets.
        for (Component c : locallyAddedComponents) {
            Container parent = c.getParent();
            parent.remove(c);
            if (c instanceof Mip3d) {
                Mip3d mip3d = (Mip3d) c;
                mip3d.setVisible(false);
                mip3d.clear();
            }
        }
        locallyAddedComponents.clear();
        if (controls != null) {
            controls.cleanup();
        }
        controls = null;
        validate();
        repaint();
    }

    private class SnapshotWorker extends SimpleWorker {

        private final VolumeSource volumeSource;
        private Collection<TextureDataI> textureDatas;

        public SnapshotWorker(VolumeSource collector) {
            this.volumeSource = collector;
            textureDatas = Collections.<TextureDataI>synchronizedCollection(new ArrayList<>());
        }

        @Override
        protected void doStuff() throws Exception {
            volumeAcceptor = new VolumeSource.VolumeAcceptor() {
                @Override
                public void accept(TextureDataI textureData) {
                    SnapshotWorker.this.textureDatas.add(textureData);
                }
            };
            try {
                volumeSource.getVolume(volumeAcceptor);
            } catch (RuntimeException rte) {
                throw new Exception(rte);
            }
        }

        @Override
        public void propertyChange(PropertyChangeEvent e) {
            if (progressMonitor == null) {
                return;
            }
            if ("progress".equals(e.getPropertyName())) {
                int progress = (Integer) e.getNewValue();
                progressMonitor.setProgress(progress);
                if (progressMonitor.isCanceled()) {
                    super.cancel(true);
                }
            }
        }

        @Override
        protected void hadSuccess() {
            progressMonitor.setNote("Launching viewer.");
            launch(textureDatas);
        }

        @Override
        protected void hadError(Throwable error) {
            FrameworkAccess.handleException(error);
        }
    }
}
