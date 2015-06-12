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
package org.eclipse.che.api.workspace.server;

import sun.security.acl.PrincipalImpl;

import org.eclipse.che.api.account.server.dao.Account;
import org.eclipse.che.api.account.server.dao.AccountDao;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.rest.ApiExceptionMapper;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.core.rest.shared.dto.ServiceError;
import org.eclipse.che.api.user.server.dao.PreferenceDao;
import org.eclipse.che.api.user.server.dao.Profile;
import org.eclipse.che.api.user.server.dao.User;
import org.eclipse.che.api.user.server.dao.UserDao;
import org.eclipse.che.api.user.server.dao.UserProfileDao;
import org.eclipse.che.api.workspace.server.dao.Member;
import org.eclipse.che.api.workspace.server.dao.MemberDao;
import org.eclipse.che.api.workspace.server.dao.Workspace;
import org.eclipse.che.api.workspace.server.dao.WorkspaceDao;
import org.eclipse.che.api.workspace.shared.dto.MemberDescriptor;
import org.eclipse.che.api.workspace.shared.dto.NewMembership;
import org.eclipse.che.api.workspace.shared.dto.NewWorkspace;
import org.eclipse.che.api.workspace.shared.dto.WorkspaceDescriptor;
import org.eclipse.che.api.workspace.shared.dto.WorkspaceUpdate;
import org.eclipse.che.commons.json.JsonHelper;
import org.eclipse.che.dto.server.DtoFactory;
import org.everrest.core.impl.ApplicationContextImpl;
import org.everrest.core.impl.ApplicationProviderBinder;
import org.everrest.core.impl.ContainerRequest;
import org.everrest.core.impl.ContainerResponse;
import org.everrest.core.impl.EnvironmentContext;
import org.everrest.core.impl.EverrestConfiguration;
import org.everrest.core.impl.EverrestProcessor;
import org.everrest.core.impl.ResourceBinderImpl;
import org.everrest.core.tools.DependencySupplierImpl;
import org.everrest.core.tools.ResourceLauncher;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import javax.ws.rs.core.SecurityContext;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static javax.ws.rs.core.Response.Status;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static org.eclipse.che.api.project.server.Constants.LINK_REL_GET_PROJECTS;
import static org.eclipse.che.api.user.server.Constants.LINK_REL_GET_USER_BY_ID;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Tests for {@link WorkspaceService}
 *
 * @author Eugene Voevodin
 * @see WorkspaceService
 */
@Listeners(value = {MockitoTestNGListener.class})
public class WorkspaceServiceTest {

    private static final String BASE_URI     = "http://localhost/service";
    private static final String SERVICE_PATH = BASE_URI + "/workspace";

    @Mock
    WorkspaceDao       workspaceDao;
    @Mock
    UserDao            userDao;
    @Mock
    MemberDao          memberDao;
    @Mock
    AccountDao         accountDao;
    @Mock
    UserProfileDao     profileDao;
    @Mock
    PreferenceDao      preferenceDao;
    @Mock
    SecurityContext    securityContext;
    @Mock
    EnvironmentContext environmentContext;

    ResourceLauncher launcher;
    WorkspaceService service;
    User             testUser;

    @BeforeMethod
    public void before() throws Exception {
        //set up launcher
        final ResourceBinderImpl resources = new ResourceBinderImpl();
        resources.addResource(WorkspaceService.class, null);
        final DependencySupplierImpl dependencies = new DependencySupplierImpl();
        dependencies.addComponent(WorkspaceDao.class, workspaceDao);
        dependencies.addComponent(MemberDao.class, memberDao);
        dependencies.addComponent(UserDao.class, userDao);
        dependencies.addComponent(UserProfileDao.class, profileDao);
        dependencies.addComponent(AccountDao.class, accountDao);
        dependencies.addComponent(PreferenceDao.class, preferenceDao);
        final ApplicationProviderBinder binder = new ApplicationProviderBinder();
        binder.addExceptionMapper(ApiExceptionMapper.class);
        final URI uri = new URI(BASE_URI);
        final ContainerRequest req = new ContainerRequest(null, uri, uri, null, null, securityContext);
        final ApplicationContextImpl contextImpl = new ApplicationContextImpl(req, null, binder);
        contextImpl.setDependencySupplier(dependencies);
        ApplicationContextImpl.setCurrent(contextImpl);
        final EverrestProcessor processor = new EverrestProcessor(resources,
                                                                  binder,
                                                                  dependencies,
                                                                  new EverrestConfiguration(),
                                                                  null);
        launcher = new ResourceLauncher(processor);
        service = (WorkspaceService)resources.getMatchedResource("/workspace", new ArrayList<String>())
                                             .getInstance(ApplicationContextImpl.getCurrent());
        when(environmentContext.get(SecurityContext.class)).thenReturn(securityContext);
        //set up test user
        final String userId = "test_user_id";
        final String userEmail = "test@test.com";
        testUser = new User().withId(userId).withEmail(userEmail);
        when(userDao.getById(userId)).thenReturn(testUser);
        when(userDao.getByAlias(userEmail)).thenReturn(testUser);
        org.eclipse.che.commons.env.EnvironmentContext.getCurrent().setUser(new org.eclipse.che.commons.user.User() {

            @Override
            public String getName() {
                return testUser.getEmail();
            }

            @Override
            public boolean isMemberOf(String s) {
                return false;
            }

            @Override
            public String getToken() {
                return null;
            }

            @Override
            public String getId() {
                return testUser.getId();
            }

            @Override
            public boolean isTemporary() {
                return false;
            }
        });
        when(securityContext.getUserPrincipal()).thenReturn(new PrincipalImpl(testUser.getEmail()));
    }

