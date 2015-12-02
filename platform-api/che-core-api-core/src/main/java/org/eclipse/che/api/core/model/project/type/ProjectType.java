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
package org.eclipse.che.api.core.model.project.type;

import java.util.List;

/**
 * @author gazarenkov
 */
public interface ProjectType {

    boolean isPersisted();

    String getId();

    String getDisplayName();

    List<? extends Attribute> getAttributes();

    List<ProjectType> getParents();

    String getDefaultRecipe();

    boolean isTypeOf(String typeId);

    Attribute getAttribute(String name);

    boolean canBeMixin();

    boolean canBePrimary();
}
