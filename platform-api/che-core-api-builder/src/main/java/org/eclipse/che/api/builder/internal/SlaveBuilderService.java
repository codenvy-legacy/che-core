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
package org.eclipse.che.api.builder.internal;

import org.eclipse.che.api.builder.BuildStatus;
import org.eclipse.che.api.builder.BuilderException;
import org.eclipse.che.api.builder.dto.BuildRequest;
import org.eclipse.che.api.builder.dto.BuildTaskDescriptor;
import org.eclipse.che.api.builder.dto.BuilderDescriptor;
import org.eclipse.che.api.builder.dto.BuilderState;
import org.eclipse.che.api.builder.dto.DependencyRequest;
import org.eclipse.che.api.builder.dto.ServerState;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.rest.Service;
import org.eclipse.che.api.core.rest.annotations.Description;
import org.eclipse.che.api.core.rest.annotations.GenerateLink;
import org.eclipse.che.api.core.rest.annotations.Required;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.core.util.ContentTypeGuesser;
import org.eclipse.che.api.core.util.SystemInfo;
import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.api.vfs.server.util.DeleteOnCloseFileInputStream;
import org.eclipse.che.commons.lang.Size;
import org.eclipse.che.commons.lang.TarUtils;
import org.eclipse.che.commons.lang.ZipUtils;
import org.eclipse.che.dto.server.DtoFactory;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriBuilder;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * RESTful interface for Builder.
 *
 * @author andrew00x
 */
@Path("internal/builder")
public final class SlaveBuilderService extends Service {
    @Inject
    private BuilderRegistry builders;

    /** Get list of available Builders which can be accessible over this SlaveBuilderService. */
    @GenerateLink(rel = Constants.LINK_REL_AVAILABLE_BUILDERS)
    @GET
    @Path("available")
    @Produces(MediaType.APPLICATION_JSON)
    public List<BuilderDescriptor> availableBuilders() {
        final Set<Builder> all = builders.getAll();
        final List<BuilderDescriptor> list = new ArrayList<>(all.size());
        final DtoFactory dtoFactory = DtoFactory.getInstance();
        for (Builder builder : all) {
            list.add(dtoFactory.createDto(BuilderDescriptor.class)
                               .withName(builder.getName())
                               .withDescription(builder.getDescription())
                               .withEnvironments(builder.getEnvironments()));
        }
        return list;
    }

    @GenerateLink(rel = Constants.LINK_REL_BUILDER_STATE)
    @GET
    @Path("state")
    @Produces(MediaType.APPLICATION_JSON)
    public BuilderState getBuilderState(@Required
                                        @Description("Name of the builder")
                                        @QueryParam("builder") String builder) throws Exception {
        final Builder myBuilder = getBuilder(builder);
        return DtoFactory.getInstance().createDto(BuilderState.class)
                         .withName(myBuilder.getName())
                         .withStats(myBuilder.getStats())
                         .withFreeWorkers(myBuilder.getNumberOfWorkers() - myBuilder.getNumberOfActiveWorkers())
                         .withServerState(getServerState());
    }

    @GenerateLink(rel = Constants.LINK_REL_SERVER_STATE)
    @GET
    @Path("server-state")
    @Produces(MediaType.APPLICATION_JSON)
    public ServerState getServerState() {
        return DtoFactory.getInstance().createDto(ServerState.class)
                         .withCpuPercentUsage(SystemInfo.cpu())
                         .withTotalMemory(SystemInfo.totalMemory())
                         .withFreeMemory(SystemInfo.freeMemory());
    }

    @GenerateLink(rel = Constants.LINK_REL_BUILD)
    @POST
    @Path("build")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public BuildTaskDescriptor build(@Description("Parameters for build task in JSON format") BuildRequest request) throws Exception {
        final Builder myBuilder = getBuilder(request.getBuilder());
        final BuildTask task = myBuilder.perform(request);
        return getDescriptor(task, getServiceContext().getServiceUriBuilder()).withBuildStats(myBuilder.getStats(task.getId()));
    }

