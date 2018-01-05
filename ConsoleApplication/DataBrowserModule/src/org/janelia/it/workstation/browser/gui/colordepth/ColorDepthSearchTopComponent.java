package org.janelia.it.workstation.browser.gui.colordepth;

import java.awt.BorderLayout;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.api.AccessManager;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.components.DomainViewerManager;
import org.janelia.it.workstation.browser.components.DomainViewerTopComponent;
import org.janelia.it.workstation.browser.events.Events;
import org.janelia.it.workstation.browser.events.lifecycle.SessionStartEvent;
import org.janelia.it.workstation.browser.gui.editor.DomainObjectEditor;
import org.janelia.it.workstation.browser.gui.editor.SampleEditorPanel;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.janelia.model.access.domain.DomainUtils;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.sample.LSMImage;
import org.janelia.model.domain.sample.NeuronFragment;
import org.janelia.model.domain.sample.Sample;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.windows.TopComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

import org.openide.util.NbBundle.Messages;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;

/**
 * Top component which displays the color depth search interface and results.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ConvertAsProperties(
        dtd = "-//org.janelia.it.workstation.browser.gui.colordepth//ColorDepthSearch//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = ColorDepthSearchTopComponent.TC_NAME,
        //iconBase="SET/PATH/TO/ICON/HERE", 
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "editor", openAtStartup = false)
@ActionID(category = "Window", id = "org.janelia.it.workstation.browser.gui.colordepth.ColorDepthSearchTopComponent")
@ActionReference(path = "Menu/Window" /*, position = 333 */)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_ColorDepthSearchAction",
        preferredID = ColorDepthSearchTopComponent.TC_NAME
)
@Messages({
    "CTL_ColorDepthSearchAction=Color Depth Search",
    "CTL_ColorDepthSearchTopComponent=Color Depth Search",
    "HINT_ColorDepthSearchTopComponent=Color Depth Search"
})
public final class ColorDepthSearchTopComponent extends TopComponent {

    private static final Logger log = LoggerFactory.getLogger(DomainViewerTopComponent.class);
    
    public static final String TC_NAME = "ColorDepthSearchTopComponent";
    public static final String TC_VERSION = "1.0";

    /* Instance variables */
    
    private final InstanceContent content = new InstanceContent();
    private DomainObjectEditor<DomainObject> editor;
    private Reference refToOpen;
    
