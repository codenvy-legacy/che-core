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
package org.eclipse.che.ide.icon;

import org.eclipse.che.ide.api.icon.Icon;
import org.eclipse.che.ide.api.icon.IconRegistry;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of {@link IconRegistry}.
 *
 * @author Vitaly Parfonov
 * @author Artem Zatsarynnyy
 */
public class IconRegistryImpl implements IconRegistry {

    private Map<String, Icon> icons = new HashMap<>();

    @Override
    public void registerIcon(Icon icon) {
        icons.put(icon.getId(), icon);
    }

    @Override
    public Icon getIcon(String id) {
        Icon icon = icons.get(id);
        if (icon == null) {
            final String prefix = id.split("\\.")[0];
            final String defaultIconId = id.replaceFirst(prefix, "default");
            icon = icons.get(defaultIconId);
            if (icon == null) {
                icon = getGenericIcon();
            }
        }
        return icon;
    }

    @Override
    public Icon getIconIfExist(String id) {
        return icons.get(id);
    }

    @Override
    public Icon getGenericIcon() {
        return icons.get("default");
    }
}
