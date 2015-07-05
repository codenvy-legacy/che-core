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

import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.project.server.handlers.ProjectHandler;
import org.eclipse.che.api.project.server.handlers.ProjectHandlerRegistry;
import org.eclipse.che.api.project.server.type.AttributeValue;
import org.eclipse.che.api.project.server.type.ProjectType;
import org.eclipse.che.api.project.server.type.ProjectTypeRegistry;
import org.eclipse.che.api.project.shared.dto.ProjectUpdate;
import org.eclipse.che.api.project.shared.dto.SourceEstimation;
import org.eclipse.che.api.vfs.server.VirtualFile;
import org.eclipse.che.api.vfs.server.VirtualFileSystemRegistry;
import org.eclipse.che.api.vfs.server.VirtualFileSystemUser;
import org.eclipse.che.api.vfs.server.VirtualFileSystemUserContext;
import org.eclipse.che.api.vfs.server.impl.memory.MemoryFileSystemProvider;
import org.eclipse.che.api.vfs.server.impl.memory.MemoryMountPoint;
import org.eclipse.che.dto.server.DtoFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.*;

import javax.ws.rs.core.MediaType;

/**
 * @author andrew00x
 */
public class ProjectTest {
    private static final String      vfsUserName   = "dev";
    private static final Set<String> vfsUserGroups = new LinkedHashSet<>(Arrays.asList("workspace/developer"));

    private ProjectManager pm;

    private static List<String> calculateAttributeValueHolder = Collections.singletonList("hello");

    @BeforeMethod
    public void setUp() throws Exception {

        final ValueProviderFactory vpf1 = new ValueProviderFactory() {

            @Override
            public ValueProvider newInstance(FolderEntry projectFolder) {
                return new ValueProvider() {

                    @Override
                    public List<String> getValues(String attributeName) {

                        return calculateAttributeValueHolder;
                        //Collections.singletonList("hello");
                    }

                    @Override
                    public void setValues(String attributeName, List<String> value) {

                        calculateAttributeValueHolder = value;
                    }
                };
            }
        };


        ProjectType pt = new ProjectType("my_project_type", "my project type", true, false) {

            {
                addVariableDefinition("calculated_attribute", "attr description", true, vpf1);
                addVariableDefinition("my_property_1", "attr description", true);
                addVariableDefinition("my_property_2", "attr description", false);
                setDefaultBuilder("builder1");
                setDefaultRunner("system:/runner/runner1");
            }

        };

        Set <ProjectType> types = new HashSet<ProjectType>();
        types.add(pt);
        ProjectTypeRegistry ptRegistry = new ProjectTypeRegistry(types);

        final EventService eventService = new EventService();
        VirtualFileSystemRegistry vfsRegistry = new VirtualFileSystemRegistry();

        final MemoryFileSystemProvider memoryFileSystemProvider =
                new MemoryFileSystemProvider("my_ws", eventService, new VirtualFileSystemUserContext() {
                    @Override
                    public VirtualFileSystemUser getVirtualFileSystemUser() {
                        return new VirtualFileSystemUser(vfsUserName, vfsUserGroups);
                    }
                }, vfsRegistry);
        MemoryMountPoint mmp = (MemoryMountPoint)memoryFileSystemProvider.getMountPoint(true);
        vfsRegistry.registerProvider("my_ws", memoryFileSystemProvider);


        //ProjectGeneratorRegistry pgRegistry = new ProjectGeneratorRegistry(new HashSet<ProjectGenerator>());

        ProjectHandlerRegistry phRegistry = new ProjectHandlerRegistry(new HashSet<ProjectHandler>());

        pm = new DefaultProjectManager(vfsRegistry, eventService, ptRegistry, phRegistry);

        ((DefaultProjectManager)pm).start();
        VirtualFile myVfRoot = mmp.getRoot();
        myVfRoot.createFolder("my_project").createFolder(Constants.CODENVY_DIR).createFile(Constants.CODENVY_PROJECT_FILE, null, null);
//        myVfRoot.createFolder("testEstimateProject");
    }

    @AfterMethod
    public void tearDown() {
        ((DefaultProjectManager)pm).stop();
    }


    @Test
    public void testGetProject() throws Exception {
        Project myProject = pm.getProject("my_ws", "my_project");
        Assert.assertNotNull(myProject);
    }

