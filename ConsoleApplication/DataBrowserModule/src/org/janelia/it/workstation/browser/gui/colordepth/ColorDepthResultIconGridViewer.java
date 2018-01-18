package org.janelia.it.workstation.browser.gui.colordepth;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.actions.DomainObjectContextMenu;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.api.DomainModel;
import org.janelia.it.workstation.browser.events.selection.ChildSelectionModel;
import org.janelia.it.workstation.browser.gui.hud.Hud;
import org.janelia.it.workstation.browser.gui.listview.ListViewer;
import org.janelia.it.workstation.browser.gui.listview.ListViewerActionListener;
import org.janelia.it.workstation.browser.gui.listview.ListViewerState;
import org.janelia.it.workstation.browser.gui.listview.icongrid.IconGridViewerConfiguration;
import org.janelia.it.workstation.browser.gui.listview.icongrid.IconGridViewerPanel;
import org.janelia.it.workstation.browser.gui.listview.icongrid.ImageModel;
import org.janelia.it.workstation.browser.gui.support.Icons;
import org.janelia.it.workstation.browser.gui.support.SearchProvider;
import org.janelia.it.workstation.browser.model.AnnotatedObjectList;
import org.janelia.it.workstation.browser.model.ImageDecorator;
import org.janelia.it.workstation.browser.model.search.ResultPage;
import org.janelia.it.workstation.browser.util.Utils;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.model.access.domain.DomainUtils;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.gui.colordepth.ColorDepthMatch;
import org.janelia.model.domain.ontology.Annotation;
import org.janelia.model.domain.sample.Sample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An IconGridViewer implementation for viewing color depth search results.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ColorDepthResultIconGridViewer 
        extends IconGridViewerPanel<ColorDepthMatch, String> 
        implements ListViewer<ColorDepthMatch, String> {
    
    private static final Logger log = LoggerFactory.getLogger(ColorDepthResultIconGridViewer.class);

    // Configuration
    private IconGridViewerConfiguration config;
    @SuppressWarnings("unused")
    private SearchProvider searchProvider;
    
    // State
    private AnnotatedObjectList<ColorDepthMatch, String> matchList;
    private Map<Reference, Sample> sampleMap = new HashMap<>();
    private Map<String, ColorDepthMatch> matchMap = new HashMap<>();
    private ChildSelectionModel<ColorDepthMatch, String> selectionModel;
    
    private final ImageModel<ColorDepthMatch, String> imageModel = new ImageModel<ColorDepthMatch, String>() {

        @Override
        public String getImageUniqueId(ColorDepthMatch match) {
            return match.getFilepath();
        }
        
        @Override
        public String getImageFilepath(ColorDepthMatch match) {
            Sample sample = sampleMap.get(match.getSample());
            if (sample==null) {
                return null;
            }
            return match.getFilepath();
        }

        @Override
        public BufferedImage getStaticIcon(ColorDepthMatch match) {
            // Assume anything without an image is locked
            return Icons.getImage("file_lock.png");
        }

        @Override
        public ColorDepthMatch getImageByUniqueId(String filepath) throws Exception {
            return matchMap.get(filepath);
        }
        
        @Override
        public String getImageTitle(ColorDepthMatch match) {
            Sample sample = sampleMap.get(match.getSample());
            if (sample==null) {
                return "Access denied";
            }
            return sample.getName();
        }

        @Override
        public String getImageSubtitle(ColorDepthMatch match) {
            return String.format("Score: %d (%2f%%)", match.getScore(), match.getScorePercent()*100);
        }
        
        @Override
        public List<ImageDecorator> getDecorators(ColorDepthMatch match) {
            return Collections.emptyList();
        }

        @Override
        public List<Annotation> getAnnotations(ColorDepthMatch imageObject) {
            return Collections.emptyList();
        }
        
    };

    public ColorDepthResultIconGridViewer() {
        setImageModel(imageModel);
        this.config = IconGridViewerConfiguration.loadConfig();
        
    }
    
//    private void setPreferenceAsync(final String category, final Object value) {
//
//        Utils.setMainFrameCursorWaitStatus(true);
//
//        SimpleWorker worker = new SimpleWorker() {
//
//            @Override
//            protected void doStuff() throws Exception {
//                setPreference(category, value);
//            }
//
//            @Override
//            protected void hadSuccess() {
//                refreshDomainObjects();
//            }
//
//            @Override
//            protected void hadError(Throwable error) {
//                Utils.setMainFrameCursorWaitStatus(false);
//                ConsoleApp.handleException(error);
//            }
//        };
//
//        worker.execute();
//    }
    
//    private String getPreference(String category) {
//        try {
//            final DomainObject parentObject = (DomainObject)selectionModel.getParentObject();
//            return FrameworkImplProvider.getRemotePreferenceValue(category, parentObject.getId().toString(), null);
//        }
//        catch (Exception e) {
//            log.error("Error getting preference", e);
//            return null;
//        }
//    }
//    
//    private void setPreference(final String category, final Object value) throws Exception {
//        final DomainObject parentObject = (DomainObject)selectionModel.getParentObject();
//        if (parentObject.getId()!=null) {
//            FrameworkImplProvider.setRemotePreferenceValue(category, parentObject.getId().toString(), value);
//        }
//    }
    
    @Override
    public JPanel getPanel() {
        return this;
    }

    @Override
    public void setActionListener(ListViewerActionListener listener) {
    }

    @Override
    public void setSearchProvider(SearchProvider searchProvider) {
        this.searchProvider = searchProvider;
    }
    
    @Override
    public void setSelectionModel(ChildSelectionModel<ColorDepthMatch, String> selectionModel) {
        super.setSelectionModel(selectionModel);
        this.selectionModel = selectionModel;
    }
    
    @Override
    public ChildSelectionModel<ColorDepthMatch, String> getSelectionModel() {
        return selectionModel;
    }

    @Override
    public void activate() {
    }

    @Override
    public void deactivate() {
    }
    
    @Override
    public int getNumItemsHidden() {
        int totalItems = this.matchList.getObjects().size();
        int totalVisibleItems = getObjects().size();
        return totalItems-totalVisibleItems;
    }

    @Override
    public void select(List<ColorDepthMatch> objects, boolean select, boolean clearAll, boolean isUserDriven, boolean notifyModel) {
        log.info("selectDomainObjects(objects={},select={},clearAll={},isUserDriven={},notifyModel={})", 
                DomainUtils.abbr(objects), select, clearAll, isUserDriven, notifyModel);

        if (objects.isEmpty()) {
            return;
        }

        if (select) {
            selectObjects(objects, clearAll, isUserDriven, notifyModel);
        }
        else {
            deselectObjects(objects, isUserDriven, notifyModel);
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                scrollSelectedObjectsToCenter();
            }
        });
    }
    
    @Override
    public void selectEditObjects(List<ColorDepthMatch> domainObjects, boolean select) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void showLoadingIndicator() {
        removeAll();
        add(new JLabel(Icons.getLoadingIcon()));
        updateUI();
    }

    @Override
    public void show(AnnotatedObjectList<ColorDepthMatch, String> matchList, Callable<Void> success) {
        
        this.matchList = matchList;
        sampleMap.clear();
        matchMap.clear();
        
        log.info("show(objects={})",DomainUtils.abbr(matchList.getObjects()));

        SimpleWorker worker = new SimpleWorker() {

            DomainModel model = DomainMgr.getDomainMgr().getModel();
            List<ColorDepthMatch> matchObjects;
            
            @Override
            protected void doStuff() throws Exception {
                matchObjects = matchList.getObjects();

                // Look up any samples
                Set<Reference> sampleRefs = new HashSet<>();
                for (ColorDepthMatch match : matchObjects) {
                    if (!sampleMap.containsKey(match.getSample())) {
                        log.info("Will load {}", match.getSample());
                        sampleRefs.add(match.getSample());
                    }
                }
                sampleMap.putAll(DomainUtils.getMapByReference(model.getDomainObjectsAs(Sample.class, new ArrayList<>(sampleRefs))));
            }

            @Override
            protected void hadSuccess() {
                showObjects(matchObjects, success);
            }

            @Override
            protected void hadError(Throwable error) {
                ConsoleApp.handleException(error);
            }
        };

        worker.execute();        
    }

    @Override
    public void toggleEditMode(boolean editMode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void refreshEditMode() {   }

    @Override
    public void setEditSelectionModel(ChildSelectionModel<ColorDepthMatch, String> editSelectionModel) {
        throw new UnsupportedOperationException();   
    }

    @Override
    public ChildSelectionModel<ColorDepthMatch, String> getEditSelectionModel() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public boolean matches(ResultPage<ColorDepthMatch, String> resultPage, ColorDepthMatch object, String text) {
        log.trace("Searching {} for {}", object.getFilepath(), text);

        String tupper = text.toUpperCase();

        String name = getImageModel().getImageTitle(object);
        if (name!=null && name.toUpperCase().contains(tupper)) {
            return true;
        }

        return false;
    }

    @Override
    public void refresh(ColorDepthMatch match) {
        refreshObject(match);
    }

    private void refreshView() {
        show(matchList, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                Utils.setMainFrameCursorWaitStatus(false);
                return null;
            }
        });
    }

    @Override
    protected DomainObjectContextMenu getContextualPopupMenu() {
        return null;
    }

    @Override
    protected JPopupMenu getAnnotationPopupMenu(Annotation annotation) {
        return null;
    }

    @Override
    protected void moreAnnotationsButtonDoubleClicked(ColorDepthMatch match) {}

    @Override
    protected void objectDoubleClick(ColorDepthMatch match) {}
    
    @Override
    protected void deleteKeyPressed() {}

    @Override
    protected void configButtonPressed() {}

    @Override
    protected void setMustHaveImage(boolean mustHaveImage) {}

    @Override
    protected boolean isMustHaveImage() {
        return false;
    }

    @Override
    protected void updateHud(boolean toggle) {

        if (!toggle && !Hud.isInitialized()) return;
        
        Hud hud = Hud.getSingletonInstance();
        hud.setKeyListener(keyListener);
//
//        try {
//            List<DomainObject> selected = getSelectedObjects();
//            
//            if (selected.size() != 1) {
//                hud.hideDialog();
//                return;
//            }
//            
//            DomainObject domainObject = selected.get(0);
//            if (toggle) {
////                hud.setObjectAndToggleDialog(domainObject, resultButton.getResultDescriptor(), typeButton.getImageTypeName());
//            }
//            else {
////                hud.setObject(domainObject, resultButton.getResultDescriptor(), typeButton.getImageTypeName(), false);
//            }
//        } 
//        catch (Exception ex) {
//            ConsoleApp.handleException(ex);
//        }
    }

    @Override
    public ListViewerState saveState() {
        return null;
    }

    @Override
    public void restoreState(ListViewerState viewerState) {
    }

//    private List<ColorDepthMatch> getSelectedObjects() {
//        try {
//            return DomainMgr.getDomainMgr().getModel().getDomainObjects(selectionModel.getSelectedIds());
//        }  catch (Exception e) {
//            ConsoleApp.handleException(e);
//            return null;
//        }
//    }
    
}