package org.eclipse.che.api.core.model;

/**
 * @author gazarenkov
 */
public interface UsersWorkspace extends WorkspaceConfig {

    String getId();

    String getName();

    void setName(String name);

    String getOwner();

    boolean isRunning();

}
