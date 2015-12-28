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

import com.google.common.io.Resources;
import org.eclipse.che.api.project.server.type.ProjectTypeDef;
import org.eclipse.che.api.project.shared.dto.ProjectTemplateDescriptor;
import org.eclipse.che.api.workspace.shared.dto.SourceStorageDto;
import org.eclipse.che.commons.lang.NameGenerator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * @author Vitaly Parfonov
 */
public class ProjectTemplateDescriptionLoaderTest {

    ProjectTemplateRegistry templateRegistry;

    private Set<ProjectTypeDef> pts;

    private String embedTypeId = "embed_type";

    private String projectType1 = "type1";

    private String projectType2 = "type2";

    @Before
    public void setUp() {
        templateRegistry = mock(ProjectTemplateRegistry.class);
        pts = new HashSet<>();
        ProjectTypeDef embedType = mock(ProjectTypeDef.class);
        when(embedType.getId()).thenReturn(embedTypeId);
        ProjectTypeDef type1 = mock(ProjectTypeDef.class);
        when(type1.getId()).thenReturn(projectType1);
        ProjectTypeDef type2 = mock(ProjectTypeDef.class);
        when(type2.getId()).thenReturn(projectType2);
        pts.add(type1);
        pts.add(type2);
        pts.add(embedType);
    }

    @Test
    public void testWithOutConfig() {
        ProjectTemplateDescriptionLoader loader = new ProjectTemplateDescriptionLoader(null, null, pts, templateRegistry);
        loader.start();
        verify(templateRegistry).register(eq(embedTypeId), anyListOf(ProjectTemplateDescriptor.class));
        verify(templateRegistry, never()).register(eq(projectType1), anyListOf(ProjectTemplateDescriptor.class));
        verify(templateRegistry, never()).register(eq(projectType2), anyListOf(ProjectTemplateDescriptor.class));
    }

    @Test
    //load templates from given dir
    public void testWithConfig() {
        URL resource = Resources.getResource(getClass(), "pt-tmpl");
        Path path = Paths.get(URI.create(resource.toString()));
        Assert.assertNotNull(resource);
        ProjectTemplateDescriptionLoader loader = new ProjectTemplateDescriptionLoader(path.toString(), null, pts, templateRegistry);
        loader.start();
        verify(templateRegistry, never()).register(eq(embedTypeId), anyListOf(ProjectTemplateDescriptor.class));
        verify(templateRegistry).register(eq(projectType1), anyListOf(ProjectTemplateDescriptor.class));
        verify(templateRegistry).register(eq(projectType2), anyListOf(ProjectTemplateDescriptor.class));
    }

    @Test
    //load templates from given dir
    public void testWithConfig2() {
        ProjectTypeDef type3 = mock(ProjectTypeDef.class);
        when(type3.getId()).thenReturn("type3");
        pts.add(type3);
        URL resource = Resources.getResource(getClass(), "pt-tmpl");
        Path path = Paths.get(URI.create(resource.toString()));
        Assert.assertNotNull(resource);
        ProjectTemplateDescriptionLoader loader = new ProjectTemplateDescriptionLoader(path.toString(), null, pts, templateRegistry);
        loader.start();
        verify(templateRegistry).register(eq(projectType1), anyListOf(ProjectTemplateDescriptor.class));
        verify(templateRegistry).register(eq(projectType2), anyListOf(ProjectTemplateDescriptor.class));
        verify(templateRegistry, never()).register(eq("type3"), anyListOf(ProjectTemplateDescriptor.class));
    }

    @Test
    public void testWithConfigAndReplaceLocations() {
        ProjectTemplateRegistry templateRegistry = new ProjectTemplateRegistry();
        Set<ProjectTypeDef> pts = new HashSet<>();
        ProjectTypeDef type2 = mock(ProjectTypeDef.class);
        when(type2.getId()).thenReturn(projectType2);
        pts.add(type2);
        URL resource = Resources.getResource(getClass(), "pt-tmpl");
        Path path = Paths.get(URI.create(resource.toString()));
        Assert.assertNotNull(resource);
        String location = NameGenerator.generate("location", 5);
        ProjectTemplateDescriptionLoader loader = new ProjectTemplateDescriptionLoader(path.toString(), location, pts, templateRegistry);
        loader.start();
        List<ProjectTemplateDescriptor> type = templateRegistry.getTemplates(projectType2);
        Assert.assertNotNull(type);
        Assert.assertEquals(1, type.size());
        ProjectTemplateDescriptor templateDescriptor = type.get(0);
        Assert.assertNotNull(templateDescriptor);
        SourceStorageDto source = templateDescriptor.getSource();
        Assert.assertNotNull(source);
        Assert.assertNotNull(source.getLocation());
        Assert.assertTrue(source.getLocation().contains(location));
    }
}
