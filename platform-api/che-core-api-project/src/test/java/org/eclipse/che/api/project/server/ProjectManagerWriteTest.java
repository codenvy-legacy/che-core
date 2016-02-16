/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
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
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.model.workspace.ProjectConfig;
import org.eclipse.che.api.project.server.type.AttributeValue;
import org.eclipse.che.api.project.server.type.BaseProjectType;
import org.eclipse.che.api.project.server.type.ProjectTypeConstraintException;
import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;
import org.eclipse.che.dto.server.DtoFactory;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author gazarenkov
 */
public class ProjectManagerWriteTest extends WsAgentTestBase {


    @Before
    public void setUp() throws Exception {

        super.setUp();

        projectTypeRegistry.registerProjectType(new PT2());
        projectTypeRegistry.registerProjectType(new PT3());
        projectTypeRegistry.registerProjectType(new PT4NoGen());
        projectTypeRegistry.registerProjectType(new M2());

        projectHandlerRegistry.register(new PT3.SrcGenerator());

    }


    @Test
    public void testCreateProject() throws Exception {


        Map<String, List<String>> attrs = new HashMap<>();
        List<String> v = new ArrayList<>();
        v.add("meV");
        attrs.put("var1", v);


        ProjectConfig config = DtoFactory.newDto(ProjectConfigDto.class)
                                         .withPath("createProject")
                                         .withName("create")
                                         .withType("primary1")
                                         .withDescription("description")
                                         .withAttributes(attrs);
        pm.createProject(config, new HashMap<>());


        RegisteredProject project = pm.getProject("/createProject");

        assertTrue(project.getBaseFolder().getVirtualFile().exists());
        assertEquals("/createProject", project.getPath());
        assertEquals(2, project.getAttributeEntries().size());
        assertEquals("meV", project.getAttributeEntries().get("var1").getString());

    }

    @Test
    public void testCreateProjectInvalidAttribute() throws Exception {

        ProjectConfig pc = new NewProjectConfig("/testCreateProjectInvalidAttributes", "pt2", null, "name", "descr", null, null);

        try {
            pm.createProject(pc, null);
            fail("ProjectTypeConstraintException should be thrown : pt-var2 attribute is mandatory");
        } catch (ProjectTypeConstraintException e) {
            //
        }

    }


    @Test
    public void testCreateProjectWithRequiredProvidedAttribute() throws Exception {

        // SPECS:
        // If project type has provided required attributes,
        // respective CreateProjectHandler MUST be provided

        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put("pt2-var2", new AttributeValue("test").getList());
        ProjectConfig pc =
                new NewProjectConfig("/testCreateProjectWithRequiredProvidedAttribute", "pt3", null, "name", "descr", attributes, null);

        pm.createProject(pc, null);

        RegisteredProject project = projectRegistry.getProject("testCreateProjectWithRequiredProvidedAttribute");
        assertEquals("pt3", project.getType());
        assertNotNull(project.getBaseFolder().getChild("file1"));
        assertEquals("pt2-provided1", project.getAttributes().get("pt2-provided1").get(0));

    }

    @Test
    public void testFailCreateProjectWithNoRequiredGenerator() throws Exception {

        // SPECS:
        // If there are no respective CreateProjectHandler ProjectTypeConstraintException will be thrown

        ProjectConfig pc = new NewProjectConfig("/testFailCreateProjectWithNoRequiredGenerator", "pt4", null, "name", "descr", null, null);

        try {
            pm.createProject(pc, null);
            fail("ProjectTypeConstraintException: Value for required attribute is not initialized pt4:pt4-provided1");
        } catch (ProjectTypeConstraintException e) {
        }


    }


    @Test
    public void testSamePathProjectCreateFailed() throws Exception {

        // SPECS:
        // If there is a project with the same path ConflictException will be thrown on create project

        ProjectConfig pc = new NewProjectConfig("/testSamePathProjectCreateFailed", "blank", null, "name", "descr", null, null);

        pm.createProject(pc, null);

        pc = new NewProjectConfig("/testSamePathProjectCreateFailed", "blank", null, "name", "descr", null, null);

        try {
            pm.createProject(pc, null);
            fail("ConflictException: Project config already exists /testSamePathProjectCreateFailed");
        } catch (ConflictException e) {
        }

        assertNotNull(projectRegistry.getProject("/testSamePathProjectCreateFailed"));

    }

