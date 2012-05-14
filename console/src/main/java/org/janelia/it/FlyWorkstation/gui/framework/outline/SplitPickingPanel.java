package org.janelia.it.FlyWorkstation.gui.framework.outline;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.*;

import org.janelia.it.FlyWorkstation.api.entity_model.management.EntitySelectionModel;
import org.janelia.it.FlyWorkstation.api.entity_model.management.ModelMgr;
import org.janelia.it.FlyWorkstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.AnnotatedImageButton;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.IconDemoPanel;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.RootedEntity;
import org.janelia.it.FlyWorkstation.gui.util.SimpleWorker;
import org.janelia.it.FlyWorkstation.shared.util.ModelMgrUtils;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.utility.GenericTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.shared.utils.EntityUtils;

public class SplitPickingPanel extends JPanel implements Refreshable {
	
	private static final int MAX_FREE_CROSSES = 1;
	
	private RootedEntity resultFolder;
	
	private JButton searchButton;
	private JButton groupButton;
	private JButton resultFolderButton;
	private IconDemoPanel crossesPanel;
	
	private static final int STEP_PANEL_HEIGHT = 35; 
	
	public SplitPickingPanel() {
	
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

		JPanel searchPanel = new JPanel();
		searchPanel.setLayout(new BoxLayout(searchPanel, BoxLayout.LINE_AXIS));
		JLabel searchLabel = new JLabel("1. Search for screen images: ");
		searchPanel.add(searchLabel);
		searchButton = new JButton("Search");
		searchButton.setFocusable(false);
		searchButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				SessionMgr.getSessionMgr().getActiveBrowser().getPatternSearchDialog().showDialog();
			}
		});
		searchPanel.add(searchButton);
		searchPanel.setPreferredSize(new Dimension(0, STEP_PANEL_HEIGHT));
		searchPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		add(searchPanel);
		

		JPanel groupPanel = new JPanel();
		groupPanel.setLayout(new BoxLayout(groupPanel, BoxLayout.LINE_AXIS));
		JLabel groupLabel = new JLabel("2. Group by split lines:");
		groupPanel.add(groupLabel);
		groupButton = new JButton("Group");
		groupButton.setFocusable(false);
		groupButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				SessionMgr.getSessionMgr().getActiveBrowser().getPatternSearchDialog().showDialog();
			}
		});
		groupPanel.add(groupButton);
		groupPanel.setPreferredSize(new Dimension(0, STEP_PANEL_HEIGHT));
		groupPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		add(groupPanel);
		
		
		JPanel folderPanel = new JPanel();
		folderPanel.setLayout(new BoxLayout(folderPanel, BoxLayout.LINE_AXIS));
		JLabel folderLabel = new JLabel("3. Choose a result folder: ");
		folderPanel.add(folderLabel);
		resultFolderButton = new JButton("Choose result folder");
		resultFolderButton.setFocusable(false);
		resultFolderButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				showPopupResultFolderMenu();
			}
		});
		folderPanel.add(resultFolderButton);
		folderPanel.setPreferredSize(new Dimension(0, STEP_PANEL_HEIGHT));
		folderPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		add(folderPanel);

		
		JPanel computePanel = new JPanel();
		computePanel.setLayout(new BoxLayout(computePanel, BoxLayout.LINE_AXIS));
		JLabel computeLabel = new JLabel("4. Compute selected intersections: ");
		computePanel.add(computeLabel);
		JButton crossButton = new JButton("Compute");
		crossButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				createCrosses();
			}
		});
		computePanel.add(crossButton);
		computePanel.setPreferredSize(new Dimension(0, STEP_PANEL_HEIGHT));
		computePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		add(computePanel);
		
		add(Box.createVerticalStrut(10));
		
		crossesPanel = new IconDemoPanel(EntitySelectionModel.CATEGORY_CROSS_VIEW) {
			@Override
			protected JToolBar createToolbar() {
				// Override to customize the toolbar
				JToolBar toolbar = super.createToolbar();
				toolbar.removeAll();
				toolbar.add(refreshButton);
				toolbar.addSeparator();
				toolbar.add(imageSizeSlider);
				return toolbar;
			}
			@Override
			protected void buttonDrillDown(AnnotatedImageButton button) {
				// Do nothing, this panel does not support drill down
			}
			@Override
			protected void buttonSelection(AnnotatedImageButton button, boolean multiSelect, boolean rangeSelect) {
				super.buttonSelection(button, multiSelect, rangeSelect);
				if (rangeSelect || multiSelect) return;
				final RootedEntity rootedEntity = button.getRootedEntity();
				final String rootedEntityId = rootedEntity.getId();

				if (!rootedEntity.getEntity().getEntityType().getName().equals(EntityConstants.TYPE_SCREEN_SAMPLE_CROSS)) {
					return;
				}
				
				// This is a sample cross, attempt to select its parents in the other viewers
				
				Entity crossEntity = rootedEntity.getEntity();
				Entity sourceEntity1 = null;
				Entity sourceEntity2 = null;
				
				for(EntityData childEd : crossEntity.getOrderedEntityData()) {
					if (!childEd.getEntityAttribute().getName().equals(EntityConstants.ATTRIBUTE_ENTITY)) continue;
					if (sourceEntity1 == null) {
						sourceEntity1 = childEd.getChildEntity();
					}
					else {
						sourceEntity2 = childEd.getChildEntity();
						break;
					}
				}

				if (sourceEntity1==null || sourceEntity2==null) {
					System.out.println("Error: incomplete sample cross entity: "+rootedEntityId);
					return;
				}
				
				final Entity finalEntity1 = sourceEntity1;
				final Entity finalEntity2 = sourceEntity2;

				
				SimpleWorker worker = new SimpleWorker() {

					private Entity sourceEntity1 = finalEntity1;
					private Entity sourceEntity2 = finalEntity2;
					
					@Override
					protected void doStuff() throws Exception {
						if (!EntityUtils.isInitialized(sourceEntity1)) {
							sourceEntity1 = ModelMgr.getModelMgr().getEntityById(""+sourceEntity1.getId());
						}
						if (!EntityUtils.isInitialized(sourceEntity2)) {
							sourceEntity2 = ModelMgr.getModelMgr().getEntityById(""+sourceEntity2.getId());
						}
					}
					
					@Override
					protected void hadSuccess() {
						final IconDemoPanel mainViewer = (IconDemoPanel)SessionMgr.getBrowser().getViewerForCategory(EntitySelectionModel.CATEGORY_MAIN_VIEW);
						if (mainViewer.getEntityById(sourceEntity1.getId()) == null) {
							mainViewer.loadEntity(rootedEntity, new Callable<Void>() {
								@Override
								public Void call() throws Exception {
									selectAndScroll(mainViewer, sourceEntity1.getId());	
									return null;
								}
							});
						}
						else {
							selectAndScroll(mainViewer, sourceEntity1.getId());	
						}
						
						final IconDemoPanel secViewer = (IconDemoPanel)SessionMgr.getBrowser().showSecViewer();
						if (secViewer.getEntityById(sourceEntity2.getId()) == null) {
							secViewer.loadEntity(rootedEntity, new Callable<Void>() {
								@Override
								public Void call() throws Exception {
									selectAndScroll(secViewer, sourceEntity2.getId());
									return null;
								}
							});
						}
						else {
							selectAndScroll(secViewer, sourceEntity2.getId());
						}
					}
					
					@Override
					protected void hadError(Throwable error) {
						SessionMgr.getSessionMgr().handleException(error);
					}
				};
				
				worker.execute();
				
			}
		};
		
		crossesPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		crossesPanel.setBorder(BorderFactory.createLineBorder((Color)UIManager.get("windowBorder")));
		crossesPanel.getSplashPanel().setShowSplashImage(false);
		add(crossesPanel);
	}

	/**
	 * Select the given entity in the given viewer, and then scroll it into view.
	 * @param viewer
	 * @param entityId
	 */
	private void selectAndScroll(final IconDemoPanel viewer, final Long entityId) {
		ModelMgr.getModelMgr().getEntitySelectionModel().selectEntity(viewer.getSelectionCategory(), ""+entityId, true);
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				viewer.getImagesPanel().scrollSelectedEntitiesToCenter();
			}
		});	
	}
	
	@Override
	public void refresh() {
		 
		final int padding = 100;
		final int fullWidth = SessionMgr.getBrowser().getViewersPanel().getWidth();
		final IconDemoPanel mainViewer = (IconDemoPanel)SessionMgr.getBrowser().getViewerForCategory(EntitySelectionModel.CATEGORY_MAIN_VIEW);
		final IconDemoPanel secViewer = (IconDemoPanel)SessionMgr.getBrowser().showSecViewer(); 
		
		// Refresh the image viewer
		crossesPanel.refresh();

		// Resize images to 1 per row
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public void run() {
				mainViewer.getImageSizeSlider().setValue((int)((double)fullWidth/2-padding));
				secViewer.getImageSizeSlider().setValue((int)((double)fullWidth/2-padding));
				crossesPanel.getImageSizeSlider().setValue(crossesPanel.getWidth()-padding);
			}
		});
	}

	private void createCrosses() {
		
		if (resultFolder==null) {
			JOptionPane.showMessageDialog(SessionMgr.getBrowser(), "Please choose a result folder first", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		EntitySelectionModel esm = ModelMgr.getModelMgr().getEntitySelectionModel();
		final List<String> mainSelectionIds = esm.getSelectedEntitiesIds(EntitySelectionModel.CATEGORY_MAIN_VIEW);		
		final List<String> secSelectionIds = esm.getSelectedEntitiesIds(EntitySelectionModel.CATEGORY_SEC_VIEW);
		
		final List<Entity> samples1 = new ArrayList<Entity>();
		final List<Entity> samples2 = new ArrayList<Entity>();

		for(String mainSelectionId : mainSelectionIds) {
			Entity sample1 = SessionMgr.getBrowser().getViewerForCategory(EntitySelectionModel.CATEGORY_MAIN_VIEW).getRootedEntityById(mainSelectionId).getEntity();
			if (!sample1.getEntityType().getName().equals(EntityConstants.TYPE_SCREEN_SAMPLE)) {
				JOptionPane.showMessageDialog(SessionMgr.getBrowser(), "Not a screen sample: "+sample1.getName(), "Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			samples1.add(sample1);
		}

		for(String secSelectionId : secSelectionIds) {
			Entity sample2 = SessionMgr.getBrowser().getViewerForCategory(EntitySelectionModel.CATEGORY_SEC_VIEW).getRootedEntityById(secSelectionId).getEntity();
			if (!sample2.getEntityType().getName().equals(EntityConstants.TYPE_SCREEN_SAMPLE)) {
				JOptionPane.showMessageDialog(SessionMgr.getBrowser(), "Not a screen sample: "+sample2.getName(), "Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			samples2.add(sample2);
		}
		
		int numCrosses = samples1.size() * samples2.size();
		if (numCrosses > MAX_FREE_CROSSES) {
			Object[] options = {"Yes", "Cancel"};
			int deleteConfirmation = JOptionPane.showOptionDialog(SessionMgr.getBrowser(),
					"Are you sure you want to compute "+numCrosses+" crosses?", "Compute Crosses",
					JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
			if (deleteConfirmation != 0) {
				return;
			}
		}
		else if (numCrosses == 0) {
			JOptionPane.showMessageDialog(SessionMgr.getBrowser(), "Please select at least one Screen Sample in each viewer", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		SimpleWorker worker = new SimpleWorker() {

			@Override
			protected void doStuff() throws Exception {
				
				Entity parent = resultFolder.getEntity();
				
				List<Long> sampleIds1 = new ArrayList<Long>();
				List<Long> sampleIds2 = new ArrayList<Long>();
				List<Long> outputIds = new ArrayList<Long>();
				
				for(Entity sample1 : samples1) {
					String[] parts1 = sample1.getName().split("-");
					
					for(Entity sample2 : samples2) {
						String[] parts2 = sample2.getName().split("-");
						
						Entity cross = ModelMgr.getModelMgr().createEntity(EntityConstants.TYPE_SCREEN_SAMPLE_CROSS, parts1[0]+"-"+parts2[0]);
						ModelMgr.getModelMgr().addEntityToParent(parent, cross, parent.getMaxOrderIndex()+1, EntityConstants.ATTRIBUTE_ENTITY);
						
						List<Long> childrenIds = new ArrayList<Long>();
						childrenIds.add(sample1.getId());
						childrenIds.add(sample2.getId());
						ModelMgr.getModelMgr().addChildren(cross.getId(), childrenIds, EntityConstants.ATTRIBUTE_ENTITY);
						
						sampleIds1.add(sample1.getId());
						sampleIds2.add(sample2.getId());
						outputIds.add(cross.getId());
					}
				}
				
				startIntersections(sampleIds1, sampleIds2, outputIds);
				SessionMgr.getBrowser().getEntityOutline().refresh();
			}
			
			@Override
			protected void hadSuccess() {
				crossesPanel.refresh();
			}
			
			@Override
			protected void hadError(Throwable error) {
				SessionMgr.getSessionMgr().handleException(error);
			}
		};
		
		worker.execute();
	}
		
	private void setResultFolder(Entity entity) {
		
		resultFolderButton.setText(entity.getName());
		EntityData entityData = new EntityData();
		entityData.setChildEntity(entity);
		resultFolder = new RootedEntity("/e_"+entity.getId(), entityData);
		crossesPanel.loadEntity(resultFolder);
	}

	private void showPopupResultFolderMenu() {

		JPopupMenu chooseFolderMenu = new JPopupMenu();
		
		List<EntityData> rootEds = SessionMgr.getBrowser().getEntityOutline().getRootEntity().getOrderedEntityData();
		
		for(EntityData rootEd : rootEds) {
			final Entity commonRoot = rootEd.getChildEntity();
			if (!commonRoot.getUser().getUserLogin().equals(SessionMgr.getUsername())) continue;
			
			JMenuItem commonRootItem = new JMenuItem(commonRoot.getName());
			commonRootItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent actionEvent) {
					SimpleWorker worker = new SimpleWorker() {
						@Override
						protected void doStuff() throws Exception {
							setResultFolder(commonRoot);
						}
						@Override
						protected void hadSuccess() {
						}
						@Override
						protected void hadError(Throwable error) {
							SessionMgr.getSessionMgr().handleException(error);
						}
					};
					worker.execute();
				}
			});
			
			chooseFolderMenu.add(commonRootItem);
		}
		
		chooseFolderMenu.addSeparator();
		
		JMenuItem createNewItem = new JMenuItem("Create new...");
		
		createNewItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {

				// Add button clicked
				final String folderName = (String) JOptionPane.showInputDialog(SessionMgr.getBrowser(), "Folder Name:\n",
						"Create top-level folder", JOptionPane.PLAIN_MESSAGE, null, null, null);
				if ((folderName == null) || (folderName.length() <= 0)) {
					return;
				}

				SimpleWorker worker = new SimpleWorker() {
					@Override
					protected void doStuff() throws Exception {
						setResultFolder(ModelMgrUtils.createNewCommonRoot(folderName));
					}
					@Override
					protected void hadSuccess() {
						// Update Tree UI
						SessionMgr.getBrowser().getEntityOutline().refresh(true, null);
					}
					@Override
					protected void hadError(Throwable error) {
						SessionMgr.getSessionMgr().handleException(error);
					}
				};
				worker.execute();
			}
		});
		
		chooseFolderMenu.add(createNewItem);
		
		chooseFolderMenu.show(resultFolderButton, 0, resultFolderButton.getHeight());
	}

    private void startIntersections(List<Long> sampleIdList1, List<Long> sampleIdList2, List<Long> outputIdList3) throws Exception {

    	String idList1Str = commafy(sampleIdList1);
    	String idList2Str = commafy(sampleIdList2);
    	String idList3Str = commafy(outputIdList3);
    	String method = "1"; // TODO: could make this a user preference
    	String kernelSize = "3"; // TODO: could make this a user preference
    	
    	HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
    	taskParameters.add(new TaskParameter("screen sample 1 id list", idList1Str, null));
    	taskParameters.add(new TaskParameter("screen sample 2 id list", idList2Str, null));
    	taskParameters.add(new TaskParameter("output entity id_list", idList3Str, null));
    	taskParameters.add(new TaskParameter("intersection method", method, null));
    	taskParameters.add(new TaskParameter("kernel size", kernelSize, null));
    	
    	Task task = new GenericTask(new HashSet<Node>(), "system", new ArrayList<Event>(), 
    			taskParameters, "screenSampleCrossService", "Screen Sample Cross Service");
        task.setJobName("Screen Sample Cross Service");
        task = ModelMgr.getModelMgr().saveOrUpdateTask(task);
        ModelMgr.getModelMgr().submitJob("ScreenSampleCrossService", task.getObjectId());
    }

	private String commafy(List<Long> list) {
		StringBuffer sb = new StringBuffer();
		for(Long l : list) {
			if (sb.length()>0) sb.append(",");
			sb.append(l);
		}
		return sb.toString();
	}
	
}
