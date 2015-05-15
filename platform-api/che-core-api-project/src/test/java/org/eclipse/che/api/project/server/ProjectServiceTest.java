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
package org.eclipse.che.api.project.server;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.core.rest.ApiExceptionMapper;
import org.eclipse.che.api.core.rest.CodenvyJsonProvider;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.core.util.LineConsumerFactory;
import org.eclipse.che.api.core.util.ValueHolder;
import org.eclipse.che.api.project.server.handlers.CreateProjectHandler;
import org.eclipse.che.api.project.server.handlers.GetItemHandler;
import org.eclipse.che.api.project.server.handlers.GetModulesHandler;
import org.eclipse.che.api.project.server.handlers.PostImportProjectHandler;
import org.eclipse.che.api.project.server.handlers.ProjectHandler;
import org.eclipse.che.api.project.server.handlers.ProjectHandlerRegistry;
import org.eclipse.che.api.project.server.type.AttributeValue;
import org.eclipse.che.api.project.server.type.ProjectType;
import org.eclipse.che.api.project.server.type.ProjectTypeRegistry;
import org.eclipse.che.api.project.shared.dto.GeneratorDescription;
import org.eclipse.che.api.project.shared.dto.ImportProject;
import org.eclipse.che.api.project.shared.dto.ImportResponse;
import org.eclipse.che.api.project.shared.dto.ImportSourceDescriptor;
import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.api.project.shared.dto.NewProject;
import org.eclipse.che.api.project.shared.dto.ProjectDescriptor;
import org.eclipse.che.api.project.shared.dto.ProjectReference;
import org.eclipse.che.api.project.shared.dto.ProjectUpdate;
import org.eclipse.che.api.project.shared.dto.Source;
import org.eclipse.che.api.project.shared.dto.TreeElement;
import org.eclipse.che.api.user.server.dao.UserDao;

import org.eclipse.che.api.vfs.server.ContentStream;
import org.eclipse.che.api.vfs.server.ContentStreamWriter;
import org.eclipse.che.api.vfs.server.MountPoint;
import org.eclipse.che.api.vfs.server.VirtualFile;
import org.eclipse.che.api.vfs.server.VirtualFileSystemRegistry;
import org.eclipse.che.api.vfs.server.VirtualFileSystemUser;
import org.eclipse.che.api.vfs.server.VirtualFileSystemUserContext;
import org.eclipse.che.api.vfs.server.impl.memory.MemoryFileSystemProvider;
import org.eclipse.che.api.vfs.server.impl.memory.MemoryMountPoint;
import org.eclipse.che.api.vfs.server.search.SearcherProvider;
import org.eclipse.che.api.vfs.shared.dto.AccessControlEntry;
import org.eclipse.che.api.vfs.shared.dto.Principal;
import org.eclipse.che.api.vfs.shared.dto.VirtualFileSystemInfo;
import org.eclipse.che.commons.json.JsonHelper;
import org.eclipse.che.commons.user.UserImpl;
import org.eclipse.che.dto.server.DtoFactory;
import org.everrest.core.ResourceBinder;
import org.everrest.core.impl.ApplicationContextImpl;
import org.everrest.core.impl.ApplicationProviderBinder;
import org.everrest.core.impl.ContainerResponse;
import org.everrest.core.impl.EnvironmentContext;
import org.everrest.core.impl.EverrestConfiguration;
import org.everrest.core.impl.EverrestProcessor;
import org.everrest.core.impl.ProviderBinder;
import org.everrest.core.impl.ResourceBinderImpl;
import org.everrest.core.tools.ByteArrayContainerResponseWriter;
import org.everrest.core.tools.DependencySupplierImpl;
import org.everrest.core.tools.ResourceLauncher;
import org.everrest.test.mock.MockHttpServletRequest;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Application;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * @author andrew00x
 * @author Eugene Voevodin
 * @author Artem Zatsarynnyy
 */
@Listeners(value = {MockitoTestNGListener.class})
public class ProjectServiceTest {
    private static final String      vfsUser       = "dev";
    private static final Set<String> vfsUserGroups = new LinkedHashSet<>(Arrays.asList("workspace/developer"));
    private static final String      workspace     = "my_ws";

    private ProjectManager          pm;
    private ResourceLauncher        launcher;
    private ProjectImporterRegistry importerRegistry;
    private ProjectHandlerRegistry  phRegistry;
    //private ProjectGeneratorRegistry generatorRegistry;

    private org.eclipse.che.commons.env.EnvironmentContext env;

    @Mock
    private UserDao                     userDao;
    private ProjectTypeResolverRegistry resolverRegistry;

