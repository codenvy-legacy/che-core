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

import com.google.gwt.core.client.JsArrayMixed;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.project.shared.dto.ProjectReference;
import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.FunctionException;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.api.promises.client.callback.AsyncPromiseHelper;
import org.eclipse.che.api.promises.client.callback.AsyncPromiseHelper.RequestCall;
import org.eclipse.che.api.promises.client.js.Promises;
import org.eclipse.che.ide.api.project.node.settings.NodeSettings;
import org.eclipse.che.ide.api.project.node.settings.SettingsProvider;
import org.eclipse.che.ide.api.project.tree.VirtualFile;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.project.node.factory.NodeFactory;
import org.eclipse.che.ide.project.shared.NodesResources;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.Unmarshallable;
import org.eclipse.che.ide.api.project.node.Node;

import javax.validation.constraints.NotNull;
import org.eclipse.che.commons.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Helper class to define various functionality related with nodes.
 * Such as get node children. Wrapping nodes.
 *
 * @author Vlad Zhukovskiy
 */
@Singleton
public class NodeManager {
    protected final NodeFactory            nodeFactory;
    protected final ProjectServiceClient   projectService;
    protected final DtoUnmarshallerFactory dtoUnmarshaller;
    protected final NodesResources         nodesResources;
    protected final SettingsProvider       nodeSettingsProvider;
    protected final DtoFactory             dtoFactory;

    @Inject
    public NodeManager(NodeFactory nodeFactory,
                       ProjectServiceClient projectService,
                       DtoUnmarshallerFactory dtoUnmarshaller,
                       NodesResources nodesResources,
                       SettingsProvider nodeSettingsProvider,
                       DtoFactory dtoFactory) {
        this.nodeFactory = nodeFactory;
        this.projectService = projectService;
        this.dtoUnmarshaller = dtoUnmarshaller;
        this.nodesResources = nodesResources;
        this.nodeSettingsProvider = nodeSettingsProvider;
        this.dtoFactory = dtoFactory;
    }

    /** *************** Children operations ********************* */

    @NotNull
    public Promise<List<Node>> getChildren(@NotNull ItemReference itemReference,
                                           @NotNull ProjectDescriptor relProjectDescriptor,
                                           @NotNull NodeSettings nodeSettings) {
        return getChildren(itemReference.getPath(), relProjectDescriptor, nodeSettings);
    }

    @NotNull
    public Promise<List<Node>> getChildren(@NotNull ProjectDescriptor projectDescriptor,
                                           @NotNull NodeSettings nodeSettings) {
        return getChildren(projectDescriptor.getPath(), projectDescriptor, nodeSettings);
    }

    @NotNull
    public Promise<List<Node>> getChildren(@NotNull String path,
                                           @NotNull ProjectDescriptor relProjectDescriptor,
                                           @NotNull NodeSettings nodeSettings) {
        return AsyncPromiseHelper.createFromAsyncRequest(getItemReferenceRC(path))
                                 .thenPromise(filterItemReference())
                                 .thenPromise(createItemReferenceNodes(relProjectDescriptor, nodeSettings))
                                 .catchError(handleError());
    }

    @NotNull
    public RequestCall<List<ItemReference>> getItemReferenceRC(@NotNull final String path) {
        return new RequestCall<List<ItemReference>>() {
            @Override
            public void makeCall(AsyncCallback<List<ItemReference>> callback) {
                projectService.getChildren(path, _callback(callback, dtoUnmarshaller.newListUnmarshaller(ItemReference.class)));
            }
        };
    }

    public Function<List<ItemReference>, Promise<List<ItemReference>>> filterItemReference() {
        //filter item references before they will be transformed int nodes
        return self();
    }

