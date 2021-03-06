package org.janelia.workstation.browser.gui.listview.icongrid;

import java.util.Map;

import org.janelia.workstation.integration.util.FrameworkAccess;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.model.domain.DomainConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * UI configuration for a IconGridViewer.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class IconGridViewerConfiguration {

    private static final Logger log = LoggerFactory.getLogger(IconGridViewerConfiguration.class);

    private Map<String,String> domainClassTitles;
    private Map<String,String> domainClassSubtitles;

    public IconGridViewerConfiguration(Map<String,String> domainClassTitles, Map<String,String> domainClassSubtitles) {
        this.domainClassTitles = domainClassTitles;
        this.domainClassSubtitles = domainClassSubtitles;
    }

    public static IconGridViewerConfiguration loadConfig() {
        try {
            Map<String, String> domainClassTitles = DomainMgr.getDomainMgr().loadPreferencesAsMap(DomainConstants.PREFERENCE_CATEGORY_DOMAIN_OBJECT_TITLES);
            log.debug("Loaded {} title preferences", domainClassTitles.size());
            Map<String, String> domainClassSubtitles = DomainMgr.getDomainMgr().loadPreferencesAsMap(DomainConstants.PREFERENCE_CATEGORY_DOMAIN_OBJECT_SUBTITLES);
            log.debug("Loaded {} subtitle preferences", domainClassSubtitles.size());
            IconGridViewerConfiguration config = new IconGridViewerConfiguration(domainClassTitles, domainClassSubtitles);
            return config;
        }
        catch (Exception e) {
            FrameworkAccess.handleException(e);
            return null;
        }
    }

    public void save() throws Exception {
        log.debug("Saving {} title preferences",domainClassTitles.size());
        DomainMgr.getDomainMgr().saveMapAsPreferences(domainClassTitles, DomainConstants.PREFERENCE_CATEGORY_DOMAIN_OBJECT_TITLES);
        log.debug("Saving {} subtitle preferences",domainClassSubtitles.size());
        DomainMgr.getDomainMgr().saveMapAsPreferences(domainClassSubtitles, DomainConstants.PREFERENCE_CATEGORY_DOMAIN_OBJECT_SUBTITLES);
    }

    public void setDomainClassTitle(String className, String title) {
        log.trace("Setting title for {} to {}",className,title);
        domainClassTitles.put(className, title);
    }

    public void setDomainClassSubtitle(String className, String subtitle) {
        log.trace("Setting subtitle for {} to {}",className,subtitle);
        domainClassSubtitles.put(className, subtitle);
    }

    public String getDomainClassTitle(String className) {
        String title = domainClassTitles.get(className);
        log.trace("Got title {} for {}",title,className);
        return title;
    }

    public String getDomainClassSubtitle(String className) {
        String subtitle = domainClassSubtitles.get(className);
        log.trace("Got subtitle {} for {}",subtitle,className);
        return subtitle;
    }
}
