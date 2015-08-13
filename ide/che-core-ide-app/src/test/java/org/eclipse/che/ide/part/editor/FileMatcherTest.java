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
package org.eclipse.che.ide.part.editor;

import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Dmitry Shnurenko
 */
@RunWith(MockitoJUnitRunner.class)
public class FileMatcherTest {

    private static final String SOME_TEXT = "someText";

    @Mock
    private AppContext appContext;

    @Mock
    private ProjectDescriptor descriptor;
    @Mock
    private CurrentProject    currentProject;

    @InjectMocks
    private FileMatcher projectManager;

    @Before
    public void setUp() {
        when(appContext.getCurrentProject()).thenReturn(currentProject);
    }

    @Test
    public void fileShouldBeMatchedToProject() {
        projectManager.matchFileToProject(SOME_TEXT, descriptor);

        projectManager.setActualProjectForFile(SOME_TEXT);

        verify(appContext).getCurrentProject();
        verify(currentProject).setProjectDescription(descriptor);
    }

    @Test
    public void actualProjectShouldNotBeSetWhenDescriptorIsNotMatched() {
        projectManager.setActualProjectForFile(SOME_TEXT);

        verify(currentProject, never()).setProjectDescription(Matchers.<ProjectDescriptor>anyObject());
    }

    @Test
    public void actualProjectShouldNotBeSetWhenCurrentProjectIsNull() {
        when(appContext.getCurrentProject()).thenReturn(null);

        projectManager.setActualProjectForFile(SOME_TEXT);

        verify(currentProject, never()).setProjectDescription(Matchers.<ProjectDescriptor>anyObject());
    }

    @Test
    public void matchShouldBeRemoved() {
        projectManager.matchFileToProject(SOME_TEXT, descriptor);
        projectManager.setActualProjectForFile(SOME_TEXT);

        verify(appContext).getCurrentProject();
        verify(currentProject).setProjectDescription(descriptor);
        reset(currentProject);

        projectManager.removeMatch(SOME_TEXT);

        projectManager.setActualProjectForFile(SOME_TEXT);

        verify(currentProject, never()).setProjectDescription(Matchers.<ProjectDescriptor>anyObject());
    }
}