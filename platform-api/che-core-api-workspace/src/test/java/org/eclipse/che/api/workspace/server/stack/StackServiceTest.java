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
package org.eclipse.che.api.workspace.server.stack;

import com.jayway.restassured.response.Response;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.rest.ApiExceptionMapper;
import org.eclipse.che.api.core.rest.shared.dto.ServiceError;
import org.eclipse.che.api.machine.server.model.impl.CommandImpl;
import org.eclipse.che.api.machine.server.model.impl.LimitsImpl;
import org.eclipse.che.api.machine.server.model.impl.MachineConfigImpl;
import org.eclipse.che.api.machine.server.model.impl.MachineSourceImpl;
import org.eclipse.che.api.machine.server.recipe.PermissionsChecker;
import org.eclipse.che.api.machine.shared.Permissible;
import org.eclipse.che.api.workspace.server.dao.StackDao;
import org.eclipse.che.api.workspace.server.model.impl.EnvironmentImpl;
import org.eclipse.che.api.workspace.server.model.impl.WorkspaceConfigImpl;
import org.eclipse.che.api.workspace.server.model.impl.stack.StackImpl;
import org.eclipse.che.api.workspace.server.model.impl.stack.StackComponent;
import org.eclipse.che.api.workspace.server.model.impl.stack.StackComponentImpl;
import org.eclipse.che.api.workspace.server.model.impl.stack.StackSourceImpl;
import org.eclipse.che.api.workspace.server.stack.image.StackIcon;
import org.eclipse.che.api.workspace.shared.dto.stack.StackDto;
import org.eclipse.che.api.workspace.shared.dto.stack.StackComponentDto;
import org.eclipse.che.api.workspace.shared.dto.stack.StackSourceDto;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.user.UserImpl;
import org.eclipse.che.dto.server.DtoFactory;
import org.everrest.assured.EverrestJetty;
import org.everrest.core.Filter;
import org.everrest.core.GenericContainerRequest;
import org.everrest.core.RequestFilter;
import org.everrest.core.impl.uri.UriBuilderImpl;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static com.jayway.restassured.RestAssured.given;
import static java.lang.String.format;
import static org.eclipse.che.dto.server.DtoFactory.newDto;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import static org.everrest.assured.JettyHttpServer.ADMIN_USER_NAME;
import static org.everrest.assured.JettyHttpServer.ADMIN_USER_PASSWORD;
import static org.everrest.assured.JettyHttpServer.SECURE_PATH;
import static org.eclipse.che.api.workspace.server.Constants.LINK_REL_REMOVE_STACK;
import static org.eclipse.che.api.workspace.server.Constants.LINK_REL_GET_STACK_BY_ID;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.testng.Assert.assertTrue;

/**
 * Test for {@link @StackService}
 *
 * @author Alexander Andrienko
 */
@Listeners(value = {EverrestJetty.class, MockitoTestNGListener.class})
public class StackServiceTest {

    private static final String ID_TEST     = "java-default";
    private static final String NAME        = "Java";
    private static final String DESCRIPTION = "Default Java Stack with JDK 8, Maven and Tomcat.";
    private static final String USER_ID     = "che";
    private static final String CREATOR     = USER_ID;
    private static final String SCOPE       = "general";

    private static final String SOURCE_TYPE   = "image";
    private static final String SOURCE_ORIGIN = "codenvy/ubuntu_jdk8";

    private static final String COMPONENT_NAME    = "Java";
    private static final String COMPONENT_VERSION = "1.8.0_45";

    private static final String WORKSPACE_CONFIG_NAME = "default";
    private static final String DEF_ENVIRONMENT_NAME  = "default";

    private static final String COMMAND_NAME = "newMaven";
    private static final String COMMAND_TYPE = "mvn";
    private static final String COMMAND_LINE = "mvn clean install -f ${current.project.path}";

    private static final String ENVIRONMENT_NAME = "default";
    private static final String ENVIRONMENT_KEY  = "default";

    private static final String  MACHINE_CONFIG_NAME = "ws-machine";
    private static final String  MACHINE_TYPE        = "docker";
    private static final boolean IS_DEV              = true;

    private static final String MACHINE_SOURCE_LOCATION = "http://localhost:8080/ide/api/recipe/recipe_ubuntu/script";
    private static final String MACHINE_SOURCE_TYPE     = "recipe";