    @Test
    public void testInvalidPTProjectCreateFailed() throws Exception {

        // SPECS:
        // If either primary or some mixin project type is not registered in PT registry
        // project creation failed with NotFoundException

        ProjectConfig pc = new NewProjectConfig("/testInvalidPTProjectCreateFailed", "invalid", null, "name", "descr", null, null);

        try {
            pm.createProject(pc, null);
            fail("NotFoundException: Project Type not found: invalid");
        } catch (NotFoundException e) {
        }

        assertNull(projectRegistry.getProject("/testInvalidPTProjectCreateFailed"));
        assertNull(projectRegistry.folder("/testInvalidPTProjectCreateFailed"));

        // check mixin as well
        List<String> ms = new ArrayList<>();
        ms.add("invalid");

        pc = new NewProjectConfig("/testInvalidPTProjectCreateFailed", "blank", ms, "name", "descr", null, null);

        try {
            pm.createProject(pc, null);
            fail("NotFoundException: Project Type not found: invalid");
        } catch (NotFoundException e) {
        }


    }

    @Test
    public void testConflictAttributesProjectCreateFailed() throws Exception {

        // SPECS:
        // If there are attributes with the same name in primary and mixin PT or between mixins
        // Project creation failed with ProjectTypeConstraintException


        List<String> ms = new ArrayList<>();
        ms.add("m2");

        ProjectConfig pc = new NewProjectConfig("/testConflictAttributesProjectCreateFailed", "pt2", ms, "name", "descr", null, null);
        try {
            pm.createProject(pc, null);
            fail("ProjectTypeConstraintException: Attribute name conflict. Duplicated attributes detected /testConflictAttributesProjectCreateFailed Attribute pt2-const1 declared in m2 already declared in pt2");
        } catch (ProjectTypeConstraintException e) {
        }

        assertNull(projectRegistry.folder("/testConflictAttributesProjectCreateFailed"));

    }

    @Test
    public void testInvalidConfigProjectCreateFailed() throws Exception {

        // SPECS:
        // If project path is not defined
        // Project creation failed with ConflictException

        ProjectConfig pc = new NewProjectConfig(null, "pt2", null, "name", "descr", null, null);

        try {
            pm.createProject(pc, null);
            fail("ConflictException: Path for new project should be defined ");
        } catch (ConflictException e) {
        }


    }


    @Test
    public void testCreateInnerProject() throws Exception {


        ProjectConfig pc = new NewProjectConfig("/testCreateInnerProject", BaseProjectType.ID, null, "name", "descr", null, null);
        pm.createProject(pc, null);

        pc = new NewProjectConfig("/testCreateInnerProject/inner", BaseProjectType.ID, null, "name", "descr", null, null);
        pm.createProject(pc, null);

        assertNotNull(projectRegistry.getProject("/testCreateInnerProject/inner"));
        assertEquals(2, projectRegistry.getProjects().size());
        assertEquals(1, projectRegistry.getProjects("/testCreateInnerProject").size());

        // If there are no parent folder it will be created

        pc = new NewProjectConfig("/nothing/inner", BaseProjectType.ID, null, "name", "descr", null, null);

        pm.createProject(pc, null);
        assertNotNull(projectRegistry.getProject("/nothing/inner"));
        assertNull(projectRegistry.getProject("/nothing"));
        assertNotNull(pm.getProjectsRoot().getChildFolder("/nothing"));

    }


