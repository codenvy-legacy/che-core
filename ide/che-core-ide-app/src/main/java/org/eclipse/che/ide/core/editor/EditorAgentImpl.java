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
package org.eclipse.che.ide.core.editor;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.editor.EditorInitException;
import org.eclipse.che.ide.api.editor.EditorInput;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.editor.EditorPartPresenter.EditorPartCloseHandler;
import org.eclipse.che.ide.api.editor.EditorProvider;
import org.eclipse.che.ide.api.editor.EditorRegistry;
import org.eclipse.che.ide.api.event.ActivePartChangedEvent;
import org.eclipse.che.ide.api.event.ActivePartChangedHandler;
import org.eclipse.che.ide.api.event.FileContentUpdateEvent;
import org.eclipse.che.ide.api.event.FileEvent;
import org.eclipse.che.ide.api.event.FileEvent.FileOperation;
import org.eclipse.che.ide.api.event.FileEventHandler;
import org.eclipse.che.ide.api.event.WindowActionEvent;
import org.eclipse.che.ide.api.event.WindowActionHandler;
import org.eclipse.che.ide.api.filetypes.FileType;
import org.eclipse.che.ide.api.filetypes.FileTypeRegistry;
import org.eclipse.che.ide.api.notification.Notification;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.parts.PartPresenter;
import org.eclipse.che.ide.api.parts.PartStackType;
import org.eclipse.che.ide.api.parts.PropertyListener;
import org.eclipse.che.ide.api.parts.WorkspaceAgent;
import org.eclipse.che.ide.api.project.node.HasStorablePath;
import org.eclipse.che.ide.api.project.tree.VirtualFile;
import org.eclipse.che.ide.api.texteditor.HasReadOnlyProperty;
import org.eclipse.che.ide.project.event.ResourceNodeDeletedEvent;
import org.eclipse.che.ide.project.event.ResourceNodeRenamedEvent;
import org.eclipse.che.ide.project.node.FileReferenceNode;
import org.eclipse.che.ide.project.node.FolderReferenceNode;
import org.eclipse.che.ide.project.node.ItemReferenceBasedNode;
import org.eclipse.che.ide.project.node.ModuleDescriptorNode;
import org.eclipse.che.ide.project.node.NodeManager;
import org.eclipse.che.ide.project.node.ResourceBasedNode;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.Unmarshallable;
import org.eclipse.che.ide.util.loging.Log;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import static org.eclipse.che.ide.api.event.FileEvent.FileOperation.CLOSE;
import static org.eclipse.che.ide.api.notification.Notification.Type.ERROR;
import static org.eclipse.che.ide.api.notification.Notification.Type.INFO;

/** @author Evgen Vidolob */
@Singleton
public class EditorAgentImpl implements EditorAgent {

    private final NavigableMap<String, EditorPartPresenter> openedEditors;
    private final EventBus                                  eventBus;
    private final NodeManager                               nodeManager;
    private final ProjectServiceClient                      projectService;
    private final DtoUnmarshallerFactory                    unmarshallerFactory;
    private final WorkspaceAgent                            workspace;
    private       List<EditorPartPresenter>                 dirtyEditors;
    private       FileTypeRegistry                          fileTypeRegistry;
    private       EditorRegistry                            editorRegistry;
    private       EditorPartPresenter                       activeEditor;
    private       NotificationManager                       notificationManager;
    private       CoreLocalizationConstant                  coreLocalizationConstant;

    @Inject
    public EditorAgentImpl(EventBus eventBus,
                           FileTypeRegistry fileTypeRegistry,
                           EditorRegistry editorRegistry,
                           final WorkspaceAgent workspace,
                           final NotificationManager notificationManager,
                           CoreLocalizationConstant coreLocalizationConstant,
                           NodeManager nodeManager,
                           ProjectServiceClient projectService,
                           DtoUnmarshallerFactory unmarshallerFactory) {
        super();
        this.eventBus = eventBus;
        this.fileTypeRegistry = fileTypeRegistry;
        this.editorRegistry = editorRegistry;
        this.workspace = workspace;
        this.notificationManager = notificationManager;
        this.coreLocalizationConstant = coreLocalizationConstant;
        this.nodeManager = nodeManager;
        openedEditors = new TreeMap<>();
        this.projectService = projectService;
        this.unmarshallerFactory = unmarshallerFactory;

        bind();
    }