    private static final String ICON_MEDIA_TYPE = "image/svg+xml";

    @SuppressWarnings("unused")
    static final   EnvironmentFilter  FILTER = new EnvironmentFilter();
    @SuppressWarnings("unused")
    static final   ApiExceptionMapper MAPPER = new ApiExceptionMapper();
    private static LinkedList<String> ROLES  = new LinkedList<>(Collections.singletonList("user"));

    private List<String> tags = Arrays.asList("java", "maven");
    private StackDto             stackDto;
    private StackImpl            stackImpl;
    private StackSourceImpl      stackSourceImpl;
    private List<StackComponent> componentsImpl;
    private StackIcon            stackIcon;

    private StackSourceDto          stackSourceDto;
    private List<StackComponentDto> componentsDto;

    @Mock
    StackDao stackDao;

    @Mock
    UriInfo uriInfo;

    @Mock
    StackComponentImpl stackComponent;
    @Mock
    PermissionsChecker checker;

    @InjectMocks
    StackService service;

    @BeforeClass
    public void setUp() throws IOException, ConflictException {
        byte[] fileContent = ID_TEST.getBytes();
        stackIcon = new StackIcon(ICON_MEDIA_TYPE, fileContent);
        componentsImpl = Collections.singletonList(new StackComponentImpl(COMPONENT_NAME, COMPONENT_VERSION));
        stackSourceImpl = new StackSourceImpl(SOURCE_TYPE, SOURCE_ORIGIN);
        CommandImpl command = new CommandImpl(COMMAND_NAME, COMMAND_LINE, COMMAND_TYPE);
        MachineSourceImpl machineSource = new MachineSourceImpl(MACHINE_SOURCE_TYPE, MACHINE_SOURCE_LOCATION);
        int limitMemory = 1000;
        LimitsImpl limits = new LimitsImpl(limitMemory);
        MachineConfigImpl machineConfig = new MachineConfigImpl(IS_DEV, MACHINE_CONFIG_NAME, MACHINE_TYPE, machineSource, limits);
        EnvironmentImpl environment = new EnvironmentImpl(ENVIRONMENT_NAME, null, Collections.singletonList(machineConfig));

        WorkspaceConfigImpl workspaceConfig = WorkspaceConfigImpl.builder()
                                                                 .setName(WORKSPACE_CONFIG_NAME)
                                                                 .setDefaultEnvName(DEF_ENVIRONMENT_NAME)
                                                                 .setCommands(Collections.singletonList(command))
                                                                 .setEnvironments(Collections.singletonMap(ENVIRONMENT_KEY,
                                                                                                           environment))
                                                                 .build();

        stackSourceDto = newDto(StackSourceDto.class).withType(SOURCE_TYPE).withOrigin(SOURCE_ORIGIN);
        StackComponentDto stackComponentDto = newDto(StackComponentDto.class).withName(COMPONENT_NAME).withVersion(COMPONENT_VERSION);
        componentsDto = Collections.singletonList(stackComponentDto);

        stackDto = DtoFactory.getInstance().createDto(StackDto.class).withId(ID_TEST)
                             .withName(NAME)
                             .withDescription(DESCRIPTION)
                             .withScope(SCOPE)
                             .withCreator(CREATOR)
                             .withTags(tags)
                             .withSource(stackSourceDto)
                             .withComponents(componentsDto);

        stackImpl = StackImpl.builder().setId(ID_TEST)
                             .setName(NAME)
                             .setDescription(DESCRIPTION)
                             .setScope(SCOPE)
                             .setCreator(CREATOR)
                             .setTags(tags)
                             .setSource(stackSourceImpl)
                             .setComponents(componentsImpl)
                             .setWorkspaceConfig(workspaceConfig)
                             .setIcon(stackIcon)
                             .build();
    }

    @BeforeMethod
    public void setUpUriInfo() throws NoSuchFieldException, IllegalAccessException {
        when(uriInfo.getBaseUriBuilder()).thenReturn(new UriBuilderImpl());

        final Field uriField = service.getClass()
                                      .getSuperclass()
                                      .getDeclaredField("uriInfo");
        uriField.setAccessible(true);
        uriField.set(service, uriInfo);
    }

