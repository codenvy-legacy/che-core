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
package org.eclipse.che.api.runner.dto;

import org.eclipse.che.dto.shared.DTO;

import java.util.Map;

/**
 * Describes port application port mapping. If run multiple application on the same instance we are not able to share the same port
 * numbers. In this case we use forwarding of traffic between application ports and public ports available for access from internet.
 * Typically user knows which ports he needs but if we use forwarding he mayn't know which public port we map for his application.
 * PortMapping helps to resolve such issues. For example if user deploy application in tomcat he expect to see application on port 8080 but
 * in fact we use any free port, for instance 43987. Port mapping specified us mapping String to String to be able add protocol for the
 * port (e.g 22/tcp), but this is implementation specific. Private ports are used as keys and public ports as values in the port {@code
 * Map}.
 *
 * @author andrew00x
 */
@DTO
public interface PortMapping {
    /** Host name. May be DNS name or IP address. */
    String getHost();

    void setHost(String host);

    PortMapping withHost(String host);

    /** Post mapping. Private ports are used as keys and public ports as values in returned {@code Map}. */
    Map<String, String> getPorts();

    void setPorts(Map<String, String> ports);

    PortMapping withPorts(Map<String, String> ports);
}
