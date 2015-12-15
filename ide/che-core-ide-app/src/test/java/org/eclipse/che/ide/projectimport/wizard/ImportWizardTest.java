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
package org.eclipse.che.ide.projectimport.wizard;

import com.google.web.bindery.event.shared.Event;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.gwt.client.ProjectTypeServiceClient;
import org.eclipse.che.api.project.shared.dto.ProjectTypeDefinition;
import org.eclipse.che.api.project.shared.dto.SourceEstimation;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.vfs.gwt.client.VfsServiceClient;
import org.eclipse.che.api.vfs.shared.dto.Item;
import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;
import org.eclipse.che.api.core.model.workspace.ProjectProblem;
import org.eclipse.che.api.workspace.shared.dto.SourceStorageDto;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.api.event.ConfigureProjectEvent;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.project.wizard.ImportProjectNotificationSubscriber;
import org.eclipse.che.ide.api.wizard.Wizard;
import org.eclipse.che.ide.commons.exception.ServerException;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.websocket.rest.RequestCallback;
import org.eclipse.che.test.GwtReflectionUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testing {@link ImportWizard}.
 *
 * @author Artem Zatsarynnyi
 */
@RunWith(MockitoJUnitRunner.class)
public class ImportWizardTest {
    private static final String PROJECT_NAME = "project1";

    @Captor
    private ArgumentCaptor<AsyncRequestCallback<Item>>                   callbackCaptorForItem;
    @Captor
    private ArgumentCaptor<RequestCallback<Void>>                        callbackCaptorForProject;
    @Captor
    private ArgumentCaptor<AsyncRequestCallback<Void>>                   callbackCaptorForVoid;
    @Captor
    private ArgumentCaptor<AsyncRequestCallback<Item>>                   callbackCaptorForItemReference;
    @Captor
    private ArgumentCaptor<AsyncRequestCallback<ProjectConfigDto>>       asyncDescriptorCaptor;
    @Captor
    private ArgumentCaptor<AsyncRequestCallback<List<SourceEstimation>>> estimationCaptor;
    @Captor
    private ArgumentCaptor<Operation<ProjectTypeDefinition>>             typeDefinitionCaptor;

    @Mock
    private ProjectServiceClient                projectServiceClient;
    @Mock
    private Promise<ProjectTypeDefinition>      definitionPromise;
    @Mock
    private SourceEstimation                    estimation;
    @Mock
    private ProjectTypeDefinition               projectTypeDefinition;
    @Mock
    private ProjectConfigDto                    projectConfig;
    @Mock
    private ProjectTypeServiceClient            projectTypeServiceClient;
    @Mock
    private ProjectConfigDto                    dataObject;
    @Mock
    private VfsServiceClient                    vfsServiceClient;
    @Mock
    private DtoUnmarshallerFactory              dtoUnmarshallerFactory;
    @Mock
    private DtoFactory                          dtoFactory;
    @Mock
    private EventBus                            eventBus;
    @Mock
    private SourceStorageDto                    source;
    @Mock
    private CoreLocalizationConstant            localizationConstant;
    @Mock
    private ImportProjectNotificationSubscriber importProjectNotificationSubscriber;
    @Mock
    private NotificationManager                 notificationManager;

    @Mock
    private Wizard.CompleteCallback completeCallback;

    @InjectMocks
    private ImportWizard wizard;

    @Before
    public void setUp() {
        when(projectConfig.getName()).thenReturn(PROJECT_NAME);
        when(projectConfig.getSource()).thenReturn(source);
        when(dataObject.getName()).thenReturn(PROJECT_NAME);
        when(dataObject.getSource()).thenReturn(source);
    }

    @Test
    public void shouldInvokeCallbackWhenFolderAlreadyExists() throws Exception {
        wizard.complete(completeCallback);

        verify(vfsServiceClient).getItemByPath(eq(PROJECT_NAME), callbackCaptorForItem.capture());

        AsyncRequestCallback<Item> callback = callbackCaptorForItem.getValue();
        GwtReflectionUtils.callOnSuccess(callback, mock(Item.class));

        verify(completeCallback).onFailure(any(Throwable.class));
    }

    @Test
    public void shouldImportAndOpenProject() throws Exception {
        when(projectTypeDefinition.getPrimaryable()).thenReturn(true);
        when(projectTypeServiceClient.getProjectType(anyString())).thenReturn(definitionPromise);

        wizard.complete(completeCallback);

        verify(vfsServiceClient).getItemByPath(eq(PROJECT_NAME), callbackCaptorForItem.capture());

        callOnSuccessUpdateProject(projectConfig);

        verify(eventBus).fireEvent(Matchers.<Event<Object>>anyObject());
        verify(completeCallback).onCompleted();
    }

    @Test
    public void shouldImportAndOpenProjectForConfiguring() throws Exception {
        ProjectProblem problem = mock(ProjectProblem.class);

        when(projectTypeDefinition.getPrimaryable()).thenReturn(true);
        when(projectConfig.getProblems()).thenReturn(Arrays.asList(problem));
        when(projectTypeServiceClient.getProjectType(anyString())).thenReturn(definitionPromise);

        wizard.complete(completeCallback);

        verify(vfsServiceClient).getItemByPath(eq(PROJECT_NAME), callbackCaptorForItem.capture());

        callOnSuccessUpdateProject(projectConfig);

        //first time method is called for creat project
        verify(eventBus, times(2)).fireEvent(Matchers.<ConfigureProjectEvent>anyObject());
    }

    private void callOnSuccessUpdateProject(ProjectConfigDto projectConfig) throws Exception {
        ServerException throwable = mock(ServerException.class);

        when(throwable.getHTTPStatus()).thenReturn(404);

        AsyncRequestCallback<Item> itemCallback = callbackCaptorForItem.getValue();
        GwtReflectionUtils.callOnFailure(itemCallback, throwable);

        verify(projectServiceClient).importProject(eq(PROJECT_NAME), eq(false), eq(source), callbackCaptorForProject.capture());
        GwtReflectionUtils.callOnSuccessVoidParameter(callbackCaptorForProject.getValue());

        verify(projectServiceClient).resolveSources(anyString(), estimationCaptor.capture());
        GwtReflectionUtils.callOnSuccess(estimationCaptor.getValue(), Arrays.asList(estimation));

        verify(definitionPromise).then(typeDefinitionCaptor.capture());
        typeDefinitionCaptor.getValue().apply(projectTypeDefinition);

        verify(projectServiceClient).updateProject(anyString(), Matchers.<ProjectConfigDto>anyObject(), asyncDescriptorCaptor.capture());
        GwtReflectionUtils.callOnSuccess(asyncDescriptorCaptor.getValue(), projectConfig);
    }
}
