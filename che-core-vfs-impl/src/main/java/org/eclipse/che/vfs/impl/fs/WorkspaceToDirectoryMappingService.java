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
package org.eclipse.che.vfs.impl.fs;

import com.google.common.base.Function;
import com.google.common.collect.Maps;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.util.Map;

/**
 * @author andrew00x
 */
@Path("vfs/directory-mapping")
public class WorkspaceToDirectoryMappingService {
    @Inject
    private MappedDirectoryLocalFSMountStrategy mappedDirectoryLocalFSMountStrategy;

    @POST
    @Path("{ws-id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> setMountPath(@PathParam("ws-id") String workspaceId, @QueryParam("mountPath") String mountPath) {
        mappedDirectoryLocalFSMountStrategy.setMountPath(workspaceId, new File(mountPath));
        return getDirectoryMapping();
    }

    @DELETE
    @Path("{ws-id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> removeMountPath(@PathParam("ws-id") String workspaceId) {
        mappedDirectoryLocalFSMountStrategy.removeMountPath(workspaceId);
        return getDirectoryMapping();
    }

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> getDirectoryMapping() {
        return Maps.transformValues(mappedDirectoryLocalFSMountStrategy.getDirectoryMapping(), new Function<File, String>() {
            @Override
            public String apply(File input) {
                return input.getAbsolutePath();
            }
        });
    }
}
