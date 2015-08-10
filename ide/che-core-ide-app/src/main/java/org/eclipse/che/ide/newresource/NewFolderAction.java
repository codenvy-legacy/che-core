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
package org.eclipse.che.ide.newresource;

import org.eclipse.che.api.project.shared.dto.ItemReference;

import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.FunctionException;
import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.Resources;

import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.project.node.FolderReferenceNode;
import org.eclipse.che.ide.project.node.ItemReferenceBasedNode;
import org.eclipse.che.ide.project.node.ResourceBasedNode;
import org.eclipse.che.ide.api.project.node.HasStorablePath;
import org.eclipse.che.ide.ui.dialogs.InputCallback;
import org.eclipse.che.ide.ui.dialogs.input.InputDialog;
import org.eclipse.che.ide.api.project.node.Node;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Action to create new folder.
 *
 * @author Artem Zatsarynnyy
 */
@Singleton
public class NewFolderAction extends AbstractNewResourceAction {
    private CoreLocalizationConstant localizationConstant;

    @Inject
    public NewFolderAction(CoreLocalizationConstant localizationConstant, Resources resources) {
        super(localizationConstant.actionNewFolderTitle(),
              localizationConstant.actionNewFolderDescription(),
              resources.defaultFolder());
        this.localizationConstant = localizationConstant;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        eventLogger.log(this);

        InputDialog inputDialog = dialogFactory.createInputDialog(
                localizationConstant.newResourceTitle(localizationConstant.actionNewFolderTitle()),
                localizationConstant.newResourceLabel(localizationConstant.actionNewFolderTitle().toLowerCase()),
                new InputCallback() {
                    @Override
                    public void accepted(String value) {
                        onAccepted(value);
                    }
                }, null).withValidator(folderNameValidator);
        inputDialog.show();
    }

    private void onAccepted(String value) {
        final ResourceBasedNode<?> parent = getResourceBasedNode();

        if (parent == null) {
            throw new IllegalStateException("No selected parent.");
        }

        final String folderPath = ((HasStorablePath)parent).getStorablePath() + '/' + value;

        projectServiceClient.createFolder(folderPath, createCallback(parent));
    }

    @Nonnull
    @Override
    protected Function<List<Node>, ItemReferenceBasedNode> iterateAndFindCreatedNode(@Nonnull final ItemReference itemReference) {
        return new Function<List<Node>, ItemReferenceBasedNode>() {
            @Override
            public ItemReferenceBasedNode apply(List<Node> nodes) throws FunctionException {
                if (nodes.isEmpty()) {
                    return null;
                }

                for (Node node : nodes) {
                    if (node instanceof FolderReferenceNode && ((FolderReferenceNode)node).getData().equals(itemReference)) {
                        return (FolderReferenceNode)node;
                    }
                }

                return null;
            }
        };
    }
}
