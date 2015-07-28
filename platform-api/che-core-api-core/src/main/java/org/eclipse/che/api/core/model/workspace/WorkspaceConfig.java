package org.eclipse.che.api.core.model.workspace;

import org.eclipse.che.api.core.model.machine.Command;

import java.util.List;
import java.util.Map;

/**
 * @author gazarenkov
 */
public interface WorkspaceConfig {

    String getName();

    String getDefaultEnvironment();

    List<? extends Command> getCommands();

    List<? extends ProjectConfig> getProjects();

    Map<String, ? extends Environment> getEnvironments();

    Map<String, String> getAttributes();
}
