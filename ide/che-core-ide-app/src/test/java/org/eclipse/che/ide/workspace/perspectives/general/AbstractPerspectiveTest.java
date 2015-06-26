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
package org.eclipse.che.ide.workspace.perspectives.general;

import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwtmockito.GwtMockitoTestRunner;

import org.eclipse.che.ide.api.constraints.Constraints;
import org.eclipse.che.ide.api.parts.PartPresenter;
import org.eclipse.che.ide.api.parts.PartStack;
import org.eclipse.che.ide.api.parts.PartStackView;
import org.eclipse.che.ide.api.parts.PartStackView.TabPosition;
import org.eclipse.che.ide.part.PartStackPresenter;
import org.eclipse.che.ide.workspace.PartStackPresenterFactory;
import org.eclipse.che.ide.workspace.PartStackViewFactory;
import org.eclipse.che.ide.workspace.WorkBenchControllerFactory;
import org.eclipse.che.ide.workspace.WorkBenchPartController;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;

import javax.annotation.Nonnull;
import javax.ws.rs.NotSupportedException;

import static junit.framework.Assert.assertSame;
import static org.eclipse.che.ide.api.parts.PartStackType.EDITING;
import static org.eclipse.che.ide.api.parts.PartStackType.INFORMATION;
import static org.eclipse.che.ide.api.parts.PartStackView.TabPosition.BELOW;
import static org.eclipse.che.ide.api.parts.PartStackView.TabPosition.LEFT;
import static org.eclipse.che.ide.api.parts.PartStackView.TabPosition.RIGHT;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Dmitry Shnurenko
 */
@RunWith(GwtMockitoTestRunner.class)
public class AbstractPerspectiveTest {

    private final static String SOME_TEXT = "someText";

    //constructor mocks
    @Mock
    private PerspectiveViewImpl        view;
    @Mock
    private PartStackPresenterFactory  stackPresenterFactory;
    @Mock
    private PartStackViewFactory       partStackViewFactory;
    @Mock
    private WorkBenchControllerFactory controllerFactory;

    //additional mocks
    @Mock
    private FlowPanel               panel;
    @Mock
    private SplitLayoutPanel        layoutPanel;
    @Mock
    private SimplePanel             simplePanel;
    @Mock
    private PartStackView           partStackView;
    @Mock
    private PartStackPresenter      partStackPresenter;
    @Mock
    private WorkBenchPartController workBenchController;
    @Mock
    private PartPresenter           partPresenter;
    @Mock
    private Constraints             constraints;
    @Mock
    private PartPresenter           activePart;

    private AbstractPerspective perspective;

    @Before
    public void setUp() throws Exception {
        when(view.getLeftPanel()).thenReturn(panel);
        when(view.getRightPanel()).thenReturn(panel);
        when(view.getBottomPanel()).thenReturn(panel);

        when(view.getSplitPanel()).thenReturn(layoutPanel);

        when(view.getNavigationPanel()).thenReturn(simplePanel);
        when(view.getInformationPanel()).thenReturn(simplePanel);
        when(view.getToolPanel()).thenReturn(simplePanel);

        when(controllerFactory.createController(Matchers.<SplitLayoutPanel>anyObject(),
                                                Matchers.<SimplePanel>anyObject())).thenReturn(workBenchController);

        when(partStackViewFactory.create(Matchers.<TabPosition>anyObject(),
                                         Matchers.<FlowPanel>anyObject())).thenReturn(partStackView);

        when(stackPresenterFactory.create(Matchers.<PartStackView>anyObject(),
                                          Matchers.<WorkBenchPartController>anyObject())).thenReturn(partStackPresenter);

        perspective = new DummyPerspective(view, stackPresenterFactory, partStackViewFactory, controllerFactory);
    }

    @Test
    public void constructorShouldBeVerified() {
        verify(view).getLeftPanel();
        verify(view).getBottomPanel();
        verify(view).getRightPanel();

        verify(partStackViewFactory).create(LEFT, panel);
        verify(partStackViewFactory).create(BELOW, panel);
        verify(partStackViewFactory).create(RIGHT, panel);

        verify(view, times(3)).getSplitPanel();
        verify(view).getNavigationPanel();
        verify(controllerFactory, times(3)).createController(layoutPanel, simplePanel);
        verify(stackPresenterFactory, times(3)).create(partStackView, workBenchController);
    }

    @Test
    public void partShouldBeRemoved() {
        when(partStackPresenter.containsPart(partPresenter)).thenReturn(true);

        perspective.removePart(partPresenter);

        verify(partStackPresenter).removePart(partPresenter);
    }

    @Test
    public void partShouldBeHided() {
        when(partStackPresenter.containsPart(partPresenter)).thenReturn(true);

        perspective.hidePart(partPresenter);

        verify(partStackPresenter).hidePart(partPresenter);
    }

    @Test
    public void partShouldBeExpanded() {
        when(partStackPresenter.getActivePart()).thenReturn(partPresenter);

        perspective.expandEditorPart();

//        verify(partStackPresenter, times(4)).hidePart(partPresenter);
    }

    @Test
    public void editorPartShouldBeRestored() {
        when(partStackPresenter.containsPart(partPresenter)).thenReturn(true);
        when(partStackPresenter.getActivePart()).thenReturn(partPresenter);
        perspective.expandEditorPart();
        when(partStackPresenter.getActivePart()).thenReturn(null);

        perspective.restoreEditorPart();

//        verify(partStackPresenter, times(4)).setActivePart(partPresenter);
    }

    @Test
    public void activePartShouldBeSet() {
        when(partStackPresenter.containsPart(partPresenter)).thenReturn(true);

        perspective.setActivePart(partPresenter);

        verify(partStackPresenter).setActivePart(partPresenter);
    }

    @Test
    public void nullShouldBeReturnedWhenPartIsNotFound() {
        PartStack partStack = perspective.findPartStackByPart(partPresenter);

        assertSame(partStack, null);
    }

    @Test
    public void nullShouldBeFound() {
        when(partStackPresenter.containsPart(partPresenter)).thenReturn(true);

        PartStack partStack = perspective.findPartStackByPart(partPresenter);

        assertSame(partStack, partStackPresenter);
    }

    @Test
    public void partShouldBeAddedWithoutConstraints() {
        perspective.openPart(partPresenter, INFORMATION);

        verify(partStackPresenter).addPart(partPresenter, null);
    }

    @Test
    public void partShouldBeAddedWithConstraints() {
        perspective.openPart(partPresenter, INFORMATION, constraints);

        verify(partStackPresenter).addPart(partPresenter, constraints);
    }

    @Test
    public void partStackShouldBeReturned() {
        perspective.openPart(partPresenter, INFORMATION);

        PartStack partStack = perspective.getPartStack(INFORMATION);

        assertSame(partStack, partStackPresenter);
    }


    private class DummyPerspective extends AbstractPerspective {

        private DummyPerspective(@Nonnull PerspectiveViewImpl view,
                                 @Nonnull PartStackPresenterFactory stackPresenterFactory,
                                 @Nonnull PartStackViewFactory partViewFactory,
                                 @Nonnull WorkBenchControllerFactory controllerFactory) {
            super(SOME_TEXT, view, stackPresenterFactory, partViewFactory, controllerFactory);

            partStacks.put(EDITING, partStackPresenter);
        }

        @Override
        public void go(@Nonnull AcceptsOneWidget container) {
            throw new NotSupportedException("This method will be tested in the class which extends AbstractPerspective");
        }
    }
}