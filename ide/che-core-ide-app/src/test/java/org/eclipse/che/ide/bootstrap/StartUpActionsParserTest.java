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
package org.eclipse.che.ide.bootstrap;

import org.eclipse.che.ide.api.app.StartUpAction;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class StartUpActionsParserTest {

    @Test
    public void test() {
        final StartUpAction startUpAction = StartUpActionsParser.parseActionQuery("createProject:projectName=test;projectType=maven");
        assertEquals("createProject", startUpAction.getActionId());
        assertNotNull(startUpAction.getParameters());
        assertEquals(2, startUpAction.getParameters().size());
        assertTrue(startUpAction.getParameters().containsKey("projectName"));
        assertTrue(startUpAction.getParameters().containsKey("projectType"));
        assertNotNull(startUpAction.getParameters().get("projectName"));
        assertNotNull(startUpAction.getParameters().get("projectType"));
        assertEquals("test", startUpAction.getParameters().get("projectName"));
        assertEquals("maven", startUpAction.getParameters().get("projectType"));
    }

    @Test
    public void test2() {
        final StartUpAction startUpAction = StartUpActionsParser.parseActionQuery("createProject:projectName;projectType");
        assertEquals("createProject", startUpAction.getActionId());
        assertNotNull(startUpAction.getParameters());
        assertEquals(2, startUpAction.getParameters().size());
        assertTrue(startUpAction.getParameters().containsKey("projectName"));
        assertTrue(startUpAction.getParameters().containsKey("projectType"));
        assertNull(startUpAction.getParameters().get("projectName"));
        assertNull(startUpAction.getParameters().get("projectType"));
    }

    @Test
    public void test3() {
        final StartUpAction startUpAction = StartUpActionsParser.parseActionQuery("createProject");
        assertEquals("createProject", startUpAction.getActionId());
        assertNull(startUpAction.getParameters());
    }
}
