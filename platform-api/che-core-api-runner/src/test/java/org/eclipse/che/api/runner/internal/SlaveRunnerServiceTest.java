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
package org.eclipse.che.api.runner.internal;

import org.eclipse.che.api.core.rest.Service;
import org.eclipse.che.api.runner.dto.RunnerServerDescriptor;
import org.eclipse.che.dto.server.DtoFactory;

import org.everrest.core.impl.uri.UriBuilderImpl;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import javax.ws.rs.core.UriInfo;
import java.lang.reflect.Field;
import java.net.URI;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * @author andrew00x
 */
@Listeners(value = {MockitoTestNGListener.class})
public class SlaveRunnerServiceTest {

    private DtoFactory dtoFactory = DtoFactory.getInstance();

    @Mock
    private RunnerRegistry runners;

    @Mock
    private ResourceAllocators allocators;

    @Spy
    @InjectMocks
    private SlaveRunnerService service = new SlaveRunnerService();

    @BeforeMethod
    public void beforeMethod() throws Exception {
        Field field = Service.class.getDeclaredField("uriInfo");
        field.setAccessible(true);
        UriInfo uriInfo = mock(UriInfo.class);
        field.set(service, uriInfo);
        doReturn(new UriBuilderImpl().uri(URI.create("http://localhost:8080/service"))).when(uriInfo).getRequestUriBuilder();
    }

    @AfterMethod
    public void afterMethod() {
    }

    @Test
    public void testGetRunnerServerDescriptor() throws Exception {
        doReturn(dto(RunnerServerDescriptor.class).withAssignedWorkspace("my_ws").withAssignedProject("my_project"))
                .when(service).createServiceDescriptor();
        RunnerServerDescriptor descriptor = (RunnerServerDescriptor)service.getServiceDescriptor();
        Assert.assertEquals(descriptor.getHref(), "http://localhost:8080/service");
        Assert.assertEquals(descriptor.getAssignedWorkspace(), "my_ws");
        Assert.assertEquals(descriptor.getAssignedProject(), "my_project");
    }

    private <T> T dto(Class<T> type) {
        return dtoFactory.createDto(type);
    }
}
