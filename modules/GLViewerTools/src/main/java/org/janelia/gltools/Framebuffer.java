
package org.janelia.gltools;

import java.util.ArrayList;
import java.util.List;
import javax.media.opengl.DebugGL3;
import javax.media.opengl.GL;
import javax.media.opengl.GL3;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Offscreen buffer for rendering OpenGL images
 * @author Christopher Bruns <brunsc at janelia.hhmi.org>
 */
public class Framebuffer implements GL3Resource, GLEventListener {
    private int frameBufferHandle = 0;
    private int width, height;
    private final List<RenderTarget> renderTargets = new ArrayList<RenderTarget>();
    private boolean needsResize = false;
    private GLAutoDrawable target;
    private final float[] clearColor4 = new float[] {0,0,0,0};
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public Framebuffer(GLAutoDrawable target) { 
        this.target = target;
        target.addGLEventListener(this);
        reshape(target.getSurfaceWidth(), target.getSurfaceHeight());
    }

    public RenderTarget addRenderTarget(int internalFormat, int attachment) {
        RenderTarget result = new RenderTarget(width, height, internalFormat, attachment);
        renderTargets.add(result);
        return result;
    }
    
    public RenderTarget addMsaaRenderTarget(int internalFormat, int attachment, int num_samples) {
        RenderTarget result = new MsaaRenderTarget(width, height, internalFormat, attachment, num_samples);
        renderTargets.add(result);
        return result;
    }
    
    public boolean bind(GL3 gl) {
        return bind(gl, GL3.GL_FRAMEBUFFER); // default to both read and write
    }

    public boolean bind(GL3 gl, int readWrite) {
        if (frameBufferHandle == 0)
            init(gl);
        if (frameBufferHandle == 0)
            return false;

        if ( (width != target.getSurfaceWidth()) || (height != target.getSurfaceHeight()) )
            reshape(target.getSurfaceWidth(), target.getSurfaceHeight());
        
        gl.glBindFramebuffer(readWrite, frameBufferHandle);
        int framebufferStatus = gl.glCheckFramebufferStatus(GL3.GL_FRAMEBUFFER);
        if(framebufferStatus != GL3.GL_FRAMEBUFFER_COMPLETE) {
            if (framebufferStatus == GL.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT)
                logger.error("GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT");
            if (framebufferStatus == GL.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS)
                logger.error("GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS");
            if (framebufferStatus == GL.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT)
                logger.error("GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT");
            if (framebufferStatus == GL.GL_FRAMEBUFFER_UNSUPPORTED)
                logger.error("GL_FRAMEBUFFER_UNSUPPORTED");
            return false; // TODO better error handling            
        }
        if (needsResize) {
            for (RenderTarget rt : renderTargets) {
                rt.reshape(gl, width, height);
                rt.bind(gl);
                gl.glFramebufferTexture2D(readWrite, rt.getAttachment(),
                        rt.getTextureTarget(),
                        rt.getHandle(), 0);
                rt.unbind(gl);
            }
            needsResize = false;
        }
        return true;
    }
    
