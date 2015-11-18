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

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.gwt.core.client.Callback;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.analytics.client.logger.AnalyticsEventLogger;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.callback.CallbackPromiseHelper;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.Resources;
import org.eclipse.che.ide.api.action.AbstractPerspectiveAction;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.action.PromisableAction;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.project.node.Node;
import org.eclipse.che.ide.api.selection.Selection;
import org.eclipse.che.ide.api.selection.SelectionAgent;
import org.eclipse.che.ide.part.explorer.project.ProjectExplorerPresenter;
import org.eclipse.che.ide.project.node.ResourceBasedNode;
import org.eclipse.che.ide.project.node.remove.DeleteNodeHandler;

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;

import static org.eclipse.che.api.promises.client.callback.CallbackPromiseHelper.createFromCallback;
import static org.eclipse.che.ide.workspace.perspectives.project.ProjectPerspective.PROJECT_PERSPECTIVE_ID;

/**
 * Action for deleting an item which is selected in 'Project Explorer'.
 *
 * @author Artem Zatsarynnyy
 * @author Dmitry Shnurenko
 * @author Vlad Zhukovskyi
 */
@Singleton
public class DeleteItemAction extends AbstractPerspectiveAction implements PromisableAction {
    private final AnalyticsEventLogger     eventLogger;
    private       SelectionAgent           selectionAgent;
    private       DeleteNodeHandler        deleteNodeHandler;
    private       AppContext               appContext;
    private final ProjectExplorerPresenter projectExplorer;

    private Callback<Void, Throwable> actionCompletedCallBack;

    @Inject
    public DeleteItemAction(Resources resources,
                            AnalyticsEventLogger eventLogger,
                            SelectionAgent selectionAgent,
                            DeleteNodeHandler deleteNodeHandler,
                            CoreLocalizationConstant localization,
                            AppContext appContext,
                            ProjectExplorerPresenter projectExplorer) {
        super(Collections.singletonList(PROJECT_PERSPECTIVE_ID),
              localization.deleteItemActionText(),
              localization.deleteItemActionDescription(),
              null,
              resources.delete());
        this.selectionAgent = selectionAgent;
        this.eventLogger = eventLogger;
        this.deleteNodeHandler = deleteNodeHandler;
        this.appContext = appContext;
        this.projectExplorer = projectExplorer;
    }

    /** {@inheritDoc} */
    @Override
    public void actionPerformed(ActionEvent e) {
        eventLogger.log(this);

        final Selection<?> selection = selectionAgent.getSelection();

        if (selection == null || selection.isEmpty()) {
            throw new IllegalStateException("Nodes weren't found in the selection agent");
        }

        if (Iterables.all(selection.getAllElements(), isResourceBasedNode())) {
            final List<ResourceBasedNode<?>> nodes = Lists.newArrayList(Iterables.transform(selection.getAllElements(), castNode()));

            Node selectedNode = nodes.get(0);

            Node parentNode = selectedNode.getParent();

            deleteNodeHandler.deleteAll(nodes, true).then(synchronizeProjectView(parentNode)).then(actionComplete());
        }
    }

    private Operation<Void> synchronizeProjectView(final Node parent) {
        return new Operation<Void>() {
            @Override
            public void apply(Void arg) throws OperationException {
                projectExplorer.reloadChildren(parent);
            }
        };
    }

    private Operation<Void> actionComplete() {
        return new Operation<Void>() {
            @Override
            public void apply(Void arg) throws OperationException {
                if (actionCompletedCallBack != null) {
                    actionCompletedCallBack.onSuccess(null);
                }
            }
        };
    }

    private Predicate<Object> isResourceBasedNode() {
        return new Predicate<Object>() {
            @Override
            public boolean apply(@Nullable Object node) {
                return node instanceof ResourceBasedNode;
            }
        };
    }

    private com.google.common.base.Function<Object, ResourceBasedNode<?>> castNode() {
        return new com.google.common.base.Function<Object, ResourceBasedNode<?>>() {
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
    public void updateInPerspective(@NotNull ActionEvent event) {
        event.getPresentation().setVisible(true);

        final Selection<?> selection = selectionAgent.getSelection();

        if (selection == null || selection.isEmpty()) {
            event.getPresentation().setEnabled(false);
            return;
        }

        boolean enable = Iterables.all(selection.getAllElements(), isResourceBasedNode());

        event.getPresentation().setEnabled(enable && appContext.getCurrentUser().isUserPermanent());
    }

    @Override
    public Promise<Void> promise(final ActionEvent event) {
        final CallbackPromiseHelper.Call<Void, Throwable> call = new CallbackPromiseHelper.Call<Void, Throwable>() {
            @Override
            public void makeCall(Callback<Void, Throwable> callback) {
                actionCompletedCallBack = callback;
                actionPerformed(event);
            }
        };

        return createFromCallback(call);
    }
}
