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
package org.eclipse.che.api.workspace.server;


import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;

//TODO

/**
 * Tests for {@link RuntimeWorkspaceRegistry}.
 *
 * @author Eugene Voevodin
 */
@Listeners(value = {MockitoTestNGListener.class})
public class RuntimeWorkspaceRegistryTest {

    @Mock
    MachineClient machineClient;

    RuntimeWorkspaceRegistry registry;

    @BeforeMethod
    public void setUpRegistry() {
        registry = new RuntimeWorkspaceRegistry(machineClient);
    }
}
