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
package org.eclipse.che.ide.event;

import com.google.gwt.event.shared.GwtEvent;

import org.eclipse.che.ide.project.node.FileReferenceNode;

/**
 * Event that notifies of item created.
 *
 * @author Igor Vinokur
 */
public class FileNodeCreatedEvent extends GwtEvent<FileNodeCreatedEventHandler> {

    public static final Type<FileNodeCreatedEventHandler> TYPE = new Type<>();

    private final FileReferenceNode node;

    public FileNodeCreatedEvent(FileReferenceNode node) {
        this.node = node;
    }

    @Override
    public Type<FileNodeCreatedEventHandler> getAssociatedType() {
        return TYPE;
    }

    /**
     * Returns created node
     */
    public FileReferenceNode getCreatedNode() {
        return node;
    }

    @Override
    protected void dispatch(FileNodeCreatedEventHandler handler) {
        handler.onFileNodeCreated(this);
    }
}
