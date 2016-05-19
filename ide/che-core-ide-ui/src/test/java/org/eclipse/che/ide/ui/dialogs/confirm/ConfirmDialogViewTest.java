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
package org.eclipse.che.ide.ui.dialogs.confirm;

import org.eclipse.che.ide.ui.dialogs.BaseTest;
import com.google.gwt.user.client.Element;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.eclipse.che.ide.ui.dialogs.confirm.ConfirmDialogView.ActionDelegate;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testing {@link ConfirmDialogViewImpl} functionality.
 *
 * @author Artem Zatsarynnyy
 */
public class ConfirmDialogViewTest extends BaseTest {
    @Mock
    private ActionDelegate        actionDelegate;
    @Mock
    private ConfirmDialogFooter   footer;
    private ConfirmDialogViewImpl view;

    @Before
    @Override
    public void setUp() {
        super.setUp();
        when(footer.getElement()).thenReturn(mock(Element.class));
        view = new ConfirmDialogViewImpl(footer);
    }

    @Test
    public void shouldSetDelegateOnFooter() throws Exception {
        view.setDelegate(actionDelegate);

        verify(footer).setDelegate(eq(actionDelegate));
    }

    @Test
    public void shouldCallAcceptedOnEnterClicked() throws Exception {
        view.setDelegate(actionDelegate);
        view.onEnterClicked();

        verify(actionDelegate).accepted();
    }
}
