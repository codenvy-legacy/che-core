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
package org.eclipse.che.ide.ui.dropdown;

import com.google.gwtmockito.GwtMockitoTestRunner;

import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.action.Presentation;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.vectomatic.dom.svg.ui.SVGResource;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** @author Valeriy Svydenko */
@RunWith(GwtMockitoTestRunner.class)
public class SimpleListElementActionTest {
    static private final String TEXT = "text";

    @Mock
    private AppContext           appContext;
    @Mock
    private SVGResource          image;
    @Mock
    private DropDownHeaderWidget header;
    @Mock
    private ActionEvent          actionEvent;
    @Mock
    private Presentation         presentation;

    private SimpleListElementAction action;

    @Before
    public void setUp() {
        when(actionEvent.getPresentation()).thenReturn(presentation);

        action = new SimpleListElementAction(appContext, TEXT, image, header);
    }

    @Test
    public void actionShouldBePerformed() throws Exception {
        action.actionPerformed(actionEvent);

        verify(header).selectElement(image, TEXT);
    }

    @Test
    public void projectActionShouldBeUpdated() throws Exception {
        CurrentProject currentProject = mock(CurrentProject.class);
        when(appContext.getCurrentProject()).thenReturn(currentProject);

        action.updateProjectAction(actionEvent);

        verify(presentation).setEnabledAndVisible(true);
    }

    @Test
    public void projectActionShouldNotBeUpdatedIfCurrentProjectIsNull() throws Exception {
        when(appContext.getCurrentProject()).thenReturn(null);

        action.updateProjectAction(actionEvent);

        verify(presentation).setEnabledAndVisible(false);
    }

    @Test
    public void nameShouldBeReturned() throws Exception {
        assertThat(action.getName(), equalTo(TEXT));
    }

    @Test
    public void imageShouldBeReturned() throws Exception {
        assertThat(action.getImage(), equalTo(image));
    }
}
