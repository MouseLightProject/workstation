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

package org.janelia.horta.actors;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import javax.imageio.ImageIO;
import javax.media.opengl.GL3;
import org.janelia.console.viewerapi.model.ChannelColorModel;
import org.janelia.console.viewerapi.model.ImageColorModel;
import org.janelia.geometry3d.AbstractCamera;
// import org.janelia.geometry3d.ChannelBrightnessModel;
import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.PerspectiveCamera;
import org.janelia.gltools.BasicShaderProgram;
import org.janelia.gltools.MeshActor;
import org.janelia.gltools.ShaderStep;
import org.janelia.gltools.material.BasicMaterial;
import org.janelia.gltools.material.DepthSlabClipper;
import org.janelia.gltools.texture.Texture2d;
import org.janelia.horta.ktx.KtxData;
import org.openide.util.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Material for tetrahedral volume rendering.
 * @author Christopher Bruns
 */
public class TetVolumeMaterial extends BasicMaterial
implements DepthSlabClipper
{
    private int volumeTextureHandle = 0;
    private int colorMapTextureHandle = 0;
    private final KtxData ktxData;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private IntBuffer pbos;
    private float zNearRelative = 0.10f;
    private float zFarRelative = 100.0f; // relative z clip planes
    private final float[] zNearFar = new float[] {0.1f, 100.0f}; // absolute clip for shader
    // multichannel brightness
    private final ImageColorModel brightnessModel;
    private Texture2d colorMapTexture = new Texture2d();

    public TetVolumeMaterial(KtxData ktxData, ImageColorModel brightnessModel) {
        this.ktxData = ktxData;
        shaderProgram = new TetVolumeShader();
        this.brightnessModel = brightnessModel;
        
        BufferedImage colorMapImage = null;
        try {
            colorMapImage = ImageIO.read(
                    getClass().getResourceAsStream(
                            "/org/janelia/horta/images/"
                            + "HotColorMap.png"));
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        colorMapTexture.loadFromBufferedImage(colorMapImage);
        colorMapTexture.setGenerateMipmaps(false);
        colorMapTexture.setMinFilter(GL3.GL_LINEAR);
        colorMapTexture.setMagFilter(GL3.GL_LINEAR);

    }
    
    // Override displayMesh() to display something other than triangles
    @Override
    protected void displayMesh(GL3 gl, MeshActor mesh, AbstractCamera camera, Matrix4 modelViewMatrix) {
        mesh.displayTriangleAdjacencies(gl);
    }

    @Override
    public void dispose(GL3 gl) {
        super.dispose(gl);
        gl.glDeleteTextures(2, new int[] {volumeTextureHandle, colorMapTextureHandle}, 0);
        volumeTextureHandle = 0;
        colorMapTextureHandle = 0;
    }
    
    @Override
    public boolean hasPerFaceAttributes() {
        return false;
    }
    
    private static int mipmapSize(long level, long baseSize) {
        int result = (int)Math.max(1, Math.floor(baseSize/(Math.pow(2,level))));
        return result;
    }
    
    @Override
    public void init(GL3 gl) 
    {
        super.init(gl);
        
        // Volume texture
        int[] h = {0, 0};
        gl.glGenTextures(2, h, 0);
        volumeTextureHandle = h[0];
        colorMapTextureHandle = h[1];

        gl.glActiveTexture(GL3.GL_TEXTURE0);
        gl.glBindTexture(GL3.GL_TEXTURE_3D, volumeTextureHandle);

        gl.glPixelStorei(GL3.GL_UNPACK_ALIGNMENT, 1); // TODO: Verify that this fits data
        
        // TODO: Test and verify endian parity behavior
        /* */
        if (ktxData.header.byteOrder == ByteOrder.nativeOrder()) {
            gl.glPixelStorei(GL3.GL_UNPACK_SWAP_BYTES, GL3.GL_FALSE);
        }
        else {
            gl.glPixelStorei(GL3.GL_UNPACK_SWAP_BYTES, GL3.GL_TRUE);
        }
        /*  */
        
        int glInternalFormat = ktxData.header.glInternalFormat;
        // Work around problem with my initial 2-channel KTX files...
        // Future versions should be OK and won't need this hack.
        if (glInternalFormat == GL3.GL_RG16UI)
            glInternalFormat = GL3.GL_RG16;
        if (glInternalFormat == GL3.GL_RG8UI)
            glInternalFormat = GL3.GL_RG8;
        
        gl.glTexParameteri(GL3.GL_TEXTURE_3D, GL3.GL_TEXTURE_MAG_FILTER, GL3.GL_NEAREST);
        gl.glTexParameteri(GL3.GL_TEXTURE_3D, GL3.GL_TEXTURE_MIN_FILTER, GL3.GL_NEAREST_MIPMAP_NEAREST);
        gl.glTexParameteri(GL3.GL_TEXTURE_3D, GL3.GL_TEXTURE_WRAP_S, GL3.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL3.GL_TEXTURE_3D, GL3.GL_TEXTURE_WRAP_T, GL3.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL3.GL_TEXTURE_3D, GL3.GL_TEXTURE_WRAP_R, GL3.GL_CLAMP_TO_EDGE);

        // Use pixel buffer objects for asynchronous transfer
        
        // Phase 1: Allocate pixel buffer objects (in GL thread)
        final boolean usePixelBufferObjects = false; // false is faster
        long t0 = System.nanoTime();
        long t1 = t0;
        if (usePixelBufferObjects) {
            int mapCount = ktxData.header.numberOfMipmapLevels;
            pbos = IntBuffer.allocate(mapCount);
            gl.glGenBuffers(mapCount, pbos);
            for(int mipmapLevel = 0; mipmapLevel < ktxData.header.numberOfMipmapLevels; ++mipmapLevel)
            {
                ByteBuffer buf1 = ktxData.mipmaps.get(mipmapLevel);
                buf1.rewind();
                gl.glBindBuffer(GL3.GL_PIXEL_UNPACK_BUFFER, pbos.get(mipmapLevel));
                gl.glBufferData(GL3.GL_PIXEL_UNPACK_BUFFER, buf1.capacity(), buf1, GL3.GL_STREAM_DRAW);
            }
            t1 = System.nanoTime();
            logger.info("Creating pixel buffer objects took "+(t1-t0)/1.0e9+" seconds");
        }
        
        final boolean useStorageSubimage = true; // true is much faster
        if (useStorageSubimage) {
            gl.glTexStorage3D(GL3.GL_TEXTURE_3D,
                    ktxData.header.numberOfMipmapLevels,
                    glInternalFormat,
                    ktxData.header.pixelWidth,
                    ktxData.header.pixelHeight,
                    ktxData.header.pixelDepth);
        }

        // Phase 2: Initiate loading of texture to GPU (in GL thread)
        for(int mipmapLevel = 0; mipmapLevel < ktxData.header.numberOfMipmapLevels; ++mipmapLevel)
        {
            // logger.info("GL Error: " + gl.glGetError());
            int mw = mipmapSize(mipmapLevel, ktxData.header.pixelWidth);
            int mh = mipmapSize(mipmapLevel, ktxData.header.pixelHeight);
            int md = mipmapSize(mipmapLevel, ktxData.header.pixelDepth);
            if (usePixelBufferObjects) {
                gl.glBindBuffer(GL3.GL_PIXEL_UNPACK_BUFFER, pbos.get(mipmapLevel));
                gl.glTexImage3D(
                        GL3.GL_TEXTURE_3D,
                        mipmapLevel,
                        glInternalFormat,
                        mw,
                        mh,
                        md,
                        0, // border
                        ktxData.header.glFormat,
                        ktxData.header.glType,
                        0); // zero means read from PBO
            }
            else {
                ByteBuffer buf1 = ktxData.mipmaps.get(mipmapLevel);
                buf1.rewind();
                if (useStorageSubimage) {
                    gl.glTexSubImage3D(
                            GL3.GL_TEXTURE_3D,
                            mipmapLevel,
                            0, 0, 0,// offsets
                            mw, mh, md,
                            ktxData.header.glFormat,
                            ktxData.header.glType,
                            buf1);
                }
                else {
                    gl.glTexImage3D(
                            GL3.GL_TEXTURE_3D,
                            mipmapLevel,
                            glInternalFormat,
                            mw,
                            mh,
                            md,
                            0, // border
                            ktxData.header.glFormat,
                            ktxData.header.glType,
                            buf1);
                }
            }
        }
        gl.glBindBuffer(GL3.GL_PIXEL_UNPACK_BUFFER, 0);
        long t2 = System.nanoTime();
        logger.info("Uploading tetrahedral volume texture to GPU took "+(t2-t1)/1.0e9+" seconds");
        
        // Phase 3: Use the texture in draw calls, after some delay... TODO:
    }
    
    @Override
    public void load(GL3 gl, AbstractCamera camera) {
        super.load(gl, camera);
        // 3D volume texture
        gl.glActiveTexture(GL3.GL_TEXTURE0);
        gl.glBindTexture(GL3.GL_TEXTURE_3D, volumeTextureHandle);
        // Z-clip planes
        float focusDistance = ((PerspectiveCamera)camera).getCameraFocusDistance();
        zNearFar[0] = zNearRelative * focusDistance;
        zNearFar[1] = zFarRelative * focusDistance;
        final int zNearFarUniformIndex = 2; // explicitly set in shader
        gl.glUniform2fv(zNearFarUniformIndex, 1, zNearFar, 0);
        // Brightness correction
        if (brightnessModel.getChannelCount() == 2) {
            // Use a multichannel model
            ChannelColorModel c0 = brightnessModel.getChannel(0);
            ChannelColorModel c1 = brightnessModel.getChannel(1);
            float max0 = c0.getDataMax();
            gl.glUniform2fv(3, 1, new float[] {c0.getBlackLevel()/max0, c1.getBlackLevel()/max0}, 0);
            gl.glUniform2fv(4, 1, new float[] {c0.getWhiteLevel()/max0, c1.getWhiteLevel()/max0}, 0);
            gl.glUniform2fv(5, 1, new float[] {(float)c0.getGamma(), (float)c1.getGamma()}, 0);
        }
        else {
            throw new UnsupportedOperationException("Unexpected number of color channels");
        }
        // Color map
        colorMapTexture.bind(gl, 1); // texture unit 1
    }
    
    @Override
    public boolean usesNormals() {
        return false;
    }

    @Override
    public void setOpaqueDepthTexture(Texture2d opaqueDepthTexture) {
        // TODO: not yet used
    }

    @Override
    public void setRelativeSlabThickness(float zNear, float zFar) {
        zNearRelative = zNear;
        zFarRelative = zFar;
    }
    
    public static class TetVolumeShader extends BasicShaderProgram
    {
        public TetVolumeShader()
        {
            try {
                getShaderSteps().add(new ShaderStep(GL3.GL_VERTEX_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/horta/shader/"
                                        + "TetVolumeVrtx330.glsl"))
                );
                getShaderSteps().add(new ShaderStep(GL3.GL_GEOMETRY_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/horta/shader/"
                                        + "TetVolumeGeom330.glsl"))
                );
                getShaderSteps().add(new ShaderStep(GL3.GL_FRAGMENT_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/horta/shader/"
                                        + "TetVolumeFrag330.glsl"))
                );
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }        
        }
    }
}