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
package org.eclipse.che.ide.projecttype.wizard.presenter;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.project.shared.dto.ProjectTemplateDescriptor;
import org.eclipse.che.api.project.shared.dto.ProjectTypeDefinition;
import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;
import org.eclipse.che.api.workspace.shared.dto.SourceStorageDto;
import org.eclipse.che.ide.api.project.type.wizard.ProjectWizardMode;
import org.eclipse.che.ide.api.project.type.wizard.ProjectWizardRegistrar;
import org.eclipse.che.ide.api.project.type.wizard.ProjectWizardRegistry;
import org.eclipse.che.ide.api.wizard.Wizard;
import org.eclipse.che.ide.api.wizard.WizardPage;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.projecttype.wizard.ProjectWizard;
import org.eclipse.che.ide.projecttype.wizard.ProjectWizardFactory;
import org.eclipse.che.ide.projecttype.wizard.categoriespage.CategoriesPagePresenter;
import org.eclipse.che.ide.ui.dialogs.DialogFactory;

import javax.validation.constraints.NotNull;
import org.eclipse.che.commons.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.che.ide.api.project.type.wizard.ProjectWizardMode.CREATE;
import static org.eclipse.che.ide.api.project.type.wizard.ProjectWizardMode.CREATE_MODULE;
import static org.eclipse.che.ide.api.project.type.wizard.ProjectWizardMode.IMPORT;
import static org.eclipse.che.ide.api.project.type.wizard.ProjectWizardMode.UPDATE;

/**
 * Presenter for project wizard.
 *
 * @author Evgen Vidolob
 * @author Oleksii Orel
 * @author Sergii Leschenko
 * @author Artem Zatsarynnyy
 */
