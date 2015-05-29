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
 * Recipe to create new {@link org.eclipse.che.api.machine.server.spi.Instance}.
 *
 * @author gazarenkov
 */
public interface Recipe {

    String getId();

    void setId(String id);

    Recipe withId(String id);

    String getType();

    void setType(String type);

    Recipe withType(String type);

    String getScript();

    void setScript(String script);

    Recipe withScript(String script);

    String getCreator();

    void setCreator(String creator);

    Recipe withCreator(String creator);

    List<String> getTags();

    void setTags(List<String> tags);

    Recipe withTags(List<String> tags);

    Permissions getPermissions();

    void setPermissions(Permissions permissions);

    Recipe withPermissions(Permissions permissions);
}
