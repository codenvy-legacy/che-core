package org.eclipse.che.api.workspace.server;

import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.model.UsersWorkspace;
import org.eclipse.che.api.core.model.Workspace;
import org.eclipse.che.api.core.model.WorkspaceConfig;
import org.eclipse.che.api.workspace.server.spi.WorkspaceDo;

import java.util.ArrayList;
import java.util.List;

/**
 * Workspace runtimes
 *
 * @author gazarenkov
 */
public class WorkspaceRuntimes {


    public Workspace start(WorkspaceDo ws) throws ForbiddenException, NotFoundException, ServerException {

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
    public Workspace get(String workspaceId) throws ServerException {

        return null;
    }

    public List<Workspace> getList(String userId) throws ForbiddenException, NotFoundException, ServerException {

        return new ArrayList<>();
    }


}