    @Test
    public void shouldBeAbleToCreateFirstWorkspace() throws Exception {
        //create new account with empty workspace list
        final Account testAccount = createAccount();
        when(workspaceDao.getByAccount(testAccount.getId())).thenReturn(Collections.<Workspace>emptyList());
        //new workspace descriptor
        final NewWorkspace newWorkspace = newDTO(NewWorkspace.class).withName("new_workspace")
                                                                    .withAccountId(testAccount.getId());

        final WorkspaceDescriptor descriptor = doPost(SERVICE_PATH, newWorkspace, CREATED);

        assertEquals(descriptor.getName(), newWorkspace.getName());
        assertEquals(descriptor.getAccountId(), newWorkspace.getAccountId());
        assertTrue(descriptor.getAttributes().isEmpty());
        assertFalse(descriptor.isTemporary());
        verify(workspaceDao).create(any(Workspace.class));
    }

    @Test
    public void workspaceNameShouldBeGeneratedIfNewWorkspaceDoesNotContainsIt() throws Exception {
        //create new account with empty workspace list
        final Account testAccount = createAccount();
        when(workspaceDao.getByAccount(testAccount.getId())).thenReturn(Collections.<Workspace>emptyList());

        final String email = testUser.getEmail();
        final String expectedWsName = email.substring(0, email.indexOf('@'));
        when(workspaceDao.getByName(expectedWsName)).thenThrow(new NotFoundException("not found"));

        final NewWorkspace newWorkspace = newDTO(NewWorkspace.class).withAccountId(testAccount.getId());

        final WorkspaceDescriptor descriptor = doPost(SERVICE_PATH, newWorkspace, CREATED);

        assertEquals(descriptor.getName(), expectedWsName);
    }

    @Test
    public void workspaceNameShouldBeGeneratedUntilNotReservedIsFound() throws Exception {
        //create new account with empty workspace list
        final Account testAccount = createAccount();
        when(workspaceDao.getByAccount(testAccount.getId())).thenReturn(Collections.<Workspace>emptyList());

        final String email = testUser.getEmail();
        final String expectedWsName = email.substring(0, email.indexOf('@')) + "11";
        when(workspaceDao.getByName(expectedWsName)).thenThrow(new NotFoundException("not found"));

        final NewWorkspace newWorkspace = newDTO(NewWorkspace.class).withAccountId(testAccount.getId());

        final WorkspaceDescriptor descriptor = doPost(SERVICE_PATH, newWorkspace, CREATED);

        assertEquals(descriptor.getName(), expectedWsName);
    }

    @Test
    public void shouldNotBeAbleToCreateWorkspaceAssociatedWithOtherAccount() throws Exception {
        //new workspace descriptor
        final NewWorkspace newWorkspace = newDTO(NewWorkspace.class).withName("new_workspace")
                                                                    .withAccountId("fake_account_id");

        when(accountDao.getByOwner(testUser.getId())).thenReturn(new ArrayList<Account>());

        final String errorJson = doPost(SERVICE_PATH, newWorkspace, CONFLICT);

        assertEquals(asError(errorJson).getMessage(), "You can create workspace associated only with your own account");
    }

    @Test
    public void shouldBeAbleToCreateMultiWorkspaces() throws Exception {
        final Account testAccount = createAccount();
        when(workspaceDao.getByAccount(testAccount.getId())).thenReturn(singletonList(new Workspace()));
        //new workspace descriptor
        final NewWorkspace newWorkspace = newDTO(NewWorkspace.class).withName("test_workspace")
                                                                    .withAccountId(testAccount.getId());

        final WorkspaceDescriptor descriptor = doPost(SERVICE_PATH, newWorkspace, CREATED);

        assertEquals(descriptor.getName(), newWorkspace.getName());
        assertEquals(descriptor.getAccountId(), newWorkspace.getAccountId());
        verify(workspaceDao).create(any(Workspace.class));
    }

