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
package org.eclipse.che.api.core.model.machine;

/**
 * Describes server that can be started in machine
 *
 * @author Alexander Garagatyi
 */
public interface ServerConf {
    /**
     * Reference to this Che server
     */
    String getRef();

    /**
     * Exposed port of the server
     * <p>
     * Valid values:
     * <ul>
     *     <li>[1-9][0-9]*</li>
     *     <li>[1-9][0-9]*{@literal /}udp</li>
     *     <li>[1-9][0-9]*{@literal /}tcp</li>
     * </ul>
     * Tcp ports will loose /tcp suffix at runtime.
     * <br>Port without /tcp or /udp will be treated as tcp.
     */
    String getPort();

    /**
     * Protocol to commute to server
     * <p>
     * Example:
     * <ul>
     *     <li>http</li>
     *     <li>https</li>
     *     <li>udp</li>
     *     <li>tcp</li>
     * </ul>
     * If value is "http(s)" than API decides whether it http or https at runtime
     */
    String getProtocol();
}
