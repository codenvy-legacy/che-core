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
package org.eclipse.che.api.core.factory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Provide factory parameter compatibility options.
 *
 * @author Alexander Garagatyi
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FactoryParameter {
    enum Obligation {
        MANDATORY, OPTIONAL
    }

    enum FactoryFormat {
        ENCODED, NONENCODED, BOTH
    }

    enum Version {
        // NEVER must be the last defined constant
        V2_0, V2_1, NEVER;

        public static Version fromString(String v) {
            if (null != v) {
                switch (v) {
                    case "2.0":
                        return V2_0;
                    case "2.1":
                        return V2_1;

                }
            }

            throw new IllegalArgumentException("Unknown version " + v + ".");
        }

        @Override
        public String toString() {
            return super.name().substring(1).replace('_', '.');
        }
    }

    FactoryFormat format() default FactoryFormat.BOTH;

    Obligation obligation();

    boolean setByServer() default false;

    boolean trackedOnly() default false;

    String queryParameterName();

    Version deprecatedSince() default Version.NEVER;

    Version ignoredSince() default Version.NEVER;
}