    @Test
    public void newStackShouldBeCreated() throws ConflictException, ServerException {
        final Response response = given().auth()
                                         .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
                                         .contentType(APPLICATION_JSON)
                                         .body(stackDto)
                                         .when()
                                         .post(SECURE_PATH + "/stack");

        assertEquals(response.getStatusCode(), 201);

        verify(stackDao).create(any(StackImpl.class));

        final StackDtoDescriptor stackDtoDescriptor = unwrapDto(response, StackDtoDescriptor.class);

        assertEquals(stackDtoDescriptor.getId(), stackDto.getId());
        assertEquals(stackDtoDescriptor.getName(), stackDto.getName());
        assertEquals(stackDtoDescriptor.getCreator(), USER_ID);
        assertEquals(stackDtoDescriptor.getDescription(), stackDto.getDescription());
        assertEquals(stackDtoDescriptor.getTags(), stackDto.getTags());

        assertEquals(stackDtoDescriptor.getComponents(), stackDto.getComponents());

        assertEquals(stackDtoDescriptor.getSource(), stackDto.getSource());

        assertEquals(stackDtoDescriptor.getScope(), stackDto.getScope());

        assertEquals(stackDtoDescriptor.getLinks().size(), 2);
        assertTrue(stackDtoDescriptor.getLinks().get(0).getHref().endsWith("/stack/java-default"));
        assertEquals(stackDtoDescriptor.getLinks().get(0).getRel(), LINK_REL_REMOVE_STACK);
        assertTrue(stackDtoDescriptor.getLinks().get(1).getHref().endsWith("/stack/java-default"));
        assertEquals(stackDtoDescriptor.getLinks().get(1).getRel(), LINK_REL_GET_STACK_BY_ID);
    }

    @Test
    public void shouldThrowBadRequestExceptionOnCreateStackWithEmptyBody() {
        final Response response = given().auth()
                                         .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
                                         .contentType(APPLICATION_JSON)
                                         .when()
                                         .post(SECURE_PATH + "/stack");

        assertEquals(response.getStatusCode(), 400);
        assertEquals(unwrapDto(response, ServiceError.class).getMessage(), "Stack required");
    }

    @Test
    public void shouldThrowBadRequestExceptionOnCreateStackWithEmptyName() {
        StackComponentDto stackComponentDto = newDto(StackComponentDto.class).withName("Java").withVersion("1.8.45");
        StackSourceDto stackSourceDto = newDto(StackSourceDto.class).withType("image").withOrigin("codenvy/ubuntu_jdk8");
        StackDto stackDto = newDto(StackDto.class).withId(USER_ID)
                                                  .withDescription("")
                                                  .withScope("Simple java stack for generation java projects")
                                                  .withTags(Arrays.asList("java", "maven"))
                                                  .withCreator("che")
                                                  .withComponents(Collections.singletonList(stackComponentDto))
                                                  .withSource(stackSourceDto);

        Response response = given().auth()
                                   .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
                                   .contentType(APPLICATION_JSON)
                                   .body(stackDto)
                                   .when()
                                   .post(SECURE_PATH + "/stack");

        assertEquals(response.getStatusCode(), 400);
        assertEquals(unwrapDto(response, ServiceError.class).getMessage(), "Stack name required");
    }

    @Test
    public void stackByIdShouldBeReturned() throws NotFoundException, ServerException {
        when(checker.hasAccess(any(StackImpl.class), eq(CREATOR), eq("read"))).thenReturn(true);

        when(stackDao.getById(anyString())).thenReturn(stackImpl);

        Response response = given().auth()
                                   .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
                                   .when()
                                   .get(SECURE_PATH + "/stack/" + ID_TEST);

        assertEquals(response.getStatusCode(), 200);

        StackDtoDescriptor result = unwrapDto(response, StackDtoDescriptor.class);
        assertEquals(result.getId(), stackImpl.getId());
        assertEquals(result.getName(), stackImpl.getName());
        assertEquals(result.getDescription(), stackImpl.getDescription());
        assertEquals(result.getScope(), stackImpl.getScope());
        assertEquals(result.getTags().get(0), stackImpl.getTags().get(0));
        assertEquals(result.getTags().get(1), stackImpl.getTags().get(1));
        assertEquals(result.getComponents().get(0).getName(), stackImpl.getComponents().get(0).getName());
        assertEquals(result.getComponents().get(0).getVersion(), stackImpl.getComponents().get(0).getVersion());
        assertEquals(result.getSource().getType(), stackImpl.getSource().getType());
        assertEquals(result.getSource().getOrigin(), stackImpl.getSource().getOrigin());
        assertEquals(result.getCreator(), stackImpl.getCreator());
    }