    @Test
    public void testGetProjectDescriptor() throws Exception {
        Project myProject = pm.getProject("my_ws", "my_project");
        Map<String, List<String>> attributes = new HashMap<>(3);
        //attributes.put("calculated_attribute", Arrays.asList("hello"));
        attributes.put("my_property_1", Arrays.asList("value_1", "value_2"));
        attributes.put("my_property_2", Arrays.asList("value_3", "value_4"));
        ProjectJson json = new ProjectJson();
        json.withType("my_project_type").withAttributes(attributes).save(myProject);
        //ProjectDescription myProjectDescription = myProject.getDescription();

        //System.out.println("JSON >> "+json.getAttributes());

        //System.out.println(">>    >>"+pm.getValueProviderFactories());

        ProjectConfig myConfig = myProject.getConfig();
        Assert.assertEquals(myConfig.getTypeId(), "my_project_type");
        //Assert.assertEquals(myProjectDescription.getProjectType().getName(), "my_project_type");

        Assert.assertEquals(pm.getProjectTypeRegistry().getProjectType("my_project_type").getAttributes().size(), 3);


        //System.out.println(">>>>"+myConfig.getAttribute("calculated_attribute"));

        Assert.assertEquals(myConfig.getAttributes().size(), 3);

        AttributeValue attributeVal;

        Assert.assertNotNull(myConfig.getAttributes().get("calculated_attribute"));
        attributeVal = myConfig.getAttributes().get("calculated_attribute");
        Assert.assertEquals(attributeVal.getList(), Arrays.asList("hello"));

        Assert.assertNotNull(myConfig.getAttributes().get("my_property_1"));
        attributeVal = myConfig.getAttributes().get("my_property_1");
        Assert.assertEquals(attributeVal.getList(), Arrays.asList("value_1", "value_2"));

        Assert.assertNotNull(myConfig.getAttributes().get("my_property_2"));
        attributeVal = myConfig.getAttributes().get("my_property_2");
        Assert.assertEquals(attributeVal.getList(), Arrays.asList("value_3", "value_4"));
    }

    @Test
    public void testUpdateProjectDescriptor() throws Exception {
        Project myProject = pm.getProject("my_ws", "my_project");
        Map<String, List<String>> attributes = new HashMap<>(2);
        attributes.put("my_property_1", Arrays.asList("value_1", "value_2"));
        ProjectJson projectJson = new ProjectJson("my_project_type", attributes, null, null, "test project");
        projectJson.save(myProject);

        Map <String, AttributeValue> attrs = new HashMap<>();
        attrs.put("calculated_attribute", new AttributeValue("updated calculated_attribute"));
        attrs.put("my_property_1", new AttributeValue("updated value 1"));
        // wont stored
        attrs.put("new_my_property_2", new AttributeValue("new value 2"));

        ProjectConfig myConfig = new ProjectConfig("descr", "my_project_type", attrs, null, null, null);

        myProject.updateConfig(myConfig);

        projectJson = ProjectJson.load(myProject);

        Assert.assertEquals(projectJson.getType(), "my_project_type");
        Assert.assertEquals(calculateAttributeValueHolder, Arrays.asList("updated calculated_attribute"));
        Map<String, List<String>> pm = projectJson.getAttributes();
        // only stored (non-provided) attributes
        Assert.assertEquals(pm.size(), 1);
        Assert.assertEquals(pm.get("my_property_1"), Arrays.asList("updated value 1"));

    }

    @Test
    public void testModificationDate() throws Exception {
        Project myProject = pm.getProject("my_ws", "my_project");
        long modificationDate1 = myProject.getModificationDate();
        Thread.sleep(1000);
        myProject.getBaseFolder().createFile("test.txt", "test".getBytes(), MediaType.TEXT_PLAIN);
        long modificationDate2 = myProject.getModificationDate();
        Assert.assertTrue(modificationDate2 > modificationDate1);
    }

    @Test
    public void testIfDefaultBuilderRunnerAppearsInProject() throws Exception {
        Project myProject = pm.getProject("my_ws", "my_project");
        Map<String, List<String>> attributes = new HashMap<>(2);
        attributes.put("my_property_1", Arrays.asList("value_1", "value_2"));
        ProjectJson projectJson = new ProjectJson("my_project_type", attributes, null , null, "test project");
        projectJson.save(myProject);

        Assert.assertNotNull(myProject.getConfig().getRunners());
        Assert.assertEquals(myProject.getConfig().getRunners().getDefault(), "system:/runner/runner1");

        Assert.assertNotNull(myProject.getConfig().getBuilders());
        Assert.assertEquals(myProject.getConfig().getBuilders().getDefault(), "builder1");
    }

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