    protected void bind() {
        eventBus.addHandler(ActivePartChangedEvent.TYPE, activePartChangedHandler);
        eventBus.addHandler(FileEvent.TYPE, fileEventHandler);
        eventBus.addHandler(WindowActionEvent.TYPE, windowActionHandler);

        eventBus.addHandler(ResourceNodeDeletedEvent.getType(), new ResourceNodeDeletedEvent.ResourceNodeDeletedHandler() {
            @Override
            public void onResourceEvent(ResourceNodeDeletedEvent event) {
                ResourceBasedNode node = event.getNode();

                if (node instanceof FileReferenceNode) {
                    for (EditorPartPresenter editor : openedEditors.values()) {
                        VirtualFile deletedVFile = (VirtualFile)node;
                        if (deletedVFile.getPath().equals(editor.getEditorInput().getFile().getPath())) {
                            eventBus.fireEvent(new FileEvent(editor.getEditorInput().getFile(), CLOSE));
                        }
                    }
                } else if (node instanceof FolderReferenceNode) {
                    for (EditorPartPresenter editor : openedEditors.values()) {
                        if (editor.getEditorInput().getFile().getPath().startsWith(((FolderReferenceNode)node).getStorablePath())) {
                            eventBus.fireEvent(new FileEvent(editor.getEditorInput().getFile(), CLOSE));
                        }
                    }
                } else if (node instanceof ModuleDescriptorNode) {
                    for (EditorPartPresenter editor : openedEditors.values()) {
                        VirtualFile virtualFile = editor.getEditorInput().getFile();
                        if (virtualFile.getProject() != null
                            && virtualFile.getProject().getProjectDescriptor().equals(node.getProjectDescriptor())) {
                            eventBus.fireEvent(new FileEvent(virtualFile, CLOSE));
                        }
                        if (node.getParent() == null || !(node.getParent() instanceof HasStorablePath)) {
                            return;
                        }

                        String parentPath = ((HasStorablePath)node.getParent()).getStorablePath();
                        String openFileName = virtualFile.getName();
                        String openFilePath = virtualFile.getPath();
                        if (openFilePath.contains(parentPath) && openFileName.equals("modules")) {
                            eventBus.fireEvent(new FileContentUpdateEvent(openFilePath));
                        }
                    }
                }
            }
        });

        eventBus.addHandler(ResourceNodeRenamedEvent.getType(), new ResourceNodeRenamedEvent.ResourceNodeRenamedHandler() {
            @Override
            public void onResourceRenamedEvent(ResourceNodeRenamedEvent event) {
                ResourceBasedNode<?> resourceBaseNode = event.getNode();

                if (resourceBaseNode instanceof FileReferenceNode) {
                    FileReferenceNode fileReferenceNode = (FileReferenceNode)event.getNode();

                    String oldPath = fileReferenceNode.getPath();

                    if (event.getNewDataObject() instanceof ItemReference) {
                        ItemReferenceBasedNode wrapped =
                                nodeManager.wrap((ItemReference)event.getNewDataObject(), fileReferenceNode.getProjectDescriptor());
                        if (wrapped instanceof FileReferenceNode) {
                            updateEditorNode(oldPath, (FileReferenceNode)wrapped);
                        }
                    }
                } else if (resourceBaseNode instanceof FolderReferenceNode || resourceBaseNode instanceof ModuleDescriptorNode) {
                    HasStorablePath renamedTargetStoragePath = ((HasStorablePath)resourceBaseNode);
                    final String oldTargetPath = renamedTargetStoragePath.getStorablePath();
                    final String newTargetPath;
                    if (resourceBaseNode instanceof FolderReferenceNode) {
                        newTargetPath = ((ItemReference)event.getNewDataObject()).getPath();
                    } else {
                        newTargetPath = ((ProjectDescriptor)event.getNewDataObject()).getPath();
                    }
                    final Unmarshallable<ItemReference> unmarshaller = unmarshallerFactory.newUnmarshaller(ItemReference.class);
                    updateEditorPartsAfterRename(new LinkedList<EditorPartPresenter>(openedEditors.values()),
                                                 oldTargetPath,
                                                 newTargetPath,
                                                 unmarshaller);
                }
            }
        });

    }