    public ColorDepthSearchTopComponent() {
        initComponents();
        setName(Bundle.CTL_ColorDepthSearchTopComponent());
        setToolTipText(Bundle.HINT_ColorDepthSearchTopComponent());
        associateLookup(new AbstractLookup(content));
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setLayout(new java.awt.BorderLayout());
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
    
    @Override
    public void componentOpened() {
        Events.getInstance().registerOnEventBus(this);
    }

    @Override
    public void componentClosed() {
        clearEditor();
        Events.getInstance().unregisterOnEventBus(this);
    }
    
    @Override
    protected void componentActivated() {
        log.info("Activating domain viewer");
        if (editor!=null) {
            editor.activate();
        }
    }
    
    @Override
    protected void componentDeactivated() {
        if (editor!=null) {
            editor.deactivate();
        }
    }
    
    void writeProperties(java.util.Properties p) {
        if (p==null) return;
        p.setProperty("version", TC_VERSION);
        DomainObject current = getCurrent();
        if (current!=null) {
            String objectRef = Reference.createFor(current).toString();
            log.info("Writing state: {}",objectRef);
            p.setProperty("objectRef", objectRef);
        }
        else {
            p.remove("objectRef");
        }
    }

    void readProperties(java.util.Properties p) {
        if (p==null) return;
        String version = p.getProperty("version");
        final String objectStrRef = p.getProperty("objectRef");
        log.info("Reading state: {}",objectStrRef);
        if (TC_VERSION.equals(version) && objectStrRef!=null) {
            // Must write to instance variables from EDT only
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    refToOpen = Reference.createFor(objectStrRef);
                    if (AccessManager.loggedIn()) {
                        loadPreviousSession();
                    }
                    else {
                        // Not logged in yet, wait for a SessionStartEvent
                    }
                }
            });
        }
    }

    @Subscribe
    public void sessionStarted(SessionStartEvent event) {
        loadPreviousSession();
    }
    
    private void loadPreviousSession() {
        
        if (refToOpen==null) return;
        log.info("Loading previous session: "+refToOpen);
        final Reference objectRef = refToOpen;
        this.refToOpen = null;
        
        SimpleWorker worker = new SimpleWorker() {
            DomainObject object;

            @Override
            protected void doStuff() throws Exception {
                object = DomainMgr.getDomainMgr().getModel().getDomainObject(objectRef);
            }

            @Override
            protected void hadSuccess() {
                if (object!=null) {
                    loadDomainObject(object, false);
                }
            }

            @Override
            protected void hadError(Throwable error) {
                ConsoleApp.handleException(error);
            }
        };
        worker.execute();
    }


    public DomainObject getCurrent() {
        return getLookup().lookup(DomainObject.class);
    }

    private boolean setCurrent(DomainObject domainObject) {
        DomainObject curr = getCurrent();
        if (domainObject.equals(curr)) {
            return false;
        }
        if (curr!=null) {
            content.remove(curr);
        }
        content.add(domainObject);
        return true;
    }

    public void clearEditor() {
        if (editor!=null) {
            remove((JComponent)editor);
            Events.getInstance().unregisterOnEventBus(editor.getEventBusListener());
        }
        this.editor = null;
    }
    
    @SuppressWarnings("unchecked")
    public void setEditorClass(Class<? extends DomainObjectEditor<?>> editorClass) {
        try {
            clearEditor();
            editor = (DomainObjectEditor<DomainObject>) editorClass.newInstance();
            add((JComponent)editor, BorderLayout.CENTER);
            Events.getInstance().registerOnEventBus(editor.getEventBusListener());
            revalidate();
            repaint();
        }
        catch (InstantiationException | IllegalAccessException e) {
            ConsoleApp.handleException(e);
        }
        setName(editor.getName());
    }
    
    public DomainObjectEditor<? extends DomainObject> getEditor() {
        return editor;
    }

    public boolean isCurrent(DomainObject domainObject) {
        try {
            domainObject = DomainViewerManager.getObjectToLoad(domainObject);
            if (domainObject==null) return getCurrent()==null;
            return DomainUtils.equals(getCurrent(), domainObject);
        }
        catch (Exception e) {
            ConsoleApp.handleException(e);
            return false;
        }
    }

    public void loadDomainObject(DomainObject domainObject, boolean isUserDriven) {
        try {
            domainObject = DomainViewerManager.getObjectToLoad(domainObject);
            if (domainObject==null) return;
            
            final Class<? extends DomainObjectEditor<?>> editorClass = getEditorClass(domainObject);
            if (editorClass == null) {
                // TODO: comment this exception back in after initial development is complete
                //throw new IllegalStateException("No viewer defined for domain object of type "+domainObject.getClass().getName());
                log.info("No viewer defined for domain object of type {}", domainObject.getClass().getName());
                return;
            }

            // Do we already have the given node loaded?
            if (!setCurrent(domainObject)) {
                return;
            }

            if (editor == null || !editor.getClass().equals(editorClass)) {
                setEditorClass(editorClass);
            }
            editor.loadDomainObject(domainObject, isUserDriven, null);
            setName(editor.getName());
        }
        catch (Exception e) {
            ConsoleApp.handleException(e);
        }
    }

    private static Class<? extends DomainObjectEditor<?>> getEditorClass(DomainObject domainObject) {
        if (domainObject instanceof ColorDepthSearchEditorPanel) {
            return ColorDepthSearchEditorPanel.class;
        }
        return null;
    }
    
    public static boolean isSupported(DomainObject domainObject) {
        return domainObject!=null && getEditorClass(domainObject)!=null;
    }
}
