package org.janelia.it.workstation.ab2.actor;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.media.opengl.GL4;

import org.janelia.geometry3d.Vector2;
import org.janelia.geometry3d.Vector4;
import org.janelia.it.workstation.ab2.controller.AB2Controller;
import org.janelia.it.workstation.ab2.event.AB2TextLabelClickEvent;
import org.janelia.it.workstation.ab2.gl.GLAbstractActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TextLabelActor extends GLAbstractActor {

    private final Logger logger = LoggerFactory.getLogger(TextLabelActor.class);

    Vector2 v0;
    Vector2 v1; // computed
    String text;
    Vector4 textColor;
    Vector4 backgroundColor;

    IntBuffer vertexArrayId=IntBuffer.allocate(1);
    IntBuffer vertexBufferId=IntBuffer.allocate(1);
    FloatBuffer vertexFb;

    IntBuffer imageTextureId=IntBuffer.allocate(1);
    BufferedImage bufferedImage;

    static BufferedImage textResourceImage;

    int labelImageWidth;
    int labelImageHeight;

    // Load the resource image once
    static {
        try {
            textResourceImage=GLAbstractActor.getImageByFilename("UbuntuFont.png");
        } catch (Exception ex) {
            ex.printStackTrace();
            textResourceImage=null;
        }
    }

    static final int UBUNTU_FONT_LEADING_OFFSET=10;
    static final int UBUNTU_FONT_BOTTOM_OFFSET=10;
    static final int UBUNTU_FONT_UNIT_WIDTH=9;
    static final int UBUNTU_FONT_UNIT_HEIGHT=16;
    static final int UBUNTU_FONT_THRESHOLD=230;

    static final String UBUNTU_FONT_STRING="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789)!@#$%^&*(-_+=[]{};:\'\""+
            ","+".<>"+"/?"+"\\"+"|"+"`~";

    public TextLabelActor(int actorId,
                          String text,
                          Vector2 v0,
                          Vector4 textColor,
                          Vector4 backgroundColor) {
        this.actorId=actorId;
        this.v0=v0;
        this.text=text;
        this.textColor=textColor;
        this.backgroundColor=backgroundColor;
    }

    public Vector4 getTextColor() { return textColor; }

    public Vector4 getBackgroundColor() { return backgroundColor; }

    public void setTextColor(Vector4 textColor) { this.textColor=textColor; }

    public void setBackgroundColor(Vector4 backgroundColor) { this.backgroundColor=backgroundColor; }

    public void setPosition(Vector2 position) { this.v0 = position; }

    @Override
    public boolean isTwoDimensional() { return true; }

    @Override
    public void init(GL4 gl) {
        if (this.mode == Mode.DRAW) {

            byte[] labelPixels=createTextImage();

            int screenWidth=AB2Controller.getController().getGljPanel().getSurfaceWidth();
            int screenHeight=AB2Controller.getController().getGljPanel().getSurfaceHeight();
            float imageNormalWidth=(float)((labelImageWidth*1.0)/screenWidth)*2.0f;
            float imageNormalHeight=(float)((labelImageHeight*1.0)/screenHeight)*2.0f;
            logger.info("imageNormalWidth="+imageNormalWidth);
            logger.info("imageNormalHeight="+imageNormalHeight);
            v1=new Vector2(v0.get(0)+imageNormalWidth, v0.get(1)+imageNormalHeight);

            // This combines positional vertices interleaved with 2D texture coordinates
            float[] vertexData = {

                    v0.get(0), v0.get(1), 0f,    0f, 0f, // lower left
                    v1.get(0), v0.get(1), 0f,    1f, 0f, // lower right
                    v0.get(0), v1.get(1), 0f,    0f, 1f, // upper left

                    v1.get(0), v0.get(1), 0f,    1f, 0f, // lower right
                    v1.get(0), v1.get(1), 0f,    1f, 1f, // upper right
                    v0.get(0), v1.get(1), 0f,    0f, 1f  // upper left
            };

            vertexFb=createGLFloatBuffer(vertexData);

            gl.glGenVertexArrays(1, vertexArrayId);
            gl.glBindVertexArray(vertexArrayId.get(0));
            gl.glGenBuffers(1, vertexBufferId);
            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vertexBufferId.get(0));
            gl.glBufferData(GL4.GL_ARRAY_BUFFER, vertexFb.capacity() * 4, vertexFb, GL4.GL_STATIC_DRAW);
            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);

            logger.info("pixel count="+labelPixels.length);

            // Create texture
            gl.glGenTextures(1, imageTextureId);
            gl.glBindTexture(GL4.GL_TEXTURE_2D, imageTextureId.get(0));

            //ByteBuffer byteBuffer = ByteBuffer.allocateDirect(pixels.length);
            //byteBuffer.wrap(pixels);

            ByteBuffer byteBuffer=ByteBuffer.allocate(labelPixels.length);
            for (int i=0;i<labelPixels.length;i++) {
                byteBuffer.put(i, labelPixels[i]);
            }

            int sizeCheck=labelImageWidth*labelImageHeight;
            logger.info("labelPixels.length="+labelPixels.length+" labelImageWidth="+labelImageWidth+" labelImageHeight="+labelImageHeight+" sizeCheck="+sizeCheck);

            byteBuffer.rewind();
            gl.glTexImage2D(GL4.GL_TEXTURE_2D,0, GL4.GL_RGBA, labelImageWidth, labelImageHeight,0,
                    GL4.GL_RGBA, GL4.GL_UNSIGNED_BYTE, byteBuffer);
            checkGlError(gl, "Uploading texture");
            gl.glTexParameteri( GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MIN_FILTER, GL4.GL_NEAREST );
            gl.glTexParameteri( GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MAG_FILTER, GL4.GL_NEAREST );
            gl.glBindTexture(GL4.GL_TEXTURE_2D, 0);

        } else if (this.mode == Mode.PICK) {
            if (pickIndex<0) {
                pickIndex = AB2Controller.getController().getNextPickIndex();
                AB2Controller.getController().setPickEvent(pickIndex, new AB2TextLabelClickEvent(this));
                logger.info("Setting pickIndex="+pickIndex);
            }
        }

    }

    protected byte[] createTextImage() {
        // Step 1: get contents
        int labelLength=text.length();
        byte zeroByte=0;
        byte oneByte=1;
        int characterPositions[] = new int[labelLength];
        for (int i=0;i<labelLength;i++) {
            char c=text.charAt(i);
            int j=0;
            for (;j<UBUNTU_FONT_STRING.length();j++) {
                if (c==UBUNTU_FONT_STRING.charAt(j)) {
                    break;
                }
            }
            if (j==UBUNTU_FONT_STRING.length()) {
                characterPositions[i]=-1; // unknown - space by convention
            } else {
                characterPositions[i]=j;
            }
        }
        // Step 2: allocate new image
        int wPad=UBUNTU_FONT_UNIT_WIDTH/2;
        int hPad=UBUNTU_FONT_UNIT_HEIGHT/4;
        int w=labelLength*UBUNTU_FONT_UNIT_WIDTH + wPad*2;
        int h=UBUNTU_FONT_UNIT_HEIGHT + hPad*2;
        byte labelPixels[]=new byte[w*h*4];
        int sourceHeight=textResourceImage.getHeight();
        int sourceWidth=textResourceImage.getWidth();
        int sourceHeightOffset=sourceHeight-(UBUNTU_FONT_UNIT_HEIGHT+UBUNTU_FONT_BOTTOM_OFFSET);
        logger.info("sourceWidth="+sourceWidth+" sourceHeight="+sourceHeight+" sourceHeightOffset="+sourceHeightOffset);
        int textPixelCount=0;
        for (int i=0;i<labelLength;i++) {
            int cp=characterPositions[i];
            if (cp>-1) {
                for (int y = 0; y < UBUNTU_FONT_UNIT_HEIGHT; y++) {
                    for (int x = 0; x < UBUNTU_FONT_UNIT_WIDTH; x++) {
                        int sX = UBUNTU_FONT_LEADING_OFFSET + cp * UBUNTU_FONT_UNIT_WIDTH + x;
                        int sY = sourceHeight - (sourceHeightOffset + y + 1);
                        int tX = wPad + UBUNTU_FONT_UNIT_WIDTH * i + x;
                        int tY = hPad + y;
                        if (sX<sourceWidth && sY<sourceHeight) {
                            int resourceRGB = textResourceImage.getRGB(sX, sY);
                            //logger.info("resourceRGB="+resourceRGB+" sX="+sX+" sY="+sY);
                            byte a = (byte) (resourceRGB >>> 24); // ignore this byte
                            byte r = (byte) (resourceRGB >>> 16);
                            byte g = (byte) (resourceRGB >>> 8);
                            byte b = (byte) (resourceRGB);
                            //logger.info("a="+a+" r="+r+" g="+g+" b="+b);
                            int byteOffset=(tY*w+tX)*4;
                            if (UBUNTU_FONT_THRESHOLD>127) {
                                int t=UBUNTU_FONT_THRESHOLD-256;
                                if (r > t || g > t || b > t) {
                                    labelPixels[byteOffset] = oneByte;
                                    textPixelCount++;
                                }
                            } else {
                                if (r > UBUNTU_FONT_THRESHOLD || g > UBUNTU_FONT_THRESHOLD || b > UBUNTU_FONT_THRESHOLD) {
                                    labelPixels[byteOffset] = oneByte;
                                    textPixelCount++;
                                }
                            }
                        }
                    }
                }
            }
        }
        logger.info("textPixelCount="+textPixelCount);
        this.labelImageHeight=h;
        this.labelImageWidth=w;
        return labelPixels;
    }

    @Override
    public void display(GL4 gl) {
        if (this.mode==Mode.DRAW) {
            gl.glActiveTexture(GL4.GL_TEXTURE0);
            checkGlError(gl, "d1 glActiveTexture");
            gl.glBindTexture(GL4.GL_TEXTURE_2D, imageTextureId.get(0));
            checkGlError(gl, "d2 glBindTexture()");
        }
        if (this.mode==Mode.DRAW || this.mode==Mode.PICK) {
            gl.glBindVertexArray(vertexArrayId.get(0));
            checkGlError(gl, "d3 glBindVertexArray()");
            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, vertexBufferId.get(0));
            checkGlError(gl, "d4 glBindBuffer()");
            gl.glVertexAttribPointer(0, 3, GL4.GL_FLOAT, false, 20, 0);
            checkGlError(gl, "d5 glVertexAttribPointer()");
            gl.glEnableVertexAttribArray(0);
            checkGlError(gl, "d6 glEnableVertexAttribArray()");
            gl.glVertexAttribPointer(1, 2, GL4.GL_FLOAT, false, 20, 12);
            checkGlError(gl, "d7 glVertexAttribPointer()");
            gl.glEnableVertexAttribArray(1);
            checkGlError(gl, "d8 glEnableVertexAttribArray()");
            gl.glDrawArrays(GL4.GL_TRIANGLES, 0, vertexFb.capacity()/3);
            checkGlError(gl, "d9 glDrawArrays()");
            gl.glBindBuffer(GL4.GL_ARRAY_BUFFER, 0);
            checkGlError(gl, "d10 glBindBuffer()");
        }
        if (this.mode==Mode.DRAW) {
            gl.glBindTexture(GL4.GL_TEXTURE_2D, 0);
            checkGlError(gl, "d11 glBindTexture()");
        }
    }

    @Override
    public void dispose(GL4 gl) {
        if (mode==Mode.DRAW) {
            gl.glDeleteVertexArrays(1, vertexArrayId);
            gl.glDeleteBuffers(1, vertexBufferId);
            gl.glDeleteTextures(1, imageTextureId);
        }
    }

}