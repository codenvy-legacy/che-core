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
package org.eclipse.che.ide.ui.smartTree;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.impl.FocusImpl;

import org.eclipse.che.ide.api.project.node.HasAction;
import org.eclipse.che.ide.api.project.node.MutableNode;
import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.ui.smartTree.event.BeforeCollapseNodeEvent;
import org.eclipse.che.ide.ui.smartTree.event.BeforeCollapseNodeEvent.HasBeforeCollapseItemHandlers;
import org.eclipse.che.ide.ui.smartTree.event.BeforeExpandNodeEvent;
import org.eclipse.che.ide.ui.smartTree.event.BeforeExpandNodeEvent.HasBeforeExpandNodeHandlers;
import org.eclipse.che.ide.ui.smartTree.event.BlurEvent;
import org.eclipse.che.ide.ui.smartTree.event.BlurEvent.HasBlurHandlers;
import org.eclipse.che.ide.ui.smartTree.event.CancellableEvent;
import org.eclipse.che.ide.ui.smartTree.event.CollapseNodeEvent;
import org.eclipse.che.ide.ui.smartTree.event.CollapseNodeEvent.HasCollapseItemHandlers;
import org.eclipse.che.ide.ui.smartTree.event.ExpandNodeEvent;
import org.eclipse.che.ide.ui.smartTree.event.ExpandNodeEvent.HasExpandItemHandlers;
import org.eclipse.che.ide.ui.smartTree.event.FocusEvent;
import org.eclipse.che.ide.ui.smartTree.event.StoreAddEvent;
import org.eclipse.che.ide.ui.smartTree.event.StoreAddEvent.StoreAddHandler;
import org.eclipse.che.ide.ui.smartTree.event.StoreClearEvent;
import org.eclipse.che.ide.ui.smartTree.event.StoreClearEvent.StoreClearHandler;
import org.eclipse.che.ide.ui.smartTree.event.StoreDataChangeEvent;
import org.eclipse.che.ide.ui.smartTree.event.StoreDataChangeEvent.StoreDataChangeHandler;
import org.eclipse.che.ide.ui.smartTree.event.StoreRemoveEvent;
import org.eclipse.che.ide.ui.smartTree.event.StoreRemoveEvent.StoreRemoveHandler;
import org.eclipse.che.ide.ui.smartTree.event.StoreSortEvent;
import org.eclipse.che.ide.ui.smartTree.event.StoreSortEvent.StoreSortHandler;
import org.eclipse.che.ide.ui.smartTree.event.StoreUpdateEvent;
import org.eclipse.che.ide.ui.smartTree.event.StoreUpdateEvent.StoreUpdateHandler;
import org.eclipse.che.ide.ui.smartTree.event.internal.NativeTreeEvent;
import org.eclipse.che.ide.ui.smartTree.presentation.DefaultPresentationRenderer;
import org.eclipse.che.ide.ui.smartTree.presentation.HasPresentation;
import org.eclipse.che.ide.ui.smartTree.presentation.PresentationRenderer;
import org.eclipse.che.ide.ui.smartTree.state.ExpandStateHandler;
import org.eclipse.che.ide.ui.status.ComponentWithEmptyText;
import org.eclipse.che.ide.ui.status.StatusText;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Vlad Zhukovskiy
 */
