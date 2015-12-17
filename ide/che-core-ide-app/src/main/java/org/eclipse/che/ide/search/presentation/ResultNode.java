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

import elemental.html.SpanElement;

import com.google.gwt.dom.client.Element;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.js.Promises;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.api.project.node.AbstractTreeNode;
import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.project.node.ItemReferenceBasedNode;
import org.eclipse.che.ide.project.node.NodeManager;
import org.eclipse.che.ide.ui.smartTree.TreeStyles;
import org.eclipse.che.ide.ui.smartTree.presentation.HasPresentation;
import org.eclipse.che.ide.ui.smartTree.presentation.NodePresentation;
import org.eclipse.che.ide.util.Pair;
import org.eclipse.che.ide.util.dom.Elements;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

import static org.eclipse.che.ide.api.theme.Style.getEditorInfoTextColor;

/**
 * Tree node represent search result.
 *
 * @author Valeriy Svydenko
 */
public class ResultNode extends AbstractTreeNode implements HasPresentation {

    private final CoreLocalizationConstant locale;
    private final NodeManager              nodeManager;

    private NodePresentation    nodePresentation;
    private TreeStyles          styles;
    private List<ItemReference> findResults;
    private String              request;

    @Inject
    public ResultNode(TreeStyles styles,
                      CoreLocalizationConstant locale,
                      NodeManager nodeManager,
                      @Assisted List<ItemReference> findResult,
                      @Assisted String request) {
        this.styles = styles;
        this.locale = locale;
        this.nodeManager = nodeManager;
        this.findResults = findResult;
        this.request = request;
    }

    @Override
    protected Promise<List<Node>> getChildrenImpl() {
        List<Node> fileNodes = new ArrayList<>();
        for (ItemReference itemReference : findResults) {
            ItemReferenceBasedNode node = nodeManager.wrap(itemReference, null);

            NodePresentation presentation = node.getPresentation(true);
            presentation.setInfoText(itemReference.getPath());
            presentation.setInfoTextWrapper(Pair.of("(", ")"));
            presentation.setInfoTextCss("color:" + getEditorInfoTextColor() + ";font-size: 11px");

            fileNodes.add(node);
        }
        return Promises.resolve(fileNodes);
    }

    @Override
    public NodePresentation getPresentation(boolean update) {
        if (nodePresentation == null) {
            nodePresentation = new NodePresentation();
            updatePresentation(nodePresentation);
        }

        if (update) {
            updatePresentation(nodePresentation);
        }
        return nodePresentation;
    }

    @Override
    public String getName() {
        return locale.actionFullTextSearch();
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public void updatePresentation(@NotNull NodePresentation presentation) {
        SpanElement spanElement = Elements.createSpanElement(styles.styles().presentableTextContainer());
        StringBuilder resultTitle = new StringBuilder("Find Occurrences of '" + request + "\'  (" + findResults.size() + " occurrence");
        if (findResults.size() > 1) {
            resultTitle.append("s)");
        } else {
            resultTitle.append(")");
        }
        spanElement.setInnerHTML(resultTitle.toString());
        presentation.setUserElement((Element)spanElement);
    }

}
