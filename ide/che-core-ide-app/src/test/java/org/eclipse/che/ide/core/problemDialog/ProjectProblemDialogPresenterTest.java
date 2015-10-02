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
package org.eclipse.che.ide.core.problemDialog;

import com.google.gwtmockito.GwtMockitoTestRunner;

import org.eclipse.che.api.project.shared.dto.ProjectTypeDefinition;
import org.eclipse.che.api.project.shared.dto.SourceEstimation;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.api.project.type.ProjectTypeRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNotNull;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testing {@link ProjectProblemDialogPresenter} functionality.
 *
 * @author Roman Nikitenko
 */
@RunWith(GwtMockitoTestRunner.class)
public class ProjectProblemDialogPresenterTest {
    private static final String MAVEN_PROJECT_TYPE = "maven";
    private static final String PHP_PROJECT_TYPE   = "php";

    @Mock
    private ProjectProblemDialogCallback  callback;
    @Mock
    private ProjectTypeRegistry           projectTypeRegistry;
    @Mock
    private ProjectProblemDialogView      view;
    @Mock
    private CoreLocalizationConstant      localizedConstant;
    @InjectMocks
    private ProjectProblemDialogPresenter presenter;


    @Test
    public void setCallbackTest() throws Exception {

        presenter.showDialog(null, callback);

        assertNotNull(presenter.callback);
    }

    @Test
    public void showDialogWhenProjectTypesListIsNull() throws Exception {

        presenter.showDialog(null, callback);

        verify(view).showDialog(isNull(List.class));
        verify(projectTypeRegistry, never()).getProjectType(anyString());
    }

    @Test
    public void showDialogWhenProjectTypeIsNotPrimaryable() throws Exception {
        List<SourceEstimation> list = new ArrayList<>(1);
        SourceEstimation sourceEstimation = mock(SourceEstimation.class);
        list.add(sourceEstimation);
        when(sourceEstimation.getType()).thenReturn(MAVEN_PROJECT_TYPE);

        presenter.showDialog(list, callback);

        verify(view).showDialog(isNotNull(List.class));
        assertTrue(presenter.estimatedTypes.isEmpty());
        verify(projectTypeRegistry).getProjectType(eq(MAVEN_PROJECT_TYPE));
    }

    @Test
    public void showDialogWhenProjectTypeIsPrimaryable() throws Exception {
        List<SourceEstimation> list = new ArrayList<>(1);
        SourceEstimation sourceEstimation = mock(SourceEstimation.class);
        list.add(sourceEstimation);
        when(sourceEstimation.getType()).thenReturn(MAVEN_PROJECT_TYPE);
        when(sourceEstimation.isPrimaryable()).thenReturn(true);
        when(projectTypeRegistry.getProjectType(MAVEN_PROJECT_TYPE)).thenReturn(mock(ProjectTypeDefinition.class));

        presenter.showDialog(list, callback);

        verify(projectTypeRegistry).getProjectType(eq(MAVEN_PROJECT_TYPE));
        verify(view).showDialog(isNotNull(List.class));
        assertFalse(presenter.estimatedTypes.isEmpty());
        assertEquals(1, presenter.estimatedTypes.size());
    }

    @Test
    public void shouldCallCallbackOnOpenAsIs() throws Exception {
        presenter.showDialog(null, callback);
        presenter.onOpenAsIs();

        verify(view).hide();
        verify(callback).onOpenAsIs();
    }

    @Test
    public void shouldCallCallbackOnOpenAsWhenProjectTypesListIsNotNull() throws Exception {
        List<SourceEstimation> list = new ArrayList<>(1);
        SourceEstimation mavenSourceEstimation = mock(SourceEstimation.class);
        list.add(mavenSourceEstimation);
        when(mavenSourceEstimation.getType()).thenReturn(MAVEN_PROJECT_TYPE);
        when(mavenSourceEstimation.isPrimaryable()).thenReturn(true);
        when(projectTypeRegistry.getProjectType(MAVEN_PROJECT_TYPE)).thenReturn(mock(ProjectTypeDefinition.class));
        SourceEstimation phpSourceEstimation = mock(SourceEstimation.class);
        list.add(phpSourceEstimation);
        when(phpSourceEstimation.getType()).thenReturn(PHP_PROJECT_TYPE);
        when(phpSourceEstimation.isPrimaryable()).thenReturn(true);
        when(projectTypeRegistry.getProjectType(PHP_PROJECT_TYPE)).thenReturn(mock(ProjectTypeDefinition.class));
        when(view.getSelectedTypeIndex()).thenReturn(1);

        presenter.showDialog(list, callback);
        presenter.onOpenAs();

        verify(view).hide();
        verify(callback).onOpenAs(eq(phpSourceEstimation));
    }

    @Test
    public void shouldCallCallbackOnConfigure() throws Exception {
        List<SourceEstimation> list = new ArrayList<>(1);
        SourceEstimation mavenSourceEstimation = mock(SourceEstimation.class);
        list.add(mavenSourceEstimation);
        when(mavenSourceEstimation.getType()).thenReturn(MAVEN_PROJECT_TYPE);
        when(mavenSourceEstimation.isPrimaryable()).thenReturn(true);
        when(projectTypeRegistry.getProjectType(MAVEN_PROJECT_TYPE)).thenReturn(mock(ProjectTypeDefinition.class));

        presenter.showDialog(list, callback);
        presenter.onConfigure();

        verify(view).hide();
        verify(callback).onConfigure(eq(mavenSourceEstimation));
    }

    @Test
    public void onEnterClickedTest() throws Exception {
        List<SourceEstimation> list = new ArrayList<>(1);
        SourceEstimation mavenSourceEstimation = mock(SourceEstimation.class);
        list.add(mavenSourceEstimation);
        when(mavenSourceEstimation.getType()).thenReturn(MAVEN_PROJECT_TYPE);
        when(mavenSourceEstimation.isPrimaryable()).thenReturn(true);
        when(projectTypeRegistry.getProjectType(MAVEN_PROJECT_TYPE)).thenReturn(mock(ProjectTypeDefinition.class));

        presenter.showDialog(list, callback);
        presenter.onConfigure();

        verify(view).hide();
        verify(callback).onConfigure(eq(mavenSourceEstimation));
    }

    @Test
    public void onSelectedTypeChangedTest() throws Exception {
        presenter.onSelectedTypeChanged(MAVEN_PROJECT_TYPE);

        verify(view).setOpenAsButtonTitle(anyString());
        verify(localizedConstant).projectProblemPressingButtonsMessage(eq(MAVEN_PROJECT_TYPE));
        verify(view).setMessage(anyString());
    }
}
