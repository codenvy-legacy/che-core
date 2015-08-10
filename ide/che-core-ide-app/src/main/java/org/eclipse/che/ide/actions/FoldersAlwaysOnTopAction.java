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
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.action.ToggleAction;
import org.eclipse.che.ide.part.explorer.project.NewProjectExplorerPresenter;
import org.eclipse.che.ide.part.explorer.project.NewProjectExplorerViewImpl;
import org.eclipse.che.ide.ui.smartTree.SortDir;
import org.eclipse.che.ide.ui.smartTree.TreeNodeStorage;
import org.eclipse.che.ide.ui.smartTree.TreeNodeStorage.StoreSortInfo;
import org.eclipse.che.ide.ui.smartTree.sorting.AlphabeticalFilter;
import org.eclipse.che.ide.ui.smartTree.sorting.FoldersOnTopFilter;
import org.eclipse.che.ide.util.loging.Log;

import java.util.List;

import static org.eclipse.che.ide.ui.smartTree.SortDir.ASC;

/**
 * @author Vlad Zhukovskiy
 */
@Singleton
public class FoldersAlwaysOnTopAction extends ToggleAction {
    private final NewProjectExplorerViewImpl view;
    private static StoreSortInfo DEFAULT      = new StoreSortInfo(new FoldersOnTopFilter(), ASC);
    private static StoreSortInfo ALPHABETICAL = new StoreSortInfo(new AlphabeticalFilter(), ASC);

    @Inject
    public FoldersAlwaysOnTopAction(NewProjectExplorerViewImpl view) {
        super("Folders Always on Top");
        this.view = view;
    }

    @Override
    public boolean isSelected(ActionEvent e) {
        return Iterables.any(view.getSortInfo(), matching());
    }

    @Override
    public void setSelected(ActionEvent e, boolean state) {
        view.getSortInfo().clear();

        if (state) {
            view.getSortInfo().add(DEFAULT);
        } else {
            view.getSortInfo().add(ALPHABETICAL);
        }

        view.onApplySort();
    }

    private Predicate<StoreSortInfo> matching() {
        return new Predicate<StoreSortInfo>() {
            @Override
            public boolean apply(StoreSortInfo predicate) {
                return predicate.getComparator() instanceof FoldersOnTopFilter;
            }
        };
    }
}