    @Test
    public void shouldNotBeAbleToCreateNewWorkspaceWithNotValidAttribute() throws Exception {
        final NewWorkspace newWorkspace = newDTO(NewWorkspace.class).withName("new_workspace")
                                                                    .withAccountId("fake_account")
                                                                    .withAttributes(singletonMap("codenvy:god_mode", "true"));

        final String errorJson = doPost(SERVICE_PATH, newWorkspace, CONFLICT);

        assertEquals(asError(errorJson).getMessage(), "Attribute2 name 'codenvy:god_mode' is not valid");
    }

    @Test
    public void shouldBeAbleToCreateNewTemporaryWorkspace() throws Exception {
        final NewWorkspace newWorkspace = newDTO(NewWorkspace.class).withName("new_workspace")
                                                                    .withAccountId("fake_account_id");

        createAccount();

        final WorkspaceDescriptor descriptor = doPost(SERVICE_PATH + "/temp", newWorkspace, CREATED);
        assertTrue(descriptor.isTemporary());
        assertEquals(descriptor.getName(), newWorkspace.getName());
        assertEquals(descriptor.getAccountId(), newWorkspace.getAccountId());
        verify(userDao, never()).create(any(User.class));
        verify(profileDao, never()).create(any(Profile.class));
        verify(workspaceDao).create(any(Workspace.class));
    }

    @Test
    public void shouldBeAbleToCreateNewTemporaryWorkspaceWhenUserDoesNotExist() throws Exception {
        when(securityContext.getUserPrincipal()).thenReturn(null);
        final NewWorkspace newWorkspace = newDTO(NewWorkspace.class).withName("new_workspace")
                                                                    .withAccountId("fake_account");

        final WorkspaceDescriptor descriptor = doPost(SERVICE_PATH + "/temp", newWorkspace, CREATED);

        assertTrue(descriptor.isTemporary());
        assertEquals(descriptor.getName(), newWorkspace.getName());
        assertEquals(descriptor.getAccountId(), newWorkspace.getAccountId());
        verify(userDao).create(any(User.class));
        verify(profileDao).create(any(Profile.class));
        verify(workspaceDao).create(any(Workspace.class));
    }

    @Test
    public void shouldNotBeAbleToCreateTemporaryWorkspaceWithNotValidAttribute() throws Exception {
        final Workspace testWorkspace = createWorkspace();
        testWorkspace.getAttributes().put("codenvy:god_mode", "true");

        final String errorJson = doPost(SERVICE_PATH + "/temp", testWorkspace, CONFLICT);

        assertEquals(asError(errorJson).getMessage(), "Attribute2 name 'codenvy:god_mode' is not valid");
    }

    @Test
    public void shouldBeAbleToGetWorkspaceByIdWithAttributesForWorkspaceAdmin() throws Exception {
        final Workspace testWorkspace = createWorkspace();
        final Map<String, String> actualAttributes = new HashMap<>(testWorkspace.getAttributes());

        prepareRole("workspace/admin");
        final WorkspaceDescriptor descriptor = doGet(SERVICE_PATH + "/" + testWorkspace.getId());

        assertEquals(descriptor.getAttributes(), actualAttributes);
    }

    @Test
    public void shouldBeAbleToGetWorkspaceByIdWithAttributesForWorkspaceDeveloper() throws Exception {
        final Workspace testWorkspace = createWorkspace();
        final Map<String, String> actualAttributes = new HashMap<>(testWorkspace.getAttributes());

        prepareRole("workspace/developer");
        final WorkspaceDescriptor descriptor = doGet(SERVICE_PATH + "/" + testWorkspace.getId());

        assertEquals(descriptor.getAttributes(), actualAttributes);
    }

    @Test
    public void shouldBeAbleToGetWorkspaceByIdWithAttributesForAccountOwner() throws Exception {
        final Workspace testWorkspace = createWorkspace();
        final Map<String, String> actualAttributes = new HashMap<>(testWorkspace.getAttributes());

        prepareRole("account/owner");
        final WorkspaceDescriptor descriptor = doGet(SERVICE_PATH + "/" + testWorkspace.getId());

        assertEquals(descriptor.getAttributes(), actualAttributes);
    }

    @Test
    public void shouldBeAbleToGetWorkspaceByIdWithEmptyAttributesForAnyone() throws Exception {
        final Workspace testWorkspace = createWorkspace();

        final WorkspaceDescriptor descriptor = doGet(SERVICE_PATH + "/" + testWorkspace.getId());

        assertEquals(descriptor.getId(), testWorkspace.getId());
        assertEquals(descriptor.getName(), testWorkspace.getName());
        assertEquals(descriptor.isTemporary(), testWorkspace.isTemporary());
        assertEquals(descriptor.getAccountId(), testWorkspace.getAccountId());
        assertTrue(descriptor.getAttributes().isEmpty());
    }

