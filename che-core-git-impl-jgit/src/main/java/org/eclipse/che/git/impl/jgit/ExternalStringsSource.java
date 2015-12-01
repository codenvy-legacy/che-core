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

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads and retrieves externally defined texts.
 *
 */
public class ExternalStringsSource {

    private static final Logger LOG = LoggerFactory.getLogger(ExternalStringsSource.class);

    private final String _bundleName;
    private ResourceBundle _bundle;

    public ExternalStringsSource(String bundleName) {
        this._bundleName = bundleName;
    }

    /**
     * Get the externally defined string with the given key.
     * 
     * @param key
     * @param params
     * @return
     */
    public String getString(String key, Object... params) {
        if (_bundle == null) {
            _bundle = ResourceBundle.getBundle(_bundleName);
        }
        String rawMessage;
        try {
            rawMessage = _bundle.getString(key);
        } catch (MissingResourceException e) {
            LOG.warn("Could not retrieve externally defined string " + key, e);
            return '!' + key + '!';
        }
        return MessageFormat.format(rawMessage, params);
    }

}
