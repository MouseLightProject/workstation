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
package org.janelia.horta.movie;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.Collection;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.windows.TopComponent;
import org.openide.util.NbBundle.Messages;
import org.openide.util.Utilities;

/**
 * Top component which displays something.
 */
@ConvertAsProperties(
        dtd = "-//org.janelia.horta//MovieMaker//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = "MovieMakerTopComponent",
        iconBase = "org/janelia/horta/16-16-8f894f2f6832f3a576bd8f6f8cdf3da0.png",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "properties", openAtStartup = false)
@ActionID(category = "Window", id = "org.janelia.horta.MovieMakerTopComponent")
@ActionReference(path = "Menu/Window/Horta" , position = 200)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_MovieMakerAction",
        preferredID = "MovieMakerTopComponent"
)
@Messages({
    "CTL_MovieMakerAction=Movie Maker",
    "CTL_MovieMakerTopComponent=Movie Maker",
    "HINT_MovieMakerTopComponent=Horta Movie Maker controls"
})
public final class MovieMakerTopComponent 
extends TopComponent 
implements LookupListener
{
    private Timeline movieTimeline;
    private MoviePlayState playState;
    private float nextFrameDuration = 8.0f; // seconds
    // private Interpolator defaultInterpolator = null;
    private final SaveFramesPanel saveFramesPanel = new SaveFramesPanel();
    private final JFileChooser movieScriptChooser = new JFileChooser();

    public MovieMakerTopComponent() {
        initComponents();
        setName(Bundle.CTL_MovieMakerTopComponent());
        setToolTipText(Bundle.HINT_MovieMakerTopComponent());
        FileNameExtensionFilter jsonFilter = new FileNameExtensionFilter("JSON Files", "json");
        movieScriptChooser.setFileFilter(jsonFilter);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        addFrameButton = new javax.swing.JButton();
        durationTextField = new javax.swing.JFormattedTextField();
        jLabel2 = new javax.swing.JLabel();
        frameCountLabel = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        playButton = new javax.swing.JButton();
        fpsTextField = new javax.swing.JFormattedTextField();
        jLabel1 = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        saveFramesButton = new javax.swing.JButton();
        saveScriptButton = new javax.swing.JButton();
        loadScriptButton = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        deleteFramesButton = new javax.swing.JButton();

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(MovieMakerTopComponent.class, "MovieMakerTopComponent.jPanel1.border.title"))); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(addFrameButton, org.openide.util.NbBundle.getMessage(MovieMakerTopComponent.class, "MovieMakerTopComponent.addFrameButton.text")); // NOI18N
        addFrameButton.setToolTipText(org.openide.util.NbBundle.getMessage(MovieMakerTopComponent.class, "MovieMakerTopComponent.addFrameButton.toolTipText")); // NOI18N
        addFrameButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addFrameButtonActionPerformed(evt);
            }
        });

        durationTextField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(new java.text.DecimalFormat("#0.00"))));
        durationTextField.setText(org.openide.util.NbBundle.getMessage(MovieMakerTopComponent.class, "MovieMakerTopComponent.durationTextField.text")); // NOI18N
        durationTextField.setToolTipText(org.openide.util.NbBundle.getMessage(MovieMakerTopComponent.class, "MovieMakerTopComponent.durationTextField.toolTipText")); // NOI18N
        durationTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                durationTextFieldActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jLabel2, org.openide.util.NbBundle.getMessage(MovieMakerTopComponent.class, "MovieMakerTopComponent.jLabel2.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(frameCountLabel, org.openide.util.NbBundle.getMessage(MovieMakerTopComponent.class, "MovieMakerTopComponent.frameCountLabel.text")); // NOI18N

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(addFrameButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(durationTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(frameCountLabel)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(addFrameButton)
                    .addComponent(durationTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(frameCountLabel))
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(MovieMakerTopComponent.class, "MovieMakerTopComponent.jPanel2.border.title"))); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(playButton, org.openide.util.NbBundle.getMessage(MovieMakerTopComponent.class, "MovieMakerTopComponent.playButton.text")); // NOI18N
        playButton.setToolTipText(org.openide.util.NbBundle.getMessage(MovieMakerTopComponent.class, "MovieMakerTopComponent.playButton.toolTipText")); // NOI18N
        playButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                playButtonActionPerformed(evt);
            }
        });

        fpsTextField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(new java.text.DecimalFormat("#0.00"))));
        fpsTextField.setText(org.openide.util.NbBundle.getMessage(MovieMakerTopComponent.class, "MovieMakerTopComponent.fpsTextField.text_2")); // NOI18N
        fpsTextField.setToolTipText(org.openide.util.NbBundle.getMessage(MovieMakerTopComponent.class, "MovieMakerTopComponent.fpsTextField.toolTipText")); // NOI18N
        fpsTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fpsTextFieldActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(MovieMakerTopComponent.class, "MovieMakerTopComponent.jLabel1.text")); // NOI18N

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(playButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(fpsTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel1)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(playButton)
                .addComponent(fpsTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jLabel1))
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(MovieMakerTopComponent.class, "MovieMakerTopComponent.jPanel3.border.title"))); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(saveFramesButton, org.openide.util.NbBundle.getMessage(MovieMakerTopComponent.class, "MovieMakerTopComponent.saveFramesButton.text")); // NOI18N
        saveFramesButton.setToolTipText(org.openide.util.NbBundle.getMessage(MovieMakerTopComponent.class, "MovieMakerTopComponent.saveFramesButton.toolTipText")); // NOI18N
        saveFramesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveFramesButtonActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(saveScriptButton, org.openide.util.NbBundle.getMessage(MovieMakerTopComponent.class, "MovieMakerTopComponent.saveScriptButton.text")); // NOI18N
        saveScriptButton.setToolTipText(org.openide.util.NbBundle.getMessage(MovieMakerTopComponent.class, "MovieMakerTopComponent.saveScriptButton.toolTipText")); // NOI18N
        saveScriptButton.setEnabled(false);
        saveScriptButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveScriptButtonActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(loadScriptButton, org.openide.util.NbBundle.getMessage(MovieMakerTopComponent.class, "MovieMakerTopComponent.loadScriptButton.text")); // NOI18N
        loadScriptButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadScriptButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(saveFramesButton)
                    .addComponent(saveScriptButton)
                    .addComponent(loadScriptButton))
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addComponent(saveFramesButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(saveScriptButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(loadScriptButton))
        );

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(MovieMakerTopComponent.class, "MovieMakerTopComponent.jPanel4.border.title"))); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(deleteFramesButton, org.openide.util.NbBundle.getMessage(MovieMakerTopComponent.class, "MovieMakerTopComponent.deleteFramesButton.text")); // NOI18N
        deleteFramesButton.setToolTipText(org.openide.util.NbBundle.getMessage(MovieMakerTopComponent.class, "MovieMakerTopComponent.deleteFramesButton.toolTipText")); // NOI18N
        deleteFramesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteFramesButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addComponent(deleteFramesButton)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(deleteFramesButton)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(93, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void addFrameButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addFrameButtonActionPerformed
        if (movieSource == null)
            return;
        ViewerState viewerState = movieSource.getViewerState();
        if (viewerState == null)
            return;
        KeyFrame keyFrame = new BasicKeyFrame(viewerState, nextFrameDuration);
        if (! movieTimeline.add(keyFrame))
            return;
        updateGui();
    }//GEN-LAST:event_addFrameButtonActionPerformed

    private void playButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_playButtonActionPerformed
        if (playState == null) {
            playButton.setEnabled(false);
            return;
        }            
        float fps = 5.0f;
        String fpsText = fpsTextField.getText();
        if (!fpsText.isEmpty()) {
            Float framesPerSecond = Float.parseFloat(fpsTextField.getText());
            fps = framesPerSecond.floatValue();
        }
        playState.playRealTime(fps);        
    }//GEN-LAST:event_playButtonActionPerformed

    private void deleteFramesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteFramesButtonActionPerformed
        if (movieTimeline != null)
            movieTimeline.clear();
        if (playState != null) {
            playState.reset();
        }
        updateGui();
    }//GEN-LAST:event_deleteFramesButtonActionPerformed

    private void fpsTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fpsTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_fpsTextFieldActionPerformed

    private void saveFramesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveFramesButtonActionPerformed
        saveFramesPanel.showDialog(playState);
    }//GEN-LAST:event_saveFramesButtonActionPerformed

    private void saveScriptButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveScriptButtonActionPerformed
        try {
            int saveDialogResult = movieScriptChooser.showSaveDialog(this);
            if (saveDialogResult != JFileChooser.APPROVE_OPTION)
                return;
            File file = movieScriptChooser.getSelectedFile();
            Writer writer;
            try {
                writer = new FileWriter(file);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                        ex.getMessage(),
                        "Error opening file " + file.getName(),
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            GsonBuilder gsonBuilder = new GsonBuilder();
            Type timelineType = new TypeToken<Timeline>(){}.getType();
            gsonBuilder.registerTypeAdapter(timelineType, new MovieTimelineSerializer(playState.isLoop(), movieSource));
            gsonBuilder.setPrettyPrinting();
            
            Gson gson = gsonBuilder.create();
            gson.toJson(movieTimeline, timelineType, writer);
            
            writer.close();
            
            JOptionPane.showMessageDialog(this,
                    "Finished writing movie script " + file.getName(),
                    "Finished writing movie script " + file.getName(),
                    JOptionPane.INFORMATION_MESSAGE);
            
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }//GEN-LAST:event_saveScriptButtonActionPerformed

    private void durationTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_durationTextFieldActionPerformed
        // TODO add your handling code here:
        float duration = Float.parseFloat(durationTextField.getText());
        if (duration > 0)
            nextFrameDuration = duration;
    }//GEN-LAST:event_durationTextFieldActionPerformed

    private void loadScriptButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadScriptButtonActionPerformed
        // TODO add your handling code here:
        int loadScriptResult = movieScriptChooser.showOpenDialog(this);
        if (loadScriptResult != JFileChooser.APPROVE_OPTION)
            return;
        File file = movieScriptChooser.getSelectedFile();
        if (! file.exists()) {
            JOptionPane.showMessageDialog(this, 
                    "No such file " + file.getAbsolutePath(),
                    "File not found",
                    JOptionPane.WARNING_MESSAGE);            
            return;
        }
        Reader jsonReader;
        try {
            jsonReader = new FileReader(file);
        } catch (FileNotFoundException ex) {
            Exceptions.printStackTrace(ex);
            JOptionPane.showMessageDialog(this, 
                    "Failed to open file " + file.getAbsolutePath(),
                    "Failed to open file " + file.getAbsolutePath(),
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        Type timelineType = new TypeToken<Timeline>(){}.getType();
        GsonBuilder gsonBuilder = new GsonBuilder();
        boolean doLoop = false;
        if (playState != null)
            doLoop = playState.isLoop();
        if (movieSource == null) {
            JOptionPane.showMessageDialog(this, 
                    "Error: Cannot load a movie script without a viewer attached. (Try clicking in the Horta window first...)",
                    "Error: Cannot load a movie script without a viewer attached",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        gsonBuilder.registerTypeAdapter(timelineType, new MovieTimelineSerializer(doLoop, movieSource));
        Gson gson = gsonBuilder.create();
        Timeline timeline = gson.fromJson(jsonReader, timelineType);
        movieTimeline = timeline;
        if (movieSource != null)
            playState = new BasicMoviePlayState(movieTimeline, movieSource);
        updateGui();
    }//GEN-LAST:event_loadScriptButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addFrameButton;
    private javax.swing.JButton deleteFramesButton;
    private javax.swing.JFormattedTextField durationTextField;
    private javax.swing.JFormattedTextField fpsTextField;
    private javax.swing.JLabel frameCountLabel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JButton loadScriptButton;
    private javax.swing.JButton playButton;
    private javax.swing.JButton saveFramesButton;
    private javax.swing.JButton saveScriptButton;
    // End of variables declaration//GEN-END:variables
    @Override
    public void componentOpened() {
        // add custom code on component opening
        
        // Movie Sources
        movieSourcesResult = Utilities.actionsGlobalContext().lookupResult(MovieSource.class);
        movieSourcesResult.addLookupListener(this);
        Collection<? extends MovieSource> allSources = movieSourcesResult.allInstances();
        if (allSources.isEmpty())
            setMovieSource(null);
        else
            setMovieSource((MovieSource)allSources.iterator().next());
    }

    @Override
    public void componentClosed() {
        movieSourcesResult.removeLookupListener(this);
    }

    void writeProperties(java.util.Properties p) {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
        // TODO store your settings
    }

    void readProperties(java.util.Properties p) {
        String version = p.getProperty("version");
        // TODO read your settings according to their version
    }

    private void updateGui() {
        if (movieTimeline == null) {
            playButton.setEnabled(false);
            frameCountLabel.setText("Movie has no key frames");
            addFrameButton.setEnabled(false);
            deleteFramesButton.setEnabled(false);
            saveFramesButton.setEnabled(false);
            saveScriptButton.setEnabled(false);
        }
        else {
            boolean bHaveFrames = (movieTimeline.size() > 0);
            playButton.setEnabled(bHaveFrames);
            deleteFramesButton.setEnabled(bHaveFrames);
            saveScriptButton.setEnabled(bHaveFrames);
            saveFramesButton.setEnabled(bHaveFrames);
            frameCountLabel.setText("Movie has " + movieTimeline.size() + " key frames");
            addFrameButton.setEnabled(true);
        }
    }
    
    private Lookup.Result<MovieSource> movieSourcesResult = null;
    private MovieSource movieSource = null;

    public MovieSource getMovieSource() {
        return movieSource;
    }

    public void setMovieSource(MovieSource movieSource) 
    {
        if ((movieSource == null) && (this.movieSource == null)) 
        {
            addFrameButton.setEnabled(false); // disable controls
            return;
        }
        if (this.movieSource == movieSource)
            return; // no change
        if (movieSource == null)
            return; // remember the old source, when the new one seems to be null
        this.movieSource = movieSource;
        
        if (movieTimeline == null)
            movieTimeline = new BasicMovieTimeline(movieSource.getDefaultInterpolator());
        movieTimeline.clear();
        playState = new BasicMoviePlayState(movieTimeline, movieSource);
        
        updateGui();
    }

    public float getPlaybackFramesPerSecond() {
        return playState.getFramesPerSecond();
    }
    
    public void setPlaybackFramesPerSecond(float fps) {
        playState.setFramesPerSecond(fps);
    }
    
    @Override
    public void resultChanged(LookupEvent le) 
    {   
        // MovieSources
        if (movieSourcesResult == null)
            return;
        Collection<? extends MovieSource> sources = movieSourcesResult.allInstances();
        if (sources.isEmpty())
            setMovieSource(null);
        else
            setMovieSource((MovieSource)sources.iterator().next());
    }
}
