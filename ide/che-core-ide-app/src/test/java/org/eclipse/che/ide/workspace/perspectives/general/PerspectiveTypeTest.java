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
package org.eclipse.che.ide.workspace.perspectives.general;

import org.eclipse.che.ide.workspace.perspectives.general.PerspectiveType.PerspectiveTypeListener;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.eclipse.che.ide.workspace.perspectives.general.Perspective.Type.MACHINE;
import static org.eclipse.che.ide.workspace.perspectives.general.Perspective.Type.PROJECT;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;

/**
 * @author Dmitry Shnurenko
 */
@RunWith(MockitoJUnitRunner.class)
public class PerspectiveTypeTest {

    @Mock
    private PerspectiveTypeListener typeListener;

    @InjectMocks
    private PerspectiveType type;

    @Test
    public void defaultTypeShouldBeGot() {
        Perspective.Type testType = type.getType();

        assertThat(testType, equalTo(PROJECT));
    }

    @Test
    public void typeShouldBeSetAndListenersShouldBeNotified() {
        type.addListener(typeListener);

        type.setType(MACHINE);

        verify(typeListener).onPerspectiveChanged();

        assertThat(type.getType(), equalTo(MACHINE));
    }

}