    @NotNull
    private Function<List<ItemReference>, Promise<List<Node>>> createItemReferenceNodes(
            @NotNull final ProjectDescriptor relProjectDescriptor,
            @NotNull final NodeSettings nodeSettings) {
        return new Function<List<ItemReference>, Promise<List<Node>>>() {
            @Override
            public Promise<List<Node>> apply(List<ItemReference> itemRefList) throws FunctionException {
                if (itemRefList == null || itemRefList.isEmpty()) {
                    return Promises.resolve(Collections.<Node>emptyList());
                }

                final List<Node> nodes = new ArrayList<>(itemRefList.size());

                List<ItemReference> modules = null;

                for (ItemReference itemReference : itemRefList) {
                    //Skip files which starts with "." if enabled
                    if (!nodeSettings.isShowHiddenFiles() && itemReference.getName().startsWith(".")) {
                        continue;
                    }

                    Node node = createNodeByType(itemReference, relProjectDescriptor, nodeSettings);
                    if (node != null) {
                        nodes.add(node);
                    } else if ("project".equals(itemReference.getType())) {
                        if (modules == null) {
                            modules = new ArrayList<>();
                        }
                        modules.add(itemReference);
                    }

                    //NOTE if we want support more type nodes than we should refactor mechanism of hardcoded types for item references
                }

                if (modules == null) {
                    return Promises.resolve(nodes);
                }

                //else we have modules, so we have get them

                final List<Node> collector = new ArrayList<>(modules.size());

                Promise<?>[] promises = new Promise[modules.size()];

                for (int i = 0; i < promises.length; i++) {
                    promises[i] = getModule(modules.get(i), collector, nodeSettings);
                }

                return Promises.all(promises).then(new Function<JsArrayMixed, List<Node>>() {
                    @Override
                    public List<Node> apply(JsArrayMixed arg) throws FunctionException {
                        nodes.addAll(collector);
                        return nodes;
                    }
                });
            }
        };
    }

    public Node createNodeByType(ItemReference itemReference, ProjectDescriptor descriptor, NodeSettings settings) {
        if ("file".equals(itemReference.getType())) {
            return nodeFactory.newFileReferenceNode(itemReference, descriptor, settings);
        } else if ("folder".equals(itemReference.getType())) {
            return nodeFactory.newFolderReferenceNode(itemReference, descriptor, settings);
        }
        return null;
    }

    private Promise<Node> getModule(ItemReference module, List<Node> collector, NodeSettings nodeSettings) {
        return AsyncPromiseHelper.createFromAsyncRequest(getModuleRC(module))
                                 .then(createModuleNode(nodeSettings))
                                 .then(_collectNodesFromPromises(collector));
    }

    private RequestCall<ProjectDescriptor> getModuleRC(final ItemReference module) {
        return new RequestCall<ProjectDescriptor>() {
            @Override
            public void makeCall(AsyncCallback<ProjectDescriptor> callback) {
                projectService
                        .getProject(module.getPath(), _callback(callback, dtoUnmarshaller.newUnmarshaller(ProjectDescriptor.class)));
            }
        };
    }

    private Function<ProjectDescriptor, Node> createModuleNode(final NodeSettings nodeSettings) {
        return new Function<ProjectDescriptor, Node>() {
            @Override
            public Node apply(ProjectDescriptor module) throws FunctionException {
                //Skip files which starts with "." if enabled
//                if (!nodeSettings.isShowHiddenFiles() && module.getName().startsWith(".")) {
//                    continue;
//                }

                return nodeFactory.newModuleNode(module, nodeSettings);
            }
        };
    }

    @NotNull
    private Operation<Node> _collectNodesFromPromises(@NotNull final List<Node> collector) {
        return new Operation<Node>() {
            @Override
            public void apply(Node nodes) throws OperationException {
                collector.add(nodes);
            }
        };
    }

    @NotNull
    private Function<PromiseError, List<Node>> handleError() {
        return new Function<PromiseError, List<Node>>() {
            @Override
            public List<Node> apply(PromiseError arg) throws FunctionException {
                return Collections.emptyList();
            }
        };
    }

    /** *************** Project Reference operations ********************* */

    @NotNull
    public Promise<List<Node>> getProjects() {
        return AsyncPromiseHelper.createFromAsyncRequest(getProjectsRC()).then(createProjectReferenceNodes());
    }

    @NotNull
    private RequestCall<List<ProjectReference>> getProjectsRC() {
        return new RequestCall<List<ProjectReference>>() {
            @Override
            public void makeCall(AsyncCallback<List<ProjectReference>> callback) {
                projectService.getProjects(_callback(callback, dtoUnmarshaller.newListUnmarshaller(ProjectReference.class)));
            }
        };
    }

    @NotNull
    private Function<List<ProjectReference>, List<Node>> createProjectReferenceNodes() {
        return new Function<List<ProjectReference>, List<Node>>() {
            @Override
            public List<Node> apply(List<ProjectReference> projects) throws FunctionException {
                if (projects == null) {
                    return Collections.emptyList();
                }

                NodeSettings nodeSettings = nodeSettingsProvider.getSettings();
                if (nodeSettings == null) {
                    nodeSettings = NodeSettings.DEFAULT_SETTINGS;
                }

                List<Node> projectList = new ArrayList<>(projects.size());

                for (ProjectReference reference : projects) {
                    ProjectReferenceNode node = nodeFactory.newProjectReferenceNode(reference, convert(reference), nodeSettings);
                    projectList.add(node);
                }

                return projectList;
            }
        };
    }

