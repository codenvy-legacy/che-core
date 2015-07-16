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
package org.eclipse.che.ide.part.widgets.partbutton;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwtmockito.GwtMockitoTestRunner;

import org.eclipse.che.ide.Resources;
import org.eclipse.che.ide.api.parts.PartPresenter;
import org.eclipse.che.ide.part.widgets.partbutton.PartButton.ActionDelegate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.vectomatic.dom.svg.OMSVGSVGElement;
import org.vectomatic.dom.svg.ui.SVGResource;

import static org.eclipse.che.ide.api.parts.PartStackView.TabPosition.BELOW;
import static org.eclipse.che.ide.api.parts.PartStackView.TabPosition.LEFT;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Dmitry Shnurenko
 */
@RunWith(GwtMockitoTestRunner.class)
public class PartButtonWidgetTest {

    private static final String SOME_TEXT = "someText";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Resources resources;

    @Mock
    private PartPresenter   partPresenter;
    @Mock
    private IsWidget        isWidget;
    @Mock
    private SVGResource     svgResource;
    @Mock
    private OMSVGSVGElement svg;
    @Mock
    private ActionDelegate  delegate;

    private PartButtonWidget partButton;

    @Before
    public void setUp() {
        partButton = new PartButtonWidget(resources, SOME_TEXT);
        partButton.setDelegate(delegate);
    }

    @Test
    public void constructorShouldBeVerified() {
        verify(partButton.buttonName).setText(SOME_TEXT);
    }

    @Test
    public void iconShouldBeAdded() {
        when(svgResource.getSvg()).thenReturn(svg);

        partButton.addIcon(svgResource);

        verify(partButton.icon).getElement();
    }

    @Test
    public void partButtonShouldBeUpdated() {
        when(partPresenter.getTitleWidget()).thenReturn(isWidget);

        partButton.update(partPresenter);

        verify(partPresenter).getTitleWidget();
        verify(partButton.icon).add(isWidget);
    }

    @Test
    public void onPartButtonShouldBeClicked() {
        ClickEvent event = mock(ClickEvent.class);

        partButton.onClick(event);

        verify(delegate).onTabClicked(partButton);
    }

    @Test
    public void partShouldBeSelectedInNotBelowPosition() {
        partButton.select();

        verify(resources.partStackCss()).selectedRightOrLeftTab();
    }

    @Test
    public void partShouldBeSelectedInBelowPosition() {
        partButton.setTabPosition(BELOW,1);

        partButton.select();

        verify(resources.partStackCss()).idePartStackToolTabSelected();
    }

    @Test
    public void partShouldNotBeSelectedInNotBelowPosition() {
        partButton.setTabPosition(LEFT, 1);

        partButton.unSelect();

        verify(resources.partStackCss()).selectedRightOrLeftTab();
    }

    @Test
    public void partShouldNotBeSelectedInBelowPosition() {
        partButton.setTabPosition(BELOW, 1);

        partButton.unSelect();

        verify(resources.partStackCss()).idePartStackToolTabSelected();
    }

    @Test
    public void tabPositionShouldBeSetWhenPositionIsLeft() {
        partButton.setTabPosition(LEFT, 1);

        verify(resources.partStackCss()).leftTabs();
    }

    @Test
    public void tabPositionShouldBeSetWhenPositionIsNotLeft() {
        partButton.setTabPosition(BELOW, 1);

        verify(resources.partStackCss()).tabBordersDefault();
    }

}