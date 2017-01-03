/*
 * Licensed under the Janelia Farm Research Campus Software Copyright 1.1
 * 
 * Copyright (c) 2014, Howard Hughes Medical Institute, All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *     1. Redistributions of source code must retain the above copyright notice, 
 *        this list of conditions and the following disclaimer.
 *     2. Redistributions in binary form must reproduce the above copyright 
 *        notice, this list of conditions and the following disclaimer in the 
 *        documentation and/or other materials provided with the distribution.
 *     3. Neither the name of the Howard Hughes Medical Institute nor the names 
 *        of its contributors may be used to endorse or promote products derived 
 *        from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, ANY 
 * IMPLIED WARRANTIES OF MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A 
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * REASONABLE ROYALTIES; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.janelia.horta;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JOptionPane;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MouseInputListener;
import javax.swing.event.UndoableEditEvent;
import org.janelia.console.viewerapi.listener.NeuronVertexCreationListener;
import org.janelia.console.viewerapi.listener.NeuronVertexDeletionListener;
import org.janelia.console.viewerapi.listener.NeuronVertexUpdateListener;
import org.janelia.console.viewerapi.model.NeuronModel;
import org.janelia.console.viewerapi.model.NeuronVertex;
import org.janelia.console.viewerapi.model.NeuronVertexCreationObservable;
import org.janelia.console.viewerapi.model.VertexCollectionWithNeuron;
import org.janelia.console.viewerapi.model.VertexWithNeuron;
import org.janelia.geometry3d.Vector3;
import org.janelia.gltools.GL3Actor;
import org.janelia.horta.actors.DensityCursorActor;
import org.janelia.horta.actors.ParentVertexActor;
import org.janelia.horta.actors.SpheresActor;
import org.janelia.horta.actors.VertexHighlightActor;
import org.janelia.console.viewerapi.commands.AppendNeuronVertexCommand;
import org.janelia.console.viewerapi.commands.CreateNeuronCommand;
import org.janelia.console.viewerapi.model.DefaultNeuron;
import org.janelia.console.viewerapi.commands.MoveNeuronAnchorCommand;
import org.janelia.console.viewerapi.model.NeuronSet;
import org.janelia.console.viewerapi.commands.UpdateNeuronAnchorRadiusCommand;
import org.janelia.console.viewerapi.commands.VertexAdder;
import org.janelia.geometry3d.ConstVector3;
import org.janelia.horta.nodes.BasicNeuronModel;
import org.janelia.horta.nodes.BasicSwcVertex;
import org.openide.awt.StatusDisplayer;
import org.openide.awt.UndoRedo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapted from C:\Users\brunsc\Documents\Fiji_Plugins\Auto_Trace\Semi_Trace.java
 * @author Christopher Bruns
 */
