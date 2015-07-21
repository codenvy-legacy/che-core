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
import org.eclipse.che.api.machine.server.command.CommandImpl;
import org.eclipse.che.api.machine.server.dao.CommandDao;
import org.eclipse.che.api.machine.server.recipe.GroupImpl;
import org.eclipse.che.api.machine.server.recipe.PermissionsImpl;
import org.eclipse.che.api.machine.server.recipe.RecipeImpl;
import org.eclipse.che.api.machine.server.dao.RecipeDao;
import org.eclipse.che.api.machine.shared.ManagedCommand;
import org.eclipse.che.api.machine.shared.Group;
import org.eclipse.che.api.machine.shared.ManagedRecipe;
import org.eclipse.che.api.user.server.TokenValidator;
import org.eclipse.che.api.user.server.dao.PreferenceDao;
import org.eclipse.che.api.user.server.dao.User;
import org.eclipse.che.api.user.server.dao.UserDao;
import org.eclipse.che.api.user.server.dao.UserProfileDao;
import org.eclipse.che.api.workspace.server.dao.Member;
import org.eclipse.che.api.workspace.server.dao.MemberDao;
import org.eclipse.che.api.workspace.server.dao.Workspace;
import org.eclipse.che.api.workspace.server.dao.WorkspaceDao;
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
        bind(MemberDao.class).to(LocalMemberDaoImpl.class);
        bind(AccountDao.class).to(LocalAccountDaoImpl.class);
        bind(AuthenticationDao.class).to(LocalAuthenticationDaoImpl.class);
//        bind(FactoryStore.class).to(InMemoryFactoryStore.class);
        bind(TokenValidator.class).to(DummyTokenValidator.class);
        bind(RecipeDao.class).to(LocalRecipeDaoImpl.class);
        bind(CommandDao.class).to(LocalCommandDaoImpl.class);
    }


    //~~~ AccountDao

    @Provides
    @Named("codenvy.local.infrastructure.accounts")
    Set<Account> accounts() {
        final Set<Account> accounts = new HashSet<>(1);
        accounts.add(new Account().withName("codenvy_account").withId("account1234567890"));
        return accounts;
    }

    @Provides
    @Named("codenvy.local.infrastructure.account.members")
    Set<org.eclipse.che.api.account.server.dao.Member> accountMembers() {
        final Set<org.eclipse.che.api.account.server.dao.Member> members = new HashSet<>(1);
        final org.eclipse.che.api.account.server.dao.Member member =
                new org.eclipse.che.api.account.server.dao.Member().withUserId("codenvy").withAccountId("account1234567890");
        Collections.addAll(member.getRoles(), "account/owner", "account/member");
        members.add(member);
        return members;
    }

    // AccountDao ~~~


    // ~~~ WorkspaceDao

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

    // MemberDao ~~~


    // ~~~ UserDao

    @Provides
    @Named("codenvy.local.infrastructure.users")
    Set<User> users() {
        final Set<User> users = new HashSet<>(1);
        final User user = new User().withId("codenvy")
                                    .withEmail("che@eclipse.org")
                                    .withPassword("secret");
        user.getAliases().add("che@eclipse.org");
        users.add(user);
        return users;
    }

    // UserDao ~~~

    @Provides
    @Named("codenvy.local.infrastructure.recipes")
    Set<ManagedRecipe> recipes() {
        final Group group = new GroupImpl("public", null, asList("read", "search"));
        final ManagedRecipe recipe1 = new RecipeImpl().withId("recipe1234567890")
                                                      .withName("UBUNTU")
                                                      .withCreator("codenvy")
                                                      .withType("docker")
                                                      .withScript("FROM ubuntu\ntail -f /dev/null")
                                                      .withTags(singletonList("ubuntu"))
                                                      .withPermissions(new PermissionsImpl(null, singletonList(group)));
        final ManagedRecipe recipe2 = new RecipeImpl().withId("recipe2345678901")
                                                      .withName("BUSYBOX")
                                                      .withCreator("codenvy")
                                                      .withType("docker")
                                                      .withScript("FROM busybox\ntail -f /dev/null")
                                                      .withTags(asList("java", "busybox"))
                                                      .withPermissions(new PermissionsImpl(null, singletonList(group)));

        return unmodifiableSet(new HashSet<>(asList(recipe1, recipe2)));
    }

    @Provides
    @Named("codenvy.local.infrastructure.commands")
    Set<ManagedCommand> commands() {
        final ManagedCommand command1 = new CommandImpl().withId("command123")
                                                         .withName("mci")
                                                         .withCreator("codenvy")
                                                         .withWorkspaceId("workspace123")
                                                         .withCommandLine("mvn clean install")
                                                         .withVisibility("private")
                                                         .withType("maven");
        final ManagedCommand command2 = new CommandImpl().withId("command234")
                                                         .withName("ab")
                                                         .withCreator("codenvy")
                                                         .withWorkspaceId("workspace123")
                                                         .withCommandLine("ant build")
                                                         .withVisibility("public")
                                                         .withType("ant");
        return unmodifiableSet(new HashSet<>(asList(command1, command2)));
    }
}
