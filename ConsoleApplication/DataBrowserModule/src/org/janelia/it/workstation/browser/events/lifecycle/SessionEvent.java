package org.janelia.it.workstation.browser.events.lifecycle;

import org.janelia.model.domain.Subject;

/**
 * The current user has changed in some way. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SessionEvent {

    private Subject subject;

    public SessionEvent(Subject subject) {
        this.subject = subject;
    }

    public Subject getSubject() {
        return subject;
    }
}
