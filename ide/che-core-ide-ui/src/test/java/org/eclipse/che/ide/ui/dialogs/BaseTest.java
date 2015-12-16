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
package org.eclipse.che.ide.ui.dialogs;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwtmockito.GwtMockitoTestRunner;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;

/** @author Artem Zatsarynnyi */
@RunWith(GwtMockitoTestRunner.class)
public abstract class BaseTest  {
    protected static String TITLE   = "title";
    protected static String MESSAGE = "message";
    protected static String CONFIRM_BUTTON_TEXT = "text";
    @Mock
    protected CancelCallback  cancelCallback;
    @Mock
    protected ConfirmCallback confirmCallback;
    @Mock
    protected InputCallback   inputCallback;
    @Mock
    protected IsWidget        isWidget;

    @Before
    public void setUp() {
    }
}
