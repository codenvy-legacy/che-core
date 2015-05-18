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
package org.eclipse.che.ide.client;

import com.google.gwt.core.client.Callback;
import com.google.inject.Provider;

import elemental.client.Browser;
import elemental.events.Event;
import elemental.events.EventListener;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.SimpleLayoutPanel;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;

import org.eclipse.che.api.analytics.logger.EventLogger;
//import org.eclipse.che.api.factory.dto.Ide;
import org.eclipse.che.ide.api.DocumentTitleDecorator;
import org.eclipse.che.ide.api.action.Action;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.action.Presentation;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.event.OpenProjectEvent;
import org.eclipse.che.ide.api.event.ProjectActionEvent;
import org.eclipse.che.ide.api.event.ProjectActionHandler;
import org.eclipse.che.ide.api.event.WindowActionEvent;
import org.eclipse.che.ide.core.Component;
import org.eclipse.che.ide.logger.AnalyticsEventLoggerExt;
import org.eclipse.che.ide.statepersistance.AppStateManager;
import org.eclipse.che.ide.ui.toolbar.PresentationFactory;
import org.eclipse.che.ide.util.Config;
import org.eclipse.che.ide.util.loging.Log;
import org.eclipse.che.ide.workspace.WorkspacePresenter;


import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Performs initial application startup.
 *
 * @author Nikolay Zamosenchuk
 */
@Singleton
public class BootstrapController {

    private final AnalyticsEventLoggerExt      analyticsEventLoggerExt;
    private final Provider<WorkspacePresenter> workspaceProvider;
    private final ExtensionInitializer         extensionInitializer;
    private final EventBus                     eventBus;
    private final ActionManager                actionManager;
    private final AppCloseHandler              appCloseHandler;
    private final PresentationFactory          presentationFactory;
    private final AppContext                   appContext;
    private final DocumentTitleDecorator       documentTitleDecorator;
    private final AppStateManager              appStateManager;
    HandlerRegistration handlerRegistration = null;


    /** Create controller. */
    @Inject
    public BootstrapController(Provider<WorkspacePresenter> workspaceProvider,
                               ExtensionInitializer extensionInitializer,
                               DtoRegistrar dtoRegistrar,
                               AnalyticsEventLoggerExt analyticsEventLoggerExt,
                               EventBus eventBus,
                               AppContext appContext,
                               ActionManager actionManager,
                               AppCloseHandler appCloseHandler,
                               AppStateManager appStateManager,
                               DocumentTitleDecorator documentTitleDecorator) {
        this.workspaceProvider = workspaceProvider;
        this.extensionInitializer = extensionInitializer;
        this.eventBus = eventBus;
        this.appContext = appContext;
        this.actionManager = actionManager;
        this.analyticsEventLoggerExt = analyticsEventLoggerExt;
        this.appCloseHandler = appCloseHandler;
        this.documentTitleDecorator = documentTitleDecorator;
        this.appStateManager = appStateManager;

        presentationFactory = new PresentationFactory();

        // Register DTO providers
        dtoRegistrar.registerDtoProviders();
    }

    @Inject
    void startComponents(Map<String, Provider<Component>> startableMap) {
        Iterator<Map.Entry<String, Provider<Component>>> iterator = startableMap.entrySet().iterator();
        startComponent(iterator);
    }

    private void startComponent(final Iterator<Map.Entry<String, Provider<Component>>> iterator) {
        if (iterator.hasNext()) {
            final Map.Entry<String, Provider<Component>> componentEntry = iterator.next();
//            Scheduler.get().scheduleDeferred(new ScheduledCommand() {
//                @Override
//                public void execute() {
            componentEntry.getValue().get().start(new Callback<Component, Exception>() {
                @Override
                public void onFailure(Exception reason) {
                    Log.error(BootstrapController.class, reason);
                    initializationFailed(reason.getMessage());
                }

                @Override
                public void onSuccess(Component result) {
                    startComponent(iterator);
                }
            });

//                }
//            });
        } else {
            startExtensions();
        }
    }

    /** Start extensions */
    private void startExtensions() {
        Scheduler.get().scheduleDeferred(new ScheduledCommand() {
            @Override
            public void execute() {
                // Instantiate extensions
                extensionInitializer.startExtensions();

                Scheduler.get().scheduleDeferred(new ScheduledCommand() {
                    @Override
                    public void execute() {
                        displayIDE();
                        boolean openLastProject = Config.getProjectName() == null && Config.getStartupParam("action") == null;
                        appStateManager.start(openLastProject);
                    }
                });
            }
        });
    }

    /** Displays the IDE */
    private void displayIDE() {
        // Start UI
        SimpleLayoutPanel mainPanel = new SimpleLayoutPanel();

        RootLayoutPanel.get().add(mainPanel);

        // Make sure the root panel creates its own stacking context
        RootLayoutPanel.get().getElement().getStyle().setZIndex(0);

        WorkspacePresenter workspacePresenter = workspaceProvider.get();

        // Display 'Update extension' button if IDE is launched in SDK runner
        workspacePresenter.setUpdateButtonVisibility(Config.getStartupParam("h") != null && Config.getStartupParam("p") != null);

        // Display IDE
        workspacePresenter.go(mainPanel);

        Document.get().setTitle(documentTitleDecorator.getDocumentTitle());

        processStartupParameters();

        final AnalyticsSessions analyticsSessions = new AnalyticsSessions();

        // Bind browser's window events
        Window.addWindowClosingHandler(new Window.ClosingHandler() {
            @Override
            public void onWindowClosing(Window.ClosingEvent event) {
                onWindowClose(analyticsSessions);
                eventBus.fireEvent(WindowActionEvent.createWindowClosingEvent(event));
            }
        });
        Window.addCloseHandler(new CloseHandler<Window>() {
            @Override
            public void onClose(CloseEvent<Window> event) {
                onWindowClose(analyticsSessions);
                eventBus.fireEvent(WindowActionEvent.createWindowClosedEvent());
            }
        });

        elemental.html.Window window = Browser.getWindow();

        window.addEventListener(Event.FOCUS, new EventListener() {
            @Override
            public void handleEvent(Event evt) {
                onSessionUsage(analyticsSessions, false);
            }
        }, true);

        window.addEventListener(Event.BLUR, new EventListener() {
            @Override
            public void handleEvent(Event evt) {
                onSessionUsage(analyticsSessions, false);
            }
        }, true);

        onSessionUsage(analyticsSessions, true); // This is necessary to forcibly print the very first event
    }