    /** *************** Content methods ********************* */

    @NotNull
    public Promise<String> getContent(@NotNull final VirtualFile virtualFile) {
        return AsyncPromiseHelper.createFromAsyncRequest(contentGetRC(virtualFile));
    }

    @NotNull
    private RequestCall<String> contentGetRC(@NotNull final VirtualFile vFile) {
        return new RequestCall<String>() {
            @Override
            public void makeCall(AsyncCallback<String> callback) {
                projectService.getFileContent(vFile.getPath(), _callback(callback, dtoUnmarshaller.newUnmarshaller(String.class)));
            }
        };
    }

    @NotNull
    public Promise<Void> updateContent(@NotNull VirtualFile virtualFile, @NotNull String content) {
        return AsyncPromiseHelper.createFromAsyncRequest(contentUpdateRC(virtualFile, content));
    }

    @NotNull
    private RequestCall<Void> contentUpdateRC(@NotNull final VirtualFile vFile, @NotNull final String content) {
        return new RequestCall<Void>() {
            @Override
            public void makeCall(AsyncCallback<Void> callback) {
                projectService.updateFile(vFile.getPath(),
                                          content,
                                          vFile.getMediaType(),
                                          _callback(callback, dtoUnmarshaller.newUnmarshaller(Void.class)));
            }
        };
    }

    /** *************** Common methods ********************* */

    @NotNull
    protected <T> AsyncRequestCallback<T> _callback(@NotNull final AsyncCallback<T> callback, @NotNull Unmarshallable<T> u) {
        return new AsyncRequestCallback<T>(u) {
            @Override
            protected void onSuccess(T result) {
                callback.onSuccess(result);
            }

            @Override
            protected void onFailure(Throwable e) {
                callback.onFailure(e);
            }
        };
    }

    @NotNull
    public ProjectDescriptor convert(@NotNull ProjectReference reference) {
        ProjectDescriptor descriptor = dtoFactory.createDto(ProjectDescriptor.class);

        descriptor.setName(reference.getName());
        descriptor.setPath(reference.getPath());
        descriptor.setType(reference.getType());
        descriptor.setTypeName(reference.getTypeName());
        descriptor.setBaseUrl(reference.getUrl());
        descriptor.setIdeUrl(reference.getIdeUrl());
        descriptor.setWorkspaceId(reference.getWorkspaceId());
        descriptor.setWorkspaceName(reference.getWorkspaceName());
        descriptor.setVisibility(reference.getVisibility());
        descriptor.setCreationDate(reference.getCreationDate());
        descriptor.setModificationDate(reference.getModificationDate());
        descriptor.setDescription(reference.getDescription());
        descriptor.setProblems(reference.getProblems());

        return descriptor;
    }

    @NotNull
    public NodesResources getNodesResources() {
        return nodesResources;
    }

    @NotNull
    public ProjectDescriptorNode wrap(@NotNull ProjectDescriptor projectDescriptor) {
        NodeSettings nodeSettings = nodeSettingsProvider.getSettings();
        return nodeFactory.newProjectDescriptorNode(projectDescriptor, nodeSettings == null ? NodeSettings.DEFAULT_SETTINGS : nodeSettings);
    }

    @Nullable
    public ItemReferenceBasedNode wrap(@NotNull ItemReference itemReference, @NotNull ProjectDescriptor relProjectDescriptor) {
        NodeSettings nodeSettings = nodeSettingsProvider.getSettings();
        if (nodeSettings == null) {
            nodeSettings = NodeSettings.DEFAULT_SETTINGS;
        }

        ItemReferenceBasedNode node = null;

        if ("file".equals(itemReference.getType())) {
            node = nodeFactory.newFileReferenceNode(itemReference, relProjectDescriptor, nodeSettings);
        } else if ("folder".equals(itemReference.getType())) {
            node = nodeFactory.newFolderReferenceNode(itemReference, relProjectDescriptor, nodeSettings);
        }

        return node;
    }

    public static boolean isProjectOrModuleNode(Node node) {
        return node instanceof ProjectDescriptorNode || node instanceof ModuleDescriptorNode;
    }

    protected <T> Function<T, Promise<T>> self() {
        return new Function<T, Promise<T>>() {
            @Override
            public Promise<T> apply(T self) throws FunctionException {
                return Promises.resolve(self);
            }
        };
    }

    public NodeFactory getNodeFactory() {
        return nodeFactory;
    }
}
