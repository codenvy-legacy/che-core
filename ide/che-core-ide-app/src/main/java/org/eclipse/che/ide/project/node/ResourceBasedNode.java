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
package org.eclipse.che.ide.project.node;

import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.ide.api.project.node.resource.DeleteProcessor;
import org.eclipse.che.ide.api.project.node.resource.RenameProcessor;
import org.eclipse.che.ide.api.project.node.resource.SupportDelete;
import org.eclipse.che.ide.api.project.node.resource.SupportRename;
import org.eclipse.che.ide.api.project.node.settings.NodeSettings;
import org.eclipse.che.ide.project.event.ResourceNodeEvent;
import org.eclipse.che.ide.util.loging.Log;

import javax.annotation.Nonnull;

/**
 * @author Vlad Zhukovskiy
 */
public abstract class ResourceBasedNode<DataObject> extends AbstractProjectBasedNode<DataObject> implements SupportRename<DataObject>,
                                                                                                            SupportDelete<DataObject> {

    protected EventBus            eventBus;
    protected ResourceNodeManager resourceNodeManager;

    public ResourceBasedNode(@Nonnull DataObject dataObject,
                             @Nonnull ProjectDescriptor projectDescriptor,
                             @Nonnull NodeSettings nodeSettings,
                             @Nonnull EventBus eventBus,
                             @Nonnull ResourceNodeManager resourceNodeManager) {
        super(dataObject, projectDescriptor, nodeSettings);
        this.eventBus = eventBus;
        this.resourceNodeManager = resourceNodeManager;
    }

    @Override
    public void delete() {
        DeleteProcessor<DataObject> deleteProcessor = getDeleteProcessor();
        if (deleteProcessor == null) {
            return;
        }

        deleteProcessor.delete(this)
                       .then(onDelete())
                       .catchError(onFailed());
    }

    @Nonnull
    private Operation<DataObject> onDelete() {
        return new Operation<DataObject>() {
            @Override
            public void apply(DataObject deletedObject) throws OperationException {
                eventBus.fireEvent(new ResourceNodeEvent(ResourceBasedNode.this, ResourceNodeEvent.Event.DELETED));
            }
        };
    }

    @Nonnull
    private Operation<PromiseError> onFailed() {
        return new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError arg) throws OperationException {
                Log.error(ResourceBasedNode.class, arg.getMessage());
            }
        };
    }

    private Operation<DataObject> onRename() {
        return new Operation<DataObject>() {
            @Override
            public void apply(DataObject arg) throws OperationException {
                eventBus.fireEvent(new ResourceNodeEvent(ResourceBasedNode.this, ResourceNodeEvent.Event.RENAMED));
            }
        };
    }

    @Override
    public void rename(@Nonnull String newName) {
        RenameProcessor<DataObject> renameProcessor = getRenameProcessor();
        if (renameProcessor == null) {
            return;
        }

        renameProcessor.rename(this, newName)
                       .then(onRename())
                       .catchError(onFailed());
    }
}
