/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.core;

import com.google.gwtmockito.GwtMockitoTestRunner;
import com.google.web.bindery.event.shared.Event;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.core.rest.shared.dto.ServiceError;
import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.Constants;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.project.shared.dto.SourceEstimation;
import org.eclipse.che.ide.api.event.ConfigureProjectEvent;
import org.eclipse.che.ide.api.event.OpenProjectEvent;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.core.problemDialog.ProjectProblemDialogPresenter;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.test.GwtReflectionUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testing {@link ProjectProblemResolver} functionality.
 *
 * @author Roman Nikitenko
 */
@RunWith(GwtMockitoTestRunner.class)
public class ProjectProblemResolverTest {
    private static final String PROJECT_PATH = "some/project/path";
    private static final String MAVEN_PROJECT_TYPE = "maven";

    @Captor
    private ArgumentCaptor<AsyncRequestCallback<List<SourceEstimation>>> resolveSourceCallbackCaptor;

    @Captor
    private ArgumentCaptor<AsyncRequestCallback<ProjectDescriptor>> updateProjectCallbackCaptor;

    @Mock
    private ProjectServiceClient          projectServiceClient;
    @Mock
    private DtoUnmarshallerFactory        dtoUnmarshallerFactory;
    @Mock
    private EventBus                      eventBus;
    @Mock
    private DtoFactory                    dtoFactory;
    @Mock
    private NotificationManager           notificationManager;
    @Mock
    private ProjectProblemDialogPresenter problemDialogPresenter;
    @Mock
    private ProjectDescriptor             projectDescriptor;
    @InjectMocks
    private ProjectProblemResolver        resolver;

    @Test
    public void testOnResolveWhenRequestIsSuccess() throws Exception {
        List<SourceEstimation> list = new ArrayList<>(1);
        SourceEstimation mavenSourceEstimation = mock(SourceEstimation.class);
        SourceEstimation phpSourceEstimation = mock(SourceEstimation.class);
        list.add(mavenSourceEstimation);
        list.add(phpSourceEstimation);
        when(projectDescriptor.getPath()).thenReturn(PROJECT_PATH);

        resolver.resolve(projectDescriptor);

        verify(projectServiceClient).resolveSources(anyString(), resolveSourceCallbackCaptor.capture());
        AsyncRequestCallback<List<SourceEstimation>> resolveSourceCallback = resolveSourceCallbackCaptor.getValue();
        GwtReflectionUtils.callOnSuccess(resolveSourceCallback, list);

        verify(dtoUnmarshallerFactory).newListUnmarshaller(anyObject());
        verify(problemDialogPresenter).showDialog(eq(list), eq(resolver));
    }

    @Test
    public void testOnResolveWhenRequestIsFailure() throws Exception {
        when(projectDescriptor.getPath()).thenReturn(PROJECT_PATH);

        resolver.resolve(projectDescriptor);

        verify(projectServiceClient).resolveSources(anyString(), resolveSourceCallbackCaptor.capture());
        AsyncRequestCallback<List<SourceEstimation>> resolveSourceCallback = resolveSourceCallbackCaptor.getValue();
        GwtReflectionUtils.callOnFailure(resolveSourceCallback, mock(Throwable.class));

        verify(dtoUnmarshallerFactory).newListUnmarshaller(anyObject());
        verify(problemDialogPresenter).showDialog(isNull(List.class), eq(resolver));
    }

    @Test
    public void testOnConfigureWhenSourceEstimationIsNotNull() throws Exception {
        SourceEstimation sourceEstimation = mock(SourceEstimation.class);

        resolver.resolve(projectDescriptor);
        resolver.onConfigure(sourceEstimation);

        verify(projectDescriptor).setType(anyString());
        verify(projectDescriptor).setAttributes(anyMap());
        verify(eventBus).fireEvent(Matchers.<Event<ConfigureProjectEvent>> anyObject());
    }

    @Test
    public void testOnConfigureWhenSourceEstimationIsNull() throws Exception {
        resolver.resolve(projectDescriptor);
        resolver.onConfigure(null);

        verify(projectDescriptor).setType(eq(Constants.BLANK_ID));
        verify(projectDescriptor, never()).setAttributes(anyMap());
        verify(eventBus).fireEvent(Matchers.<Event<ConfigureProjectEvent>>anyObject());
    }

    @Test
    public void testOnOpenAsIs() throws Exception {
        resolver.resolve(projectDescriptor);
        resolver.onOpenAsIs();

        verify(projectDescriptor).setType(eq(Constants.BLANK_ID));
        verify(projectDescriptor, never()).setAttributes(anyMap());
        verify(eventBus, never()).fireEvent(Matchers.<Event<ConfigureProjectEvent>> anyObject());
        verify(projectServiceClient).updateProject(anyString(), (ProjectDescriptor)anyObject(), anyObject());
    }

