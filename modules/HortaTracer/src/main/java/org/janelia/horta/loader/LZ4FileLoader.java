package org.janelia.horta.loader;

import java.io.IOException;
import java.io.InputStream;
import net.jpountz.lz4.LZ4FrameInputStream;
import org.apache.commons.io.FilenameUtils;


/**
 *
 * @author Christopher Bruns
 */
public class LZ4FileLoader implements FileTypeLoader
{

    @Override
    public boolean supports(DataSource source)
    {
        String ext = FilenameUtils.getExtension(source.getFileName()).toUpperCase();
        if (ext.equals("LZ4"))
            return true;
        return false;
    }

    @Override
    public boolean load(DataSource source, FileHandler handler) throws IOException
    {
        // Delegate to uncompressed datasource
        InputStream uncompressedStream = new LZ4FrameInputStream(source.getInputStream());
        String uncompressedName = FilenameUtils.getBaseName(source.getFileName());
        DataSource uncompressed = new BasicDataSource(uncompressedStream, uncompressedName);
        return handler.handleDataSource(uncompressed);
    }
    
}
