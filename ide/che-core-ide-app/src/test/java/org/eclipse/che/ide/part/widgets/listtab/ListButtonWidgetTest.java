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
package org.eclipse.che.ide.part.widgets.listtab;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwtmockito.GwtMockitoTestRunner;

import org.eclipse.che.ide.Resources;
import org.eclipse.che.ide.part.widgets.listtab.ListButton.ActionDelegate;
import org.eclipse.che.ide.part.widgets.listtab.item.ListItem;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Dmitry Shnurenko
 */
@RunWith(GwtMockitoTestRunner.class)
public class ListButtonWidgetTest {

    private static final String SOME_TEXT = "someText";

    //constructor mocks
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Resources  resources;
    @Mock
    private FlowPanel  listPanel;
    @Mock
    private PopupPanel popupPanel;

    //additional mocks
    @Mock
    private Widget         widget;
    @Mock
    private Element        element;
    @Mock
    private ListItem       listItem;
    @Mock
    private ClickEvent     event;
    @Mock
    private ActionDelegate delegate;

    private ListButtonWidget listButton;

    @Before
    public void setUp() {
        when(resources.partStackCss().listItemPanel()).thenReturn(SOME_TEXT);

        listButton = new ListButtonWidget(resources, listPanel, popupPanel);
        listButton.setDelegate(delegate);
    }

    @Test
    public void constructorShouldBeVerified() {
        verify(listPanel).addStyleName(SOME_TEXT);
        verify(popupPanel).removeStyleName(anyString());
        verify(popupPanel).add(listPanel);
    }

    @Test
    public void listItemsShouldBeShown() {
        when(popupPanel.asWidget()).thenReturn(widget);
        when(widget.getElement()).thenReturn(element);

        listButton.showList();

        verify(listButton.button).getAbsoluteLeft();
        verify(listButton.button).getAbsoluteTop();
        verify(listButton.button).getOffsetWidth();

        verify(popupPanel).show();
        verify(element).setAttribute(anyString(), anyString());
    }

    @Test
    public void listItemShouldBeAdded() {
        listButton.addListItem(listItem);

        verify(resources.partStackCss()).listShownButtonBackground();
        verify(listPanel).add(listItem);
    }

    @Test
    public void listItemShouldBeRemoved() {
        listButton.addListItem(listItem);

        listButton.removeListItem(listItem);

        verify(listPanel).remove(listItem);
    }

    @Test
    public void listButtonShouldBeHidden() {
        listButton.hide();

        verify(popupPanel).hide();
        verify(resources.partStackCss()).listShownButtonBackground();
    }

    @Test
    public void onButtonShouldBeClicked() {
        listButton.onClick(event);

        verify(popupPanel, never()).hide();
        verify(delegate).onListButtonClicked();
        verify(resources.partStackCss()).listShownButtonBackground();
    }

    @Test
    public void onButtonShouldBeTwiceClicked() {
        listButton.onClick(event);
        reset(delegate);

        listButton.onClick(event);

        verify(popupPanel).hide();
        verify(delegate, never()).onListButtonClicked();
        verify(resources.partStackCss(), times(2)).listShownButtonBackground();
    }

}