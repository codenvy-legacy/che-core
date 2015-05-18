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
package org.eclipse.che.api.factory;

import com.google.common.collect.ImmutableMap;
import com.jayway.restassured.response.Response;

import org.eclipse.che.api.account.server.dao.AccountDao;
import org.eclipse.che.api.account.server.dao.Member;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.rest.ApiExceptionMapper;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.core.rest.shared.dto.ServiceError;
import org.eclipse.che.api.factory.dto.Author;
import org.eclipse.che.api.factory.dto.Button;
import org.eclipse.che.api.factory.dto.ButtonAttributes;
import org.eclipse.che.api.factory.dto.Factory;
import org.eclipse.che.api.project.server.ProjectManager;
import org.eclipse.che.api.project.shared.dto.ImportSourceDescriptor;
import org.eclipse.che.api.project.shared.dto.NewProject;
import org.eclipse.che.api.project.shared.dto.Source;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.json.JsonHelper;
import org.eclipse.che.commons.lang.Pair;
import org.eclipse.che.commons.user.UserImpl;
import org.eclipse.che.dto.server.DtoFactory;
import org.everrest.assured.EverrestJetty;
import org.everrest.assured.JettyHttpServer;
import org.everrest.core.Filter;
import org.everrest.core.GenericContainerRequest;
import org.everrest.core.RequestFilter;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.ITestContext;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import javax.ws.rs.core.MediaType;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.jayway.restassured.RestAssured.given;
import static java.lang.String.format;
import static java.net.URLEncoder.encode;
import static javax.ws.rs.core.Response.Status;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

@Listeners(value = {EverrestJetty.class, MockitoTestNGListener.class})
public class FactoryServiceTest {
    private final String             CORRECT_FACTORY_ID = "correctFactoryId";
    private final String             ILLEGAL_FACTORY_ID = "illegalFactoryId";
    private final String             SERVICE_PATH       = "/factory";
    private final ApiExceptionMapper exceptionMapper    = new ApiExceptionMapper();

    private EnvironmentFilter filter = new EnvironmentFilter();

    @Mock
    private FactoryStore factoryStore;

    @Mock
    private AccountDao accountDao;

    @Mock
    private FactoryCreateValidator createValidator;

    @Mock
    private FactoryAcceptValidator acceptValidator;

    @Mock
    private FactoryEditValidator editValidator;

    @Mock
    private ProjectManager projectManager;

    private FactoryBuilder factoryBuilder;

    private FactoryService factoryService;
    private DtoFactory     dto;

    @BeforeMethod
    public void setUp() throws Exception {
        dto = DtoFactory.getInstance();
        factoryBuilder = spy(new FactoryBuilder(new SourceProjectParametersValidator()));
        factoryService = new FactoryService("https://codenvy.com/api",
                                            factoryStore,
                                            accountDao,
                                            createValidator,
                                            acceptValidator,
                                            editValidator,
                                            new LinksHelper(),
                                            factoryBuilder,
                                            projectManager);

        when(accountDao.getByMember(anyString())).thenReturn(Arrays.asList(new Member().withRoles(Arrays.asList("account/owner"))));
    }

    @Filter
    public static class EnvironmentFilter implements RequestFilter {

        public void doFilter(GenericContainerRequest request) {
            EnvironmentContext context = EnvironmentContext.getCurrent();
            context.setUser(new UserImpl(JettyHttpServer.ADMIN_USER_NAME, "id-2314", "token-2323",
                                         Collections.<String>emptyList(), false));
        }

    }


    @Test
    public void shouldReturnSavedFactoryIfUserDidNotUseSpecialMethod() throws Exception {
        // given

        Factory factory = dto.createDto(Factory.class)
                             .withV("2.0")
                             .withSource(dto.createDto(Source.class)
                                            .withProject(dto.createDto(ImportSourceDescriptor.class)
                                                            .withType("git")
                                                            .withLocation(
                                                                    "http://github.com/codenvy/platform-api.git")
                                                            .withParameters(ImmutableMap.of("keepVcs", "true"))))
                             .withProject(dto.createDto(NewProject.class)
                                             .withType("ptype")
                                             .withName("pname"));

        factory.setId(CORRECT_FACTORY_ID);
        Factory expected = dto.clone(factory);

        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).thenReturn(factory);
        when(factoryStore.getFactoryImages(CORRECT_FACTORY_ID, null)).thenReturn(Collections.<FactoryImage>emptySet());

        // when
        Response response = given().when().get(SERVICE_PATH + "/" + CORRECT_FACTORY_ID);

