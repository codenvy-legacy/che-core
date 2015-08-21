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
package org.eclipse.che.ide.actions;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.analytics.client.logger.AnalyticsEventLogger;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.Resources;
import org.eclipse.che.ide.api.action.Action;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.selection.Selection;
import org.eclipse.che.ide.part.explorer.project.NewProjectExplorerPresenter;
import org.eclipse.che.ide.part.projectexplorer.DeleteNodeHandler;
import org.eclipse.che.ide.project.node.ResourceBasedNode;
import org.eclipse.che.ide.util.loging.Log;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Action for deleting an item which is selected in 'Project Explorer'.
 *
 * @author Artem Zatsarynnyy
 */
@Singleton
public class DeleteItemAction extends Action {
    private final AnalyticsEventLogger        eventLogger;
    private       NewProjectExplorerPresenter projectExplorer;
    private       DeleteNodeHandler           deleteNodeHandler;
    private       AppContext                  appContext;

    @Inject
    public DeleteItemAction(Resources resources,
                            AnalyticsEventLogger eventLogger,
                            NewProjectExplorerPresenter projectExplorer,
                            DeleteNodeHandler deleteNodeHandler, CoreLocalizationConstant localization, AppContext appContext) {
        super(localization.deleteItemActionText(), localization.deleteItemActionDescription(), null, resources.delete());
        this.projectExplorer = projectExplorer;
        this.eventLogger = eventLogger;
        this.deleteNodeHandler = deleteNodeHandler;
        this.appContext = appContext;
    }

    /** {@inheritDoc} */
    @Override
    public void actionPerformed(ActionEvent e) {
        eventLogger.log(this);

        List<?> selection = projectExplorer.getSelection().getAllElements();

        if (selection.size() == 1) {
            Object o = selection.get(0);
            if (o instanceof ResourceBasedNode<?>) {
                projectExplorer.resetGoIntoMode();
                ((ResourceBasedNode)o).delete();
            } else {
                throw new IllegalArgumentException("Node isn't resource based.");
            }
        } else {
            Iterable<ResourceBasedNode<?>> nodes = Iterables.transform(selection, castNode());
            doDelete(nodes);
        }
    }

    private Function<Object, ResourceBasedNode<?>> castNode() {
        return new Function<Object, ResourceBasedNode<?>>() {
            @Nullable
            @Override
            public ResourceBasedNode<?> apply(Object o) {
                if (o instanceof ResourceBasedNode<?>) {
                    return (ResourceBasedNode<?>)o;
                }

                throw new IllegalArgumentException("Node isn't resource based");
            }
        };
    }

    private void doDelete(Iterable<ResourceBasedNode<?>> nodes) {
        Log.info(this.getClass(), "doDelete():103: " + "nodes: " + nodes);
    }

    /** {@inheritDoc} */
    @Override
    public void update(ActionEvent e) {
        if ((appContext.getCurrentProject() == null && !appContext.getCurrentUser().isUserPermanent()) ||
            (appContext.getCurrentProject() != null && appContext.getCurrentProject().isReadOnly())) {
            e.getPresentation().setVisible(true);
            e.getPresentation().setEnabled(false);
            return;
        }

        Selection<?> selection = projectExplorer.getSelection();

        if (selection == null) {
            e.getPresentation().setEnabled(false);
            return;
        }

        for (Object o : selection.getAllElements()) {
            if (!(o instanceof ResourceBasedNode<?>)) {
                e.getPresentation().setEnabled(false);
                return;
            }
        }

        e.getPresentation().setEnabled(true);

//        boolean isEnabled = false;
//        Selection<?> selection = selectionAgent.getSelection();
//        if (selection != null && selection.getFirstElement() instanceof StorableNode) {
//            isEnabled = ((StorableNode)selection.getFirstElement()).isDeletable();
//        }
//        e.getPresentation().setEnabled(isEnabled);
    }
}
