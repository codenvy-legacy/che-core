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
package org.eclipse.che.ide.actions;


import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwtmockito.GwtMockitoTestRunner;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.notification.Notification;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.project.tree.TreeNode;
import org.eclipse.che.ide.api.project.tree.TreeStructure;
import org.eclipse.che.ide.part.projectexplorer.ProjectExplorerPartPresenter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.eclipse.che.ide.actions.OpenNodeAction.NODE_PARAM_ID;

/**
 * @author Andrienko Alexander
 */
@RunWith(GwtMockitoTestRunner.class)
public class OpenNodeActionTest {

    private static final String TEXT         = "some text";
    private static final String PATH_TO_NODE = TEXT + "/" + TEXT;

    @Mock
    private EventBus                     eventBus;
    @Mock
    private AppContext                   appContext;
    @Mock
    private NotificationManager          notificationManager;
    @Mock
    private CoreLocalizationConstant     localization;
    @Mock
    private ProjectExplorerPartPresenter projectExplorerPartPresenter;

    @Mock
    private CurrentProject    currentProject;
    @Mock
    private ProjectDescriptor activeProject;
    @Mock
    private ActionEvent       event;
    @Mock
    private TreeStructure     tree;
    @Mock
    private TreeNode<?>       treeNode;
    @Mock
    private Throwable         throwable;

    @Captor
    private ArgumentCaptor<AsyncCallback<TreeNode<?>>> argumentCaptor;

    @InjectMocks
    private OpenNodeAction openNodeAction;

    @Before
    public void setUp() {
        when(appContext.getCurrentProject()).thenReturn(currentProject);
        when(currentProject.getRootProject()).thenReturn(activeProject);
        when(activeProject.getPath()).thenReturn(TEXT);
        when(currentProject.getCurrentTree()).thenReturn(tree);

        when(localization.canNotOpenNodeWithoutParams()).thenReturn(TEXT);
        when(localization.nodeToOpenIsNotSpecified()).thenReturn(TEXT);

        Map<String, String> params = new HashMap<>();
        params.put(NODE_PARAM_ID, TEXT);

        when(event.getParameters()).thenReturn(params);
    }

    @Test
    public void actionShouldBePerformed() {
        openNodeAction.actionPerformed(event);

        verify(appContext).getCurrentProject();
        verify(currentProject, times(2)).getRootProject();
        verify(activeProject).getPath();
        verify(currentProject).getCurrentTree();
        verify(event, times(2)).getParameters();

        verify(currentProject).getCurrentTree();
        verify(tree).getNodeByPath(eq(PATH_TO_NODE), argumentCaptor.capture());

        argumentCaptor.getValue().onSuccess(treeNode);

        verify(projectExplorerPartPresenter).expandNode(treeNode);
    }

    @Test
    public void actionShouldBePerformedWhenPathToNodeHasFirstSymbolSlash() {
        Map<String, String> params = new HashMap<>();
        params.put(NODE_PARAM_ID, "/" + TEXT);
        when(event.getParameters()).thenReturn(params);

        openNodeAction.actionPerformed(event);

        verify(appContext).getCurrentProject();
        verify(currentProject, times(2)).getRootProject();
        verify(activeProject).getPath();
        verify(currentProject).getCurrentTree();
        verify(event, times(2)).getParameters();

        verify(currentProject).getCurrentTree();
        verify(tree).getNodeByPath(eq(PATH_TO_NODE), argumentCaptor.capture());

        argumentCaptor.getValue().onSuccess(treeNode);

        verify(projectExplorerPartPresenter).expandNode(treeNode);
    }

    @Test
    public void actionShouldBeFailedBecauseNodeIsInaccessible() {
        openNodeAction.actionPerformed(event);

        verify(appContext).getCurrentProject();
        verify(currentProject, times(2)).getRootProject();
        verify(activeProject).getPath();
        verify(currentProject).getCurrentTree();
        verify(event, times(2)).getParameters();

        verify(currentProject).getCurrentTree();
        verify(tree).getNodeByPath(eq(PATH_TO_NODE), argumentCaptor.capture());

        argumentCaptor.getValue().onFailure(throwable);

        verify(localization).unableOpenResource(PATH_TO_NODE);
        verify(notificationManager).showNotification(any(Notification.class));
    }

    @Test
    public void actionShouldBeFailedBecauseCurrentProjectIsNull() {
        when(appContext.getCurrentProject()).thenReturn(null);

        openNodeAction.actionPerformed(event);

        verify(appContext).getCurrentProject();
    }

    @Test
    public void actionShouldBeFailedBecauseRootProjectIsNull() {
        when(currentProject.getRootProject()).thenReturn(null);

        openNodeAction.actionPerformed(event);

        verify(appContext).getCurrentProject();
        verify(currentProject).getRootProject();
    }

    @Test
    public void actionShouldBeFailedBecauseParametersIsNull() {
        when(event.getParameters()).thenReturn(null);

        openNodeAction.actionPerformed(event);

        verify(appContext).getCurrentProject();
        verify(currentProject, times(2)).getRootProject();

        verify(event).getParameters();
        verify(localization).canNotOpenNodeWithoutParams();
    }

    @Test
    public void actionShouldBeFailedBecausePathToNodeIsNull() {
        Map<String, String> params = new HashMap<>();
        params.put(NODE_PARAM_ID, null);
        when(event.getParameters()).thenReturn(params);

        openNodeAction.actionPerformed(event);

        verify(appContext).getCurrentProject();
        verify(currentProject, times(2)).getRootProject();

        verify(event, times(2)).getParameters();
        verify(localization).nodeToOpenIsNotSpecified();
    }

    @Test
    public void actionShouldBeFailedBecausePathToNodeIsEmptyLine() {
        Map<String, String> params = new HashMap<>();
        params.put(NODE_PARAM_ID, "");
        when(event.getParameters()).thenReturn(params);

        openNodeAction.actionPerformed(event);

        verify(appContext).getCurrentProject();
        verify(currentProject, times(2)).getRootProject();

        verify(event, times(2)).getParameters();
        verify(localization).nodeToOpenIsNotSpecified();
    }
}
