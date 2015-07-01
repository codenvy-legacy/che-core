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
package org.eclipse.che.ide.part.editor;

import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwtmockito.GwtMockitoTestRunner;

import org.eclipse.che.ide.api.parts.PartPresenter;
import org.eclipse.che.ide.api.parts.PartStackUIResources;
import org.eclipse.che.ide.api.parts.PartStackView.ActionDelegate;
import org.eclipse.che.ide.api.parts.PartStackView.TabItem;
import org.eclipse.che.ide.part.widgets.listtab.ListButton;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.util.Arrays;

import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Dmitry Shnurenko
 */
@RunWith(GwtMockitoTestRunner.class)
public class EditorPartStackViewTest {

    private static final String SOME_TEXT = "someText";

    //constructor mocks
    @Mock(answer = RETURNS_DEEP_STUBS)
    private PartStackUIResources resources;

    //additional mocks
    @Mock
    private ListButton     listButton;
    @Mock
    private ActionDelegate delegate;
    @Mock
    private TabItem        tabItem;
    @Mock
    private PartPresenter  partPresenter;
    @Mock
    private IsWidget       isWidget;
    @Mock
    private Widget         widget;

    @Captor
    private ArgumentCaptor<AcceptsOneWidget> containerCaptor;

    private EditorPartStackView view;

    @Before
    public void setUp() {
        when(tabItem.getView()).thenReturn(isWidget);
        when(partPresenter.getView()).thenReturn(isWidget);
        when(isWidget.asWidget()).thenReturn(widget);

        when(resources.partStackCss().unSelectEditorBorder()).thenReturn(SOME_TEXT);

        view = new EditorPartStackView(resources);
        view.setDelegate(delegate);
        view.setListButton(listButton);
    }

    @Test
    public void listButtonShouldBeSet() {
        verify(view.tabsPanel).add(listButton);
    }

    @Test
    public void mouseShouldBeDown() {
        MouseDownEvent event = mock(MouseDownEvent.class);
        view.onMouseDown(event);

        verify(delegate).onRequestFocus();
    }

    @Test
    public void tabShouldBeAdded() {
        view.addTab(tabItem, partPresenter);

        verify(view.tabsPanel).add(isWidget);
        verify(partPresenter).go(containerCaptor.capture());

        containerCaptor.getValue().setWidget(isWidget);

        verify(view.contentPanel).add(isWidget);
    }

    @Test
    public void tabShouldBeAddedInStartOfPanel() {
        when(widget.getOffsetWidth()).thenReturn(200);

        view.addTab(tabItem, partPresenter);
        reset(view.tabsPanel, partPresenter, listButton);

        view.addTab(tabItem, partPresenter);

        verify(view.tabsPanel).insert(isWidget, 1);
        verify(view.tabsPanel, never()).add(isWidget);

        verify(partPresenter).go(containerCaptor.capture());
        verify(listButton).setVisible(true);
    }

    @Test
    public void tabShouldBeRemoved() {
        when(view.tabsPanel.getOffsetWidth()).thenReturn(300);
        when(widget.getOffsetWidth()).thenReturn(250);

        view.addTab(tabItem, partPresenter);

        view.removeTab(partPresenter);

        verify(view.tabsPanel).remove(isWidget);
        verify(view.contentPanel).remove(isWidget);
    }

    @Test
    public void tabShouldBeSelected() {
        view.addTab(tabItem, partPresenter);

        view.selectTab(partPresenter);

        verify(view.contentPanel).getWidgetIndex(isWidget);
        verify(view.contentPanel).showWidget(0);

        verify(tabItem).select();

        verify(delegate).onRequestFocus();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void unsupportedOperationExceptionShouldBeThrownWhenTrySetTabPositions() throws Exception {
        view.setTabPositions(Arrays.asList(partPresenter));
    }

    @Test
    public void focusShouldBeSet() {
        view.addTab(tabItem, partPresenter);
        view.selectTab(partPresenter);
        reset(tabItem);

        view.setFocus(true);

        verify(view.contentPanel).removeStyleName(SOME_TEXT);
        verify(tabItem).select();
    }

    @Test
    public void focusShouldNotBeSet() {
        view.addTab(tabItem, partPresenter);
        view.selectTab(partPresenter);
        reset(tabItem);

        view.setFocus(false);

        verify(view.contentPanel).addStyleName(SOME_TEXT);
        verify(tabItem).unSelect();
    }

    @Test
    public void tabItemShouldBeUpdated() {
        view.addTab(tabItem, partPresenter);

        view.updateTabItem(partPresenter);

        verify(tabItem).update(partPresenter);
    }
}