    @Test
    public void shouldBeAbleToGetWorkspaceByIdWithAttributeAllowAnyoneAddMember() throws Exception {
        final Workspace testWorkspace = createWorkspace();
        testWorkspace.getAttributes().put("test2_attribute", "test");
        testWorkspace.getAttributes().put("allowAnyoneAddMember", "true");

        final WorkspaceDescriptor descriptor = doGet(SERVICE_PATH + "/" + testWorkspace.getId());

        assertEquals(descriptor.getId(), testWorkspace.getId());
        assertEquals(descriptor.getName(), testWorkspace.getName());
        assertEquals(descriptor.isTemporary(), testWorkspace.isTemporary());
        assertEquals(descriptor.getAccountId(), testWorkspace.getAccountId());
        assertEquals(descriptor.getAttributes().size(), 1);
        assertEquals(descriptor.getAttributes().get("allowAnyoneAddMember"), "true");
    }

    @Test
    public void shouldBeAbleToGetWorkspaceByNameForWorkspaceAdmin() throws Exception {
        final Workspace testWorkspace = createWorkspace();
        final Map<String, String> actualAttributes = new HashMap<>(testWorkspace.getAttributes());
        prepareRole("workspace/admin");

        final WorkspaceDescriptor descriptor = doGet(SERVICE_PATH + "?name=" + testWorkspace.getName());

        assertEquals(descriptor.getAttributes(), actualAttributes);
    }

    @Test
    public void shouldBeAbleToGetWorkspaceByNameForWorkspaceDeveloper() throws Exception {
        final Workspace testWorkspace = createWorkspace();
        final Map<String, String> actualAttributes = new HashMap<>(testWorkspace.getAttributes());
        prepareRole("workspace/developer");

        final WorkspaceDescriptor descriptor = doGet(SERVICE_PATH + "?name=" + testWorkspace.getName());

        assertEquals(descriptor.getAttributes(), actualAttributes);
    }

    @Test
    public void shouldBeAbleToGetWorkspaceByNameForWorkspaceAccountOwner() throws Exception {
        final Workspace testWorkspace = createWorkspace();
        final Map<String, String> actualAttributes = new HashMap<>(testWorkspace.getAttributes());
        prepareRole("account/owner");

        final WorkspaceDescriptor descriptor = doGet(SERVICE_PATH + "?name=" + testWorkspace.getName());

        assertEquals(descriptor.getAttributes(), actualAttributes);
    }

    @Test
    public void shouldBeAbleToGetWorkspaceByNameWithEmptyAttributesForAnyone() throws Exception {
        final Workspace testWorkspace = createWorkspace();

        final WorkspaceDescriptor descriptor = doGet(SERVICE_PATH + "/" + testWorkspace.getId());

        assertEquals(descriptor.getId(), testWorkspace.getId());
        assertEquals(descriptor.getName(), testWorkspace.getName());
        assertEquals(descriptor.isTemporary(), testWorkspace.isTemporary());
        assertEquals(descriptor.getAccountId(), testWorkspace.getAccountId());
        assertTrue(descriptor.getAttributes().isEmpty());
    }

    @Test
    public void shouldBeAbleToGetWorkspaceByNameWithAttributeAllowAnyoneAddMember() throws Exception {
        final Workspace testWorkspace = createWorkspace();
        testWorkspace.getAttributes().put("test2_attribute", "test");
        testWorkspace.getAttributes().put("allowAnyoneAddMember", "true");

        final WorkspaceDescriptor descriptor = doGet(SERVICE_PATH + "/" + testWorkspace.getId());

        assertEquals(descriptor.getId(), testWorkspace.getId());
        assertEquals(descriptor.getName(), testWorkspace.getName());
        assertEquals(descriptor.isTemporary(), testWorkspace.isTemporary());
        assertEquals(descriptor.getAccountId(), testWorkspace.getAccountId());
        assertEquals(descriptor.getAttributes().size(), 1);
        assertEquals(descriptor.getAttributes().get("allowAnyoneAddMember"), "true");
    }

    @Test
    public void shouldNotBeAbleToRemoveAttributeIfAttributeNameStartsWithCodenvy() throws Exception {
        final Workspace testWorkspace = createWorkspace();

        final String errorJson = doDelete(SERVICE_PATH + "/" + testWorkspace.getId() + "/attribute?name=codenvy:runner_ram", CONFLICT);

        assertEquals(asError(errorJson).getMessage(), "Attribute2 name 'codenvy:runner_ram' is not valid");
    }

    @Test
    public void shouldBeAbleToRemoveAttribute() throws Exception {
        final Workspace testWorkspace = createWorkspace().withAttributes(new HashMap<>(singletonMap("test", "test")));

        doDelete(SERVICE_PATH + "/" + testWorkspace.getId() + "/attribute?name=test", NO_CONTENT);

        verify(workspaceDao).update(testWorkspace);
        assertTrue(testWorkspace.getAttributes().isEmpty());
    }

