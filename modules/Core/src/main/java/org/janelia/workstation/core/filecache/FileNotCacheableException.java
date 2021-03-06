package org.janelia.workstation.core.filecache;

/**
 * Thrown when a file cannot be cached.
 *
 * @author Eric Trautman
 */
public class FileNotCacheableException extends Exception {

    public FileNotCacheableException(String message) {
        super(message);
    }

    public FileNotCacheableException(String message,
                                     Throwable cause) {
        super(message, cause);
    }
}