        // then
        assertEquals(response.getStatusCode(), 200);
        Factory responseFactoryUrl = dto.createDtoFromJson(response.getBody().asInputStream(), Factory.class);
        responseFactoryUrl.setLinks(Collections.<Link>emptyList());
        assertEquals(responseFactoryUrl, expected);
    }


    @Test
    public void shouldBeAbleToSaveFactory() throws Exception {
        // given
        Factory factory = dto.createDto(Factory.class)
                             .withV("2.0")
                             .withSource(dto.createDto(Source.class)
                                            .withProject(dto.createDto(ImportSourceDescriptor.class)
                                                            .withType("git")
                                                            .withLocation(
                                                                    "http://github.com/codenvy/platform-api.git")));


        Path path = Paths.get(Thread.currentThread().getContextClassLoader()
                                    .getResource("100x100_image.jpeg")
                                    .toURI());


        FactorySaveAnswer factorySaveAnswer = new FactorySaveAnswer();
        when(factoryStore.saveFactory((Factory)any(), anySetOf(FactoryImage.class))).then(factorySaveAnswer);
        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).then(factorySaveAnswer);


        // when, then
        Response response =
                given().auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).//
                        multiPart("factoryUrl", JsonHelper.toJson(factory), MediaType.APPLICATION_JSON).//
                        multiPart("image", path.toFile(), "image/jpeg").//
                        when().//
                        post("/private" + SERVICE_PATH);

        assertEquals(response.getStatusCode(), 200);
        Factory responseFactoryUrl = dto.createDtoFromJson(response.getBody().asInputStream(), Factory.class);
        boolean found = false;
        for (Link link : responseFactoryUrl.getLinks()) {
            if (link.getRel().equals("image") && link.getProduces().equals("image/jpeg") && !link.getHref().isEmpty())
                found = true;
        }
        assertTrue(found);
    }

    @Test
    public void shouldReturnStatus409IfSaveRequestHaveNotFactoryInfo() throws Exception {
        // given

        // when, then
        Response response = given().auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).//
                multiPart("someOtherData", "Some content", MediaType.TEXT_PLAIN).//
                expect().//
                statusCode(Status.CONFLICT.getStatusCode()).//
                when().//
                post("/private" + SERVICE_PATH);

        assertEquals(dto.createDtoFromJson(response.getBody().asInputStream(), ServiceError.class).getMessage(),
                     "No factory URL information found in 'factoryUrl' section of multipart/form-data.");
    }

    @Test
    public void shouldBeAbleToSaveFactoryWithOutImage(ITestContext context) throws Exception {
        // given


        Factory factory = dto.createDto(Factory.class)
                             .withV("2.0")
                             .withSource(dto.createDto(Source.class)
                                            .withProject(dto.createDto(ImportSourceDescriptor.class)
                                                            .withType("git")
                                                            .withLocation(
                                                                    "http://github.com/codenvy/platform-api.git")
                                                            .withParameters(ImmutableMap.of("commitId", "12345679"))));


        Link expectedCreateProject =
                dto.createDto(Link.class).withMethod("GET").withProduces("text/html").withRel("create-project")
                   .withHref(getServerUrl(context) + "/f?id=" + CORRECT_FACTORY_ID);

        FactorySaveAnswer factorySaveAnswer = new FactorySaveAnswer();
        when(factoryStore.saveFactory((Factory)any(), anySetOf(FactoryImage.class))).then(factorySaveAnswer);
        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).then(factorySaveAnswer);

        // when, then
        Response response =
                given().auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD)//
                        .multiPart("factoryUrl", JsonHelper.toJson(factory), MediaType.APPLICATION_JSON).when()
                        .post("/private" + SERVICE_PATH);

        // then
        assertEquals(response.getStatusCode(), 200);
        Factory responseFactoryUrl = dto.createDtoFromJson(response.getBody().asString(), Factory.class);
        assertTrue(responseFactoryUrl.getLinks().contains(
                dto.createDto(Link.class).withMethod("GET").withProduces("application/json")
                   .withHref(getServerUrl(context) + "/rest/private/factory/" +
                             CORRECT_FACTORY_ID).withRel("self")
                                                         ));
        assertTrue(responseFactoryUrl.getLinks().contains(expectedCreateProject));
        assertTrue(responseFactoryUrl.getLinks()
                                     .contains(dto.createDto(Link.class).withMethod("GET").withProduces("text/plain")
                                                  .withHref(getServerUrl(context) +
                                                            "/rest/private/analytics/public-metric/factory_used?factory=" +
                                                            encode(expectedCreateProject.getHref(), "UTF-8"))
                                                  .withRel("accepted")));
        assertTrue(responseFactoryUrl.getLinks()
                                     .contains(dto.createDto(Link.class).withMethod("GET").withProduces("text/plain")
                                                  .withHref(getServerUrl(context) + "/rest/private/factory/" +
                                                            CORRECT_FACTORY_ID + "/snippet?type=url")
                                                  .withRel("snippet/url")));
        assertTrue(responseFactoryUrl.getLinks()
                                     .contains(dto.createDto(Link.class).withMethod("GET").withProduces("text/plain")
                                                  .withHref(getServerUrl(context) + "/rest/private/factory/" +
                                                            CORRECT_FACTORY_ID + "/snippet?type=html")
                                                  .withRel("snippet/html")));
        assertTrue(responseFactoryUrl.getLinks()
                                     .contains(dto.createDto(Link.class).withMethod("GET").withProduces("text/plain")
                                                  .withHref(getServerUrl(context) + "/rest/private/factory/" +
                                                            CORRECT_FACTORY_ID + "/snippet?type=markdown")
                                                  .withRel("snippet/markdown")));


        List<Link> expectedLinks = new ArrayList<>(8);
        expectedLinks.add(expectedCreateProject);

        Link self = dto.createDto(Link.class);
        self.setMethod("GET");
        self.setProduces("application/json");
        self.setHref(getServerUrl(context) + "/rest/private/factory/" + CORRECT_FACTORY_ID);
        self.setRel("self");
        expectedLinks.add(self);

        Link accepted = dto.createDto(Link.class);
        accepted.setMethod("GET");
        accepted.setProduces("text/plain");
        accepted.setHref(getServerUrl(context) + "/rest/private/analytics/public-metric/factory_used?factory=" +
                         encode(expectedCreateProject.getHref(), "UTF-8"));
        accepted.setRel("accepted");
        expectedLinks.add(accepted);

        Link snippetUrl = dto.createDto(Link.class);
        snippetUrl.setProduces("text/plain");
        snippetUrl.setHref(getServerUrl(context) + "/rest/private/factory/" + CORRECT_FACTORY_ID + "/snippet?type=url");
        snippetUrl.setRel("snippet/url");
        snippetUrl.setMethod("GET");
        expectedLinks.add(snippetUrl);

        Link snippetHtml = dto.createDto(Link.class);
        snippetHtml.setProduces("text/plain");
        snippetHtml.setHref(getServerUrl(context) + "/rest/private/factory/" + CORRECT_FACTORY_ID +
                            "/snippet?type=html");
        snippetHtml.setMethod("GET");
        snippetHtml.setRel("snippet/html");
        expectedLinks.add(snippetHtml);

        Link snippetMarkdown = dto.createDto(Link.class);
        snippetMarkdown.setProduces("text/plain");
        snippetMarkdown.setHref(getServerUrl(context) + "/rest/private/factory/" + CORRECT_FACTORY_ID +
                                "/snippet?type=markdown");
        snippetMarkdown.setRel("snippet/markdown");
        snippetMarkdown.setMethod("GET");
        expectedLinks.add(snippetMarkdown);

        Link snippetiFrame = dto.createDto(Link.class);
        snippetiFrame.setProduces("text/plain");
        snippetiFrame.setHref(getServerUrl(context) + "/rest/private/factory/" + CORRECT_FACTORY_ID +
                              "/snippet?type=iframe");
        snippetiFrame.setRel("snippet/iframe");
        snippetiFrame.setMethod("GET");
        expectedLinks.add(snippetiFrame);

        for (Link link : responseFactoryUrl.getLinks()) {
            //This transposition need because proxy objects doesn't contains equals method.
            Link testLink = dto.createDto(Link.class);
            testLink.setProduces(link.getProduces());
            testLink.setHref(link.getHref());
            testLink.setRel(link.getRel());
            testLink.setMethod("GET");
            assertTrue(expectedLinks.contains(testLink));
        }

        verify(factoryStore).saveFactory(Matchers.<Factory>any(), eq(Collections.<FactoryImage>emptySet()));
    }

    @Test
    public void shouldBeAbleToSaveFactoryWithOutImageWithOrgId() throws Exception {
        // given
        Factory factory = dto.createDto(Factory.class)
                             .withV("2.0")
                             .withSource(dto.createDto(Source.class)
                                            .withProject(dto.createDto(ImportSourceDescriptor.class)
                                                            .withType("git")
                                                            .withLocation(
                                                                    "http://github.com/codenvy/platform-api.git")
                                                            .withParameters(ImmutableMap.of("commitId", "12345679"))));


        FactorySaveAnswer factorySaveAnswer = new FactorySaveAnswer();
        when(factoryStore.saveFactory((Factory)any(), anySetOf(FactoryImage.class))).then(factorySaveAnswer);
        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).then(factorySaveAnswer);

        // when, then
        Response response =
                given().auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD)//
                        .multiPart("factoryUrl", JsonHelper.toJson(factory), MediaType.APPLICATION_JSON).when()
                        .post("/private" + SERVICE_PATH);

        // then
        assertEquals(response.getStatusCode(), 200);

    }


    @Test
    public void shouldBeAbleToSaveFactoryWithSetImageFieldButWithOutImageContent() throws Exception {
        // given
        Factory factory = dto.createDto(Factory.class)
                             .withV("2.0")
                             .withSource(dto.createDto(Source.class)
                                            .withProject(dto.createDto(ImportSourceDescriptor.class)
                                                            .withType("git")
                                                            .withLocation(
                                                                    "http://github.com/codenvy/platform-api.git")
                                                            .withParameters(ImmutableMap.of("commitId", "12345679"))));

        FactorySaveAnswer factorySaveAnswer = new FactorySaveAnswer();
        when(factoryStore.saveFactory((Factory)any(), anySetOf(FactoryImage.class))).then(factorySaveAnswer);
        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).then(factorySaveAnswer);

        // when, then
        given().auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD)//
                .multiPart("factoryUrl", dto.toJson(factory), MediaType.APPLICATION_JSON)//
                .multiPart("image", File.createTempFile("123456", ".jpeg"), "image/jpeg")//
                .expect().statusCode(200)
                .when().post("/private" + SERVICE_PATH);

        verify(factoryStore).saveFactory(Matchers.<Factory>any(), eq(Collections.<FactoryImage>emptySet()));
    }

    @Test
    public void shouldReturnStatus409OnSaveFactoryIfImageHasUnsupportedMediaType() throws Exception {
        // given
        Factory factory = dto.createDto(Factory.class)
                             .withV("2.0")
                             .withSource(dto.createDto(Source.class)
                                            .withProject(dto.createDto(ImportSourceDescriptor.class)
                                                            .withType("git")
                                                            .withLocation(
                                                                    "http://github.com/codenvy/platform-api.git")
                                                            .withParameters(ImmutableMap.of("commitId", "12345679"))));

        Path path = Paths.get(Thread.currentThread().getContextClassLoader().getResource("100x100_image.jpeg").toURI());

        when(factoryStore.saveFactory((Factory)any(), anySetOf(FactoryImage.class))).thenReturn(CORRECT_FACTORY_ID);
        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).thenReturn(factory);

        // when, then
        Response response = given().auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD)//
                .multiPart("factoryUrl", JsonHelper.toJson(factory), MediaType.APPLICATION_JSON)//
                .multiPart("image", path.toFile(), "image/tiff")//
                .expect()
                .statusCode(409)
                .when().post("/private" + SERVICE_PATH);

        assertEquals(dto.createDtoFromJson(response.getBody().asString(), ServiceError.class).getMessage(),
                     "Image media type 'image/tiff' is unsupported.");
    }

    @Test
    public void shouldBeAbleToGetFactory(ITestContext context) throws Exception {
        // given
        Factory factoryUrl = dto.createDto(Factory.class);
        factoryUrl.setId(CORRECT_FACTORY_ID);
        Path path = Paths.get(Thread.currentThread().getContextClassLoader().getResource("100x100_image.jpeg").toURI());
        byte[] data = Files.readAllBytes(path);
        FactoryImage image1 = new FactoryImage(data, "image/jpeg", "image123456789");
        FactoryImage image2 = new FactoryImage(data, "image/png", "image987654321");
        Set<FactoryImage> images = new HashSet<>();
        images.add(image1);
        images.add(image2);
        Link expectedCreateProject = dto.createDto(Link.class);
        expectedCreateProject.setProduces("text/html");
        expectedCreateProject.setHref(getServerUrl(context) + "/f?id=" + CORRECT_FACTORY_ID);
        expectedCreateProject.setRel("create-project");

        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).thenReturn(factoryUrl);
        when(factoryStore.getFactoryImages(CORRECT_FACTORY_ID, null)).thenReturn(images);

        // when
        Response response = given().when().get(SERVICE_PATH + "/" + CORRECT_FACTORY_ID);

        // then
        assertEquals(response.getStatusCode(), 200);
        Factory responseFactoryUrl = JsonHelper.fromJson(response.getBody().asString(),
                                                         Factory.class, null);

        List<Link> expectedLinks = new ArrayList<>(9);
        expectedLinks.add(expectedCreateProject);

        Link self = dto.createDto(Link.class);
        self.setProduces("application/json");
        self.setHref(getServerUrl(context) + "/rest/factory/" + CORRECT_FACTORY_ID);
        self.setRel("self");
        expectedLinks.add(self);

        Link imageJpeg = dto.createDto(Link.class);
        imageJpeg.setProduces("image/jpeg");
        imageJpeg.setHref(getServerUrl(context) + "/rest/factory/" + CORRECT_FACTORY_ID +
                          "/image?imgId=image123456789");
        imageJpeg.setRel("image");
        expectedLinks.add(imageJpeg);

        Link imagePng = dto.createDto(Link.class);
        imagePng.setProduces("image/png");
        imagePng.setHref(getServerUrl(context) + "/rest/factory/" + CORRECT_FACTORY_ID + "/image?imgId=image987654321");
        imagePng.setRel("image");
        expectedLinks.add(imagePng);

        Link accepted = dto.createDto(Link.class);
        accepted.setProduces("text/plain");
        accepted.setHref(getServerUrl(context) + "/rest/analytics/public-metric/factory_used?factory=" +
                         encode(expectedCreateProject.getHref(), "UTF-8"));
        accepted.setRel("accepted");
        expectedLinks.add(accepted);

        Link snippetUrl = dto.createDto(Link.class);
        snippetUrl.setProduces("text/plain");
        snippetUrl.setHref(getServerUrl(context) + "/rest/factory/" + CORRECT_FACTORY_ID + "/snippet?type=url");
        snippetUrl.setRel("snippet/url");
        expectedLinks.add(snippetUrl);

        Link snippetHtml = dto.createDto(Link.class);
        snippetHtml.setProduces("text/plain");
        snippetHtml.setHref(getServerUrl(context) + "/rest/factory/" + CORRECT_FACTORY_ID + "/snippet?type=html");
        snippetHtml.setRel("snippet/html");
        expectedLinks.add(snippetHtml);

        Link snippetMarkdown = dto.createDto(Link.class);
        snippetMarkdown.setProduces("text/plain");
        snippetMarkdown.setHref(getServerUrl(context) + "/rest/factory/" + CORRECT_FACTORY_ID +
                                "/snippet?type=markdown");
        snippetMarkdown.setRel("snippet/markdown");
        expectedLinks.add(snippetMarkdown);

        Link snippetiFrame = dto.createDto(Link.class);
        snippetiFrame.setProduces("text/plain");
        snippetiFrame.setHref(getServerUrl(context) + "/rest/factory/" + CORRECT_FACTORY_ID +
                              "/snippet?type=iframe");
        snippetiFrame.setRel("snippet/iframe");
        expectedLinks.add(snippetiFrame);

        for (Link link : responseFactoryUrl.getLinks()) {
            Link testLink = dto.createDto(Link.class);
            testLink.setProduces(link.getProduces());
            testLink.setHref(link.getHref());
            testLink.setRel(link.getRel());
            //This transposition need because proxy objects doesn't contains equals method.
            assertTrue(expectedLinks.contains(testLink));
        }
    }

    @Test
    public void shouldReturnStatus404OnGetFactoryWithIllegalId() throws Exception {
        // given
        when(factoryStore.getFactory(ILLEGAL_FACTORY_ID)).thenReturn(null);

        // when, then
        Response response = given().//
                expect().//
                statusCode(404).//
                when().//
                get(SERVICE_PATH + "/" + ILLEGAL_FACTORY_ID);

        assertEquals(dto.createDtoFromJson(response.getBody().asString(), ServiceError.class).getMessage(),
                     format("Factory URL with id %s is not found.", ILLEGAL_FACTORY_ID));
    }

    @Test
    public void shouldBeAbleToGetFactoryImage() throws Exception {
        // given
        Path path = Paths.get(Thread.currentThread().getContextClassLoader().getResource("100x100_image.jpeg").toURI());
        byte[] imageContent = Files.readAllBytes(path);
        FactoryImage image = new FactoryImage(imageContent, "image/jpeg", "imageName");

        when(factoryStore.getFactoryImages(CORRECT_FACTORY_ID, null)).thenReturn(new HashSet<>(Arrays.asList(image)));

        // when
        Response response = given().when().get(SERVICE_PATH + "/" + CORRECT_FACTORY_ID + "/image?imgId=imageName");

        // then
        assertEquals(response.getStatusCode(), 200);
        assertEquals(response.getContentType(), "image/jpeg");
        assertEquals(response.getHeader("content-length"), String.valueOf(imageContent.length));
        assertEquals(response.asByteArray(), imageContent);
    }

    @Test
    public void shouldBeAbleToGetFactoryDefaultImage() throws Exception {
        // given
        Path path = Paths.get(Thread.currentThread().getContextClassLoader().getResource("100x100_image.jpeg").toURI());
        byte[] imageContent = Files.readAllBytes(path);
        FactoryImage image = new FactoryImage(imageContent, "image/jpeg", "imageName");

        when(factoryStore.getFactoryImages(CORRECT_FACTORY_ID, null)).thenReturn(new HashSet<>(Arrays.asList(image)));

        // when
        Response response = given().when().get(SERVICE_PATH + "/" + CORRECT_FACTORY_ID + "/image");

        // then
        assertEquals(response.getStatusCode(), 200);
        assertEquals(response.getContentType(), "image/jpeg");
        assertEquals(response.getHeader("content-length"), String.valueOf(imageContent.length));
        assertEquals(response.asByteArray(), imageContent);
    }

    @Test
    public void shouldReturnStatus404OnGetFactoryImageWithIllegalId() throws Exception {
        // given
        when(factoryStore.getFactoryImages(CORRECT_FACTORY_ID, null)).thenReturn(new HashSet<FactoryImage>());

        // when, then
        Response response = given().//
                expect().//
                statusCode(404).//
                when().//
                get(SERVICE_PATH + "/" + CORRECT_FACTORY_ID + "/image?imgId=illegalImageId");

        assertEquals(dto.createDtoFromJson(response.getBody().asString(), ServiceError.class).getMessage(),
                     format("Image with id %s is not found.", "illegalImageId"));
    }

    @Test
    public void shouldResponse404OnGetImageIfFactoryDoesNotExist() throws Exception {
        // given
        when(factoryStore.getFactoryImages(ILLEGAL_FACTORY_ID, null)).thenReturn(null);

        // when, then
        Response response = given().//
                expect().//
                statusCode(404).//
                when().//
                get(SERVICE_PATH + "/" + ILLEGAL_FACTORY_ID + "/image?imgId=ImageId");

        assertEquals(dto.createDtoFromJson(response.getBody().asString(), ServiceError.class).getMessage(),
                     format("Factory URL with id %s is not found.", ILLEGAL_FACTORY_ID));
    }

    @Test
    public void shouldBeAbleToReturnUrlSnippet(ITestContext context) throws Exception {
        // given
        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).thenReturn(dto.createDto
                (Factory.class));

        // when, then
        given().//
                expect().//
                statusCode(200).//
                contentType(MediaType.TEXT_PLAIN).//
                body(equalTo(getServerUrl(context) + "/factory?id=" + CORRECT_FACTORY_ID)).//
                when().//
                get(SERVICE_PATH + "/" + CORRECT_FACTORY_ID + "/snippet?type=url");
    }

    @Test
    public void shouldBeAbleToReturnUrlSnippetIfTypeIsNotSet(ITestContext context) throws Exception {
        // given
        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).thenReturn(dto.createDto
                (Factory.class));

        // when, then
        given().//
                expect().//
                statusCode(200).//
                contentType(MediaType.TEXT_PLAIN).//
                body(equalTo(getServerUrl(context) + "/factory?id=" + CORRECT_FACTORY_ID)).//
                when().//
                get(SERVICE_PATH + "/" + CORRECT_FACTORY_ID + "/snippet");
    }

    @Test
    public void shouldBeAbleToReturnHtmlSnippet(ITestContext context) throws Exception {
        // given
        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).thenReturn(dto.createDto(Factory.class));

        // when, then
        Response response = given().//
                expect().//
                statusCode(200).//
                contentType(MediaType.TEXT_PLAIN).//
                when().//
                get(SERVICE_PATH + "/" + CORRECT_FACTORY_ID + "/snippet?type=html");

        assertEquals(response.body().asString(), "<script type=\"text/javascript\" src=\"" + getServerUrl(context) +
                                                 "/factory/resources/factory.js?" + CORRECT_FACTORY_ID + "\"></script>");
    }

    @Test
    public void shouldBeAbleToReturnMarkdownSnippetForFactory1WithImage(ITestContext context) throws Exception {
        // given
        Factory factory = (Factory)dto.createDto(Factory.class)
                                      .withV("2.0")
                                      .withSource(dto.createDto(Source.class)
                                                     .withProject(dto.createDto(ImportSourceDescriptor.class)
                                                                     .withType("git")
                                                                     .withLocation(
                                                                             "http://github.com/codenvy/platform-api.git")
                                                                     .withParameters(ImmutableMap.of("commitId", "12345679"))))

                                      .withId(CORRECT_FACTORY_ID)
                                      .withButton(dto.createDto(Button.class).withType(Button.ButtonType.logo));
        String imageName = "1241234";
        FactoryImage image = new FactoryImage();
        image.setName(imageName);

        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).thenReturn(factory);
        when(factoryStore.getFactoryImages(CORRECT_FACTORY_ID, null)).thenReturn(new HashSet<>(Arrays.asList(image)));
        // when, then
        given().//
                expect().//
                statusCode(200).//
                contentType(MediaType.TEXT_PLAIN).//
                body(
                equalTo("[![alt](" + getServerUrl(context) + "/api/factory/" + CORRECT_FACTORY_ID + "/image?imgId=" +
                        imageName + ")](" +
                        getServerUrl(context) + "/factory?id=" +
                        CORRECT_FACTORY_ID + ")")
                    ).//
                when().//
                get(SERVICE_PATH + "/" + CORRECT_FACTORY_ID + "/snippet?type=markdown");
    }


    @Test
    public void shouldBeAbleToReturnMarkdownSnippetForFactory2WithImage(ITestContext context) throws Exception {
        // given
        String imageName = "1241234";
        Factory furl = dto.createDto(Factory.class);
        furl.setId(CORRECT_FACTORY_ID);
        furl.setV("2.0");
        furl.setButton(dto.createDto(Button.class).withType(Button.ButtonType.logo));

        FactoryImage image = new FactoryImage();
        image.setName(imageName);

        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).thenReturn(furl);
        when(factoryStore.getFactoryImages(CORRECT_FACTORY_ID, null)).thenReturn(new HashSet<>(Arrays.asList(image)));
        // when, then
        given().//
                expect().//
                statusCode(200).//
                contentType(MediaType.TEXT_PLAIN).//
                body(
                equalTo("[![alt](" + getServerUrl(context) + "/api/factory/" + CORRECT_FACTORY_ID + "/image?imgId=" +
                        imageName + ")](" +
                        getServerUrl(context) + "/factory?id=" +
                        CORRECT_FACTORY_ID + ")")
                    ).//
                when().//
                get(SERVICE_PATH + "/" + CORRECT_FACTORY_ID + "/snippet?type=markdown");
    }

    @Test
    public void shouldBeAbleToReturnMarkdownSnippetForFactory1WithoutImage(ITestContext context) throws Exception {
        // given
        Factory factory = (Factory)dto.createDto(Factory.class)
                                      .withV("2.0")
                                      .withSource(dto.createDto(Source.class)
                                                     .withProject(dto.createDto(ImportSourceDescriptor.class)
                                                                     .withType("git")
                                                                     .withLocation(
                                                                             "http://github.com/codenvy/platform-api.git")
                                                                     .withParameters(ImmutableMap.of("commitId", "12345679"))))

                                      .withId(CORRECT_FACTORY_ID)
                                      .withButton(dto.createDto(Button.class).withType(Button.ButtonType.nologo)
                                                     .withAttributes(dto.createDto(ButtonAttributes.class).withColor("white")));

        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).thenReturn(factory);
        // when, then
        given().//
                expect().//
                statusCode(200).//
                contentType(MediaType.TEXT_PLAIN).//
                body(
                equalTo("[![alt](" + getServerUrl(context) + "/factory/resources/factory-white.png)](" + getServerUrl
                        (context) +
                        "/factory?id=" +
                        CORRECT_FACTORY_ID + ")")
                    ).//
                when().//
                get(SERVICE_PATH + "/" + CORRECT_FACTORY_ID + "/snippet?type=markdown");
    }


    @Test
    public void shouldNotBeAbleToGetMarkdownSnippetForFactory1WithoutStyle() throws Exception {
        // given

        Factory factory = (Factory)dto.createDto(Factory.class)
                                      .withV("2.0")
                                      .withSource(dto.createDto(Source.class)
                                                     .withProject(dto.createDto(ImportSourceDescriptor.class)
                                                                     .withType("git")
                                                                     .withLocation(
                                                                             "http://github.com/codenvy/platform-api.git")
                                                                     .withParameters(ImmutableMap.of("commitId", "12345679"))))

                                      .withId(CORRECT_FACTORY_ID);
        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).thenReturn(factory);
        // when, then
        Response response = given().//
                expect().//
                statusCode(409).//
                when().//
                get(SERVICE_PATH + "/" + CORRECT_FACTORY_ID + "/snippet?type=markdown");

        assertEquals(dto.createDtoFromJson(response.getBody().asInputStream(), ServiceError.class).getMessage(),
                     "Unable to generate markdown snippet for factory without button");
    }

    @Test
    public void shouldNotBeAbleToGetMarkdownSnippetForFactory2WithoutColor() throws Exception {
        // given
        Factory factory = (Factory)dto.createDto(Factory.class)
                                      .withV("2.0")
                                      .withSource(dto.createDto(Source.class)
                                                     .withProject(dto.createDto(ImportSourceDescriptor.class)
                                                                     .withType("git")
                                                                     .withLocation(
                                                                             "http://github.com/codenvy/platform-api.git")
                                                                     .withParameters(ImmutableMap.of("commitId", "12345679"))))

                                      .withId(CORRECT_FACTORY_ID)
                                      .withButton(dto.createDto(Button.class).withType(Button.ButtonType.nologo)
                                                     .withAttributes(dto.createDto(ButtonAttributes.class).withColor(null)));

        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).thenReturn(factory);
        // when, then
        Response response = given().//
                expect().//
                statusCode(409).//
                when().//
                get(SERVICE_PATH + "/" + CORRECT_FACTORY_ID + "/snippet?type=markdown");

        assertEquals(dto.createDtoFromJson(response.getBody().asInputStream(), ServiceError.class).getMessage(),
                     "Unable to generate markdown snippet with nologo button and empty color");
    }

    @Test
    public void shouldResponse404OnGetSnippetIfFactoryDoesNotExist() throws Exception {
        // given
        when(factoryStore.getFactory(ILLEGAL_FACTORY_ID)).thenReturn(null);

        // when, then
        Response response = given().//
                expect().//
                statusCode(404).//
                when().//
                get(SERVICE_PATH + "/" + ILLEGAL_FACTORY_ID + "/snippet?type=url");

        assertEquals(dto.createDtoFromJson(response.getBody().asString(), ServiceError.class).getMessage(),
                     "Factory URL with id " + ILLEGAL_FACTORY_ID + " is not found.");
    }


    /**
     * Checks that the user can remove an existing factory
     * @throws Exception
     */
    @Test
    public void shouldBeAbleToRemoveAFactory() throws Exception {

        // given
        Factory factory = dto.createDto(Factory.class)
                             .withV("2.0")
                             .withSource(dto.createDto(Source.class)
                                            .withProject(dto.createDto(ImportSourceDescriptor.class)
                                                            .withType("git")
                                                            .withLocation(
                                                                    "http://github.com/codenvy/platform-api.git")));
        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).thenReturn(factory);

        // when, then
        Response response =
                given().auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).//
                        param("id", CORRECT_FACTORY_ID).//
                        when().//
                        delete("/private" + SERVICE_PATH + "/" + CORRECT_FACTORY_ID);


        assertEquals(response.getStatusCode(), 204);

        // check there was a call on the remove operation with expected ID
        verify(factoryStore).removeFactory(CORRECT_FACTORY_ID);

    }

    /**
     * Checks that the user can not remove an unknown factory
     * @throws Exception
     */
    @Test
    public void shouldNotBeAbleToRemoveNotExistingFactory() throws Exception {

        // when, then
        Response response =
                given().auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).//
                        param("id", ILLEGAL_FACTORY_ID).//
                        when().//
                        delete("/private" + SERVICE_PATH + "/" + ILLEGAL_FACTORY_ID);


        assertEquals(response.getStatusCode(), 404);

    }


    /**
     * Checks that the user can update an existing factory
     * @throws Exception
     */
    @Test
    public void shouldBeAbleToUpdateAFactory() throws Exception {

        // given
        Factory beforeFactory = dto.createDto(Factory.class)
                                   .withV("2.0")
                                   .withSource(dto.createDto(Source.class)
                                                  .withProject(dto.createDto(ImportSourceDescriptor.class)
                                                                  .withType("git")
                                                                  .withLocation(
                                                                          "http://github.com/codenvy/platform-api.git")))
                                   .withCreator(dto.createDto(Author.class).withCreated(System.currentTimeMillis()));
        beforeFactory.setId(CORRECT_FACTORY_ID);
        Factory afterFactory = dto.createDto(Factory.class)
                                  .withV("2.0")
                                  .withSource(dto.createDto(Source.class)
                                                 .withProject(dto.createDto(ImportSourceDescriptor.class)
                                                                 .withType("git")
                                                                 .withLocation(
                                                                         "http://github.com/codenvy/platform-api2.git")));


        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).thenReturn(beforeFactory);

        // when, then
        Response response =
                given().auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).//
                        contentType("application/json").
                               body(JsonHelper.toJson(afterFactory)).
                               when().//
                        put("/private" + SERVICE_PATH + "/" + CORRECT_FACTORY_ID);


        assertEquals(response.getStatusCode(), 200);

        Factory responseFactory = dto.createDtoFromJson(response.getBody().asInputStream(), Factory.class);
        assertEquals(responseFactory.getSource(), afterFactory.getSource());


        // check there was a call on the update operation with expected ID
        verify(factoryStore).updateFactory(eq(CORRECT_FACTORY_ID), any(Factory.class));

    }


    /**
     * Checks that the user can not update an unknown existing factory
     * @throws Exception
     */
    @Test
    public void shouldNotBeAbleToUpdateAnUnknownFactory() throws Exception {

        // given
        Factory testFactory = dto.createDto(Factory.class)
                                 .withV("2.0")
                                 .withSource(dto.createDto(Source.class)
                                                .withProject(dto.createDto(ImportSourceDescriptor.class)
                                                                .withType("git")
                                                                .withLocation(
                                                                        "http://github.com/codenvy/platform-api.git")));


        // when, then
        Response response =
                given().auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).//
                        contentType("application/json").
                               body(JsonHelper.toJson(testFactory)).
                               when().//
                        put("/private" + SERVICE_PATH + "/" + ILLEGAL_FACTORY_ID);


        assertEquals(response.getStatusCode(), 404);
        assertEquals(dto.createDtoFromJson(response.getBody().asString(), ServiceError.class).getMessage(),
                     format("Factory with id %s does not exist.", ILLEGAL_FACTORY_ID));

    }


    /**
     * Checks that the user can not update a factory with a null one
     * @throws Exception
     */
    @Test
    public void shouldNotBeAbleToUpdateANullFactory() throws Exception {


        // when, then
        Response response =
                given().auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).//
                        contentType("application/json").
                               when().//
                        put("/private" + SERVICE_PATH + "/" + ILLEGAL_FACTORY_ID);
        assertEquals(response.getStatusCode(), 500);
        assertEquals(dto.createDtoFromJson(response.getBody().asString(), ServiceError.class).getMessage(),
                     format("The updating factory shouldn't be null"));

    }

    @Test(dataProvider = "badSnippetTypeProvider")
    public void shouldResponse409OnGetSnippetIfTypeIsIllegal(String type) throws Exception {
        // given
        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).thenReturn(dto.createDto(Factory.class));

        // when, then
        Response response = given().//
                expect().//
                statusCode(409).//
                when().//
                get(SERVICE_PATH + "/" + CORRECT_FACTORY_ID + "/snippet?type=" + type);

        assertEquals(dto.createDtoFromJson(response.getBody().asString(), ServiceError.class).getMessage(),
                     format("Snippet type \"%s\" is unsupported.", type));
    }

    @DataProvider(name = "badSnippetTypeProvider")
    public String[][] badSnippetTypeProvider() {
        return new String[][]{{""},
                              {null},
                              {"mark"}};
    }

    private String getServerUrl(ITestContext context) {
        String serverPort = String.valueOf(context.getAttribute(EverrestJetty.JETTY_PORT));
        return "http://localhost:" + serverPort;
    }

    @Test
    public void shouldRespondNotFoundIfProjectIsNotExist() throws ServerException, ForbiddenException {

        given().//
                expect().//
                statusCode(404).//
                when().//
                get(SERVICE_PATH + "/ws/projectName");
        verify(projectManager).getProject(eq("ws"), eq("projectName"));

    }

    @Test
    public void shouldNotFindWhenNoAttributesProvided() throws Exception {
        // when
        Response response =
                given().auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when().get(
                        "/private" + SERVICE_PATH + "/find");
        // then
        assertEquals(response.getStatusCode(), 500);
    }

    @Test
    public void shoutFindByAttribute() throws Exception {
        // given
        Factory factory = (Factory)dto.createDto(Factory.class)
                                      .withV("2.0")
                                      .withSource(dto.createDto(Source.class)
                                                     .withProject(dto.createDto(ImportSourceDescriptor.class)
                                                                     .withType("git")
                                                                     .withLocation(
                                                                             "http://github.com/codenvy/platform-api.git")
                                                                     .withParameters(ImmutableMap.of("commitId", "12345679"))))

                                      .withId(CORRECT_FACTORY_ID)
                                      .withCreator(dto.createDto(Author.class).withAccountId("testorg"));


        when(factoryStore.findByAttribute(Pair.of("creator.accountid", "testorg"))).thenReturn(
                Arrays.asList(factory, factory));

        // when
        Response response = given().auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).
                when().get("/private" + SERVICE_PATH + "/find?creator.accountid=testorg");

        // then
        assertEquals(response.getStatusCode(), 200);
        List<Link> responseLinks = dto.createListDtoFromJson(response.getBody().asString(), Link.class);
        assertEquals(responseLinks.size(), 2);
    }

    private class FactorySaveAnswer implements Answer<Object> {

        private Factory savedFactory;

        @Override
        public Object answer(InvocationOnMock invocation) throws Throwable {
            if (savedFactory == null) {
                savedFactory = (Factory)invocation.getArguments()[0];
                return CORRECT_FACTORY_ID;
            }
            return dto.clone(savedFactory).withId(CORRECT_FACTORY_ID);
        }
    }

}
