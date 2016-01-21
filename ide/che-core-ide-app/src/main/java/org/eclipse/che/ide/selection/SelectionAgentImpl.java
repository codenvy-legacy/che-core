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
package org.eclipse.che.ide.selection;

import org.eclipse.che.ide.api.event.ActivePartChangedEvent;
import org.eclipse.che.ide.api.event.ActivePartChangedHandler;
import org.eclipse.che.ide.api.event.SelectionChangedEvent;
import org.eclipse.che.ide.api.parts.PartPresenter;
import org.eclipse.che.ide.api.parts.PropertyListener;
import org.eclipse.che.ide.api.selection.Selection;
import org.eclipse.che.ide.api.selection.SelectionAgent;
import org.eclipse.che.ide.part.explorer.project.ProjectExplorerPresenter;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;

/**
 * Implements {@link SelectionAgent}
 *
 * @author Nikolay Zamosenchuk
 */
public class SelectionAgentImpl implements ActivePartChangedHandler, PropertyListener, SelectionAgent {

    private       PartPresenter activePart;
    private final EventBus      eventBus;
    private final ProjectExplorerPresenter projectExplorer;

    @Inject
    public SelectionAgentImpl(EventBus eventBus, ProjectExplorerPresenter projectExplorer) {
        this.eventBus = eventBus;
        this.projectExplorer = projectExplorer;
        // bind event listener
        eventBus.addHandler(ActivePartChangedEvent.TYPE, this);
    }

    /** {@inheritDoc} */
    @Override
    public Selection<?> getActivePartSelection() {
        return activePart != null ? activePart.getSelection() : null;
    }

    @Override
    public Selection<?> getProjectExplorerSelection() {
        return projectExplorer.getSelection();
    }

    protected void notifySelectionChanged() {
        eventBus.fireEvent(new SelectionChangedEvent(getActivePartSelection()));
    }

    /** {@inheritDoc} */
    @Override
    public void onActivePartChanged(ActivePartChangedEvent event) {
        // remove listener from previous active part
        if (activePart != null) {
            activePart.removePropertyListener(this);
        }
        // set new active part
        activePart = event.getActivePart();
        if (activePart != null) {
            activePart.addPropertyListener(this);
        }
        notifySelectionChanged();
    }

    /** {@inheritDoc} */
    @Override
    public void propertyChanged(PartPresenter source, int propId) {
        // Check property and ensure came from active part
        if (propId == PartPresenter.SELECTION_PROPERTY && source == activePart) {
            notifySelectionChanged();
        }
    }

}