    //todo Warning: this code should be reworked or deleted when folders and maven modules won't being closed after rename
    /**
     * Recursive update opened editor parts after renaming their parent folder or parent module
     * @param editorParts list opened editor parts
     */
    private void updateEditorPartsAfterRename(final LinkedList<EditorPartPresenter> editorParts,
                                              final String oldTargetPath,
                                              final String newTargetPath,
                                              final Unmarshallable<ItemReference> unmarshaller) {
        if (editorParts.isEmpty()) {
            return;
        }
        final EditorPartPresenter editorPart = editorParts.pop();
        final String oldFilePath = editorPart.getEditorInput().getFile().getPath();

        if (!oldFilePath.startsWith(oldTargetPath + "/")) {
            updateEditorPartsAfterRename(editorParts, oldTargetPath, newTargetPath, unmarshaller);
        }

        final String newFilePath = oldFilePath.replaceFirst(oldTargetPath, newTargetPath);

        projectService.getItem(newFilePath, new AsyncRequestCallback<ItemReference>(unmarshaller) {
            @Override
            protected void onSuccess(ItemReference result) {
                FileReferenceNode fileReferenceNode = ((FileReferenceNode)editorPart.getEditorInput().getFile());
                ProjectDescriptor projectDescriptor = fileReferenceNode.getProjectDescriptor();
                final ItemReferenceBasedNode wrappedNode = nodeManager.wrap(result, projectDescriptor);

                if (wrappedNode instanceof FileReferenceNode) {
                    updateEditorNode(oldFilePath, (FileReferenceNode)wrappedNode);
                }
                updateEditorPartsAfterRename(editorParts, oldTargetPath, newTargetPath, unmarshaller);
            }

            @Override
            protected void onFailure(Throwable exception) {
                updateEditorPartsAfterRename(editorParts, oldTargetPath, newTargetPath, unmarshaller);
            }
        });
    }

    /** Used to notify {@link EditorAgentImpl} that editor has closed */
    private final EditorPartCloseHandler editorClosed = new EditorPartCloseHandler() {
        @Override
        public void onClose(EditorPartPresenter editor) {
            editorClosed(editor);
        }
    };

    private final FileEventHandler fileEventHandler = new FileEventHandler() {
        @Override
        public void onFileOperation(final FileEvent event) {
            if (event.getOperationType() == FileOperation.OPEN) {
                openEditor(event.getFile());
            } else if (event.getOperationType() == CLOSE) {
                // close associated editor. OR it can be closed itself TODO
            }
        }
    };

    private final ActivePartChangedHandler activePartChangedHandler = new ActivePartChangedHandler() {
        @Override
        public void onActivePartChanged(ActivePartChangedEvent event) {
            if (event.getActivePart() instanceof EditorPartPresenter) {
                activeEditor = (EditorPartPresenter)event.getActivePart();
                activeEditor.activate();
            }
        }
    };

    private final WindowActionHandler windowActionHandler = new WindowActionHandler() {
        @Override
        public void onWindowClosing(final WindowActionEvent event) {
            for (EditorPartPresenter editorPartPresenter : openedEditors.values()) {
                if (editorPartPresenter.isDirty()) {
                    event.setMessage(coreLocalizationConstant.changesMayBeLost());
                }
            }
        }

        @Override
        public void onWindowClosed(WindowActionEvent event) {
        }
    };

    /** {@inheritDoc} */
    @Override
    public void openEditor(@NotNull final VirtualFile file) {
        doOpen(file, null);
    }

    @Override
    public void openEditor(@NotNull VirtualFile file, @NotNull OpenEditorCallback callback) {
        doOpen(file, callback);
    }

