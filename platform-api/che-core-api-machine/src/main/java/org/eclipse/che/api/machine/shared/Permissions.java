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
package org.eclipse.che.api.machine.shared;

import java.util.List;
import java.util.Map;

/**
 * @author Eugene Voevodin
 */
public interface Permissions {

    Map<String, List<String>> getUsers();

    void setUsers(Map<String, List<String>> users);

    Permissions withUsers(Map<String, List<String>> users);

    List<Group> getGroups();

    void setGroups(List<Group> groups);

    Permissions withGroups(List<Group> groups);
}
