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

import org.eclipse.che.ide.ui.UILocalizationConstant;
import org.eclipse.che.ide.ui.dialogs.BaseTest;
import com.google.gwt.event.dom.client.ClickEvent;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.eclipse.che.ide.ui.dialogs.confirm.ConfirmDialogView.ActionDelegate;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Testing {@link ConfirmDialogFooter} functionality.
 *
 * @author Artem Zatsarynnyy
 */
public class ConfirmDialogFooterTest extends BaseTest {
    @Mock
    private UILocalizationConstant           uiLocalizationConstant;
    @Mock
    private ConfirmDialogView.ActionDelegate actionDelegate;
    @InjectMocks
    private ConfirmDialogFooter              footer;

    @Before
    @Override
    public void setUp() {
        super.setUp();
        footer.setDelegate(actionDelegate);
    }

    @Test
    public void shouldCallAcceptedOnOkClicked() throws Exception {
        footer.handleOkClick(mock(ClickEvent.class));

        verify(actionDelegate).accepted();
    }

    @Test
    public void shouldCallCancelledOnCancelClicked() throws Exception {
        footer.handleCancelClick(mock(ClickEvent.class));

        verify(actionDelegate).cancelled();
    }
}
