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
package org.eclipse.che.ide.api.project.tree.generic;

import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.event.FileEvent;
import org.eclipse.che.ide.api.event.NodeChangedEvent;
import org.eclipse.che.ide.api.project.tree.TreeNode;
import org.eclipse.che.ide.api.project.tree.TreeStructure;
import org.eclipse.che.ide.api.project.tree.VirtualFile;
import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.StringUnmarshaller;
import org.eclipse.che.ide.rest.Unmarshallable;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.web.bindery.event.shared.EventBus;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

/**
 * A node that represents a file (an {@link ItemReference} with type - file).
 *
 * @author Artem Zatsarynnyy
 */
public class FileNode extends ItemNode implements VirtualFile {

    private final EditorAgent editorAgent;

    @Inject
    public FileNode(@Assisted TreeNode<?> parent,
                    @Assisted ItemReference data,
                    @Assisted TreeStructure treeStructure,
                    EventBus eventBus,
                    ProjectServiceClient projectServiceClient,
                    DtoUnmarshallerFactory dtoUnmarshallerFactory,
                    EditorAgent editorAgent) {
        super(parent, data, treeStructure, eventBus, projectServiceClient, dtoUnmarshallerFactory);
        this.editorAgent = editorAgent;
    }

    /** {@inheritDoc} */
    @Override
    public void rename(final String newName, final RenameCallback callback) {
        final FileNode fileNode = this;
        String newMediaType = fileNode.getMediaType();

        final String parentPath = ((StorableNode)getParent()).getPath();
        final String oldNodePath = getPath();
        final String newPath = parentPath + "/" + newName;

        projectServiceClient.rename(oldNodePath, newName, newMediaType, new AsyncRequestCallback<Void>() {
            @Override
            protected void onSuccess(Void result) {
                Unmarshallable<ItemReference> fileReferenceUnmarshallable = dtoUnmarshallerFactory.newUnmarshaller(ItemReference.class);

                projectServiceClient.getItem(newPath, new AsyncRequestCallback<ItemReference>(fileReferenceUnmarshallable) {
                    @Override
                    protected void onSuccess(ItemReference result) {
                        setData(result);

                        updateEditor(oldNodePath);

                        eventBus.fireEvent(NodeChangedEvent.createNodeRenamedEvent(fileNode));
                        callback.onRenamed();
                    }

                    @Override
                    protected void onFailure(Throwable exception) {
                        callback.onFailure(exception);
                    }
                });
            }

            @Override
            protected void onFailure(Throwable exception) {
                callback.onFailure(exception);
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void updateData(final AsyncCallback<Void> asyncCallback, final String updatedParentNodePath) {
        final String oldNodePath = FileNode.this.getPath();

        AsyncCallback<Void> updateEditorCallBack = new AsyncCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                updateEditor(oldNodePath);
                asyncCallback.onSuccess(null);
            }

            @Override
            public void onFailure(Throwable throwable) {
                asyncCallback.onFailure(throwable);
            }
        };

        super.updateData(updateEditorCallBack, updatedParentNodePath);
    }

    private void updateEditor(String oldPath) {
        final Array<String> pathOpenedEditors = editorAgent.getOpenedEditors().getKeys();

        if (pathOpenedEditors.contains(oldPath)) {
            editorAgent.updateEditorNode(oldPath, FileNode.this);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isLeaf() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public void processNodeAction() {
        eventBus.fireEvent(new FileEvent(this, FileEvent.FileOperation.OPEN));
    }

    /** {@inheritDoc} */
    @Override
    public void delete(final DeleteCallback callback) {
        super.delete(new DeleteCallback() {
            @Override
            public void onDeleted() {
                eventBus.fireEvent(new FileEvent(FileNode.this, FileEvent.FileOperation.CLOSE));
                callback.onDeleted();
            }

            @Override
            public void onFailure(Throwable caught) {
                callback.onFailure(caught);
            }
        });
    }

    @Nullable
    @Override
    public String getMediaType() {
        return getData().getMediaType();
    }

    @Override
    public boolean isReadOnly() {
        //todo add permissions check here
        return false;
    }

    @Override
    public String getContentUrl() {
        List<Link> links = getData().getLinks();
        Link li = null;
        for (Link link : links) {
            if (link.getRel().equals("get content")) {
                li = link;
                break;
            }
        }
        return li == null ? null : li.getHref();
    }

    /**
     * Get content of the file which this node represents.
     *
     * @param callback
     *         callback to return retrieved content
     */
    public void getContent(final AsyncCallback<String> callback) {
        projectServiceClient.getFileContent(getPath(), new AsyncRequestCallback<String>(new StringUnmarshaller()) {
            @Override
            protected void onSuccess(String result) {
                callback.onSuccess(result);
            }

            @Override
            protected void onFailure(Throwable exception) {
                callback.onFailure(exception);
            }
        });
    }

    /**
     * Update content of the file which this node represents.
     *
     * @param content
     *         new content of the file
     * @param callback
     *         callback to return retrieved content
     */
    public void updateContent(String content, final AsyncCallback<Void> callback) {
        projectServiceClient.updateFile(getPath(), content, null, new AsyncRequestCallback<Void>() {
            @Override
            protected void onSuccess(Void result) {
                callback.onSuccess(result);
            }

            @Override
            protected void onFailure(Throwable exception) {
                callback.onFailure(exception);
            }
        });
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof FileNode)) {
            return false;
        }

        FileNode other = (FileNode)o;
        return Objects.equals(getData(), other.getData());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getData());
    }

    @Override
    public boolean canContainsFolder() {
        return false;
    }
}
