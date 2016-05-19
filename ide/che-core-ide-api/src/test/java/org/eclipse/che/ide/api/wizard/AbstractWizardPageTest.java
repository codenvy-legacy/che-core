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
package org.eclipse.che.ide.api.wizard;

import com.google.gwt.user.client.ui.AcceptsOneWidget;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Testing {@link AbstractWizardPage}.
 *
 * @author Artem Zatsarynnyy
 */
@RunWith(MockitoJUnitRunner.class)
public class AbstractWizardPageTest {
    private AbstractWizardPage<String> wizardPage;

    @Before
    public void setUp() {
        wizardPage = new DummyWizardPage();
    }

    @Test
    public void shouldInitPage() throws Exception {
        String dataObject = "dataObject";
        wizardPage.init(dataObject);
        assertEquals(dataObject, wizardPage.dataObject);
    }

    @Test
    public void shouldSetContext() throws Exception {
        Map<String, String> context = new HashMap<>();
        wizardPage.setContext(context);
        assertEquals(context, wizardPage.context);
    }

    @Test
    public void shouldSetUpdateDelegate() throws Exception {
        Wizard.UpdateDelegate updateDelegate = mock(Wizard.UpdateDelegate.class);
        wizardPage.setUpdateDelegate(updateDelegate);
        assertEquals(updateDelegate, wizardPage.updateDelegate);
    }

    @Test
    public void shouldNotSkipped() throws Exception {
        assertFalse(wizardPage.canSkip());
    }

    @Test
    public void shouldBeCompleted() throws Exception {
        assertTrue(wizardPage.isCompleted());
    }

    private class DummyWizardPage extends AbstractWizardPage<String> {
        @Override
        public void go(AcceptsOneWidget container) {
            // do nothing
        }
    }
}