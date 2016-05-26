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
package org.eclipse.che.api.project.server;

import org.eclipse.che.api.project.shared.EnvironmentId;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author andrew00x
 */
public class EnvironmentIdTest {
    @Test
    public void testEnvironmentIdNoCategoryToString() {
        EnvironmentId id = new EnvironmentId(EnvironmentId.Scope.project, "env");
        Assert.assertEquals(id.toString(), "project:/env");
    }

    @Test
    public void testEnvironmentIdToString() {
        EnvironmentId id = new EnvironmentId(EnvironmentId.Scope.system, "Java/Web", "Tomcat7");
        Assert.assertEquals(id.toString(), "system:/Java/Web/Tomcat7");
    }

    @Test
    public void testEnvironmentIdNoCategoryParse() {
        String fqn = "project://env";
        EnvironmentId id = EnvironmentId.parse(fqn);
        Assert.assertEquals(id.getScope(), EnvironmentId.Scope.project);
        Assert.assertEquals(id.getCategory(), "");
        Assert.assertEquals(id.getName(), "env");
    }

    @Test
    public void testEnvironmentIdParse() {
        String fqn = "system:/Java/Web/Tomcat7";
        EnvironmentId id = EnvironmentId.parse(fqn);
        Assert.assertEquals(id.getScope(), EnvironmentId.Scope.system);
        Assert.assertEquals(id.getCategory(), "Java/Web");
        Assert.assertEquals(id.getName(), "Tomcat7");
    }

    @Test(expectedExceptions = {IllegalArgumentException.class})
    public void testEnvironmentIdInvalidScope() {
        String fqn = "invalid://env";
        EnvironmentId id = EnvironmentId.parse(fqn);
        Assert.assertEquals(id.getScope(), EnvironmentId.Scope.project);
        Assert.assertEquals(id.getCategory(), "");
        Assert.assertEquals(id.getName(), "env");
    }

    @Test(expectedExceptions = {IllegalArgumentException.class})
    public void testEnvironmentIdInvalidFormat() {
        String fqn = "system: Tomcat7";
        EnvironmentId id = EnvironmentId.parse(fqn);
        Assert.assertEquals(id.getScope(), EnvironmentId.Scope.system);
        Assert.assertEquals(id.getCategory(), "Java/Web");
        Assert.assertEquals(id.getName(), "Tomcat7");
    }
}
