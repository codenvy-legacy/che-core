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
package org.eclipse.che.ide.api.event;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.eclipse.che.ide.api.event.UpdateNodeEvent.TYPE;
import static org.mockito.Mockito.verify;

/**
 * @author Andrienko Alexander
 */
@RunWith(MockitoJUnitRunner.class)
public class UpdateNodeEventTest {

    @Mock
    private UpdateNodeEventHandler handler;

    @InjectMocks
    private UpdateNodeEvent updateNodeEvent;

    @Test
    public void associatedTypeShouldBeReturned() {
        assertThat(updateNodeEvent.getAssociatedType(), is(TYPE));
    }

    @Test
    public void handlerShouldBeDispatched() {
        updateNodeEvent.dispatch(handler);

        verify(handler).onNodeUpdated();
    }
}