    @GenerateLink(rel = Constants.LINK_REL_DEPENDENCIES_ANALYSIS)
    @POST
    @Path("dependencies")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public BuildTaskDescriptor dependencies(@Description("Parameters for analyze dependencies in JSON format") DependencyRequest request)
            throws Exception {
        final Builder myBuilder = getBuilder(request.getBuilder());
        final BuildTask task = myBuilder.perform(request);
        return getDescriptor(task, getServiceContext().getServiceUriBuilder()).withBuildStats(myBuilder.getStats(task.getId()));
    }

    @GET
    @Path("status/{builder}/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public BuildTaskDescriptor getStatus(@PathParam("builder") String builder, @PathParam("id") Long id) throws Exception {
        final Builder myBuilder = getBuilder(builder);
        final BuildTask task = myBuilder.getBuildTask(id);
        return getDescriptor(task, getServiceContext().getServiceUriBuilder()).withBuildStats(myBuilder.getStats(id));
    }

    @GET
    @Path("logs/{builder}/{id}")
    public Response getLogs(@PathParam("builder") String builder, @PathParam("id") Long id) throws Exception {
        final BuildLogger logger = getBuilder(builder).getBuildTask(id).getBuildLogger();
        return Response.ok(logger.getReader(), logger.getContentType()).build();
    }

    @POST
    @Path("cancel/{builder}/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public BuildTaskDescriptor cancel(@PathParam("builder") String builder, @PathParam("id") Long id) throws Exception {
        final Builder myBuilder = getBuilder(builder);
        final BuildTask task = myBuilder.getBuildTask(id);
        task.cancel();
        return getDescriptor(task, getServiceContext().getServiceUriBuilder()).withBuildStats(myBuilder.getStats(task.getId()));
    }

    @GET
    @Path("browse/{builder}/{id}")
    @Produces(MediaType.TEXT_HTML)
    public Response browseDirectory(@PathParam("builder") String builder,
                                    @PathParam("id") Long id,
                                    @DefaultValue(".") @QueryParam("path") String path) throws Exception {
        final Builder myBuilder = getBuilder(builder);
        final BuildTask task = myBuilder.getBuildTask(id);
        final java.io.File workDir = task.getConfiguration().getWorkDir();
        final java.io.File target = new java.io.File(workDir, path);
        final java.nio.file.Path workDirPath = workDir.toPath().normalize();
        final java.nio.file.Path targetPath = target.toPath().normalize();
        if (!(targetPath.startsWith(workDirPath))) {
            throw new NotFoundException(String.format("Invalid relative path %s", path));
        }
        if (target.isDirectory()) {
            StreamingOutput response = new StreamingOutput() {
                @Override
                public void write(OutputStream outputStream) throws IOException, WebApplicationException {
                    String indentation = "  ";
                    final PrintWriter writer = new PrintWriter(outputStream);
                    writer.print("<div class='file-browser'>");
                    final UriBuilder serviceUriBuilder = getServiceContext().getServiceUriBuilder();
                    writer.print("<ul>\n");
                    java.io.File parent = target.getParentFile();
                    java.nio.file.Path parentPath = parent.toPath();
                    if (!targetPath.equals(workDirPath) && parentPath.startsWith(workDirPath)) {
                        java.io.File[] parentChildren = parent.listFiles();
                        while (parentPath.startsWith(workDirPath) && parentChildren != null && parentChildren.length == 1) {
                            parent = parent.getParentFile();
                            parentPath = parent.toPath();
                            parentChildren = parent.listFiles();
                        }
                        writer.print(indentation);
                        final String upHref = serviceUriBuilder.clone().path(SlaveBuilderService.class, "browseDirectory")
                                                               .replaceQueryParam("path", workDirPath.relativize(parentPath))
                                                               .build(task.getBuilder(), task.getId()).toString();
                        writer.printf("<li><a href='%s'><span class='file-browser-directory-open'>..</span></a></li>", upHref);
                    }
                    final java.io.File[] list = target.listFiles();
                    if (list != null) {
                        Arrays.sort(list);
                        for (java.io.File file : list) {
                            String name = file.getName();
                            java.nio.file.Path filePath = workDirPath.relativize(file.toPath()).normalize();
                            writer.print(indentation);
                            writer.print("<li>");
                            if (file.isFile()) {
                                final String openHref = serviceUriBuilder.clone().path(SlaveBuilderService.class, "viewFile")
                                                                         .replaceQueryParam("path", filePath)
                                                                         .build(task.getBuilder(), task.getId()).toString();
                                writer.printf("<a href='%s'><span class='file-browser-file-open'>%s</span></a>", openHref, name);
                                writer.print("&nbsp;");
                                final String downloadHref = serviceUriBuilder.clone().path(SlaveBuilderService.class, "downloadFile")
                                                                             .replaceQueryParam("path", filePath)
                                                                             .build(task.getBuilder(), task.getId()).toString();
                                writer.printf("<a href='%s'><span class='file-browser-file-download'>download</span></a>", downloadHref);
                                writer.print("&nbsp;");
                                writer.printf("Size: %s", Size.toHumanSize(file.length()));
                            } else if (file.isDirectory()) {
                                java.io.File[] children = file.listFiles();
                                // Flatten empty directories
                                java.io.File singleChildDir = null;
                                while (children != null && children.length == 1 && children[0].isDirectory()) {
                                    singleChildDir = children[0];
                                    children = singleChildDir.listFiles();
                                }
                                if (singleChildDir != null) {
                                    filePath = workDirPath.relativize(singleChildDir.toPath()).normalize();
                                    name = targetPath.relativize(singleChildDir.toPath()).normalize().toString();
                                }
                                if (children != null && children.length == 0) {
                                    writer.printf("<span class='file-browser-directory-open'>%s/</span></a>", name);
                                } else {
                                    final String openHref = serviceUriBuilder.clone().path(SlaveBuilderService.class, "browseDirectory")
                                                                             .replaceQueryParam("path", filePath)
                                                                             .build(task.getBuilder(), task.getId()).toString();
                                    writer.printf("<a href='%s'><span class='file-browser-directory-open'>%s/</span></a>", openHref, name);
                                }
                            }
                            writer.print("</li>\n");
                        }
                    }
                    writer.print("</ul>\n");
                    writer.print("</div>");
                    writer.flush();
                }
            };
            return Response.status(200).entity(response).type(MediaType.TEXT_HTML).build();
        }
        throw new NotFoundException(String.format("%s does not exist or is not a folder", path));
    }

