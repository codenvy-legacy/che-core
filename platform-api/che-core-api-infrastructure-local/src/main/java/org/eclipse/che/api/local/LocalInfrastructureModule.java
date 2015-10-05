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
package org.eclipse.che.api.local;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import org.eclipse.che.api.account.server.dao.Account;
import org.eclipse.che.api.account.server.dao.AccountDao;
import org.eclipse.che.api.auth.AuthenticationDao;
import org.eclipse.che.api.local.storage.LocalStorageFactory;
import org.eclipse.che.api.machine.server.dao.RecipeDao;
import org.eclipse.che.api.machine.server.recipe.GroupImpl;
import org.eclipse.che.api.machine.server.recipe.PermissionsImpl;
import org.eclipse.che.api.machine.server.recipe.RecipeImpl;
import org.eclipse.che.api.machine.shared.Group;
import org.eclipse.che.api.machine.shared.ManagedCommand;
import org.eclipse.che.api.machine.shared.ManagedRecipe;
import org.eclipse.che.api.user.server.TokenValidator;
import org.eclipse.che.api.user.server.dao.PreferenceDao;
import org.eclipse.che.api.user.server.dao.User;
import org.eclipse.che.api.user.server.dao.UserDao;
import org.eclipse.che.api.user.server.dao.UserProfileDao;
//import org.eclipse.che.api.vfs.server.VirtualFileSystemRegistry;
//import org.eclipse.che.api.workspace.server.dao.Member;
//import org.eclipse.che.api.workspace.server.dao.MemberDao;
//import org.eclipse.che.api.workspace.server.dao.Workspace;
//import org.eclipse.che.api.workspace.server.dao.WorkspaceDao;
import org.eclipse.che.api.workspace.server.spi.WorkspaceDao;
import org.eclipse.che.inject.DynaModule;

import javax.inject.Named;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableSet;

@DynaModule
public class LocalInfrastructureModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(UserDao.class).to(LocalUserDaoImpl.class);
        bind(WorkspaceDao.class).to(LocalWorkspaceDaoImpl.class);
        bind(UserProfileDao.class).to(LocalProfileDaoImpl.class);
        bind(PreferenceDao.class).to(LocalPreferenceDaoImpl.class);
//        bind(MemberDao.class).to(LocalMemberDaoImpl.class);
        bind(AuthenticationDao.class).to(LocalAuthenticationDaoImpl.class);
//        bind(FactoryStore.class).to(InMemoryFactoryStore.class);
        bind(TokenValidator.class).to(DummyTokenValidator.class);
        bind(RecipeDao.class).to(LocalRecipeDaoImpl.class);
        bind(LocalStorageFactory.class);
    }


    // ~~~ WorkspaceDao
/*
    @Provides
    @Named("codenvy.local.infrastructure.workspaces")
    Set<Workspace> workspaces() {
        final Set<Workspace> workspaces = new HashSet<>(1);
        workspaces.add(new Workspace().withId("1q2w3e").withName("default").withTemporary(false));
        return workspaces;
    }

    // WorkspaceDao ~~~


    // ~~~ MemberDao

    @Provides
    @Named("codenvy.local.infrastructure.workspace.members")
    Set<Member> workspaceMembers() {
        final Set<Member> members = new HashSet<>(1);
        final Member member =
                new Member().withUserId("codenvy").withWorkspaceId("1q2w3e");
        Collections.addAll(member.getRoles(), "workspace/admin", "workspace/developer");
        members.add(member);
        return members;
    }
*/
    // MemberDao ~~~


    // ~~~ UserDao

    @Provides
    @Named("codenvy.local.infrastructure.users")
    Set<User> users() {
        final Set<User> users = new HashSet<>(1);
        final User user = new User().withId("codenvy")
                                    .withName("codenvy")
                                    .withEmail("che@eclipse.org")
                                    .withPassword("secret");
        user.getAliases().add("che@eclipse.org");
        users.add(user);
        return users;
    }

    // UserDao ~~~
}