    @Test
    public void shouldThrowForbiddenExceptionWhenWeTryToGetForeignStack() throws NotFoundException, ServerException {
        StackImpl foreignStack = StackImpl.builder()
                                          .setId(ID_TEST)
                                          .setName(NAME)
                                          .setDescription(DESCRIPTION)
                                          .setScope(SCOPE)
                                          .setCreator("SomeUser")
                                          .setTags(tags)
                                          .setSource(stackSourceImpl)
                                          .setComponents(componentsImpl)
                                          .setIcon(stackIcon)
                                          .build();

        when(stackDao.getById(anyString())).thenReturn(foreignStack);

        Response response = given().auth()
                                   .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
                                   .when()
                                   .get(SECURE_PATH + "/stack/" + ID_TEST);

        assertEquals(response.getStatusCode(), 403);
        String expectedMessage = format("User %s doesn't have access to stack %s", USER_ID, stackImpl.getId());
        assertEquals(unwrapDto(response, ServiceError.class).getMessage(), expectedMessage);
    }

    @Test
    public void shouldThrowForbiddenExceptionWhenUserTryUpdatePredefinedStack()
            throws NotFoundException, ServerException, ConflictException {
        String updateSuffix = " updated";
        StackDto updatedStackDto = DtoFactory.getInstance().createDto(StackDto.class)
                                             .withId(ID_TEST)
                                             .withName(NAME + updateSuffix)
                                             .withDescription(DESCRIPTION + updateSuffix)
                                             .withScope(SCOPE + updateSuffix)
                                             .withCreator(CREATOR + 1)
                                             .withTags(tags)
                                             .withSource(stackSourceDto)
                                             .withComponents(componentsDto);

        StackImpl updatedStack = StackImpl.builder().setId(ID_TEST)
                                          .setName(NAME + updateSuffix)
                                          .setDescription(DESCRIPTION + updateSuffix)
                                          .setScope(SCOPE + updateSuffix)
                                          .setCreator(CREATOR + 1)
                                          .setTags(tags)
                                          .setSource(stackSourceImpl)
                                          .setComponents(componentsImpl)
                                          .setIcon(stackIcon)
                                          .build();

        when(stackDao.getById(ID_TEST)).thenReturn(updatedStack);
        when(checker.hasAccess(any(Permissible.class), eq(CREATOR), anyString())).thenReturn(false);

        Response response = given().auth()
                                   .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
                                   .contentType(APPLICATION_JSON)
                                   .content(updatedStackDto)
                                   .when()
                                   .put(SECURE_PATH + "/stack");

        assertEquals(response.getStatusCode(), 403);
        String expectedMessage = format("User %s doesn't have access to update stack %s", USER_ID, stackImpl.getId());
        assertEquals(unwrapDto(response, ServiceError.class).getMessage(), expectedMessage);
        verify(stackDao).getById(ID_TEST);
        verify(stackDao, never()).update(any());
    }

