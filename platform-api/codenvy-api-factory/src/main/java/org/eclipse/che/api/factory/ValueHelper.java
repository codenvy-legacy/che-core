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
package org.eclipse.che.api.factory;

import java.lang.reflect.*;
import java.util.*;

/**
 * @author Sergii Kabashniuk
 */
public class ValueHelper {
    /**
     * Primitive types map, this map contains all primitive java types except
     * char because {@link Character} has not static method valueOf with String
     * parameter.
     */
    static final Map<String, Class<?>> PRIMITIVE_TYPES_MAP;

    static {
        Map<String, Class<?>> m = new HashMap<String, Class<?>>(7);
        m.put("boolean", Boolean.class);
        m.put("byte", Byte.class);
        m.put("short", Short.class);
        m.put("int", Integer.class);
        m.put("long", Long.class);
        m.put("float", Float.class);
        m.put("double", Double.class);
        PRIMITIVE_TYPES_MAP = Collections.unmodifiableMap(m);
    }

    static Object createValue(Class<?> parameterClass, Set<String> values)
            throws Exception {
        // parameters is not collection
        Method methodValueOf;
        Constructor<?> constructor;
        String firstValue = null;
        if (values.size() > 0) {
            firstValue = values.iterator().next();
        }

        if (parameterClass.isPrimitive()) {
            Class<?> c = PRIMITIVE_TYPES_MAP.get(parameterClass.getName());
            Method method = getStringValueOfMethod(c);

            // invoke valueOf method for creation object
            return method.invoke(null, firstValue);
        } else if (parameterClass == String.class) {
            // String
            return firstValue;
        } else if ((methodValueOf = getStringValueOfMethod(parameterClass)) != null) {
            // static valueOf method
            return methodValueOf.invoke(null, firstValue);
        } else if ((constructor = getStringConstructor(parameterClass)) != null) {
            // constructor with String
            return constructor.newInstance(firstValue);
        }
        return null;
    }


    /**
     * Get static {@link java.lang.reflect.Method} with single string argument and name 'valueOf'
     * for supplied class.
     *
     * @param clazz
     *         class for discovering to have public static method with name
     *         'valueOf' and single string argument
     * @return valueOf method or null if class has not it
     */
    static Method getStringValueOfMethod(Class<?> clazz) {
        try {
            Method method = clazz.getDeclaredMethod("valueOf", String.class);
            return Modifier.isStatic(method.getModifiers()) ? method : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get constructor with single string argument for supplied class.
     *
     * @param clazz
     *         class for discovering to have constructor with single string
     *         argument
     * @return constructor or null if class has not constructor with single
     * string argument
     */
    static Constructor<?> getStringConstructor(Class<?> clazz) {
        try {
            return clazz.getConstructor(String.class);
        } catch (Exception e) {
            return null;
        }

    }

    /**
     * Check that value wasn't set by json parser.
     *
     * @param value
     *         - value to check
     * @return - true if value is useless for factory (0 for primitives or empty collection or map), false otherwise
     */
    static boolean isEmpty(Object value) {
        if ((null == value) ||
            (value.getClass().equals(Boolean.class) && (Boolean)value == false) ||
            (value.getClass().equals(Integer.class) && (Integer)value == 0) ||
            (value.getClass().equals(Long.class) && (Long)value == 0) ||
            (Collection.class.isAssignableFrom(value.getClass()) && ((Collection)value).isEmpty()) ||
            (Map.class.isAssignableFrom(value.getClass()) && ((Map)value).isEmpty()) ||
            (value.getClass().equals(Byte.class) && (Byte)value == 0) ||
            (value.getClass().equals(Short.class) && (Short)value == 0) ||
            (value.getClass().equals(Double.class) && (Double)value == 0) ||
            (value.getClass().equals(Float.class) && (Float)value == 0)) {

            return true;
        }
        return false;
    }

}
