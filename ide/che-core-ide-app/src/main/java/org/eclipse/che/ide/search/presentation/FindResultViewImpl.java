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
package org.eclipse.che.ide.search.presentation;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.api.parts.PartStackUIResources;
import org.eclipse.che.ide.api.parts.base.BaseView;
import org.eclipse.che.ide.api.project.node.HasStorablePath;
import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.api.project.node.interceptor.NodeInterceptor;
import org.eclipse.che.ide.search.factory.FindResultNodeFactory;
import org.eclipse.che.ide.ui.smartTree.NodeUniqueKeyProvider;
import org.eclipse.che.ide.ui.smartTree.Tree;
import org.eclipse.che.ide.ui.smartTree.TreeNodeLoader;
import org.eclipse.che.ide.ui.smartTree.TreeNodeStorage;
import org.eclipse.che.ide.ui.smartTree.UniqueKeyProvider;

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;

/**
 * Implementation for FindResult view.
 * Uses tree for presenting search results.
 *
 * @author Valeriy Svydenko
 */
@Singleton
class FindResultViewImpl extends BaseView<FindResultView.ActionDelegate> implements FindResultView {
    private final Tree                  tree;
    private final FindResultNodeFactory findResultNodeFactory;

    private ScrollPanel container;

    @Inject
    public FindResultViewImpl(PartStackUIResources resources,
                              FindResultNodeFactory findResultNodeFactory,
                              CoreLocalizationConstant localizationConstant) {
        super(resources);
        setTitle(localizationConstant.actionFullTextSearch());
        this.findResultNodeFactory = findResultNodeFactory;

        UniqueKeyProvider<Node> nodeIdProvider = new NodeUniqueKeyProvider() {
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

        TreeNodeStorage nodeStorage = new TreeNodeStorage(nodeIdProvider);
        TreeNodeLoader loader = new TreeNodeLoader(Collections.<NodeInterceptor>emptySet());
        tree = new Tree(nodeStorage, loader);

        container = new ScrollPanel(tree);
        setContentWidget(container);
        tree.getElement().setTabIndex(0);
        tree.getElement().getStyle().setHeight(100, Style.Unit.PCT);
        tree.getElement().getParentElement().getStyle().setHeight(100, Style.Unit.PCT);
        tree.setAutoSelect(true);
    }

    @Override
    public void showResults(List<ItemReference> nodes, String request) {
        tree.getNodeStorage().clear();
        ResultNode rootNode = findResultNodeFactory.newResultNode(nodes, request);
        tree.getNodeStorage().add(rootNode);
        tree.expandAll();
        tree.getSelectionModel().select(rootNode, false);
    }
}