    @Override
    public void dispose(GL3 gl) {
        if (frameBufferHandle != 0) {
            int[] vals = {frameBufferHandle};
            gl.glDeleteFramebuffers(1, vals, 0);
            frameBufferHandle = 0;
        }
        for (RenderTarget rt : renderTargets) {
            rt.dispose(gl);
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public RenderTarget getRenderTarget(int attachment) {
        for (RenderTarget rt : renderTargets) {
            if (rt.getAttachment() == attachment)
                return rt;
        }
        return null;
    }

    @Override
    public void init(GL3 gl) {
        // TODO - avoid premature initialization
        if (width*height == 0)
            return;
        // DebugGL3 gl = new DebugGL3(gl0);
        if (frameBufferHandle == 0) {
            int[] vals = new int[1];
            gl.glGenFramebuffers(1, vals, 0);
            frameBufferHandle = vals[0];
        }
        if (frameBufferHandle == 0) 
            return;
        gl.glBindFramebuffer(GL3.GL_FRAMEBUFFER, frameBufferHandle);
        for (RenderTarget rt : renderTargets) {
            rt.reshape(gl, width, height);
            rt.init(gl);
            rt.bind(gl);
            // rt.clear(gl); // Causes problems on ATI
            gl.glFramebufferTexture2D(GL3.GL_FRAMEBUFFER, rt.getAttachment(), 
                    rt.getTextureTarget(),
                    rt.getHandle(), 0);
        }
        // TODO - parameterize draw attachments
        int[] drawBuffers = new int[] {GL3.GL_COLOR_ATTACHMENT0};
        gl.glDrawBuffers(drawBuffers.length, drawBuffers, 0);
        clear(gl); // Sometimes required on Mac to avoid fb corruption
        unbind(gl);
    }
    
    private void clear(GL3 gl) {
        // Attempt to blank the color buffer
        gl.glClearBufferfv(GL3.GL_COLOR, 0, clearColor4, 0);
    }

    public final boolean reshape(int w, int h) {
        // System.out.println("Framebuffer.reshape(): "+w+", "+h);
        if ( (w == width ) && (h == height) )
            return false;
        for (RenderTarget rt : renderTargets)
            rt.setDirty(true); // Forces repaint during resizing
        width = w;
        height = h;
        if (renderTargets.size() > 0)
            needsResize = true;
        // System.out.println("Framebuffer.reshape() flagged");
        return true;
    }
    
    public void unbind(GL3 gl) {
        gl.glBindFramebuffer(GL3.GL_FRAMEBUFFER, 0);
    }

    // GLEventListener interface...
    
    @Override
    public void init(GLAutoDrawable glad) {
        reshape(glad.getSurfaceWidth(), glad.getSurfaceHeight());
        GL3 gl = new DebugGL3(glad.getGL().getGL3());
        init(gl);
    }

    @Override
    public void dispose(GLAutoDrawable glad) {
        GL3 gl = new DebugGL3(glad.getGL().getGL3());
        init(gl);
        dispose(gl);
    }

    @Override
    public void display(GLAutoDrawable glad) {
        // display is delegated to containing actor
    }

    @Override
    public void reshape(GLAutoDrawable glad, int x, int y, int w, int h) {
        if ( ! reshape(w, h) ) return;
        GL3 gl = new DebugGL3(glad.getGL().getGL3());
        // GL3 gl = glad.getGL().getGL3();
        bind(gl);
        for (RenderTarget rt : renderTargets) {
            rt.reshape(gl, w, h);
            rt.bind(gl);
            // rt.clear(gl);
            gl.glFramebufferTexture2D(GL3.GL_FRAMEBUFFER, rt.getAttachment(), 
                    rt.getTextureTarget(),
                    rt.getHandle(), 0);
            rt.unbind(gl);
        }
        clear(gl);
        unbind(gl);
    }
    
    private static class MsaaRenderTarget extends RenderTarget {
        private final int num_samples;

        public MsaaRenderTarget(int width, int height, int internalFormat, int attachment, int num_samples)
        {
            super(width, height, internalFormat, attachment);
            useReadParameters = false; // ordinary reading of MSAA texture is not permitted.
            this.num_samples = num_samples;
            textureTarget = GL3.GL_TEXTURE_2D_MULTISAMPLE;
        }
        
        @Override
        protected void allocateTextureStorage(GL3 gl, int mipmapCount) {
            //
            int[] max_samples = {1, 1, 1, 1};
            gl.glGetIntegerv(GL3.GL_MAX_SAMPLES, max_samples, 0);
            // gl.glGetIntegerv(GL3.GL_MAX_COLOR_TEXTURE_SAMPLES, max_samples, 1);
            // gl.glGetIntegerv(GL3.GL_MAX_DEPTH_TEXTURE_SAMPLES, max_samples, 2);
            // TODO: Smaller number for GL_MAX_INTEGER_SAMPLES
            // is a problem for simultaneously using MSAA and integer pick buffer.
            // gl.glGetIntegerv(GL3.GL_MAX_INTEGER_SAMPLES, max_samples, 3);
            int samples = Math.min(max_samples[0], num_samples);
            gl.glTexImage2DMultisample(
                    textureTarget,
                    samples,
                    internalFormat, 
                    width, height,
                    false);
        }

    }

}
