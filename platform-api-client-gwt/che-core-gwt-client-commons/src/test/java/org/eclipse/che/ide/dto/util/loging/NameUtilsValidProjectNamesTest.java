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
package org.eclipse.che.ide.dto.util.loging;

import org.eclipse.che.ide.util.NameUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertTrue;

/**
 * Tests check that NameUtils correct validate project name
 *
 * @author Andrienko Alexander
 */

@RunWith(Parameterized.class)
public class NameUtilsValidProjectNamesTest {

    String validName;

    public NameUtilsValidProjectNamesTest(String validName) {
        this.validName = validName;
    }

    /**
     * method get valid names for testing
     * @return collection valid names
     */

    @Parameterized.Parameters
    public static Collection getAllCases() {
        return Arrays.asList(new Object[][]{
                {"validprojectname"},
                {"valid_project_names"},
                {"valid-project-name"},
                {"valid.project.name"},
                {"..valid.project.name"},
                {"10ValidProjectName"},
                {"Valid10ProjectNAME"},
                {"ValidProjectName10"}
        });
    }

    @Test
    public void testNameUtilsValidator() {
        assertTrue(NameUtils.checkProjectName(validName));
    }
}