    @GET
    @Path("tree/{builder}/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ItemReference> listDirectory(@PathParam("builder") String builder,
                                             @PathParam("id") Long id,
                                             @DefaultValue(".") @QueryParam("path") String path) throws Exception {
        final Builder myBuilder = getBuilder(builder);
        final BuildTask task = myBuilder.getBuildTask(id);
        final java.io.File workDir = task.getConfiguration().getWorkDir();
        final java.io.File target = new java.io.File(workDir, path);
        final java.nio.file.Path workDirPath = workDir.toPath().normalize();
        final java.nio.file.Path targetPath = target.toPath().normalize();
        if (!(targetPath.startsWith(workDirPath))) {
            throw new NotFoundException(String.format("Invalid relative path %s", path));
        }
        final List<ItemReference> result = new LinkedList<>();
        final DtoFactory dtoFactory = DtoFactory.getInstance();
        if (target.isDirectory()) {
            final UriBuilder serviceUriBuilder = getServiceContext().getServiceUriBuilder();
            java.io.File parent = target.getParentFile();
            java.nio.file.Path parentPath = parent.toPath();
            if (!targetPath.equals(workDirPath) && parentPath.startsWith(workDirPath)) {
                java.io.File[] parentChildren = parent.listFiles();
                while (parentPath.startsWith(workDirPath) && parentChildren != null && parentChildren.length == 1) {
                    parent = parent.getParentFile();
                    parentPath = parent.toPath();
                    parentChildren = parent.listFiles();
                }
                final String upHref = serviceUriBuilder.clone().path(SlaveBuilderService.class, "listDirectory")
                                                       .replaceQueryParam("path", workDirPath.relativize(parentPath))
                                                       .build(task.getBuilder(), task.getId()).toString();
                final ItemReference up = dtoFactory.createDto(ItemReference.class).withName("..").withPath("..").withType("folder");
                up.getLinks().add(dtoFactory.createDto(Link.class).withRel("children").withHref(upHref).withMethod(HttpMethod.GET)
                                            .withProduces(MediaType.APPLICATION_JSON));
                result.add(up);
            }
            final java.io.File[] list = target.listFiles();
            if (list != null) {
                Arrays.sort(list);
                for (java.io.File file : list) {
                    String name = file.getName();
                    java.nio.file.Path filePath = workDirPath.relativize(file.toPath()).normalize();
                    if (file.isFile()) {
                        final String openHref = serviceUriBuilder.clone().path(SlaveBuilderService.class, "viewFile")
                                                                 .replaceQueryParam("path", filePath)
                                                                 .build(task.getBuilder(), task.getId()).toString();
                        final String downloadHref = serviceUriBuilder.clone().path(SlaveBuilderService.class, "downloadFile")
                                                                     .replaceQueryParam("path", filePath)
                                                                     .build(task.getBuilder(), task.getId()).toString();
                        final ItemReference fileItem =
                                dtoFactory.createDto(ItemReference.class).withName(name).withPath("/" + filePath.toString())
                                          .withType("file");
                        final List<Link> links = fileItem.getLinks();
                        final String contentType = ContentTypeGuesser.guessContentType(file);
                        links.add(dtoFactory.createDto(Link.class).withRel("view").withHref(openHref).withMethod(HttpMethod.GET)
                                            .withProduces(contentType));
                        links.add(dtoFactory.createDto(Link.class).withRel("download").withHref(downloadHref).withMethod(HttpMethod.GET)
                                            .withProduces(contentType));
                        fileItem.getAttributes().put("size", Size.toHumanSize(file.length()));
                        result.add(fileItem);
                    } else if (file.isDirectory()) {
                        java.io.File[] children = file.listFiles();
                        // Flatten empty directories
                        java.io.File singleChildDir = null;
                        while (children != null && children.length == 1 && children[0].isDirectory()) {
                            singleChildDir = children[0];
                            children = singleChildDir.listFiles();
                        }
                        if (singleChildDir != null) {
                            filePath = workDirPath.relativize(singleChildDir.toPath()).normalize();
                            name = targetPath.relativize(singleChildDir.toPath()).normalize().toString();
                        }
                        final ItemReference folderItem =
                                dtoFactory.createDto(ItemReference.class).withName(name).withPath("/" + filePath.toString())
                                          .withType("folder");

                        if (children == null || children.length != 0) {
                            final String childrenHref = serviceUriBuilder.clone().path(SlaveBuilderService.class, "listDirectory")
                                                                         .replaceQueryParam("path", filePath)
                                                                         .build(task.getBuilder(), task.getId()).toString();
                            final List<Link> links = folderItem.getLinks();
                            links.add(dtoFactory.createDto(Link.class).withRel("children").withHref(childrenHref).withMethod(HttpMethod.GET)
                                                .withProduces(MediaType.APPLICATION_JSON));
                        }

                        result.add(folderItem);
                    }
                }
            }
            return result;
        }
        throw new NotFoundException(String.format("%s does not exist or is not a folder", path));
    }

