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
package org.eclipse.che.ide.part.widgets.editortab;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.client.Element;
import com.google.gwtmockito.GwtMockitoTestRunner;

import org.eclipse.che.ide.api.parts.PartStackUIResources;
import org.eclipse.che.ide.part.widgets.editortab.EditorTab.ActionDelegate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.vectomatic.dom.svg.OMSVGSVGElement;
import org.vectomatic.dom.svg.ui.SVGResource;

import static junit.framework.Assert.assertEquals;
import static org.eclipse.che.ide.api.parts.PartStackView.TabPosition.BELOW;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Dmitry Shnurenko
 */
@RunWith(GwtMockitoTestRunner.class)
public class EditorTabWidgetTest {

    private static final String SOME_TEXT = "someText";

    //constructor mocks
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PartStackUIResources resources;
    @Mock
    private SVGResource          icon;

    //additional mocks
    @Mock
    private Element         element;
    @Mock
    private OMSVGSVGElement svg;
    @Mock
    private ActionDelegate  delegate;
    @Mock
    private ClickEvent      event;

    private EditorTabWidget tab;

    @Before
    public void setUp() {
        when(icon.getSvg()).thenReturn(svg);

        when(resources.partStackCss().opacity()).thenReturn(SOME_TEXT);
        when(resources.partStackCss().activeTabTextColor()).thenReturn(SOME_TEXT);
        when(resources.partStackCss().selectEditorTab()).thenReturn(SOME_TEXT);

        tab = new EditorTabWidget(resources, icon, SOME_TEXT);
        tab.setDelegate(delegate);
    }

    @Test
    public void constructorShouldBeVerified() {
        verify(tab.title).setText(SOME_TEXT);
        verify(tab.icon).getElement();
    }

    @Test
    public void titleShouldBeReturned() {
        tab.getTitle();

        verify(tab.title).getText();
    }

    @Test
    public void tabShouldBeSelected() {
        tab.select();

        verify(resources.partStackCss()).opacity();
        verify(tab.closeIcon).addStyleName(SOME_TEXT);

        verify(resources.partStackCss()).activeTabTextColor();
        verify(tab.title).addStyleName(SOME_TEXT);

        verify(resources.partStackCss()).selectEditorTab();
    }

    @Test
    public void tabShouldBeUnSelected() {
        tab.unSelect();

        verify(resources.partStackCss()).opacity();
        verify(tab.closeIcon).removeStyleName(SOME_TEXT);

        verify(resources.partStackCss()).activeTabTextColor();
        verify(tab.title).removeStyleName(SOME_TEXT);

        verify(resources.partStackCss()).selectEditorTab();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void exceptionShouldBeThrownWhenTrySetTabPosition() {
        tab.setTabPosition(BELOW, 1);
    }

    @Test
    public void errorMarkShouldBeSet() {
        when(resources.partStackCss().lineError()).thenReturn(SOME_TEXT);

        tab.setErrorMark(true);

        verify(resources.partStackCss()).lineError();
        verify(tab.title).addStyleName(SOME_TEXT);
    }

    @Test
    public void errorMarkShouldNotBeSet() {
        when(resources.partStackCss().lineError()).thenReturn(SOME_TEXT);

        tab.setErrorMark(false);

        verify(resources.partStackCss()).lineError();
        verify(tab.title).removeStyleName(SOME_TEXT);
    }

    @Test
    public void warningMarkShouldBeSet() {
        when(resources.partStackCss().lineWarning()).thenReturn(SOME_TEXT);

        tab.setWarningMark(true);

        verify(resources.partStackCss()).lineWarning();
        verify(tab.title).addStyleName(SOME_TEXT);
    }

    @Test
    public void warningMarkShouldNotBeSet() {
        when(resources.partStackCss().lineWarning()).thenReturn(SOME_TEXT);

        tab.setWarningMark(false);

        verify(resources.partStackCss()).lineWarning();
        verify(tab.title).removeStyleName(SOME_TEXT);
    }

    @Test
    public void onTabShouldBeClicked() {
        tab.onClick(event);

        verify(delegate).onTabClicked(tab);
    }

    @Test
    public void onCloseButtonShouldBeClicked() {
        tab.onCloseButtonClicked(event);

        verify(delegate).onTabClose(tab);
    }

    @Test
    public void equalsAndHashCodeShouldBeTested() {
        EditorTab tab1 = new EditorTabWidget(resources, icon, SOME_TEXT);

        assertEquals(tab1, tab);
        assertEquals(tab1.hashCode(), tab.hashCode());
    }
}