    @BeforeMethod
    public void setUp() throws Exception {


        final EventService eventService = new EventService();
        VirtualFileSystemRegistry vfsRegistry = new VirtualFileSystemRegistry();

        final MemoryFileSystemProvider memoryFileSystemProvider =
                new MemoryFileSystemProvider(workspace, eventService, new VirtualFileSystemUserContext() {
                    @Override
                    public VirtualFileSystemUser getVirtualFileSystemUser() {
                        return new VirtualFileSystemUser(vfsUser, vfsUserGroups);
                    }
                }, vfsRegistry);

        MemoryMountPoint mmp = (MemoryMountPoint)memoryFileSystemProvider.getMountPoint(true);
        vfsRegistry.registerProvider(workspace, memoryFileSystemProvider);

        // PTs for test
        ProjectType chuck = new ProjectType("chuck_project_type", "chuck_project_type", true, false) {

            {
                addConstantDefinition("x", "attr description",
                                      new AttributeValue(Arrays.asList("a", "b")));

                addVariableDefinition("y", "descr", false);
            }

        };

        Set<ProjectType> projTypes = new HashSet<>();
        projTypes.add(new MyProjType());
        projTypes.add(chuck);
        ProjectTypeRegistry ptRegistry = new ProjectTypeRegistry(projTypes);

        phRegistry = new ProjectHandlerRegistry(new HashSet<ProjectHandler>());

        pm = new DefaultProjectManager(vfsRegistry, eventService,
                                       ptRegistry, phRegistry);

        pm.createProject(workspace, "my_project", new ProjectConfig("my test project", "my_project_type",
                                                                    new HashMap<String, AttributeValue>(), null, null), null, null);


        DependencySupplierImpl dependencies = new DependencySupplierImpl();
        importerRegistry = new ProjectImporterRegistry(Collections.<ProjectImporter>emptySet());

        HashSet<ProjectTypeResolver> resolvers = new HashSet<>();
        resolvers.add(new ProjectTypeResolver() {

            @Override
            public boolean resolve(FolderEntry projectFolder) throws ServerException, ValueStorageException,
                                                                     InvalidValueException, ProjectTypeConstraintException {
                ProjectConfig conf = new ProjectConfig("my proj", "my_project_type");

                Project project = new Project(projectFolder, pm);
                project.updateConfig(conf);
                return true;
            }
        });
        resolverRegistry = new ProjectTypeResolverRegistry(resolvers);

        dependencies.addComponent(ProjectTypeRegistry.class, ptRegistry);
        dependencies.addComponent(UserDao.class, userDao);
        dependencies.addComponent(ProjectManager.class, pm);
        dependencies.addComponent(ProjectImporterRegistry.class, importerRegistry);
        dependencies.addComponent(ProjectHandlerRegistry.class, phRegistry);
        dependencies.addComponent(SearcherProvider.class, mmp.getSearcherProvider());
        dependencies.addComponent(ProjectTypeResolverRegistry.class, resolverRegistry);
        dependencies.addComponent(EventService.class, eventService);


        ResourceBinder resources = new ResourceBinderImpl();
        ProviderBinder providers = new ApplicationProviderBinder();
        EverrestProcessor processor = new EverrestProcessor(resources, providers, dependencies, new EverrestConfiguration(), null);
        launcher = new ResourceLauncher(processor);

        processor.addApplication(new Application() {
            @Override
            public Set<Class<?>> getClasses() {
                return java.util.Collections.<Class<?>>singleton(ProjectService.class);
            }

            @Override
            public Set<Object> getSingletons() {
                return new HashSet<>(Arrays.asList(new CodenvyJsonProvider(new HashSet<>(Arrays.asList(ContentStream.class))),
                                                   new ContentStreamWriter(),
                                                   new ApiExceptionMapper()));
            }
        });

        ApplicationContextImpl.setCurrent(new ApplicationContextImpl(null, null, ProviderBinder.getInstance()));

        env = org.eclipse.che.commons.env.EnvironmentContext.getCurrent();
        env.setUser(new UserImpl(vfsUser, vfsUser, "dummy_token", vfsUserGroups, false));
        env.setWorkspaceName(workspace);
        env.setWorkspaceId(workspace);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetProjects() throws Exception {

        List p = pm.getProjects(workspace);

        assertEquals(p.size(), 1);

        MountPoint mountPoint = pm.getProjectsRoot(workspace).getVirtualFile().getMountPoint();
        mountPoint.getRoot().createFolder("not_project");


        ContainerResponse response =
                launcher.service("GET", "http://localhost:8080/api/project/my_ws", "http://localhost:8080/api", null, null, null);
        assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        List<ProjectReference> result = (List<ProjectReference>)response.getEntity();
        assertNotNull(result);
        assertEquals(result.size(), 2);
        ProjectReference projectReference = result.get(0);
        assertEquals(projectReference.getName(), "my_project");
        assertEquals(projectReference.getUrl(), String.format("http://localhost:8080/api/project/%s/my_project", workspace));
        assertEquals(projectReference.getDescription(), "my test project");
        assertEquals(projectReference.getWorkspaceId(), workspace);
        assertEquals(projectReference.getVisibility(), "public");

        assertEquals(projectReference.getType(), "my_project_type");
        //Assert.assertEquals(projectReference.getTypeName(), "my project type");


        ProjectReference badProject = result.get(1);
        assertEquals(badProject.getName(), "not_project");
        assertEquals(badProject.getUrl(), String.format("http://localhost:8080/api/project/%s/not_project", workspace));
        assertEquals(badProject.getWorkspaceId(), workspace);
        assertEquals(badProject.getVisibility(), "public");
        assertNotNull(badProject.getProblems());
        Assert.assertTrue(badProject.getProblems().size() > 0);
        assertEquals(1, badProject.getProblems().get(0).getCode());

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetModules() throws Exception {

        ProjectType pt = new ProjectType("testGetModules", "my module type", true, false) {

            {
                addConstantDefinition("my_module_attribute", "attr description", "attribute value 1");
            }

        };
        pm.getProjectTypeRegistry().registerProjectType(pt);

        Project myProject = pm.getProject(workspace, "my_project");

        ProjectConfig config = new ProjectConfig("my test module", pt.getId());

        FolderEntry moduleFolder = myProject.getBaseFolder().createFolder("my_module");
        Project module = new Project(moduleFolder, pm);
        module.updateConfig(config);
        myProject.getModules().add("my_module");

        ContainerResponse response = launcher.service("GET",
                                                      String.format("http://localhost:8080/api/project/%s/modules/my_project", workspace),
                                                      "http://localhost:8080/api", null, null, null);
        assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        List<ProjectDescriptor> result = (List<ProjectDescriptor>)response.getEntity();
        assertNotNull(result);

        assertEquals(result.size(), 1);
        ProjectDescriptor moduleDescriptor = result.get(0);
        assertEquals(moduleDescriptor.getDescription(), "my test module");
        assertEquals(moduleDescriptor.getType(), "testGetModules");
        //Assert.assertEquals(moduleDescriptor.getTypeName(), "my module type");
        assertEquals(moduleDescriptor.getVisibility(), "public");
        validateProjectLinks(moduleDescriptor);
    }


    @Test
    @SuppressWarnings("unchecked")
    public void testGetModulesWithHandler() throws Exception {

        ProjectType pt = new ProjectType("testGetModules", "my module type", true, false) {

            {
                addConstantDefinition("my_module_attribute", "attr description", "attribute value 1");
            }

        };
        pm.getProjectTypeRegistry().registerProjectType(pt);

        Project myProject = pm.getProject(workspace, "my_project");

        ProjectConfig config = new ProjectConfig("my test module", pt.getId());

        FolderEntry moduleFolder = myProject.getBaseFolder().createFolder("my_module");
        Project module = new Project(moduleFolder, pm);
        module.updateConfig(config);
        myProject.getModules().add("my_module");

        //create other module but not add to modules should be added to response by handler
        final FolderEntry moduleFolder2 = myProject.getBaseFolder().createFolder("my_module2");
        Project module2 = new Project(moduleFolder2, pm);
        module2.updateConfig(config);

        phRegistry.register(new GetModulesHandler() {
            @Override
            public void onGetModules(FolderEntry parentProjectFolder, final List<String> modulesPath)
                    throws ForbiddenException, ServerException, NotFoundException, IOException {
                FolderEntry child = (FolderEntry)parentProjectFolder.getChild("my_module2");
                if (child.isProjectFolder()) {
                    modulesPath.add(child.getPath());
                }
            }

            @Override
            public String getProjectType() {
                return "my_project_type";
            }
        });

        ContainerResponse response = launcher.service("GET",
                                                      String.format("http://localhost:8080/api/project/%s/modules/my_project", workspace),
                                                      "http://localhost:8080/api", null, null, null);
        assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        List<ProjectDescriptor> result = (List<ProjectDescriptor>)response.getEntity();
        assertNotNull(result);

        assertEquals(result.size(), 2);
        ProjectDescriptor moduleDescriptor = result.get(0);
        assertEquals(moduleDescriptor.getDescription(), "my test module");
        assertEquals(moduleDescriptor.getType(), "testGetModules");
        //Assert.assertEquals(moduleDescriptor.getTypeName(), "my module type");
        assertEquals(moduleDescriptor.getVisibility(), "public");
        validateProjectLinks(moduleDescriptor);
    }

    @Test
    public void testGetProject() throws Exception {
        ContainerResponse response = launcher.service("GET", String.format("http://localhost:8080/api/project/%s/my_project", workspace),
                                                      "http://localhost:8080/api", null, null, null);
        assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        ProjectDescriptor result = (ProjectDescriptor)response.getEntity();
        assertNotNull(result);
        assertEquals(result.getDescription(), "my test project");
        assertEquals(result.getType(), "my_project_type");
        assertEquals(result.getTypeName(), "my project type");
        assertEquals(result.getVisibility(), "public");
        Map<String, List<String>> attributes = result.getAttributes();
        assertNotNull(attributes);
        assertEquals(attributes.size(), 1);
        assertEquals(attributes.get("my_attribute"), Arrays.asList("attribute value 1"));
        validateProjectLinks(result);
    }



    @Test
    public void testGetNotValidProject() throws Exception {
        MountPoint mountPoint = pm.getProjectsRoot(workspace).getVirtualFile().getMountPoint();
        mountPoint.getRoot().createFolder("not_project");
        ContainerResponse response = launcher.service("GET", String.format("http://localhost:8080/api/project/%s/not_project", workspace),
                                                      "http://localhost:8080/api", null, null, null);
        assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        ProjectDescriptor badProject = (ProjectDescriptor)response.getEntity();
        assertNotNull(badProject);
        assertEquals(badProject.getName(), "not_project");
        assertEquals(badProject.getWorkspaceId(), workspace);
        assertEquals(badProject.getVisibility(), "public");
        assertNotNull(badProject.getProblems());
        Assert.assertTrue(badProject.getProblems().size() > 0);
        assertEquals(1, badProject.getProblems().get(0).getCode());
        validateProjectLinks(badProject);
    }




    @Test
    public void testGetProjectCheckUserPermissions() throws Exception {
        // Without roles Collections.<String>emptySet() should get default set of permissions
        env.setUser(new UserImpl(vfsUser, vfsUser, "dummy_token", Collections.<String>emptySet(), false));
        ContainerResponse response = launcher.service("GET", String.format("http://localhost:8080/api/project/%s/my_project", workspace),
                                                      "http://localhost:8080/api", null, null, null);
        assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        ProjectDescriptor result = (ProjectDescriptor)response.getEntity();
        assertNotNull(result);
        assertEquals(result.getPermissions(), Arrays.asList("read"));
    }

    @Test
    public void testGetModule() throws Exception {


        ProjectType pt = new ProjectType("my_module_type", "my module type", true, false) {

            {
                addConstantDefinition("my_module_attribute", "attr description", "attribute value 1");
            }

        };

        pm.getProjectTypeRegistry().registerProjectType(pt);
        Project myProject = pm.getProject(workspace, "my_project");

        ProjectConfig config = new ProjectConfig("my test module", pt.getId());
        FolderEntry moduleFolder = myProject.getBaseFolder().createFolder("my_module");
        Project module = new Project(moduleFolder, pm);

        module.updateConfig(config);

        ContainerResponse response =
                launcher.service("GET", String.format("http://localhost:8080/api/project/%s/my_project/my_module", workspace),
                                 "http://localhost:8080/api", null, null, null);
        assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        ProjectDescriptor result = (ProjectDescriptor)response.getEntity();
        assertNotNull(result);
        assertEquals(result.getDescription(), "my test module");
        assertEquals(result.getType(), "my_module_type");
        assertEquals(result.getTypeName(), "my module type");
        assertEquals(result.getVisibility(), "public");


        Map<String, List<String>> attributes = result.getAttributes();
        assertNotNull(attributes);
        assertEquals(attributes.size(), 1);
        assertEquals(attributes.get("my_module_attribute"), Arrays.asList("attribute value 1"));
        validateProjectLinks(result);
    }

    @Test
    public void testGetProjectInvalidPath() throws Exception {
        ContainerResponse response = launcher.service("GET",
                                                      String.format("http://localhost:8080/api/project/%s/my_project_invalid", workspace),
                                                      "http://localhost:8080/api", null, null, null);
        assertEquals(response.getStatus(), 404);
    }




    @Test
    public void testCreateProject() throws Exception {


        phRegistry.register(new CreateProjectHandler() {
            @Override
            public void onCreateProject(FolderEntry baseFolder, Map<String, AttributeValue> attributes, Map<String, String> options) throws ForbiddenException, ConflictException, ServerException {
                baseFolder.createFolder("a");
                baseFolder.createFolder("b");
                baseFolder.createFile("test.txt", "test".getBytes(), "text/plain");
            }

            @Override
            public String getProjectType() {
                return "testCreateProject";
            }
        });

        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Arrays.asList("application/json"));


        ProjectType pt = new ProjectType("testCreateProject", "my project type", true, false) {

            {
                addConstantDefinition("new_project_attribute", "attr description", "to be or not to be");
            }

        };

        pm.getProjectTypeRegistry().registerProjectType(pt);


        Map<String, List<String>> attributeValues = new LinkedHashMap<>();
        attributeValues.put("new project attribute", Arrays.asList("to be or not to be"));
        GeneratorDescription generatorDescription = DtoFactory.getInstance().createDto(GeneratorDescription.class);
//                                                          .withName("my_generator");

        NewProject descriptor = DtoFactory.getInstance().createDto(NewProject.class)
                                          .withType("testCreateProject")
                                          .withDescription("new project")
                                          .withAttributes(attributeValues)
                                          .withGeneratorDescription(generatorDescription);


        ContainerResponse response = launcher.service("POST",
                                                      String.format("http://localhost:8080/api/project/%s?name=new_project", workspace),
                                                      "http://localhost:8080/api",
                                                      headers,
                                                      DtoFactory.getInstance().toJson(descriptor).getBytes(),
                                                      null);
        assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        ProjectDescriptor result = (ProjectDescriptor)response.getEntity();
        assertNotNull(result);
        assertEquals(result.getName(), "new_project");
        assertEquals(result.getPath(), "/new_project");
        assertEquals(result.getDescription(), "new project");
        assertEquals(result.getType(), "testCreateProject");
        assertEquals(result.getTypeName(), "my project type");
        assertEquals(result.getVisibility(), "public");
        assertEquals(result.getWorkspaceId(), workspace);
        assertEquals(result.getWorkspaceName(), workspace);
        assertEquals(result.getIdeUrl(), String.format("http://localhost:8080/ws/%s/new_project", workspace));
        assertEquals(result.getBaseUrl(), String.format("http://localhost:8080/api/project/%s/new_project", workspace));
        Map<String, List<String>> attributes = result.getAttributes();
        assertNotNull(attributes);
        assertEquals(attributes.size(), 1);
        assertEquals(attributes.get("new_project_attribute"), Arrays.asList("to be or not to be"));
        validateProjectLinks(result);

        Project project = pm.getProject(workspace, "new_project");
        assertNotNull(project);


        ProjectConfig config = project.getConfig(); //new ProjectConfig("new project", "testCreateProject");

        assertEquals(config.getDescription(), "new project");
        assertEquals(config.getTypeId(), "testCreateProject");
        AttributeValue attributeVal = config.getAttributes().get("new_project_attribute");
        assertNotNull(attributeVal);
        assertEquals(attributeVal.getString(), "to be or not to be");

        assertNotNull(project.getBaseFolder().getChild("a"));
        assertNotNull(project.getBaseFolder().getChild("b"));
        assertNotNull(project.getBaseFolder().getChild("test.txt"));

    }



    @Test
    public void testCreateModule() throws Exception {
        phRegistry.register(new CreateProjectHandler() {

            @Override
            public String getProjectType() {
                return "my_project_type";
            }

            @Override
            public void onCreateProject(FolderEntry baseFolder, Map<String, AttributeValue> attributes, Map<String, String> options)
                    throws ConflictException, ForbiddenException, ServerException {
                baseFolder.createFolder("a");
                baseFolder.createFolder("b");
                baseFolder.createFile("test.txt", "test".getBytes(), "text/plain");
            }
        });

        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Arrays.asList("application/json"));



        Map<String, List<String>> attributeValues = new LinkedHashMap<>();
        attributeValues.put("new module attribute", Arrays.asList("to be or not to be"));
        GeneratorDescription generatorDescription = DtoFactory.getInstance().createDto(GeneratorDescription.class);
//                                                          .withName("my_generator");


        //pm.createProject(workspace, "")

        NewProject descriptor = DtoFactory.getInstance().createDto(NewProject.class)
                                          .withType("my_project_type")
                                          .withDescription("new module")
                                          .withAttributes(attributeValues)
                                          .withGeneratorDescription(generatorDescription);

        ContainerResponse response = launcher.service("POST",
                                                      String.format("http://localhost:8080/api/project/%s/my_project?path=%s",
                                                                    workspace, "new_module"),
                                                      "http://localhost:8080/api",
                                                      headers,
                                                      DtoFactory.getInstance().toJson(descriptor).getBytes(),
                                                      null);
        assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        ProjectDescriptor result = (ProjectDescriptor)response.getEntity();
        assertNotNull(result);
        assertEquals(result.getName(), "new_module");
        assertEquals(result.getPath(), "/my_project/new_module");
        assertEquals(result.getDescription(), "new module");
        assertEquals(result.getType(), "my_project_type");
        assertEquals(result.getTypeName(), "my project type");
        assertEquals(result.getVisibility(), "public");
        assertEquals(result.getWorkspaceId(), workspace);
        assertEquals(result.getWorkspaceName(), workspace);
        assertEquals(result.getIdeUrl(), String.format("http://localhost:8080/ws/%s/my_project/new_module", workspace));

        assertEquals(result.getBaseUrl(), String.format("http://localhost:8080/api/project/%s/my_project/new_module", workspace));


        Map<String, List<String>> attributes = result.getAttributes();
        assertNotNull(attributes);
        assertEquals(attributes.size(), 1);
        assertEquals(attributes.get("my_attribute"), Arrays.asList("attribute value 1"));
        validateProjectLinks(result);

        Project project = pm.getProject(workspace, "my_project/new_module");
        assertNotNull(project);



        ProjectConfig config = project.getConfig();

        //ProjectDescription description = project.getDescription();

        assertEquals(config.getDescription(), "new module");
        assertEquals(config.getTypeId(), "my_project_type");
       // Assert.assertEquals(description.getProjectType().getName(), "my project type");
        AttributeValue attributeVal = config.getAttributes().get("my_attribute");

        assertEquals(attributeVal.getString(), "attribute value 1");

        assertNotNull(project.getBaseFolder().getChild("a"));
        assertNotNull(project.getBaseFolder().getChild("b"));
        assertNotNull(project.getBaseFolder().getChild("test.txt"));

    }

    @Test
    public void testCreateModuleAbsolutePath() throws Exception {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Arrays.asList("application/json"));

        pm.createProject(workspace, "another", new ProjectConfig("", "my_project_type"), null, null);

        assertEquals(pm.getProject(workspace, "my_project").getModules().get().size(), 0);


