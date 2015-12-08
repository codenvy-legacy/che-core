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
package org.eclipse.che.ide.projectimport.wizard;

import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.inject.Inject;

import org.eclipse.che.api.machine.gwt.client.ExtServerStateController;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.api.promises.client.OperationException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseError;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.notification.Notification;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.api.project.wizard.ImportProjectNotificationSubscriber;
import org.eclipse.che.ide.commons.exception.UnmarshallerException;
import org.eclipse.che.ide.util.loging.Log;
import org.eclipse.che.ide.websocket.Message;
import org.eclipse.che.ide.websocket.MessageBus;
import org.eclipse.che.ide.websocket.WebSocketException;
import org.eclipse.che.ide.websocket.rest.SubscriptionHandler;

/**
 * Subscribes on import project notifications.
 * It can be produced by {@code ImportProjectNotificationSubscriberFactory}
 *
 * @author Anton Korneta
 */
public class ImportProjectNotificationSubscriberImpl implements ImportProjectNotificationSubscriber {

    private final Operation<PromiseError>  logErrorHandler;
    private final CoreLocalizationConstant locale;
    private final NotificationManager      notificationManager;
    private final String                   workspaceId;
    private final Promise<MessageBus>      messageBusPromise;

    private String                      wsChannel;
    private String                      projectName;
    private Notification                notification;
    private SubscriptionHandler<String> subscriptionHandler;

    @Inject
    public ImportProjectNotificationSubscriberImpl(CoreLocalizationConstant locale,
                                                   AppContext appContext,
                                                   NotificationManager notificationManager,
                                                   ExtServerStateController extServerStateController) {
        this.locale = locale;
        this.notificationManager = notificationManager;
        this.workspaceId = appContext.getWorkspace().getId();
        this.messageBusPromise = extServerStateController.getMessageBus();
        this.logErrorHandler = new Operation<PromiseError>() {
            @Override
            public void apply(PromiseError error) throws OperationException {
                Log.error(ImportProjectNotificationSubscriberImpl.class, error);
            }
        };
    }

    @Override
    public void subscribe(final String projectName) {
        this.notification = new Notification(locale.importingProject(projectName), Notification.Status.PROGRESS, true);
        subscribe(projectName, notification);
        notificationManager.showNotification(notification);
    }

    @Override
    public void subscribe(final String projectName, final Notification existingNotification) {
        this.projectName = projectName;
        this.wsChannel = "importProject:output:" + workspaceId + ":" + projectName;
        this.notification = existingNotification;
        this.subscriptionHandler = new SubscriptionHandler<String>(new LineUnmarshaller()) {
            @Override
            protected void onMessageReceived(String result) {
                notification.setMessage(locale.importingProject(projectName) + " " + result);
            }

            @Override
            protected void onErrorReceived(final Throwable throwable) {
                messageBusPromise.then(new Operation<MessageBus>() {
                    @Override
                    public void apply(MessageBus messageBus) throws OperationException {
                        try {
                            messageBus.unsubscribe(wsChannel, subscriptionHandler);
                        } catch (WebSocketException e) {
                            Log.error(getClass(), e);
                        }
                        notification.setType(Notification.Type.ERROR);
                        notification.setImportant(true);
                        notification.setMessage(locale.importProjectMessageFailure(projectName) + " " + throwable.getMessage());
                        Log.error(getClass(), throwable);
                    }
                }).catchError(logErrorHandler);
            }
        };

        messageBusPromise.then(new Operation<MessageBus>() {
            @Override
            public void apply(final MessageBus messageBus) throws OperationException {
                try {
                    messageBus.subscribe(wsChannel, subscriptionHandler);
                } catch (WebSocketException wsEx) {
                    Log.error(ImportProjectNotificationSubscriberImpl.class, wsEx);
                }
            }
        }).catchError(logErrorHandler);
    }

    @Override
    public void onSuccess() {
        messageBusPromise.then(new Operation<MessageBus>() {
            @Override
            public void apply(MessageBus messageBus) throws OperationException {
                try {
                    messageBus.unsubscribe(wsChannel, subscriptionHandler);
                } catch (WebSocketException e) {
                    Log.error(getClass(), e);
                }
                notification.setStatus(Notification.Status.FINISHED);
                notification.setMessage(locale.importProjectMessageSuccess(projectName));
            }
        }).catchError(logErrorHandler);
    }

    @Override
    public void onFailure(final String errorMessage) {
        messageBusPromise.then(new Operation<MessageBus>() {
            @Override
            public void apply(MessageBus messageBus) throws OperationException {
                try {
                    messageBus.unsubscribe(wsChannel, subscriptionHandler);
                } catch (WebSocketException e) {
                    Log.error(getClass(), e);
                }
                notification.setType(Notification.Type.ERROR);
                notification.setStatus(Notification.Status.FINISHED);
                notification.setImportant(true);
                notification.setMessage(errorMessage);
            }
        }).catchError(logErrorHandler);
    }

    static class LineUnmarshaller implements org.eclipse.che.ide.websocket.rest.Unmarshallable<String> {
        private String line;

        @Override
        public void unmarshal(Message response) throws UnmarshallerException {
            JSONObject jsonObject = JSONParser.parseStrict(response.getBody()).isObject();
            if (jsonObject == null) {
                return;
            }
            if (jsonObject.containsKey("line")) {
                line = jsonObject.get("line").isString().stringValue();
            }
        }

        @Override
        public String getPayload() {
            return line;
        }
    }

}
