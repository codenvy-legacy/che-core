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
package org.eclipse.che.api.factory.converter;

import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.factory.dto.Action;
import org.eclipse.che.api.factory.dto.Actions;
import org.eclipse.che.api.factory.dto.Factory;
import org.eclipse.che.api.factory.dto.Ide;
import org.eclipse.che.api.factory.dto.OnAppClosed;
import org.eclipse.che.api.factory.dto.OnAppLoaded;
import org.eclipse.che.api.factory.dto.OnProjectOpened;
import org.eclipse.che.api.factory.dto.WelcomePage;
import org.eclipse.che.api.vfs.shared.dto.ReplacementSet;
import org.eclipse.che.api.vfs.shared.dto.Variable;
import org.eclipse.che.dto.server.DtoFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

/**
 * Convert 2.0 actions to 2.1 format.
 *
 * @author Sergii Kabashniuk
 * @author Sergii Leschenko
 */
public class ActionsConverter implements LegacyConverter {
    private final DtoFactory dto = DtoFactory.getInstance();

    @Override
    public void convert(Factory factory) throws ApiException {
        if (factory.getActions() == null) {
            //nothing to convert
            return;
        }

        if (factory.getIde() != null) {
            throw new ConflictException("Factory contains both 2.0 and 2.1 actions");
        }

        factory.setIde(dto.createDto(Ide.class));

        Actions actions = factory.getActions();

        final WelcomePage welcomePage = actions.getWelcome();
        if (welcomePage != null) {
            Map<String, String> welcomeProperties = new HashMap<>();

            if (welcomePage.getAuthenticated() != null) {
                welcomeProperties.put("authenticatedTitle", welcomePage.getAuthenticated().getTitle());
                welcomeProperties.put("authenticatedContentUrl", welcomePage.getAuthenticated().getContenturl());
                welcomeProperties.put("authenticatedNotification", welcomePage.getAuthenticated().getNotification());
            }

            if (welcomePage.getNonauthenticated() != null) {
                welcomeProperties.put("nonAuthenticatedTitle", welcomePage.getNonauthenticated().getTitle());
                welcomeProperties.put("nonAuthenticatedContentUrl", welcomePage.getNonauthenticated().getContenturl());
                welcomeProperties.put("nonAuthenticatedNotification", welcomePage.getNonauthenticated().getNotification());
            }

            addToOnAppLoaded(factory, singletonList(dto.createDto(Action.class)
                                                       .withId("openWelcomePage")
                                                       .withProperties(welcomeProperties)));
        }

        final String openFile = actions.getOpenFile();
        if (openFile != null) {
            addToOnProjectOpened(factory, singletonList(dto.createDto(Action.class)
                                                           .withId("openFile")
                                                           .withProperties(singletonMap("file", openFile))));
        }

        final List<ReplacementSet> replacement = actions.getFindReplace();
        if (replacement != null) {
            List<Action> replacementActions = new ArrayList<>();
            for (ReplacementSet replacementSet : replacement) {
                for (String file : replacementSet.getFiles()) {
                    for (Variable variable : replacementSet.getEntries()) {
                        Map<String, String> findReplaceProperties = new HashMap<>();
                        findReplaceProperties.put("in", file);
                        findReplaceProperties.put("find", variable.getFind());
                        findReplaceProperties.put("replace", variable.getReplace());
                        findReplaceProperties.put("replaceMode", variable.getReplacemode());

                        replacementActions.add(dto.createDto(Action.class)
                                                  .withId("findReplace")
                                                  .withProperties(findReplaceProperties));
                    }
                }
            }
            addToOnProjectOpened(factory, replacementActions);
        }

        final Boolean warnOnClose = actions.getWarnOnClose();
        if (warnOnClose != null && warnOnClose) {
            addToOnAppClosed(factory, singletonList(dto.createDto(Action.class).withId("warnOnClose")));
        }

        factory.setActions(null);
    }

    private void addToOnAppLoaded(Factory factory, List<Action> actions) {
        OnAppLoaded onAppLoaded = factory.getIde().getOnAppLoaded();
        if (onAppLoaded == null) {
            onAppLoaded = dto.createDto(OnAppLoaded.class);
            factory.getIde().setOnAppLoaded(onAppLoaded);
        }
        if (actions != null) {
            List<Action> currentActions = onAppLoaded.getActions();
            if (currentActions == null) {
                currentActions = new ArrayList<>();
                onAppLoaded.setActions(currentActions);
            }
            currentActions.addAll(actions);
        }
    }

    private void addToOnAppClosed(Factory factory, List<Action> actions) {
        OnAppClosed onAppClosed = factory.getIde().getOnAppClosed();
        if (onAppClosed == null) {
            onAppClosed = dto.createDto(OnAppClosed.class);
            factory.getIde().setOnAppClosed(onAppClosed);
        }
        if (actions != null) {
            List<Action> currentActions = onAppClosed.getActions();
            if (currentActions == null) {
                currentActions = new ArrayList<>();
                onAppClosed.setActions(currentActions);
            }
            currentActions.addAll(actions);
        }
    }

    private void addToOnProjectOpened(Factory factory, List<Action> actions) {
        OnProjectOpened onProjectOpened = factory.getIde().getOnProjectOpened();
        if (onProjectOpened == null) {
            onProjectOpened = dto.createDto(OnProjectOpened.class);
            factory.getIde().setOnProjectOpened(onProjectOpened);
        }

        if (actions != null) {
            List<Action> currentActions = onProjectOpened.getActions();
            if (currentActions == null) {
                currentActions = new ArrayList<>();
                onProjectOpened.setActions(currentActions);
            }
            currentActions.addAll(actions);
        }
    }
}
