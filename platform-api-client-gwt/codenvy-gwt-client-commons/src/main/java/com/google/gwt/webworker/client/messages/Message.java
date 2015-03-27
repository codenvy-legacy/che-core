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
package com.google.gwt.webworker.client.messages;

/**
 *  Base interface for all DTOs that adds a type tag for routing messages.
 * @author <a href="mailto:evidolob@codenvy.com">Evgen Vidolob</a>
 * @version $Id:
 */
public interface Message {
    public static final int    NON_ROUTABLE_TYPE = -2;
    public static final String TYPE_FIELD = "_type";

    /**
     * Every DTO needs to report a type for the purposes of routing messages on
     * the client.
     */
    public int getType();
}