                        VirtualFileEntry file = null;
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
                setDefaultBuilder("builder1");
                setDefaultRunner("system:/runner/runner1");
            }

        };

        pm.getProjectTypeRegistry().registerProjectType(pt);

        Map<String, AttributeValue> attrs = pm.estimateProject("my_ws", "testEstimateProjectGood", "testEstimateProjectPT");
        Assert.assertEquals(attrs.size(), 1);
        Assert.assertNotNull(attrs.get("calculated_attribute"));
        Assert.assertEquals(attrs.get("calculated_attribute").getString(), "checked");


        try {
            pm.estimateProject("my_ws", "testEstimateProjectBad", "testEstimateProjectPT");
            Assert.fail("ValueStorageException should be thrown");
        } catch (ValueStorageException e) {

        }
    }


    @Test
    public void testResolveSources() throws Exception {

        VirtualFile root = pm.getVirtualFileSystemRegistry().getProvider("my_ws").getMountPoint(false).getRoot();
        root.createFolder("testEstimateProjectGood").createFolder("check");
        root.createFolder("testEstimateProjectBad");

        final ValueProviderFactory vpf1 = new ValueProviderFactory() {

            @Override
            public ValueProvider newInstance(final FolderEntry projectFolder) {
                return new ValueProvider() {

                    @Override
                    public List<String> getValues(String attributeName) throws ValueStorageException {

                        VirtualFileEntry file = null;
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
                addVariableDefinition("my_calculated_attribute", "attr description", true, vpf1);
                addVariableDefinition("my_property_1", "attr description", true);
                addVariableDefinition("my_property_2", "attr description", false);
                setDefaultBuilder("builder1");
                setDefaultRunner("system:/runner/runner1");
            }

        };

        pm.getProjectTypeRegistry().registerProjectType(pt);

        List<SourceEstimation> estimations = pm.resolveSources("my_ws", "testEstimateProjectGood", false);


        Assert.assertEquals(estimations.size(), 2);
//        Assert.assertEquals(estimations.get(0).getAttributes().get("my_calculated_attribute").get(0), "checked");
//        Assert.assertEquals(estimations.get(0).getType(), "testEstimateProjectPT");

        estimations = pm.resolveSources("my_ws", "testEstimateProjectBad", false);
        Assert.assertEquals(estimations.size(), 1);

//        Assert.assertNotNull(attrs.get("calculated_attribute"));
//        Assert.assertEquals(attrs.get("calculated_attribute").getString(), "checked");
//
//
//        try {
//            pm.estimateProject("my_ws", "testEstimateProjectBad", "testEstimateProjectPT");
//            Assert.fail("ValueStorageException should be thrown");
//        } catch (ValueStorageException e) {
//
//        }
    }

    @Test
    public void testPTConstraints() throws Exception {

        pm.getProjectTypeRegistry().registerProjectType(new ProjectType("testMixinAndPrimary", "my type", true, true) {});
        pm.getProjectTypeRegistry().registerProjectType(new ProjectType("testPrimary", "my type", true, false) {});
        pm.getProjectTypeRegistry().registerProjectType(new ProjectType("testMixin", "my type", false, true) {});
        pm.getProjectTypeRegistry().registerProjectType(new ProjectType("testAbstract", "my type", false, false) {});

        pm.createProject("my_ws", "all", new ProjectConfig("proj", "testMixinAndPrimary"), null, null);
        pm.createProject("my_ws", "prim", new ProjectConfig("proj", "testPrimary"), null, null);


        // not possible to create Project with wrong PT
        try {
            pm.createProject("my_ws", "mix", new ProjectConfig("proj", "testMixin"), null, null);
            Assert.fail("ProjectTypeConstraintException expected");
        } catch (ProjectTypeConstraintException e) { }
        try {
            pm.createProject("my_ws", "abstr", new ProjectConfig("proj", "testAbstract"), null, null);
            Assert.fail("ProjectTypeConstraintException expected");
        } catch (ProjectTypeConstraintException e) {  }

        ProjectConfig config = pm.getProject("my_ws", "all").getConfig();
        config.getMixinTypes().add("testMixin");
        pm.getProject("my_ws", "all").updateConfig(config);

        // not possible to add wrong mixin PT
        config.getMixinTypes().add("testAbstract");
        try {
            pm.getProject("my_ws", "all").updateConfig(config);
            Assert.fail("ProjectTypeConstraintException expected");
        } catch (ProjectTypeConstraintException e) {}


    }

    @Test
    public void testAddMixin() throws Exception {

        pm.getProjectTypeRegistry().registerProjectType(new ProjectType("testPrimary", "my type", true, false) {

            {
                addConstantDefinition("c1", "","c1");
            }

        });
        pm.getProjectTypeRegistry().registerProjectType(new ProjectType("testMixin", "my type", false, true) {
            {
                addConstantDefinition("m1", "","m1");
            }
        });

        pm.createProject("my_ws", "p1", new ProjectConfig("proj", "testPrimary"), null, null);
//        pm.createProject("my_ws", "p2", new ProjectConfig("proj", "testPrimary"), null, null);


        ProjectConfig config = pm.getProject("my_ws", "p1").getConfig();
        Assert.assertEquals(config.getMixinTypes().size(), 0);
        Assert.assertEquals(config.getAttributes().size(), 1);
        config.getMixinTypes().add("testMixin");
        pm.getProject("my_ws", "p1").updateConfig(config);
        config = pm.getProject("my_ws", "p1").getConfig();
        Assert.assertEquals(config.getMixinTypes().size(), 1);
        Assert.assertEquals("testMixin", config.getMixinTypes().get(0));
        Assert.assertEquals(config.getAttributes().size(), 2);


        // add same mixin as existed
        config.getMixinTypes().add("testMixin");
        try {
            pm.getProject("my_ws", "p1").updateConfig(config);
            Assert.fail("ProjectTypeConstraintException (duplicated attributes) expected");
        } catch (ProjectTypeConstraintException e) { }
        //config = pm.getProject("my_ws", "p1").getConfig();
        //Assert.assertEquals(config.getMixinTypes().size(), 1);

    }


    @Test
    public void testAddMixinWithProvidedAttrs() throws Exception {

        final ValueProviderFactory vpfPrimary = new ValueProviderFactory() {

            @Override
            public ValueProvider newInstance(final FolderEntry projectFolder) {
                return new ValueProvider() {

                    @Override
                    public List<String> getValues(String attributeName) throws ValueStorageException {

                        VirtualFileEntry file = checkFolder();

                        if(file == null)
                            throw new ValueStorageException("Primary folder not found");
                        return Collections.singletonList("checked");

                    }

                    @Override
                    public void setValues(String attributeName, List<String> value) throws ValueStorageException {
                        if(checkFolder() == null) {
                            try {
                                projectFolder.createFolder("primary");
                            } catch (Exception e) {
                                throw new ValueStorageException(e.getMessage());
                            }
                        }

                    }

                    private VirtualFileEntry checkFolder() throws ValueStorageException {
                        VirtualFileEntry file = null;
                        try {
                            file = projectFolder.getChild("primary");
                        } catch (Exception e) {
                            throw new ValueStorageException(e.getMessage());
                        }
                        return file;
                    }


                };
            }
        };


        final ValueProviderFactory vpfMixin = new ValueProviderFactory() {

            @Override
            public ValueProvider newInstance(final FolderEntry projectFolder) {
                return new ValueProvider() {

                    @Override
                    public List<String> getValues(String attributeName) throws ValueStorageException {

                        VirtualFileEntry file = checkFolder();

                        if(file == null)
                            throw new ValueStorageException("Mixin folder not found");
                        return Collections.singletonList("checked");

                    }

                    @Override
                    public void setValues(String attributeName, List<String> value) throws ValueStorageException {
                        if(checkFolder() == null) {
                            try {
                                projectFolder.createFolder("mixin");
                            } catch (Exception e) {
                                throw new ValueStorageException(e.getMessage());
                            }
                        }
                    }

                    private VirtualFileEntry checkFolder() throws ValueStorageException {
                        VirtualFileEntry file = null;
                        try {
                            file = projectFolder.getChild("mixin");
                        } catch (Exception e) {
                            throw new ValueStorageException(e.getMessage());
                        }
                        return file;
                    }


                };
            }
        };


        pm.getProjectTypeRegistry().registerProjectType(new ProjectType("testPrimary", "my type", true, false) {

            {
                addVariableDefinition("p.calculate", "", true, vpfPrimary);
            }

        });
        pm.getProjectTypeRegistry().registerProjectType(new ProjectType("testMixin", "my type", false, true) {
            {
                addVariableDefinition("m.calculate", "", true, vpfMixin);
            }
        });

        Map <String, AttributeValue> attrs = new HashMap<>();
        attrs.put("p.calculate", new AttributeValue(""));
        ProjectConfig config = new ProjectConfig("proj", "testPrimary", attrs, null, null, null);
        Project proj = pm.createProject("my_ws", "provided", config , null, null);

        Assert.assertEquals(proj.getConfig().getMixinTypes().size(), 0);
        Assert.assertEquals(proj.getConfig().getAttributes().get("p.calculate").getString(), "checked");

        config.getMixinTypes().add("testMixin");
        config.getAttributes().put("m.calculate", new AttributeValue(""));
        proj.updateConfig(config);

        Assert.assertEquals(proj.getConfig().getMixinTypes().size(), 1);
        Assert.assertEquals(proj.getConfig().getAttributes().get("m.calculate").getString(), "checked");

        // reread it in case
        proj = pm.getProject("my_ws", "provided");
        Assert.assertEquals(proj.getConfig().getMixinTypes().size(), 1);
        Assert.assertEquals(proj.getConfig().getAttributes().get("p.calculate").getString(), "checked");
        Assert.assertEquals(proj.getConfig().getAttributes().get("m.calculate").getString(), "checked");


    }


    @Test
    public void testAddModule() throws Exception {

        pm.getProjectTypeRegistry().registerProjectType(new ProjectType("testModule", "my type", true, false) {
        });

        Project myProject = pm.getProject("my_ws", "my_project");
        myProject.updateConfig(new ProjectConfig("my proj", "testModule"));

        Assert.assertEquals(myProject.getModules().get().size(), 0);

        pm.addModule("my_ws", "my_project", "test", new ProjectConfig("descr", "testModule"), null, null);

        Assert.assertEquals(myProject.getModules().get().size(), 1);
        Assert.assertEquals(myProject.getModules().get().iterator().next(), "test");

    }


    @Test
    public void testModulePathShouldBeAdded() throws Exception {
        String modulePath = "newModule";
        pm.getProjectTypeRegistry().registerProjectType(new ProjectType("testModule", "my type", true, false) {
        });

        Project myProject = pm.getProject("my_ws", "my_project");
        myProject.updateConfig(new ProjectConfig("my proj", "testModule"));

        Assert.assertEquals(myProject.getModules().get().size(), 0);

        myProject.getModules().add(modulePath);

        Assert.assertTrue(myProject.getModules().get().contains(modulePath));
    }


    @Test
    public void testModulePathShouldBeRemoved() throws Exception {
        String modulePath = "newModule";
        pm.getProjectTypeRegistry().registerProjectType(new ProjectType("testModule", "my type", true, false) {
        });

        Project myProject = pm.getProject("my_ws", "my_project");
        myProject.updateConfig(new ProjectConfig("my proj", "testModule"));

        Assert.assertEquals(myProject.getModules().get().size(), 0);

        myProject.getModules().add(modulePath);

        Assert.assertTrue(myProject.getModules().get().contains(modulePath));

        myProject.getModules().remove(modulePath);
        Assert.assertFalse(myProject.getModules().get().contains(modulePath));
    }


    @Test
    public void testModuleShouldBeUpdated() throws Exception {
        String modulePath = "newModule";
        String innerModulePath1 = "newModule/innerModule1";
        String innerModulePath2 = "newModule/innerModule2";

        String newModulePath = "modulePathUpdated";

        pm.getProjectTypeRegistry().registerProjectType(new ProjectType("testModule", "my type", true, false) {
        });

        Project myProject = pm.getProject("my_ws", "my_project");
        myProject.updateConfig(new ProjectConfig("my proj", "testModule"));

        Assert.assertEquals(myProject.getModules().get().size(), 0);

        myProject.getModules().add(modulePath);
        myProject.getModules().add(innerModulePath1);
        myProject.getModules().add(innerModulePath2);

        Assert.assertTrue(myProject.getModules().get().contains(modulePath));
        Assert.assertTrue(myProject.getModules().get().contains(innerModulePath1));
        Assert.assertTrue(myProject.getModules().get().contains(innerModulePath2));

        myProject.getModules().update(modulePath, newModulePath);

        Assert.assertTrue(myProject.getModules().get().contains(newModulePath));
        Assert.assertTrue(myProject.getModules().get().contains("modulePathUpdated/innerModule1"));
        Assert.assertTrue(myProject.getModules().get().contains("modulePathUpdated/innerModule2"));

        Assert.assertEquals(myProject.getModules().get().size(), 3);
    }


    @Test
    public void testModuleShouldBeUpdated2() throws Exception {
        String modulePath = "newModule";
        String innerModulePath1 = "newModule/innerModule1";
        String innerModulePath2 = "newModule/innerModule2";

        String newInnerModulePath1 = "newModule/innerModule1_1";
        String newModulePath = "modulePathUpdated";

        pm.getProjectTypeRegistry().registerProjectType(new ProjectType("testModule", "my type", true, false) {
        });

        Project myProject = pm.getProject("my_ws", "my_project");
        myProject.updateConfig(new ProjectConfig("my proj", "testModule"));

        Assert.assertEquals(myProject.getModules().get().size(), 0);

        myProject.getModules().add(modulePath);
        myProject.getModules().add(innerModulePath1);
        myProject.getModules().add(innerModulePath2);

        Assert.assertTrue(myProject.getModules().get().contains(modulePath));
        Assert.assertTrue(myProject.getModules().get().contains(innerModulePath1));
        Assert.assertTrue(myProject.getModules().get().contains(innerModulePath2));

        myProject.getModules().update(innerModulePath1, newInnerModulePath1);
        myProject.getModules().update(modulePath, newModulePath);

        Assert.assertTrue(myProject.getModules().get().contains("modulePathUpdated/innerModule1_1"));
        Assert.assertTrue(myProject.getModules().get().contains("modulePathUpdated"));
        Assert.assertTrue(myProject.getModules().get().contains("modulePathUpdated/innerModule2"));

        Assert.assertEquals(myProject.getModules().get().size(), 3);
    }


    @Test
    public void testAddFolderAndProjectAsAModule() throws Exception {

        pm.getProjectTypeRegistry().registerProjectType(new ProjectType("testAddFolderAsAModule", "my type", true, false) {
        });

        Project parent = pm.getProject("my_ws", "my_project");
        parent.updateConfig(new ProjectConfig("my proj", "testAddFolderAsAModule"));

        Assert.assertEquals(parent.getModules().get().size(), 0);

        parent.getBaseFolder().createFolder("module");


        pm.addModule("my_ws", "my_project", "module", new ProjectConfig("my proj", "testAddFolderAsAModule"), null, null);

        Assert.assertEquals(parent.getModules().get().size(), 1);
        Assert.assertEquals(parent.getModules().get().iterator().next(), "module");


        Project module2 = pm.createProject("my_ws", "module2", new ProjectConfig("my proj", "testAddFolderAsAModule"), null, null);
        pm.addModule("my_ws", "my_project", "/module2", null, null, null);

        Assert.assertEquals(parent.getModules().get().size(), 2);

    }

    @Test
    public void testDtoConverterWithMixin() throws Exception {
        //final ProjectTypeRegistry registry = injector.getInstance(ProjectTypeRegistry.class);

        pm.getProjectTypeRegistry().registerProjectType(new ProjectType("testDtoConverterWithMixin", "my type", false, true) {
            {
                addVariableDefinition("var2", "var", true);
            }
        });

        final Map<String, List<String>> attributes = new HashMap<>();
        attributes.put("var2", Arrays.asList("var2Value"));

        final ProjectUpdate projectUpdate = DtoFactory.getInstance().createDto(ProjectUpdate.class)
                .withType("blank")
                .withMixinTypes(Arrays.asList("testDtoConverterWithMixin"))
                .withAttributes(attributes);

        final ProjectConfig projectConfig = DtoConverter.fromDto2(projectUpdate, pm.getProjectTypeRegistry());

        Assert.assertEquals(projectConfig.getTypeId(), "blank");
        Assert.assertEquals(projectConfig.getMixinTypes(), Arrays.asList("testDtoConverterWithMixin"));

        // here the attribute is null because the project type is not the owner of the attribute but we have
        // a mixin so it should works ?
        Assert.assertEquals(projectConfig.getAttributes().get("var2"), new AttributeValue("var2Value"));
    }


}

