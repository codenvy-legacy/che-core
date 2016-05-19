/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.api.project.node.event;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * Event fires when project explorer part should be initialized.
 *
 * @author Vlad Zhukovskiy
 */
public class ProjectPartLoadEvent extends GwtEvent<ProjectPartLoadEvent.ProjectPartLoadHandler> {

    public interface ProjectPartLoadHandler extends EventHandler {
        /** Method invokes when project explorer part should initialize and load project to display it. */
        void onProjectPartLoad(ProjectPartLoadEvent event);
    }

    private static Type<ProjectPartLoadHandler> TYPE;

    public static Type<ProjectPartLoadHandler> getType() {
        if (TYPE == null) {
            TYPE = new Type<>();
        }
        return TYPE;
    }

    public ProjectPartLoadEvent() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Type<ProjectPartLoadHandler> getAssociatedType() {
        return (Type)TYPE;
    }

    @Override
    protected void dispatch(ProjectPartLoadHandler handler) {
        handler.onProjectPartLoad(this);
    }
}
