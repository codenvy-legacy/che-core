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
package org.eclipse.che.ide.core.problemDialog;

import org.eclipse.che.api.project.shared.dto.ProjectTypeDefinition;
import org.eclipse.che.api.project.shared.dto.SourceEstimation;
import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.api.project.type.ProjectTypeRegistry;

import com.google.inject.Inject;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * The purpose of this class is show dialog when we can't recognize project type.
 *
 * @author Roman Nikitenko
 */
public class ProjectProblemDialogPresenter implements ProjectProblemDialogView.ActionDelegate {

    private final ProjectProblemDialogView view;
    private final ProjectTypeRegistry      projectTypeRegistry;
    private final CoreLocalizationConstant localizedConstant;

    protected ProjectProblemDialogCallback callback;
    protected List<SourceEstimation>       estimatedTypes;

    @Inject
    public ProjectProblemDialogPresenter(final @NotNull ProjectProblemDialogView view,
                                         final ProjectTypeRegistry projectTypeRegistry,
                                         final CoreLocalizationConstant localizedConstant) {
        this.view = view;
        this.projectTypeRegistry = projectTypeRegistry;
        this.localizedConstant = localizedConstant;
        this.view.setDelegate(this);
    }

    /**
     * Displays new dialog.
     *
     * @param sourceEstimationList
     *         the list of estimated project types for display in popup window
     * @param callback
     *         the callback that call after user interact
     */
    public void showDialog(@Nullable List<SourceEstimation> sourceEstimationList, @NotNull ProjectProblemDialogCallback callback) {
        this.callback = callback;
        if (sourceEstimationList == null || sourceEstimationList.isEmpty()) {
            estimatedTypes = null;
            view.showDialog(null);
            return;
        }

        estimatedTypes = new ArrayList<>(sourceEstimationList.size());
        List<String> estimatedTypeNames = new ArrayList<>(sourceEstimationList.size());
        for (SourceEstimation estimatedType : sourceEstimationList) {
            ProjectTypeDefinition projectType = projectTypeRegistry.getProjectType(estimatedType.getType());
            if (estimatedType.isPrimaryable() && projectType != null) {
                this.estimatedTypes.add(estimatedType);
                estimatedTypeNames.add(projectType.getDisplayName());
            }
        }
        view.showDialog(estimatedTypeNames);
    }

    @Override
    public void onOpenAsIs() {
        view.hide();
        callback.onOpenAsIs();
    }

    @Override
    public void onOpenAs() {
        view.hide();
        callback.onOpenAs(getSourceEstimation());
    }

    @Override
    public void onConfigure() {
        view.hide();
        callback.onConfigure(getSourceEstimation());
    }

    @Override
    public void onEnterClicked() {
        view.hide();
        callback.onConfigure(getSourceEstimation());
    }

    @Override
    public void onSelectedTypeChanged(String projectType) {
        view.setOpenAsButtonTitle(localizedConstant.projectProblemOpenAsButtonTitle(projectType));
        view.setMessage(localizedConstant.projectProblemPressingButtonsMessage(projectType));
    }

    @Nullable
    private SourceEstimation getSourceEstimation() {
        if (estimatedTypes == null || estimatedTypes.isEmpty()) {
            return null;
        }
        if (estimatedTypes.size() == 1) {
            return estimatedTypes.get(0);
        }
        return estimatedTypes.get(view.getSelectedTypeIndex());
    }
}