        ContainerResponse response = launcher.service("POST",
                String.format("http://localhost:8080/api/project/%s/my_project?path=%s",
                        workspace, "/another"),
                "http://localhost:8080/api",
                headers,
                null,
                //DtoFactory.getInstance().toJson(descriptor).getBytes(),
                null);
        assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());

        assertEquals(pm.getProject(workspace, "my_project").getModules().get().size(), 1);
        assertEquals(pm.getProject(workspace, "my_project").getModules().get().iterator().next(), "/another");


    }


    @Test
    public void testRemoveModule() throws Exception {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Arrays.asList("application/json"));

        pm.createProject(workspace, "todel", new ProjectConfig("", "my_project_type"), null, null);

        pm.addModule(workspace, "my_project", "/todel", null, null, null);

        assertEquals(pm.getProject(workspace, "my_project").getModules().get().size(), 1);
        assertEquals(pm.getProject(workspace, "my_project").getModules().get().iterator().next(), "/todel");

        ContainerResponse response = launcher.service("DELETE",
                String.format("http://localhost:8080/api/project/%s/my_project?module=/todel", workspace),
                "http://localhost:8080/api", null, null, null);

        assertEquals(response.getStatus(), 204, "Error: " + response.getEntity());
        assertEquals(pm.getProject(workspace, "my_project").getModules().get().size(), 0);

    }

    @Test
    public void testCreateProjectUnknownProjectType() throws Exception {
        final String newProjectTypeId = "new_project_type";
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Arrays.asList("application/json"));
        Map<String, List<String>> attributeValues = new LinkedHashMap<>();
        attributeValues.put("new project attribute", Arrays.asList("to be or not to be"));
        ProjectDescriptor descriptor = DtoFactory.getInstance().createDto(ProjectDescriptor.class)
                                                 .withType(newProjectTypeId)
                                                 .withDescription("new project")
                                                 .withAttributes(attributeValues);
        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        ContainerResponse response = launcher.service("POST", "http://localhost:8080/api/project/my_ws?name=new_project",
                                                      "http://localhost:8080/api",
                                                      headers,
                                                      DtoFactory.getInstance().toJson(descriptor).getBytes(),
                                                      writer,
                                                      null);

        // PT is not registered, Unknown PT error thrown
        assertEquals(response.getStatus(), 409);

    }


    @Test
    public void testUpdateProject() throws Exception {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Arrays.asList("application/json"));

        ProjectType pt = new ProjectType("testUpdateProject", "my project type", true, false) {

            {
                addVariableDefinition("my_attribute", "attr description", false);
            }

        };
        pm.getProjectTypeRegistry().registerProjectType(pt);
        pm.createProject(workspace, "testUpdateProject", new ProjectConfig("created project", "testUpdateProject"), null, null);

        Map<String, List<String>> attributeValues = new LinkedHashMap<>();
        attributeValues.put("my_attribute", Arrays.asList("to be or not to be"));
        ProjectUpdate descriptor = DtoFactory.getInstance().createDto(ProjectUpdate.class)
                .withType("testUpdateProject")
                .withDescription("updated project")
                .withAttributes(attributeValues);


        ContainerResponse response = launcher.service("PUT",
                                                      String.format("http://localhost:8080/api/project/%s/testUpdateProject", workspace),
                                                      "http://localhost:8080/api",
                                                      headers,
                                                      DtoFactory.getInstance().toJson(descriptor).getBytes(),
                                                      null);
        assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());

        Project project = pm.getProject(workspace, "testUpdateProject");
        assertNotNull(project);
        ProjectConfig config = project.getConfig();

        assertEquals(config.getDescription(), "updated project");
        assertEquals(config.getTypeId(), "testUpdateProject");
        //Assert.assertEquals(description.getProjectType().getName(), "my project type");
        AttributeValue attributeVal = config.getAttributes().get("my_attribute");
        assertNotNull(attributeVal);
        assertEquals(attributeVal.getList(), Arrays.asList("to be or not to be"));


    }

    @Test
    public void testUpdateBadProject() throws Exception {
        MountPoint mountPoint = pm.getProjectsRoot(workspace).getVirtualFile().getMountPoint();
        mountPoint.getRoot().createFolder("not_project");

        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Arrays.asList("application/json"));
        Map<String, List<String>> attributeValues = new LinkedHashMap<>();
        attributeValues.put("my_attribute", Arrays.asList("to be or not to be"));
        ProjectUpdate descriptor = DtoFactory.getInstance().createDto(ProjectUpdate.class)
                                             .withType("my_project_type")
                                             .withDescription("updated project")
                                             .withAttributes(attributeValues);
        ContainerResponse response = launcher.service("PUT",
                                                      String.format("http://localhost:8080/api/project/%s/not_project", workspace),
                                                      "http://localhost:8080/api",
                                                      headers,
                                                      DtoFactory.getInstance().toJson(descriptor).getBytes(),
                                                      null);
        assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        Project project = pm.getProject(workspace, "not_project");
        assertNotNull(project);
        ProjectConfig description = project.getConfig();

        assertEquals(description.getDescription(), "updated project");
        assertEquals(description.getTypeId(), "my_project_type");
        //Assert.assertEquals(description.getProjectType().getName(), "my project type");
        //Attribute attribute = description.getAttribute("my_attribute");
        //Assert.assertEquals(attribute.getValues(), Arrays.asList("to be or not to be"));

    }


    @Test
    public void testUpdateProjectInvalidPath() throws Exception {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Arrays.asList("application/json"));
        Map<String, List<String>> attributeValues = new LinkedHashMap<>();
        attributeValues.put("my_attribute", Arrays.asList("to be or not to be"));
        ProjectUpdate descriptor = DtoFactory.getInstance().createDto(ProjectUpdate.class)
                                             .withType("my_project_type")
                                             .withDescription("updated project")
                                             .withAttributes(attributeValues);
        ContainerResponse response = launcher.service("PUT",
                                                      String.format("http://localhost:8080/api/project/%s/my_project_invalid",
                                                                    workspace),
                                                      "http://localhost:8080/api",
                                                      headers,
                                                      DtoFactory.getInstance().toJson(descriptor).getBytes(),
                                                      null);
        assertEquals(response.getStatus(), 404);
    }

    @Test
    public void testCreateFile() throws Exception {
        String myContent = "to be or not to be";
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Arrays.asList("text/plain"));
        ContainerResponse response = launcher.service("POST",
                                                      String.format("http://localhost:8080/api/project/%s/file/my_project?name=test.txt",
                                                                    workspace),
                                                      "http://localhost:8080/api",
                                                      headers,
                                                      myContent.getBytes(),
                                                      null);
        assertEquals(response.getStatus(), 201, "Error: " + response.getEntity());
        ItemReference fileItem = (ItemReference)response.getEntity();
        assertEquals(fileItem.getType(), "file");
        assertEquals(fileItem.getMediaType(), "text/plain");
        assertEquals(fileItem.getName(), "test.txt");
        assertEquals(fileItem.getPath(), "/my_project/test.txt");
        validateFileLinks(fileItem);
        assertEquals(response.getHttpHeaders().getFirst("Location"),
                     URI.create(String.format("http://localhost:8080/api/project/%s/file/my_project/test.txt", workspace)));
        VirtualFileEntry file = pm.getProject(workspace, "my_project").getBaseFolder().getChild("test.txt");
        Assert.assertTrue(file.isFile());
        FileEntry _file = (FileEntry)file;
        assertEquals(_file.getMediaType(), "text/plain");
        assertEquals(new String(_file.contentAsBytes()), myContent);
    }

    @Test
    public void testUploadFile() throws Exception {
        String fileContent = "to be or not to be";
        String fileName = "test.txt";
        String fileMediaType = "text/plain";
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Arrays.asList("multipart/form-data; boundary=abcdef"));
        String uploadBodyPattern =
                "--abcdef\r\nContent-Disposition: form-data; name=\"file\"; filename=\"%1$s\"\r\nContent-Type: %2$s\r\n\r\n%3$s"
                + "\r\n--abcdef\r\nContent-Disposition: form-data; name=\"mimeType\"\r\n\r\n%4$s"
                + "\r\n--abcdef\r\nContent-Disposition: form-data; name=\"name\"\r\n\r\n%5$s"
                + "\r\n--abcdef\r\nContent-Disposition: form-data; name=\"overwrite\"\r\n\r\n%6$b"
                + "\r\n--abcdef--\r\n";
        byte[] formData = String.format(uploadBodyPattern, fileName, fileMediaType, fileContent, fileMediaType, fileName, false).getBytes();
        EnvironmentContext env = new EnvironmentContext();
        env.put(HttpServletRequest.class, new MockHttpServletRequest("", new ByteArrayInputStream(formData),
                                                                     formData.length, "POST", headers));
        ContainerResponse response = launcher.service("POST",
                                                      String.format("http://localhost:8080/api/project/%s/uploadfile/my_project",
                                                                    workspace),
                                                      "http://localhost:8080/api",
                                                      headers,
                                                      formData,
                                                      env);
        assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        VirtualFileEntry file = pm.getProject(workspace, "my_project").getBaseFolder().getChild(fileName);
        Assert.assertTrue(file.isFile());
        FileEntry _file = (FileEntry)file;
        assertEquals(_file.getMediaType(), fileMediaType);
        assertEquals(new String(_file.contentAsBytes()), fileContent);
    }

    @Test
    public void testUploadFileWhenFileAlreadyExistAndOverwriteIsTrue() throws Exception {
        String oldFileContent = "to be or not to be";
        String newFileContent = "To be, or not to be, that is the question!";
        String fileName = "test.txt";
        String fileMediaType = "text/plain";
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Arrays.asList("multipart/form-data; boundary=abcdef"));
        String uploadBodyPattern =
                "--abcdef\r\nContent-Disposition: form-data; name=\"file\"; filename=\"%1$s\"\r\nContent-Type: %2$s\r\n\r\n%3$s"
                + "\r\n--abcdef\r\nContent-Disposition: form-data; name=\"mimeType\"\r\n\r\n%4$s"
                + "\r\n--abcdef\r\nContent-Disposition: form-data; name=\"name\"\r\n\r\n%5$s"
                + "\r\n--abcdef\r\nContent-Disposition: form-data; name=\"overwrite\"\r\n\r\n%6$b"
                + "\r\n--abcdef--\r\n";
        pm.getProject(workspace, "my_project").getBaseFolder().createFile(fileName, oldFileContent.getBytes(), fileMediaType);
        byte[] newFileData = String.format(uploadBodyPattern, fileName, fileMediaType, newFileContent, fileMediaType, fileName, true).getBytes();

        EnvironmentContext env = new EnvironmentContext();
        env.put(HttpServletRequest.class, new MockHttpServletRequest("", new ByteArrayInputStream(newFileData),
                                                                     newFileData.length, "POST", headers));
        ContainerResponse response = launcher.service("POST",
                                                      String.format("http://localhost:8080/api/project/%s/uploadfile/my_project",
                                                                    workspace),
                                                      "http://localhost:8080/api",
                                                      headers,
                                                      newFileData,
                                                      env);
        assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        VirtualFileEntry file = pm.getProject(workspace, "my_project").getBaseFolder().getChild(fileName);
        Assert.assertTrue(file.isFile());
        FileEntry _file = (FileEntry)file;
        assertEquals(_file.getMediaType(), fileMediaType);
        assertEquals(new String(_file.contentAsBytes()), newFileContent);
    }

    @Test
    public void testUploadFileWhenFileAlreadyExistAndOverwriteIsFalse() throws Exception {
        String oldFileContent = "to be or not to be";
        String newFileContent = "To be, or not to be, that is the question!";
        String fileName = "test.txt";
        String fileMediaType = "text/plain";
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Arrays.asList("multipart/form-data; boundary=abcdef"));
        String uploadBodyPattern =
                "--abcdef\r\nContent-Disposition: form-data; name=\"file\"; filename=\"%1$s\"\r\nContent-Type: %2$s\r\n\r\n%3$s"
                + "\r\n--abcdef\r\nContent-Disposition: form-data; name=\"mimeType\"\r\n\r\n%4$s"
                + "\r\n--abcdef\r\nContent-Disposition: form-data; name=\"name\"\r\n\r\n%5$s"
                + "\r\n--abcdef\r\nContent-Disposition: form-data; name=\"overwrite\"\r\n\r\n%6$b"
                + "\r\n--abcdef--\r\n";
        pm.getProject(workspace, "my_project").getBaseFolder().createFile(fileName, oldFileContent.getBytes(), fileMediaType);
        byte[] newFileData = String.format(uploadBodyPattern, fileName, fileMediaType, newFileContent, fileMediaType, fileName, false).getBytes();

        EnvironmentContext env = new EnvironmentContext();
        env.put(HttpServletRequest.class, new MockHttpServletRequest("", new ByteArrayInputStream(newFileData),
                                                                     newFileData.length, "POST", headers));
        ContainerResponse response = launcher.service("POST",
                                                      String.format("http://localhost:8080/api/project/%s/uploadfile/my_project",
                                                                    workspace),
                                                      "http://localhost:8080/api",
                                                      headers,
                                                      newFileData,
                                                      env);
        assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        VirtualFileEntry file = pm.getProject(workspace, "my_project").getBaseFolder().getChild(fileName);
        Assert.assertTrue(file.isFile());
        FileEntry _file = (FileEntry)file;
        assertEquals(_file.getMediaType(), fileMediaType);
        assertEquals(new String(_file.contentAsBytes()), oldFileContent);
    }

    @Test
    public void testGetFileContent() throws Exception {
        String myContent = "to be or not to be";
        pm.getProject(workspace, "my_project").getBaseFolder().createFile("test.txt", myContent.getBytes(), "text/plain");
        ByteArrayContainerResponseWriter writer = new ByteArrayContainerResponseWriter();
        ContainerResponse response = launcher.service("GET",
                                                      String.format("http://localhost:8080/api/project/%s/file/my_project/test.txt",
                                                                    workspace),
                                                      "http://localhost:8080/api", null, null, writer, null);
        assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        assertEquals(response.getContentType().toString(), "text/plain");
        assertEquals(new String(writer.getBody()), myContent);
    }

    @Test
    public void testUpdateFileContent() throws Exception {
        String myContent = "<test>hello</test>";
        pm.getProject(workspace, "my_project").getBaseFolder().createFile("test", "to be or not to be".getBytes(), "text/plain");
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Arrays.asList("text/xml"));
        ContainerResponse response = launcher.service("PUT",
                                                      String.format("http://localhost:8080/api/project/%s/file/my_project/test", workspace),
                                                      "http://localhost:8080/api", headers, myContent.getBytes(), null);
        assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        VirtualFileEntry file = pm.getProject(workspace, "my_project").getBaseFolder().getChild("test");
        Assert.assertTrue(file.isFile());
        FileEntry _file = (FileEntry)file;
        assertEquals(_file.getMediaType(), "text/xml");
        assertEquals(new String(_file.contentAsBytes()), myContent);
    }

    @Test
    public void testCreateFolder() throws Exception {
        ContainerResponse response = launcher.service("POST",
                                                      String.format("http://localhost:8080/api/project/%s/folder/my_project/test",
                                                                    workspace),
                                                      "http://localhost:8080/api", null, null, null);
        assertEquals(response.getStatus(), 201, "Error: " + response.getEntity());
        ItemReference fileItem = (ItemReference)response.getEntity();
        assertEquals(fileItem.getType(), "folder");
        assertEquals(fileItem.getMediaType(), "text/directory");
        assertEquals(fileItem.getName(), "test");
        assertEquals(fileItem.getPath(), "/my_project/test");
        validateFolderLinks(fileItem);
        assertEquals(response.getHttpHeaders().getFirst("Location"),
                     URI.create(String.format("http://localhost:8080/api/project/%s/children/my_project/test", workspace)));
        VirtualFileEntry folder = pm.getProject(workspace, "my_project").getBaseFolder().getChild("test");
        Assert.assertTrue(folder.isFolder());
    }

    @Test
    public void testCreatePath() throws Exception {
        ContainerResponse response = launcher.service("POST",
                                                      String.format("http://localhost:8080/api/project/%s/folder/my_project/a/b/c",
                                                                    workspace),
                                                      "http://localhost:8080/api", null, null, null);
        assertEquals(response.getStatus(), 201, "Error: " + response.getEntity());
        assertEquals(response.getHttpHeaders().getFirst("Location"),
                     URI.create(String.format("http://localhost:8080/api/project/%s/children/my_project/a/b/c", workspace)));
        VirtualFileEntry folder = pm.getProject(workspace, "my_project").getBaseFolder().getChild("a/b/c");
        Assert.assertTrue(folder.isFolder());
    }

    @Test
    public void testDeleteFile() throws Exception {
        pm.getProject(workspace, "my_project").getBaseFolder().createFile("test.txt", "to be or not to be".getBytes(), "text/plain");
        ContainerResponse response = launcher.service("DELETE",
                                                      String.format("http://localhost:8080/api/project/%s/my_project/test.txt", workspace),
                                                      "http://localhost:8080/api", null, null, null);
        assertEquals(response.getStatus(), 204, "Error: " + response.getEntity());
        Assert.assertNull(pm.getProject(workspace, "my_project").getBaseFolder().getChild("test.txt"));
    }

    @Test
    public void testDeleteFolder() throws Exception {
        pm.getProject(workspace, "my_project").getBaseFolder().createFolder("test");
        ContainerResponse response = launcher.service("DELETE",
                                                      String.format("http://localhost:8080/api/project/%s/my_project/test", workspace),
                                                      "http://localhost:8080/api", null, null, null);
        assertEquals(response.getStatus(), 204, "Error: " + response.getEntity());
        Assert.assertNull(pm.getProject(workspace, "my_project").getBaseFolder().getChild("test"));
    }

    @Test
    public void testDeletePath() throws Exception {
        pm.getProject(workspace, "my_project").getBaseFolder().createFolder("a/b/c");
        ContainerResponse response = launcher.service("DELETE",
                                                      String.format("http://localhost:8080/api/project/%s/my_project/a/b/c", workspace),
                                                      "http://localhost:8080/api", null, null, null);
        assertEquals(response.getStatus(), 204, "Error: " + response.getEntity());
        Assert.assertNull(pm.getProject(workspace, "my_project").getBaseFolder().getChild("a/b/c"));
    }

    @Test
    public void testDeleteInvalidPath() throws Exception {
        ContainerResponse response = launcher.service("DELETE",
                                                      String.format("http://localhost:8080/api/project/%s/my_project/a/b/c", workspace),
                                                      "http://localhost:8080/api", null, null, null);
        assertEquals(response.getStatus(), 404);
        assertNotNull(pm.getProject(workspace, "my_project"));
    }

    @Test
    public void testDeleteProject() throws Exception {
        ContainerResponse response = launcher.service("DELETE",
                                                      String.format("http://localhost:8080/api/project/%s/my_project", workspace),
                                                      "http://localhost:8080/api", null, null, null);
        assertEquals(response.getStatus(), 204, "Error: " + response.getEntity());
        Assert.assertNull(pm.getProject(workspace, "my_project"));
    }

    @Test
    public void testCopyFile() throws Exception {
        Project myProject = pm.getProject(workspace, "my_project");
        myProject.getBaseFolder().createFolder("a/b/c");
        ((FolderEntry)myProject.getBaseFolder().getChild("a/b")).createFile("test.txt", "to be or not no be".getBytes(), "text/plain");
        ContainerResponse response = launcher.service("POST",
                                                      String.format(
                                                              "http://localhost:8080/api/project/%s/copy/my_project/a/b/test.txt?to=/my_project/a/b/c",
                                                              workspace),
                                                      "http://localhost:8080/api", null, null, null);
        assertEquals(response.getStatus(), 201, "Error: " + response.getEntity());
        assertEquals(response.getHttpHeaders().getFirst("Location"),
                     URI.create(String.format("http://localhost:8080/api/project/%s/file/my_project/a/b/c/test.txt", workspace)));
        assertNotNull(myProject.getBaseFolder().getChild("a/b/c/test.txt")); // new
        assertNotNull(myProject.getBaseFolder().getChild("a/b/test.txt")); // old
    }

    @Test
    public void testCopyFolder() throws Exception {
        Project myProject = pm.getProject(workspace, "my_project");
        myProject.getBaseFolder().createFolder("a/b/c");
        ((FolderEntry)myProject.getBaseFolder().getChild("a/b")).createFile("test.txt", "to be or not no be".getBytes(), "text/plain");
        ContainerResponse response = launcher.service("POST",
                                                      String.format(
                                                              "http://localhost:8080/api/project/%s/copy/my_project/a/b?to=/my_project/a/b/c",
                                                              workspace),
                                                      "http://localhost:8080/api", null, null, null);
        assertEquals(response.getStatus(), 201, "Error: " + response.getEntity());
        assertEquals(response.getHttpHeaders().getFirst("Location"),
                     URI.create(String.format("http://localhost:8080/api/project/%s/children/my_project/a/b/c/b", workspace)));
        assertNotNull(myProject.getBaseFolder().getChild("a/b/test.txt"));
        assertNotNull(myProject.getBaseFolder().getChild("a/b/c/b/test.txt"));
    }

    @Test
    public void testMoveFile() throws Exception {
        Project myProject = pm.getProject(workspace, "my_project");
        myProject.getBaseFolder().createFolder("a/b/c");
        ((FolderEntry)myProject.getBaseFolder().getChild("a/b")).createFile("test.txt", "to be or not no be".getBytes(), "text/plain");
        ContainerResponse response = launcher.service("POST",
                                                      String.format(
                                                              "http://localhost:8080/api/project/%s/move/my_project/a/b/test.txt?to=/my_project/a/b/c",
                                                              workspace),
                                                      "http://localhost:8080/api", null, null, null);
        assertEquals(response.getStatus(), 201, "Error: " + response.getEntity());
        assertEquals(response.getHttpHeaders().getFirst("Location"),
                     URI.create(String.format("http://localhost:8080/api/project/%s/file/my_project/a/b/c/test.txt", workspace)));
        assertNotNull(myProject.getBaseFolder().getChild("a/b/c/test.txt")); // new
        Assert.assertNull(myProject.getBaseFolder().getChild("a/b/test.txt")); // old
    }

    @Test
    public void testMoveFolder() throws Exception {
        Project myProject = pm.getProject(workspace, "my_project");
        myProject.getBaseFolder().createFolder("a/b/c");
        ((FolderEntry)myProject.getBaseFolder().getChild("a/b/c")).createFile("test.txt", "to be or not no be".getBytes(), "text/plain");
        ContainerResponse response = launcher.service("POST",
                                                      String.format(
                                                              "http://localhost:8080/api/project/%s/move/my_project/a/b/c?to=/my_project/a",
                                                              workspace),
                                                      "http://localhost:8080/api", null, null, null);
        assertEquals(response.getStatus(), 201, "Error: " + response.getEntity());
        assertEquals(response.getHttpHeaders().getFirst("Location"),
                     URI.create(String.format("http://localhost:8080/api/project/%s/children/my_project/a/c", workspace)));
        assertNotNull(myProject.getBaseFolder().getChild("a/c/test.txt"));
        Assert.assertNull(myProject.getBaseFolder().getChild("a/b/c/test.txt"));
        Assert.assertNull(myProject.getBaseFolder().getChild("a/b/c"));
    }

    @Test
    public void testRenameFile() throws Exception {
        Project myProject = pm.getProject(workspace, "my_project");
        myProject.getBaseFolder().createFile("test.txt", "hello".getBytes(), "text/plain");
        ContainerResponse response = launcher.service("POST",
                                                      String.format(
                                                              "http://localhost:8080/api/project/%s/rename/my_project/test.txt?name=_test.txt",
                                                              workspace),
                                                      "http://localhost:8080/api", null, null, null);
        assertEquals(response.getStatus(), 201, "Error: " + response.getEntity());
        assertEquals(response.getHttpHeaders().getFirst("Location"),
                     URI.create(String.format("http://localhost:8080/api/project/%s/file/my_project/_test.txt", workspace)));
        assertNotNull(myProject.getBaseFolder().getChild("_test.txt"));
        Assert.assertNull(myProject.getBaseFolder().getChild("test.txt"));
    }

    @Test
    public void testRenameFileAndUpdateMediaType() throws Exception {
        Project myProject = pm.getProject(workspace, "my_project");
        myProject.getBaseFolder().createFile("test.txt", "hello".getBytes(), "text/*");
        ContainerResponse response = launcher.service("POST",
                                                      String.format(
                                                              "http://localhost:8080/api/project/%s/rename/my_project/test.txt?name=_test.txt&mediaType=text/plain",
                                                              workspace),
                                                      "http://localhost:8080/api", null, null, null);
        assertEquals(response.getStatus(), 201, "Error: " + response.getEntity());
        assertEquals(response.getHttpHeaders().getFirst("Location"),
                     URI.create(String.format("http://localhost:8080/api/project/%s/file/my_project/_test.txt", workspace)));
        FileEntry renamed = (FileEntry)myProject.getBaseFolder().getChild("_test.txt");
        assertNotNull(renamed);
        assertEquals(renamed.getMediaType(), "text/plain");
        Assert.assertNull(myProject.getBaseFolder().getChild("test.txt"));
    }

    @Test
    public void testRenameFolder() throws Exception {
        Project myProject = pm.getProject(workspace, "my_project");
        myProject.getBaseFolder().createFolder("a/b/c");
        ContainerResponse response = launcher.service("POST",
                                                      String.format("http://localhost:8080/api/project/%s/rename/my_project/a/b?name=x",
                                                                    workspace),
                                                      "http://localhost:8080/api", null, null, null);
        assertEquals(response.getStatus(), 201, "Error: " + response.getEntity());
        assertEquals(response.getHttpHeaders().getFirst("Location"),
                     URI.create(String.format("http://localhost:8080/api/project/%s/children/my_project/a/x", workspace)));
        assertNotNull(myProject.getBaseFolder().getChild("a/x"));
        assertNotNull(myProject.getBaseFolder().getChild("a/x/c"));
        Assert.assertNull(myProject.getBaseFolder().getChild("a/b"));
    }



    @Test
    public void testImportProject() throws Exception {


        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ZipOutputStream zipOut = new ZipOutputStream(bout);
        zipOut.putNextEntry(new ZipEntry("folder1/"));
        zipOut.putNextEntry(new ZipEntry("folder1/file1.txt"));
        zipOut.write("to be or not to be".getBytes());
        zipOut.putNextEntry(new ZipEntry(Constants.CODENVY_DIR + "/"));
        zipOut.putNextEntry(new ZipEntry(Constants.CODENVY_PROJECT_FILE_RELATIVE_PATH));
        zipOut.write(("{\"type\":\"chuck_project_type\"," +
                      "\"description\":\"import test\"," +
                      "\"attributes\":{\"x\": [\"a\",\"b\"]}}").getBytes());
        zipOut.close();
        final InputStream zip = new ByteArrayInputStream(bout.toByteArray());
        final String importType = "_123_";
        final ValueHolder<FolderEntry> folderHolder = new ValueHolder<>();
        importerRegistry.register(new ProjectImporter() {
            @Override
            public String getId() {
                return importType;
            }


            @Override
            public boolean isInternal() {
                return false;
            }

            @Override
            public String getDescription() {
                return "Chuck importer";
            }

            @Override
            public void importSources(FolderEntry baseFolder, String location, Map<String, String> parameters)
                    throws ConflictException, ServerException, ForbiddenException {
                importSources(baseFolder, location, parameters, LineConsumerFactory.NULL);
            }

            @Override
            public void importSources(FolderEntry baseFolder, String location, Map<String, String> parameters,
                                      LineConsumerFactory importOutputConsumerFactory)
                    throws ConflictException, ServerException, ForbiddenException {
                // Don't really use location in this test.
                baseFolder.getVirtualFile().unzip(zip, true, 0);
                folderHolder.set(baseFolder);
            }


            @Override
            public ImporterCategory getCategory() {
                return ImporterCategory.ARCHIVE;
            }
        });

        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Arrays.asList("application/json"));
        byte[] b = String.format("{\"source\":{\"project\":{\"location\":null,\"type\":\"%s\",\"parameters\":{}},\"runners\":{}}}", importType).getBytes();
        ContainerResponse response = launcher.service("POST",
                                                      String.format("http://localhost:8080/api/project/%s/import/new_project", workspace),
                                                      "http://localhost:8080/api", headers, b, null);
        assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        ImportResponse importResponse = (ImportResponse)response.getEntity();
        Assert.assertTrue(importResponse.getProjectDescriptor().getProblems().isEmpty());
        assertEquals(importResponse.getProjectDescriptor().getDescription(), "import test");
        assertEquals(importResponse.getProjectDescriptor().getType(), "chuck_project_type");
        assertEquals(importResponse.getProjectDescriptor().getAttributes().get("x"), Arrays.asList("a", "b"));

        Project newProject = pm.getProject(workspace, "new_project");
        assertNotNull(newProject);
    }



    @Test
    public void testImportProjectWithModules() throws Exception {


        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ZipOutputStream zipOut = new ZipOutputStream(bout);
        zipOut.putNextEntry(new ZipEntry("module1/"));
        zipOut.putNextEntry(new ZipEntry("module1/marker"));
        zipOut.write("to be or not to be".getBytes());
        zipOut.putNextEntry(new ZipEntry(Constants.CODENVY_DIR + "/"));
        zipOut.putNextEntry(new ZipEntry(Constants.CODENVY_PROJECT_FILE_RELATIVE_PATH));
        zipOut.write(("{\"type\":\"chuck_project_type\"," +
                      "\"description\":\"import test\"," +
                      "\"attributes\":{\"x\": [\"a\",\"b\"]}}").getBytes());
        zipOut.close();
        final InputStream zip = new ByteArrayInputStream(bout.toByteArray());
        final String importType = "_123_";
        final ValueHolder<FolderEntry> folderHolder = new ValueHolder<>();
        importerRegistry.register(new ProjectImporter() {
            @Override
            public String getId() {
                return importType;
            }


            @Override
            public boolean isInternal() {
                return false;
            }

            @Override
            public String getDescription() {
                return "Chuck importer";
            }

            @Override
            public void importSources(FolderEntry baseFolder, String location, Map<String, String> parameters)
                    throws ConflictException, ServerException, ForbiddenException {
                importSources(baseFolder, location, parameters, LineConsumerFactory.NULL);
            }

            @Override
            public void importSources(FolderEntry baseFolder, String location, Map<String, String> parameters,
                                      LineConsumerFactory importOutputConsumerFactory)
                    throws ConflictException, ServerException, ForbiddenException {
                // Don't really use location in this test.
                baseFolder.getVirtualFile().unzip(zip, true, 0);
                folderHolder.set(baseFolder);
            }


            @Override
            public ImporterCategory getCategory() {
                return ImporterCategory.ARCHIVE;
            }
        });


        phRegistry.register(new PostImportProjectHandler() {
            @Override
            public void onProjectImported(FolderEntry projectFolder)
                    throws ForbiddenException, ConflictException, ServerException, IOException, NotFoundException {
                VirtualFileEntry module1 = projectFolder.getChild("module1");
                pm.addModule(workspace, projectFolder.getPath(), module1.getPath(), new ProjectConfig("ggg", "chuck_project_type"), null, null);

            }

            @Override
            public String getProjectType() {
                return "chuck_project_type";
            }
        });



        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Arrays.asList("application/json"));
        byte[] b = String.format("{\"source\":{\"project\":{\"location\":null,\"type\":\"%s\",\"parameters\":{}},\"runners\":{}}}", importType).getBytes();
        ContainerResponse response = launcher.service("POST",
                                                      String.format("http://localhost:8080/api/project/%s/import/new_project", workspace),
                                                      "http://localhost:8080/api", headers, b, null);
        assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        ImportResponse importResponse = (ImportResponse)response.getEntity();
        ProjectDescriptor descriptor = importResponse.getProjectDescriptor();
        Assert.assertTrue(descriptor.getProblems().isEmpty());
        assertEquals(descriptor.getDescription(), "import test");
        assertEquals(descriptor.getType(), "chuck_project_type");
        Project newProject = pm.getProject(workspace, "new_project");
        assertNotNull(newProject);
        assertNotNull(newProject.getModules());
        assertEquals(newProject.getModules().get().size(), 1);
        Project module = pm.getProject(workspace, "new_project/module1");
        assertNotNull(module);
        ProjectConfig moduleConfig = module.getConfig();
        assertNotNull(moduleConfig);
        assertEquals(moduleConfig.getTypeId(), "chuck_project_type");
    }


    @Test
    public void testImportProjectWithVisibility() throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ZipOutputStream zipOut = new ZipOutputStream(bout);
        zipOut.putNextEntry(new ZipEntry("folder1/"));
        zipOut.putNextEntry(new ZipEntry("folder1/file1.txt"));
        zipOut.write("to be or not to be".getBytes());
        zipOut.close();
        final InputStream zip = new ByteArrayInputStream(bout.toByteArray());
        final String importType = "_123_";
        final ValueHolder<FolderEntry> folderHolder = new ValueHolder<>();
        importerRegistry.register(new ProjectImporter() {
            @Override
            public String getId() {
                return importType;
            }


            @Override
            public boolean isInternal() {
                return false;
            }

            @Override
            public String getDescription() {
                return "Chuck importer";
            }

            @Override
            public void importSources(FolderEntry baseFolder, String location, Map<String, String> parameters)
                    throws ConflictException, ServerException, ForbiddenException {
                importSources(baseFolder, location, parameters, LineConsumerFactory.NULL);
            }

            @Override
            public void importSources(FolderEntry baseFolder, String location, Map<String, String> parameters,
                                      LineConsumerFactory importOutputConsumerFactory)
                    throws ConflictException, ServerException, ForbiddenException {
                // Don't really use location in this test.
                baseFolder.getVirtualFile().unzip(zip, true, 0);
                folderHolder.set(baseFolder);
            }


            @Override
            public ImporterCategory getCategory() {
                return ImporterCategory.ARCHIVE;
            }
        });

        String visibility = "private";
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Arrays.asList("application/json"));

        byte[] b = String.format("{\"project\":{\"name\":null,\"type\":\"%s\",\"attributes\":{},\"visibility\":\"%s\",\"description\":null," +
                                 "\"builders\":null,\"runners\":null},\"source\":{\"project\":{\"location\":null,\"type\":\"%s\"," +
                                 "\"parameters\":{}},\"runners\":{}}}", "chuck_project_type", visibility, importType).getBytes();
        ContainerResponse response = launcher.service("POST",
                                                      String.format("http://localhost:8080/api/project/%s/import/new_project", workspace),
                                                      "http://localhost:8080/api", headers, b, null);
        assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        ImportResponse importResponse = (ImportResponse)response.getEntity();
        assertNotNull(importResponse.getProjectDescriptor().getVisibility());
        assertEquals(importResponse.getProjectDescriptor().getVisibility(), visibility);
        Project newProject = pm.getProject(workspace, "new_project");
        assertNotNull(newProject);
        assertNotNull(newProject.getVisibility());
        assertEquals(newProject.getVisibility(), visibility);
    }


    @Test
    public void testImportProjectWithProjectType() throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ZipOutputStream zipOut = new ZipOutputStream(bout);
        zipOut.putNextEntry(new ZipEntry("folder1/"));
        zipOut.putNextEntry(new ZipEntry("folder1/file1.txt"));
        zipOut.write("to be or not to be".getBytes());
        zipOut.close();
        final InputStream zip = new ByteArrayInputStream(bout.toByteArray());
        final String importType = "_123_";
        final ValueHolder<FolderEntry> folderHolder = new ValueHolder<>();
        importerRegistry.register(new ProjectImporter() {
            @Override
            public String getId() {
                return importType;
            }


            @Override
            public boolean isInternal() {
                return false;
            }

            @Override
            public String getDescription() {
                return "Chuck importer";
            }

            @Override
            public void importSources(FolderEntry baseFolder, String location, Map<String, String> parameters)
                    throws ConflictException, ServerException, ForbiddenException {
                importSources(baseFolder, location, parameters, LineConsumerFactory.NULL);
            }

            @Override
            public void importSources(FolderEntry baseFolder, String location, Map<String, String> parameters,
                                      LineConsumerFactory importOutputConsumerFactory)
                    throws ConflictException, ServerException, ForbiddenException {
                // Don't really use location in this test.
                baseFolder.getVirtualFile().unzip(zip, true, 0);
                folderHolder.set(baseFolder);
            }


            @Override
            public ImporterCategory getCategory() {
                return ImporterCategory.ARCHIVE;
            }
        });


        ImportProject dto = DtoFactory.getInstance().createDto(ImportProject.class).withProject(DtoFactory.getInstance().createDto(NewProject.class)).withSource(DtoFactory.getInstance().createDto(Source.class).withProject(DtoFactory.getInstance().createDto(ImportSourceDescriptor.class)));
        System.out.println(dto.toString());

        String myType = "chuck_project_type";

        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Arrays.asList("application/json"));

        byte[] b = String.format("{\"project\":{\"name\":null,\"type\":\"%s\",\"attributes\":{},\"visibility\":null,\"description\":null," +
                                 "\"builders\":null,\"runners\":null},\"source\":{\"project\":{\"location\":null,\"type\":\"%s\"," +
                                 "\"parameters\":{}},\"runners\":{}}}",myType, importType).getBytes();
        ContainerResponse response = launcher.service("POST",
                                                      String.format("http://localhost:8080/api/project/%s/import/new_project", workspace),
                                                      "http://localhost:8080/api", headers, b, null);
        assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        ImportResponse importResponse = (ImportResponse)response.getEntity();
        assertNotNull(importResponse.getProjectDescriptor().getType());
        assertEquals(importResponse.getProjectDescriptor().getType(), "chuck_project_type");
        Project newProject = pm.getProject(workspace, "new_project");
        assertNotNull(newProject);

        assertNotNull(newProject.getConfig());
