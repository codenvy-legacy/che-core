package org.eclipse.che.api.core.model.workspace;

/**
 * @author gazarenkov
 */
public interface UsersWorkspace extends WorkspaceConfig {

    String getId();

    String getOwner();

}
