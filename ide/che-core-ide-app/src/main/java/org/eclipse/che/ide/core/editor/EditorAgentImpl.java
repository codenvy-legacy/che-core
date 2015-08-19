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
import org.eclipse.che.ide.api.event.DeleteModuleEvent;
import org.eclipse.che.ide.api.event.DeleteModuleEventHandler;
import org.eclipse.che.ide.api.event.FileEvent;
import org.eclipse.che.ide.api.event.FileEvent.FileOperation;
import org.eclipse.che.ide.api.event.FileEventHandler;
import org.eclipse.che.ide.api.event.ItemEvent;
import org.eclipse.che.ide.api.event.ItemHandler;
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
import org.eclipse.che.ide.api.project.tree.VirtualFile;
import org.eclipse.che.ide.api.project.tree.generic.FolderNode;
import org.eclipse.che.ide.api.project.tree.generic.ItemNode;
import org.eclipse.che.ide.api.project.tree.generic.ProjectNode;
import org.eclipse.che.ide.api.texteditor.HasReadOnlyProperty;
import org.eclipse.che.ide.util.loging.Log;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import static org.eclipse.che.ide.api.event.FileEvent.FileOperation.CLOSE;
import static org.eclipse.che.ide.api.event.ItemEvent.ItemOperation.DELETED;
import static org.eclipse.che.ide.api.notification.Notification.Type.ERROR;
import static org.eclipse.che.ide.api.notification.Notification.Type.INFO;

/** @author Evgen Vidolob */
@Singleton
public class EditorAgentImpl implements EditorAgent {

    private final NavigableMap<String, EditorPartPresenter> openedEditors;
    /** Used to notify {@link EditorAgentImpl} that editor has closed */
    private final EditorPartCloseHandler editorClosed     = new EditorPartCloseHandler() {
        @Override
        public void onClose(EditorPartPresenter editor) {
            editorClosed(editor);
        }
    };
    private final FileEventHandler       fileEventHandler = new FileEventHandler() {
        @Override
        public void onFileOperation(final FileEvent event) {
            if (event.getOperationType() == FileOperation.OPEN) {
                openEditor(event.getFile());
            } else if (event.getOperationType() == CLOSE) {
                // close associated editor. OR it can be closed itself TODO
            }
        }
    };
    private final EventBus                  eventBus;
    private final WorkspaceAgent            workspace;
    private       List<EditorPartPresenter> dirtyEditors;
    private       FileTypeRegistry          fileTypeRegistry;
    private       EditorRegistry            editorRegistry;
    private       EditorPartPresenter       activeEditor;
    private final ActivePartChangedHandler activePartChangedHandler = new ActivePartChangedHandler() {
        @Override
        public void onActivePartChanged(ActivePartChangedEvent event) {
            if (event.getActivePart() instanceof EditorPartPresenter) {
                activeEditor = (EditorPartPresenter)event.getActivePart();
                activeEditor.activate();
            }
        }
    };
    private NotificationManager      notificationManager;
    private CoreLocalizationConstant coreLocalizationConstant;
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

    @Inject
    public EditorAgentImpl(EventBus eventBus,
                           FileTypeRegistry fileTypeRegistry,
                           EditorRegistry editorRegistry,
                           final WorkspaceAgent workspace,
                           final NotificationManager notificationManager,
                           CoreLocalizationConstant coreLocalizationConstant) {
        super();
        this.eventBus = eventBus;
        this.fileTypeRegistry = fileTypeRegistry;
        this.editorRegistry = editorRegistry;
        this.workspace = workspace;
        this.notificationManager = notificationManager;
        this.coreLocalizationConstant = coreLocalizationConstant;
        openedEditors = new TreeMap<>();

        bind();
    }

    protected void bind() {
        eventBus.addHandler(ActivePartChangedEvent.TYPE, activePartChangedHandler);
        eventBus.addHandler(FileEvent.TYPE, fileEventHandler);
        eventBus.addHandler(WindowActionEvent.TYPE, windowActionHandler);
        eventBus.addHandler(ItemEvent.TYPE, new ItemHandler() {
            @Override
            public void onItem(ItemEvent event) {
                final ItemNode item = event.getItem();
                if (event.getOperation() == DELETED && item instanceof FolderNode) {
                    closeAllFilesByPath(item.getPath());
                }
            }
        });
        eventBus.addHandler(DeleteModuleEvent.TYPE, new DeleteModuleEventHandler() {
            @Override
            public void onModuleDeleted(DeleteModuleEvent event) {
                ProjectNode projectNode = event.getModule();
                closeAllFilesByModule(projectNode);
            }
        });
    }

    private void closeAllFilesByModule(ProjectNode projectNode) {
        for (EditorPartPresenter editor : getOpenedEditors().values()) {
            VirtualFile virtualFile = editor.getEditorInput().getFile();
            ProjectNode projectParent = virtualFile.getProject();

            if (projectParent.equals(projectNode)) {
                eventBus.fireEvent(new FileEvent(virtualFile, CLOSE));
            }
        }
    }

    private void closeAllFilesByPath(String path) {
        for (EditorPartPresenter editor : getOpenedEditors().values()) {
            if (editor.getEditorInput().getFile().getPath().startsWith(path)) {
                eventBus.fireEvent(new FileEvent(editor.getEditorInput().getFile(), CLOSE));
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void openEditor(@Nonnull final VirtualFile file) {
        doOpen(file, null);
    }

    @Override
    public void openEditor(@Nonnull VirtualFile file, @Nonnull OpenEditorCallback callback) {
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
                        if(editor instanceof HasReadOnlyProperty) {
                            ((HasReadOnlyProperty)editor).setReadOnly(file.isReadOnly());
                        }
                        if(callback != null) {
                            callback.onEditorOpened(editor);
                        }
                    }

                }
            });

        }
    }

    @Override
    public void activateEditor(@Nonnull EditorPartPresenter editor) {
        workspace.setActivePart(editor);
    }

    /** {@inheritDoc} */
    @Override
    public List<EditorPartPresenter> getDirtyEditors() {
        List<EditorPartPresenter> dirtyEditors = new ArrayList<>();
        for (EditorPartPresenter partPresenter : getOpenedEditors().values()) {
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
    public @Nonnull NavigableMap<String, EditorPartPresenter> getOpenedEditors() {
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

    /** {@inheritDoc} */
    @Override
    public void updateEditorNode(@Nonnull String path, @Nonnull VirtualFile virtualFile) {
        final EditorPartPresenter editor = getOpenedEditors().remove(path);
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
