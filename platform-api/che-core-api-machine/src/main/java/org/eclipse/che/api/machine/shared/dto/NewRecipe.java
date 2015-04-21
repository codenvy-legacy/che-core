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
package org.eclipse.che.api.machine.shared.dto;

import org.eclipse.che.dto.shared.DTO;

import java.util.List;

/**
 * Describes new recipe that is recipe type, script and tags.
 *
 * @author Eugene Voevodin
 */
@DTO
public interface NewRecipe {

    String getType();

    void setType(String type);

    NewRecipe withType(String type);

    String getScript();

    void setScript(String script);

    NewRecipe withScript(String script);

    List<String> getTags();

    void setTags(List<String> tags);

    NewRecipe withTags(List<String> tags);
}
