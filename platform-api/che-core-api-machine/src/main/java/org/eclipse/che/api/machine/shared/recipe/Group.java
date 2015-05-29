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
package org.eclipse.che.api.machine.shared.recipe;

import java.util.List;

/**
 * @author Eugene Voevodin
 */
public interface Group {

    String getName();

    void setName(String name);

    Group withName(String name);

    String getUnit();

    void setUnit(String unit);

    Group withUnit(String unit);

    List<String> getAcl();

    void setAcl(List<String> acl);

    Group withAcl(List<String> acl);
}