//        Assert.assertNotNull(newProject.getConfig().getTypeId());
//        Assert.assertEquals(newProject.getConfig().getTypeId(), myType);

    }





    @Test
    public void testImportProjectWithRunners() throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ZipOutputStream zipOut = new ZipOutputStream(bout);
        zipOut.putNextEntry(new ZipEntry("folder1/"));
        zipOut.putNextEntry(new ZipEntry("folder1/file1.txt"));
        zipOut.write("to be or not to be".getBytes());
        zipOut.putNextEntry(new ZipEntry(Constants.CODENVY_DIR + "/"));
        zipOut.putNextEntry(new ZipEntry(Constants.CODENVY_PROJECT_FILE_RELATIVE_PATH));
        zipOut.write(("{\"type\":\"my_project_type\"," +
                      "\"description\":\"import test\"," +
                      "\"attributes\":{\"x\": [\"a\",\"b\"]}}").getBytes());
        zipOut.close();
        final InputStream zip = new ByteArrayInputStream(bout.toByteArray());
        final String importType = "_123_";
        final ValueHolder<FolderEntry> folderHolder = new ValueHolder<>();
        Set<ProjectTypeResolver> resolvers = resolverRegistry.getResolvers();
        for (ProjectTypeResolver resolver : resolvers) {//unregistered all resolvers
            resolverRegistry.unregister(resolver);
        }
        importerRegistry.register(new ProjectImporter() {
            @Override
            public String getId() {
                return importType;
            }


            @Override
            public boolean isInternal() {
                return false;
            }

            @Override
            public String getDescription() {
                return "Chuck importer";
            }

            @Override
            public void importSources(FolderEntry baseFolder, String location, Map<String, String> parameters)
                    throws ConflictException, ServerException, ForbiddenException {
                importSources(baseFolder, location, parameters, LineConsumerFactory.NULL);
            }

            @Override
            public void importSources(FolderEntry baseFolder, String location, Map<String, String> parameters,
                                      LineConsumerFactory importOutputConsumerFactory)
                    throws ConflictException, ServerException, ForbiddenException {
                // Don't really use location in this test.
                baseFolder.getVirtualFile().unzip(zip, true, 0);
                folderHolder.set(baseFolder);
            }


            @Override
            public ImporterCategory getCategory() {
                return ImporterCategory.ARCHIVE;
            }
        });

        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Arrays.asList("application/json"));

        byte[] b = String.format(
                "{\"source\":{\"project\":{\"location\":\"host.com/some/path\",\"type\":\"%s\",\"parameters\":{}},\"runners\":{}}," +
                "\"project\":{\"name\":\"spring\",\"visibility\":\"public\",\"runners\":{\"configs\":{\"recommend\":{\"options\":{}," +
                "\"variables\":{},\"ram\":256}},\"default\":\"system:/java/web/tomcat7\"},\"builders\":{\"default\":\"maven\"}," +
                "\"type\":\"chuck_project_type\",\"attributes\":{},\"description\":\"import test\"},\"variables\":[],\"v\":\"2.0\"}",
                importType).getBytes();
        ContainerResponse response = launcher.service("POST",
                                                      String.format("http://localhost:8080/api/project/%s/import/new_project", workspace),
                                                      "http://localhost:8080/api", headers, b, null);


        assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        ImportResponse importResponse = (ImportResponse)response.getEntity();