    @Test
    public void testUpdateProjectWithPersistedAttributes() throws Exception {
        Map<String, List<String>> attributes = new HashMap<>();

        ProjectConfig pc = new NewProjectConfig("/testUpdateProject", BaseProjectType.ID, null, "name", "descr", null, null);
        RegisteredProject p = pm.createProject(pc, null);

        assertEquals(BaseProjectType.ID , p.getType());
        assertEquals("name", p.getName());

        attributes.put("pt2-var2", new AttributeValue("updated").getList());
        ProjectConfig pc1 = new NewProjectConfig("/testUpdateProject", "pt2", null, "updatedName", "descr", attributes, null);

        p = pm.updateProject(pc1);

        assertEquals("pt2", p.getType());
        assertEquals("updated", p.getAttributes().get("pt2-var2").get(0));
        assertEquals("updatedName", p.getName());


    }

    @Test
    public void testUpdateProjectWithProvidedAttributes() throws Exception {

        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put("pt2-var2", new AttributeValue("test").getList());

        ProjectConfig pc = new NewProjectConfig("/testUpdateProject", "pt2", null, "name", "descr", attributes, null);
        RegisteredProject p = pm.createProject(pc, null);

        // SPECS:
        // If project type is updated with one required provided attributes
        // those attributes should be provided before update

        pc = new NewProjectConfig("/testUpdateProject", "pt3", null, "updatedName", "descr", attributes, null);

        try {
            pm.updateProject(pc);
            fail("ProjectTypeConstraintException: Value for required attribute is not initialized pt3:pt2-provided1 ");
        } catch (ProjectTypeConstraintException e) {
        }


        p.getBaseFolder().createFolder("file1");
        p = pm.updateProject(pc);
        assertEquals(new AttributeValue("pt2-provided1"), p.getAttributeEntries().get("pt2-provided1"));


    }

    @Test
    public void testInvalidUpdateConfig() throws Exception {

        ProjectConfig pc = new NewProjectConfig(null, BaseProjectType.ID, null, "name", "descr", null, null);

        try {
            pm.updateProject(pc);
            fail("ConflictException: Project path is not defined");
        } catch (ConflictException e) {
        }

        pc = new NewProjectConfig("/nothing", BaseProjectType.ID, null, "name", "descr", null, null);
        try {
            pm.updateProject(pc);
            fail("NotFoundException: Project '/nothing' doesn't exist.");
        } catch (NotFoundException e) {
        }
    }


    @Test
    public void testDeleteProject() throws Exception {

        ProjectConfig pc = new NewProjectConfig("/testDeleteProject", BaseProjectType.ID, null, "name", "descr", null, null);
        pm.createProject(pc, null);
        pc = new NewProjectConfig("/testDeleteProject/inner", BaseProjectType.ID, null, "name", "descr", null, null);
        pm.createProject(pc, null);

        assertNotNull(projectRegistry.getProject("/testDeleteProject/inner"));

        pm.delete("/testDeleteProject");

        assertNull(projectRegistry.getProject("/testDeleteProject/inner"));
        assertNull(projectRegistry.getProject("/testDeleteProject"));
        assertNull(projectRegistry.folder("/testDeleteProject/inner"));

    }


    @Test
    public void testImportProject() throws Exception {

    }


    @Test
    public void testProvidedAttributesNotSerialized() throws Exception {

        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put("pt2-var2", new AttributeValue("test2").getList());
        attributes.put("pt2-var1", new AttributeValue("test1").getList());
        ProjectConfig pc = new NewProjectConfig("/testProvidedAttributesNotSerialized", "pt3", null, "name", "descr", attributes, null);

        pm.createProject(pc, null);

        workspaceHolder.updateProjects(projectRegistry.getProjects());


        // SPECS:
        // Only persisted variables should be persisted (no constants, no provided variables)

        for (ProjectConfig project : workspaceHolder.getWorkspace().getProjects()) {

            if (project.getPath().equals("/testProvidedAttributesNotSerialized")) {

                assertNotNull(project.getAttributes().get("pt2-var1"));
                assertNotNull(project.getAttributes().get("pt2-var2"));
                assertNull(project.getAttributes().get("pt2-const1"));
                assertNull(project.getAttributes().get("pt2-provided1"));
            }

        }


    }


}
