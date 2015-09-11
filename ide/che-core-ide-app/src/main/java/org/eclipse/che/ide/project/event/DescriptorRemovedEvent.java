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
package org.eclipse.che.ide.project.event;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.api.project.node.HasProjectDescriptor;
import org.eclipse.che.ide.project.event.DescriptorRemovedEvent.DescriptorRemoveHandler;

import javax.validation.constraints.NotNull;

/**
 * @author Vlad Zhukovskiy
 */
public class DescriptorRemovedEvent extends GwtEvent<DescriptorRemoveHandler> {

    public interface DescriptorRemoveHandler extends EventHandler {
        void onProjectModuleDelete(DescriptorRemovedEvent event);
    }

    private static Type<DescriptorRemoveHandler> TYPE;

    public static Type<DescriptorRemoveHandler> getType() {
        if (TYPE == null) {
            TYPE = new Type<>();
        }
        return TYPE;
    }

    private final ProjectDescriptor descriptor;

    public DescriptorRemovedEvent(@NotNull ProjectDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    @NotNull
    public ProjectDescriptor getDescriptor() {
        return descriptor;
    }

    /** {@inheritDoc} */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public Type<DescriptorRemoveHandler> getAssociatedType() {
        return (Type)TYPE;
    }

    /** {@inheritDoc} */
    @Override
    protected void dispatch(DescriptorRemoveHandler handler) {
        handler.onProjectModuleDelete(this);
    }
}
