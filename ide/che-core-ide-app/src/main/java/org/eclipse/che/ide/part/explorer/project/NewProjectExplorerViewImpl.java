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
package org.eclipse.che.ide.part.explorer.project;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.Resources;
import org.eclipse.che.ide.api.parts.base.BaseView;
import org.eclipse.che.ide.api.parts.base.ToolButton;
import org.eclipse.che.ide.api.project.node.HasDataObject;
import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.api.project.node.interceptor.NodeInterceptor;
import org.eclipse.che.ide.menu.ContextMenu;
import org.eclipse.che.ide.project.ProjectContextMenu;
import org.eclipse.che.ide.project.node.AbstractProjectBasedNode;
import org.eclipse.che.ide.project.node.ProjectDescriptorNode;
import org.eclipse.che.ide.ui.Tooltip;
import org.eclipse.che.ide.ui.smartTree.NodeDescriptor;
import org.eclipse.che.ide.ui.smartTree.NodeNameConverter;
import org.eclipse.che.ide.ui.smartTree.NodeUniqueKeyProvider;
import org.eclipse.che.ide.ui.smartTree.SortDir;
import org.eclipse.che.ide.ui.smartTree.SpeedSearch;
import org.eclipse.che.ide.ui.smartTree.Tree;
import org.eclipse.che.ide.ui.smartTree.TreeNodeLoader;
import org.eclipse.che.ide.ui.smartTree.TreeNodeStorage;
import org.eclipse.che.ide.ui.smartTree.TreeNodeStorage.StoreSortInfo;
import org.eclipse.che.ide.ui.smartTree.event.GoIntoStateEvent;
import org.eclipse.che.ide.ui.smartTree.event.GoIntoStateEvent.GoIntoStateHandler;
import org.eclipse.che.ide.ui.smartTree.event.SelectionChangedEvent;
import org.eclipse.che.ide.ui.smartTree.event.SelectionChangedEvent.SelectionChangedHandler;
import org.eclipse.che.ide.ui.smartTree.sorting.AlphabeticalFilter;
import org.eclipse.che.ide.ui.smartTree.sorting.FoldersOnTopFilter;
import org.vectomatic.dom.svg.ui.SVGImage;
import org.vectomatic.dom.svg.ui.SVGResource;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.eclipse.che.ide.ui.menu.PositionController.HorizontalAlign.MIDDLE;
import static org.eclipse.che.ide.ui.menu.PositionController.VerticalAlign.BOTTOM;
import static org.eclipse.che.ide.ui.smartTree.event.GoIntoStateEvent.State.ACTIVATED;
import static org.eclipse.che.ide.ui.smartTree.event.GoIntoStateEvent.State.DEACTIVATED;

/**
 * @author Vlad Zhukovskiy
 */
