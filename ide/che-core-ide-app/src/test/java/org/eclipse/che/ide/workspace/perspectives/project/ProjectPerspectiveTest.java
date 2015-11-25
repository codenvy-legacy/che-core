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
package org.eclipse.che.ide.workspace.perspectives.project;

import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwtmockito.GwtMockitoTestRunner;

import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.parts.EditorPartStack;
import org.eclipse.che.ide.api.parts.PartStackView;
import org.eclipse.che.ide.api.parts.ProjectExplorerPart;
import org.eclipse.che.ide.part.PartStackPresenter;
import org.eclipse.che.ide.workspace.PartStackPresenterFactory;
import org.eclipse.che.ide.workspace.PartStackViewFactory;
import org.eclipse.che.ide.workspace.WorkBenchControllerFactory;
import org.eclipse.che.ide.workspace.WorkBenchPartController;
import org.eclipse.che.ide.workspace.perspectives.general.PerspectiveViewImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Dmitry Shnurenko
 */
@RunWith(GwtMockitoTestRunner.class)
public class ProjectPerspectiveTest {

    //constructor mocks
    @Mock
    private PerspectiveViewImpl        view;
    @Mock
    private EditorPartStack            editorPartStackPresenter;
    @Mock
    private PartStackViewFactory       partViewFactory;
    @Mock
    private WorkBenchControllerFactory controllerFactory;
    @Mock
    private PartStackPresenterFactory  stackPresenterFactory;
    @Mock
    private ProjectExplorerPart        projectExplorerPart;
    @Mock
    private NotificationManager        notificationManager;

    //additional mocks
    @Mock
    private FlowPanel               panel;
    @Mock
    private SplitLayoutPanel        layoutPanel;
    @Mock
    private SimplePanel             simplePanel;
    @Mock
    private WorkBenchPartController workBenchController;
    @Mock
    private PartStackView           partStackView;
    @Mock
    private PartStackPresenter      partStackPresenter;
    @Mock
    private AcceptsOneWidget        container;

    private ProjectPerspective perspective;

    @Before
    public void setUp() {

        when(view.getLeftPanel()).thenReturn(panel);
        when(view.getRightPanel()).thenReturn(panel);
        when(view.getBottomPanel()).thenReturn(panel);

        when(view.getSplitPanel()).thenReturn(layoutPanel);

        when(view.getNavigationPanel()).thenReturn(simplePanel);
        when(view.getInformationPanel()).thenReturn(simplePanel);
        when(view.getToolPanel()).thenReturn(simplePanel);

        when(controllerFactory.createController(Matchers.<SplitLayoutPanel>anyObject(),
                                                Matchers.<SimplePanel>anyObject())).thenReturn(workBenchController);

        when(partViewFactory.create(Matchers.<PartStackView.TabPosition>anyObject(),
                                    Matchers.<FlowPanel>anyObject())).thenReturn(partStackView);

        when(stackPresenterFactory.create(Matchers.<PartStackView>anyObject(),
                                          Matchers.<WorkBenchPartController>anyObject())).thenReturn(partStackPresenter);


        perspective = new ProjectPerspective(view,
                                             editorPartStackPresenter,
                                             stackPresenterFactory,
                                             partViewFactory,
                                             controllerFactory,
                                             projectExplorerPart,
                                             notificationManager);
    }

    @Test
    public void constructorShouldBeVerified() {
        when(partStackPresenter.containsPart(projectExplorerPart)).thenReturn(true);

        perspective = new ProjectPerspective(view,
                                             editorPartStackPresenter,
                                             stackPresenterFactory,
                                             partViewFactory,
                                             controllerFactory,
                                             projectExplorerPart,
                                             notificationManager);

        verify(partStackPresenter, times(2)).addPart(notificationManager, null);
        verify(partStackPresenter).addPart(projectExplorerPart, null);

        verify(partStackPresenter).setActivePart(projectExplorerPart);
    }

    @Test
    public void perspectiveShouldBeDisplayed() {
        perspective.go(container);

        verify(view).getEditorPanel();
        verify(view, times(2)).getNavigationPanel();
        verify(view, times(2)).getToolPanel();
        verify(view, times(2)).getInformationPanel();

        verify(partStackPresenter, times(3)).go(simplePanel);
        verify(partStackPresenter).openPreviousActivePart();
        verify(container).setWidget(view);
    }
}