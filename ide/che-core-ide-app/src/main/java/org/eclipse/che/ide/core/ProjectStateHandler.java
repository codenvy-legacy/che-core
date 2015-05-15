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
package org.eclipse.che.ide.core;

import elemental.client.Browser;

import com.google.gwt.core.client.Callback;
import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.Window;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;

import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.api.DocumentTitleDecorator;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.event.CloseCurrentProjectEvent;
import org.eclipse.che.ide.api.event.CloseCurrentProjectHandler;
import org.eclipse.che.ide.api.event.ConfigureProjectEvent;
import org.eclipse.che.ide.api.event.ConfigureProjectHandler;
import org.eclipse.che.ide.api.event.OpenProjectEvent;
import org.eclipse.che.ide.api.event.OpenProjectHandler;
import org.eclipse.che.ide.api.event.ProjectActionEvent;
import org.eclipse.che.ide.api.event.ProjectDescriptorChangedEvent;
import org.eclipse.che.ide.api.event.ProjectDescriptorChangedHandler;
import org.eclipse.che.ide.api.event.RefreshProjectTreeEvent;
import org.eclipse.che.ide.core.problemDialog.ProjectProblemDialog;
import org.eclipse.che.ide.projecttype.wizard.presenter.ProjectWizardPresenter;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.rest.Unmarshallable;
import org.eclipse.che.ide.util.Config;
import org.eclipse.che.ide.util.loging.Log;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Component that does some preliminary operations before opening/closing projects.
 * <p/>
 * E.g.:
 * <ul>
 * <li>closing already opened project before opening another one;</li>
 * <li>setting Browser tab's title;</li>
 * <li>rewriting URL in Browser's address bar;</li>
 * <li>setting {@link CurrentProject} to {@link AppContext};</li>
 * <li>etc.</li>
 * </ul>
 *
 * @author Artem Zatsarynnyy
 */
