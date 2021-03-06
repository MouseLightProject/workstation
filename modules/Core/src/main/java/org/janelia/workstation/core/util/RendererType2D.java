package org.janelia.workstation.core.util;

import org.janelia.model.domain.enums.NamedEnum;

/**
 * Enumeration of renderers that the workstation supports. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public enum RendererType2D implements NamedEnum {
    
    IMAGE_IO("Java Image IO"),
    LOCI("LOCI Bio-Formats");
    
    private String name;

    private RendererType2D(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
