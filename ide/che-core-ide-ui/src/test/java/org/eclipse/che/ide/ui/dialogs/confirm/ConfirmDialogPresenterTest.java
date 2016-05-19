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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Testing {@link ConfirmDialogPresenter} functionality.
 *
 * @author Artem Zatsarynnyy
 */
public class ConfirmDialogPresenterTest extends BaseTest {
    @Mock
    private ConfirmDialogView      view;
    private ConfirmDialogPresenter presenter;

    @Before
    @Override
    public void setUp() {
        super.setUp();
        presenter = new ConfirmDialogPresenter(view, TITLE, MESSAGE, confirmCallback, cancelCallback);
    }

    @Test
    public void shouldCallCallbackOnCanceled() throws Exception {
        presenter.cancelled();

        verify(view).closeDialog();
        verify(cancelCallback).cancelled();
    }

    @Test
    public void shouldNotCallCallbackOnCanceled() throws Exception {
        presenter = new ConfirmDialogPresenter(view, TITLE, MESSAGE, confirmCallback, null);

        presenter.cancelled();

        verify(view).closeDialog();
        verify(cancelCallback, never()).cancelled();
    }

    @Test
    public void shouldCallCallbackOnAccepted() throws Exception {
        presenter.accepted();

        verify(view).closeDialog();
        verify(confirmCallback).accepted();
    }

    @Test
    public void shouldNotCallCallbackOnAccepted() throws Exception {
        presenter = new ConfirmDialogPresenter(view, TITLE, MESSAGE, null, cancelCallback);

        presenter.accepted();

        verify(view).closeDialog();
        verify(confirmCallback, never()).accepted();
    }

    @Test
    public void shouldShowView() throws Exception {
        presenter.show();

        verify(view).showDialog();
    }
}
