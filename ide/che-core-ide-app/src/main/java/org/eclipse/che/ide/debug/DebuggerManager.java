/*******************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.debug;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.ide.api.event.ProjectActionEvent;
import org.eclipse.che.ide.api.event.ProjectActionHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * The manager provides to return debugger for current project.
 *
 * @author Andrey Plotnikov
 */
@Singleton
public class DebuggerManager {
    private Debugger              currentDebugger;
    private Map<String, Debugger> debuggers;

    /**
     * Create manager.
     *
     * @param eventBus
     */
    @Inject
    protected DebuggerManager(EventBus eventBus) {
        this.debuggers = new HashMap<>();
        eventBus.addHandler(ProjectActionEvent.TYPE, new ProjectActionHandler() {
            @Override
            public void onProjectDeleted(ProjectActionEvent event) {
                currentDebugger = null;
            }

            @Override
            public void onProjectCreated(ProjectActionEvent event) {
                currentDebugger = debuggers.get(event.getProject().getType());
            }
        });
    }

    /**
     * Register new debugger for the specified project type ID.
     *
     * @param projectTypeId
     * @param debugger
     */
    public void registeredDebugger(String projectTypeId, Debugger debugger) {
        debuggers.put(projectTypeId, debugger);
    }

    /** @return debugger for project type */
    public Debugger getDebugger() {
        return currentDebugger;
    }
}