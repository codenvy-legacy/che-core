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
import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.project.node.factory.NodeFactory;
import org.eclipse.che.ide.project.shared.NodesResources;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.Unmarshallable;
import org.eclipse.che.ide.api.project.node.Node;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
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

    /** ***************** Children operations ********************* */

    @Nonnull
    public Promise<List<Node>> getChildren(@Nonnull ItemReference itemReference,
                                           @Nonnull ProjectDescriptor relProjectDescriptor,
                                           @Nonnull NodeSettings nodeSettings,
                                           @Nullable ItemReferenceChainFilter... filters) {
        return getChildren(itemReference.getPath(), relProjectDescriptor, nodeSettings, filters);
    }

    @Nonnull
    public Promise<List<Node>> getChildren(@Nonnull ProjectDescriptor projectDescriptor,
                                           @Nonnull NodeSettings nodeSettings,
                                           @Nullable ItemReferenceChainFilter... filters) {
        return getChildren(projectDescriptor.getPath(), projectDescriptor, nodeSettings, filters);
    }

    @Nonnull
    public Promise<List<Node>> getChildren(@Nonnull String path,
                                           @Nonnull ProjectDescriptor relProjectDescriptor,
                                           @Nonnull NodeSettings nodeSettings,
                                           final @Nullable ItemReferenceChainFilter... filters) {
        return AsyncPromiseHelper.createFromAsyncRequest(getItemReferenceRC(path))
                .thenPromise(getListFromArray()) //TODO remove this function after JsoArray remove in 4.x
                .thenPromise(filterItemReference(filters))
                .thenPromise(createItemReferenceNodes(relProjectDescriptor, nodeSettings))
                .catchError(handleError());
    }

    @Nonnull
    public RequestCall<Array<ItemReference>> getItemReferenceRC(@Nonnull final String path) {
        return new RequestCall<Array<ItemReference>>() {
            @Override
            public void makeCall(AsyncCallback<Array<ItemReference>> callback) {
                projectService.getChildren(path, _callback(callback, dtoUnmarshaller.newArrayUnmarshaller(ItemReference.class)));
            }
        };
    }

    @Nonnull
    public <T extends ItemReference> Function<Array<T>, Promise<List<T>>> getListFromArray() {
        return new Function<Array<T>, Promise<List<T>>>() {
            @Override
            public Promise<List<T>> apply(Array<T> objects) throws FunctionException {
                return Promises.resolve((List<T>)Lists.newArrayList(objects.asIterable()));
            }
        };
    }

    private Function<List<ItemReference>, Promise<List<ItemReference>>> filterItemReference(
            final @Nullable ItemReferenceChainFilter... filters) {
        if (filters == null || filters.length == 0) {
            return self();
        }

        return new Function<List<ItemReference>, Promise<List<ItemReference>>>() {
            @Override
            public Promise<List<ItemReference>> apply(List<ItemReference> itemReference) throws FunctionException {

                Promise<List<ItemReference>> internalPromise = null;

                for (final ItemReferenceChainFilter filter : filters) {
                    if (internalPromise == null) {
                        internalPromise = filter.process(itemReference);
                    } else {
                        internalPromise.thenPromise(new Function<List<ItemReference>, Promise<List<ItemReference>>>() {
                            @Override
                            public Promise<List<ItemReference>> apply(List<ItemReference> secondPass) throws FunctionException {
                                return filter.process(secondPass);
                            }
                        });
                    }
                }

                return internalPromise;
            }
        };
    }

    @Nonnull
    private Function<List<ItemReference>, Promise<List<Node>>> createItemReferenceNodes(
            @Nonnull final ProjectDescriptor relProjectDescriptor,
            @Nonnull final NodeSettings nodeSettings) {
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





//                    if ("file".equals(itemReference.getType())) {
//                        nodes.add(nodeFactory.newFileReferenceNode(itemReference, relProjectDescriptor, nodeSettings));
//                    } else if ("folder".equals(itemReference.getType())) {
//                        nodes.add(nodeFactory.newFolderReferenceNode(itemReference, relProjectDescriptor, nodeSettings));
//                    } else if ("project".equals(itemReference.getType())) {
//                        if (modules == null) {
//                            modules = new ArrayList<>();
//                        }
//                        modules.add(itemReference);
//                    }
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

    @Nonnull
    private Operation<Node> _collectNodesFromPromises(@Nonnull final List<Node> collector) {
        return new Operation<Node>() {
            @Override
            public void apply(Node nodes) throws OperationException {
                collector.add(nodes);
            }
        };
    }

    @Nonnull
    private Function<PromiseError, List<Node>> handleError() {
        return new Function<PromiseError, List<Node>>() {
            @Override
            public List<Node> apply(PromiseError arg) throws FunctionException {
                return Collections.emptyList();
            }
        };
    }

    /** ***************** Project Reference operations ********************* */

    @Nonnull
    public Promise<List<Node>> getProjects() {
        return AsyncPromiseHelper.createFromAsyncRequest(getProjectsRC()).then(createProjectReferenceNodes());
    }

    @Nonnull
    private RequestCall<Array<ProjectReference>> getProjectsRC() {
        return new RequestCall<Array<ProjectReference>>() {
            @Override
            public void makeCall(AsyncCallback<Array<ProjectReference>> callback) {
                projectService.getProjects(_callback(callback, dtoUnmarshaller.newArrayUnmarshaller(ProjectReference.class)));
            }
        };
    }

    @Nonnull
    private Function<Array<ProjectReference>, List<Node>> createProjectReferenceNodes() {
        return new Function<Array<ProjectReference>, List<Node>>() {
            @Override
            public List<Node> apply(Array<ProjectReference> projects) throws FunctionException {
                if (projects == null) {
                    return Collections.emptyList();
                }

                NodeSettings nodeSettings = nodeSettingsProvider.getSettings();
                if (nodeSettings == null) {
                    nodeSettings = NodeSettings.DEFAULT_SETTINGS;
                }

                List<Node> projectList = new ArrayList<>(projects.size());

                for (ProjectReference reference : projects.asIterable()) {
                    ProjectReferenceNode node = nodeFactory.newProjectReferenceNode(reference, convert(reference), nodeSettings);
                    projectList.add(node);
                }

                return projectList;
            }
        };
    }

    /** ***************** Content methods ********************* */

    @Nonnull
    public Promise<String> getContent(@Nonnull final VirtualFile virtualFile) {
        return AsyncPromiseHelper.createFromAsyncRequest(contentGetRC(virtualFile));
    }

    @Nonnull
    private RequestCall<String> contentGetRC(@Nonnull final VirtualFile vFile) {
        return new RequestCall<String>() {
            @Override
            public void makeCall(AsyncCallback<String> callback) {
                projectService.getFileContent(vFile.getPath(), _callback(callback, dtoUnmarshaller.newUnmarshaller(String.class)));
            }
        };
    }

    @Nonnull
    public Promise<Void> updateContent(@Nonnull VirtualFile virtualFile, @Nonnull String content) {
        return AsyncPromiseHelper.createFromAsyncRequest(contentUpdateRC(virtualFile, content));
    }

    @Nonnull
    private RequestCall<Void> contentUpdateRC(@Nonnull final VirtualFile vFile, @Nonnull final String content) {
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

    /** ***************** Common methods ********************* */

    @Nonnull
    protected <T> AsyncRequestCallback<T> _callback(@Nonnull final AsyncCallback<T> callback, @Nonnull Unmarshallable<T> u) {
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

    @Nonnull
    public ProjectDescriptor convert(@Nonnull ProjectReference reference) {
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

    @Nonnull
    public NodesResources getNodesResources() {
        return nodesResources;
    }

    @Nonnull
    public ProjectDescriptorNode wrap(@Nonnull ProjectDescriptor projectDescriptor) {
        NodeSettings nodeSettings = nodeSettingsProvider.getSettings();
        return nodeFactory.newProjectDescriptorNode(projectDescriptor, nodeSettings == null ? NodeSettings.DEFAULT_SETTINGS : nodeSettings);
    }

    @Nullable
    public ItemReferenceBasedNode wrap(@Nonnull ItemReference itemReference, @Nonnull ProjectDescriptor relProjectDescriptor) {
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