    @Test
    public void testOnOpenAsIsWhenUpdateProjectIsSuccess() throws Exception {
        resolver.resolve(projectDescriptor);
        resolver.onOpenAsIs();

        verify(projectServiceClient).updateProject(anyString(), (ProjectDescriptor)anyObject(), updateProjectCallbackCaptor.capture());
        AsyncRequestCallback<ProjectDescriptor> updateProjectCallback = updateProjectCallbackCaptor.getValue();
        GwtReflectionUtils.callOnSuccess(updateProjectCallback, projectDescriptor);

        verify(projectDescriptor).setType(eq(Constants.BLANK_ID));
        verify(projectDescriptor, never()).setAttributes(anyMap());
        verify(eventBus).fireEvent(Matchers.<Event<OpenProjectEvent>>anyObject());
        verify(notificationManager, never()).showError(anyString());
    }

    @Test
    public void testOnOpenAsIsWhenUpdateProjectIsFailure() throws Exception {
        when(dtoFactory.createDtoFromJson(anyString(), anyObject())).thenReturn(mock(ServiceError.class));

        resolver.resolve(projectDescriptor);
        resolver.onOpenAsIs();

        verify(projectServiceClient).updateProject(anyString(), (ProjectDescriptor)anyObject(), updateProjectCallbackCaptor.capture());
        AsyncRequestCallback<ProjectDescriptor> updateProjectCallback = updateProjectCallbackCaptor.getValue();
        GwtReflectionUtils.callOnFailure(updateProjectCallback, mock(Throwable.class));

        verify(projectDescriptor).setType(eq(Constants.BLANK_ID));
        verify(projectDescriptor, never()).setAttributes(anyMap());
        verify(eventBus, never()).fireEvent(Matchers.<Event<OpenProjectEvent>> anyObject());
        verify(notificationManager).showError(anyString());
    }

    @Test
    public void testOnOpenAs() throws Exception {
        SourceEstimation sourceEstimation = mock(SourceEstimation.class);
        when(sourceEstimation.getType()).thenReturn(MAVEN_PROJECT_TYPE);

        resolver.resolve(projectDescriptor);
        resolver.onOpenAs(sourceEstimation);

        verify(sourceEstimation).getType();
        verify(sourceEstimation).getAttributes();
        verify(projectDescriptor).setAttributes(anyMap());
        verify(projectDescriptor).setType(eq(MAVEN_PROJECT_TYPE));
        verify(eventBus, never()).fireEvent(Matchers.<Event<ConfigureProjectEvent>> anyObject());
        verify(projectServiceClient).updateProject(anyString(), (ProjectDescriptor)anyObject(), anyObject());
    }

    @Test
    public void testOnOpenAsWhenUpdateProjectIsSuccess() throws Exception {
        SourceEstimation sourceEstimation = mock(SourceEstimation.class);
        when(sourceEstimation.getType()).thenReturn(MAVEN_PROJECT_TYPE);

        resolver.resolve(projectDescriptor);
        resolver.onOpenAs(sourceEstimation);

        verify(projectServiceClient).updateProject(anyString(), (ProjectDescriptor)anyObject(), updateProjectCallbackCaptor.capture());
        AsyncRequestCallback<ProjectDescriptor> updateProjectCallback = updateProjectCallbackCaptor.getValue();
        GwtReflectionUtils.callOnSuccess(updateProjectCallback, projectDescriptor);

        verify(projectDescriptor).setType(eq(MAVEN_PROJECT_TYPE));
        verify(projectDescriptor).setAttributes(anyMap());
        verify(eventBus).fireEvent(Matchers.<Event<OpenProjectEvent>>anyObject());
        verify(notificationManager, never()).showError(anyString());
    }

    @Test
    public void testOnOpenAsWhenUpdateProjectIsFailure() throws Exception {
        SourceEstimation sourceEstimation = mock(SourceEstimation.class);
        when(sourceEstimation.getType()).thenReturn(MAVEN_PROJECT_TYPE);
        when(dtoFactory.createDtoFromJson(anyString(), anyObject())).thenReturn(mock(ServiceError.class));

        resolver.resolve(projectDescriptor);
        resolver.onOpenAs(sourceEstimation);

        verify(projectServiceClient).updateProject(anyString(), (ProjectDescriptor)anyObject(), updateProjectCallbackCaptor.capture());
        AsyncRequestCallback<ProjectDescriptor> updateProjectCallback = updateProjectCallbackCaptor.getValue();
        GwtReflectionUtils.callOnFailure(updateProjectCallback, mock(Throwable.class));

        verify(projectDescriptor).setType(eq(MAVEN_PROJECT_TYPE));
        verify(projectDescriptor).setAttributes(anyMap());
        verify(eventBus, never()).fireEvent(Matchers.<Event<OpenProjectEvent>> anyObject());
        verify(notificationManager).showError(anyString());
    }
}
