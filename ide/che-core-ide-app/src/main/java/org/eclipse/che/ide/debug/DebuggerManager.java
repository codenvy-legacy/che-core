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

import org.eclipse.che.ide.api.event.project.CloseCurrentProjectEvent;
import org.eclipse.che.ide.api.event.project.CloseCurrentProjectHandler;
import org.eclipse.che.ide.api.event.project.ProjectReadyEvent;
import org.eclipse.che.ide.api.event.project.ProjectReadyHandler;

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

    @Inject
    protected DebuggerManager(EventBus eventBus) {
        this.debuggers = new HashMap<>();

        eventBus.addHandler(CloseCurrentProjectEvent.TYPE, new CloseCurrentProjectHandler() {
            @Override
            public void onCloseCurrentProject(CloseCurrentProjectEvent event) {
                currentDebugger = null;
            }
        });

        eventBus.addHandler(ProjectReadyEvent.TYPE, new ProjectReadyHandler() {
            @Override
            public void onProjectReady(ProjectReadyEvent event) {
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