    @Test
    public void StackShouldBeUpdated() throws NotFoundException, ServerException, ConflictException {
        final String updatedDescription = "some description";
        final String updatedScope = "advanced";
        StackDto updatedStackDto = DtoFactory.getInstance().createDto(StackDto.class)
                                             .withId(ID_TEST)
                                             .withName(NAME)
                                             .withDescription(updatedDescription)
                                             .withScope(updatedScope)
                                             .withCreator(CREATOR)
                                             .withTags(tags)
                                             .withSource(stackSourceDto)
                                             .withComponents(componentsDto);

        StackImpl updateStack = new StackImpl(stackImpl, stackImpl.getIcon());
        updateStack.setDescription(updatedDescription);
        updateStack.setScope(updatedScope);

        when(checker.hasAccess(any(StackImpl.class), anyString(), eq("write"))).thenReturn(true);
        when(stackDao.getById(ID_TEST)).thenReturn(stackImpl).thenReturn(updateStack);

        Response response = given().auth()
                                   .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
                                   .contentType(APPLICATION_JSON)
                                   .content(updatedStackDto)
                                   .when()
                                   .put(SECURE_PATH + "/stack");

        assertEquals(response.getStatusCode(), 200);
        StackDtoDescriptor result = unwrapDto(response, StackDtoDescriptor.class);

        assertEquals(result.getId(), updatedStackDto.getId());
        assertEquals(result.getName(), updatedStackDto.getName());
        assertEquals(result.getDescription(), updatedStackDto.getDescription());
        assertEquals(result.getScope(), updatedStackDto.getScope());
        assertEquals(result.getTags().get(0), updatedStackDto.getTags().get(0));
        assertEquals(result.getTags().get(1), updatedStackDto.getTags().get(1));
        assertEquals(result.getComponents().get(0).getName(), updatedStackDto.getComponents().get(0).getName());
        assertEquals(result.getComponents().get(0).getVersion(), updatedStackDto.getComponents().get(0).getVersion());
        assertEquals(result.getSource().getType(), updatedStackDto.getSource().getType());
        assertEquals(result.getSource().getOrigin(), updatedStackDto.getSource().getOrigin());
        assertEquals(result.getCreator(), updatedStackDto.getCreator());

        verify(stackDao).update(any());
        verify(stackDao, times(2)).getById(ID_TEST);
    }

    @Test
    public void creatorShouldNotBeUpdated() throws ServerException, NotFoundException {
        StackDto updatedStackDto = DtoFactory.getInstance().createDto(StackDto.class)
                                             .withId(ID_TEST)
                                             .withName(NAME)
                                             .withDescription(DESCRIPTION)
                                             .withScope(SCOPE)
                                             .withCreator("creator changed")
                                             .withTags(tags)
                                             .withSource(stackSourceDto)
                                             .withComponents(componentsDto);

        when(checker.hasAccess(any(StackImpl.class), anyString(), eq("write"))).thenReturn(true);
        when(stackDao.getById(ID_TEST)).thenReturn(stackImpl);

        Response response = given().auth()
                                   .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
                                   .contentType(APPLICATION_JSON)
                                   .content(updatedStackDto)
                                   .when()
                                   .put(SECURE_PATH + "/stack");

        assertEquals(response.getStatusCode(), 200);
        StackDtoDescriptor result = unwrapDto(response, StackDtoDescriptor.class);

        assertEquals(result.getId(), updatedStackDto.getId());
        assertEquals(result.getName(), updatedStackDto.getName());
        assertEquals(result.getDescription(), updatedStackDto.getDescription());
        assertEquals(result.getScope(), updatedStackDto.getScope());
        assertEquals(result.getTags().get(0), updatedStackDto.getTags().get(0));
        assertEquals(result.getTags().get(1), updatedStackDto.getTags().get(1));
        assertEquals(result.getComponents().get(0).getName(), updatedStackDto.getComponents().get(0).getName());
        assertEquals(result.getComponents().get(0).getVersion(), updatedStackDto.getComponents().get(0).getVersion());
        assertEquals(result.getSource().getType(), updatedStackDto.getSource().getType());
        assertEquals(result.getSource().getOrigin(), updatedStackDto.getSource().getOrigin());
        assertEquals(result.getCreator(), stackImpl.getCreator());

        verify(stackDao).update(any());
        verify(stackDao, times(2)).getById(ID_TEST);
    }

    @Test
    public void stackShouldBeDeleted() throws ServerException, NotFoundException {
        when(stackDao.getById(ID_TEST)).thenReturn(stackImpl);
        when(checker.hasAccess(any(StackImpl.class), anyString(), eq("write"))).thenReturn(true);

        Response response = given().auth()
                                   .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
                                   .when()
                                   .delete(SECURE_PATH + "/stack/" + ID_TEST);

        verify(stackDao).getById(ID_TEST);
        verify(stackDao).remove(any());
        assertEquals(response.getStatusCode(), 204);
    }

    @Test
    public void shouldThrowForbiddenExceptionWhenWeTryDeleteAlienStack() throws NotFoundException, ServerException {
        StackImpl stack = StackImpl.builder()
                                   .setId(ID_TEST)
                                   .setName(NAME)
                                   .setDescription(DESCRIPTION)
                                   .setScope(SCOPE)
                                   .setCreator("someUser")
                                   .setTags(tags)
                                   .setSource(stackSourceImpl)
                                   .setComponents(componentsImpl)
                                   .setIcon(stackIcon)
                                   .build();

        when(stackDao.getById(ID_TEST)).thenReturn(stack);

        Response response = given().auth()
                                   .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
                                   .when()
                                   .delete(SECURE_PATH + "/stack/" + ID_TEST);

        verify(stackDao).getById(ID_TEST);
        assertEquals(response.getStatusCode(), 403);
        assertEquals(unwrapDto(response, ServiceError.class).getMessage(), format("User %s doesn't have access to stack %s", USER_ID, ID_TEST));
    }