@Singleton
public class ProjectWizardPresenter implements Wizard.UpdateDelegate,
                                               ProjectWizardView.ActionDelegate,
                                               CategoriesPagePresenter.ProjectTypeSelectionListener,
                                               CategoriesPagePresenter.ProjectTemplateSelectionListener {

    private final ProjectWizardView                         view;
    private final DtoFactory                                dtoFactory;
    private final DialogFactory                             dialogFactory;
    private final ProjectWizardFactory                      projectWizardFactory;
    private final ProjectWizardRegistry                     wizardRegistry;
    private final Provider<CategoriesPagePresenter>         categoriesPageProvider;
    private final Map<ProjectTypeDefinition, ProjectWizard> wizardsCache;
    private       CategoriesPagePresenter                   categoriesPage;
    private       ProjectWizard                             wizard;
    private       ProjectWizard                             importWizard;
    private       WizardPage                                currentPage;

    private ProjectWizardMode wizardMode;
    /** Contains project's path when project wizard opened for updating project. */
    private String            projectPath;

    @Inject
    public ProjectWizardPresenter(ProjectWizardView view,
                                  DtoFactory dtoFactory,
                                  DialogFactory dialogFactory,
                                  ProjectWizardFactory projectWizardFactory,
                                  ProjectWizardRegistry wizardRegistry,
                                  Provider<CategoriesPagePresenter> categoriesPageProvider) {
        this.view = view;
        this.dtoFactory = dtoFactory;
        this.dialogFactory = dialogFactory;
        this.projectWizardFactory = projectWizardFactory;
        this.wizardRegistry = wizardRegistry;
        this.categoriesPageProvider = categoriesPageProvider;
        wizardsCache = new HashMap<>();
        view.setDelegate(this);
    }

    @Override
    public void onBackClicked() {
        final WizardPage prevPage = wizard.navigateToPrevious();
        if (prevPage != null) {
            showPage(prevPage);
        }
    }

    @Override
    public void onNextClicked() {
        final WizardPage nextPage = wizard.navigateToNext();
        if (nextPage != null) {
            showPage(nextPage);
        }
    }

    @Override
    public void onSaveClicked() {
        view.setLoaderVisibility(true);
        wizard.complete(new Wizard.CompleteCallback() {
            @Override
            public void onCompleted() {
                view.close();
            }

            @Override
            public void onFailure(Throwable e) {
                view.setLoaderVisibility(false);
                dialogFactory.createMessageDialog("", e.getMessage(), null).show();
            }
        });
    }

    @Override
    public void onCancelClicked() {
        view.close();
    }

    @Override
    public void updateControls() {
        view.setPreviousButtonEnabled(wizard.hasPrevious());
        view.setNextButtonEnabled(wizard.hasNext() && currentPage.isCompleted());
        view.setFinishButtonEnabled(wizard.canComplete());
    }

    /** Open the project wizard for creating a new project. */
    public void show() {
        resetState();
        wizardMode = CREATE;
        showDialog(null);
    }

    /** Open the project wizard for updating the given {@code project}. */
    public void show(@NotNull ProjectDescriptor project) {
        resetState();
        wizardMode = UPDATE;
        projectPath = project.getPath();
        final ProjectConfigDto dataObject = dtoFactory.createDto(ProjectConfigDto.class)
                                                                          .withType(project.getType())
                                                                          .withName(project.getName())
                                                                          .withDescription(project.getDescription())
                                                                          .withAttributes(new HashMap<>(project.getAttributes()));
        dataObject.setMixins(project.getMixins());
        showDialog(dataObject);
    }

    /** Open the project wizard for creating module from the given {@code folder}. */
    public void show(@NotNull ItemReference folder) {
        resetState();
        wizardMode = CREATE_MODULE;
        projectPath = folder.getPath();
        final ProjectConfigDto dataObject = dtoFactory.createDto(ProjectConfigDto.class)
                                                                          .withName(folder.getName());

        showDialog(dataObject);
    }

    private void resetState() {
        wizardsCache.clear();
        categoriesPage = categoriesPageProvider.get();
        wizardMode = null;
        categoriesPage.setProjectTypeSelectionListener(this);
        categoriesPage.setProjectTemplateSelectionListener(this);
        projectPath = null;
        importWizard = null;
    }

    private void showDialog(@Nullable ProjectConfigDto dataObject) {
        wizard = createDefaultWizard(dataObject, wizardMode);
        final WizardPage<ProjectConfigDto> firstPage = wizard.navigateToFirst();
        if (firstPage != null) {
            showPage(firstPage);
            view.showDialog(wizardMode);
        }
    }

    @Override
    public void onProjectTypeSelected(ProjectTypeDefinition projectType) {
        final ProjectConfigDto prevData = wizard.getDataObject();
        wizard = getWizardForProjectType(projectType);
        wizard.navigateToFirst();
        final ProjectConfigDto newProject = wizard.getDataObject();

        // some values should be shared between wizards for different project types
        newProject.setName(prevData.getName());
        newProject.setDescription(prevData.getDescription());
        newProject.setMixins(prevData.getMixins());
        if (wizardMode == UPDATE) {
            newProject.setAttributes(prevData.getAttributes());
        }

        // set dataObject's values from projectType
        newProject.setType(projectType.getId());
//        newProject.setRecipe(projectType.getDefaultRecipe());
    }

    @Override
    public void onProjectTemplateSelected(ProjectTemplateDescriptor projectTemplate) {
        final ProjectConfigDto prevData = wizard.getDataObject();
        wizard = importWizard == null ? importWizard = createDefaultWizard(null, IMPORT) : importWizard;
        wizard.navigateToFirst();
        final ProjectConfigDto dataObject = wizard.getDataObject();

        // some values should be shared between wizards for different project types
        dataObject.setName(prevData.getName());
        dataObject.setDescription(prevData.getDescription());

        // set dataObject's values from projectTemplate
        dataObject.setType(projectTemplate.getProjectType());
        dataObject.setSource(projectTemplate.getSource());
    }

    /** Creates or returns project wizard for the specified projectType with the given dataObject. */
    private ProjectWizard getWizardForProjectType(@NotNull ProjectTypeDefinition projectType) {
        if (wizardsCache.containsKey(projectType)) {
            return wizardsCache.get(projectType);
        }

        final ProjectWizardRegistrar wizardRegistrar = wizardRegistry.getWizardRegistrar(projectType.getId());
        if (wizardRegistrar == null) {
            // should never occur
            throw new IllegalStateException("WizardRegistrar for the project type " + projectType.getId() + " isn't registered.");
        }

        List<Provider<? extends WizardPage<ProjectConfigDto>>> pageProviders = wizardRegistrar.getWizardPages();
        final ProjectWizard projectWizard = createDefaultWizard(null, wizardMode);
        for (Provider<? extends WizardPage<ProjectConfigDto>> provider : pageProviders) {
            projectWizard.addPage(provider.get(), 1, false);
        }

        wizardsCache.put(projectType, projectWizard);
        return projectWizard;
    }

    /** Creates and returns 'default' project wizard with pre-defined pages only. */
    private ProjectWizard createDefaultWizard(@Nullable ProjectConfigDto dataObject, @NotNull ProjectWizardMode mode) {
        if (dataObject == null) {
            dataObject = dtoFactory.createDto(ProjectConfigDto.class)
                                   .withSource(dtoFactory.createDto(SourceStorageDto.class));
        }

        final ProjectWizard projectWizard = projectWizardFactory.newWizard(dataObject, mode, projectPath);
        projectWizard.setUpdateDelegate(this);

        // add pre-defined pages - first and last
        projectWizard.addPage(categoriesPage);
        return projectWizard;
    }

    private void showPage(@NotNull WizardPage wizardPage) {
        currentPage = wizardPage;
        updateControls();
        view.showPage(currentPage);
    }
}
