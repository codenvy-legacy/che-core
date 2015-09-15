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
package org.eclipse.che.ide.ui.dropdown;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.action.ProjectAction;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.vectomatic.dom.svg.ui.SVGResource;

import javax.validation.constraints.NotNull;

/**
 * The action which describes simple element of the custom drop down list.
 *
 * @author Valeriy Svydenko
 */
public class SimpleListElementAction extends ProjectAction {

    private final AppContext           appContext;
    private final String               name;
    private final SVGResource          image;
    private final DropDownHeaderWidget header;

    @AssistedInject
    public SimpleListElementAction(AppContext appContext,
                                   @NotNull @Assisted String name,
                                   @NotNull @Assisted SVGResource image,
                                   @NotNull @Assisted DropDownHeaderWidget header) {
        super(name, name, image);

        this.name = name;
        this.image = image;
        this.header = header;
        this.appContext = appContext;
    }

    /** {@inheritDoc} */
    @Override
    public void actionPerformed(ActionEvent e) {
        header.selectElement(image, name);
    }

    /** {@inheritDoc} */
    @Override
    protected void updateProjectAction(ActionEvent event) {
        CurrentProject currentProject = appContext.getCurrentProject();

        event.getPresentation().setEnabledAndVisible(currentProject != null);
    }


    /** @return title of the element */
    @NotNull
    public String getName() {
        return name;
    }

    /** @return icon of the element */
    @NotNull
    public SVGResource getImage() {
        return image;
    }
}
