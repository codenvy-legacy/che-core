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
package org.eclipse.che.ide.outline;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.Resources;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.event.ActivePartChangedEvent;
import org.eclipse.che.ide.api.event.ActivePartChangedHandler;
import org.eclipse.che.ide.api.event.ProjectActionEvent;
import org.eclipse.che.ide.api.event.ProjectActionHandler;
import org.eclipse.che.ide.api.mvp.View;
import org.eclipse.che.ide.api.parts.HasView;
import org.eclipse.che.ide.api.parts.OutlinePart;
import org.eclipse.che.ide.api.parts.base.BasePresenter;
import org.eclipse.che.ide.api.texteditor.outline.HasOutline;
import org.vectomatic.dom.svg.ui.SVGResource;


/**
 * Part presenter for Outline.
 *
 * @author <a href="mailto:evidolob@exoplatform.com">Evgen Vidolob</a>
 * @author Dmitry Shnurenko
 */
@Singleton
public class OutlinePartPresenter extends BasePresenter implements ActivePartChangedHandler,
                                                                   OutlinePart,
                                                                   OutlinePartView.ActionDelegate,
                                                                   HasView {
    private final OutlinePartView          view;
    private final CoreLocalizationConstant coreLocalizationConstant;
    private       HasOutline               lastHasOutlineActivePart;
    private       Resources                resources;

    @Inject
    public OutlinePartPresenter(final OutlinePartView view,
                                EventBus eventBus,
                                CoreLocalizationConstant coreLocalizationConstant,
                                Resources resources) {
        this.view = view;
        this.coreLocalizationConstant = coreLocalizationConstant;
        this.resources = resources;

        view.setTitle(coreLocalizationConstant.outlineTitleBarText());
        view.setDelegate(this);

        eventBus.addHandler(ActivePartChangedEvent.TYPE, this);
        eventBus.addHandler(ProjectActionEvent.TYPE, new ProjectActionHandler() {
            @Override
            public void onProjectReady(ProjectActionEvent event) {

            }

            @Override
            public void onProjectClosing(ProjectActionEvent event) {

            }

            @Override
            public void onProjectClosed(ProjectActionEvent event) {
                view.clear();
            }

            @Override
            public void onProjectOpened(ProjectActionEvent event) {

            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public String getTitle() {
        return coreLocalizationConstant.outlineButtonTitle();
    }

    /** {@inheritDoc} */
    @Override
    public void setVisible(boolean visible) {
        view.setVisible(visible);
    }

    @Override
    public View getView() {
        return view;
    }

    /** {@inheritDoc} */
    @Override
    public ImageResource getTitleImage() {
        // TODO need to add an image
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public SVGResource getTitleSVGImage() {
        return resources.outlinePartIcon();
    }

    /** {@inheritDoc} */
    @Override
    public String getTitleToolTip() {
        // TODO need to add a tooltip
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void go(AcceptsOneWidget container) {
        container.setWidget(view);
    }

    /** {@inheritDoc} */
    @Override
    public void onActivePartChanged(ActivePartChangedEvent event) {
        if (event.getActivePart() == null) {
            lastHasOutlineActivePart = null;
            view.disableOutline(coreLocalizationConstant.outlineNoFileOpenedMessage());
            return;
        }

        if (!(event.getActivePart() instanceof EditorPartPresenter)) {
            return;
        }

        if (!(event.getActivePart() instanceof HasOutline)) {
            lastHasOutlineActivePart = null;
            view.disableOutline(coreLocalizationConstant.outlineNotAvailableMessage());
            return;
        }

        if (lastHasOutlineActivePart != event.getActivePart()) {
            lastHasOutlineActivePart = (HasOutline)event.getActivePart();
            if (lastHasOutlineActivePart.getOutline() != null) {
                lastHasOutlineActivePart.getOutline().go(view.getContainer());
                view.enableOutline();
            } else {
                view.disableOutline(coreLocalizationConstant.outlineNotAvailableMessage());
            }
        }
    }

}