    @GET
    @Path("download/{builder}/{id}")
    public Response downloadFile(@PathParam("builder") String builder,
                                 @PathParam("id") Long id,
                                 @Required @QueryParam("path") String path) throws Exception {
        final java.io.File workDir = getBuilder(builder).getBuildTask(id).getConfiguration().getWorkDir();
        final java.io.File target = new java.io.File(workDir, path);
        if (!(target.toPath().normalize().startsWith(workDir.toPath().normalize()))) {
            throw new NotFoundException(String.format("Invalid relative path %s", path));
        }
        if (target.isFile()) {
            return Response.status(200)
                           .header(HttpHeaders.CONTENT_DISPOSITION, String.format("attachment; filename=\"%s\"", target.getName()))
                           .type(ContentTypeGuesser.guessContentType(target))
                           .entity(target)
                           .build();
        }
        throw new NotFoundException(String.format("%s does not exist or is not a file", path));
    }

    @GET
    @Path("download-all/{builder}/{id}")
    public Response downloadResultArchive(@PathParam("builder") String builder,
                                          @PathParam("id") Long id,
                                          @DefaultValue(Constants.RESULT_ARCHIVE_TAR) @QueryParam("arch") String arch) throws Exception {
        final List<File> results = getBuilder(builder).getBuildTask(id).getResult().getResults();
        if (results.isEmpty()) {
            throw new NotFoundException("Archive with build result is not available.");
        }
        File archFile;
        if (Constants.RESULT_ARCHIVE_TAR.equals(arch)) {
            archFile = Files.createTempFile(String.format("%s-%d-", builder, id), ".tar").toFile();
            TarUtils.tarFiles(archFile, 0, results.toArray(new File[results.size()]));
        } else if (Constants.RESULT_ARCHIVE_ZIP.equals(arch)) {
            archFile = Files.createTempFile(String.format("%s-%d-", builder, id), ".zip").toFile();
            ZipUtils.zipFiles(archFile, results.toArray(new File[results.size()]));
        } else {
            throw new ConflictException(String.format("Unsupported archive type: %s", arch));
        }
        return Response.status(200)
                       .header(HttpHeaders.CONTENT_DISPOSITION, String.format("attachment; filename=\"%s\"", archFile.getName()))
                       .type(ContentTypeGuesser.guessContentType(archFile))
                       .entity(new DeleteOnCloseFileInputStream(archFile))
                       .build();
    }

