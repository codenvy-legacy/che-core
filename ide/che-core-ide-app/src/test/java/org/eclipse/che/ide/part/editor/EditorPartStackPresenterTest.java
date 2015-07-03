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

import com.google.gwtmockito.GwtMockitoTestRunner;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.ide.api.editor.EditorWithErrors;
import org.eclipse.che.ide.api.event.ProjectActionEvent;
import org.eclipse.che.ide.api.event.ProjectActionHandler;
import org.eclipse.che.ide.api.parts.PartPresenter;
import org.eclipse.che.ide.api.parts.PropertyListener;
import org.eclipse.che.ide.client.inject.factories.TabItemFactory;
import org.eclipse.che.ide.part.PartStackPresenter.PartStackEventHandler;
import org.eclipse.che.ide.part.PartsComparator;
import org.eclipse.che.ide.part.widgets.editortab.EditorTab;
import org.eclipse.che.ide.part.widgets.listtab.ListButton;
import org.eclipse.che.ide.part.widgets.listtab.item.ListItem;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.vectomatic.dom.svg.ui.SVGResource;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Dmitry Shnurenko
 */
@RunWith(GwtMockitoTestRunner.class)
public class EditorPartStackPresenterTest {

    private static final String SOME_TEXT = "someText";

    //constructor mocks
    @Mock
    private EditorPartStackView   view;
    @Mock
    private PartsComparator       partsComparator;
    @Mock
    private EventBus              eventBus;
    @Mock
    private TabItemFactory        tabItemFactory;
    @Mock
    private PartStackEventHandler partStackEventHandler;
    @Mock
    private ListButton            listButton;

    //additional mocks
    @Mock
    private EditorTab          editorTab;
    @Mock
    private EditorWithErrors   withErrorsPart;
    @Mock
    private PartPresenter      partPresenter;
    @Mock
    private SVGResource        resource;
    @Mock
    private ProjectActionEvent actionEvent;

    @Captor
    private ArgumentCaptor<ListItem>             itemCaptor;
    @Captor
    private ArgumentCaptor<ProjectActionHandler> projectActionHandlerCaptor;

    private EditorPartStackPresenter presenter;

    @Before
    public void setUp() {
        when(partPresenter.getTitle()).thenReturn(SOME_TEXT);
        when(partPresenter.getTitleSVGImage()).thenReturn(resource);

        when(tabItemFactory.createEditorPartButton(resource, SOME_TEXT)).thenReturn(editorTab);

        presenter = new EditorPartStackPresenter(view, partsComparator, eventBus, tabItemFactory, partStackEventHandler, listButton);
    }

    @Test
    public void constructorShouldBeVerified() {
        verify(listButton).setDelegate(presenter);
        verify(view, times(2)).setDelegate(presenter);
        verify(view).setListButton(listButton);

        verify(eventBus).addHandler(eq(ProjectActionEvent.TYPE), Matchers.<ProjectActionHandler>anyObject());
    }

    @Test
    public void allTabsShouldBeClosed() {
        presenter.addPart(partPresenter);

        verify(eventBus).addHandler(eq(ProjectActionEvent.TYPE), projectActionHandlerCaptor.capture());
        projectActionHandlerCaptor.getValue().onProjectClosed(actionEvent);

        verify(listButton).addListItem(itemCaptor.capture());
        ListItem item = itemCaptor.getValue();

        verify(view).removeTab(partPresenter);
        verify(listButton).removeListItem(item);
    }

    @Test
    public void focusShouldBeSet() {
        presenter.setFocus(true);

        verify(view).setFocus(true);
    }

    @Test
    public void partShouldBeAdded() {
        presenter.addPart(partPresenter);

        verify(partPresenter).addPropertyListener(Matchers.<PropertyListener>anyObject());

        verify(tabItemFactory).createEditorPartButton(resource, SOME_TEXT);

        verify(partPresenter).getTitleSVGImage();
        verify(partPresenter).getTitle();

        verify(editorTab).setDelegate(presenter);

        verify(view).addTab(editorTab, partPresenter);

        verify(listButton).addListItem(Matchers.<ListItem>anyObject());

        verify(view).selectTab(partPresenter);
    }

    @Test
    public void partShouldNotBeAddedWhenItExist() {
        presenter.addPart(partPresenter);
        reset(view);

        presenter.addPart(partPresenter);

        verify(view, never()).addTab(editorTab, partPresenter);

        verify(view).selectTab(partPresenter);
    }

    @Test
    public void activePartShouldBeReturned() {
        presenter.setActivePart(partPresenter);

        assertThat(presenter.getActivePart(), sameInstance(partPresenter));
    }

    @Test
    public void onTabShouldBeClicked() {
        presenter.addPart(partPresenter);
        reset(view);

        presenter.onTabClicked(editorTab);

        verify(view).selectTab(partPresenter);
    }

    @Test
    public void tabShouldBeClosed() {
        presenter.addPart(partPresenter);

        presenter.onTabClose(editorTab);

        verify(view).removeTab(partPresenter);
    }

    @Test
    public void onListButtonShouldBeClicked() {
        presenter.onListButtonClicked();

        verify(listButton).showList();
    }

    @Test
    public void onCloseItemShouldBeClicked() {
        presenter.addPart(partPresenter);

        verify(listButton).addListItem(itemCaptor.capture());
        ListItem item = itemCaptor.getValue();

        presenter.onCloseItemClicked(item);

        verify(listButton).removeListItem(item);
        verify(listButton).hide();
    }
}