//        assertNotNull(importResponse.getProjectDescriptor().getRunners());
//        assertEquals(importResponse.getProjectDescriptor().getRunners().getDefault(), "system:/java/web/tomcat7");
        Project newProject = pm.getProject(workspace, "new_project");
        assertNotNull(newProject);
//        VirtualFileEntry environments = newProject.getBaseFolder().getChild(Constants.CODENVY_RUNNER_ENVIRONMENTS_DIR);
//        assertNotNull(environments);
//        Assert.assertTrue(environments.isFolder());

    }

    @Test
    public void testImportProjectWithBuilders() throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ZipOutputStream zipOut = new ZipOutputStream(bout);
        zipOut.putNextEntry(new ZipEntry("folder1/"));
        zipOut.putNextEntry(new ZipEntry("folder1/file1.txt"));
        zipOut.write("to be or not to be".getBytes());
        zipOut.putNextEntry(new ZipEntry(Constants.CODENVY_DIR + "/"));
        zipOut.putNextEntry(new ZipEntry(Constants.CODENVY_PROJECT_FILE_RELATIVE_PATH));
        zipOut.write(("{\"type\":\"my_project_type\"," +
                      "\"description\":\"import test\"," +
                      "\"attributes\":{\"x\": [\"a\",\"b\"]}}").getBytes());
        zipOut.close();
        final InputStream zip = new ByteArrayInputStream(bout.toByteArray());
        final String importType = "_123_";
        final ValueHolder<FolderEntry> folderHolder = new ValueHolder<>();
        Set<ProjectTypeResolver> resolvers = resolverRegistry.getResolvers();
        for (ProjectTypeResolver resolver : resolvers) {//unregistered all resolvers
            resolverRegistry.unregister(resolver);
        }
        importerRegistry.register(new ProjectImporter() {
            @Override
            public String getId() {
                return importType;
            }


            @Override
            public boolean isInternal() {
                return false;
            }

            @Override
            public String getDescription() {
                return "Chuck importer";
            }

            @Override
            public void importSources(FolderEntry baseFolder, String location, Map<String, String> parameters)
                    throws ConflictException, ServerException, ForbiddenException {
                importSources(baseFolder, location, parameters, LineConsumerFactory.NULL);
            }

            @Override
            public void importSources(FolderEntry baseFolder, String location, Map<String, String> parameters,
                                      LineConsumerFactory importOutputConsumerFactory)
                    throws ConflictException, ServerException, ForbiddenException {
                // Don't really use location in this test.
                baseFolder.getVirtualFile().unzip(zip, true, 0);
                folderHolder.set(baseFolder);
            }


            @Override
            public ImporterCategory getCategory() {
                return ImporterCategory.ARCHIVE;
            }
        });

        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Arrays.asList("application/json"));

        byte[] b = String.format(
                "{\"source\":{\"project\":{\"location\":\"host.com/some/path\",\"type\":\"%s\",\"parameters\":{}},\"runners\":{}}," +
                "\"project\":{\"name\":\"spring\",\"visibility\":\"public\",\"runners\":{},\"builders\":{\"default\":\"maven\"}," +
                "\"type\":\"chuck_project_type\",\"attributes\":{},\"description\":\"import test\"},\"variables\":[],\"v\":\"2.0\"}",
                importType).getBytes();
        ContainerResponse response = launcher.service("POST",
                                                      String.format("http://localhost:8080/api/project/%s/import/new_project", workspace),
                                                      "http://localhost:8080/api", headers, b, null);
        assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        ImportResponse importResponse = (ImportResponse)response.getEntity();
