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
import org.eclipse.che.ide.part.widgets.listtab.ListItem;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.vectomatic.dom.svg.ui.SVGResource;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
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
    private EditorTab          editorTab1;
    @Mock
    private EditorTab          editorTab2;
    @Mock
    private EditorWithErrors   withErrorsPart;
    @Mock
    private PartPresenter      partPresenter1;
    @Mock
    private SVGResource        resource1;
    @Mock
    private PartPresenter      partPresenter2;
    @Mock
    private SVGResource        resource2;
    @Mock
    private ProjectActionEvent actionEvent;

    @Captor
    private ArgumentCaptor<ListItem>             itemCaptor;
    @Captor
    private ArgumentCaptor<ProjectActionHandler> projectActionHandlerCaptor;

    private EditorPartStackPresenter presenter;

    @Before
    public void setUp() {
        when(partPresenter1.getTitle()).thenReturn(SOME_TEXT);
        when(partPresenter1.getTitleSVGImage()).thenReturn(resource1);

        when(partPresenter2.getTitle()).thenReturn(SOME_TEXT);
        when(partPresenter2.getTitleSVGImage()).thenReturn(resource2);

        when(tabItemFactory.createEditorPartButton(resource1, SOME_TEXT)).thenReturn(editorTab1);
        when(tabItemFactory.createEditorPartButton(resource2, SOME_TEXT)).thenReturn(editorTab2);

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
        presenter.addPart(partPresenter1);

        verify(eventBus).addHandler(eq(ProjectActionEvent.TYPE), projectActionHandlerCaptor.capture());
        projectActionHandlerCaptor.getValue().onProjectClosed(actionEvent);

        verify(listButton).addListItem(itemCaptor.capture());
        ListItem item = itemCaptor.getValue();

        verify(view).removeTab(partPresenter1);
        verify(listButton).removeListItem(item);
    }

    @Test
    public void focusShouldBeSet() {
        presenter.setFocus(true);

        verify(view).setFocus(true);
    }

    @Test
    public void partShouldBeAdded() {
        presenter.addPart(partPresenter1);

        verify(partPresenter1).addPropertyListener(Matchers.<PropertyListener>anyObject());

        verify(tabItemFactory).createEditorPartButton(resource1, SOME_TEXT);

        verify(partPresenter1).getTitleSVGImage();
        verify(partPresenter1).getTitle();

        verify(editorTab1).setDelegate(presenter);

        verify(view).addTab(editorTab1, partPresenter1);

        verify(listButton).addListItem(Matchers.<ListItem>anyObject());

        verify(view).selectTab(partPresenter1);
    }

    @Test
    public void partShouldNotBeAddedWhenItExist() {
        presenter.addPart(partPresenter1);
        reset(view);

        presenter.addPart(partPresenter1);

        verify(view, never()).addTab(editorTab1, partPresenter1);

        verify(view).selectTab(partPresenter1);
    }

    @Test
    public void activePartShouldBeReturned() {
        presenter.setActivePart(partPresenter1);

        assertThat(presenter.getActivePart(), sameInstance(partPresenter1));
    }

    @Test
    public void onTabShouldBeClicked() {
        presenter.addPart(partPresenter1);
        reset(view);

        presenter.onTabClicked(editorTab1);

        verify(view).selectTab(partPresenter1);
    }

    @Test
    public void tabShouldBeClosed() {
        presenter.addPart(partPresenter1);

        presenter.onTabClose(editorTab1);

        verify(view).removeTab(partPresenter1);
    }

    @Test
    public void activePartShouldBeChangedWhenWeClickOnTab() {
        presenter.addPart(partPresenter1);
        presenter.addPart(partPresenter2);

        presenter.onTabClicked(editorTab1);

        assertThat(presenter.getActivePart(), equalTo(partPresenter1));

        presenter.onTabClicked(editorTab2);

        assertThat(presenter.getActivePart(), equalTo(partPresenter2));
    }

    @Test
    public void previousTabSelectedWhenWeRemovePart() {
        presenter.addPart(partPresenter1);
        presenter.addPart(partPresenter2);

        presenter.onTabClicked(editorTab2);
        presenter.onTabClose(editorTab2);

        assertThat(presenter.getActivePart(), equalTo(partPresenter1));
    }

    @Test
    public void activePartShouldBeNullWhenWeCloseAllParts() {
        presenter.addPart(partPresenter1);

        presenter.onTabClose(editorTab1);

        assertThat(presenter.getActivePart(), is(nullValue()));
    }
}
