package org.janelia.workstation.gui.large_volume_viewer.action;

import org.janelia.it.jacs.shared.geom.CoordinateAxis;
import org.janelia.it.jacs.shared.geom.Vec3;
import org.janelia.it.jacs.shared.lvv.FileBasedOctreeMetadataSniffer;
import org.janelia.it.jacs.shared.lvv.TileFormat;
import org.janelia.it.jacs.shared.lvv.TileIndex;
import org.janelia.workstation.gui.large_volume_viewer.MicronCoordsFormatter;
import org.janelia.workstation.gui.large_volume_viewer.camera.BasicObservableCamera3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.nio.file.Path;

/**
 * Helps classes that do similar things to move stuff into the clipboard.
 *
 * @author fosterl
 */
public class ClipboardActionHelper {
    private final static String FILE_SEP = System.getProperty("file.separator");
    private final static String LINUX_FILE_SEP = "/";
    private static Logger log = LoggerFactory.getLogger(ClipboardActionHelper.class);

    public static Vec3 getCoordVector(String coordStr) {
        final MicronCoordsFormatter micronCoordsFormatter = new MicronCoordsFormatter(null);
        double[] micronLocation = micronCoordsFormatter.messageToTuple(coordStr);
        Vec3 vec = new Vec3(micronLocation[0], micronLocation[1], micronLocation[2]);
        return vec;
    }

    public static String getOctreePathAtCoords(TileFormat tileFormat, BasicObservableCamera3d camera, CoordinateAxis axis, Vec3 vec) {
        //  Method return found to be flawed at greater-than-0-level zoom.
        final int requiredZoomLevel = tileFormat.zoomLevelForCameraZoom(camera.getPixelsPerSceneUnit());
        TileIndex index = tileFormat.tileIndexForXyz(vec, requiredZoomLevel, axis);
        Path path = FileBasedOctreeMetadataSniffer.getOctreeFilePath(index, tileFormat);
        String filePathStr = path.toString().replace(FILE_SEP, LINUX_FILE_SEP);
        log.info("Returning {} for required zoom of {}, and full zoom path is {}.", path, requiredZoomLevel, filePathStr);
        return filePathStr;
    }

    public static void setClipboard(String clipboardString) {
        StringSelection selection = new StringSelection(clipboardString);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, selection);
    }

}