@Singleton
public class NewProjectExplorerViewImpl extends BaseView<NewProjectExplorerView.ActionDelegate> implements NewProjectExplorerView,
                                                                                                           GoIntoStateHandler {
    private       Resources                resources;
    private final ProjectExplorerResources explorerResources;
    private final ProjectContextMenu       projectContextMenu;
    private       Tree                     tree;
    private       FlowPanel                projectHeader;
    private       ToolButton               goIntoBackButton;

    @Inject
    public NewProjectExplorerViewImpl(Resources resources,
                                      ProjectExplorerResources ExplorerResources,
                                      final ContextMenu contextMenu,
                                      final ProjectContextMenu projectContextMenu,
                                      CoreLocalizationConstant coreLocalizationConstant) {
        super(resources);
        this.resources = resources;
        explorerResources = ExplorerResources;
        this.projectContextMenu = projectContextMenu;

        setTitle(coreLocalizationConstant.projectExplorerTitleBarText());

        projectHeader = new FlowPanel();
        projectHeader.setStyleName(resources.partStackCss().idePartStackToolbarBottom());

        TreeNodeStorage nodeStorage = new TreeNodeStorage(new NodeUniqueKeyProvider() {
            @Nonnull
            @Override
            public String getKey(@Nonnull Node item) {
                if (item instanceof AbstractProjectBasedNode) {
                    return ((AbstractProjectBasedNode)item).getData() + "";
                }
                return item.getName();
            }
        });

        //TODO make it configurable
        TreeNodeLoader nodeLoader = new TreeNodeLoader(new HashSet<NodeInterceptor>());

        tree = new Tree(nodeStorage, nodeLoader);
        tree.setContextMenuInvocationHandler(new Tree.ContextMenuInvocationHandler() {
            @Override
            public void invokeContextMenuOn(int x, int y) {
                contextMenu.show(x, y);
            }
        });
        tree.getNodeStorage().add(Collections.<Node>emptyList());

        StoreSortInfo foldersOnTop = new StoreSortInfo(new FoldersOnTopFilter(), SortDir.ASC);
        StoreSortInfo alphabetical = new StoreSortInfo(new AlphabeticalFilter(), SortDir.ASC);
        tree.getNodeStorage().addSortInfo(foldersOnTop);
        tree.getNodeStorage().addSortInfo(alphabetical);

        tree.getSelectionModel().addSelectionChangedHandler(new SelectionChangedHandler() {
            @Override
            public void onSelectionChanged(SelectionChangedEvent event) {
                delegate.onSelectionChanged(event.getSelection());
            }
        });

        tree.getGoIntoMode().addGoIntoHandler(this);

        new SpeedSearch(tree, new NodeNameConverter());

        setContentWidget(new ScrollPanel(tree));
    }

    @Override
    public void setRootNodes(List<Node> nodes) {
        tree.getNodeStorage().replaceChildren(null, nodes);

        if (nodes.size() == 1) {
            tree.setExpanded(nodes.get(0), true);
        }

        showProjectInfo();
    }

    @Override
    public List<StoreSortInfo> getSortInfo() {
        return tree.getNodeStorage().getSortInfo();
    }

    @Override
    public void onApplySort() {
        tree.getExpandStateHandler().saveState();
        tree.getNodeStorage().applySort(false);
        tree.getExpandStateHandler().loadState();
    }

    @Override
    public void scrollFromSource(Object object) {
        for (NodeDescriptor nodeDescriptor : tree.getNodeStorage().getStoredNodes()) {
            Node storedNode = nodeDescriptor.getNode();
            if (storedNode.equals(object) ||
                (storedNode instanceof HasDataObject<?> && ((HasDataObject)storedNode).getData().equals(object))) {
                tree.setExpanded(storedNode, true);
                tree.getSelectionModel().select(storedNode, false);
                return;
            }
        }
    }

    public void clear() {
        tree.clear();
    }

    @Override
    public void setRootNode(Node node) {
        setRootNodes(Collections.singletonList(node));
    }

    @Override
    public void onChildrenRemoved(Node node) {
        tree.getNodeStorage().remove(node);
    }

    @Override
    public void onChildrenCreated(Node parent, final Node child) {
        //it may happened
        if (child.getParent() == null) {
            child.setParent(parent);
        }
        tree.getNodeStorage().add(parent, child);

        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                tree.getSelectionModel().select(child, false);
                tree.scrollIntoView(child);
            }
        });
    }


    private void showProjectInfo() {
        if (toolBar.getWidgetIndex(projectHeader) < 0) {
            toolBar.addSouth(projectHeader, 28);
            setToolbarHeight(50);
        }
        projectHeader.clear();

        List<Node> rootItems = tree.getNodeStorage().getRootItems();

        for (Node rootNode : rootItems) {
            if (rootNode instanceof ProjectDescriptorNode) {
                ProjectDescriptor descriptor = ((ProjectDescriptorNode)rootNode).getProjectDescriptor();

                if (descriptor == null) {
                    continue;
                }

                FlowPanel delimiter = new FlowPanel();
                delimiter.setStyleName(resources.partStackCss().idePartStackToolbarSeparator());
                projectHeader.add(delimiter);

                SVGImage projectVisibilityImage = new SVGImage("private".equals(descriptor.getVisibility()) ? resources.privateProject()
                                                                                                            : resources.publicProject());
                projectVisibilityImage.getElement().setAttribute("class", resources.partStackCss().idePartStackToolbarBottomIcon());

                projectHeader.add(projectVisibilityImage);

                InlineLabel projectTitle = new InlineLabel(descriptor.getName());
                projectHeader.add(projectTitle);

                SVGImage gearSettingsIcon = new SVGImage(explorerResources.gear());

                ToolButton settings = new ToolButton(gearSettingsIcon);
                settings.addClickHandler(new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        projectContextMenu.show(event.getClientX(), event.getClientY());
                    }
                });
                Tooltip.create((elemental.dom.Element)settings.getElement(),
                               BOTTOM,
                               MIDDLE,
                               "Show Settings");
                addMenuButton(settings);

                SVGImage scrollFromSourceIcon = new SVGImage(explorerResources.source());
                ToolButton scrollFromSource = new ToolButton(scrollFromSourceIcon);
                scrollFromSource.addClickHandler(new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        throw new UnsupportedOperationException("Not implemented yet.");
                    }
                });
                Tooltip.create((elemental.dom.Element)scrollFromSource.getElement(),
                               BOTTOM,
                               MIDDLE,
                               "Scroll from Source");
                addMenuButton(scrollFromSource);

                SVGImage collapseAllIcon = new SVGImage(explorerResources.collapse());
                ToolButton collapseAll = new ToolButton(collapseAllIcon);
                collapseAll.addClickHandler(new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        if (tree.getGoIntoMode().isActivated()) {
                            Node lastNode = tree.getGoIntoMode().getLastNode();
                            tree.setExpanded(lastNode, false, true);
                            return;
                        }

                        tree.collapseAll();
                    }
                });
                Tooltip.create((elemental.dom.Element)collapseAll.getElement(),
                               BOTTOM,
                               MIDDLE,
                               "Collapse All");
                addMenuButton(collapseAll);

                FlowPanel refreshButton = new FlowPanel();
                refreshButton.add(new SVGImage(resources.refresh()));
                refreshButton.setStyleName(resources.partStackCss().idePartStackToolbarBottomButton());
                refreshButton.addStyleName(resources.partStackCss().idePartStackToolbarBottomButtonRight());
                projectHeader.add(refreshButton);

                refreshButton.addDomHandler(new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        tree.synchronize();
                    }
                }, ClickEvent.getType());

                return;
            }
        }

        hideProjectInfo();
    }

    private void hideProjectInfo() {
        goIntoBackButton = null;
        menuPanel.clear();
        toolBar.remove(projectHeader);
        setToolbarHeight(22);
    }

    @Override
    public boolean goInto(Node node) {
        return tree.getGoIntoMode().goInto(node);
    }

    @Override
    public void resetGoIntoMode() {
        tree.getGoIntoMode().reset();
    }

    @Override
    public void onGoIntoStateChanged(GoIntoStateEvent event) {
        if (event.getState() == ACTIVATED) {
            //lazy button initializing
            if (goIntoBackButton == null) {
                initGoIntoBackButton();
                return;
            }

            goIntoBackButton.setVisible(true);

        } else if (event.getState() == DEACTIVATED) {
            goIntoBackButton.setVisible(false);
        }
    }

    private void initGoIntoBackButton() {
        SVGImage upwardArrowIcon = new SVGImage(explorerResources.up());
        goIntoBackButton = new ToolButton(upwardArrowIcon);
        goIntoBackButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                tree.getGoIntoMode().reset();
            }
        });
        Tooltip.create((elemental.dom.Element)goIntoBackButton.getElement(),
                       BOTTOM,
                       MIDDLE,
                       "Go Back");
        addMenuButton(goIntoBackButton);
    }

    public interface ProjectExplorerResources extends ClientBundle {
        @Source("gear.svg")
        SVGResource gear();

        @Source("source.svg")
        SVGResource source();

        @Source("upwardArrow.svg")
        SVGResource up();

        @Source("collapse.svg")
        SVGResource collapse();
    }
}