public class Tree extends Widget implements HasBeforeExpandNodeHandlers, HasExpandItemHandlers, HasBeforeCollapseItemHandlers,
                                            HasCollapseItemHandlers, ComponentWithEmptyText, HasBlurHandlers {


    private Map<String, NodeDescriptor> nodesByDom = new HashMap<>();

    protected TreeNodeStorage nodeStorage;

    protected TreeNodeLoader nodeLoader;

    private boolean     autoExpand;
    private boolean     autoLoad;
    private DelayedTask updateTask, cleanTask;
    private boolean trackMouseOver = true;

    protected final FocusImpl focusImpl = FocusImpl.getFocusImplForPanel();
    protected Element focusEl;
    private GroupingHandlerRegistration storeHandlers = new GroupingHandlerRegistration();
    private boolean                     autoSelect    = true;
    protected TreeSelectionModel selectionModel;

    protected boolean focusConstrainScheduled  = false;
    protected boolean disableNativeContextMenu = false;

    protected Element    rootContainer;
    private   TreeStyles treeStyles;
    private boolean allowTextSelection = true;

    protected TreeView view = new TreeView();
    private ContextMenuInvocationHandler contextMenuInvocationHandler;

    private StatusText emptyText;

    private PresentationRenderer<Node> nodePresentationRenderer;

    private ExpandStateHandler expandStateHandler;

    private GoIntoMode goIntoMode;

    public enum Joint {
        COLLAPSED(1), EXPANDED(2), NONE(0);

        private int value;

        private Joint(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }


    public static interface ContextMenuInvocationHandler {
        void invokeContextMenuOn(int x, int y);
    }

    private class Handler implements StoreAddHandler, StoreClearHandler, StoreDataChangeHandler, StoreRemoveHandler, StoreUpdateHandler,
                                     StoreSortHandler {

        @Override
        public void onAdd(StoreAddEvent event) {
            Tree.this.onAdd(event);
        }

        @Override
        public void onClear(StoreClearEvent event) {
            Tree.this.onClear(event);
        }

        @Override
        public void onDataChange(StoreDataChangeEvent event) {
            Tree.this.onDataChanged(event);
        }

        @Override
        public void onRemove(StoreRemoveEvent event) {
            Tree.this.onRemove(event);
        }

        @Override
        public void onSort(StoreSortEvent event) {
            Tree.this.onSort(event);
        }

        @Override
        public void onUpdate(StoreUpdateEvent event) {
            Tree.this.onUpdate(event);
        }

    }

    @Override
    public HandlerRegistration addBeforeCollapseHandler(BeforeCollapseNodeEvent.BeforeCollapseNodeHandler handler) {
        return addHandler(handler, BeforeCollapseNodeEvent.getType());
    }

    @Override
    public HandlerRegistration addBeforeExpandHandler(BeforeExpandNodeEvent.BeforeExpandNodeHandler handler) {
        return addHandler(handler, BeforeExpandNodeEvent.getType());
    }

    @Override
    public HandlerRegistration addCollapseHandler(CollapseNodeEvent.CollapseNodeHandler handler) {
        return addHandler(handler, CollapseNodeEvent.getType());
    }

    @Override
    public HandlerRegistration addExpandHandler(ExpandNodeEvent.ExpandNodeHandler handler) {
        return addHandler(handler, ExpandNodeEvent.getType());
    }

    @Override
    public HandlerRegistration addBlurHandler(BlurEvent.BlurHandler handler) {
        return addHandler(handler, BlurEvent.getType());
    }

    @UiConstructor
    public Tree(TreeNodeStorage nodeStorage, TreeNodeLoader nodeLoader) {
        this(nodeStorage, nodeLoader, (TreeStyles)GWT.create(TreeStyles.class));
    }

    public Tree(TreeNodeStorage nodeStorage, TreeNodeLoader nodeLoader, TreeStyles treeStyles) {

        this.treeStyles = treeStyles;
        this.treeStyles.styles().ensureInjected();


        DivElement element = Document.get().createDivElement();
        element.addClassName(treeStyles.styles().tree());
        setElement(element);
        ensureFocusElement();

        setNodeStorage(nodeStorage);
        setSelectionModel(new TreeSelectionModel());
        setNodeLoader(nodeLoader);
        disableBrowserContextMenu(true);


        setGoIntoMode(new GoIntoMode());


//        this.expandStateHandler = new ExpandStateHandler(this, new HtmlStorageProvider());


        view.bind(this);

        this.emptyText = new StatusText(this) {
            @Override
            protected boolean isStatusVisible() {
                return Tree.this.getNodeStorage().getRootCount() == 0;
            }
        };
    }

    public void setView(TreeView view) {
        this.view = view;
        view.bind(this);
    }

    public boolean isExpanded(Node node) {
        NodeDescriptor nodeDescriptor = findNode(node);
        return nodeDescriptor != null && nodeDescriptor.isExpanded(); //TODO use expanded state of nodes
    }

    public boolean isLeaf(Node node) {
        return !hasChildren(node);
    }

    protected boolean hasChildren(Node node) {
        NodeDescriptor nodeDescriptor = findNode(node);
        if (nodeLoader != null && !nodeDescriptor.isLoaded()) {
            return nodeLoader.mayHaveChildren(nodeDescriptor.getNode());
        }
        if (!nodeDescriptor.isLeaf() || nodeStorage.hasChildren(nodeDescriptor.getNode())) {
            return true;
        }
        return false;
    }

    public NodeDescriptor findNode(Node node) {
        if (nodeStorage == null || node == null) return null;
        return nodeStorage.getNodeMap().get(getUniqueId(node));
    }


    @Nullable
    public NodeDescriptor findNode(@Nonnull Element target) {
        Element nodeElement = getNearestParentElement(target, treeStyles.styles().rootContainer());
        if (nodeElement != null) {
            return nodesByDom.get(nodeElement.getId());
        }
        return null;
    }

    private native Element getNearestParentElement(Element element, String className) /*-{
        function findAncestor(el, cls) {
            while ((el = el.parentElement) && !el.classList.contains(cls));
            return el;
        }

        return findAncestor(element, className);
    }-*/;

    @Nullable
    public ExpandStateHandler getExpandStateHandler() {
        return expandStateHandler;
    }

    public void setExpandStateHandler(@Nonnull ExpandStateHandler expandStateHandler) {
        this.expandStateHandler = expandStateHandler;
    }

    protected String getUniqueId(Node node) {
        return nodeStorage.getKeyProvider().getKey(node);
    }

    public void setExpanded(Node node, boolean expand) {
        setExpanded(node, expand, false);
    }

    public void setExpanded(Node node, boolean expand, boolean deep) {
        if (expand) {
            // make item visible by expanding parents
            List<Node> list = new ArrayList<>();
            Node p = node;
            while ((p = nodeStorage.getParent(p)) != null) {
                NodeDescriptor nodeDescriptor = findNode(p);
                if (nodeDescriptor == null || !nodeDescriptor.isExpanded()) {
                    list.add(p);
                }
            }
            for (int i = list.size() - 1; i >= 0; i--) {
                Node item = list.get(i);
                setExpanded(item, true, false);
            }
        }


        NodeDescriptor nodeDescriptor = findNode(node);
        if (nodeDescriptor == null) {
            return;
        }

        if (!isAttached()) {
            nodeDescriptor.setExpand(expand); //TODO write expanded node to expanded state storage
            return;
        }

        if (expand) {
            onExpand(node, nodeDescriptor, deep);
        } else {
            onCollapse(node, nodeDescriptor, deep);
        }
    }

    public void setLeaf(Node node, boolean leaf) {
        if (node instanceof MutableNode) {
            NodeDescriptor nodeDescriptor = findNode(node);
            if (nodeDescriptor != null) {
                nodeDescriptor.setLeaf(leaf);
                refresh(node);
            }
        }
    }

    public void setNodeLoader(TreeNodeLoader nodeLoader) {
        if (this.nodeLoader != null) {
            this.nodeLoader.bindTree(null);
        }

        this.nodeLoader = nodeLoader;
        if (nodeLoader != null) {
            nodeLoader.bindTree(this);
        }
    }

    protected void onExpand(Node node, NodeDescriptor nodeDescriptor, boolean deep) {
        if (!isLeaf(nodeDescriptor.getNode())) {
            if (nodeDescriptor.isLoading()) {
                return;
            }

            if (!fireCancellableEvent(new BeforeExpandNodeEvent(node))) {
                return;
            }

            if (!nodeDescriptor.isExpanded() && nodeLoader != null && (!nodeDescriptor.isLoaded())) {
                nodeStorage.removeChildren(node);
                nodeDescriptor.setExpand(true);
                nodeDescriptor.setExpandDeep(deep);
                nodeDescriptor.setLoading(true);
                view.onLoadChange(nodeDescriptor, true);
                nodeLoader.loadChildren(node);
                return;
            }

            if (!nodeDescriptor.isExpanded()) {
                nodeDescriptor.setExpanded(true);

                if (!nodeDescriptor.isChildrenRendered()) {
                    renderChildren(node);
                    nodeDescriptor.setChildrenRendered(true);
                }

                // expand
                view.expand(nodeDescriptor); //TODO write expanded node to expanded state storage

                update();
                fireEvent(new ExpandNodeEvent(node));
            }

            if (deep) {
                setExpandChildren(node, true);
            }
        }

    }

    private void setExpandChildren(Node node, boolean expand) {
        for (Node child : nodeStorage.getChildren(node)) {
            setExpanded(child, expand, true);
        }
    }

    protected void renderChildren(Node parent) {
        renderChildren(parent, false);
    }

    protected void renderChildren(Node parent, boolean resetCache) {
        int depth = nodeStorage.getDepth(parent);
        List<Node> children = parent == null ? nodeStorage.getRootItems() : nodeStorage.getChildren(parent);
        if (children.size() == 0) {
            emptyText.paint();
            return;
        }

        Element container = getContainer(parent);

        for (Node child : children) {
            Element element = renderChild(child, depth);
            container.appendChild(element);
        }


        for (Node child : children) {
            NodeDescriptor nodeDescriptor = findNode(child);
            if (resetCache) {
                if (nodeLoader.mayHaveChildren(child)) {
                    nodeLoader.loadChildren(child);
                }
            } else if (autoExpand) {
                setExpanded(child, true);
            } else if (nodeDescriptor.isExpand() && !isLeaf(nodeDescriptor.getNode())) {
                nodeDescriptor.setExpand(false);
                setExpanded(child, true);
            } else if (nodeLoader != null) {
                if (autoLoad) {
                    if (nodeLoader.mayHaveChildren(child)) {
                        nodeLoader.loadChildren(child);
                    }
                }
            } else if (autoLoad) {
                renderChildren(child);
            }
        }

        if (parent == null) {
            ensureFocusElement();
        }
        update();
    }

    protected Element getContainer(Node node) {
        if (node == null) {
            return rootContainer;
        }

        NodeDescriptor nodeDescriptor = findNode(node);
        if (nodeDescriptor != null) {
            return view.getDescendantsContainer(nodeDescriptor);
        }

        return null;
    }

    protected void onCollapse(Node node, NodeDescriptor nodeDescriptor, boolean deep) {
        if (nodeDescriptor.isExpanded() && fireCancellableEvent(new BeforeCollapseNodeEvent(node))) {
            nodeDescriptor.setExpanded(false);
            view.collapse(nodeDescriptor);

            update();

//            moveFocus(nodeDescriptor.getRootContainer());
            fireEvent(new CollapseNodeEvent(node));
        }

        if (deep) {
            setExpandChildren(node, false);
        }
    }

    protected void moveFocus(Element selectedElem) {
        if (selectedElem == null) {
            return;
        }

        int containerLeft = getAbsoluteLeft();
        int containerTop = getAbsoluteTop();

        int left = selectedElem.getAbsoluteLeft() - containerLeft;
        int top = selectedElem.getAbsoluteTop() - containerTop;

        int width = selectedElem.getOffsetWidth();
        int height = selectedElem.getOffsetHeight();

        if (width == 0 || height == 0) {
            focusEl.getStyle().setTop(0, Style.Unit.PX);
            focusEl.getStyle().setLeft(0, Style.Unit.PX);
            return;
        }

        focusEl.getStyle().setTop(top, Style.Unit.PX);
        focusEl.getStyle().setLeft(left, Style.Unit.PX);
    }

    protected String register(Node node) {
        String id = getUniqueId(node);
        if (nodeStorage.getNodeMap().containsKey(id)) {
            NodeDescriptor nodeDescriptor = nodeStorage.getNodeMap().get(id);
            if (nodeDescriptor.getDomId() == null || nodeDescriptor.getDomId().isEmpty()) {
                String domId = Document.get().createUniqueId();
                nodeDescriptor.setDomId(domId);
            }
            nodeDescriptor.reset();
            nodeDescriptor.clearElements();
            nodesByDom.put(nodeDescriptor.getDomId(), nodeDescriptor);
            return nodeDescriptor.getDomId();
        } else {
            NodeDescriptor nodeDescriptor = nodeStorage.wrap(node);
            String domId = Document.get().createUniqueId();
            nodeDescriptor.setDomId(domId);

            nodesByDom.put(nodeDescriptor.getDomId(), nodeDescriptor);
            return domId;
        }
    }

    protected void unregister(Node node) {
        if (node != null) {
            NodeDescriptor nodeDescriptor = nodeStorage.getNodeMap().remove(getUniqueId(node));
            if (nodeDescriptor != null) {
                nodesByDom.remove(nodeDescriptor.getDomId());
                nodeDescriptor.clearElements();
                nodeStorage.getNodeMap().remove(nodeStorage.getKeyProvider().getKey(node));
            }
        }

    }

    protected boolean fireCancellableEvent(GwtEvent<?> event) {
        fireEvent(event);
        if (event instanceof CancellableEvent) {
            return !((CancellableEvent)event).isCancelled();
        }
        return true;
    }

    public void setAutoLoad(boolean autoLoad) {
        this.autoLoad = autoLoad;
    }

    public void setAutoExpand(boolean autoExpand) {
        this.autoExpand = autoExpand;
    }

    public void setAutoSelect(boolean autoSelect) {
        this.autoSelect = autoSelect;
    }

    protected void ensureFocusElement() {
        if (focusEl != null) {
            focusEl.removeFromParent();
        }
        focusEl = getElement().appendChild(focusImpl.createFocusable());
        focusEl.addClassName(treeStyles.styles().noFocusOutline());
        if (focusEl.hasChildNodes()) {
            focusEl.getFirstChildElement().addClassName(treeStyles.styles().noFocusOutline());
            Style focusElStyle = focusEl.getFirstChildElement().getStyle();
            focusElStyle.setBorderWidth(0, Style.Unit.PX);
            focusElStyle.setFontSize(1, Style.Unit.PX);
            focusElStyle.setPropertyPx("lineHeight", 1);
        }
        focusEl.getStyle().setLeft(0, Style.Unit.PX);
        focusEl.getStyle().setTop(0, Style.Unit.PX);
        focusEl.getStyle().setPosition(Style.Position.ABSOLUTE);
        addEventsSunk(focusEl, Event.FOCUSEVENTS);
    }

    private void addEventsSunk(Element element, int event) {
        int bits = DOM.getEventsSunk((Element)element.cast());
        DOM.sinkEvents((Element)element.cast(), bits | event);
    }

    protected void update() {
        if (updateTask == null) {
            updateTask = new DelayedTask() {
                @Override
                public void onExecute() {
                    doUpdate();
                }
            };
        }
        updateTask.delay(view.getScrollDelay());
    }

    protected void doUpdate() {
        int count = getVisibleRowCount();
        if (count > 0) {
            List<Node> rootItems = getRootNodes();
            List<Node> visible = getAllChildNodes(rootItems, true);
            int[] vr = getVisibleRows(visible, count);

            for (int i = vr[0]; i <= vr[1]; i++) {
                if (goIntoMode.isActivated()) {
                    //constraint node indention
                    int goIntoDirDepth = nodeStorage.getDepth(goIntoMode.getLastNode());
                    int currentNodeDepth = nodeStorage.getDepth(visible.get(i));

                    view.onDepthUpdated(findNode(visible.get(i)), currentNodeDepth - goIntoDirDepth);
                }
                if (!isRowRendered(i, visible)) {
                    Node parent = nodeStorage.getParent(visible.get(i));
                    Element html = renderChild(visible.get(i), nodeStorage.getDepth(parent));
                    view.getRootContainer(findNode(visible.get(i))).getFirstChildElement().setInnerHTML(html.getString());
                }
            }
            clean();
        }
    }


    public List<Node> getRootNodes() {
        return goIntoMode.isActivated() ? Collections.singletonList(goIntoMode.getLastNode()) : nodeStorage.getRootItems();
    }

    protected void clean() {
        if (cleanTask == null) {
            cleanTask = new DelayedTask() {
                @Override
                public void onExecute() {
                    doClean();
                }
            };
        }
        cleanTask.delay(view.getCleanDelay()); //todo make it configurable
    }

    protected void doClean() {
        int count = getVisibleRowCount();
        if (count > 0) {
            List<Node> rows = getAllChildNodes(getRootNodes(), true);
            int[] vr = getVisibleRows(rows, count);
            vr[0] -= view.getCacheSize();
            vr[1] += view.getCacheSize();

            int i = 0;

            // if first is less than 0, all rows have been rendered
            // so lets clean the end...
            if (vr[0] <= 0) {
                i = vr[1] + 1;
            }
            for (int len = rows.size(); i < len; i++) {
                // if current row is outside of first and last and
                // has content, update the innerHTML to nothing
                if (i < vr[0] || i > vr[1]) {
                    cleanNode(findNode(rows.get(i)));
                }
            }
        }
    }

    protected void cleanNode(NodeDescriptor node) {
        if (node != null && node.getRootContainer() != null) {
            node.clearElements();
            Element element = view.getRootContainer(node).getFirstChildElement();
            removeElementChildren(element);
        }
    }

    protected boolean isRowRendered(int i, List<Node> visible) {
        Element e = view.getRootContainer(findNode(visible.get(i)));
        return e != null && e.getFirstChild().hasChildNodes();
    }

    public TreeView getView() {
        return view;
    }

    protected int getVisibleRowCount() {
        int rh = view.getCalculatedRowHeight();
        int visibleHeight = getElement().getOffsetHeight(); //TODO do we need to remove padding and margin from height?
        return (int)((visibleHeight < 1) ? 0 : Math.ceil(visibleHeight / rh));
    }

    public List<Node> getAllChildNodes(List<Node> parent, boolean onlyVisible) {
        List<Node> list = new ArrayList<>();
        for (Node node : parent) {
            list.add(node);
            if (!onlyVisible || findNode(node).isExpanded()) {
                findChildren(node, list, onlyVisible);
            }
        }
        return list;
    }

    protected void findChildren(Node parent, List<Node> list, boolean onlyVisible) {
        for (Node child : nodeStorage.getChildren(parent)) {
            list.add(child);
            if (!onlyVisible || findNode(child).isExpanded()) {
                findChildren(child, list, onlyVisible);
            }
        }
    }

    protected int[] getVisibleRows(List<Node> visible, int count) {
        int sc = getElement().getScrollTop();
        int start = (int)(sc == 0 ? 0 : Math.floor(sc / view.getCalculatedRowHeight()) - 1);
        int first = Math.max(start, 0);
        int last = Math.min(start + count + 2, visible.size() - 1);
        return new int[]{first, last};
    }

    protected Element renderChild(Node child, int depth) {
        String domID = register(child);
        return getNodePresentationRenderer().render(child, domID, getJoint(child), depth);
    }

    protected Joint getJoint(Node node) {
        if (node == null) {
            return Joint.NONE;
        }

        if (isLeaf(node)) {
            return Joint.NONE;
        }

        if (findNode(node) != null && findNode(node).isLoaded() && nodeStorage.getChildCount(node) == 0) {
            return Joint.NONE;
        }

        return findNode(node).isExpanded() ? Joint.EXPANDED : Joint.COLLAPSED;

//        return isLeaf(node) ? Joint.NONE : findNode(node).isExpanded() ? Joint.EXPANDED : Joint.COLLAPSED;
    }

    public void scrollIntoView(Node model) {
        NodeDescriptor node = findNode(model);
        if (node != null) {
            Element con = node.getNodeContainerElement();
            if (con != null) {
                con.scrollIntoView();
                focusEl.getStyle().setLeft(con.getAbsoluteLeft(), Style.Unit.PX);
                focusEl.getStyle().setTop(con.getAbsoluteTop(), Style.Unit.PX);
            }
        }
    }

    public void focus() {
        focusImpl.focus(focusEl);
    }

    public TreeNodeStorage getNodeStorage() {
        return nodeStorage;
    }

    public TreeNodeLoader getNodeLoader() {
        return nodeLoader;
    }

    public void setNodeStorage(TreeNodeStorage nodeStorage) {
        if (this.nodeStorage != null) {
            storeHandlers.removeHandler();
            if (isOrWasAttached()) {
                clear();
            }
        }

        this.nodeStorage = nodeStorage;

        if (this.nodeStorage != null) {
            Handler handler = new Handler();
            storeHandlers.add(nodeStorage.addStoreAddHandler(handler));
            storeHandlers.add(nodeStorage.addStoreUpdateHandler(handler));
            storeHandlers.add(nodeStorage.addStoreRemoveHandler(handler));
            storeHandlers.add(nodeStorage.addStoreDataChangeHandler(handler));
            storeHandlers.add(nodeStorage.addStoreClearHandler(handler));
            storeHandlers.add(nodeStorage.addStoreSortHandler(handler));

            if (getSelectionModel() != null) {
                getSelectionModel().bindStorage(nodeStorage);
            }
            if (isOrWasAttached()) {
                renderChildren(null);
            }
        } else {
            if (isAttached()) {
                throw new IllegalStateException("Tree should be initialized with tree storage.");
            }
        }
    }

    public void clear() {
        if (isOrWasAttached()) {
            Element container = getContainer(null);
            container.setInnerHTML("");

            Map<String, NodeDescriptor> nodeMap = getNodeStorage().getNodeMap();
            for (NodeDescriptor nodeDescriptor : nodeMap.values()) {
                nodeDescriptor.clearElements();
                nodeDescriptor.reset();
            }

            nodesByDom.clear();
            if (isAttached()) {
                moveFocus(getContainer(null));
            }
            getEmptyText().paint(); //draw empty label
        }
    }

    public TreeSelectionModel getSelectionModel() {
        return selectionModel;
    }

    public void setSelectionModel(TreeSelectionModel sm) {
        if (this.selectionModel != null) {
            this.selectionModel.bindTree(null);
        }
        this.selectionModel = sm;
        if (sm != null) {
            sm.bindTree(this);
        }
    }

    @Override
    protected void onAttach() {
        boolean isOrWasAttached = isOrWasAttached();
        super.onAttach();
        if (nodeStorage == null) {
            throw new IllegalStateException("Cannot attach a tree without a store");
        }

        if (!isOrWasAttached) {
            onAfterFirstAttach();
        }

        update();
    }

    /** {@inheritDoc} */
    @Override
    public void onBrowserEvent(Event event) {

        switch (event.getTypeInt()) {
            case Event.ONCLICK:
                onClick(event);
                break;
            case Event.ONDBLCLICK:
                onDoubleClick(event);
                break;
            case Event.ONSCROLL:
                onScroll(event);
                break;
            case Event.ONFOCUS:
                onFocus(event);
                break;
            case Event.ONBLUR:
                onBlur(event);
                break;
            case Event.ONCONTEXTMENU:
                if (disableNativeContextMenu) {
                    event.preventDefault();
                }
                onRightClick(event);
                break;
        }
        view.onEvent(event);

        // we are not calling super so must fire dom events
        DomEvent.fireNativeEvent(event, this, this.getElement());
    }

    /**
     * Returns {@code true} if nodes are highlighted on mouse over.
     *
     * @return true if enabled
     */
    public boolean isTrackMouseOver() {
        return trackMouseOver;
    }

    /**
     * True to highlight nodes when the mouse is over (defaults to {@code true}).
     *
     * @param trackMouseOver
     *         {@code true} to highlight nodes on mouse over
     */
    public void setTrackMouseOver(boolean trackMouseOver) {
        this.trackMouseOver = trackMouseOver;
    }

    protected void onRightClick(final Event event) {
        event.preventDefault();
        event.stopPropagation();

        final int x = event.getClientX();
        final int y = event.getClientY();
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                onShowContextMenu(x, y);
            }
        });
    }

    private void onShowContextMenu(int x, int y) {
        if (contextMenuInvocationHandler != null && disableNativeContextMenu) {
            contextMenuInvocationHandler.invokeContextMenuOn(x, y);
        }
    }

    protected void onFocus(Event event) {
        fireEvent(new FocusEvent());
    }

    protected void onBlur(Event event) {
        fireEvent(new BlurEvent());
    }

    protected void onScroll(Event event) {
        update();
        constrainFocusElement();
    }

    protected void constrainFocusElement() {
        if (!focusConstrainScheduled) {
            focusConstrainScheduled = true;
            Scheduler.get().scheduleFinally(new Scheduler.ScheduledCommand() {
                @Override
                public void execute() {
                    focusConstrainScheduled = false;
                    int scrollLeft = getElement().getScrollLeft();
                    int scrollTop = getElement().getScrollTop();
                    int left = getElement().getOffsetWidth() / 2 + scrollLeft;
                    int top = getElement().getOffsetHeight() / 2 + scrollTop;
                    focusEl.getStyle().setTop(top, Style.Unit.PX);
                    focusEl.getStyle().setLeft(left, Style.Unit.PX);
                }
            });
        }
    }

    protected void onDoubleClick(Event event) {
        NodeDescriptor nodeDescriptor = findNode(event.getEventTarget().<Element>cast());
        if (nodeDescriptor == null) {
            return;
        }

        if (nodeDescriptor.isLeaf()) {
            if (nodeDescriptor.getNode() instanceof HasAction) {
                ((HasAction)nodeDescriptor.getNode()).actionPerformed();
            }
        } else {
            setExpanded(nodeDescriptor.getNode(), !nodeDescriptor.isExpanded());
        }
    }

    protected void onClick(Event event) {
        NativeTreeEvent e = event.cast();
        NodeDescriptor node = findNode((Element)event.getEventTarget().cast());
        if (node != null) {
            Element jointEl = view.getJointContainer(node);
            if (jointEl != null && e.within(jointEl)) {
                toggle(node.getNode());
            }
        }

        focusEl.getStyle().setTop(event.getClientY(), Style.Unit.PX);
        focusEl.getStyle().setLeft(event.getClientX(), Style.Unit.PX);
        focus();
    }

    public void toggle(Node node) {
        NodeDescriptor nodeDescriptor = findNode(node);
        if (nodeDescriptor != null) {
            setExpanded(node, !nodeDescriptor.isExpanded());
        }
    }

    protected void onAfterFirstAttach() {
        rootContainer = getRootContainer();

        getElement().getStyle().setVisibility(Style.Visibility.VISIBLE);

        renderChildren(null);

        setAllowTextSelection(false);
        sinkEvents(Event.ONSCROLL | Event.ONCLICK | Event.ONDBLCLICK | Event.MOUSEEVENTS | Event.KEYEVENTS);
    }

    private Element createNodesContainer() {
        return getNodePresentationRenderer().getDescendantsContainer();
    }

    public void setAllowTextSelection(boolean enable) {
        allowTextSelection = enable;
        if (isAttached()) {
            disableTextSelection(getRootContainer(), !enable);
        }
    }

    protected Element getRootContainer() {
        return getElement();
    }

    protected void onAdd(StoreAddEvent event) {
        for (Node child : event.getNodes()) {
            register(child);
        }
        if (isOrWasAttached()) {
            Node parent = nodeStorage.getParent(event.getNodes().get(0));
            NodeDescriptor pn = findNode(parent);
            if (parent == null || (pn != null && pn.isChildrenRendered())) {
                int parentDepth = parent == null ? 0 : nodeStorage.getDepth(parent);

                Element container = getContainer(parent);

                int index = event.getIndex();
                int parentChildCount = parent == null ? nodeStorage.getRootCount() : nodeStorage.getChildCount(parent);


                for (Node child : event.getNodes()) {
                    if (index == 0) {
                        container.insertFirst(renderChild(child, parentDepth));
                    } else if (index == parentChildCount - event.getNodes().size()) {
                        com.google.gwt.dom.client.Node lastChild = container.getLastChild();
                        container.insertAfter(renderChild(child, parentDepth), lastChild);
                    } else {
                        container.insertBefore(renderChild(child, parentDepth), container.getChild(index));
                    }
                }
            }
            refresh(parent);
            update();
        }
    }

    protected void onClear(StoreClearEvent event) {
        clear();
    }

    protected void onDataChanged(StoreDataChangeEvent event) {
        redraw(event.getParent());
    }

    protected void onRemove(StoreRemoveEvent se) {
        NodeDescriptor nodeDescriptor = findNode(se.getNode());
        if (nodeDescriptor != null) {
            if (view.getRootContainer(nodeDescriptor) != null) {
                nodeDescriptor.getRootContainer().removeFromParent();
            }
            unregister(se.getNode());

            for (Node child : se.getChildren()) {
                unregister(child);
            }

            Node parent = se.getParent();
            NodeDescriptor pNodeDescriptor = findNode(parent);
            if (pNodeDescriptor != null && pNodeDescriptor.isExpanded() && nodeStorage.getChildCount(pNodeDescriptor.getNode()) == 0) {
                setExpanded(pNodeDescriptor.getNode(), false);
            } else if (pNodeDescriptor != null && nodeStorage.getChildCount(pNodeDescriptor.getNode()) == 0) {
                refresh(parent);
            }
            moveFocus(nodeDescriptor.getRootContainer());
        }
    }

    protected void onSort(StoreSortEvent se) {
        redraw(null);
    }

    protected void onUpdate(StoreUpdateEvent event) {
        for (Node node : event.getNodes()) {
            NodeDescriptor nodeDescriptor = findNode(node);
            if (nodeDescriptor != null) {
                if (nodeDescriptor.getNode() != node) {
                    nodeDescriptor.setNode(node);
                }
                refresh(node);
            }
        }
    }

    public void refresh(Node node) {
        if (isOrWasAttached()) {
            NodeDescriptor nodeDescriptor = findNode(node);
            if (node != null && view.getRootContainer(nodeDescriptor) != null) {
                view.onJointChange(nodeDescriptor, getJoint(node));
                view.onInfoTextChange(nodeDescriptor, getUpdatedInfoText(node));
            }
        }
    }

    @Nullable
    private String getUpdatedInfoText(Node node) {
        if (node instanceof HasPresentation) {
            return ((HasPresentation)node).getPresentation(false).getInfoText();
        }

        return null;
    }

    public void synchronize() {
        //TODO need to improve this block of code to support refreshing dedicated folder
        redraw(null, true);
    }

    protected void redraw(Node node) {
        redraw(node, false);
    }


    /**
     * Completely redraws the children of the given parent (or all items if parent is null), throwing away details like
     * currently expanded nodes, etc.
     *
     * @param parent
     *         the parent of the items to redraw
     */
    protected void redraw(Node parent, boolean resetCache) {
        if (!isOrWasAttached()) {
            return;
        }

        if (parent == null) {
            clear();
            renderChildren(null, resetCache);

            if (autoSelect) {
                Node child = nodeStorage.getChild(0);
                if (child != null) {
                    List<Node> selection = new ArrayList<>();
                    selection.add(child);
                    getSelectionModel().setSelection(selection);
                }
            }
        } else {
            NodeDescriptor nodeDescriptor = findNode(parent);
            nodeDescriptor.setLoaded(true);
            nodeDescriptor.setLoading(false);

            if (isLeaf(nodeDescriptor.getNode())) {
                return;
            }

            if (isExpanded(parent)) {
                setExpanded(parent, false, true);
                Element container = getContainer(parent);
                container.setInnerHTML("");
                nodeDescriptor.setChildrenRendered(false);
                setExpanded(parent, true, nodeDescriptor.isExpandDeep());
            } else {
                if (nodeDescriptor.isChildrenRendered()) {
                    Element container = getContainer(parent);
                    container.setInnerHTML("");
                    nodeDescriptor.setChildrenRendered(false);
                }
                setExpanded(parent, true, nodeDescriptor.isExpandDeep());
            }
        }
    }

    public void disableBrowserContextMenu(boolean disable) {
        disableNativeContextMenu = disable;
        if (disable) {
            sinkEvents(Event.ONCONTEXTMENU);
        }
    }

    public boolean isAllowTextSelection() {
        return allowTextSelection;
    }

    public void setContextMenuInvocationHandler(ContextMenuInvocationHandler invocationHandler) {
        contextMenuInvocationHandler = invocationHandler;
    }

    public TreeStyles getTreeStyles() {
        return treeStyles;
    }

    @Nonnull
    @Override
    public StatusText getEmptyText() {
        return emptyText;
    }

    public ContextMenuInvocationHandler getContextMenuInvocationHandler() {
        return contextMenuInvocationHandler;
    }

    private native static void disableTextSelection(Element e, boolean disable)/*-{
        if (disable) {
            e.ondrag = function () {
                return false;
            };
            e.onselectstart = function () {
                return false;
            };
            e.style.MozUserSelect = "none"
        } else {
            e.ondrag = null;
            e.onselectstart = null;
            e.style.MozUserSelect = "text"
        }
    }-*/;

    public void expandAll() {
        for (Node node : nodeStorage.getRootItems()) {
            setExpanded(node, true, true);
        }
    }

    public void collapseAll() {
        for (Node node : nodeStorage.getRootItems()) {
            setExpanded(node, false, true);
        }
    }

    public PresentationRenderer<Node> getNodePresentationRenderer() {
        if (nodePresentationRenderer == null) {
            nodePresentationRenderer = new DefaultPresentationRenderer<>(treeStyles);
        }
        return nodePresentationRenderer;
    }

    public void setNodePresentationRenderer(PresentationRenderer<Node> nodePresentationRenderer) {
        this.nodePresentationRenderer = nodePresentationRenderer;
    }

    private void removeElementChildren(Element element) {
        Element child;
        while ((child = element.getFirstChildElement()) != null) {
            element.removeChild(child);
        }
    }

    public void setGoIntoMode(GoIntoMode goIntoMode) {
        this.goIntoMode = goIntoMode;
        this.goIntoMode.bindTree(this);
    }

    public GoIntoMode getGoIntoMode() {
        return goIntoMode;
    }
}