    @Test
    public void shouldBeAbleToUpdateWorkspace() throws Exception {
        final Workspace testWorkspace = createWorkspace().withAttributes(new HashMap<>(singletonMap("test", "test")));
        final String newName = "new_workspace";
        //workspace update descriptor
        final WorkspaceUpdate update = newDTO(WorkspaceUpdate.class).withName(newName)
                                                                    .withAttributes(singletonMap("test", "other_value"));

        final WorkspaceDescriptor descriptor = doPost(SERVICE_PATH + "/" + testWorkspace.getId(), update, OK);

        assertEquals(descriptor.getName(), newName);
        assertEquals(descriptor.getAttributes().size(), 1);
        assertEquals(descriptor.getAttributes().get("test"), "other_value");
        verify(workspaceDao).update(testWorkspace);
    }

    @Test
    public void shouldNotBeAbleToUpdateWorkspaceIfUpdateContainsNotValidAttribute() throws Exception {
        final Workspace testWorkspace = createWorkspace();
        final String newName = "new_workspace";
        //ensure workspace with update workspace name doesn't exist
        when(workspaceDao.getByName(newName)).thenThrow(new NotFoundException("workspace doesn't exist"));
        //workspace update descriptor
        final WorkspaceUpdate update = newDTO(WorkspaceUpdate.class).withName(newName)
                                                                    .withAttributes(singletonMap("codenvy:runner_ram", "64GB"));

        final String errorJson = doPost(SERVICE_PATH + "/" + testWorkspace.getId(), update, CONFLICT);

        assertEquals(asError(errorJson).getMessage(), "Attribute2 name 'codenvy:runner_ram' is not valid");
    }

    @Test
    public void shouldBeAbleToGetWorkspaceMembers() throws Exception {
        final Workspace testWorkspace = createWorkspace();
        final Member testMember = new Member().withWorkspaceId(testWorkspace.getId())
                                              .withUserId(testUser.getId())
                                              .withRoles(singletonList("workspace/admin"));
        final List<Member> members = singletonList(testMember);
        when(memberDao.getWorkspaceMembers(testWorkspace.getId())).thenReturn(members);

        final List<MemberDescriptor> descriptors = doGet(SERVICE_PATH + "/" + testWorkspace.getId() + "/members");

        assertEquals(descriptors.size(), 1);
        final MemberDescriptor descriptor = descriptors.get(0);
        assertEquals(descriptor.getUserId(), testMember.getUserId());
        assertEquals(descriptor.getWorkspaceReference().getId(), testMember.getWorkspaceId());
        assertEquals(descriptor.getRoles(), testMember.getRoles());
    }

    @Test
    public void shouldBeAbleToGetWorkspaceMember() throws Exception {
        final Workspace testWorkspace = createWorkspace();
        final Member testMember = new Member().withWorkspaceId(testWorkspace.getId())
                                              .withUserId(testUser.getId())
                                              .withRoles(singletonList("workspace/admin"));
        when(memberDao.getWorkspaceMember(testMember.getWorkspaceId(), testMember.getUserId())).thenReturn(testMember);

        final MemberDescriptor descriptor = doGet(SERVICE_PATH + "/" + testMember.getWorkspaceId() + "/membership");

        assertEquals(descriptor.getUserId(), testMember.getUserId());
        assertEquals(descriptor.getWorkspaceReference().getId(), testMember.getWorkspaceId());
        assertEquals(descriptor.getRoles(), testMember.getRoles());
    }

    @Test
    public void shouldBeAbleToAddWorkspaceMemberToNotEmptyWorkspaceForWorkspaceAdmin() throws Exception {
        final Workspace testWorkspace = createWorkspace();
        when(memberDao.getWorkspaceMembers(testWorkspace.getId())).thenReturn(singletonList(new Member()));
        final NewMembership membership = newDTO(NewMembership.class).withRoles(singletonList("workspace/developer"))
                                                                    .withUserId("test_user_id");
        prepareRole("workspace/admin");

        final MemberDescriptor descriptor = doPost(SERVICE_PATH + "/" + testWorkspace.getId() + "/members", membership, CREATED);

        assertEquals(descriptor.getUserId(), membership.getUserId());
        assertEquals(descriptor.getWorkspaceReference().getId(), testWorkspace.getId());
        assertEquals(descriptor.getRoles(), membership.getRoles());
        verify(memberDao).create(any(Member.class));
    }

    @Test
    public void shouldNotBeAbleToAddMemberToNotEmptyWorkspaceIfUserIsNotWorkspaceAdmin() throws Exception {
        final Workspace testWorkspace = createWorkspace();
        when(memberDao.getWorkspaceMembers(testWorkspace.getId())).thenReturn(singletonList(new Member()));
        final NewMembership membership = newDTO(NewMembership.class).withRoles(singletonList("workspace/developer"))
                                                                    .withUserId(testUser.getId());
        prepareRole("workspace/developer");

        final String errorJson = doPost(SERVICE_PATH + "/" + testWorkspace.getId() + "/members", membership, FORBIDDEN);

        assertEquals(asError(errorJson).getMessage(), "Access denied");
    }

