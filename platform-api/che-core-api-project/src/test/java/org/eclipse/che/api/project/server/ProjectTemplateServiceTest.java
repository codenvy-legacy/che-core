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

import org.eclipse.che.api.core.rest.ApiExceptionMapper;
import org.eclipse.che.api.core.rest.CodenvyJsonProvider;
import org.eclipse.che.api.project.shared.dto.ProjectTemplateDescriptor;
import org.eclipse.che.api.vfs.server.ContentStream;
import org.eclipse.che.api.vfs.server.ContentStreamWriter;
import org.everrest.core.ResourceBinder;
import org.everrest.core.impl.ApplicationContextImpl;
import org.everrest.core.impl.ApplicationProviderBinder;
import org.everrest.core.impl.ContainerResponse;
import org.everrest.core.impl.EverrestConfiguration;
import org.everrest.core.impl.EverrestProcessor;
import org.everrest.core.impl.ProviderBinder;
import org.everrest.core.impl.ResourceBinderImpl;
import org.everrest.core.tools.DependencySupplierImpl;
import org.everrest.core.tools.ResourceLauncher;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Application;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.mock;

/**
 * @author Vitaly Parfonov
 */
public class ProjectTemplateServiceTest {


    ProjectTemplateRegistry templateRegistry = new ProjectTemplateRegistry();

    private ResourceLauncher launcher;

    @BeforeTest
    public void setUp() {

        ProjectTemplateDescriptor template = mock(ProjectTemplateDescriptor.class);

        templateRegistry.register("test", template);

        DependencySupplierImpl dependencies = new DependencySupplierImpl();
        dependencies.addComponent(ProjectTemplateRegistry.class, templateRegistry);

        ResourceBinder resources = new ResourceBinderImpl();
        ProviderBinder providers = new ApplicationProviderBinder();
        EverrestProcessor processor = new EverrestProcessor(resources, providers, dependencies, new EverrestConfiguration(), null);
        launcher = new ResourceLauncher(processor);

        processor.addApplication(new Application() {
            @Override
            public Set<Class<?>> getClasses() {
                return java.util.Collections.<Class<?>>singleton(ProjectTemplateService.class);
            }

            @Override
            public Set<Object> getSingletons() {
                return new HashSet<>(Arrays.asList(new CodenvyJsonProvider(new HashSet<>(Arrays.asList(ContentStream.class))),
                                                   new ContentStreamWriter(),
                                                   new ApiExceptionMapper()));
            }
        });

        ApplicationContextImpl.setCurrent(new ApplicationContextImpl(null, null, ProviderBinder.getInstance()));
    }


    @Test
    public void getTemplates() throws Exception {
        ContainerResponse response =
                launcher.service(HttpMethod.GET, "http://localhost:8080/api/project-template/test", "http://localhost:8080/api", null, null, null);
        Assert.assertEquals(response.getStatus(), 200, "Error: " + response.getEntity());
        List<ProjectTemplateDescriptor> result = (List<ProjectTemplateDescriptor>)response.getEntity();

        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
        Assert.assertNotNull(result.get(0));
    }


}
