package org.eclipse.che.ide.actions;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.action.ProjectAction;
import org.eclipse.che.ide.part.explorer.project.NewProjectExplorerPresenter;

/**
 * @author Vlad Zhukovskiy
 */
@Singleton
public class CollapseAllAction extends ProjectAction {

    private NewProjectExplorerPresenter projectExplorer;

    @Inject
    public CollapseAllAction(NewProjectExplorerPresenter projectExplorer) {
        super("Collapse All");
        this.projectExplorer = projectExplorer;
    }

    @Override
    protected void updateProjectAction(ActionEvent e) {
        //stub
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        projectExplorer.collapseAll();
    }
}
