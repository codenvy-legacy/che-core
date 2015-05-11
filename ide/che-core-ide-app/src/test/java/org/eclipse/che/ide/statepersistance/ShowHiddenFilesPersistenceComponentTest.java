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
package org.eclipse.che.ide.statepersistance;

import com.google.gwtmockito.GwtMockitoTestRunner;
import org.eclipse.che.ide.actions.ShowHiddenFilesAction;
import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.project.tree.TreeSettings;
import org.eclipse.che.ide.api.project.tree.TreeStructure;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.statepersistance.dto.ActionDescriptor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.eclipse.che.ide.actions.ShowHiddenFilesAction.SHOW_HIDDEN_FILES_PARAM_ID;

/**
 * @author Alexander Andrienko
 */
@RunWith(GwtMockitoTestRunner.class)
public class ShowHiddenFilesPersistenceComponentTest {

    private static final String TEXT = "some text";

    @Mock
    private AppContext appContext;
    @Mock
    private ActionManager actionManager;
    @Mock
    private ShowHiddenFilesAction showHiddenFilesAction;
    @Mock
    private DtoFactory dtoFactory;

    @Mock
    private CurrentProject currentProject;
    @Mock
    private TreeStructure treeStructure;
    @Mock
    private TreeSettings treeSettings;
    @Mock
    private ActionDescriptor actionDescriptor;

    @InjectMocks
    private ShowHiddenFilesPersistenceComponent persistenceComponent;

    @Before
    public void setUp() {
        when(appContext.getCurrentProject()).thenReturn(currentProject);
        when(currentProject.getCurrentTree()).thenReturn(treeStructure);
        when(treeStructure.getSettings()).thenReturn(treeSettings);
        when(actionManager.getId(showHiddenFilesAction)).thenReturn(SHOW_HIDDEN_FILES_PARAM_ID);
        when(treeSettings.isShowHiddenItems()).thenReturn(false);
        when(dtoFactory.createDto(ActionDescriptor.class)).thenReturn(actionDescriptor);
        when(actionDescriptor.withId(SHOW_HIDDEN_FILES_PARAM_ID)).thenReturn(actionDescriptor);
        when(actionDescriptor.withParameters(Matchers.<Map<String, String>>anyObject())).thenReturn(actionDescriptor);
    }

    @Test
    public void actionShouldBeFailedBecauseCurrentProjectIsNull() {
        when(appContext.getCurrentProject()).thenReturn(null);

        persistenceComponent.getActions(TEXT);

        verify(appContext).getCurrentProject();

        verifyNoMoreInteractions(dtoFactory, currentProject, appContext, showHiddenFilesAction);
    }

    @Test
    public void actionShouldBePerformed() {
        List<ActionDescriptor> result =  persistenceComponent.getActions(TEXT);

        verify(appContext).getCurrentProject();
        verify(currentProject).getCurrentTree();
        verify(treeStructure).getSettings();
        verify(actionManager).getId(showHiddenFilesAction);
        verify(treeSettings).isShowHiddenItems();
        verify(dtoFactory).createDto(ActionDescriptor.class);
        verify(actionDescriptor).withId(SHOW_HIDDEN_FILES_PARAM_ID);
        verify(actionDescriptor).withParameters(Matchers.<Map<String, String>>anyObject());

        assertThat(result.contains(actionDescriptor), is(true));
        assertThat(result.size(), is(1));
    }
}
