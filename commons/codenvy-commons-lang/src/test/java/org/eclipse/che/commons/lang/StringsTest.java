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
package org.eclipse.che.commons.lang;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

public class StringsTest {

    @Test
    public void testNullToEmpty() {
        assertEquals(Strings.nullToEmpty(null), "");
        assertEquals(Strings.nullToEmpty(""), "");
        assertEquals(Strings.nullToEmpty("a"), "a");
    }

    @Test
    public void testEmptyToNull() {
        assertNull(Strings.emptyToNull(null));
        assertNull(Strings.emptyToNull(""));
        assertEquals(Strings.emptyToNull("a"), "a");
    }

    @Test
    public void testIsNullOrEmpty() {
        assertTrue(Strings.isNullOrEmpty(null));
        assertTrue(Strings.isNullOrEmpty(""));
        assertFalse(Strings.isNullOrEmpty("a"));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testJoinShouldThrowIllegalArgumentExceptionIfDelimiterIsNull() {
        assertEquals(Strings.join(null, "1", "2"), "");
    }

    @Test
    public void testJoin() {
        assertEquals(Strings.join(",", "1", "2"), "1,2");
        assertEquals(Strings.join("1", "1", "1"), "111");
        assertEquals(Strings.join(",", "2"), "2");
        assertEquals(Strings.join(","), "");
    }

    @Test
    public void shouldReturnEmptyStringOnEmptyParameters_longestCommonPrefix() {
        assertEquals("", Strings.longestCommonPrefix());

    }

    @Test
    public void shouldReturnSameStringIf1Parameter_longestCommonPrefix() {
        assertEquals("param", Strings.longestCommonPrefix("param"));

    }

    @Test
    public void shouldReturnEmptyIfNoCommonPrefix_longestCommonPrefix() {
        assertEquals("", Strings.longestCommonPrefix("dff", "blafa"));

    }

    @Test
    public void shouldFindCommonPrefix_longestCommonPrefix() {
        assertEquals("bla", Strings.longestCommonPrefix("blafoijqoweir", "blafa", "blamirfjo"));

    }
}
