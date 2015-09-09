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
import com.google.common.base.Predicate;
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

import javax.annotation.Nullable;
import java.util.List;

import static com.google.common.collect.Lists.transform;

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
                deleteNodeHandler.delete((ResourceBasedNode<?>)o);
            } else {
                throw new IllegalArgumentException("Node isn't resource based.");
            }
        } else {
            List<ResourceBasedNode<?>> nodes = transform(selection, castNode());
            deleteNodeHandler.deleteNodes(nodes);
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

        boolean enable = Iterables.all(selection.getAllElements(), new Predicate<Object>() {
            @Override
            public boolean apply(@Nullable Object o) {
                return o instanceof ResourceBasedNode<?>;
            }
        });

        e.getPresentation().setEnabled(enable);
    }
}