    @Test
    public void shouldBeAbleToAddMemberToNotEmptyWorkspaceIfUserIsAccountOwner() throws Exception {
        final Workspace testWorkspace = createWorkspace();
        final Account account = createAccount();
        when(workspaceDao.getByAccount(account.getId())).thenReturn(singletonList(testWorkspace));
        when(memberDao.getWorkspaceMembers(testWorkspace.getId())).thenReturn(singletonList(new Member()));

        final NewMembership membership = newDTO(NewMembership.class).withRoles(singletonList("workspace/developer"))
                                                                    .withUserId(testUser.getId());
        prepareRole("account/owner");

        final MemberDescriptor descriptor = doPost(SERVICE_PATH + "/" + testWorkspace.getId() + "/members", membership, CREATED);

        assertEquals(descriptor.getUserId(), membership.getUserId());
        assertEquals(descriptor.getWorkspaceReference().getId(), testWorkspace.getId());
        assertEquals(descriptor.getRoles(), membership.getRoles());
        verify(memberDao).create(any(Member.class));
    }

    @Test
    public void shouldBeAbleToAddMemberToNotEmptyWorkspaceForAnyUserIfAllowAttributeIsTrue() throws Exception {
        final Workspace testWorkspace = createWorkspace();
        when(memberDao.getWorkspaceMembers(testWorkspace.getId())).thenReturn(singletonList(new Member()));
        testWorkspace.getAttributes().put("allowAnyoneAddMember", "true");
        final NewMembership membership = newDTO(NewMembership.class).withRoles(singletonList("workspace/developer"))
                                                                    .withUserId(testUser.getId());

        final MemberDescriptor descriptor = doPost(SERVICE_PATH + "/" + testWorkspace.getId() + "/members", membership, CREATED);

        assertEquals(descriptor.getRoles(), membership.getRoles());
        assertEquals(descriptor.getUserId(), membership.getUserId());
        assertEquals(descriptor.getWorkspaceReference().getId(), testWorkspace.getId());
        verify(memberDao).create(any(Member.class));
    }

    @Test
    public void shouldNotBeAbleToAddMemberToNotEmptyWorkspaceForAnyUserIfAllowAttributeIsFalse() throws Exception {
        final Workspace testWorkspace = createWorkspace();
        when(memberDao.getWorkspaceMembers(testWorkspace.getId())).thenReturn(singletonList(any(Member.class)));
        testWorkspace.getAttributes().put("allowAnyoneAddMember", "false");
        final NewMembership membership = newDTO(NewMembership.class).withRoles(singletonList("workspace/developer"))
                                                                    .withUserId(testUser.getId());

        final String errorJson = doPost(SERVICE_PATH + "/" + testWorkspace.getId() + "/members", membership, FORBIDDEN);

        assertEquals(asError(errorJson).getMessage(), "Access denied");
    }

    @Test
    public void shouldNotBeAbleToAddMemberToNotEmptyWorkspaceForAnyUserIfAllowAttributeMissed() throws Exception {
        final Workspace testWorkspace = createWorkspace();
        when(memberDao.getWorkspaceMembers(testWorkspace.getId())).thenReturn(singletonList(new Member()));
        final NewMembership membership = newDTO(NewMembership.class).withRoles(singletonList("workspace/developer"))
                                                                    .withUserId(testUser.getId());

        final String errorJson = doPost(SERVICE_PATH + "/" + testWorkspace.getId() + "/members", membership, FORBIDDEN);

        assertEquals(asError(errorJson).getMessage(), "Access denied");
    }

    @Test
    public void shouldBeAbleToAddMemberToEmptyWorkspaceForAnyUser() throws Exception {
        final Workspace testWorkspace = createWorkspace();
        final NewMembership membership = newDTO(NewMembership.class).withUserId(testUser.getId());

        final MemberDescriptor descriptor = doPost(SERVICE_PATH + "/" + testWorkspace.getId() + "/members", membership, CREATED);

        assertEquals(descriptor.getUserId(), membership.getUserId());
        assertEquals(descriptor.getWorkspaceReference().getId(), testWorkspace.getId());
        assertEquals(new HashSet<>(descriptor.getRoles()), new HashSet<>(asList("workspace/admin", "workspace/developer")));
    }

    @Test
    public void shouldNotBeAbleToAddNewWorkspaceMemberWithoutOfRolesToNotEmptyWorkspace() throws Exception {
        final Workspace testWorkspace = createWorkspace();
        when(memberDao.getWorkspaceMembers(testWorkspace.getId())).thenReturn(singletonList(new Member()));
        final NewMembership membership = newDTO(NewMembership.class).withUserId(testUser.getId());
        prepareRole("workspace/admin");

        final String errorJson = doPost(SERVICE_PATH + "/" + testWorkspace.getId() + "/members", membership, CONFLICT);

        assertEquals(asError(errorJson).getMessage(), "Roles should not be empty");
    }