@Singleton
public class ProjectStateHandler implements Component, OpenProjectHandler, CloseCurrentProjectHandler, ProjectDescriptorChangedHandler,
                                            ConfigureProjectHandler {
    private final EventBus                 eventBus;
    private final AppContext               appContext;
    private final ProjectServiceClient     projectServiceClient;
    private final ProjectWizardPresenter   projectWizardPresenter;
    private final DtoUnmarshallerFactory   dtoUnmarshallerFactory;
    private final CoreLocalizationConstant constant;
    private final DocumentTitleDecorator   documentTitleDecorator;

    @Inject
    public ProjectStateHandler(AppContext appContext,
                               EventBus eventBus,
                               ProjectServiceClient projectServiceClient,
                               ProjectWizardPresenter projectWizardPresenter,
                               CoreLocalizationConstant constant,
                               DtoUnmarshallerFactory dtoUnmarshallerFactory,
                               DocumentTitleDecorator documentTitleDecorator) {
        this.eventBus = eventBus;
        this.appContext = appContext;
        this.projectServiceClient = projectServiceClient;
        this.projectWizardPresenter = projectWizardPresenter;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        this.constant = constant;
        this.documentTitleDecorator = documentTitleDecorator;
    }

    @Override
    public void start(Callback<Component, Exception> callback) {
        eventBus.addHandler(OpenProjectEvent.TYPE, this);
        eventBus.addHandler(CloseCurrentProjectEvent.TYPE, this);
        eventBus.addHandler(ProjectDescriptorChangedEvent.TYPE, this);
        eventBus.addHandler(ConfigureProjectEvent.TYPE, this);
        callback.onSuccess(this);
    }

    @Override
    public void onOpenProject(final OpenProjectEvent event) {
        tryOpenProject(event.getProjectName());
    }

    @Override
    public void onCloseCurrentProject(CloseCurrentProjectEvent event) {
        closeCurrentProject(null, false);
    }

    @Override
    public void onProjectDescriptorChanged(ProjectDescriptorChangedEvent event) {
        final String path = event.getProjectDescriptor().getPath();
        final CurrentProject currentProject = appContext.getCurrentProject();
        if (currentProject != null && currentProject.getProjectDescription().getPath().equals(path)) {
            currentProject.setProjectDescription(event.getProjectDescriptor());
        }
    }

    @Override
    public void onConfigureProject(@Nonnull ConfigureProjectEvent event) {
        ProjectDescriptor toConfigure = event.getProject();
        if (toConfigure != null) {
            projectWizardPresenter.show(toConfigure);
        }
    }

    private void closeCurrentProject(CloseCallback closeCallback, boolean closingBeforeOpening) {
        final CurrentProject currentProject = appContext.getCurrentProject();
        if (currentProject == null) {
            return;
        }

        eventBus.fireEvent(ProjectActionEvent.createProjectClosingEvent(currentProject.getRootProject()));

        closeCurrentProject(closingBeforeOpening);
        if (closeCallback != null) {
            closeCallback.onClosed();
        }
    }

    private void tryOpenProject(String projectName) {
        Unmarshallable<ProjectDescriptor> unmarshaller = dtoUnmarshallerFactory.newUnmarshaller(ProjectDescriptor.class);
        projectServiceClient.getProject(projectName, new AsyncRequestCallback<ProjectDescriptor>(unmarshaller) {
            @Override
            protected void onSuccess(final ProjectDescriptor project) {
                if (hasProblems(project)) {
                    if (appContext.getCurrentProject() != null) {
                        closeCurrentProject(new CloseCallback() {
                            @Override
                            public void onClosed() {
                                openProblemProject(project);
                            }
                        }, false);
                    } else {
                        openProblemProject(project);
                    }
                } else {
                    if (appContext.getCurrentProject() != null) {
                        closeCurrentProject(new CloseCallback() {
                            @Override
                            public void onClosed() {
                                openProject(project);
                            }
                        }, true);
                    } else {
                        openProject(project);
                    }
                }
            }

            @Override
            protected void onFailure(Throwable throwable) {
                Log.error(ProjectStateHandler.class, throwable);
            }
        });
    }

    private boolean hasProblems(ProjectDescriptor project) {
        return !project.getProblems().isEmpty();
    }

    private void closeCurrentProject(boolean closingBeforeOpening) {
        final CurrentProject currentProject = appContext.getCurrentProject();
        if (currentProject != null) {
            ProjectDescriptor closedProject = currentProject.getRootProject();

            Document.get().setTitle(documentTitleDecorator.getDocumentTitle());
            rewriteBrowserHistory(null);

            // notify all listeners about current project has been closed
            eventBus.fireEvent(ProjectActionEvent.createProjectClosedEvent(closedProject, closingBeforeOpening));
            appContext.setCurrentProject(null);
        }
    }

    private void openProject(ProjectDescriptor project) {
        appContext.setCurrentProject(new CurrentProject(project));

        Document.get().setTitle(documentTitleDecorator.getDocumentTitle(project.getName()));
        rewriteBrowserHistory(project.getName());

        // notify all listeners about opening project
        eventBus.fireEvent(ProjectActionEvent.createProjectOpenedEvent(project));
    }

    private void openProblemProject(final ProjectDescriptor project) {
        ProjectProblemDialog dialog = new ProjectProblemDialog(constant.projectProblemTitle(),
                                                               constant.projectProblemMessage(),
                                                               getAskHandler(project));

        dialog.show();

        eventBus.fireEvent(new RefreshProjectTreeEvent());
    }

    private ProjectProblemDialog.AskHandler getAskHandler(final ProjectDescriptor project) {
        return new ProjectProblemDialog.AskHandler() {
            @Override
            public void onConfigure() {
                eventBus.fireEvent(new ConfigureProjectEvent(project));
            }
        };
    }

    private void rewriteBrowserHistory(@Nullable String projectName) {
        StringBuilder url = new StringBuilder();
        url.append(Config.getContext()).append('/').append(Config.getWorkspaceName());
        if (projectName != null) {
            url.append('/').append(projectName);
        }
        Browser.getWindow().getHistory().replaceState(null, Window.getTitle(), url.toString());
    }

    private interface CloseCallback {
        void onClosed();
    }

}
