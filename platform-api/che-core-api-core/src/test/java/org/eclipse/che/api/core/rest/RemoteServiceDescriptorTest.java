/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.api.core.rest;

import com.jayway.restassured.response.Response;

import org.eclipse.che.api.core.rest.shared.ParameterType;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.core.rest.shared.dto.LinkParameter;
import org.eclipse.che.api.core.rest.shared.dto.ServiceDescriptor;
import org.everrest.assured.EverrestJetty;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import java.util.List;

import static com.jayway.restassured.RestAssured.when;
import static org.testng.Assert.assertEquals;

/**
 * Tests for {@link RemoteServiceDescriptor}
 *
 * @author Andrey Parfonov
 * @author Alexander Garagatyi
 */
@Listeners(value = {EverrestJetty.class, MockitoTestNGListener.class})
public class RemoteServiceDescriptorTest {
    final String BASE_URI    = "http://localhost/service";
    final String SERVICE_URI = BASE_URI + "/test";


    EchoService echoService;

//    public static class Deployer extends Application {
//        private final Set<Object>   singletons;
//        private final Set<Class<?>> classes;
//
//        public Deployer() {
//            classes = new HashSet<>(1);
//            classes.add(EchoService.class);
//            singletons = Collections.emptySet();
//        }
//
//        @Override
//        public Set<Class<?>> getClasses() {
//            return classes;
//        }
//
//        @Override
//        public Set<Object> getSingletons() {
//            return singletons;
//        }
//    }

//    ResourceLauncher launcher;

//    @BeforeTest
//    public void setUp() throws Exception {
//        DependencySupplierImpl dependencies = new DependencySupplierImpl();
//        ResourceBinder resources = new ResourceBinderImpl();
//        ProviderBinder providers = new ApplicationProviderBinder();
//        EverrestProcessor processor = new EverrestProcessor(resources,providers,dependencies,new EverrestConfiguration(), null);
//        launcher = new ResourceLauncher(processor);
//        processor.addApplication(new Deployer());
//        ApplicationContextImpl.setCurrent(new ApplicationContextImpl(null, null, ProviderBinder.getInstance()));
//        System.out.println("initilize");
//    }

    @Test
    public void testDescription(ITestContext ctx) throws Exception {
        assertEquals(getDescriptor(ctx).getDescription(), "test service");
    }

    @Test
    public void testServiceLocation(ITestContext ctx) throws Exception {
        assertEquals(getDescriptor(ctx).getHref(), SERVICE_URI);
    }

    @Test
    public void testLinkAvailable(ITestContext ctx) throws Exception {
        assertEquals(getDescriptor(ctx).getLinks().size(), 1);
    }

    @Test
    public void testLinkInfo(ITestContext ctx) throws Exception {
        Link link = getLink(ctx, "echo");
        assertEquals(link.getMethod(), HttpMethod.GET);
        assertEquals(link.getHref(), SERVICE_URI + "/my_method");
        assertEquals(link.getProduces(), MediaType.TEXT_PLAIN);
    }

    @Test
    public void testLinkParameters(ITestContext ctx) throws Exception {
        Link link = getLink(ctx, "echo");
        List<LinkParameter> parameters = link.getParameters();
        assertEquals(parameters.size(), 1);
        LinkParameter linkParameter = parameters.get(0);
        assertEquals(linkParameter.getDefaultValue(), "a");
        assertEquals(linkParameter.getDescription(), "some text");
        assertEquals(linkParameter.getName(), "text");
        assertEquals(linkParameter.getType(), ParameterType.String);
        Assert.assertTrue(linkParameter.isRequired());
        List<String> valid = linkParameter.getValid();
        assertEquals(valid.size(), 2);
        Assert.assertTrue(valid.contains("a"));
        Assert.assertTrue(valid.contains("b"));
    }

    @Test
    public void should() throws Exception {
        final Response response = when().options("/test");

        assertEquals(response.getBody().prettyPrint(), "s");
        assertEquals(response.getStatusCode(), 200);
    }

    private Link getLink(ITestContext ctx, String rel) throws Exception {
        List<Link> links = getDescriptor(ctx).getLinks();
        for (Link link : links) {
            if (link.getRel().equals(rel)) {
                return link;
            }
        }
        return null;
    }

    private ServiceDescriptor getDescriptor(ITestContext ctx) throws Exception {
        RemoteServiceDescriptor remoteServiceDescriptor = new RemoteServiceDescriptor(getUrl(ctx) + "/test");
        return remoteServiceDescriptor.getServiceDescriptor();
//        String path = SERVICE_URI;
//        ContainerResponse response = launcher.service(HttpMethod.OPTIONS, path, BASE_URI, null, null, null, null);
//        Assert.assertEquals(response.getStatus(), 200);
//        return (ServiceDescriptor)response.getEntity();
    }

    private String getUrl(ITestContext ctx) {
        return "http://localhost:" + ctx.getAttribute(EverrestJetty.JETTY_PORT) + "/rest";
    }
}