    @Test
    public void shouldBeAbleToRemoveWorkspaceMember() throws Exception {
        final Workspace testWorkspace = createWorkspace();

        doDelete(SERVICE_PATH + "/" + testWorkspace.getId() + "/members/" + testUser.getId(), NO_CONTENT);

        verify(memberDao).remove(any(Member.class));
    }

    @Test
    public void shouldBeAbleToRemoveWorkspace() throws Exception {
        final Workspace testWorkspace = createWorkspace();

        doDelete(SERVICE_PATH + "/" + testWorkspace.getId(), NO_CONTENT);

        verify(workspaceDao).remove(testWorkspace.getId());
    }

    @Test
    public void testWorkspaceDescriptorLinksForUser() throws NotFoundException, ServerException {
        final Workspace testWorkspace = createWorkspace();
        when(securityContext.isUserInRole("user")).thenReturn(true);

        final Set<String> expectedRels = new HashSet<>(asList(Constants.LINK_REL_GET_CURRENT_USER_WORKSPACES,
                                                              Constants.LINK_REL_GET_CURRENT_USER_MEMBERSHIP,
                                                              LINK_REL_GET_PROJECTS));

        assertEquals(asRels(service.toDescriptor(testWorkspace, securityContext).getLinks()), expectedRels);
    }

    @Test
    public void testWorkspaceDescriptorLinksForWorkspaceDeveloper() throws NotFoundException, ServerException {
        final Workspace testWorkspace = createWorkspace();
        prepareRole("workspace/developer");

        final Set<String> expectedRels = new HashSet<>(asList(Constants.LINK_REL_GET_WORKSPACE_BY_NAME,
                                                              Constants.LINK_REL_GET_WORKSPACE_BY_ID,
                                                              Constants.LINK_REL_GET_WORKSPACE_MEMBERS,
                                                              Constants.LINK_REL_GET_CURRENT_USER_WORKSPACES,
                                                              Constants.LINK_REL_GET_CURRENT_USER_MEMBERSHIP,
                                                              LINK_REL_GET_PROJECTS));

        assertEquals(asRels(service.toDescriptor(testWorkspace, securityContext).getLinks()), expectedRels);
    }

    @Test
    public void testWorkspaceDescriptorLinksForWorkspaceAdmin() throws NotFoundException, ServerException {
        final Workspace testWorkspace = createWorkspace();
        prepareRole("workspace/admin");

        final Set<String> expectedRels = new HashSet<>(asList(Constants.LINK_REL_GET_WORKSPACE_BY_NAME,
                                                              Constants.LINK_REL_GET_WORKSPACE_BY_ID,
                                                              Constants.LINK_REL_GET_WORKSPACE_MEMBERS,
                                                              Constants.LINK_REL_REMOVE_WORKSPACE,
                                                              Constants.LINK_REL_GET_CURRENT_USER_WORKSPACES,
                                                              Constants.LINK_REL_GET_CURRENT_USER_MEMBERSHIP,
                                                              LINK_REL_GET_PROJECTS));

        assertEquals(asRels(service.toDescriptor(testWorkspace, securityContext).getLinks()), expectedRels);
    }

    @Test
    public void testWorkspaceDescriptorLinksForSystemManager() throws NotFoundException, ServerException {
        final Workspace testWorkspace = createWorkspace();
        prepareRole("system/manager");

        final Set<String> expectedRels = new HashSet<>(asList(Constants.LINK_REL_GET_WORKSPACE_BY_NAME,
                                                              Constants.LINK_REL_GET_WORKSPACE_BY_ID,
                                                              Constants.LINK_REL_GET_WORKSPACE_MEMBERS));

        assertEquals(asRels(service.toDescriptor(testWorkspace, securityContext).getLinks()), expectedRels);
    }

    @Test
    public void testWorkspaceDescriptorLinksForSystemAdmin() throws NotFoundException, ServerException {
        final Workspace testWorkspace = createWorkspace();
        prepareRole("system/admin");

        final Set<String> expectedRels = new HashSet<>(asList(Constants.LINK_REL_GET_WORKSPACE_BY_NAME,
                                                              Constants.LINK_REL_GET_WORKSPACE_BY_ID,
                                                              Constants.LINK_REL_GET_WORKSPACE_MEMBERS,
                                                              Constants.LINK_REL_REMOVE_WORKSPACE));

        assertEquals(asRels(service.toDescriptor(testWorkspace, securityContext).getLinks()), expectedRels);
    }

    @Test
    public void testMemberDescriptorLinksForWorkspaceDeveloper() throws NotFoundException, ServerException {
        final Workspace testWorkspace = createWorkspace();
        final Member testMember = new Member().withUserId(testUser.getId())
                                              .withWorkspaceId(testWorkspace.getId());
        prepareRole("workspace/developer");

        final Set<String> expectedRels = new HashSet<>(asList(Constants.LINK_REL_GET_WORKSPACE_MEMBERS,
                                                              LINK_REL_GET_USER_BY_ID));

        assertEquals(asRels(service.toDescriptor(testMember, testWorkspace, securityContext).getLinks()), expectedRels);
    }

