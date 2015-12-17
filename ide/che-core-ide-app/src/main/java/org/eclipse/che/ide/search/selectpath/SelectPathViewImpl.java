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
package org.eclipse.che.ide.search.selectpath;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.api.project.node.HasStorablePath;
import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.api.project.node.interceptor.NodeInterceptor;
import org.eclipse.che.ide.ui.smartTree.NodeUniqueKeyProvider;
import org.eclipse.che.ide.ui.smartTree.Tree;
import org.eclipse.che.ide.ui.smartTree.TreeNodeLoader;
import org.eclipse.che.ide.ui.smartTree.TreeNodeStorage;
import org.eclipse.che.ide.ui.smartTree.UniqueKeyProvider;
import org.eclipse.che.ide.ui.window.Window;

import javax.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Implementation of {@link SelectPathView}.
 *
 * @author Valeriy Svydenko
 */
@Singleton
public class SelectPathViewImpl extends Window implements SelectPathView {
    private Tree           tree;
    private ActionDelegate delegate;

    @UiField
    ScrollPanel treeContainer;

    interface SelectPathViewImplUiBinder extends UiBinder<Widget, SelectPathViewImpl> {
    }

    @Inject
    public SelectPathViewImpl(CoreLocalizationConstant locale, SelectPathViewImplUiBinder uiBinder) {
        setTitle(locale.selectPathWindowTitle());

        Widget widget = uiBinder.createAndBindUi(this);
        setWidget(widget);

        Button cancel = createButton(locale.cancel(), "select-path-cancel-button", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                hide();
            }
        });

        Button ok = createPrimaryButton(locale.ok(), "select-path-ok-button", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                acceptButtonClicked();
            }
        });

        getFooter().add(ok);
        getFooter().add(cancel);
    }

    @Override
    protected void onEnterClicked() {
        acceptButtonClicked();
    }

    @Override
    public void setDelegate(ActionDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public void showStructure(List<Node> nodes) {
        UniqueKeyProvider<Node> uniqueKeyProvider = new NodeUniqueKeyProvider() {
            @NotNull
            @Override
            public String getKey(@NotNull Node item) {
                if (item instanceof HasStorablePath) {
                    return ((HasStorablePath)item).getStorablePath();
                } else {
                    return String.valueOf(item.hashCode());
                }
            }
        };
        FolderNodeInterceptor interceptor = new FolderNodeInterceptor();
        Set<NodeInterceptor> interceptors = new HashSet<>();
        interceptors.add(interceptor);
        TreeNodeLoader loader = new TreeNodeLoader(interceptors);
        TreeNodeStorage treeNodeStorage = new TreeNodeStorage(uniqueKeyProvider);

        tree = new Tree(treeNodeStorage, loader);
        treeContainer.setWidget(tree);

        treeNodeStorage.add(nodes);
        show();

        if (!nodes.isEmpty()) {
            tree.getSelectionModel().select(nodes.get(0), false);
        }
    }

    private void acceptButtonClicked() {
        List<Node> nodes = tree.getSelectionModel().getSelectedNodes();
        if (nodes.isEmpty()) {
            return;
        }
        Node selectedNode = nodes.get(0);
        delegate.setSelectedPath(((HasStorablePath)selectedNode).getStorablePath());

        hide();
    }
}