
package org.janelia.gltools.material;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GL3;
import org.janelia.geometry3d.AbstractCamera;
import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.PerspectiveCamera;
import org.janelia.gltools.BasicShaderProgram;
import org.janelia.gltools.MeshActor;
import org.janelia.gltools.ShaderStep;
import org.janelia.gltools.texture.Texture2d;
import org.openide.util.Exceptions;

/**
 *
 * @author Christopher Bruns <brunsc at janelia.hhmi.org>
 */
public class ImageParticleMaterial extends BasicMaterial {
    private Texture2d particleTexture;
    private int sphereTextureIndex = -1;
    private int particleScaleIndex = -1;
    
    private int diffuseColorIndex = -1;
    private int specularColorIndex = -1;
    private float[] diffuseColor = new float[] {1, 0, 1, 1};
    private float[] specularColor = new float[] {0, 0, 0, 0};    

    public ImageParticleMaterial(Texture2d particleTexture) {
        initialize(particleTexture);
    }
    
    public ImageParticleMaterial(BufferedImage particleImage) {
        particleTexture = new Texture2d();
        particleTexture.loadFromBufferedImage(particleImage);
        initialize(particleTexture);
    }
    
    private void initialize(Texture2d particleTexture) {
        shaderProgram = new ImageParticleShader();
        this.particleTexture = particleTexture;
    }

    // Override displayMesh() to display something other than triangles
    @Override
    protected void displayMesh(GL3 gl, MeshActor mesh, AbstractCamera camera, Matrix4 modelViewMatrix) {
        gl.glEnable(GL3.GL_VERTEX_PROGRAM_POINT_SIZE); // important with my latest Windows nvidia driver 10/20/2014
        mesh.displayParticles(gl);
    }
    
    @Override
    public void dispose(GL3 gl) {
        super.dispose(gl);
        sphereTextureIndex = -1;
        particleScaleIndex = -1;
        diffuseColorIndex = -1;
        specularColorIndex = -1;
        particleTexture.dispose(gl);
    }
    
    @Override
    public boolean hasPerFaceAttributes() {
        return false;
    }

    @Override
    public void init(GL3 gl) {
        super.init(gl);
        particleTexture.init(gl);
        sphereTextureIndex = gl.glGetUniformLocation(
            shaderProgram.getProgramHandle(),
            "sphereTexture");
        particleScaleIndex = gl.glGetUniformLocation(
            shaderProgram.getProgramHandle(),
            "particleScale");
        diffuseColorIndex = gl.glGetUniformLocation(
            shaderProgram.getProgramHandle(),
            "diffuseColor");
        specularColorIndex = gl.glGetUniformLocation(
            shaderProgram.getProgramHandle(),
            "specularColor");       
        // System.out.println("sphereTextureIndex = "+sphereTextureIndex);
    }

    @Override
    public void load(GL3 gl, AbstractCamera camera) {
        if (particleScaleIndex == 0) 
            init(gl);
        super.load(gl, camera);
        int textureUnit = 0;
        particleTexture.bind(gl, textureUnit);
        gl.glUniform1i(sphereTextureIndex, textureUnit);
        float particleScale = 1.0f; // in pixels
        if (camera instanceof PerspectiveCamera) {
            PerspectiveCamera pc = (PerspectiveCamera)camera;
            particleScale = 0.5f * pc.getViewport().getHeightPixels()
                    / (float)Math.tan(0.5 * pc.getFovRadians());
        }
        // System.out.println("Particle scale = "+particleScale);
        gl.glUniform1f(particleScaleIndex, particleScale);
        gl.glUniform4fv(diffuseColorIndex, 1, diffuseColor, 0);
        gl.glUniform4fv(specularColorIndex, 1, specularColor, 0);
    }
    
    @Override
    public boolean usesNormals() {
        return false;
    }

    public void setDiffuseColor(Color color) {
        diffuseColor[0] = color.getRed()/255f;
        diffuseColor[1] = color.getGreen()/255f;
        diffuseColor[2] = color.getBlue()/255f;
        diffuseColor[3] = color.getAlpha()/255f;
    }

    public void setSpecularColor(Color color) {
        specularColor[0] = color.getRed()/255f;
        specularColor[1] = color.getGreen()/255f;
        specularColor[2] = color.getBlue()/255f;
        specularColor[3] = color.getAlpha()/255f;
    }

    public Color getColor()
    {
        return new Color(diffuseColor[0], diffuseColor[1], diffuseColor[2]);
    }

    private static class ImageParticleShader extends BasicShaderProgram {

        public ImageParticleShader() {
            try {
                getShaderSteps().add(new ShaderStep(GL2ES2.GL_VERTEX_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/gltools/material/shader/"
                                        + "ImageParticleVrtx.glsl"))
                );
                getShaderSteps().add(new ShaderStep(GL2ES2.GL_FRAGMENT_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/gltools/material/shader/"
                                        + "ImageParticleFrag.glsl"))
                );
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }        
        }
    }
    
}
