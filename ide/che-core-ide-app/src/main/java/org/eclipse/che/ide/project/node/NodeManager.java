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

import com.google.common.collect.Lists;
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
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.api.promises.client.callback.AsyncPromiseHelper;
import org.eclipse.che.api.promises.client.callback.AsyncPromiseHelper.RequestCall;
import org.eclipse.che.api.promises.client.js.Promises;
import org.eclipse.che.api.workspace.shared.dto.ModuleConfigDto;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.api.project.node.settings.NodeSettings;
import org.eclipse.che.ide.api.project.node.settings.SettingsProvider;
import org.eclipse.che.ide.api.project.tree.VirtualFile;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.part.explorer.project.FoldersOnTopFilter;
import org.eclipse.che.ide.project.node.factory.NodeFactory;
import org.eclipse.che.ide.project.node.icon.NodeIconProvider;
import org.eclipse.che.ide.project.shared.NodesResources;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.Unmarshallable;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.eclipse.che.api.promises.client.callback.PromiseHelper.newCallback;
import static org.eclipse.che.api.promises.client.callback.PromiseHelper.newPromise;

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
    protected final Set<NodeIconProvider>  nodeIconProvider;

    private ModuleDescriptorNode moduleNode;

    @Inject
    public NodeManager(NodeFactory nodeFactory,
                       ProjectServiceClient projectService,
                       DtoUnmarshallerFactory dtoUnmarshaller,
                       NodesResources nodesResources,
                       SettingsProvider nodeSettingsProvider,
                       DtoFactory dtoFactory,
                       Set<NodeIconProvider> nodeIconProvider) {
        this.nodeFactory = nodeFactory;
        this.projectService = projectService;
        this.dtoUnmarshaller = dtoUnmarshaller;
        this.nodesResources = nodesResources;
        this.nodeSettingsProvider = nodeSettingsProvider;
        this.dtoFactory = dtoFactory;
        this.nodeIconProvider = nodeIconProvider;
    }

    /** ********** Children operations ********************* */
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
    public Promise<List<Node>> getChildren(final @NotNull String path,
                                           @NotNull ProjectDescriptor relProjectDescriptor,
                                           @NotNull NodeSettings nodeSettings) {
        return newPromise(new RequestCall<List<ItemReference>>() {
            @Override
            public void makeCall(AsyncCallback<List<ItemReference>> callback) {
                projectService.getChildren(path, newCallback(callback, dtoUnmarshaller.newListUnmarshaller(ItemReference.class)));
            }
        }).thenPromise(filterItemReference())
          .thenPromise(createItemReferenceNodes(relProjectDescriptor, nodeSettings))
          .catchError(handleError());
    }

    protected Function<List<Node>, Promise<List<Node>>> sortNodes() {
        return new Function<List<Node>, Promise<List<Node>>>() {
            @Override
            public Promise<List<Node>> apply(List<Node> nodes) throws FunctionException {
                Collections.sort(nodes, new FoldersOnTopFilter());
                return Promises.resolve(nodes);
            }
        };
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

                for (ItemReference itemReference : itemRefList) {
                    //Skip files which starts with "." if enabled
                    if (!nodeSettings.isShowHiddenFiles() && itemReference.getName().startsWith(".")) {
                        continue;
                    }

                    Node node = createNodeByType(itemReference, relProjectDescriptor, nodeSettings);
                    if (node != null) {
                        nodes.add(node);
                    }

                }

                return Promises.resolve(nodes);
            }
        };
    }

    public Node createNodeByType(ItemReference itemReference, ProjectDescriptor descriptor, NodeSettings settings) {
        String itemType = itemReference.getType();

        if ("file".equals(itemType)) {
            return nodeFactory.newFileReferenceNode(itemReference, descriptor, settings);
        }

        if ("folder".equals(itemType)) {
            return nodeFactory.newFolderReferenceNode(itemReference, descriptor, settings);
        }

        if ("module".equals(itemType)) {
            moduleNode = null;

            for (ModuleConfigDto moduleConfigDto : descriptor.getModules()) {
                createModuleNode(itemReference, moduleConfigDto, descriptor, settings);

                if (moduleNode != null) {
                    return moduleNode;
                }
            }
        }

        return null;
    }

    public void createModuleNode(ItemReference itemReference,
                                 ModuleConfigDto moduleConfigDto,
                                 ProjectDescriptor descriptor,
                                 NodeSettings settings) {

        if (itemReference.getName().equals(moduleConfigDto.getName())) {
            moduleNode = nodeFactory.newModuleNode(moduleConfigDto, descriptor, settings);

            return;
        }

        for (ModuleConfigDto moduleConfig : moduleConfigDto.getModules()) {
            createModuleNode(itemReference, moduleConfig, descriptor, settings);
        }
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

    /** ********** Project Reference operations ********************* */
    public Promise<ProjectDescriptor> getProjectDescriptor(String path) {
        return AsyncPromiseHelper.createFromAsyncRequest(getProjectDescriptoRC(path));
    }

    private RequestCall<ProjectDescriptor> getProjectDescriptoRC(final String path) {
        return new RequestCall<ProjectDescriptor>() {
            @Override
            public void makeCall(AsyncCallback<ProjectDescriptor> callback) {
                projectService.getProject(path, _callback(callback, dtoUnmarshaller.newUnmarshaller(ProjectDescriptor.class)));
            }
        };
    }

    /**
     * Get project list and construct project nodes for the tree.
     *
     * @return list of the {@link ProjectDescriptorNode} nodes.
     */
    @NotNull
    public Promise<List<Node>> getProjectNodes() {
        return newPromise(new RequestCall<List<ProjectDescriptor>>() {
            @Override
            public void makeCall(AsyncCallback<List<ProjectDescriptor>> callback) {
                projectService.getProjects(true, newCallback(callback, dtoUnmarshaller.newListUnmarshaller(ProjectDescriptor.class)));
            }
        }).then(new Function<List<ProjectDescriptor>, List<Node>>() {
            @Override
            public List<Node> apply(List<ProjectDescriptor> projects) throws FunctionException {
                if (projects == null) {
                    return Collections.emptyList();
                }

                final NodeSettings settings = nodeSettingsProvider.getSettings();

                return Lists.transform(projects, new com.google.common.base.Function<ProjectDescriptor, Node>() {
                    @org.eclipse.che.commons.annotation.Nullable
                    @Override
                    public Node apply(@Nullable ProjectDescriptor project) {
                        return nodeFactory.newProjectDescriptorNode(project, settings == null ? NodeSettings.DEFAULT_SETTINGS : settings);
                    }
                });
            }
        });
    }

    /** ********** Content methods ********************* */

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

    /** ********** Common methods ********************* */

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

    public Set<NodeIconProvider> getNodeIconProvider() {
        return nodeIconProvider;
    }
}