//        assertNotNull(importResponse.getProjectDescriptor().getBuilders());
//        assertEquals(importResponse.getProjectDescriptor().getBuilders().getDefault(), "maven");
        Project newProject = pm.getProject(workspace, "new_project");
        assertNotNull(newProject);
//        VirtualFileEntry environments = newProject.getBaseFolder().getChild(Constants.CODENVY_RUNNER_ENVIRONMENTS_DIR);
//        assertNotNull(environments);
//        Assert.assertTrue(environments.isFolder());

    }

    @Test
    public void testImportProjectWithAttributes() throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ZipOutputStream zipOut = new ZipOutputStream(bout);
        zipOut.putNextEntry(new ZipEntry("folder1/"));
        zipOut.putNextEntry(new ZipEntry("folder1/file1.txt"));
        zipOut.write("to be or not to be".getBytes());
        zipOut.putNextEntry(new ZipEntry(Constants.CODENVY_DIR + "/"));
        zipOut.putNextEntry(new ZipEntry(Constants.CODENVY_PROJECT_FILE_RELATIVE_PATH));
        zipOut.write(("{\"type\":\"my_project_type\"," +
                      "\"description\":\"import test\"," +
                      "\"attributes\":{\"x\": [\"a\",\"b\"]}}").getBytes());
        zipOut.close();
        final InputStream zip = new ByteArrayInputStream(bout.toByteArray());
        final String importType = "_123_";
        final ValueHolder<FolderEntry> folderHolder = new ValueHolder<>();
        Set<ProjectTypeResolver> resolvers = resolverRegistry.getResolvers();
        for (ProjectTypeResolver resolver : resolvers) {//unregistered all resolvers
            resolverRegistry.unregister(resolver);
        }
        importerRegistry.register(new ProjectImporter() {
            @Override
            public String getId() {
                return importType;
            }


            @Override
            public boolean isInternal() {
                return false;
            }

            @Override
            public String getDescription() {
                return "Chuck importer";
            }

            @Override
            public void importSources(FolderEntry baseFolder, String location, Map<String, String> parameters)
                    throws ConflictException, ServerException, ForbiddenException {
                importSources(baseFolder, location, parameters, LineConsumerFactory.NULL);
            }

            @Override
            public void importSources(FolderEntry baseFolder, String location, Map<String, String> parameters,
                                      LineConsumerFactory importOutputConsumerFactory)
                    throws ConflictException, ServerException, ForbiddenException {
                // Don't really use location in this test.
                baseFolder.getVirtualFile().unzip(zip, true, 0);
                folderHolder.set(baseFolder);
            }


            @Override
            public ImporterCategory getCategory() {
                return ImporterCategory.ARCHIVE;
            }
        });

        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Arrays.asList("application/json"));

        byte[] b = String.format(
                "{\"source\":{\"project\":{\"location\":\"host.com/some/path\",\"type\":\"%s\",\"parameters\":{}},\"runners\":{}}," +
                "\"project\":{\"name\":\"spring\",\"visibility\":\"public\",\"runners\":{\"configs\":{\"recommend\":{\"options\":{}," +
                "\"variables\":{},\"ram\":256}},\"default\":\"system:/java/web/tomcat7\"},\"builders\":{\"default\":\"maven\"}," +
                "\"type\":\"chuck_project_type\",\"attributes\":{\"y\": [\"q\",\"z\"]},\"description\":\"import test\"},\"variables\":[],\"v\":\"2.0\"}",
                importType).getBytes();
        ContainerResponse response = launcher.service("POST",
                                                      String.format("http://localhost:8080/api/project/%s/import/new_project", workspace),
                                                      "http://localhost:8080/api", headers, b, null);
        assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        ImportResponse importResponse = (ImportResponse)response.getEntity();
        assertNotNull(importResponse.getProjectDescriptor().getAttributes());
        assertEquals(importResponse.getProjectDescriptor().getAttributes().get("x"), Arrays.asList("a", "b"));
        //Assert.assertEquals(importResponse.getAttributes().get("y"), Arrays.asList("q", "z"));
        Project newProject = pm.getProject(workspace, "new_project");
        assertNotNull(newProject);
