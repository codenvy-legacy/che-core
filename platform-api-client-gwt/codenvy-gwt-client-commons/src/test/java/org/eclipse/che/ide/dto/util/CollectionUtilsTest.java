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
package org.eclipse.che.ide.dto.util;

import org.eclipse.che.ide.util.CollectionUtils;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Vitaly Parfonov
 */
public class CollectionUtilsTest {

    private Map<String,String> map;

    @Before
    public void setUp() {
        map = new HashMap<>();
        map.put("key1", "foo");
        map.put("key3", "foo");
        map.put("key2", "bar");
    }


    @Test
    public void test() {
        Set<String> strings = map.keySet();
        for (String key : strings) {
            System.out.println(key);
        }
    }

    @Test
    public void getKeysByValue() {
        Set<String> keysByValue = CollectionUtils.getKeysByValue(map, "foo");
        Assert.assertNotNull(keysByValue);
        Assert.assertEquals(2, keysByValue.size());
        Assert.assertTrue(keysByValue.contains("key1"));
        Assert.assertTrue(keysByValue.contains("key3"));

    }

    @Test
    public void getKeyByValue() {
        String foo = CollectionUtils.getKeyByValue(map, "bar");
        Assert.assertNotNull(foo);
        Assert.assertEquals("key2", foo);
    }

    @Test
    public void getKeyByValue2() {
        String foo = CollectionUtils.getKeyByValue(map, "foo");
        Assert.assertNotNull(foo);
        Assert.assertTrue(foo.equals("key1") || foo.equals("key3") );
    }


    @Test
    public void getKeyByValueNotFond() {
        Assert.assertNull(CollectionUtils.getKeyByValue(map, "XXX"));
    }


}