    @Test
    public void testMemberDescriptorLinksForWorkspaceAdmin() throws NotFoundException, ServerException {
        final Workspace testWorkspace = createWorkspace();
        final Member testMember = new Member().withUserId(testUser.getId())
                                              .withWorkspaceId(testWorkspace.getId());
        prepareRole("workspace/admin");

        final Set<String> expectedRels = new HashSet<>(asList(Constants.LINK_REL_GET_WORKSPACE_MEMBERS,
                                                              LINK_REL_GET_USER_BY_ID,
                                                              Constants.LINK_REL_REMOVE_WORKSPACE_MEMBER));

        assertEquals(asRels(service.toDescriptor(testMember, testWorkspace, securityContext).getLinks()), expectedRels);
    }

    @SuppressWarnings("unchecked")
    private <T> T doDelete(String path, Status expectedResponseStatus) throws Exception {
        final ContainerResponse response = launcher.service("DELETE", path, BASE_URI, null, null, null, environmentContext);
        assertEquals(response.getStatus(), expectedResponseStatus.getStatusCode());
        return (T)response.getEntity();
    }

    @SuppressWarnings("unchecked")
    private <T> T doGet(String path) throws Exception {
        final ContainerResponse response = launcher.service("GET", path, BASE_URI, null, null, null, environmentContext);
        assertEquals(response.getStatus(), OK.getStatusCode());
        return (T)response.getEntity();
    }

    @SuppressWarnings("unchecked")
    private <T> T doPost(String path, Object entity, Status expectedResponseStatus) throws Exception {
        final byte[] data = JsonHelper.toJson(entity).getBytes();
        final Map<String, List<String>> headers = new HashMap<>(4);
        headers.put("Content-Type", singletonList("application/json"));
        final ContainerResponse response = launcher.service("POST", path, BASE_URI, headers, data, null, environmentContext);
        assertEquals(response.getStatus(), expectedResponseStatus.getStatusCode());
        return (T)response.getEntity();
    }

    private ServiceError asError(String json) {
        return DtoFactory.getInstance().createDtoFromJson(json, ServiceError.class);
    }

    private <T> T newDTO(Class<T> dto) {
        return DtoFactory.getInstance().createDto(dto);
    }

    private Set<String> asRels(List<Link> links) {
        final Set<String> rels = new HashSet<>();
        for (Link link : links) {
            rels.add(link.getRel());
        }
        return rels;
    }

    private Account createAccount() throws NotFoundException, ServerException {
        final Account account = new Account().withId("fake_account_id");
        when(accountDao.getById(account.getId())).thenReturn(account);
        when(accountDao.getByOwner(testUser.getId())).thenReturn(singletonList(account));
        return account;
    }

    private Workspace createWorkspace() throws NotFoundException, ServerException {
        final String workspaceId = "test_workspace_id";
        final String workspaceName = "test_workspace_name";
        final String accountId = "test_account_id";
        final Map<String, String> attributes = new HashMap<>();
        attributes.put("default_attribute", "default_value");
        final Workspace testWorkspace = new Workspace().withId(workspaceId)
                                                       .withName(workspaceName)
                                                       .withTemporary(false)
                                                       .withAccountId(accountId)
                                                       .withAttributes(attributes);
        when(workspaceDao.getById(workspaceId)).thenReturn(testWorkspace);
        when(workspaceDao.getByName(workspaceName)).thenReturn(testWorkspace);
        when(workspaceDao.getByAccount(accountId)).thenReturn(singletonList(testWorkspace));
        return testWorkspace;
    }

    private Workspace createExtraWorkspace() throws NotFoundException, ServerException {
        final String workspaceId = "extra_test_workspace_id";
        final String workspaceName = "extra_test_workspace_name";
        final String accountId = "test_account_id";
        final Map<String, String> attributes = new HashMap<>();
        attributes.put("codenvy:role", "extra");
        final Workspace extraWorkspace = new Workspace().withId(workspaceId)
                                                        .withName(workspaceName)
                                                        .withTemporary(false)
                                                        .withAccountId(accountId)
                                                        .withAttributes(attributes);
        when(workspaceDao.getById(workspaceId)).thenReturn(extraWorkspace);
        when(workspaceDao.getByName(workspaceName)).thenReturn(extraWorkspace);
        return extraWorkspace;
    }

    private void prepareRole(String role) {
        when(securityContext.isUserInRole(anyString())).thenReturn(false);
        if (!role.equals("system/admin") && !role.equals("system/manager")) {
            when(securityContext.isUserInRole("user")).thenReturn(true);
        }
        when(securityContext.isUserInRole(role)).thenReturn(true);
    }
}
