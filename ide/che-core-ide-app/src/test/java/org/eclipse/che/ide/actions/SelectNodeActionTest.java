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
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.notification.Notification;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.project.tree.TreeNode;
import org.eclipse.che.ide.api.project.tree.TreeStructure;
import org.eclipse.che.ide.part.projectexplorer.ProjectExplorerView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.eclipse.che.ide.actions.SelectNodeAction.SELECT_NODE_PARAM_ID;


/**
 * @author Alexander Andrienko
 */
@RunWith(GwtMockitoTestRunner.class)
public class SelectNodeActionTest {

    private static final String TEXT = "some text";

    @Mock
    private AppContext appContext;
    @Mock
    private NotificationManager notificationManager;
    @Mock
    private CoreLocalizationConstant localization;
    @Mock
    private ProjectExplorerView projectExplorerView;

    @Mock
    private ActionEvent event;
    @Mock
    private CurrentProject currentProject;
    @Mock
    private ProjectDescriptor activeProject;
    @Mock
    private Map<String, String> parameters;
    @Mock
    private TreeStructure treeStructure;
    @Mock
    private TreeNode<?> treeNode;
    @Mock
    private Throwable throwable;

    @Captor
    private ArgumentCaptor<AsyncCallback<TreeNode<?>>> treeNodeArgumentCaptor;

    @InjectMocks
    private SelectNodeAction selectNodeAction;

    @Before
    public void setUp() {
        when(parameters.get(SELECT_NODE_PARAM_ID)).thenReturn(TEXT);
        when(appContext.getCurrentProject()).thenReturn(currentProject);
        when(currentProject.getRootProject()).thenReturn(activeProject);
        when(event.getParameters()).thenReturn(parameters);
        when(currentProject.getCurrentTree()).thenReturn(treeStructure);
        when(activeProject.getPath()).thenReturn(TEXT);
    }

    @Test
    public void actionShouldBeFailedBecauseCurrentProjectIsNull() {
        when(appContext.getCurrentProject()).thenReturn(null);
        selectNodeAction.actionPerformed(event);

        verify(appContext).getCurrentProject();

        verifyNoMoreInteractions(currentProject);
    }

    @Test
    public void actionShouldBeFailedBecauseRootProjectIsNull() {
        when(currentProject.getRootProject()).thenReturn(null);
        selectNodeAction.actionPerformed(event);

        verify(appContext).getCurrentProject();
        verify(currentProject).getRootProject();

        verifyNoMoreInteractions(currentProject);
    }

    @Test
    public void actionShouldBeFailedBecauseActionParametersIsNull() {
        when(event.getParameters()).thenReturn(null);
        selectNodeAction.actionPerformed(event);

        verify(appContext).getCurrentProject();
        verify(currentProject, times(2)).getRootProject();

        verify(localization).canNotSelectNodeWithoutParams();
        verifyNoMoreInteractions(activeProject);
    }

    @Test
    public void actionShouldBeFailedBecauseActionSelectNodeParameterIsNull() {
        when(parameters.get(SELECT_NODE_PARAM_ID)).thenReturn(null);

        selectNodeAction.actionPerformed(event);

        verify(appContext).getCurrentProject();
        verify(currentProject, times(2)).getRootProject();

        verify(event, times(2)).getParameters();
        verify(parameters).get(SELECT_NODE_PARAM_ID);
        verify(localization).nodeToSelectIsNotSpecified();
        verifyNoMoreInteractions(activeProject);
    }

    @Test
    public void actionShouldBeFailedBecauseActionSelectNodeParameterIsEmptyLine() {
        when(parameters.get(SELECT_NODE_PARAM_ID)).thenReturn("");

        selectNodeAction.actionPerformed(event);

        verify(appContext).getCurrentProject();
        verify(currentProject, times(2)).getRootProject();

        verify(event, times(2)).getParameters();
        verify(parameters).get(SELECT_NODE_PARAM_ID);
        verify(localization).nodeToSelectIsNotSpecified();
        verifyNoMoreInteractions(activeProject);
    }

    @Test
    public void actionShouldBePerformed() {
        selectNodeAction.actionPerformed(event);

        verify(appContext).getCurrentProject();
        verify(currentProject, times(2)).getRootProject();

        verify(event, times(2)).getParameters();
        verify(parameters).get(SELECT_NODE_PARAM_ID);

        verify(activeProject).getPath();

        verify(currentProject).getCurrentTree();
        verify(treeStructure).getNodeByPath(eq(TEXT + "/" + TEXT), treeNodeArgumentCaptor.capture());
        treeNodeArgumentCaptor.getValue().onSuccess(treeNode);

        verify(projectExplorerView).selectNode(treeNode);
    }

    @Test
    public void actionShouldBePerformedWhenPathToNodeHasSlash() {
        when(parameters.get(SELECT_NODE_PARAM_ID)).thenReturn("/" + TEXT);

        selectNodeAction.actionPerformed(event);

        verify(appContext).getCurrentProject();
        verify(currentProject, times(2)).getRootProject();

        verify(event, times(2)).getParameters();
        verify(parameters).get(SELECT_NODE_PARAM_ID);

        verify(activeProject).getPath();

        verify(currentProject).getCurrentTree();
        verify(treeStructure).getNodeByPath(eq(TEXT + "/" + TEXT), treeNodeArgumentCaptor.capture());
        treeNodeArgumentCaptor.getValue().onSuccess(treeNode);

        verify(projectExplorerView).selectNode(treeNode);
    }

    @Test
    public void actionShouldBeFailedBecauseNodeIsUnavailable() {
        when(parameters.get(SELECT_NODE_PARAM_ID)).thenReturn("/" + TEXT);

        selectNodeAction.actionPerformed(event);

        verify(appContext).getCurrentProject();
        verify(currentProject, times(2)).getRootProject();

        verify(event, times(2)).getParameters();
        verify(parameters).get(SELECT_NODE_PARAM_ID);

        verify(activeProject).getPath();

        verify(currentProject).getCurrentTree();
        verify(treeStructure).getNodeByPath(eq(TEXT + "/" + TEXT), treeNodeArgumentCaptor.capture());
        treeNodeArgumentCaptor.getValue().onFailure(throwable);

        verify(notificationManager).showNotification(any(Notification.class));
        verify(localization).unableSelectResource(TEXT + "/" + TEXT);
    }
}