//        VirtualFileEntry environments = newProject.getBaseFolder().getChild(Constants.CODENVY_RUNNER_ENVIRONMENTS_DIR);
//        assertNotNull(environments);
//        Assert.assertTrue(environments.isFolder());
    }

    @Test
    public void testImportZip() throws Exception {
        Project myProject = pm.getProject(workspace, "my_project");
        myProject.getBaseFolder().createFolder("a/b");

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ZipOutputStream zipOut = new ZipOutputStream(bout);
        zipOut.putNextEntry(new ZipEntry("folder1/"));
        zipOut.putNextEntry(new ZipEntry("folder1/file1.txt"));
        zipOut.write("to be or not to be".getBytes());
        zipOut.close();
        byte[] zip = bout.toByteArray();
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Arrays.asList("application/zip"));
        ContainerResponse response = launcher.service("POST",
                                                      String.format("http://localhost:8080/api/project/%s/import/my_project/a/b",
                                                                    workspace),
                                                      "http://localhost:8080/api", headers, zip, null);
        assertEquals(response.getStatus(), 201, "Error: " + response.getEntity());
        assertEquals(response.getHttpHeaders().getFirst("Location"),
                     URI.create(String.format("http://localhost:8080/api/project/%s/children/my_project/a/b", workspace)));
        assertNotNull(myProject.getBaseFolder().getChild("a/b/folder1/file1.txt"));
    }


    @Test
    public void testImportZipWithoutSkipFirstLevel() throws Exception {
        Project myProject = pm.getProject(workspace, "my_project");
        myProject.getBaseFolder().createFolder("a/b");

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ZipOutputStream zipOut = new ZipOutputStream(bout);
        zipOut.putNextEntry(new ZipEntry("folder1/"));
        zipOut.putNextEntry(new ZipEntry("folder1/folder2/"));
        zipOut.putNextEntry(new ZipEntry("folder1/folder2/file1.txt"));
        zipOut.write("to be or not to be".getBytes());
        zipOut.close();
        byte[] zip = bout.toByteArray();
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Arrays.asList("application/zip"));
        ContainerResponse response = launcher.service("POST",
                                                      String.format("http://localhost:8080/api/project/%s/import/my_project/a/b?skipFirstLevel=false",
                                                                    workspace),
                                                      "http://localhost:8080/api", headers, zip, null);
        assertEquals(response.getStatus(), 201, "Error: " + response.getEntity());
        assertEquals(response.getHttpHeaders().getFirst("Location"),
                     URI.create(String.format("http://localhost:8080/api/project/%s/children/my_project/a/b", workspace)));
        assertNotNull(myProject.getBaseFolder().getChild("a/b/folder1/"));
        assertNotNull(myProject.getBaseFolder().getChild("a/b/folder1/folder2"));
        assertNotNull(myProject.getBaseFolder().getChild("a/b/folder1/folder2/file1.txt"));
    }


    @Test
    public void testExportZip() throws Exception {
        Project myProject = pm.getProject(workspace, "my_project");
        myProject.getBaseFolder().createFolder("a/b").createFile("test.txt", "hello".getBytes(), "text/plain");
        ContainerResponse response = launcher.service("GET",
                                                      String.format("http://localhost:8080/api/project/%s/export/my_project", workspace),
                                                      "http://localhost:8080/api", null, null, null);
        assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        assertEquals(response.getContentType().toString(), "application/zip");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetChildren() throws Exception {
        Project myProject = pm.getProject(workspace, "my_project");
        FolderEntry a = myProject.getBaseFolder().createFolder("a");
        a.createFolder("b");
        a.createFile("test.txt", "test".getBytes(), "text/plain");
        ContainerResponse response = launcher.service("GET",
                                                      String.format("http://localhost:8080/api/project/%s/children/my_project/a",
                                                                    workspace),
                                                      "http://localhost:8080/api", null, null, null);
        assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        List<ItemReference> result = (List<ItemReference>)response.getEntity();
        assertEquals(result.size(), 2);
        Set<String> names = new LinkedHashSet<>(2);
        for (ItemReference itemReference : result) {
            names.add(itemReference.getName());
        }
        Assert.assertTrue(names.contains("b"));
        Assert.assertTrue(names.contains("test.txt"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetItem() throws Exception {
        Project myProject = pm.getProject(workspace, "my_project");
        FolderEntry a = myProject.getBaseFolder().createFolder("a");
        a.createFolder("b");
        a.createFile("test.txt", "test".getBytes(), "text/plain");
        ContainerResponse response = launcher.service("GET",
                String.format("http://localhost:8080/api/project/%s/item/my_project/a/b",
                        workspace),
                "http://localhost:8080/api", null, null, null);
        assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());

        ItemReference result = (ItemReference)response.getEntity();
        assertEquals(result.getType(), "folder");
        assertEquals(result.getName(), "b");

        response = launcher.service("GET",
                String.format("http://localhost:8080/api/project/%s/item/my_project/a/test.txt",
                        workspace),
                "http://localhost:8080/api", null, null, null);
        assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        result = (ItemReference)response.getEntity();
        assertEquals(result.getType(), "file");
        assertEquals(result.getMediaType(), "text/plain");

    }


    @Test
    @SuppressWarnings("unchecked")
    public void testGetItemWithHandler() throws Exception {



        final Project myProject = pm.getProject(workspace, "my_project");


//        final Map <String, String> attrs = new HashMap<>();
//        attrs.put("my", "myValue");

        GetItemHandler myHandler = new GetItemHandler() {
            @Override
            public void onGetItem(VirtualFileEntry virtualFile) {

                virtualFile.getAttributes().put("my", "myValue");
                if(virtualFile.isFile())
                    virtualFile.getAttributes().put("file", "a");
            }

            @Override
            public String getProjectType() {
                return "my_project_type";
            }
        };
        pm.getHandlers().register(myHandler);

        FolderEntry a = myProject.getBaseFolder().createFolder("a");
        a.createFolder("b");
        a.createFile("test.txt", "test".getBytes(), "text/plain");
        ContainerResponse response = launcher.service("GET",
                String.format("http://localhost:8080/api/project/%s/item/my_project/a/b",
                        workspace),
                "http://localhost:8080/api", null, null, null);
        assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());

        ItemReference result = (ItemReference)response.getEntity();
        assertEquals(result.getType(), "folder");
        assertEquals(result.getName(), "b");
        assertNotNull(result.getCreated());
        assertNotNull(result.getModified());
        assertEquals(result.getAttributes().size(), 1);

        response = launcher.service("GET",
                String.format("http://localhost:8080/api/project/%s/item/my_project/a/test.txt",
                        workspace),
                "http://localhost:8080/api", null, null, null);
        assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        result = (ItemReference)response.getEntity();
        assertEquals(result.getType(), "file");
        assertEquals(result.getMediaType(), "text/plain");
        assertNotNull(result.getContentLength());
        assertEquals(result.getAttributes().size(), 2);

//        System.out.println(">>>>> "+result);
//
//        System.out.println(">>>>> "+new Date(new Long(result.getAttributes().get("created"))));

    }

    @Test
    public void testGetTree() throws Exception {
        Project myProject = pm.getProject(workspace, "my_project");
        FolderEntry a = myProject.getBaseFolder().createFolder("a");
        a.createFolder("b/c");
        a.createFolder("x/y");
        a.createFile("test.txt", "test".getBytes(), "text/plain");
        ContainerResponse response = launcher.service("GET",
                                                      String.format("http://localhost:8080/api/project/%s/tree/my_project/a", workspace),
                                                      "http://localhost:8080/api", null, null, null);
        assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        TreeElement tree = (TreeElement)response.getEntity();
        ItemReference a_node = tree.getNode();
        assertEquals(a_node.getName(), "a");
        validateFolderLinks(a_node);
        List<TreeElement> children = tree.getChildren();
        assertNotNull(children);
        assertEquals(children.size(), 2);
        Set<String> names = new LinkedHashSet<>(2);
        for (TreeElement subTree : children) {
            ItemReference _node = subTree.getNode();
            validateFolderLinks(_node);
            names.add(_node.getName());
            Assert.assertTrue(subTree.getChildren().isEmpty()); // default depth is 1
        }
        Assert.assertTrue(names.contains("b"));
        Assert.assertTrue(names.contains("x"));
    }

    @Test
    public void testGetTreeWithDepth() throws Exception {
        Project myProject = pm.getProject(workspace, "my_project");
        FolderEntry a = myProject.getBaseFolder().createFolder("a");
        a.createFolder("b/c");
        a.createFolder("x/y");
        a.createFile("test.txt", "test".getBytes(), "text/plain");
        ContainerResponse response = launcher.service("GET",
                                                      String.format("http://localhost:8080/api/project/%s/tree/my_project/a?depth=2",
                                                                    workspace),
                                                      "http://localhost:8080/api", null, null, null);
        assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        TreeElement tree = (TreeElement)response.getEntity();
        ItemReference a_node = tree.getNode();
        assertEquals(a_node.getName(), "a");
        List<TreeElement> children = tree.getChildren();
        assertNotNull(children);
        Set<String> names = new LinkedHashSet<>(4);
        for (TreeElement subTree : children) {
            ItemReference _node = subTree.getNode();
            validateFolderLinks(_node);
            String name = _node.getName();
            names.add(name);
            for (TreeElement subSubTree : subTree.getChildren()) {
                ItemReference __node = subSubTree.getNode();
                validateFolderLinks(__node);
                names.add(name + "/" + __node.getName());
            }
        }
        Assert.assertTrue(names.contains("b"));
        Assert.assertTrue(names.contains("x"));
        Assert.assertTrue(names.contains("b/c"));
        Assert.assertTrue(names.contains("x/y"));
    }

    @Test
    public void testSwitchProjectVisibilityToPrivate() throws Exception {
        Project myProject = pm.getProject(workspace, "my_project");
        ContainerResponse response = launcher.service("POST",
                                                      String.format(
                                                              "http://localhost:8080/api/project/%s/switch_visibility/my_project?visibility=private",
                                                              workspace),
                                                      "http://localhost:8080/api", null, null, null);
        assertEquals(response.getStatus(), 204, "Error: " + response.getEntity());
        // Private project is accessible only for user who are in the group "workspace/developer"
        Map<Principal, Set<String>> permissions = myProject.getBaseFolder().getVirtualFile().getPermissions();
        assertEquals(permissions.size(), 1);
        Principal principal = DtoFactory.getInstance().createDto(Principal.class)
                                        .withName("workspace/developer")
                                        .withType(Principal.Type.GROUP);
        assertEquals(permissions.get(principal), Arrays.asList(VirtualFileSystemInfo.BasicPermissions.ALL.value()));

        response = launcher.service("GET",
                                    String.format("http://localhost:8080/api/project/%s/my_project", workspace),
                                    "http://localhost:8080/api", null, null, null);
        assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        ProjectDescriptor descriptor = (ProjectDescriptor)response.getEntity();
        assertEquals(descriptor.getVisibility(), "private");
    }

    @Test
    public void testUpdateProjectVisibilityToPublic() throws Exception {
        Project myProject = pm.getProject(workspace, "my_project");
        myProject.setVisibility("private");
        ContainerResponse response = launcher.service("POST",
                                                      String.format(
                                                              "http://localhost:8080/api/project/%s/switch_visibility/my_project?visibility=public",
                                                              workspace),
                                                      "http://localhost:8080/api", null, null, null);
        assertEquals(response.getStatus(), 204, "Error: " + response.getEntity());
        // List of permissions should be cleared. After that project inherits permissions from parent folder (typically root folder)
        Map<Principal, Set<String>> permissions = myProject.getBaseFolder().getVirtualFile().getPermissions();
        assertEquals(permissions.size(), 0);

        response = launcher.service("GET",
                                    String.format("http://localhost:8080/api/project/%s/my_project", workspace),
                                    "http://localhost:8080/api", null, null, null);
        assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        ProjectDescriptor descriptor = (ProjectDescriptor)response.getEntity();
        assertEquals(descriptor.getVisibility(), "public");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSearchByName() throws Exception {
        Project myProject = pm.getProject(workspace, "my_project");
        myProject.getBaseFolder().createFolder("a/b").createFile("test.txt", "hello".getBytes(), "text/plain");
        myProject.getBaseFolder().createFolder("x/y").createFile("test.txt", "test".getBytes(), "text/plain");
        myProject.getBaseFolder().createFolder("c").createFile("exclude", "test".getBytes(), "text/plain");

        ContainerResponse response = launcher.service("GET",
                                                      String.format("http://localhost:8080/api/project/%s/search/my_project?name=test.txt",
                                                                    workspace),
                                                      "http://localhost:8080/api", null, null, null);
        assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        List<ItemReference> result = (List<ItemReference>)response.getEntity();
        assertEquals(result.size(), 2);
        Set<String> paths = new LinkedHashSet<>(2);
        for (ItemReference itemReference : result) {
            paths.add(itemReference.getPath());
        }
        Assert.assertTrue(paths.contains("/my_project/a/b/test.txt"));
        Assert.assertTrue(paths.contains("/my_project/x/y/test.txt"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSearchByText() throws Exception {
        Project myProject = pm.getProject(workspace, "my_project");
        myProject.getBaseFolder().createFolder("a/b").createFile("test.txt", "hello".getBytes(), "text/plain");
        myProject.getBaseFolder().createFolder("x/y").createFile("__test.txt", "searchhit".getBytes(), "text/plain");
        myProject.getBaseFolder().createFolder("c").createFile("_test", "searchhit".getBytes(), "text/plain");

        ContainerResponse response = launcher.service("GET",
                                                      String.format("http://localhost:8080/api/project/%s/search/my_project?text=searchhit",
                                                                    workspace),
                                                      "http://localhost:8080/api", null, null, null);
        assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        List<ItemReference> result = (List<ItemReference>)response.getEntity();
        assertEquals(result.size(), 2);
        Set<String> paths = new LinkedHashSet<>(2);
        for (ItemReference itemReference : result) {
            paths.add(itemReference.getPath());
        }
        Assert.assertTrue(paths.contains("/my_project/x/y/__test.txt"));
        Assert.assertTrue(paths.contains("/my_project/c/_test"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSearchByMediaType() throws Exception {
        Project myProject = pm.getProject(workspace, "my_project");
        myProject.getBaseFolder().createFolder("a/b").createFile("test.txt", "6769675".getBytes(), "text/plain");
        myProject.getBaseFolder().createFolder("x/y").createFile("test.txt", "132434".getBytes(), "text/plain");
        myProject.getBaseFolder().createFolder("c").createFile("test", "2343124".getBytes(), "text/plain");

        ContainerResponse response = launcher.service("GET",
                                                      String.format(
                                                              "http://localhost:8080/api/project/%s/search/my_project?mediatype=text/plain",
                                                              workspace),
                                                      "http://localhost:8080/api", null, null, null);
        assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        List<ItemReference> result = (List<ItemReference>)response.getEntity();
        assertEquals(result.size(), 3);
        Set<String> paths = new LinkedHashSet<>(3);
        for (ItemReference itemReference : result) {
            paths.add(itemReference.getPath());
        }
        Assert.assertTrue(paths.contains("/my_project/x/y/test.txt"));
        Assert.assertTrue(paths.contains("/my_project/a/b/test.txt"));
        Assert.assertTrue(paths.contains("/my_project/c/test"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSearchByNameAndTextAndMediaType() throws Exception {
        Project myProject = pm.getProject(workspace, "my_project");
        myProject.getBaseFolder().createFolder("a/b").createFile("test.txt", "test".getBytes(), "text/plain");
        myProject.getBaseFolder().createFolder("x/y").createFile("test.txt", "test".getBytes(), "text/*");
        myProject.getBaseFolder().createFolder("c").createFile("test", "test".getBytes(), "text/plain");

        ContainerResponse response = launcher.service("GET",
                                                      String.format(
                                                              "http://localhost:8080/api/project/%s/search/my_project?text=test&name=test&mediatype=text/plain",
                                                              workspace),
                                                      "http://localhost:8080/api", null, null, null);
        assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        List<ItemReference> result = (List<ItemReference>)response.getEntity();
        assertEquals(result.size(), 1);
        Assert.assertTrue(result.get(0).getPath().equals("/my_project/c/test"));
    }


    @SuppressWarnings("unchecked")
    @Test
    public void testSearchFromWSRoot() throws Exception {
        Project myProject = pm.getProject(workspace, "my_project");
        myProject.getBaseFolder().createFolder("a/b").createFile("test.txt", "test".getBytes(), "text/plain");
        myProject.getBaseFolder().createFolder("x/y").createFile("test.txt", "test".getBytes(), "text/*");
        myProject.getBaseFolder().createFolder("c").createFile("test", "test".getBytes(), "text/plain");

        ContainerResponse response = launcher.service("GET",
                String.format(
                        "http://localhost:8080/api/project/%s/search/?text=test&name=test&mediatype=text/plain",
                        workspace),
                "http://localhost:8080/api", null, null, null);
        assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        List<ItemReference> result = (List<ItemReference>)response.getEntity();
        assertEquals(result.size(), 1);
        Assert.assertTrue(result.get(0).getPath().equals("/my_project/c/test"));
    }

    @Test
    public void testSetBasicPermissions() throws Exception {
        Project myProject = pm.getProject(workspace, "my_project");
        clearAcl(myProject);
        String user = "user";
        HashMap<String, List<String>> headers = new HashMap<>(1);
        headers.put("Content-Type", Arrays.asList("application/json"));

        AccessControlEntry entry1 = DtoFactory.getInstance().createDto(AccessControlEntry.class)
                                              .withPermissions(Arrays.asList("all"))
                                              .withPrincipal(DtoFactory.getInstance().createDto(Principal.class)
                                                                       .withName(user).withType(Principal.Type.USER));
        launcher.service("POST",
                         String.format("http://localhost:8080/api/project/%s/permissions/my_project", workspace),
                         "http://localhost:8080/api",
                         headers,
                         JsonHelper.toJson(Arrays.asList(entry1)).getBytes(),
                         null
                        );
        List<AccessControlEntry> acl = myProject.getBaseFolder().getVirtualFile().getACL();
        AccessControlEntry entry2 = null;
        for (AccessControlEntry ace : acl) {
            if (ace.getPrincipal().getName().equals(user)) {
                entry2 = ace;
            }
        }
        assertNotNull(entry2, "Not found expected ACL entry after update");

        assertEquals(entry2.getPrincipal(), entry1.getPrincipal());
        Assert.assertTrue(entry2.getPermissions().containsAll(entry1.getPermissions()));
    }

    @Test
    public void testSetCustomPermissions() throws Exception {
        Project myProject = pm.getProject(workspace, "my_project");
        clearAcl(myProject);
        String user = "user";
        HashMap<String, List<String>> headers = new HashMap<>(1);
        headers.put("Content-Type", Arrays.asList("application/json"));

        AccessControlEntry entry1 = DtoFactory.getInstance().createDto(AccessControlEntry.class)
                                              .withPermissions(Arrays.asList("custom"))
                                              .withPrincipal(DtoFactory.getInstance().createDto(Principal.class)
                                                                       .withName(user).withType(Principal.Type.USER));
        launcher.service("POST",
                         String.format("http://localhost:8080/api/project/%s/permissions/my_project", workspace),
                         "http://localhost:8080/api",
                         headers,
                         JsonHelper.toJson(Arrays.asList(entry1)).getBytes(),
                         null
                        );
        List<AccessControlEntry> acl = myProject.getBaseFolder().getVirtualFile().getACL();
        AccessControlEntry entry2 = null;
        for (AccessControlEntry ace : acl) {
            if (ace.getPrincipal().getName().equals(user)) {
                entry2 = ace;
            }
        }
        assertNotNull(entry2, "Not found expected ACL entry after update");

        assertEquals(entry2.getPrincipal(), entry1.getPrincipal());
        Assert.assertTrue(entry2.getPermissions().containsAll(entry1.getPermissions()));
    }

    @Test
    public void testSetBothBasicAndCustomPermissions() throws Exception {
        Project myProject = pm.getProject(workspace, "my_project");
        clearAcl(myProject);
        String user = "user";
        HashMap<String, List<String>> headers = new HashMap<>(1);
        headers.put("Content-Type", Arrays.asList("application/json"));

        AccessControlEntry entry1 = DtoFactory.getInstance().createDto(AccessControlEntry.class)
                                              .withPermissions(Arrays.asList("build", "run", "update_acl", "read", "write"))
                                              .withPrincipal(DtoFactory.getInstance().createDto(Principal.class)
                                                                       .withName(user).withType(Principal.Type.USER));
        launcher.service("POST",
                         String.format("http://localhost:8080/api/project/%s/permissions/my_project", workspace),
                         "http://localhost:8080/api",
                         headers,
                         JsonHelper.toJson(Arrays.asList(entry1)).getBytes(),
                         null
                        );

        List<AccessControlEntry> acl = myProject.getBaseFolder().getVirtualFile().getACL();
        AccessControlEntry entry2 = null;
        for (AccessControlEntry ace : acl) {
            if (ace.getPrincipal().getName().equals(user)) {
                entry2 = ace;
            }
        }
        assertNotNull(entry2, "Not found expected ACL entry after update");

        assertEquals(entry2.getPrincipal(), entry1.getPrincipal());
        Assert.assertTrue(entry2.getPermissions().containsAll(entry1.getPermissions()));
    }

    @Test
    public void testUpdatePermissions() throws Exception {
        Project myProject = pm.getProject(workspace, "my_project");
        AccessControlEntry newEntry = DtoFactory.getInstance().createDto(AccessControlEntry.class)
                                                .withPermissions(Arrays.asList("all"))
                                                .withPrincipal(DtoFactory.getInstance().createDto(Principal.class)
                                                                         .withName("other")
                                                                         .withType(Principal.Type.USER));
        AccessControlEntry newEntry2 = DtoFactory.getInstance().createDto(AccessControlEntry.class)
                                                 .withPermissions(Arrays.asList("all"))
                                                 .withPrincipal(DtoFactory.getInstance().createDto(Principal.class)
                                                                          .withName(vfsUser).withType(Principal.Type.USER));
        //set up basic permissions
        myProject.getBaseFolder().getVirtualFile().updateACL(Arrays.asList(newEntry, newEntry2), false, null);

        HashMap<String, List<String>> headers = new HashMap<>(1);
        headers.put("Content-Type", Arrays.asList("application/json"));

        AccessControlEntry update = DtoFactory.getInstance().createDto(AccessControlEntry.class)
                                              .withPermissions(Arrays.asList("only_custom"))
                                              .withPrincipal(DtoFactory.getInstance().createDto(Principal.class)
                                                                       .withName(vfsUser).withType(Principal.Type.USER));
        launcher.service("POST",
                         String.format("http://localhost:8080/api/project/%s/permissions/my_project", workspace),
                         "http://localhost:8080/api",
                         headers,
                         JsonHelper.toJson(Arrays.asList(update)).getBytes(),
                         null
                        );

        List<AccessControlEntry> acl = myProject.getBaseFolder().getVirtualFile().getACL();
        assertEquals(acl.size(), 2);
        Map<Principal, Set<String>> map = new HashMap<>(2);
        for (AccessControlEntry ace : acl) {
            map.put(ace.getPrincipal(), new HashSet<>(ace.getPermissions()));
        }
        assertNotNull(map.get(newEntry.getPrincipal()));
        assertNotNull(map.get(newEntry2.getPrincipal()));
        assertEquals(map.get(newEntry.getPrincipal()).size(), 1);
        assertEquals(map.get(newEntry2.getPrincipal()).size(), 1);
        Assert.assertTrue(map.get(newEntry.getPrincipal()).contains("all"));
        Assert.assertTrue(map.get(newEntry2.getPrincipal()).contains("only_custom"));
    }

    @Test
    public void testGetPermissionsForCertainUser() throws Exception {
        Project myProject = pm.getProject(workspace, "my_project");
        AccessControlEntry newEntry = DtoFactory.getInstance().createDto(AccessControlEntry.class)
                                                .withPermissions(Arrays.asList("all"))
                                                .withPrincipal(DtoFactory.getInstance().createDto(Principal.class)
                                                                         .withName(vfsUser).withType(Principal.Type.USER));
        AccessControlEntry newEntry2 = DtoFactory.getInstance().createDto(AccessControlEntry.class)
                                                 .withPermissions(Arrays.asList("all"))
                                                 .withPrincipal(DtoFactory.getInstance().createDto(Principal.class)
                                                                          .withName("other").withType(Principal.Type.USER));
        //set up permissions
        myProject.getBaseFolder().getVirtualFile().updateACL(Arrays.asList(newEntry, newEntry2), false, null);

        ContainerResponse response = launcher.service("GET",
                                                      String.format("http://localhost:8080/api/project/%s/permissions/my_project?userid=%s",
                                                                    workspace, vfsUser),
                                                      "http://localhost:8080/api",
                                                      null,
                                                      null,
                                                      null
                                                     );
        //response entity is ACL
        @SuppressWarnings("unchecked")
        List<AccessControlEntry> entries = (List<AccessControlEntry>)response.getEntity();

        assertEquals(entries.size(), 1);
        //"all" should be replaced with "read" & "write" & "update_acl", etc
        Set<String> permissions = new HashSet<>(entries.get(0).getPermissions());
        Assert.assertTrue(permissions.contains("read"));
        Assert.assertTrue(permissions.contains("write"));
        Assert.assertTrue(permissions.contains("update_acl"));
        Assert.assertTrue(permissions.contains("run"));
        Assert.assertTrue(permissions.contains("build"));
    }

    @Test
    public void testGetAllProjectPermissions() throws Exception {
        Project myProject = pm.getProject(workspace, "my_project");
        AccessControlEntry newEntry = DtoFactory.getInstance().createDto(AccessControlEntry.class)
                                                .withPermissions(Arrays.asList("all"))
                                                .withPrincipal(DtoFactory.getInstance().createDto(Principal.class)
                                                                         .withName(vfsUser).withType(Principal.Type.USER));
        AccessControlEntry newEntry2 = DtoFactory.getInstance().createDto(AccessControlEntry.class)
                                                 .withPermissions(Arrays.asList("all"))
                                                 .withPrincipal(DtoFactory.getInstance().createDto(Principal.class)
                                                                          .withName("other").withType(Principal.Type.USER));
        //set up permissions
        myProject.getBaseFolder().getVirtualFile().updateACL(Arrays.asList(newEntry, newEntry2), false, null);

        ContainerResponse response = launcher.service("GET",
                                                      String.format("http://localhost:8080/api/project/%s/permissions/my_project",
                                                                    workspace),
                                                      "http://localhost:8080/api",
                                                      null,
                                                      null,
                                                      null
                                                     );
        //response entity is ACL
        @SuppressWarnings("unchecked")
        List<AccessControlEntry> entries = (List<AccessControlEntry>)response.getEntity();

        assertEquals(entries.size(), 2);
    }

    @Test
    public void testClearPermissionsForCertainUserToCertainProject() throws Exception {
        Project myProject = pm.getProject(workspace, "my_project");
        AccessControlEntry entry = DtoFactory.getInstance().createDto(AccessControlEntry.class)
                                             .withPermissions(Arrays.asList("all"))
                                             .withPrincipal(DtoFactory.getInstance().createDto(Principal.class)
                                                                      .withName(vfsUser)
                                                                      .withType(Principal.Type.USER));
        //set up permissions
        myProject.getBaseFolder().getVirtualFile().updateACL(Arrays.asList(entry), false, null);

        HashMap<String, List<String>> headers = new HashMap<>(1);
        headers.put("Content-Type", Arrays.asList("application/json"));

        launcher.service("POST",
                         String.format("http://localhost:8080/api/project/%s/permissions/my_project", workspace),
                         "http://localhost:8080/api",
                         headers,
                         JsonHelper.toJson(Arrays.asList(entry.withPermissions(null))).getBytes(),
                         null
                        );

        assertEquals(myProject.getBaseFolder().getVirtualFile().getACL().size(), 0);
    }

//    @Test
//    public void testGetRunnerEnvironments() throws Exception {
//        Project myProject = pm.getProject(workspace, "my_project");
//        FolderEntry environmentsFolder = myProject.getBaseFolder().createFolder(".codenvy/runners/environments");
//        environmentsFolder.createFolder("my_env_1");
//        environmentsFolder.createFolder("my_env_2");
//        ContainerResponse response = launcher.service("GET",
//                                                      String.format("http://localhost:8080/api/project/%s/runner_environments/my_project",
//                                                                    workspace),
//                                                      "http://localhost:8080/api", null, null, null);
//        assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
//        RunnerEnvironmentTree runnerEnvironmentTree = (RunnerEnvironmentTree)response.getEntity();
//        assertEquals(runnerEnvironmentTree.getDisplayName(), "project");
//        List<RunnerEnvironmentLeaf> environments = runnerEnvironmentTree.getLeaves();
//        assertNotNull(environments);
//        assertEquals(environments.size(), 2);
//
//        Set<String> ids = new LinkedHashSet<>(2);
//        Set<String> names = new LinkedHashSet<>(2);
//        for (RunnerEnvironmentLeaf environment : environments) {
//            ids.add(environment.getEnvironment().getId());
//            names.add(environment.getDisplayName());
//        }
//        Assert.assertTrue(ids.contains("project://my_env_1"));
//        Assert.assertTrue(ids.contains("project://my_env_2"));
//        Assert.assertTrue(names.contains("my_env_1"));
//        Assert.assertTrue(names.contains("my_env_2"));
//    }


    @Test
    public void testEstimateProject() throws Exception {

        VirtualFile root = pm.getVirtualFileSystemRegistry().getProvider("my_ws").getMountPoint(false).getRoot();
        root.createFolder("testEstimateProjectGood").createFolder("check");
        root.createFolder("testEstimateProjectBad");

        final ValueProviderFactory vpf1 = new ValueProviderFactory() {

            @Override
            public ValueProvider newInstance(final FolderEntry projectFolder) {
                return new ValueProvider() {

                    @Override
                    public List<String> getValues(String attributeName) throws ValueStorageException {

                        VirtualFileEntry file;
                        try {
                            file = projectFolder.getChild("check");
                        } catch (ForbiddenException e) {
                            throw new ValueStorageException(e.getMessage());
                        } catch (ServerException e) {
                            throw new ValueStorageException(e.getMessage());
                        }

                        if(file == null)
                            throw new ValueStorageException("Check not found");
                        return Collections.singletonList("checked");

                    }

                    @Override
                    public void setValues(String attributeName, List<String> value) {

                        //calculateAttributeValueHolder = value;
                    }
                };
            }
        };


        ProjectType pt = new ProjectType("testEstimateProjectPT", "my testEstimateProject type", true, false) {

            {
                addVariableDefinition("calculated_attribute", "attr description", true, vpf1);
                addVariableDefinition("my_property_1", "attr description", true);
                addVariableDefinition("my_property_2", "attr description", false);
//                setDefaultBuilder("builder1");
//                setDefaultRunner("system:/runner/runner1");
            }

        };

        pm.getProjectTypeRegistry().registerProjectType(pt);


        ContainerResponse response =
                launcher.service("GET", String.format("http://localhost:8080/api/project/%s/estimate/%s?type=%s",
                                workspace, "testEstimateProjectGood","testEstimateProjectPT"),
                        "http://localhost:8080/api", null, null, null);
        assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        Map<String, List<String>> result = (Map<String, List<String>>)response.getEntity();

        assertEquals(result.size(), 1);
        assertEquals(result.get("calculated_attribute").get(0), "checked");

        response =
                launcher.service("GET", String.format("http://localhost:8080/api/project/%s/estimate/%s?type=%s",
                                workspace, "testEstimateProjectBad","testEstimateProjectPT"),
                        "http://localhost:8080/api", null, null, null);
        assertEquals(response.getStatus(), 409, "Error: " + response.getEntity());


    }


    private void validateFileLinks(ItemReference item) {
        Link link = item.getLink("delete");
        assertNotNull(link);
        assertEquals(link.getMethod(), "DELETE");
        assertEquals(link.getHref(), "http://localhost:8080/api/project/" + workspace + item.getPath());

        link = item.getLink("get content");
        assertNotNull(link);
        assertEquals(link.getMethod(), "GET");
        assertEquals(link.getProduces(), item.getMediaType());
        assertEquals(link.getHref(), "http://localhost:8080/api/project/" + workspace + "/file" + item.getPath());

        link = item.getLink("update content");
        assertNotNull(link);
        assertEquals(link.getMethod(), "PUT");
        assertEquals(link.getConsumes(), "*/*");
        assertEquals(link.getHref(), "http://localhost:8080/api/project/" + workspace + "/file" + item.getPath());
    }

    private void validateFolderLinks(ItemReference item) {
        Link link = item.getLink("children");
        assertNotNull(link);
        assertEquals(link.getMethod(), "GET");
        assertEquals(link.getHref(), "http://localhost:8080/api/project/" + workspace + "/children" + item.getPath());
        assertEquals(link.getProduces(), "application/json");

        link = item.getLink("tree");
        assertNotNull(link);
        assertEquals(link.getMethod(), "GET");
        assertEquals(link.getHref(), "http://localhost:8080/api/project/" + workspace + "/tree" + item.getPath());
        assertEquals(link.getProduces(), "application/json");

        link = item.getLink("modules");
        assertNotNull(link);
        assertEquals(link.getMethod(), "GET");
        assertEquals(link.getHref(), "http://localhost:8080/api/project/" + workspace + "/modules" + item.getPath());
        assertEquals(link.getProduces(), "application/json");

        link = item.getLink("zipball sources");
        assertNotNull(link);
        assertEquals(link.getMethod(), "GET");
        assertEquals(link.getHref(), "http://localhost:8080/api/project/" + workspace + "/export" + item.getPath());
        assertEquals(link.getProduces(), "application/zip");

        link = item.getLink("delete");
        assertNotNull(link);
        assertEquals(link.getMethod(), "DELETE");
        assertEquals(link.getHref(), "http://localhost:8080/api/project/" + workspace + item.getPath());
    }

    private void validateProjectLinks(ProjectDescriptor project) {
        Link link = project.getLink("update project");
        assertNotNull(link);
        assertEquals(link.getMethod(), "PUT");
        assertEquals(link.getHref(), "http://localhost:8080/api/project/" + workspace + project.getPath());
        assertEquals(link.getConsumes(), "application/json");
        assertEquals(link.getProduces(), "application/json");

        link = project.getLink("children");
        assertNotNull(link);
        assertEquals(link.getMethod(), "GET");
        assertEquals(link.getHref(), "http://localhost:8080/api/project/" + workspace + "/children" + project.getPath());
        assertEquals(link.getProduces(), "application/json");

        link = project.getLink("tree");
        assertNotNull(link);
        assertEquals(link.getMethod(), "GET");
        assertEquals(link.getHref(), "http://localhost:8080/api/project/" + workspace + "/tree" + project.getPath());
        assertEquals(link.getProduces(), "application/json");

        link = project.getLink("modules");
        assertNotNull(link);
        assertEquals(link.getMethod(), "GET");
        assertEquals(link.getHref(), "http://localhost:8080/api/project/" + workspace + "/modules" + project.getPath());
        assertEquals(link.getProduces(), "application/json");

        link = project.getLink("zipball sources");
        assertNotNull(link);
        assertEquals(link.getMethod(), "GET");
        assertEquals(link.getHref(), "http://localhost:8080/api/project/" + workspace + "/export" + project.getPath());
        assertEquals(link.getProduces(), "application/zip");

        link = project.getLink("delete");
        assertNotNull(link);
        assertEquals(link.getMethod(), "DELETE");
        assertEquals(link.getHref(), "http://localhost:8080/api/project/" + workspace + project.getPath());

//        link = project.getLink("get runner environments");
//        assertNotNull(link);
//        assertEquals(link.getMethod(), "GET");
//        assertEquals(link.getHref(), "http://localhost:8080/api/project/" + workspace + "/runner_environments" + project.getPath());
    }

    private void clearAcl(Project project) throws ServerException, ForbiddenException {
        project.getBaseFolder().getVirtualFile().updateACL(Collections.<AccessControlEntry>emptyList(), true, null);
    }


    private class MyProjType extends ProjectType {
        private MyProjType() {

            super("my_project_type", "my project type", true, false);
            addConstantDefinition("my_attribute", "Constant", "attribute value 1");

        }
    }

}
