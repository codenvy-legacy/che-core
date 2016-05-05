/*******************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - API 
 *   SAP           - initial implementation
 *******************************************************************************/
package org.eclipse.che.git.impl.jgit;

public class Messages {
    private static final String BUNDLE_NAME = "org.eclipse.che.git.impl.jgit.messages"; //$NON-NLS-1$

    private static ExternalStringsSource SOURCE = new ExternalStringsSource(BUNDLE_NAME);

    private Messages() {
    }

    public static String getString(String key, Object... params) {
        return SOURCE.getString(key, params);
    }

}
