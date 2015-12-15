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
package org.eclipse.che.api.factory.server;

import com.jayway.restassured.response.Response;

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.model.workspace.WorkspaceStatus;
import org.eclipse.che.api.core.rest.ApiExceptionMapper;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.core.rest.shared.dto.ServiceError;
import org.eclipse.che.api.factory.server.builder.FactoryBuilder;
import org.eclipse.che.api.factory.server.impl.SourceStorageParametersValidator;
import org.eclipse.che.api.factory.shared.dto.Author;
import org.eclipse.che.api.factory.shared.dto.Button;
import org.eclipse.che.api.factory.shared.dto.ButtonAttributes;
import org.eclipse.che.api.factory.shared.dto.Factory;
import org.eclipse.che.api.machine.shared.dto.CommandDto;
import org.eclipse.che.api.workspace.server.WorkspaceManager;
import org.eclipse.che.api.workspace.server.model.impl.UsersWorkspaceImpl;
import org.eclipse.che.api.workspace.shared.dto.EnvironmentDto;
import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;
import org.eclipse.che.api.workspace.shared.dto.SourceStorageDto;
import org.eclipse.che.api.workspace.shared.dto.WorkspaceConfigDto;
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

import javax.ws.rs.HttpMethod;
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
import static org.eclipse.che.api.workspace.server.DtoConverter.asDto;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
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
    private FactoryCreateValidator createValidator;

    @Mock
    private FactoryAcceptValidator acceptValidator;

    @Mock
    private FactoryEditValidator editValidator;

    @Mock
    private WorkspaceManager workspaceManager;

    private FactoryBuilder factoryBuilder;

    private FactoryService factoryService;
    private DtoFactory     dto;

    @BeforeMethod
    public void setUp() throws Exception {
        dto = DtoFactory.getInstance();
        factoryBuilder = spy(new FactoryBuilder(new SourceStorageParametersValidator()));
        doNothing().when(factoryBuilder).checkValid(any(Factory.class));
        factoryService = new FactoryService("https://codenvy.com/api",
                                            factoryStore,
                                            createValidator,
                                            acceptValidator,
                                            editValidator,
                                            new LinksHelper(),
                                            factoryBuilder,
                                            workspaceManager);
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
        Factory factory = prepareFactoryWithGivenStorage("git", "http://github.com/codenvy/platform-api.git").withId(CORRECT_FACTORY_ID);

        Factory expected = dto.clone(factory);

        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).thenReturn(factory);
        when(factoryStore.getFactoryImages(CORRECT_FACTORY_ID, null)).thenReturn(Collections.<FactoryImage>emptySet());

        // when
        Response response = given().when()
                                   .get(SERVICE_PATH + "/" + CORRECT_FACTORY_ID);

        // then
        assertEquals(response.getStatusCode(), 200);
        Factory responseFactory = dto.createDtoFromJson(response.getBody()
                                                                .asInputStream(), Factory.class);
        responseFactory.setLinks(Collections.<Link>emptyList());
        assertEquals(responseFactory, expected);
    }


    @Test
    public void shouldBeAbleToSaveFactory() throws Exception {
        // given
        Factory factory = prepareFactoryWithGivenStorage("git", "http://github.com/codenvy/platform-api.git");

        Path path = Paths.get(Thread.currentThread().getContextClassLoader()
                                    .getResource("100x100_image.jpeg")
                                    .toURI());


        FactorySaveAnswer factorySaveAnswer = new FactorySaveAnswer();
        when(factoryStore.saveFactory(any(Factory.class), anySetOf(FactoryImage.class))).then(factorySaveAnswer);
        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).then(factorySaveAnswer);


        // when, then
        Response response =
                given().auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).//
                        multiPart("factory", JsonHelper.toJson(factory), MediaType.APPLICATION_JSON).//
                        multiPart("image", path.toFile(), "image/jpeg").//
                        when().//
                        post("/private" + SERVICE_PATH);

        assertEquals(response.getStatusCode(), 200);
        Factory responseFactory = dto.createDtoFromJson(response.getBody().asInputStream(), Factory.class);
        boolean found = false;
        for (Link link : responseFactory.getLinks()) {
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
                     "No factory information found in 'factory' section of multipart/form-data.");
    }

    @Test
    public void shouldBeAbleToSaveFactoryWithOutImage(ITestContext context) throws Exception {
        // given
        Factory factory = prepareFactoryWithGivenStorage("git", "http://github.com/codenvy/platform-api.git");

        Link expectedCreateProject =
                dto.createDto(Link.class).withMethod(HttpMethod.GET).withProduces("text/html").withRel("create-project")
                   .withHref(getServerUrl(context) + "/f?id=" + CORRECT_FACTORY_ID);

        FactorySaveAnswer factorySaveAnswer = new FactorySaveAnswer();
        when(factoryStore.saveFactory(any(Factory.class), anySetOf(FactoryImage.class))).then(factorySaveAnswer);
        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).then(factorySaveAnswer);

        // when, then
        Response response =
                given().auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD)//
                        .multiPart("factory", JsonHelper.toJson(factory), MediaType.APPLICATION_JSON).when()
                        .post("/private" + SERVICE_PATH);

        // then
        assertEquals(response.getStatusCode(), 200);
        Factory responseFactory = dto.createDtoFromJson(response.getBody().asString(), Factory.class);
        assertTrue(responseFactory.getLinks().contains(
                dto.createDto(Link.class).withMethod(HttpMethod.GET).withProduces(MediaType.APPLICATION_JSON)
                   .withHref(getServerUrl(context) + "/rest/private/factory/" +
                             CORRECT_FACTORY_ID).withRel("self")
                                                         ));
        assertTrue(responseFactory.getLinks().contains(expectedCreateProject));
        assertTrue(responseFactory.getLinks()
                                     .contains(dto.createDto(Link.class).withMethod(HttpMethod.GET).withProduces(MediaType.TEXT_PLAIN)
                                                  .withHref(getServerUrl(context) +
                                                            "/rest/private/analytics/public-metric/factory_used?factory=" +
                                                            encode(expectedCreateProject.getHref(), "UTF-8"))
                                                  .withRel("accepted")));
        assertTrue(responseFactory.getLinks()
                                     .contains(dto.createDto(Link.class).withMethod(HttpMethod.GET).withProduces(MediaType.TEXT_PLAIN)
                                                  .withHref(getServerUrl(context) + "/rest/private/factory/" +
                                                            CORRECT_FACTORY_ID + "/snippet?type=url")
                                                  .withRel("snippet/url")));
        assertTrue(responseFactory.getLinks()
                                     .contains(dto.createDto(Link.class).withMethod(HttpMethod.GET).withProduces(MediaType.TEXT_PLAIN)
                                                  .withHref(getServerUrl(context) + "/rest/private/factory/" +
                                                            CORRECT_FACTORY_ID + "/snippet?type=html")
                                                  .withRel("snippet/html")));
        assertTrue(responseFactory.getLinks()
                                     .contains(dto.createDto(Link.class).withMethod(HttpMethod.GET).withProduces(MediaType.TEXT_PLAIN)
                                                  .withHref(getServerUrl(context) + "/rest/private/factory/" +
                                                            CORRECT_FACTORY_ID + "/snippet?type=markdown")
                                                  .withRel("snippet/markdown")));


        List<Link> expectedLinks = new ArrayList<>(8);
        expectedLinks.add(expectedCreateProject);

        Link self = dto.createDto(Link.class);
        self.setMethod(HttpMethod.GET);
        self.setProduces(MediaType.APPLICATION_JSON);
        self.setHref(getServerUrl(context) + "/rest/private/factory/" + CORRECT_FACTORY_ID);
        self.setRel("self");
        expectedLinks.add(self);

        Link accepted = dto.createDto(Link.class);
        accepted.setMethod(HttpMethod.GET);
        accepted.setProduces(MediaType.TEXT_PLAIN);
        accepted.setHref(getServerUrl(context) + "/rest/private/analytics/public-metric/factory_used?factory=" +
                         encode(expectedCreateProject.getHref(), "UTF-8"));
        accepted.setRel("accepted");
        expectedLinks.add(accepted);

        Link snippetUrl = dto.createDto(Link.class);
        snippetUrl.setProduces(MediaType.TEXT_PLAIN);
        snippetUrl.setHref(getServerUrl(context) + "/rest/private/factory/" + CORRECT_FACTORY_ID + "/snippet?type=url");
        snippetUrl.setRel("snippet/url");
        snippetUrl.setMethod(HttpMethod.GET);
        expectedLinks.add(snippetUrl);

        Link snippetHtml = dto.createDto(Link.class);
        snippetHtml.setProduces(MediaType.TEXT_PLAIN);
        snippetHtml.setHref(getServerUrl(context) + "/rest/private/factory/" + CORRECT_FACTORY_ID +
                            "/snippet?type=html");
        snippetHtml.setMethod(HttpMethod.GET);
        snippetHtml.setRel("snippet/html");
        expectedLinks.add(snippetHtml);

        Link snippetMarkdown = dto.createDto(Link.class);
        snippetMarkdown.setProduces(MediaType.TEXT_PLAIN);
        snippetMarkdown.setHref(getServerUrl(context) + "/rest/private/factory/" + CORRECT_FACTORY_ID +
                                "/snippet?type=markdown");
        snippetMarkdown.setRel("snippet/markdown");
        snippetMarkdown.setMethod(HttpMethod.GET);
        expectedLinks.add(snippetMarkdown);

        Link snippetiFrame = dto.createDto(Link.class);
        snippetiFrame.setProduces(MediaType.TEXT_PLAIN);
        snippetiFrame.setHref(getServerUrl(context) + "/rest/private/factory/" + CORRECT_FACTORY_ID +
                              "/snippet?type=iframe");
        snippetiFrame.setRel("snippet/iframe");
        snippetiFrame.setMethod(HttpMethod.GET);
        expectedLinks.add(snippetiFrame);

        for (Link link : responseFactory.getLinks()) {
            //This transposition need because proxy objects doesn't contains equals method.
            Link testLink = dto.createDto(Link.class);
            testLink.setProduces(link.getProduces());
            testLink.setHref(link.getHref());
            testLink.setRel(link.getRel());
            testLink.setMethod(HttpMethod.GET);
            assertTrue(expectedLinks.contains(testLink));
        }

        verify(factoryStore).saveFactory(Matchers.<Factory>any(), eq(Collections.<FactoryImage>emptySet()));
    }

    @Test
    public void shouldBeAbleToSaveFactoryWithOutImageWithOrgId() throws Exception {
        // given
        Factory factory = prepareFactoryWithGivenStorage("git", "http://github.com/codenvy/platform-api.git");

        FactorySaveAnswer factorySaveAnswer = new FactorySaveAnswer();
        when(factoryStore.saveFactory(any(Factory.class), anySetOf(FactoryImage.class))).then(factorySaveAnswer);
        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).then(factorySaveAnswer);

        // when, then
        Response response =
                given().auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD)//
                        .multiPart("factory", JsonHelper.toJson(factory), MediaType.APPLICATION_JSON).when()
                        .post("/private" + SERVICE_PATH);

        // then
        assertEquals(response.getStatusCode(), 200);

    }


    @Test
    public void shouldBeAbleToSaveFactoryWithSetImageFieldButWithOutImageContent() throws Exception {
        // given
        Factory factory  =  prepareFactoryWithGivenStorage("git", "http://github.com/codenvy/platform-api.git");

        FactorySaveAnswer factorySaveAnswer = new FactorySaveAnswer();
        when(factoryStore.saveFactory(any(Factory.class), anySetOf(FactoryImage.class))).then(factorySaveAnswer);
        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).then(factorySaveAnswer);

        // when, then
        given().auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD)//
                .multiPart("factory", dto.toJson(factory), MediaType.APPLICATION_JSON)//
                .multiPart("image", File.createTempFile("123456", ".jpeg"), "image/jpeg")//
                .expect().statusCode(200)
                .when().post("/private" + SERVICE_PATH);

        verify(factoryStore).saveFactory(Matchers.<Factory>any(), eq(Collections.<FactoryImage>emptySet()));
    }

    @Test
    public void shouldReturnStatus409OnSaveFactoryIfImageHasUnsupportedMediaType() throws Exception {
        // given
        Factory factory  =  prepareFactoryWithGivenStorage("git", "http://github.com/codenvy/platform-api.git");

        Path path = Paths.get(Thread.currentThread().getContextClassLoader().getResource("100x100_image.jpeg").toURI());

        when(factoryStore.saveFactory(any(Factory.class), anySetOf(FactoryImage.class))).thenReturn(CORRECT_FACTORY_ID);
        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).thenReturn(factory);

        // when, then
        Response response = given().auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD)//
                .multiPart("factory", JsonHelper.toJson(factory), MediaType.APPLICATION_JSON)//
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
        Factory factory = dto.createDto(Factory.class);
        factory.setId(CORRECT_FACTORY_ID);
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

        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).thenReturn(factory);
        when(factoryStore.getFactoryImages(CORRECT_FACTORY_ID, null)).thenReturn(images);

        // when
        Response response = given().when().get(SERVICE_PATH + "/" + CORRECT_FACTORY_ID);

        // then
        assertEquals(response.getStatusCode(), 200);
        Factory responseFactory = JsonHelper.fromJson(response.getBody().asString(),
                                                         Factory.class, null);

        List<Link> expectedLinks = new ArrayList<>(9);
        expectedLinks.add(expectedCreateProject);

        Link self = dto.createDto(Link.class);
        self.setProduces(MediaType.APPLICATION_JSON);
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
        accepted.setProduces(MediaType.TEXT_PLAIN);
        accepted.setHref(getServerUrl(context) + "/rest/analytics/public-metric/factory_used?factory=" +
                         encode(expectedCreateProject.getHref(), "UTF-8"));
        accepted.setRel("accepted");
        expectedLinks.add(accepted);

        Link snippetUrl = dto.createDto(Link.class);
        snippetUrl.setProduces(MediaType.TEXT_PLAIN);
        snippetUrl.setHref(getServerUrl(context) + "/rest/factory/" + CORRECT_FACTORY_ID + "/snippet?type=url");
        snippetUrl.setRel("snippet/url");
        expectedLinks.add(snippetUrl);

        Link snippetHtml = dto.createDto(Link.class);
        snippetHtml.setProduces(MediaType.TEXT_PLAIN);
        snippetHtml.setHref(getServerUrl(context) + "/rest/factory/" + CORRECT_FACTORY_ID + "/snippet?type=html");
        snippetHtml.setRel("snippet/html");
        expectedLinks.add(snippetHtml);

        Link snippetMarkdown = dto.createDto(Link.class);
        snippetMarkdown.setProduces(MediaType.TEXT_PLAIN);
        snippetMarkdown.setHref(getServerUrl(context) + "/rest/factory/" + CORRECT_FACTORY_ID +
                                "/snippet?type=markdown");
        snippetMarkdown.setRel("snippet/markdown");
        expectedLinks.add(snippetMarkdown);

        Link snippetiFrame = dto.createDto(Link.class);
        snippetiFrame.setProduces(MediaType.TEXT_PLAIN);
        snippetiFrame.setHref(getServerUrl(context) + "/rest/factory/" + CORRECT_FACTORY_ID +
                              "/snippet?type=iframe");
        snippetiFrame.setRel("snippet/iframe");
        expectedLinks.add(snippetiFrame);

        for (Link link : responseFactory.getLinks()) {
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
        doThrow(new NotFoundException(format("Factory with id %s is not found.", ILLEGAL_FACTORY_ID))).when(factoryStore)
                                                                                                          .getFactory(anyString());

        // when, then
        Response response = given()
                .expect()
                .statusCode(404)
                .when()
                .get(SERVICE_PATH + "/" + ILLEGAL_FACTORY_ID);

        assertEquals(dto.createDtoFromJson(response.getBody().asString(), ServiceError.class).getMessage(),
                     format("Factory with id %s is not found.", ILLEGAL_FACTORY_ID));
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
        when(factoryStore.getFactoryImages(CORRECT_FACTORY_ID, null)).thenReturn(new HashSet<>());

        // when, then
        Response response = given()
                .expect()
                .statusCode(404)
                .when()
                .get(SERVICE_PATH + "/" + CORRECT_FACTORY_ID + "/image?imgId=illegalImageId");

        assertEquals(dto.createDtoFromJson(response.getBody().asString(), ServiceError.class).getMessage(),
                     format("Image with id %s is not found.", "illegalImageId"));
    }

    @Test
    public void shouldResponse404OnGetImageIfFactoryDoesNotExist() throws Exception {
        // given
        doThrow(new NotFoundException(format("Factory with id %s is not found.", ILLEGAL_FACTORY_ID))).when(factoryStore)
                                                                                                      .getFactoryImages(anyString(), anyString());

        // when, then
        Response response = given()
                .expect()
                .statusCode(404)
                .when()
                .get(SERVICE_PATH + "/" + ILLEGAL_FACTORY_ID + "/image?imgId=ImageId");

        assertEquals(dto.createDtoFromJson(response.getBody().asString(), ServiceError.class).getMessage(),
                     format("Factory with id %s is not found.", ILLEGAL_FACTORY_ID));
    }

    @Test
    public void shouldBeAbleToReturnUrlSnippet(ITestContext context) throws Exception {
        // given
        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).thenReturn(dto.createDto
                (Factory.class));

        // when, then
        given()
                .expect()
                .statusCode(200)
                .contentType(MediaType.TEXT_PLAIN)
                .body(equalTo(getServerUrl(context) + "/factory?id=" + CORRECT_FACTORY_ID))
                .when()
                .get(SERVICE_PATH + "/" + CORRECT_FACTORY_ID + "/snippet?type=url");
    }

    @Test
    public void shouldBeAbleToReturnUrlSnippetIfTypeIsNotSet(ITestContext context) throws Exception {
        // given
        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).thenReturn(dto.createDto
                (Factory.class));

        // when, then
        given()
                .expect()
                .statusCode(200)
                .contentType(MediaType.TEXT_PLAIN)
                .body(equalTo(getServerUrl(context) + "/factory?id=" + CORRECT_FACTORY_ID))
                .when()
                .get(SERVICE_PATH + "/" + CORRECT_FACTORY_ID + "/snippet");
    }

    @Test
    public void shouldBeAbleToReturnHtmlSnippet(ITestContext context) throws Exception {
        // given
        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).thenReturn(dto.createDto(Factory.class));

        // when, then
        Response response = given()
                .expect()
                .statusCode(200)
                .contentType(MediaType.TEXT_PLAIN)
                .when()
                .get(SERVICE_PATH + "/" + CORRECT_FACTORY_ID + "/snippet?type=html");

        assertEquals(response.body().asString(), "<script type=\"text/javascript\" src=\"" + getServerUrl(context) +
                                                 "/factory/resources/factory.js?" + CORRECT_FACTORY_ID + "\"></script>");
    }

    @Test
    public void shouldBeAbleToReturnMarkdownSnippetForFactory1WithImage(ITestContext context) throws Exception {
        // given
        Factory factory  =  dto.createDto(Factory.class)
                               .withV("4.0")
                               .withWorkspace(dto.createDto(WorkspaceConfigDto.class)
                                                 .withProjects(Collections.singletonList(dto.createDto(
                                                         ProjectConfigDto.class)
                                                                                            .withSource(
                                                                                                    dto.createDto(SourceStorageDto.class)
                                                                                                       .withType("git")
                                                                                                       .withLocation(
                                                                                                               "http://github.com/codenvy/platform-api.git")))))
                               .withId(CORRECT_FACTORY_ID)
                               .withButton(dto.createDto(Button.class)
                                              .withType(Button.ButtonType.logo));
        String imageName = "1241234";
        FactoryImage image = new FactoryImage();
        image.setName(imageName);

        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).thenReturn(factory);
        when(factoryStore.getFactoryImages(CORRECT_FACTORY_ID, null)).thenReturn(new HashSet<>(Arrays.asList(image)));
        // when, then
        given()
                .expect()
                .statusCode(200)
                .contentType(MediaType.TEXT_PLAIN)
                .body(
                equalTo("[![alt](" + getServerUrl(context) + "/api/factory/" + CORRECT_FACTORY_ID + "/image?imgId=" +
                        imageName + ")](" +
                        getServerUrl(context) + "/factory?id=" +
                        CORRECT_FACTORY_ID + ")")
                    )
                .when()
                .get(SERVICE_PATH + "/" + CORRECT_FACTORY_ID + "/snippet?type=markdown");
    }


    @Test
    public void shouldBeAbleToReturnMarkdownSnippetForFactory2WithImage(ITestContext context) throws Exception {
        // given
        String imageName = "1241234";
        Factory factory = dto.createDto(Factory.class);
        factory.setId(CORRECT_FACTORY_ID);
        factory.setV("2.0");
        factory.setButton(dto.createDto(Button.class).withType(Button.ButtonType.logo));

        FactoryImage image = new FactoryImage();
        image.setName(imageName);

        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).thenReturn(factory);
        when(factoryStore.getFactoryImages(CORRECT_FACTORY_ID, null)).thenReturn(new HashSet<>(Arrays.asList(image)));
        // when, then
        given()
                .expect()
                .statusCode(200)
                .contentType(MediaType.TEXT_PLAIN)
                .body(
                        equalTo("[![alt](" + getServerUrl(context) + "/api/factory/" + CORRECT_FACTORY_ID + "/image?imgId=" +
                                imageName + ")](" +
                                getServerUrl(context) + "/factory?id=" +
                                CORRECT_FACTORY_ID + ")")
                     )
                .when()
                .get(SERVICE_PATH + "/" + CORRECT_FACTORY_ID + "/snippet?type=markdown");
    }

    @Test
    public void shouldBeAbleToReturnMarkdownSnippetForFactory1WithoutImage(ITestContext context) throws Exception {
        // given
        Factory factory = prepareFactoryWithGivenStorage("git", "http://github.com/codenvy/platform-api.git")
                .withId(CORRECT_FACTORY_ID)
                .withButton(dto.createDto(Button.class)
                               .withType(Button.ButtonType.nologo)
                               .withAttributes(dto.createDto(ButtonAttributes.class)
                                                  .withColor("white")));

        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).thenReturn(factory);
        // when, then
        given()
                .expect()
                .statusCode(200)
                .contentType(MediaType.TEXT_PLAIN)
                .body(
                        equalTo("[![alt](" + getServerUrl(context) + "/factory/resources/factory-white.png)](" + getServerUrl
                                (context) +
                                "/factory?id=" +
                                CORRECT_FACTORY_ID + ")")
                     )
                .when()
                .get(SERVICE_PATH + "/" + CORRECT_FACTORY_ID + "/snippet?type=markdown");
    }


    @Test
    public void shouldNotBeAbleToGetMarkdownSnippetForFactory1WithoutStyle() throws Exception {
        // given
        Factory factory = prepareFactoryWithGivenStorage("git", "http://github.com/codenvy/platform-api.git").withId(CORRECT_FACTORY_ID);
        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).thenReturn(factory);
        // when, then
        Response response = given()
                .expect()
                .statusCode(409)
                .when()
                .get(SERVICE_PATH + "/" + CORRECT_FACTORY_ID + "/snippet?type=markdown");

        assertEquals(dto.createDtoFromJson(response.getBody().asInputStream(), ServiceError.class).getMessage(),
                     "Unable to generate markdown snippet for factory without button");
    }

    @Test
    public void shouldNotBeAbleToGetMarkdownSnippetForFactory2WithoutColor() throws Exception {
        // given
        Factory factory = prepareFactoryWithGivenStorage("git", "http://github.com/codenvy/platform-api.git")
                .withId(CORRECT_FACTORY_ID)
                .withButton(dto.createDto(Button.class)
                               .withType(Button.ButtonType.nologo)
                               .withAttributes(dto.createDto(ButtonAttributes.class)
                                                  .withColor(null)));

        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).thenReturn(factory);
        // when, then
        Response response = given()
                .expect()
                .statusCode(409)
                .when()
                .get(SERVICE_PATH + "/" + CORRECT_FACTORY_ID + "/snippet?type=markdown");

        assertEquals(dto.createDtoFromJson(response.getBody().asInputStream(), ServiceError.class).getMessage(),
                     "Unable to generate markdown snippet with nologo button and empty color");
    }

    @Test
    public void shouldResponse404OnGetSnippetIfFactoryDoesNotExist() throws Exception {
        // given
        doThrow(new NotFoundException("Factory URL with id " + ILLEGAL_FACTORY_ID + " is not found.")).when(factoryStore)
                                                                                                      .getFactory(anyString());

        // when, then
        Response response = given()
                .expect()
                .statusCode(404)
                .when()
                .get(SERVICE_PATH + "/" + ILLEGAL_FACTORY_ID + "/snippet?type=url");

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
        Factory factory = prepareFactoryWithGivenStorage("git", "http://github.com/codenvy/platform-api.git");
        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).thenReturn(factory);

        // when, then
        Response response =
                given().auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD)
                       .param("id", CORRECT_FACTORY_ID)
                       .when()
                       .delete("/private" + SERVICE_PATH + "/" + CORRECT_FACTORY_ID);


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
        doThrow(new NotFoundException("Not found")).when(factoryStore).removeFactory(anyString());

        // when, then
        Response response =
                given().auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD)
                       .param("id", ILLEGAL_FACTORY_ID)
                       .when()
                       .delete("/private" + SERVICE_PATH + "/" + ILLEGAL_FACTORY_ID);

        assertEquals(response.getStatusCode(), 404);

    }


    /**
     * Checks that the user can update an existing factory
     * @throws Exception
     */
    @Test
    public void shouldBeAbleToUpdateFactory() throws Exception {

        // given
        Factory beforeFactory = prepareFactoryWithGivenStorage("git", "http://github.com/codenvy/platform-api.git")
                                   .withCreator(dto.createDto(Author.class)
                                                   .withCreated(System.currentTimeMillis()));
        beforeFactory.setId(CORRECT_FACTORY_ID);
        Factory afterFactory = prepareFactoryWithGivenStorage("git", "http://github.com/codenvy/platform-api2.git");

        when(factoryStore.getFactory(CORRECT_FACTORY_ID)).thenReturn(beforeFactory);

        // when, then
        Response response =
                given().auth()
                       .basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD)
                       .contentType(MediaType.APPLICATION_JSON)
                       .body(JsonHelper.toJson(afterFactory))
                       .when()
                       .put("/private" + SERVICE_PATH + "/" + CORRECT_FACTORY_ID);

        assertEquals(response.getStatusCode(), 200);

        Factory responseFactory = dto.createDtoFromJson(response.getBody().asInputStream(), Factory.class);
        assertEquals(responseFactory.getWorkspace(), afterFactory.getWorkspace());


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
        Factory factory = prepareFactoryWithGivenStorage("git", "http://github.com/codenvy/platform-api.git");
        doThrow(new NotFoundException(format("Factory with id %s is not found.", ILLEGAL_FACTORY_ID))).when(factoryStore)
                                                                                                      .getFactory(anyString());

        // when, then
        Response response =
                given().auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD)
                       .contentType(MediaType.APPLICATION_JSON)
                       .body(JsonHelper.toJson(factory))
                       .when()
                       .put("/private" + SERVICE_PATH + "/" + ILLEGAL_FACTORY_ID);

        assertEquals(response.getStatusCode(), 404);
        assertEquals(dto.createDtoFromJson(response.getBody().asString(), ServiceError.class).getMessage(),
                     format("Factory with id %s is not found.", ILLEGAL_FACTORY_ID));
    }


    /**
     * Checks that the user can not update a factory with a null one
     * @throws Exception
     */
    @Test
    public void shouldNotBeAbleToUpdateANullFactory() throws Exception {


        // when, then
        Response response =
                given().auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD)
                       .contentType(MediaType.APPLICATION_JSON)
                       .when()
                       .put("/private" + SERVICE_PATH + "/" + ILLEGAL_FACTORY_ID);
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
    public void shouldNotFindWhenNoAttributesProvided() throws Exception {
        // when
        Response response =
                given().auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD).when().get(
                        "/private" + SERVICE_PATH + "/find");
        // then
        assertEquals(response.getStatusCode(), 500);
    }

    @Test
    public void shouldFindByAttribute() throws Exception {
        // given
        Factory factory = prepareFactoryWithGivenStorage("git", "http://github.com/codenvy/platform-api.git") 
                .withId(CORRECT_FACTORY_ID)
                .withCreator(dto.createDto(Author.class)
                                .withAccountId("testorg"));
                                
        List<Pair<String, String>>  expected = Collections.singletonList(Pair.of("creator.accountid", "testorg"));
        when(factoryStore.findByAttribute(anyInt(), anyInt(), eq(expected))).thenReturn(
                Arrays.asList(factory, factory));

        // when
        Response response = given().auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD)
                                   .when().get("/private" + SERVICE_PATH + "/find?creator.accountid=testorg");

        // then
        assertEquals(response.getStatusCode(), 200);
        List<Factory> responseFactories = dto.createListDtoFromJson(response.getBody().asString(), Factory.class);
        assertEquals(responseFactories.size(), 2);
    }


    @Test
    public void shouldGenerateFactoryJsonIncludeAllProjects() throws Exception {
        // given
        final String wsId = "workspace123234";
        UsersWorkspaceImpl.UsersWorkspaceImplBuilder ws = UsersWorkspaceImpl.builder();
        ws.setId(wsId);
        ws.setProjects(Arrays.asList(dto.createDto(ProjectConfigDto.class)
                                        .withSource(dto.createDto(SourceStorageDto.class)
                                                       .withType("git")
                                                       .withLocation("location")),
                                     dto.createDto(ProjectConfigDto.class)
                                        .withSource(dto.createDto(SourceStorageDto.class)
                                                       .withType("git")
                                                       .withLocation("location"))));
        ws.setName("wsname");
        ws.setOwner("id-2314");
        ws.setDefaultEnvName("env1");
        ws.setEnvironments(Collections.singletonMap("env1", dto.createDto(EnvironmentDto.class).withName("env1")));
        ws.setStatus(WorkspaceStatus.RUNNING);
        ws.setCommands(Collections.singletonList(
                dto.createDto(CommandDto.class).withName("MCI").withType("mvn").withCommandLine("clean install")));

        UsersWorkspaceImpl usersWorkspace = ws.build();
        when(workspaceManager.getWorkspace(eq(wsId))).thenReturn(usersWorkspace);

        // when
        Response response = given().auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD)
                .when().get("/private" + SERVICE_PATH + "/workspace/" + wsId);

        // then
        assertEquals(response.getStatusCode(), 200);
        Factory result = dto.createDtoFromJson(response.getBody().asString(), Factory.class);
        assertEquals(result.getWorkspace().getProjects().size(), 2);
        assertEquals(result.getWorkspace().getName(), usersWorkspace.getName());
        assertEquals(result.getWorkspace().getEnvironments().get("env1").toString(),
                     asDto(usersWorkspace.getEnvironments().get("env1")).toString());
        assertEquals(result.getWorkspace().getCommands().get(0), asDto(usersWorkspace.getCommands().get(0)));
    }

    @Test
    public void shouldGenerateFactoryJsonIncludeGivenProjects() throws Exception {
        // given
        final String wsId = "workspace123234";
        UsersWorkspaceImpl.UsersWorkspaceImplBuilder ws  = UsersWorkspaceImpl.builder();
        ws.setId(wsId);
        ws.setProjects(Arrays.asList(dto.createDto(ProjectConfigDto.class)
                                        .withPath("/proj1")
                                        .withSource(dto.createDto(SourceStorageDto.class)
                                                       .withType("git")
                                                       .withLocation("location")),
                                     dto.createDto(ProjectConfigDto.class)
                                        .withPath("/proj2")
                                        .withSource(
                                                dto.createDto(SourceStorageDto.class)
                                                   .withType("git")
                                                   .withLocation("location"))));
        ws.setName("wsname");
        ws.setOwner("id-2314");
        ws.setEnvironments(Collections.singletonMap("env1", dto.createDto(EnvironmentDto.class).withName("env1")));
        ws.setDefaultEnvName("env1");
        ws.setStatus(WorkspaceStatus.RUNNING);
        ws.setCommands(Collections.singletonList(
                dto.createDto(CommandDto.class).withName("MCI").withType("mvn").withCommandLine("clean install")));

        UsersWorkspaceImpl usersWorkspace = ws.build();
        when(workspaceManager.getWorkspace(eq(wsId))).thenReturn(usersWorkspace);

        // when
        Response response = given().auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD)
                .when().get("/private" + SERVICE_PATH + "/workspace/" + wsId + "?path=/proj1");

        // then
        assertEquals(response.getStatusCode(), 200);
        Factory result = dto.createDtoFromJson(response.getBody().asString(), Factory.class);
        assertEquals(result.getWorkspace().getProjects().size(), 1);
    }


    @Test
    public void shouldNotGenerateFactoryIfNoProjectsWithSourceStorage() throws Exception {
        // given
        final String wsId = "workspace123234";
        UsersWorkspaceImpl.UsersWorkspaceImplBuilder ws  = UsersWorkspaceImpl.builder();
        ws.setId(wsId);
        ws.setProjects(Arrays.asList(dto.createDto(ProjectConfigDto.class)
                                        .withPath("/proj1"),
                                     dto.createDto(
                                             ProjectConfigDto.class)
                                        .withPath("/proj2")));
        ws.setName("wsname");
        ws.setOwner("id-2314");
        ws.setEnvironments(Collections.singletonMap("env1", dto.createDto(EnvironmentDto.class).withName("env1")));
        ws.setDefaultEnvName("env1");
        ws.setStatus(WorkspaceStatus.RUNNING);
        ws.setCommands(Collections.singletonList(
                dto.createDto(CommandDto.class).withName("MCI").withType("mvn").withCommandLine("clean install")));

        UsersWorkspaceImpl usersWorkspace = ws.build();
        when(workspaceManager.getWorkspace(eq(wsId))).thenReturn(usersWorkspace);

        // when
        Response response = given().auth().basic(JettyHttpServer.ADMIN_USER_NAME, JettyHttpServer.ADMIN_USER_PASSWORD)
                .when().get("/private" + SERVICE_PATH + "/workspace/" + wsId);

        // then
        assertEquals(response.getStatusCode(), 409);
    }


    private Factory prepareFactoryWithGivenStorage(String type, String location) {
        return dto.createDto(Factory.class)
                  .withV("4.0")
                  .withWorkspace(dto.createDto(WorkspaceConfigDto.class)
                                    .withProjects(Collections.singletonList(dto.createDto(
                                            ProjectConfigDto.class)
                                                                               .withSource(
                                                                                       dto.createDto(
                                                                                               SourceStorageDto.class)
                                                                                          .withType(
                                                                                                  type)
                                                                                          .withLocation(
                                                                                                  location)))));
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
