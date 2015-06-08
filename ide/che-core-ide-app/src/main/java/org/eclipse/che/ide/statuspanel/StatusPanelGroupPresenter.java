/*******************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * <p/>
 * Contributors:
 * Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.statuspanel;

import com.codenvy.ide.subscriptions.client.MemoryIndicatorAction;
import com.codenvy.ide.subscriptions.client.OnPremisesChecker;
import com.codenvy.ide.subscriptions.client.QueueType;
import com.codenvy.ide.subscriptions.client.QueueTypeIndicatorAction;
import com.codenvy.ide.subscriptions.client.RedirectLinkAction;
import com.codenvy.ide.subscriptions.client.SubscriptionIndicatorAction;
import com.codenvy.ide.subscriptions.client.SubscriptionPanelLocalizationConstant;
import com.codenvy.ide.subscriptions.client.TrademarkLinkAction;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.account.gwt.client.AccountServiceClient;
import org.eclipse.che.api.account.server.Constants;
import org.eclipse.che.api.account.shared.dto.SubscriptionDescriptor;
import org.eclipse.che.api.runner.dto.ResourcesDescriptor;
import org.eclipse.che.api.runner.gwt.client.RunnerServiceClient;
import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.action.DefaultActionGroup;
import org.eclipse.che.ide.api.action.IdeActions;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.constraints.Constraints;
import org.eclipse.che.ide.api.mvp.Presenter;
import org.eclipse.che.ide.collections.Array;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.util.Config;
import org.eclipse.che.ide.util.loging.Log;
import org.eclipse.che.ide.websocket.MessageBus;
import org.eclipse.che.ide.websocket.WebSocketException;
import org.eclipse.che.ide.websocket.rest.SubscriptionHandler;

import static com.codenvy.ide.subscriptions.client.QueueType.DEDICATED;
import static com.codenvy.ide.subscriptions.client.QueueType.SHARED;

/**
 * Controls what data will be shown on subscription panel.
 *
 * @author Alexander Garagatyi
 * @author Sergii Leschenko
 * @author Vitaliy Guliy
 * @author Kevin Pollet
 * @author Oleksii Orel
 */
@Singleton
public class StatusPanelGroupPresenter implements Presenter, StatusPanelGroupView.ActionDelegate {
    public static final String USED_RESOURCES_CHANGE_CHANEL = "workspace:resources:";

    private final SubscriptionPanelLocalizationConstant locale;

    private final AccountServiceClient accountServiceClient;
    private final RunnerServiceClient  runnerServiceClient;

    private final DtoUnmarshallerFactory dtoUnmarshallerFactory;
    private final MessageBus             messageBus;
    private final AppContext             appContext;
    private final OnPremisesChecker      onPremisesChecker;

    private final ActionManager               actionManager;
    private final MemoryIndicatorAction       memoryIndicatorAction;
    private final QueueTypeIndicatorAction    queueTypeIndicatorAction;
    private final SubscriptionIndicatorAction subscriptionIndicatorAction;
    private final TrademarkLinkAction         trademarkLinkAction;
    private final RedirectLinkAction          redirectLinkAction;

    private final StatusPanelGroupView view;

    @Inject
    public StatusPanelGroupPresenter(StatusPanelGroupView view,
                                     AccountServiceClient accountServiceClient,
                                     RunnerServiceClient runnerServiceClient,
                                     DtoUnmarshallerFactory dtoUnmarshallerFactory,
                                     MessageBus messageBus,
                                     AppContext appContext,
                                     OnPremisesChecker onPremisesChecker,
                                     ActionManager actionManager,
                                     RedirectLinkAction redirectLinkAction,
                                     SubscriptionIndicatorAction subscriptionIndicatorAction,
                                     TrademarkLinkAction trademarkLinkAction,
                                     QueueTypeIndicatorAction queueTypeIndicatorAction,
                                     MemoryIndicatorAction memoryIndicatorAction,
                                     SubscriptionPanelLocalizationConstant locale) {

        this.accountServiceClient = accountServiceClient;
        this.runnerServiceClient = runnerServiceClient;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        this.appContext = appContext;
        this.messageBus = messageBus;
        this.onPremisesChecker = onPremisesChecker;
        this.actionManager = actionManager;
        this.redirectLinkAction = redirectLinkAction;
        this.subscriptionIndicatorAction = subscriptionIndicatorAction;
        this.trademarkLinkAction = trademarkLinkAction;
        this.queueTypeIndicatorAction = queueTypeIndicatorAction;
        this.memoryIndicatorAction = memoryIndicatorAction;
        this.locale = locale;
        this.view = view;

        this.view.setDelegate(this);
    }

