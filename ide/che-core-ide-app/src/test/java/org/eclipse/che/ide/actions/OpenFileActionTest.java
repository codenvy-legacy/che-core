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

import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.web.bindery.event.shared.EventBus;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwtmockito.GwtMockitoTestRunner;
import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.event.FileEvent;
import org.eclipse.che.ide.api.notification.Notification;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.project.tree.TreeNode;
import org.eclipse.che.ide.api.project.tree.TreeStructure;
import org.eclipse.che.ide.api.project.tree.generic.FileNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Map;

import static org.eclipse.che.ide.actions.OpenFileAction.FILE_PARAM_ID;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Alexander Andrienko
 */
@RunWith(GwtMockitoTestRunner.class)
public class OpenFileActionTest {

    private static final String TEXT = "some text";

    @Mock
    private EventBus eventBus;
    @Mock
    private AppContext appContext;
    @Mock
    private NotificationManager notificationManager;
    @Mock
    private CoreLocalizationConstant localization;
    @Mock
    private ProjectServiceClient projectServiceClient;

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
    private FileNode fileNode;
    @Mock
    private Throwable throwable;

    @Captor
    private ArgumentCaptor<AsyncCallback<TreeNode<?>>> treeNodeArgumentCaptor;

    @InjectMocks
    private OpenFileAction openFileAction;

    @Before
    public void setUp() {
        when(parameters.get(FILE_PARAM_ID)).thenReturn(TEXT);
        when(appContext.getCurrentProject()).thenReturn(currentProject);
        when(currentProject.getRootProject()).thenReturn(activeProject);
        when(event.getParameters()).thenReturn(parameters);
        when(currentProject.getCurrentTree()).thenReturn(treeStructure);
        when(activeProject.getPath()).thenReturn(TEXT);
    }

    @Test
    public void actionShouldBeFailedBecauseCurrentProjectIsNull() {
        when(appContext.getCurrentProject()).thenReturn(null);
        openFileAction.actionPerformed(event);

        verify(appContext).getCurrentProject();

        verifyNoMoreInteractions(currentProject);
    }

    @Test
    public void actionShouldBeFailedBecauseRootProjectIsNull() {
        when(currentProject.getRootProject()).thenReturn(null);
        openFileAction.actionPerformed(event);

        verify(appContext, times(2)).getCurrentProject();
        verify(currentProject).getRootProject();

        verifyNoMoreInteractions(currentProject);
    }

    @Test
    public void actionShouldBeFailedBecauseActionParametersIsNull() {
        when(event.getParameters()).thenReturn(null);
        openFileAction.actionPerformed(event);

        verify(appContext, times(3)).getCurrentProject();
        verify(currentProject, times(2)).getRootProject();

        verify(localization).canNotOpenFileWithoutParams();
        verifyNoMoreInteractions(activeProject);
    }

    @Test
    public void actionShouldBeFailedBecauseActionSelectNodeParameterIsNull() {
        when(parameters.get(FILE_PARAM_ID)).thenReturn(null);

        openFileAction.actionPerformed(event);

        verify(appContext, times(3)).getCurrentProject();
        verify(currentProject, times(2)).getRootProject();

        verify(event, times(2)).getParameters();
        verify(parameters).get(FILE_PARAM_ID);
        verify(localization).fileToOpenIsNotSpecified();
        verifyNoMoreInteractions(activeProject);
    }

    @Test
    public void actionShouldBePerformed() {
        openFileAction.actionPerformed(event);

        verify(appContext, times(4)).getCurrentProject();
        verify(currentProject, times(2)).getRootProject();

        verify(event, times(2)).getParameters();
        verify(parameters).get(FILE_PARAM_ID);

        verify(activeProject).getPath();

        verify(currentProject).getCurrentTree();
        verify(treeStructure).getNodeByPath(eq(TEXT + "/" + TEXT), treeNodeArgumentCaptor.capture());
        treeNodeArgumentCaptor.getValue().onSuccess(fileNode);

        verify(eventBus).fireEvent(any(OpenEvent.class));
    }

    @Test
    public void fileShouldNotBeOpenedBecauseNodeIsNotInstanceOfFileNode() {
        openFileAction.actionPerformed(event);

        verify(appContext, times(4)).getCurrentProject();
        verify(currentProject, times(2)).getRootProject();

        verify(event, times(2)).getParameters();
        verify(parameters).get(FILE_PARAM_ID);

        verify(activeProject).getPath();

        verify(currentProject).getCurrentTree();
        verify(treeStructure).getNodeByPath(eq(TEXT + "/" + TEXT), treeNodeArgumentCaptor.capture());
        treeNodeArgumentCaptor.getValue().onSuccess(treeNode);

        verify(eventBus, never()).fireEvent(any(FileEvent.class));
    }

    @Test
    public void actionShouldBeFailedBecauseNodeIsUnavailable() {
        when(parameters.get(FILE_PARAM_ID)).thenReturn("/" + TEXT);

        openFileAction.actionPerformed(event);

        verify(appContext, times(4)).getCurrentProject();
        verify(currentProject, times(2)).getRootProject();

        verify(event, times(2)).getParameters();
        verify(parameters).get(FILE_PARAM_ID);

        verify(activeProject).getPath();

        verify(currentProject).getCurrentTree();
        verify(treeStructure).getNodeByPath(eq(TEXT + "/" + TEXT), treeNodeArgumentCaptor.capture());
        treeNodeArgumentCaptor.getValue().onFailure(throwable);

        verify(notificationManager).showNotification(any(Notification.class));
        verify(localization).unableOpenResource(TEXT + "/" + TEXT);
    }
}