    private void onSessionUsage(AnalyticsSessions analyticsSessions, boolean force) {
        if (analyticsSessions.getIdleUsageTime() > 600000) { // 10 min
            analyticsSessions.makeNew();
            logSessionUsageEvent(analyticsSessions, true);
        } else {
            logSessionUsageEvent(analyticsSessions, force);
            analyticsSessions.updateUsageTime();
        }
    }

    private void onWindowClose(AnalyticsSessions analyticsSessions) {
        if (analyticsSessions.getIdleUsageTime() <= 60000) { // 1 min
            logSessionUsageEvent(analyticsSessions, true);
            analyticsSessions.updateUsageTime();
        }
    }

    private void logSessionUsageEvent(AnalyticsSessions analyticsSessions, boolean force) {
        if (force || analyticsSessions.getIdleLogTime() > 60000) { // 1 min, don't log frequently than once per minute
            Map<String, String> parameters = new HashMap<>();
            parameters.put("SESSION-ID", analyticsSessions.getId());

            analyticsEventLoggerExt.logEvent(EventLogger.SESSION_USAGE, parameters);

            if (Config.getCurrentWorkspace() != null && Config.getCurrentWorkspace().isTemporary()) {
                analyticsEventLoggerExt.logEvent(EventLogger.SESSION_FACTORY_USAGE, parameters);
            }

            analyticsSessions.updateLogTime();
        }
    }


    private void processStartupParameters() {
        final String projectNameToOpen = Config.getProjectName();
        if (projectNameToOpen != null) {
            handlerRegistration = eventBus.addHandler(ProjectActionEvent.TYPE, getStartupActionHandler());
            eventBus.fireEvent(new OpenProjectEvent(projectNameToOpen));
        } else {
            processStartupAction();

//            handlerRegistration = eventBus.addHandler(ProjectActionEvent.TYPE, getFactoryActionHandler());
        }

//        if (appContext.getFactory() != null && appContext.getFactory().getIde() != null) {
//            final Ide ide = appContext.getFactory().getIde();
//
//            if (ide.getOnAppClosed() != null && ide.getOnAppClosed().getActions() != null) {
//                appCloseHandler.performBeforeClose(ide.getOnAppClosed().getActions());
//            }
//
//            if (ide.getOnAppLoaded() != null && ide.getOnAppLoaded().getActions() != null) {
//                performActions(ide.getOnAppLoaded().getActions());
//            }
//        }
    }

//    private ProjectActionHandler getFactoryActionHandler() {
//        return new ProjectActionHandler() {
//            @Override
//            public void onProjectOpened(ProjectActionEvent event) {
//                if (handlerRegistration != null) {
//                    handlerRegistration.removeHandler();
//                }
//
//                if (appContext.getFactory() != null && appContext.getFactory().getIde() != null
//                    && appContext.getFactory().getIde().getOnProjectOpened() != null
//                    && appContext.getFactory().getIde().getOnProjectOpened().getActions() != null) {
//
//                    performActions(appContext.getFactory().getIde().getOnProjectOpened().getActions());
//                }
//            }
//
//            @Override
//            public void onProjectClosing(ProjectActionEvent event) {
//            }
//
//            @Override
//            public void onProjectClosed(ProjectActionEvent event) {
//                //do nothing
//            }
//        };
//    }

    private ProjectActionHandler getStartupActionHandler() {
        return new ProjectActionHandler() {
            //process action only after opening project
            @Override
            public void onProjectOpened(ProjectActionEvent event) {
                processStartupAction();
            }

            @Override
            public void onProjectClosing(ProjectActionEvent event) {
            }

            @Override
            public void onProjectClosed(ProjectActionEvent event) {
            }
        };
    }

    private void processStartupAction() {
        final String startupAction = Config.getStartupParam("action");
        if (startupAction != null) {
            performAction(startupAction);
        }
    }

//    private void performActions(List<org.eclipse.che.api.factory.dto.Action> actions) {
//        for (org.eclipse.che.api.factory.dto.Action action : actions) {
//            performAction(action.getId(), action.getProperties());
//        }
//    }

    private void performAction(String actionId) {
        performAction(actionId, null);
    }

    private void performAction(String actionId, Map<String, String> parameters) {
        Action action = actionManager.getAction(actionId);

        if (action == null) {
            return;
        }

        final Presentation presentation = presentationFactory.getPresentation(action);

        ActionEvent e = new ActionEvent("", presentation, actionManager, 0, parameters);
        action.update(e);

        if (presentation.isEnabled() && presentation.isVisible()) {
            action.actionPerformed(e);
        }
    }

    /**
     * Handles any of initialization errors.
     * Tries to call predefined IDE.eventHandlers.ideInitializationFailed function.
     *
     * @param message
     *         error message
     */
    private native void initializationFailed(String message) /*-{
        try {
            $wnd.IDE.eventHandlers.initializationFailed(message);
        } catch (e) {
            console.log(e.message);
        }
    }-*/;

}