    @Override
    public void go(AcceptsOneWidget container) {
        actionManager.registerAction("memoryIndicator", memoryIndicatorAction);
        actionManager.registerAction("queueTypeIndicator", queueTypeIndicatorAction);
        actionManager.registerAction("centerContent", redirectLinkAction);
        actionManager.registerAction("subscriptionTitle", subscriptionIndicatorAction);
        actionManager.registerAction("trademarkLink", trademarkLinkAction);

        DefaultActionGroup rightBottomToolbarGroup = (DefaultActionGroup)actionManager.getAction(IdeActions.GROUP_RIGHT_STATUS_PANEL);
        rightBottomToolbarGroup.add(memoryIndicatorAction, Constraints.FIRST);
        rightBottomToolbarGroup.addSeparator();
        rightBottomToolbarGroup.add(queueTypeIndicatorAction, Constraints.LAST);
        rightBottomToolbarGroup.addSeparator();

        DefaultActionGroup centerBottomToolbarGroup = (DefaultActionGroup)actionManager.getAction(IdeActions.GROUP_CENTER_STATUS_PANEL);
        centerBottomToolbarGroup.add(redirectLinkAction);
        centerBottomToolbarGroup.add(trademarkLinkAction);

        DefaultActionGroup leftBottomToolbarGroup = (DefaultActionGroup)actionManager.getAction(IdeActions.GROUP_LEFT_STATUS_PANEL);
        leftBottomToolbarGroup.add(subscriptionIndicatorAction, Constraints.LAST);


        if (Config.getCurrentWorkspace().isTemporary()) {
            updateResourcesInformation(SHARED);

            if (!appContext.getCurrentUser().isUserPermanent()) {
                redirectLinkAction.updateLinkElement(locale.createAccountActionTitle(),
                                                     Window.Location.getHref() + "?login", true);
            }
        } else {
            checkOnPremisesSubscription();
        }

        container.setWidget(view);
    }

    private void checkOnPremisesSubscription() {
        if (!onPremisesChecker.isOnPremises()) {
            checkSaasSubscription();
        } else {
            subscriptionIndicatorAction.setDescription(locale.onPremisesDescription());
            trademarkLinkAction.updateLinkElement(locale.trademarkTitle(), locale.trademarkUrl());
            updateResourcesInformation(DEDICATED);
        }
    }

    private void checkSaasSubscription() {
        String accountId = Config.getCurrentWorkspace().getAccountId();
        accountServiceClient.getSubscriptionByServiceId(accountId, "Saas", new AsyncRequestCallback<Array<SubscriptionDescriptor>>(
                dtoUnmarshallerFactory.newArrayUnmarshaller(SubscriptionDescriptor.class)) {
            @Override
            protected void onSuccess(Array<SubscriptionDescriptor> result) {
                if (result.isEmpty()) {
                    Log.error(getClass(), "Required Saas subscription is absent");
                    updateSaasInformation("Community", SHARED);
                    return;
                }

                if (result.size() > 1) {
                    Log.error(getClass(), "User has more than 1 Saas subscriptions");
                    updateSaasInformation("Community", SHARED);
                    return;
                }

                SubscriptionDescriptor subscription = result.get(0);

                final String subscriptionPackage = subscription.getProperties().get("Package");
                final QueueType queueType = "Community".equalsIgnoreCase(subscription.getProperties().get("Package")) ? SHARED : DEDICATED;

                updateSaasInformation(subscriptionPackage, queueType);
            }

            @Override
            protected void onFailure(Throwable exception) {
                //User hasn't permission to account
                updateSaasInformation("Community", SHARED);
            }
        });
    }

    private void updateSaasInformation(String subscriptionPackage, QueueType queueType) {
        final String formattedSubscriptionPackage = subscriptionPackage.substring(0, 1).toUpperCase() +
                                                    subscriptionPackage.substring(1).toLowerCase();
        subscriptionIndicatorAction.setDescription("Subscription: SAAS " + formattedSubscriptionPackage);
        updateResourcesInformation(queueType);
    }

    private void updateResourcesInformation(QueueType queueType) {
        queueTypeIndicatorAction.setQueueType(queueType);
        runnerServiceClient.getResources(
                new AsyncRequestCallback<ResourcesDescriptor>(dtoUnmarshallerFactory.newUnmarshaller(ResourcesDescriptor.class)) {
                    @Override
                    protected void onSuccess(ResourcesDescriptor result) {
                        memoryIndicatorAction.setUsedMemorySize(result.getUsedMemory());
                        memoryIndicatorAction.setTotalMemorySize(result.getTotalMemory());
                        try {
                            messageBus.subscribe(USED_RESOURCES_CHANGE_CHANEL + Config.getWorkspaceId(),
                                                 new UsedResourcesUpdater(Config.getWorkspaceId()));
                        } catch (WebSocketException e) {
                            Log.error(getClass(), "Can't open websocket connection");
                        }
                    }

                    @Override
                    protected void onFailure(Throwable exception) {
                        Log.error(getClass(), exception.getMessage());
                    }
                });
        if (Config.getCurrentWorkspace().getAttributes().containsKey(Constants.RESOURCES_LOCKED_PROPERTY)) {
            redirectLinkAction.updateLinkElement(locale.lockDownModeTitle(), locale.lockDownModeUrl(), true);
        }
    }


    private class UsedResourcesUpdater extends SubscriptionHandler<ResourcesDescriptor> {
        private final String workspaceId;

        UsedResourcesUpdater(String workspaceId) {
            super(dtoUnmarshallerFactory.newWSUnmarshaller(ResourcesDescriptor.class));
            this.workspaceId = workspaceId;
        }

        @Override
        protected void onMessageReceived(ResourcesDescriptor result) {
            if (result.getUsedMemory() != null) {
                memoryIndicatorAction.setUsedMemorySize(result.getUsedMemory());
            }

            if (result.getTotalMemory() != null) {
                memoryIndicatorAction.setTotalMemorySize(result.getTotalMemory());
            }
        }

        @Override
        protected void onErrorReceived(Throwable throwable) {
            try {
                messageBus.unsubscribe(USED_RESOURCES_CHANGE_CHANEL + workspaceId, this);
                Log.error(UsedResourcesUpdater.class, throwable);
            } catch (WebSocketException e) {
                Log.error(UsedResourcesUpdater.class, e);
            }
        }
    }
}