public class TracingInteractor extends MouseAdapter
        implements MouseInputListener, KeyListener, 
        NeuronVertexDeletionListener, NeuronVertexCreationListener,
        NeuronVertexUpdateListener
{
    private final VolumeProjection volumeProjection;
    private final int max_tol = 5; // pixels
        
    // For selection affordance
    // For GUI feedback on existing model, contains zero or one vertex.
    // Larger yellow overlay over an existing vertex under the mouse pointer.
    private final NeuronModel highlightHoverModel = new BasicNeuronModel("Hover highlight");
    private NeuronVertex cachedHighlightVertex = null;
    private NeuronModel cachedHighlightNeuron = null;
    
    // For Tracing
    // Larger blueish vertex with a "P" for current selected persisted parent
    // first model is an ephemeral single vertex neuron model for display of "P"
    private final NeuronModel parentVertexModel = new BasicNeuronModel("Selected parent vertex"); // TODO: begin point of auto tracing
    private NeuronVertex cachedParentVertex = null;
    // second model is the actual associated in-memory full parent neuron domain model
    private NeuronModel cachedParentNeuronModel = null;
    
    // White ghost vertex for potential new vertex under cursor 
    // TODO: Maybe color RED until a good path from parent is found
    // This is the new neuron cursor
    private final NeuronModel densityCursorModel = new BasicNeuronModel("Hover density");
    private Vector3 cachedDensityCursorXyz = null;
    
    private final NeuronModel anchorEditModel = new BasicNeuronModel("Interactive anchor edit view");
    
    private final UndoRedo.Manager undoRedoManager;
    
    // Data structure to help unravel serial undo/redo appendVertex commands
    Map<List<Float>, VertexAdder> appendCommandForVertex = new HashMap<>();
    
    RadiusEstimator radiusEstimator = 
            // new TwoDimensionalRadiusEstimator(); // TODO: Use this again
            new ConstantRadiusEstimator(DefaultNeuron.radius);
    
    private StatusDisplayer.Message previousHoverMessage;
    
    private NeuronSet defaultWorkspace = null;

    private Logger log = LoggerFactory.getLogger(this.getClass());
    private NeuronVertex cachedDragVertex;

    public NeuronSet getDefaultWorkspace() {
        return defaultWorkspace;
    }

    public void setDefaultWorkspace(NeuronSet defaultWorkspace) {
        if (this.defaultWorkspace == defaultWorkspace)
            return;
        this.defaultWorkspace = defaultWorkspace;
    }
    
    @Override
    public void keyTyped(KeyEvent keyEvent) {
        // System.out.println("KeyTyped");
        // System.out.println(keyEvent.getKeyCode()+", "+KeyEvent.VK_ESCAPE);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // System.out.println("KeyPressed");
    }

    @Override
    public void keyReleased(KeyEvent keyEvent) {
        // System.out.println("KeyReleased");
    }

    public TracingInteractor(
            VolumeProjection volumeProjection,
            UndoRedo.Manager undoRedoManager) 
    {
        this.volumeProjection = volumeProjection;
        // connectMouseToComponent();
        this.undoRedoManager = undoRedoManager;
    }
    
    public List<GL3Actor> createActors() {
        List<GL3Actor> result = new ArrayList<>();

        // Create actors in the order that they should be rendered;

        // Create a special single-vertex actor for highlighting the selected parent vertex
        SpheresActor parentActor = new ParentVertexActor(parentVertexModel);
        parentActor.setMinPixelRadius(5.0f);
        result.add(parentActor);
        
        // Create a special single-vertex actor for highlighting the vertex under the cursor
        SpheresActor highlightActor = new VertexHighlightActor(highlightHoverModel);
        highlightActor.setMinPixelRadius(7.0f);
        result.add(highlightActor);
        
        // Create a special single-vertex actor for highlighting the vertex under the cursor
        SpheresActor densityCursorActor = new DensityCursorActor(densityCursorModel);
        densityCursorActor.setMinPixelRadius(1.0f);
        result.add(densityCursorActor);
        
        SpheresActor anchorEditActor = new VertexHighlightActor(anchorEditModel);
        result.add(anchorEditActor);
        
        return result;
    }
    
    // List<Float> for comparing vertex locations, even if the underlying vertex object has changed identity.
    private static List<Float> vtxKey(NeuronVertex vtx) {
        float[] v = vtx.getLocation();
        return Arrays.asList(v[0], v[1], v[2]);
    }
    
    // Mouse clicking for recentering, selection, and tracing
    @Override
    public void mouseClicked(MouseEvent event) {
        // System.out.println("Mouse clicked in tracer");
        long clickTime = System.nanoTime();
        
        // Cache the current state, in case subsequent asynchronous changes occur to hoveredDensity etc.
        InteractorContext context = createContext();
        
        // single click on primary (left) button
        if ( (event.getClickCount() == 1) && (event.getButton() == MouseEvent.BUTTON1) )
        {
            // Shift-clicking might add a new vertex to the neuron model
            if (event.isShiftDown()) { // Hold down shift to build neurons
                if (context.canAppendVertex()) {
                    context.appendVertex();
                    long appendedTime = System.nanoTime();
                    double elapsed = (appendedTime - clickTime) / 1.0e6;
                    // System.out.println("Append took " + elapsed + "milliseconds"); // 1200 ms -- way too long
                }
                else if (context.canMergeNeurite()) { // Maybe merge two neurons
                    context.mergeNeurite();
                }
                else {
                    if (context.canCreateNeuron()) {
                        context.createNeuron();
                    }
                }
            }
            else { // Non-shift click to select vertices
                // Click on highlighted vertex to make it the next parent
                if (volumeProjection.isNeuronModelAt(event.getPoint())) {
                    if (context.canSelectParent())
                        context.selectParent();
                }
                // Click away from existing neurons to clear parent point
                else {
                    // User requested not to unselect parent March 2016
                    // clearParentVertexAndNotify();
                }
            }
        }
    }
    
    @Override
    public void mouseExited(MouseEvent event) {
        // System.out.println("mouse exited");
        
        // keep showing hover highlight cursor, if dragging, even when mouse exits
        int buttonsDownMask = MouseEvent.BUTTON1_DOWN_MASK 
                | MouseEvent.BUTTON2_DOWN_MASK 
                | MouseEvent.BUTTON3_DOWN_MASK;
        if ( (event.getModifiersEx() & buttonsDownMask) != 0 )
            return;
        
        // Stop displaying hover highlight when cursor exits the viewport
        Collection<NeuronVertex> highlightVertexes = highlightHoverModel.getVertexes();
        if (clearHighlightHoverVertex()) {
            highlightHoverModel.getVertexesRemovedObservable().notifyObservers(
                    new VertexCollectionWithNeuron(highlightVertexes, cachedHighlightNeuron)
            ); // repaint
        }

    }

    public boolean selectParentVertex(NeuronVertex vertex, NeuronModel neuron)
    {
        if (vertex == null) return false;
        
        if (cachedParentVertex == vertex)
            return false;
        cachedParentVertex = vertex;
        cachedParentNeuronModel = neuron;
        
        // Remove any previous vertex
        parentVertexModel.getVertexes().clear();
        parentVertexModel.getEdges().clear();

        // NeuronVertexSpatialIndex vix = volumeProjection.getVertexIndex();
        // NeuronModel neuron = vix.neuronForVertex(vertex);
        float loc[] = vertex.getLocation();

        // Create a modified vertex to represent the enlarged, highlighted actor
        BasicSwcVertex parentVertex = new BasicSwcVertex(loc[0], loc[1], loc[2]); // same center location as real vertex
        // Set parent actor radius X% larger than true vertex radius, and at least 2 pixels larger
        float startRadius = DefaultNeuron.radius;
        if (vertex.hasRadius())
            startRadius = vertex.getRadius();
        float parentRadius = startRadius * 1.15f;
        // plus at least 2 pixels bigger - this is handled in actor creation time
        parentVertex.setRadius(parentRadius);
        // blend neuron color with pale yellow parent color
        float parentColor[] = {0.5f, 0.6f, 1.0f, 0.5f}; // pale blue and transparent
        float neuronColor[] = {1.0f, 0.0f, 1.0f, 1.0f};
        if (neuron != null) {
            neuronColor = neuron.getColor().getColorComponents(neuronColor);
        }
        float parentBlend = 0.75f;
        Color blendedColor = new Color(
                neuronColor[0] - parentBlend * (neuronColor[0] - parentColor[0]),
                neuronColor[1] - parentBlend * (neuronColor[1] - parentColor[1]),
                neuronColor[2] - parentBlend * (neuronColor[2] - parentColor[2]),
                parentColor[3] // always use the same alpha transparency value
                );
        parentVertexModel.setVisible(true);
        parentVertexModel.setColor(blendedColor);
        
        parentVertexModel.getVertexes().add(parentVertex);
        
        // parentVertexModel.setColor(Color.MAGENTA); // for debugging
        NeuronVertexCreationObservable addedSignal = parentVertexModel.getVertexCreatedObservable();
        addedSignal.setChanged();
        addedSignal.notifyObservers(new VertexWithNeuron(parentVertex, neuron));

        parentVertexModel.getColorChangeObservable().notifyObservers();
        
        return true; 
    }
    
    private boolean parentIsSelected() {
        if (parentVertexModel == null) return false;
        if (parentVertexModel.getVertexes() == null) return false;
        if (parentVertexModel.getVertexes().isEmpty()) return false;
        return true;
    }
    
    private boolean densityIsHovered() {
        if (densityCursorModel == null) return false;
        if (densityCursorModel.getVertexes() == null) return false;
        if (densityCursorModel.getVertexes().isEmpty()) return false;
        return true;
    }
    
    // Clear display of existing vertex highlight
    private boolean clearParentVertex() 
    {
        if (parentVertexModel.getVertexes().isEmpty()) {
            return false;
        }
        parentVertexModel.getVertexes().clear();
        parentVertexModel.getEdges().clear();
        parentVertexModel.getVertexesRemovedObservable().setChanged();
        cachedParentVertex = null;
        cachedParentNeuronModel = null;
        return true;
    }
    
    private boolean clearParentVertexAndNotify() {
        Collection<NeuronVertex> parentVertexes = parentVertexModel.getVertexes();
        if (clearParentVertex()) {
             parentVertexModel.getVertexesRemovedObservable().notifyObservers(
                     new VertexCollectionWithNeuron(parentVertexes, cachedParentNeuronModel)
             );
             return true;
        }
        return false;
    }
    
    private boolean setDensityCursor(Vector3 xyz, Point screenPoint)
    {
        if (xyz == null) return false;
        
        if (cachedDensityCursorXyz == xyz)
            return false;
        cachedDensityCursorXyz = xyz;
        
        // Remove any previous vertex
        densityCursorModel.getVertexes().clear();
        densityCursorModel.getEdges().clear();

        // Create a modified vertex to represent the enlarged, highlighted actor
        BasicSwcVertex densityVertex = new BasicSwcVertex(xyz.getX(), xyz.getY(), xyz.getZ()); // same center location as real vertex

        float radius = radiusEstimator.estimateRadius(screenPoint, volumeProjection);
        // densityVertex.setRadius(DefaultNeuron.radius); // TODO: measure radius and set this rationally
        densityVertex.setRadius(radius);

        // blend neuron color with white(?) provisional vertex color
        Color vertexColor = new Color(0.2f, 1.0f, 0.8f, 0.5f);

        densityCursorModel.setVisible(true);
        densityCursorModel.setColor(vertexColor);
        // densityCursorModel.setColor(Color.MAGENTA); // for debugging

        densityCursorModel.getVertexes().add(densityVertex);
        densityCursorModel.getVertexCreatedObservable().setChanged();

        densityCursorModel.getVertexCreatedObservable().notifyObservers(new VertexWithNeuron(densityVertex, null));     
        densityCursorModel.getColorChangeObservable().notifyObservers();
        
        return true; 
    }
    
    // Clear display of existing vertex highlight
    private boolean clearDensityCursor()
    {
        if (densityCursorModel.getVertexes().isEmpty()) {
            return false;
        }
        densityCursorModel.getVertexes().clear();
        densityCursorModel.getEdges().clear();
        densityCursorModel.getVertexesRemovedObservable().setChanged();
        cachedDensityCursorXyz = null;
        return true;
    }
    
    // GUI feedback for hovering existing vertex under cursor
    // returns true if a previously unhighlighted vertex is highlighted
    private boolean highlightHoverVertex(NeuronVertex vertex, NeuronModel neuron) 
    {
        if (vertex == null) return false;
        
        if (cachedHighlightVertex == vertex)
            return false; // No change
        cachedHighlightVertex = vertex;
        cachedHighlightNeuron = neuron;
        
        // NeuronVertexSpatialIndex vix = volumeProjection.getVertexIndex();
        // NeuronModel neuron = vix.neuronForVertex(vertex);
        float loc[] = vertex.getLocation();
        
        boolean doShowStatusMessage = true;
        if (doShowStatusMessage) {
            String message = "";
            if (neuron != null) {
                message += neuron.getName() + ": ";
            }
            message += " XYZ = [" + loc[0] + ", " + loc[1] + ", " + loc[2] + "]";
            message += "; Vertex Object ID = " + System.identityHashCode(vertex);
            if (message.length() > 0)
                previousHoverMessage = StatusDisplayer.getDefault().setStatusText(message, 2);            
        }
        
        boolean doShowVertexActor = true;
        if (doShowVertexActor) {
            // Remove any previous vertex
            highlightHoverModel.getVertexes().clear();
            highlightHoverModel.getEdges().clear();

            // Create a modified vertex to represent the enlarged, highlighted actor
            BasicSwcVertex highlightVertex = new BasicSwcVertex(loc[0], loc[1], loc[2]); // same center location as real vertex
            // Set highlight actor radius X% larger than true vertex radius, and at least 2 pixels larger
            float startRadius = DefaultNeuron.radius;
            if (vertex.hasRadius())
                startRadius = vertex.getRadius();
            float highlightRadius = startRadius * 1.30f;
            // we add at least 2 pixels to glyph size - this is handled in actor creation time
            highlightVertex.setRadius(highlightRadius);
            // blend neuron color with pale yellow highlight color
            float highlightColor[] = {1.0f, 1.0f, 0.6f, 0.5f}; // pale yellow and transparent
            float neuronColor[] = {1.0f, 0.0f, 1.0f, 1.0f};
            if (neuron != null) {
                neuronColor = neuron.getColor().getColorComponents(neuronColor);
            }
            float highlightBlend = 0.75f;
            Color blendedColor = new Color(
                    neuronColor[0] - highlightBlend * (neuronColor[0] - highlightColor[0]),
                    neuronColor[1] - highlightBlend * (neuronColor[1] - highlightColor[1]),
                    neuronColor[2] - highlightBlend * (neuronColor[2] - highlightColor[2]),
                    highlightColor[3] // always use the same alpha transparency value
                    );
            highlightHoverModel.setVisible(true);
            highlightHoverModel.setColor(blendedColor);
            // highlightHoverModel.setColor(Color.MAGENTA); // for debugging
            
            highlightHoverModel.getVertexes().add(highlightVertex);
            highlightHoverModel.getVertexCreatedObservable().setChanged();
            
            highlightHoverModel.getVertexCreatedObservable().notifyObservers(new VertexWithNeuron(highlightVertex, neuron));     
            highlightHoverModel.getColorChangeObservable().notifyObservers();
        }
        
        return true;
    }
    
    // Clear display of existing vertex highlight
    private boolean clearHighlightHoverVertex() 
    {
        if (highlightHoverModel.getVertexes().isEmpty()) {
            return false;
        }
        highlightHoverModel.getVertexes().clear();
        highlightHoverModel.getEdges().clear();
        highlightHoverModel.getVertexesRemovedObservable().setChanged();
        previousHoverPoint = null;
        cachedHighlightVertex = null;
        cachedHighlightNeuron = null;
        return true;
    }
    
    private ConstVector3 previousDragXYZ = null;
 
    @Override
    public void mouseDragged(MouseEvent event) 
    {
        // log.info("Tracing Dragging");
        if (cachedDragVertex == null)
            return; // no vertex to drag
        if (! SwingUtilities.isLeftMouseButton(event))
            return; // left button drag only

        // log.info("Dragging a vertex");       
        // Update display (only) of dragged vertex
        // Update location of hover vertex glyph
        ConstVector3 p1 = volumeProjection.worldXyzForScreenXyInPlane(event.getPoint());
        ConstVector3 dXYZ = p1.minus(previousDragXYZ);
        NeuronVertex hoverVertex = highlightHoverModel.getVertexes().iterator().next();
        ConstVector3 oldLocation = new Vector3(hoverVertex.getLocation());
        ConstVector3 newLocation = oldLocation.plus(dXYZ);
        hoverVertex.setLocation(newLocation.getX(), newLocation.getY(), newLocation.getZ());

        // Trigger display update
        highlightHoverModel.getVertexUpdatedObservable().setChanged();
        highlightHoverModel.getVertexUpdatedObservable().notifyObservers(null);            

        // Update incremental screen location
        previousDragXYZ = p1;

        event.consume(); // Don't let OrbitPanZoomInteractor drag the world
        // log.info("Consumed tracing drag event");
    }
    
    @Override
    public void mouseMoved(MouseEvent event) 
    {
        // TODO: update old provisional tracing behavior
        moveHoverCursor(event.getPoint());
    }
    
    private ConstVector3 startingDragVertexLocation = null;
    @Override
    public void mousePressed(MouseEvent event) {
        // log.info("Begin drag");
        if (cachedHighlightVertex != null) {
            cachedDragVertex = cachedHighlightVertex;
            previousDragXYZ = volumeProjection.worldXyzForScreenXyInPlane(event.getPoint());
            startingDragVertexLocation = new Vector3(cachedHighlightVertex.getLocation());
            // log.info("Begin drag vertex");
        }
        else {
            cachedDragVertex = null;
        }
    }
    
    @Override
    public void mouseReleased(MouseEvent event) {
        // log.info("End drag");
        // Maybe complete an "anchor dragged" gesture
        if (cachedDragVertex != null) {
            assert(cachedDragVertex == cachedHighlightVertex);
            // log.info("End drag vertex");
            NeuronVertex hoverVertex = highlightHoverModel.getVertexes().iterator().next();
            ConstVector3 newLocation = new Vector3(hoverVertex.getLocation());
            if (! newLocation.equals(startingDragVertexLocation)) 
            {
                moveAnchor(cachedHighlightNeuron, cachedHighlightVertex, newLocation);
            }
        }
        cachedDragVertex = null;
    }
    
    private boolean moveAnchor(NeuronModel neuron, NeuronVertex anchor, ConstVector3 newLocation) 
    {
        MoveNeuronAnchorCommand cmd = new MoveNeuronAnchorCommand(
                neuron,
                anchor,
                new float[] {
                    newLocation.getX(),
                    newLocation.getY(),
                    newLocation.getZ()
                }
        );
        String errorMessage = "Failed to move neuron anchor";
        try {
            if (cmd.execute()) {
                log.info("User drag-moved anchor in Horta");
                undoRedoManager.undoableEditHappened(new UndoableEditEvent(this, cmd));

                // repaint right now...
                highlightHoverModel.getVertexUpdatedObservable().setChanged();
                highlightHoverModel.getVertexUpdatedObservable().notifyObservers(null);
                return true;
            }
        }
        catch (Exception exc) {
            errorMessage += ":\n" + exc.getMessage();
        }
        JOptionPane.showMessageDialog(
                volumeProjection.getMouseableComponent(),
                errorMessage,
                "Failed to move neuron anchor",
                JOptionPane.WARNING_MESSAGE);
        return false;
    }

    // Show provisional Anchor radius and position for current mouse location
    private Point previousHoverPoint = null;
    public void moveHoverCursor(Point screenPoint) {
        if (screenPoint == previousHoverPoint)
            return; // no change from last time
        previousHoverPoint = screenPoint;
        
        // Question: Which of these three locations is the current mouse cursor in?
        //  1) upon an existing neuron model vertex
        //  2) upon a region of image density
        //  3) neither
        
        // 1) (maybe) Highlight existing neuron annotation model vertex
        Point hoverPoint = screenPoint;
        boolean foundGoodHighlightVertex = true; // start optimistic...
        NeuronVertex nearestVertex = null;
        NeuronModel neuronModel = null;
        if (volumeProjection.isNeuronModelAt(hoverPoint)) { // found an existing annotation model under the cursor
            Vector3 cursorXyz = volumeProjection.worldXyzForScreenXy(hoverPoint);
            NeuronVertexSpatialIndex vix = volumeProjection.getVertexIndex();
            nearestVertex = vix.getNearest(cursorXyz);
            if (nearestVertex == null) // no vertices to be found?
                foundGoodHighlightVertex = false;
            else {
                neuronModel = vix.neuronForVertex(nearestVertex);
                if (neuronModel == null) {
                    // TODO: Should not happen
                    System.out.println("Unexpected null neuron");
                }
                // Is cursor too far from closest vertex?
                Vector3 vertexXyz = new Vector3(nearestVertex.getLocation());
                float dist = vertexXyz.distance(cursorXyz);
                float radius = DefaultNeuron.radius;
                if (nearestVertex.hasRadius())
                    radius = nearestVertex.getRadius();
                float absoluteHoverRadius = 2.50f * radius; // look this far away, relative to absolute vertex size
                // Also look a certain distance away in pixels, in case the view is way zoomed out.
                float screenHoverRadius = 10 / volumeProjection.getPixelsPerSceneUnit(); // look within at least 10 pixels
                // TODO: accept vertices within a certain number of pixels too
                if (dist > (absoluteHoverRadius + screenHoverRadius))
                    foundGoodHighlightVertex = false;
            }
        }
        if (nearestVertex == null)
            foundGoodHighlightVertex = false;
        // Make sure we can find the parent neuron, to avoid mysterious NPEs
        if (neuronModel == null)
            foundGoodHighlightVertex = false;
        if (foundGoodHighlightVertex) {
            highlightHoverVertex(nearestVertex, neuronModel);
        }
        else {
            Collection<NeuronVertex> highlightVertexes = highlightHoverModel.getVertexes();
            if (clearHighlightHoverVertex())
                highlightHoverModel.getVertexesRemovedObservable().notifyObservers(
                        new VertexCollectionWithNeuron(highlightVertexes, cachedHighlightNeuron)
                ); // repaint
            // Clear previous vertex message, if necessary
            if (previousHoverMessage != null) {
                previousHoverMessage.clear(2);
                previousHoverMessage = null;
            }
        }
        
        // 2) (maybe) show provisional anchor at current image density
        if ( (! foundGoodHighlightVertex) && (volumeProjection.isVolumeDensityAt(hoverPoint))) 
        {
            // Find nearby brightest point
            // screenPoint = optimizePosition(screenPoint); // TODO: disabling optimization for now
            Point optimizedPoint = optimizePosition(hoverPoint);
            Vector3 cursorXyz = volumeProjection.worldXyzForScreenXy(optimizedPoint);
            setDensityCursor(cursorXyz, optimizedPoint);
        }
        else {
            Collection<NeuronVertex> densityVertexes = densityCursorModel.getVertexes();
            if (clearDensityCursor())
                densityCursorModel.getVertexesRemovedObservable().notifyObservers(
                        new VertexCollectionWithNeuron(densityVertexes, null)
                );
        }
        
        // TODO: build up from current parent toward current mouse position
    }

    private Point optimizePosition(Point screenPoint) {
        // TODO - this method is pretty crude; but maybe good enough?
        screenPoint = optimizeX(screenPoint, max_tol);
        screenPoint = optimizeY(screenPoint, max_tol);
        // Refine to local maximum
        Point prevPoint = new Point(screenPoint.x, screenPoint.y);
        int stepCount = 0;
        do {
            stepCount += 1;
            if (stepCount > 5) 
                break;
            screenPoint = optimizeX(screenPoint, 2);
            screenPoint = optimizeY(screenPoint, 2);
        } while (! prevPoint.equals(screenPoint));
        return screenPoint;
    }
    
    // Find a nearby brighter pixel
    private Point optimizeX(Point p, int max) {
        Point p1 = searchOptimizeBrightness(p, -1, 0, max);
        Point p2 = searchOptimizeBrightness(p, 1, 0, max);
        double intensity1 = volumeProjection.getIntensity(p1);
        double intensity2 = volumeProjection.getIntensity(p2);
        if (intensity1 > intensity2) {
            return p1;
        } else {
            return p2;
        }
    }

    private Point optimizeY(Point p, int max) {
        Point p1 = searchOptimizeBrightness(p, 0, -1, max);
        Point p2 = searchOptimizeBrightness(p, 0, 1, max);
        double intensity1 = volumeProjection.getIntensity(p1);
        double intensity2 = volumeProjection.getIntensity(p2);
        if (intensity1 > intensity2) {
            return p1;
        } else {
            return p2;
        }
    }

    private Point searchOptimizeBrightness(Point point, int dx, int dy, int max_step) {
        double i_orig = volumeProjection.getIntensity(point);
        double best_i = i_orig;
        int best_t = 0;
        double max_drop = 10 + 0.05 * i_orig;
        for (int t = 1; t <= max_step; ++t) {
            double i_test = volumeProjection.getIntensity(new Point(
                    point.x + t * dx, 
                    point.y + t * dy));
            if (i_test > best_i) {
                best_i = i_test;
                best_t = t;
            } else if (i_test < (best_i - max_drop)) {
                break; // Don't cross valleys
            }
        }
        if (best_t == 0) {
            return point;
        } else {
            return new Point(point.x + best_t * dx, point.y + best_t * dy);
        }
    }

    @Override
    public void neuronVertexesDeleted(VertexCollectionWithNeuron doomed) {
        // Possibly remove parent glyph, if vertex is deleted
        if (cachedParentVertex == null) 
            return; // no sense checking 
        NeuronModel neuron = cachedParentNeuronModel; // In case we want to select a different parent below
        NeuronVertex removedParent = null;
        Set<NeuronVertex> allDoomedVertexes = new HashSet<>(); // remember them efficiently, for later checking reparent
        List<Float> pvXyz = vtxKey(cachedParentVertex);
        for (NeuronVertex doomedVertex : doomed.vertexes) {
            allDoomedVertexes.add(doomedVertex);
            if (vtxKey(doomedVertex).equals(pvXyz)) {
                removedParent = doomedVertex;
                clearParentVertex();
                break;
            }
        }
        // Maybe set parent to previous parent
        if (removedParent != null) {
            VertexAdder removedParentAppendCommand = appendCommandForVertex.get(vtxKey(removedParent));
            if (removedParentAppendCommand != null) {
                NeuronVertex previousParent = removedParentAppendCommand.getParentVertex();
                if (previousParent != null) {
                    selectParentVertex(previousParent, neuron);
                }
            }
        }
    }

    @Override
    public void neuronVertexCreated(VertexWithNeuron vertexWithNeuron) {
        selectParentVertex(vertexWithNeuron.vertex, vertexWithNeuron.neuron);
    }

    public InteractorContext createContext() {
        return new InteractorContext();
    }

    @Override
    public void neuronVertexUpdated(VertexWithNeuron vertexWithNeuron) {
        if (cachedParentVertex == null) {
            return; // no sense checking now
        }
        if (cachedParentVertex == vertexWithNeuron.vertex) {
            // To keep things simple, just delete and recreate
            clearParentVertex();
            selectParentVertex(vertexWithNeuron.vertex, vertexWithNeuron.neuron);
        }
    }

    
    // Cached state of interactor at one moment in time, so hovered density, for
    // example, does not go stale before the user selects a menu option.
    public class InteractorContext
    {
        private final NeuronVertex hoveredVertex;
        private final NeuronModel hoveredNeuron;
        private final NeuronVertex parentVertex;
        private final NeuronModel parentNeuron;
        private final NeuronVertex densityVertex;
        
        private InteractorContext() {
            // Persist interactor state at moment of contruction
            // Cache the current state, in case asynchronous changes occur
            hoveredVertex = cachedHighlightVertex;
            hoveredNeuron = cachedHighlightNeuron;
            boolean haveParent = parentIsSelected();
            parentVertex = haveParent ? cachedParentVertex : null;
            parentNeuron = haveParent ? cachedParentNeuronModel : null;
            densityVertex = densityIsHovered() ? densityCursorModel.getVertexes().iterator().next() : null;
        }
        
        public NeuronVertex getCurrentParentAnchor() {
            return parentVertex;
        }
        
        public NeuronVertex getHighlightedAnchor() {
            return hoveredVertex;
        }
        
        public NeuronVertex getHighlightedDensity() {
            return densityVertex;
        }
        
        public boolean canAppendVertex() {
            if (parentVertex == null) return false;
            if (densityVertex == null) return false;
            if (parentNeuron == null) return false;
            return true;
        }
        
        public boolean appendVertex() {
            long beginAppendTime = System.nanoTime();
            if (! canAppendVertex())
                return false;
            // OLD WAY, pre Undo: NeuronVertex addedVertex = neuron.appendVertex(parentVertex, templateVertex.getLocation(), templateVertex.getRadius());
            // First, store a link to upstream append command, to be able to handle serial undo/redo, and the resulting chain of replaced parent vertices
            VertexAdder parentAppendCmd = appendCommandForVertex.get(vtxKey(parentVertex));
            AppendNeuronVertexCommand appendCmd = new AppendNeuronVertexCommand(
                    parentNeuron, 
                    parentVertex, 
                    parentAppendCmd,
                    densityVertex.getLocation(), 
                    parentVertex.getRadius());
            long beginExecuteTime = System.nanoTime();
            if (appendCmd.execute()) {
                long endExecuteTime = System.nanoTime();
                // System.out.println("appendCmd.execute() took " + (endExecuteTime - beginExecuteTime) / 1.0e6 + " milliseconds");
                NeuronVertex addedVertex = appendCmd.getAddedVertex();
                if (addedVertex != null) {
                    selectParentVertex(addedVertex, parentNeuron);
                    // undoRedoManager.addEdit(appendCmd);
                    undoRedoManager.undoableEditHappened(new UndoableEditEvent(this, appendCmd));
                    appendCommandForVertex.put(vtxKey(addedVertex), appendCmd);
                    return true;
                }
            }
            return false;
        }
        
        public boolean canClearParent() {
            if (parentVertex == null)
                return false;
            return true;
        }
        
        public boolean clearParent() {
            if (! canClearParent())
                return false;
            return clearParentVertexAndNotify();
        }
        
        public boolean canCreateNeuron() {
            if (parentNeuron != null) return false; // must not already have an active neuron
            if (parentVertex != null) return false; // must not already have an active vertex
            if (hoveredVertex != null) return false; // must not be looking at an existing vertex
            if (densityVertex == null) return false; // must have a place to plant the seed
            if (defaultWorkspace == null) return false; // must have a workspace to place the neuron into
            return true;
        }
        
        public boolean createNeuron() {
            if (! canCreateNeuron())
                return false;
            
            // TODO: come up with a unique neuron name
            String defaultName = "Neuron 1";
            
            //  showInputDialog(Component parentComponent, Object message, String title, int messageType, Icon icon, Object[] selectionValues, Object initialSelectionValue)
            Object neuronName = JOptionPane.showInputDialog(
                    volumeProjection.getMouseableComponent(),
                    "Create new neuron here?",
                    "Create new neuron",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    null,
                    defaultName); // default button
            if (neuronName == null) {
                return false; // User pressed "Cancel"
            }
            CreateNeuronCommand cmd = new CreateNeuronCommand(
                    defaultWorkspace,
                    neuronName.toString(),
                    densityVertex.getLocation(),
                    densityVertex.getRadius());
            String errorMessage = "Failed to create neuron";
            try {
                if (cmd.execute()) {
                    log.info("Neuron created in Horta");
                    NeuronVertex addedVertex = cmd.getAddedVertex();
                    if (addedVertex != null) {
                        selectParentVertex(addedVertex, cmd.getNewNeuron());
                        undoRedoManager.undoableEditHappened(new UndoableEditEvent(this, cmd));
                        appendCommandForVertex.put(vtxKey(addedVertex), cmd);
                    }
                    return true;
                }
            }
            catch (Exception exc) {
                errorMessage += ":\n" + exc.getMessage();
            }
            JOptionPane.showMessageDialog(
                    volumeProjection.getMouseableComponent(),
                    errorMessage,
                    "Failed to create neuron",
                    JOptionPane.WARNING_MESSAGE);                
            return false;
        }
        
        public boolean canMergeNeurite() {
            if (parentNeuron == null) return false;
            if (hoveredVertex == null) return false;
            if (parentVertex == null) return false;
            if (hoveredVertex == parentVertex) return false;
            if (hoveredNeuron == parentNeuron) return false; // cannot merge a neuron with itself
            return true;
        }
        
        public boolean mergeNeurite() {
            if (!canMergeNeurite())
                return false;
            Object[] options = {"Merge", "Cancel"};
            int answer = JOptionPane.showOptionDialog(
                    volumeProjection.getMouseableComponent(),
                    String.format("Merge neurite from neuron %s\nto neurite in neuron %s?",
                            parentNeuron.getName(),
                            hoveredNeuron.getName()),
                    "Merge neurites?", 
                    JOptionPane.YES_NO_OPTION, 
                    JOptionPane.QUESTION_MESSAGE, 
                    null, 
                    options,
                    options[1]); // default button
            if (answer == JOptionPane.YES_OPTION) 
            {
                // merging is not undoable, and thus taints previous edits 
                undoRedoManager.discardAllEdits();
                
                // TODO: Create Undo-able command for mergeNeurite, and activate it from context menu
                // 3/18/2016 reverse order of merge, with respect to traditional LVV behavior
                boolean merged = parentNeuron.mergeNeurite(hoveredVertex, parentVertex);
                if (!merged) {
                    JOptionPane.showMessageDialog(
                            volumeProjection.getMouseableComponent(),
                            "merge failed",
                            "merge failed",
                            JOptionPane.WARNING_MESSAGE
                    );
                    return false;
                }
                else {
                    return true;
                }
            }
            else {
                return false;
            }
        }
        
        public boolean canSelectParent() {
            if (hoveredVertex == null) return false;
            if (hoveredNeuron == null) return false;
            return true;
        }
        
        public boolean selectParent() {
            return selectParentVertex(hoveredVertex, hoveredNeuron);
        }

        boolean canUpdateAnchorRadius() {
            if (hoveredVertex == null) return false;
            if (hoveredNeuron == null) return false;
            return true;
        }

        boolean updateAnchorRadius() {
            if (! canUpdateAnchorRadius()) {
                return false;
            }
            
            RadiusDialog radiusDialog = new RadiusDialog(hoveredNeuron, hoveredVertex);
            Object selectedValue = radiusDialog.getValue();
            int result = -1;
            if (selectedValue == null) {
                result = JOptionPane.CLOSED_OPTION;
            }
            else {
                result = Integer.parseInt(selectedValue.toString());
            }
            
            if ( result == JOptionPane.CANCEL_OPTION )
            {
                log.info("Radius dialog canceled");
                radiusDialog.revertRadiusChange();
                return false;
            }
            else if (result == JOptionPane.CLOSED_OPTION) {
                log.info("Radius dialog closed");
                radiusDialog.revertRadiusChange();
                return false;                
            }
            else {
                log.info("Radius dialog accepted");
                radiusDialog.commitRadius();
                return true;
            }
        }
    }
    
    class RadiusDialog extends JOptionPane 
    {
        private final float radiusMin = 0.10f;
        private final float radiusMax = 10.0f;
        private final float logRadiusRange = (float)Math.log(radiusMax/radiusMin);
        
        private final JSlider slider;
        private final int sliderMax = 100;
        private final JFormattedTextField radiusField; 

        private final float initialRadius;
        private float currentRadius;
        
        private final NeuronVertex anchor;
        private final NeuronModel neuron;
        
        private float radiusForSliderValue(int sliderValue) 
        {
            double intRatio = sliderValue / (double)sliderMax; // range 0-1
            double logRadius = logRadiusRange * intRatio; // range 0-logRadiusRange
            double radius = Math.exp(logRadius) * radiusMin;
            return (float)radius;
        }
        
        private int sliderValueForRadius(float radius) {
            double radiusRatio = Math.log(radius / radiusMin) / logRadiusRange; // range 0-1
            int sliderValue = (int)Math.round(radiusRatio * sliderMax);
            return sliderValue;
        }
        
        public void revertRadiusChange() {
            anchor.setRadius(initialRadius);
        }
        
        public void commitRadius() {
            if (currentRadius == initialRadius)
                return; // no change
            String errorMessage = "Failed to adjust anchor radius";
            UpdateNeuronAnchorRadiusCommand cmd = new UpdateNeuronAnchorRadiusCommand(neuron, anchor, initialRadius, currentRadius);
            try {
                if (cmd.execute()) {
                    log.info("User adjusted anchor radius in Horta");
                    undoRedoManager.undoableEditHappened(new UndoableEditEvent(this, cmd));

                    // repaint right now...
                    highlightHoverModel.getVertexUpdatedObservable().setChanged();
                    highlightHoverModel.getVertexUpdatedObservable().notifyObservers(null);
                    return;
                }
            }
            catch (Exception exc) {
                errorMessage += ":\n" + exc.getMessage();
            }
            JOptionPane.showMessageDialog(
                    volumeProjection.getMouseableComponent(),
                    errorMessage,
                    "Failed to adjust neuron anchor radius",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        public RadiusDialog(NeuronModel parentNeuron, NeuronVertex vertex) 
        {
            anchor = vertex;
            neuron = parentNeuron;
            initialRadius = vertex.getRadius();
            currentRadius = initialRadius;
            
            // Populate a temporary little neuron model, to display current radius before comitting
            anchorEditModel.getVertexes().clear();
            anchorEditModel.getVertexes().add(anchor);
            // Also add adjacent anchors TODO:
            /*
            for (NeuronEdge edge : neuron.getEdges()) {
                for (NeuronVertex a : edge) {
                    if (a == anchor)
                }
            }
            */
            anchorEditModel.getVertexUpdatedObservable().setChanged();
            anchorEditModel.getVertexUpdatedObservable().notifyObservers(new VertexWithNeuron(anchor, anchorEditModel));
            
            slider = new JSlider();
            slider.setMaximum(sliderMax);
            slider.setValue(sliderValueForRadius(currentRadius));

            slider.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    int sliderValue = slider.getValue();
                    int oldValue = sliderValueForRadius(currentRadius);
                    
                    if (oldValue == sliderValue) {
                        return; // no (significant) change
                    }
                    
                    float newRadius = radiusForSliderValue(sliderValue);
                    
                    int sanityCheck = sliderValueForRadius(newRadius);
                    if (sliderValue != sanityCheck) {
                        log.error("Radius slider value {} diverged to {} with radius {}", sliderValue, sanityCheck, newRadius);
                    }
                    // log.info("Radius visually adjusted to {} micrometers", newRadius);
                    
                    anchor.setRadius(currentRadius);
                    
                    // Update the display only, by signalling change to the model, but not to the neuron (yet)
                    anchorEditModel.getVertexUpdatedObservable().setChanged();
                    anchorEditModel.getVertexUpdatedObservable().notifyObservers(new VertexWithNeuron(anchor, anchorEditModel));
                    
                    currentRadius = newRadius;
                    radiusField.setValue(newRadius);
                }
            });
            
            NumberFormat radiusFormat = new DecimalFormat("#.##"); 
            radiusField = new JFormattedTextField(radiusFormat);
            radiusField.setValue(currentRadius);
            radiusField.addPropertyChangeListener("value", new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    float newRadius = Float.parseFloat(radiusField.getValue().toString());
                    if (newRadius < radiusMin)
                        return;
                    if (newRadius > radiusMax)
                        return;
                    int sliderValue = sliderValueForRadius(newRadius);
                    if (sliderValue == slider.getValue())
                        return;
                    slider.setValue(sliderValue);
                }
            });
            
            setMessage(new Object[] {
                "Adjust Radius for Neuron Anchor",
                slider,
                radiusField
            });
            setOptionType(JOptionPane.OK_CANCEL_OPTION);
            
            JDialog dialog = createDialog("Adjust Radius");
            dialog.setVisible(true);
            
            // Turn off editing model after dialog is done displaying
            anchorEditModel.getVertexes().clear();
            anchorEditModel.getEdges().clear();
            anchorEditModel.getVertexesRemovedObservable().setChanged();
            anchorEditModel.getVertexesRemovedObservable().notifyObservers(null);
        }
    }

}