    private void doOpen(final VirtualFile file, final OpenEditorCallback callback) {
        String filePath = file.getPath();
        if (openedEditors.containsKey(filePath)) {
            workspace.setActivePart(openedEditors.get(filePath));
        } else {
            FileType fileType = fileTypeRegistry.getFileTypeByFile(file);
            EditorProvider editorProvider = editorRegistry.getEditor(fileType);
            final EditorPartPresenter editor = editorProvider.getEditor();
            try {
                editor.init(new EditorInputImpl(fileType, file));
                editor.addCloseHandler(editorClosed);
            } catch (EditorInitException e) {
                Log.error(getClass(), e);
            }
            workspace.openPart(editor, PartStackType.EDITING);
            openedEditors.put(file.getPath(), editor);

            workspace.setActivePart(editor);
            editor.addPropertyListener(new PropertyListener() {
                @Override
                public void propertyChanged(PartPresenter source, int propId) {
                    if (propId == EditorPartPresenter.PROP_INPUT) {
                        if (editor instanceof HasReadOnlyProperty) {
                            ((HasReadOnlyProperty)editor).setReadOnly(file.isReadOnly());
                        }
                        if (callback != null) {
                            callback.onEditorOpened(editor);
                        }
                    }

                }
            });

        }
    }

    @Override
    public void activateEditor(@NotNull EditorPartPresenter editor) {
        workspace.setActivePart(editor);
    }

    /** {@inheritDoc} */
    @Override
    public List<EditorPartPresenter> getDirtyEditors() {
        List<EditorPartPresenter> dirtyEditors = new ArrayList<>();
        for (EditorPartPresenter partPresenter : openedEditors.values()) {
            if (partPresenter.isDirty()) {
                dirtyEditors.add(partPresenter);
            }
        }
        return dirtyEditors;
    }

    /** @param editor */
    protected void editorClosed(EditorPartPresenter editor) {
        String closedFilePath = editor.getEditorInput().getFile().getPath();
        openedEditors.remove(closedFilePath);

        //call close() method
        editor.close(false);

        if (activeEditor == null) {
            return;
        }

        String activeFilePath = activeEditor.getEditorInput().getFile().getPath();
        if (activeFilePath.equals(closedFilePath)) {
            activeEditor = null;
        }
    }

    /** {@inheritDoc} */
    @Override
    @NotNull
    public NavigableMap<String, EditorPartPresenter> getOpenedEditors() {
        return openedEditors;
    }

    /** {@inheritDoc} */
    @Override
    public void saveAll(final AsyncCallback callback) {
        dirtyEditors = getDirtyEditors();
        if (dirtyEditors.isEmpty()) {
            Notification notification = new Notification(coreLocalizationConstant.allFilesSaved(), INFO);
            notificationManager.showNotification(notification);
            callback.onSuccess("Success");
        } else {
            doSave(callback);
        }
    }

    private void doSave(final AsyncCallback callback) {
        final EditorPartPresenter partPresenter = dirtyEditors.get(0);
        partPresenter.doSave(new AsyncCallback<EditorInput>() {
            @Override
            public void onFailure(Throwable caught) {
                callback.onFailure(caught);
                Notification notification = new Notification(coreLocalizationConstant.someFilesCanNotBeSaved(), ERROR);
                notificationManager.showNotification(notification);
            }

            @Override
            public void onSuccess(EditorInput result) {
                dirtyEditors.remove(partPresenter);
                if (dirtyEditors.isEmpty()) {
                    Notification notification = new Notification(coreLocalizationConstant.allFilesSaved(), INFO);
                    notificationManager.showNotification(notification);
                    callback.onSuccess("Success");
                } else {
                    doSave(callback);
                }
            }
        });
    }

    //TODO highly recommend to refactor or remove this method
    private void updateEditorNode(@NotNull String path, @NotNull VirtualFile virtualFile) {
        final EditorPartPresenter editor = openedEditors.remove(path);
        if (editor != null) {
            editor.getEditorInput().setFile(virtualFile);
            getOpenedEditors().put(virtualFile.getPath(), editor);
            editor.onFileChanged();
        }
    }

    /** {@inheritDoc} */
    @Override
    public EditorPartPresenter getActiveEditor() {
        return activeEditor;
    }

}
