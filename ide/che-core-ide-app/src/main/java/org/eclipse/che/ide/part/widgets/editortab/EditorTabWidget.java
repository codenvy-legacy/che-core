/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.part.widgets.editortab;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ContextMenuEvent;
import com.google.gwt.event.dom.client.ContextMenuHandler;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import com.google.web.bindery.event.shared.EventBus;
import org.eclipse.che.ide.api.event.FileEvent;
import org.eclipse.che.ide.api.parts.PartPresenter;
import org.eclipse.che.ide.api.parts.PartStackUIResources;
import org.eclipse.che.ide.api.parts.PartStackView.TabPosition;
import org.eclipse.che.ide.api.project.tree.VirtualFile;
import org.eclipse.che.ide.part.editor.EditorTabContextMenuFactory;
import org.vectomatic.dom.svg.ui.SVGImage;
import org.vectomatic.dom.svg.ui.SVGResource;

import javax.validation.constraints.NotNull;

/**
 * Editor tab widget. Contains icon, title and close mark.
 * May be pinned. Pin state indicates whether this tab should be skipped during operation "Close All but Pinned".
 *
 * @author Dmitry Shnurenko
 * @author Valeriy Svydenko
 * @author Vitaliy Guliy
 * @author Vlad Zhukovskyi
 */
public class EditorTabWidget extends Composite implements EditorTab, ContextMenuHandler {

    interface EditorTabWidgetUiBinder extends UiBinder<Widget, EditorTabWidget> {
    }

    private static final EditorTabWidgetUiBinder UI_BINDER = GWT.create(EditorTabWidgetUiBinder.class);

    @UiField
    SimplePanel iconPanel;

    @UiField
    Label title;

    @UiField
    SVGImage closeIcon;

    @UiField(provided = true)
    final PartStackUIResources resources;

    private final EventBus eventBus;
    private final EditorTabContextMenuFactory editorTabContextMenu;
    private final VirtualFile file;

    private ActionDelegate delegate;
    private boolean        pinned;
    private SVGResource    icon;

    @Inject
    public EditorTabWidget(@Assisted VirtualFile file,
                           @Assisted SVGResource icon,
                           @Assisted String title,
                           PartStackUIResources resources,
                           EditorTabContextMenuFactory editorTabContextMenu,
                           EventBus eventBus) {
        this.resources = resources;
        this.eventBus = eventBus;

        initWidget(UI_BINDER.createAndBindUi(this));

        this.editorTabContextMenu = editorTabContextMenu;
        this.file = file;
        this.icon = icon;
        this.title.setText(title);

        iconPanel.add(getIcon());


        addDomHandler(this, ClickEvent.getType());
        addDomHandler(this, DoubleClickEvent.getType());
        addDomHandler(this, ContextMenuEvent.getType());
    }

    /** {@inheritDoc} */
    @Override
    public Widget getIcon() {
        return new SVGImage(icon);
    }

    /** {@inheritDoc} */
    @Override
    @NotNull
    public String getTitle() {
        return title.getText();
    }

    /** {@inheritDoc} */
    @NotNull
    @Override
    public IsWidget getView() {
        return this.asWidget();
    }

    /** {@inheritDoc} */
    @Override
    public void update(@NotNull PartPresenter part) {
        this.title.setText(part.getTitle());
    }

    /** {@inheritDoc} */
    @Override
    public void select() {
        /** Marks tab is focused */
        getElement().setAttribute("focused", "");
    }

    /** {@inheritDoc} */
    @Override
    public void unSelect() {
        /** Marks tab is not focused */
        getElement().removeAttribute("focused");
    }

    /** {@inheritDoc} */
    @Override
    public void setTabPosition(@NotNull TabPosition tabPosition) {
        throw new UnsupportedOperationException("This method doesn't allow in this class " + getClass());
    }

    /** {@inheritDoc} */
    @Override
    public void setErrorMark(boolean isVisible) {
        if (isVisible) {
            title.addStyleName(resources.partStackCss().lineError());
        } else {
            title.removeStyleName(resources.partStackCss().lineError());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setWarningMark(boolean isVisible) {
        if (isVisible) {
            title.addStyleName(resources.partStackCss().lineWarning());
        } else {
            title.removeStyleName(resources.partStackCss().lineWarning());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onClick(@NotNull ClickEvent event) {
        if (NativeEvent.BUTTON_LEFT == event.getNativeButton()) {
            delegate.onTabClicked(this);
        } else if (NativeEvent.BUTTON_MIDDLE == event.getNativeButton()) {
            eventBus.fireEvent(new FileEvent(file, FileEvent.FileOperation.CLOSE));
            delegate.onTabClose(this);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onContextMenu(ContextMenuEvent event) {
        //construct for each editor tab own context menu,
        //that will have store information about selected virtual file and pin state at first step
        //in future maybe we should create another mechanism to associate context menu with initial dto's
        editorTabContextMenu.newContextMenu(this)
                            .show(event.getNativeEvent().getClientX(), event.getNativeEvent().getClientY());
    }

    /** {@inheritDoc} */
    @Override
    public void onDoubleClick(@NotNull DoubleClickEvent event) {
        expandEditor();
    }

    private native void expandEditor() /*-{
        try {
            $wnd.IDE.eventHandlers.expandEditor();
        } catch (e) {
            console.log(e.message);
        }
    }-*/;

    /** {@inheritDoc} */
    @Override
    public void setDelegate(ActionDelegate delegate) {
        this.delegate = delegate;
    }

    @UiHandler("closeIcon")
    public void onCloseButtonClicked(@SuppressWarnings("UnusedParameters") ClickEvent event) {
        eventBus.fireEvent(new FileEvent(file, FileEvent.FileOperation.CLOSE));
        delegate.onTabClose(this);
    }

    /** {@inheritDoc} */
    @Override
    public void setReadOnlyMark(boolean isVisible) {
        if (isVisible) {
            getElement().setAttribute("readonly", "");
        } else {
            getElement().removeAttribute("readonly");
        }
    }

    /** {@inheritDoc} */
    @Override
    public VirtualFile getFile() {
        return file;
    }

    /** {@inheritDoc} */
    @Override
    public void setPinMark(boolean pinned) {
        this.pinned = pinned;

        if (pinned) {
            getElement().setAttribute("pinned", "");
        } else {
            getElement().removeAttribute("pinned");
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isPinned() {
        return pinned;
    }
}
