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
import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.project.node.factory.NodeFactory;
import org.eclipse.che.ide.project.shared.NodesResources;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.Unmarshallable;
import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.util.loging.Log;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Vlad Zhukovskiy
 */
@Singleton
public class ResourceNodeManager {
    private       NodeFactory            nodeFactory;
    private       ProjectServiceClient   projectService;
    private final DtoUnmarshallerFactory dtoUnmarshaller;
    private final NodesResources         nodesResources;
    private final SettingsProvider       nodeSettingsProvider;
    private final DtoFactory             dtoFactory;

    @Inject
    public ResourceNodeManager(NodeFactory nodeFactory,
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

    @Nonnull
    public Promise<List<Node>> getChildren(@Nonnull ItemReference itemReference,
                                           @Nonnull ProjectDescriptor relProjectDescriptor,
                                           @Nonnull NodeSettings nodeSettings) {
        return getChildren(itemReference.getPath(), relProjectDescriptor, nodeSettings);
    }

    @Nonnull
    public Promise<List<Node>> getChildren(@Nonnull ProjectDescriptor projectDescriptor,
                                           @Nonnull NodeSettings nodeSettings) {
        return getChildren(projectDescriptor.getPath(), projectDescriptor, nodeSettings);
    }

    @Nonnull
    public Promise<List<Node>> getChildren(@Nonnull String path,
                                           @Nonnull ProjectDescriptor relProjectDescriptor,
                                           @Nonnull NodeSettings nodeSettings) {
        //Very dirty hack, need to find better solution to collect nodes from promises
//        final List<Node> collector = new ArrayList<>();
//
//        Promise<List<Node>> moduleNodes = AsyncPromiseHelper.createFromAsyncRequest(getModulesRequestCall(path))
//                                                            .then(_createModuleNodes(nodeSettings))
//                                                            .then(_collectNodesFromPromises(collector))
//                                                            .catchError(handleError());

        Promise<List<Node>> itemRefNodes = AsyncPromiseHelper.createFromAsyncRequest(getItemReferenceRequestCall(path))
                                                             .thenPromise(_createItemReferenceNodes(relProjectDescriptor, nodeSettings))
//                                                             .then(_collectNodesFromPromises(collector))
                                                             .catchError(handleError());

        return itemRefNodes;

//        return Promises.all(moduleNodes, itemRefNodes).then(new Function<JsArrayMixed, List<Node>>() {
//            @Override
//            public List<Node> apply(JsArrayMixed arg) throws FunctionException {
//                return collector;
//            }
//        });
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

    @Nonnull
    public Promise<List<Node>> getProjects() {
        return AsyncPromiseHelper.createFromAsyncRequest(getProjectsRequestCall())
                                 .then(_createProjectReferenceNodes());
    }

    @Nonnull
    private Function<Array<ProjectReference>, List<Node>> _createProjectReferenceNodes() {
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

    @Nonnull
    private RequestCall<Array<ProjectReference>> getProjectsRequestCall() {
        return new RequestCall<Array<ProjectReference>>() {
            @Override
            public void makeCall(AsyncCallback<Array<ProjectReference>> callback) {
                projectService.getProjects(_createCallback(callback, dtoUnmarshaller.newArrayUnmarshaller(ProjectReference.class)));
            }
        };
    }

    @Nonnull
    private <T> AsyncRequestCallback<T> _createCallback(@Nonnull final AsyncCallback<T> callback,
                                                        @Nonnull Unmarshallable<T> u) {
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
    private RequestCall<Array<ProjectDescriptor>> getModulesRequestCall(@Nonnull final String path) {
        return new RequestCall<Array<ProjectDescriptor>>() {
            @Override
            public void makeCall(AsyncCallback<Array<ProjectDescriptor>> callback) {
                projectService.getModules(path, _createCallback(callback, dtoUnmarshaller.newArrayUnmarshaller(ProjectDescriptor.class)));
            }
        };
    }

    @Nonnull
    private Function<Array<ProjectDescriptor>, List<Node>> _createModuleNodes(@Nonnull final NodeSettings nodeSettings) {
        return new Function<Array<ProjectDescriptor>, List<Node>>() {
            @Override
            public List<Node> apply(Array<ProjectDescriptor> modules) throws FunctionException {
                if (modules == null || modules.isEmpty()) {
                    return Collections.emptyList();
                }

                List<Node> nodes = new ArrayList<>(modules.size());

                for (ProjectDescriptor moduleDescriptor : modules.asIterable()) {
                    //Skip files which starts with "." if enabled
                    if (!nodeSettings.isShowHiddenFiles() && moduleDescriptor.getName().startsWith(".")) {
                        continue;
                    }

                    ModuleDescriptorNode moduleDescriptorNode = nodeFactory.newModuleNode(moduleDescriptor, nodeSettings);
                    nodes.add(moduleDescriptorNode);
                }

                return nodes;
            }
        };
    }

    @Nonnull
    private RequestCall<Array<ItemReference>> getItemReferenceRequestCall(@Nonnull final String path) {
        return new RequestCall<Array<ItemReference>>() {
            @Override
            public void makeCall(AsyncCallback<Array<ItemReference>> callback) {
                projectService.getChildren(path, _createCallback(callback, dtoUnmarshaller.newArrayUnmarshaller(ItemReference.class)));
            }
        };
    }

//    @Nonnull
//    private Function<Array<ItemReference>, List<Node>> _createItemReferenceNodes(@Nonnull final ProjectDescriptor relProjectDescriptor,
//                                                                                 @Nonnull final NodeSettings nodeSettings) {
//        return new Function<Array<ItemReference>, List<Node>>() {
//            @Override
//            public List<Node> apply(Array<ItemReference> itemRefList) throws FunctionException {
//                if (itemRefList == null || itemRefList.isEmpty()) {
//                    return Collections.emptyList();
//                }
//
//                List<Node> nodes = new ArrayList<>(itemRefList.size());
//
//                for (ItemReference itemReference : itemRefList.asIterable()) {
//                    //Skip files which starts with "." if enabled
//                    if (!nodeSettings.isShowHiddenFiles() && itemReference.getName().startsWith(".")) {
//                        continue;
//                    }
//
//                    if ("file".equals(itemReference.getType())) {
//                        nodes.add(nodeFactory.newFileReferenceNode(itemReference, relProjectDescriptor, nodeSettings));
//                    } else if ("folder".equals(itemReference.getType())) {
//                        nodes.add(nodeFactory.newFolderReferenceNode(itemReference, relProjectDescriptor, nodeSettings));
//                    } else if ("project".equals(itemReference.getType())) {
//
//                    }
//
//                    //NOTE if we want support more type nodes than we should refactor mechanism of hardcoded types for item references
//                }
//
//                return nodes;
//            }
//        };
//    }

    @Nonnull
    private Function<Array<ItemReference>, Promise<List<Node>>> _createItemReferenceNodes(@Nonnull final ProjectDescriptor relProjectDescriptor,
                                                                                 @Nonnull final NodeSettings nodeSettings) {
        return new Function<Array<ItemReference>, Promise<List<Node>>>() {
            @Override
            public Promise<List<Node>> apply(Array<ItemReference> itemRefList) throws FunctionException {
                if (itemRefList == null || itemRefList.isEmpty()) {
                    return Promises.resolve(Collections.<Node>emptyList());
                }

                final List<Node> nodes = new ArrayList<>(itemRefList.size());

                List<ItemReference> modules = null;

                for (ItemReference itemReference : itemRefList.asIterable()) {
                    //Skip files which starts with "." if enabled
                    if (!nodeSettings.isShowHiddenFiles() && itemReference.getName().startsWith(".")) {
                        continue;
                    }

                    if ("file".equals(itemReference.getType())) {
                        nodes.add(nodeFactory.newFileReferenceNode(itemReference, relProjectDescriptor, nodeSettings));
                    } else if ("folder".equals(itemReference.getType())) {
                        nodes.add(nodeFactory.newFolderReferenceNode(itemReference, relProjectDescriptor, nodeSettings));
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
                        Log.info(this.getClass(), "apply():315: " + "");
                        nodes.addAll(collector);
                        return nodes;
                    }
                });
            }
        };
    }

    private Promise<Node> getModule(ItemReference module, List<Node> collector, NodeSettings nodeSettings) {
        return AsyncPromiseHelper.createFromAsyncRequest(createGetModuleRequest(module))
                                 .then(createModuleNode(nodeSettings))
                                 .then(_collectNodesFromPromises(collector));
    }

    private RequestCall<ProjectDescriptor> createGetModuleRequest(final ItemReference module) {
        return new RequestCall<ProjectDescriptor>() {
            @Override
            public void makeCall(AsyncCallback<ProjectDescriptor> callback) {
                projectService.getProject(module.getPath(), _createCallback(callback, dtoUnmarshaller.newUnmarshaller(ProjectDescriptor.class)));
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

    @Nonnull
    public Promise<String> getContent(@Nonnull final VirtualFile virtualFile) {
//        return AsyncPromiseHelper.createFromAsyncRequest(getContentRequestCall(virtualFile));
        return AsyncPromiseHelper.createFromAsyncRequest(new RequestCall<String>() {
            @Override
            public void makeCall(final AsyncCallback<String> callback) {
                Unmarshallable<String> u = dtoUnmarshaller.newUnmarshaller(String.class);
                projectService.getFileContent(virtualFile.getPath(), new AsyncRequestCallback<String>(u) {
                    @Override
                    protected void onSuccess(String result) {
                        Log.info(this.getClass(), "getContent s: ", result);
                        callback.onSuccess(result);
                    }

                    @Override
                    protected void onFailure(Throwable exception) {
                        Log.info(this.getClass(), "getContent f: ", exception);
                        callback.onFailure(exception);
                    }
                });
            }
        });
    }

    @Nonnull
    private RequestCall<String> getContentRequestCall(@Nonnull final VirtualFile vFile) {
        return new RequestCall<String>() {
            @Override
            public void makeCall(AsyncCallback<String> callback) {
                projectService.getFileContent(vFile.getPath(), _createCallback(callback, dtoUnmarshaller.newUnmarshaller(String.class)));
            }
        };
    }

    @Nonnull
    public Promise<Void> updateContent(@Nonnull VirtualFile virtualFile, @Nonnull String content) {
        return AsyncPromiseHelper.createFromAsyncRequest(updateContentRequestCall(virtualFile, content));
    }

    @Nonnull
    private RequestCall<Void> updateContentRequestCall(@Nonnull final VirtualFile vFile, @Nonnull final String content) {
        return new RequestCall<Void>() {
            @Override
            public void makeCall(AsyncCallback<Void> callback) {
                projectService.updateFile(vFile.getPath(),
                                          content,
                                          vFile.getMediaType(),
                                          _createCallback(callback, dtoUnmarshaller.newUnmarshaller(Void.class)));
            }
        };
    }

    @Nonnull
    public NodesResources getNodesResources() {
        return nodesResources;
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
}
