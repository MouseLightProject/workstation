package org.janelia.it.workstation.gui.geometric_search.search;

import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.Rotation;
import org.janelia.it.workstation.gui.framework.outline.Refreshable;

import javax.media.opengl.GL3;
import javax.swing.*;

import org.janelia.it.workstation.gui.geometric_search.gl.*;
import org.janelia.it.workstation.gui.geometric_search.viewer.GL3Viewer;
import org.janelia.it.workstation.gui.viewer3d.Mip3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;


/**
 * Created by murphys on 3/9/15.
 */
public class GeometricSearchPanel extends JPanel implements Refreshable {

    private final Logger logger = LoggerFactory.getLogger(GeometricSearchPanel.class);
    GL3Viewer viewer;

    @Override
    public void refresh() {

        if ( viewer == null ) {
            createGL3Viewer();
        }

        viewer.refresh();
    }

    @Override
    public void totalRefresh() {
        refresh();
    }

    private void createGL3Viewer() {

        if ( viewer != null ) {
            viewer.releaseMenuActions();
        }
        viewer = new GL3Viewer();
        viewer.setPreferredSize(new Dimension(1200, 900));
        viewer.setVisible(true);
        viewer.setResetFirstRedraw(true);

        GL3ShaderActionSequence actionSequence = new GL3ShaderActionSequence("Experimental Shader Action Sequence");

        setupMeshExperiment(actionSequence);

        logger.info("Adding glSequence...");
        viewer.addShaderAction(actionSequence);

        add(viewer, BorderLayout.CENTER);

    }

    public void displayReady() {
        if (viewer==null) {
            createGL3Viewer();
        }
        viewer.resetView();
        viewer.refresh();
    }

    private void setupMeshExperiment(GL3ShaderActionSequence actionSequence) {
        final MeshObjFileV2Shader shader = new MeshObjFileV2Shader();

        shader.setUpdateCallback(new GLDisplayUpdateCallback() {
            @Override
            public void update(GL3 gl) {
                Matrix4 viewMatrix=viewer.getRenderer().getViewMatrix();
                shader.setView(gl, viewMatrix);
                Matrix4 projMatrix=viewer.getRenderer().getProjectionMatrix();
                shader.setProjection(gl, projMatrix);
            }
        });

        final MeshObjFileV2Actor meshActor1 = new MeshObjFileV2Actor(new File("/Users/murphys/meshes/compartment_62.obj"));

        meshActor1.setUpdateCallback(new GLDisplayUpdateCallback() {
            @Override
            public void update(GL3 gl) {
                Matrix4 actorModel = meshActor1.getModel();
                shader.setModel(gl, actorModel);
            }
        });

        final MeshObjFileV2Actor meshActor2 = new MeshObjFileV2Actor(new File("/Users/murphys/meshes/compartment_39.obj"));

        meshActor2.setUpdateCallback(new GLDisplayUpdateCallback() {
            @Override
            public void update(GL3 gl) {
                Matrix4 actorModel = meshActor2.getModel();
                shader.setModel(gl, actorModel);
            }
        });

        actionSequence.setShader(shader);
        actionSequence.getActorSequence().add(meshActor1);
        actionSequence.getActorSequence().add(meshActor2);
    }

}