    @GET
    @Path("view/{builder}/{id}")
    public Response viewFile(@PathParam("builder") String builder,
                             @PathParam("id") Long id,
                             @Required @QueryParam("path") String path) throws Exception {
        final java.io.File workDir = getBuilder(builder).getBuildTask(id).getConfiguration().getWorkDir();
        final java.io.File target = new java.io.File(workDir, path);
        if (!(target.toPath().normalize().startsWith(workDir.toPath().normalize()))) {
            throw new NotFoundException(String.format("Invalid relative path %s", path));
        }
        if (target.isFile()) {
            return Response.status(200).type(ContentTypeGuesser.guessContentType(target)).entity(target).build();
        }
        throw new NotFoundException(String.format("%s does not exist or is not a file", path));
    }

    private Builder getBuilder(String name) throws NotFoundException {
        final Builder myBuilder = builders.get(name);
        if (myBuilder == null) {
            throw new NotFoundException(String.format("Unknown builder %s", name));
        }
        return myBuilder;
    }

    private BuildTaskDescriptor getDescriptor(BuildTask task, UriBuilder uriBuilder) throws BuilderException {
        final String builder = task.getBuilder();
        final Long taskId = task.getId();
        final BuildResult result = task.getResult();
        final java.nio.file.Path workDirPath = task.getConfiguration().getWorkDir().toPath();
        final BuildStatus status = task.isDone()
                                   ? (task.isCancelled() ? BuildStatus.CANCELLED
                                                         : (result.isSuccessful() ? BuildStatus.SUCCESSFUL : BuildStatus.FAILED))
                                   : (task.isStarted() ? BuildStatus.IN_PROGRESS : BuildStatus.IN_QUEUE);
        final List<Link> links = new LinkedList<>();
        final DtoFactory dtoFactory = DtoFactory.getInstance();
        links.add(dtoFactory.createDto(Link.class)
                            .withRel(Constants.LINK_REL_GET_STATUS)
                            .withHref(uriBuilder.clone().path(SlaveBuilderService.class, "getStatus").build(builder, taskId).toString())
                            .withMethod(HttpMethod.GET)
                            .withProduces(MediaType.APPLICATION_JSON));

        if (status == BuildStatus.IN_QUEUE || status == BuildStatus.IN_PROGRESS) {
            links.add(dtoFactory.createDto(Link.class)
                                .withRel(Constants.LINK_REL_CANCEL)
                                .withHref(uriBuilder.clone().path(SlaveBuilderService.class, "cancel").build(builder, taskId).toString())
                                .withMethod(HttpMethod.POST)
                                .withProduces(MediaType.APPLICATION_JSON));
        }

        if (status != BuildStatus.IN_QUEUE) {
            links.add(dtoFactory.createDto(Link.class)
                                .withRel(Constants.LINK_REL_VIEW_LOG)
                                .withHref(uriBuilder.clone().path(SlaveBuilderService.class, "getLogs").build(builder, taskId).toString())
                                .withMethod(HttpMethod.GET)
                                .withProduces(task.getBuildLogger().getContentType()));
            links.add(dtoFactory.createDto(Link.class)
                                .withRel(Constants.LINK_REL_BROWSE)
                                .withHref(uriBuilder.clone().path(SlaveBuilderService.class, "browseDirectory").queryParam("path", "/")
                                                    .build(builder, taskId).toString())
                                .withMethod(HttpMethod.GET)
                                .withProduces(MediaType.TEXT_HTML));
        }

        if (status == BuildStatus.SUCCESSFUL) {
            final List<File> results = result.getResults();
            for (java.io.File ru : results) {
                if (ru.isFile()) {
                    String relativePath = workDirPath.relativize(ru.toPath()).toString();
                    if (SystemInfo.isWindows()) {
                        relativePath = relativePath.replace("\\", "/");
                    }
                    links.add(dtoFactory.createDto(Link.class)
                                        .withRel(Constants.LINK_REL_DOWNLOAD_RESULT)
                                        .withHref(uriBuilder.clone().path(SlaveBuilderService.class, "downloadFile")
                                                            .queryParam("path", relativePath).build(builder, taskId).toString())
                                        .withMethod(HttpMethod.GET)
                                        .withProduces(ContentTypeGuesser.guessContentType(ru)));
                }
            }
            if (!results.isEmpty()) {
                links.add(dtoFactory.createDto(Link.class)
                                    .withRel(Constants.LINK_REL_DOWNLOAD_RESULTS_TARBALL)
                                    .withHref(uriBuilder.clone().path(SlaveBuilderService.class, "downloadResultArchive")
                                                        .queryParam("arch", Constants.RESULT_ARCHIVE_TAR)
                                                        .build(builder, taskId).toString())
                                    .withMethod(HttpMethod.GET));
                links.add(dtoFactory.createDto(Link.class)
                                    .withRel(Constants.LINK_REL_DOWNLOAD_RESULTS_ZIPBALL)
                                    .withHref(uriBuilder.clone().path(SlaveBuilderService.class, "downloadResultArchive")
                                                        .queryParam("arch", Constants.RESULT_ARCHIVE_ZIP)
                                                        .build(builder, taskId).toString())
                                    .withMethod(HttpMethod.GET));
            }
        }

        if ((status == BuildStatus.SUCCESSFUL || status == BuildStatus.FAILED) && result.hasBuildReport()) {
            final java.io.File br = result.getBuildReport();
            String relativePath = workDirPath.relativize(br.toPath()).toString();

            if (SystemInfo.isWindows()) {
                relativePath = relativePath.replace("\\", "/");
            }

            if (br.isDirectory()) {
                links.add(dtoFactory.createDto(Link.class)
                                    .withRel(Constants.LINK_REL_VIEW_REPORT)
                                    .withHref(uriBuilder.clone().path(SlaveBuilderService.class, "browseDirectory")
                                                        .queryParam("path", relativePath)
                                                        .build(builder, taskId).toString())
                                    .withMethod(HttpMethod.GET)
                                    .withProduces(MediaType.TEXT_HTML));
            } else {
                links.add(dtoFactory.createDto(Link.class)
                                    .withRel(Constants.LINK_REL_VIEW_REPORT)
                                    .withHref(uriBuilder.clone().path(SlaveBuilderService.class, "viewFile")
                                                        .queryParam("path", relativePath)
                                                        .build(builder, taskId).toString())
                                    .withMethod(HttpMethod.GET)
                                    .withProduces(ContentTypeGuesser.guessContentType(br)));
            }
        }

        return dtoFactory.createDto(BuildTaskDescriptor.class)
                         .withTaskId(taskId)
                         .withStatus(status)
                         .withLinks(links)
                         .withStartTime(task.getStartTime())
                         .withEndTime(task.getEndTime())
                         .withCommandLine(task.getCommandLine().toString());
    }
}
