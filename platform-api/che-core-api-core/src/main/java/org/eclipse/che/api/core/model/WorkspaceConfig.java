package org.eclipse.che.api.core.model;

import org.eclipse.che.api.core.model.workspace.Command;

import java.util.List;
import java.util.Map;

/**
 * @author gazarenkov
 */
public interface WorkspaceConfig {



//    Set<? extends Membership> getMembers();

    Map<String, String> getAttributes();

    List<? extends Command> getCommands();

    List<? extends ProjectConfig> getProjects();

    Environment getDefaultEnvironment();

    Map <String, ? extends Environment> getEnvironments();
}
