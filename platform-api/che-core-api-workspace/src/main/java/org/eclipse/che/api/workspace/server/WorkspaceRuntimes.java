package org.eclipse.che.api.workspace.server;

import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.model.workspace.RuntimeWorkspace;
import org.eclipse.che.api.workspace.server.model.impl.UsersWorkspaceImpl;

import java.util.ArrayList;
import java.util.List;

/**
 * Workspace runtimes
 *
 * @author gazarenkov
 */
public class WorkspaceRuntimes {


    public RuntimeWorkspace start(UsersWorkspaceImpl ws) throws ForbiddenException, NotFoundException, ServerException {

        return null;

    }


    public boolean share(String id, String userId) throws ForbiddenException, NotFoundException, ServerException {

        return true;

    }

    public boolean unshare(String id, String userId) throws ForbiddenException, NotFoundException, ServerException {

        return true;

    }

    public void stop(String id) throws ForbiddenException, NotFoundException, ServerException {


    }

    /**
     *
     * @param workspaceId
     * @return Workspace or null if not found
     * @throws ServerException
     */
    public RuntimeWorkspace get(String workspaceId) throws ServerException {

        return null;
    }

    public List<RuntimeWorkspace> getList(String userId) throws ForbiddenException, NotFoundException, ServerException {

        return new ArrayList<>();
    }


}