    @Test
    public void createdStacksShouldBeReturned() throws ServerException {
        when(stackDao.getByCreator(USER_ID, 0, 1)).thenReturn(Collections.singletonList(stackImpl));

        Response response = given().auth()
                                   .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
                                   .when()
                                   .get(SECURE_PATH + "/stack?skipCount=0&maxItems=1");

        assertEquals(response.getStatusCode(), 200);
        StackDtoDescriptor result = DtoFactory.getInstance()
                                              .createListDtoFromJson(response.body().print(), StackDtoDescriptor.class)
                                              .get(0);

        assertEquals(result.getId(), stackImpl.getId());
        assertEquals(result.getName(), stackImpl.getName());
        assertEquals(result.getDescription(), stackImpl.getDescription());
        assertEquals(result.getScope(), stackImpl.getScope());
        assertEquals(result.getTags().get(0), stackImpl.getTags().get(0));
        assertEquals(result.getTags().get(1), stackImpl.getTags().get(1));
        assertEquals(result.getComponents().get(0).getName(), stackImpl.getComponents().get(0).getName());
        assertEquals(result.getComponents().get(0).getVersion(), stackImpl.getComponents().get(0).getVersion());
        assertEquals(result.getSource().getType(), stackImpl.getSource().getType());
        assertEquals(result.getSource().getOrigin(), stackImpl.getSource().getOrigin());
        assertEquals(result.getCreator(), stackImpl.getCreator());

        verify(stackDao).getByCreator(USER_ID, 0, 1);
    }

    @Test
    public void shouldBeReturnedStackList() throws ServerException {
        when(stackDao.getByCreator(anyString(), anyInt(), anyInt())).thenReturn(Collections.singletonList(stackImpl));

        Response response = given().auth()
                                   .basic(ADMIN_USER_NAME, ADMIN_USER_PASSWORD)
                                   .when()
                                   .get(SECURE_PATH + "/stack/");

        verify(stackDao).getByCreator(anyString(), anyInt(), anyInt());
        assertEquals(response.getStatusCode(), 200);

        List<StackDtoDescriptor> result = unwrapListDto(response, StackDtoDescriptor.class);

        assertEquals(result.get(0).getId(), stackImpl.getId());
        assertEquals(result.get(0).getName(), stackImpl.getName());
        assertEquals(result.get(0).getDescription(), stackImpl.getDescription());
        assertEquals(result.get(0).getScope(), stackImpl.getScope());
        assertEquals(result.get(0).getTags().get(0), stackImpl.getTags().get(0));
        assertEquals(result.get(0).getTags().get(1), stackImpl.getTags().get(1));
        assertEquals(result.get(0).getComponents().get(0).getName(), stackImpl.getComponents().get(0).getName());
        assertEquals(result.get(0).getComponents().get(0).getVersion(), stackImpl.getComponents().get(0).getVersion());
        assertEquals(result.get(0).getSource().getType(), stackImpl.getSource().getType());
        assertEquals(result.get(0).getSource().getOrigin(), stackImpl.getSource().getOrigin());
        assertEquals(result.get(0).getCreator(), stackImpl.getCreator());

        verify(stackDao).getByCreator(anyString(), anyInt(), anyInt());
    }

    private static <T> T unwrapDto(Response response, Class<T> dtoClass) {
        return DtoFactory.getInstance().createDtoFromJson(response.body().print(), dtoClass);
    }

    private static <T> List<T> unwrapListDto(Response response, Class<T> dtoClass) {
        return DtoFactory.getInstance().createListDtoFromJson(response.body().print(), dtoClass);
    }

    @Filter
    public static class EnvironmentFilter implements RequestFilter {
        public void doFilter(GenericContainerRequest request) {
            EnvironmentContext.getCurrent().setUser(new UserImpl("user", USER_ID, "token", ROLES, false));
        }
    }
}
