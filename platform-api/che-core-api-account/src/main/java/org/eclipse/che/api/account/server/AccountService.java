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
package org.eclipse.che.api.account.server;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

import org.eclipse.che.api.account.server.dao.Account;
import org.eclipse.che.api.account.server.dao.AccountDao;
import org.eclipse.che.api.account.server.dao.Member;
import org.eclipse.che.api.account.server.dao.PlanDao;
import org.eclipse.che.api.account.server.dao.Subscription;
import org.eclipse.che.api.account.shared.dto.AccountDescriptor;
import org.eclipse.che.api.account.shared.dto.AccountReference;
import org.eclipse.che.api.account.shared.dto.AccountUpdate;
import org.eclipse.che.api.account.shared.dto.MemberDescriptor;
import org.eclipse.che.api.account.shared.dto.NewAccount;
import org.eclipse.che.api.account.shared.dto.NewMembership;
import org.eclipse.che.api.account.shared.dto.NewSubscription;
import org.eclipse.che.api.account.shared.dto.Plan;
import org.eclipse.che.api.account.shared.dto.SubscriptionDescriptor;
import org.eclipse.che.api.account.shared.dto.SubscriptionReference;
import org.eclipse.che.api.account.shared.dto.SubscriptionResourcesUsed;
import org.eclipse.che.api.account.shared.dto.SubscriptionState;
import org.eclipse.che.api.account.shared.dto.UpdateResourcesDescriptor;
import org.eclipse.che.api.account.shared.dto.UsedAccountResources;
import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.rest.Service;
import org.eclipse.che.api.core.rest.annotations.GenerateLink;
import org.eclipse.che.api.core.rest.annotations.Required;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.core.util.LinksHelper;
import org.eclipse.che.api.user.server.dao.User;
import org.eclipse.che.api.user.server.dao.UserDao;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.lang.NameGenerator;
import org.eclipse.che.dto.server.DtoFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.String.format;
import static java.util.Collections.singletonList;

/**
 * Account API
 *
 * @author Eugene Voevodin
 * @author Alex Garagatyi
 */
@Api(value = "/account",
        description = "Account manager")
@Path("/account")
public class AccountService extends Service {
    private static final Logger LOG = LoggerFactory.getLogger(AccountService.class);
    private final AccountDao                  accountDao;
    private final UserDao                     userDao;
    private final SubscriptionServiceRegistry registry;
    private final PlanDao                     planDao;
    private final ResourcesManager            resourcesManager;

    @Inject
    public AccountService(AccountDao accountDao,
                          UserDao userDao,
                          SubscriptionServiceRegistry registry,
                          PlanDao planDao,
                          ResourcesManager resourcesManager) {
        this.accountDao = accountDao;
        this.userDao = userDao;
        this.registry = registry;
        this.planDao = planDao;
        this.resourcesManager = resourcesManager;
    }

    /**
     * Creates new account and adds current user as member to created account
     * with role <i>"account/owner"</i>. Returns status <b>201 CREATED</b>
     * and {@link AccountDescriptor} of created account if account has been created successfully.
     * Each new account should contain at least name.
     *
     * @param newAccount
     *         new account
     * @return descriptor of created account
     * @throws NotFoundException
     *         when some error occurred while retrieving account
     * @throws ConflictException
     *         when new account is {@code null}
     *         or new account name is {@code null}
     *         or when any of new account attributes is not valid
     * @throws ServerException
     * @see AccountDescriptor
     * @see #getById(String, SecurityContext)
     * @see #getByName(String, SecurityContext)
     */
    @ApiOperation(value = "Create a new account",
            notes = "Create a new account",
            response = Account.class,
            position = 1)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "CREATED"),
            @ApiResponse(code = 404, message = "Not Found"),
            @ApiResponse(code = 409, message = "Conflict Error"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @POST
    @GenerateLink(rel = Constants.LINK_REL_CREATE_ACCOUNT)
    @RolesAllowed({"user", "system/admin"})
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(@Context SecurityContext securityContext,
                           @Required NewAccount newAccount) throws NotFoundException,
                                                                   ConflictException,
                                                                   ServerException {
        requiredNotNull(newAccount, "New account");
        requiredNotNull(newAccount.getName(), "Account name");
        if (newAccount.getAttributes() != null) {
            for (String attributeName : newAccount.getAttributes().keySet()) {
                validateAttributeName(attributeName);
            }
        }
        User current = null;
        if (securityContext.isUserInRole("user")) {
            current = userDao.getByAlias(securityContext.getUserPrincipal().getName());
            //for now account <-One to One-> user
            if (accountDao.getByOwner(current.getId()).size() != 0) {
                throw new ConflictException(format("Account which owner is %s already exists", current.getId()));
            }
        }

        try {
            accountDao.getByName(newAccount.getName());
            throw new ConflictException(format("Account with name %s already exists", newAccount.getName()));
        } catch (NotFoundException ignored) {
        }
        final String accountId = NameGenerator.generate(Account.class.getSimpleName().toLowerCase(), Constants.ID_LENGTH);
        final Account account = new Account().withId(accountId)
                                             .withName(newAccount.getName())
                                             .withAttributes(newAccount.getAttributes());

        accountDao.create(account);
        if (current != null) {
            final Member owner = new Member().withAccountId(accountId)
                                             .withUserId(current.getId())
                                             .withRoles(Arrays.asList("account/owner"));
            accountDao.addMember(owner);
        }
        return Response.status(Response.Status.CREATED)
                       .entity(toDescriptor(account, securityContext))
                       .build();
    }

    /**
     * Returns all accounts memberships for current user.
     *
     * @return accounts memberships of current user
     * @throws NotFoundException
     *         when any of memberships contains account that doesn't exist
     * @throws ServerException
     *         when some error occurred while retrieving accounts or memberships
     * @see MemberDescriptor
     */
    @ApiOperation(value = "Get current user memberships",
            notes = "This API call returns a JSON with all user membership in a single or multiple accounts",
            response = MemberDescriptor.class,
            responseContainer = "List",
            position = 2)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Not Found"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @GenerateLink(rel = Constants.LINK_REL_GET_ACCOUNTS)
    @RolesAllowed("user")
    @Produces(MediaType.APPLICATION_JSON)
    public List<MemberDescriptor> getMemberships(@Context SecurityContext securityContext) throws NotFoundException, ServerException {
        final Principal principal = securityContext.getUserPrincipal();
        final User current = userDao.getByAlias(principal.getName());
        final List<Member> memberships = accountDao.getByMember(current.getId());
        final List<MemberDescriptor> result = new ArrayList<>(memberships.size());
        for (Member membership : memberships) {
            result.add(toDescriptor(membership, accountDao.getById(membership.getAccountId()), securityContext));
        }
        return result;
    }

    /**
     * Returns all accounts memberships for user with given identifier.
     *
     * @param userId
     *         user identifier to search memberships
     * @return accounts memberships
     * @throws ConflictException
     *         when user identifier is {@code null}
     * @throws NotFoundException
     *         when user with given identifier doesn't exist
     * @throws ServerException
     *         when some error occurred while retrieving user or memberships
     * @see MemberDescriptor
     */
    @ApiOperation(value = "Get memberships of a specific user",
            notes = "ID of a user should be specified as a query parameter. JSON with membership details is returned. For this API call system/admin or system/manager role is required",
            response = MemberDescriptor.class,
            responseContainer = "List",
            position = 3)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Not Found"),
            @ApiResponse(code = 409, message = "No User ID specified"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/memberships")
    @GenerateLink(rel = Constants.LINK_REL_GET_ACCOUNTS)
    @RolesAllowed({"system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public List<MemberDescriptor> getMembershipsOfSpecificUser(@ApiParam(value = "User ID", required = true)
                                                               @Required @QueryParam("userid") String userId,
                                                               @Context SecurityContext securityContext) throws NotFoundException,
                                                                                                                ServerException,
                                                                                                                ConflictException {
        requiredNotNull(userId, "User identifier");
        final User user = userDao.getById(userId);
        final List<Member> memberships = accountDao.getByMember(user.getId());
        final List<MemberDescriptor> result = new ArrayList<>(memberships.size());
        for (Member membership : memberships) {
            result.add(toDescriptor(membership, accountDao.getById(membership.getAccountId()), securityContext));
        }
        return result;
    }

    /**
     * Removes attribute with given name from certain account.
     *
     * @param accountId
     *         account identifier
     * @param attributeName
     *         attribute name to remove attribute
     * @throws ConflictException
     *         if attribute name is not valid
     * @throws NotFoundException
     *         if account with given identifier doesn't exist
     * @throws ServerException
     *         when some error occurred while getting/updating account
     */
    @ApiOperation(value = "Delete account attribute",
            notes = "Remove attribute from an account. Attribute name is used as a quary parameter. For this API request account/owner, system/admin or system/manager role is required",
            position = 4)
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "OK"),
            @ApiResponse(code = 404, message = "Not Found"),
            @ApiResponse(code = 409, message = "Invalid attribute name"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @DELETE
    @Path("/{id}/attribute")
    @RolesAllowed({"account/owner", "system/admin", "system/manager"})
    public void removeAttribute(@ApiParam(value = "Account ID", required = true)
                                @PathParam("id") String accountId,
                                @ApiParam(value = "Attribute name to be removed", required = true)
                                @QueryParam("name") String attributeName) throws ConflictException, NotFoundException, ServerException {
        validateAttributeName(attributeName);
        final Account account = accountDao.getById(accountId);
        account.getAttributes().remove(attributeName);
        accountDao.update(account);
    }

    /**
     * Searches for account with given identifier and returns {@link AccountDescriptor} for it.
     *
     * @param id
     *         account identifier
     * @return descriptor of found account
     * @throws NotFoundException
     *         when account with given identifier doesn't exist
     * @throws ServerException
     *         when some error occurred while retrieving account
     * @see AccountDescriptor
     * @see #getByName(String, SecurityContext)
     */
    @ApiOperation(value = "Get account by ID",
            notes = "Get account information by its ID. JSON with account details is returned. This API call requires account/owner, system/admin or system/manager role.",
            response = AccountDescriptor.class,
            position = 5)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Not Found"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/{id}")
    @RolesAllowed({"account/owner", "system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public AccountDescriptor getById(@ApiParam(value = "Account ID", required = true)
                                     @PathParam("id") String id,
                                     @Context SecurityContext securityContext) throws NotFoundException, ServerException {
        final Account account = accountDao.getById(id);
        return toDescriptor(account, securityContext);
    }

    /**
     * Searches for account with given name and returns {@link AccountDescriptor} for it.
     *
     * @param name
     *         account name
     * @return descriptor of found account
     * @throws NotFoundException
     *         when account with given name doesn't exist
     * @throws ConflictException
     *         when account name is {@code null}
     * @throws ServerException
     *         when some error occurred while retrieving account
     * @see AccountDescriptor
     * @see #getById(String, SecurityContext)
     */
    @ApiOperation(value = "Get account by name",
            notes = "Get account information by its name. JSON with account details is returned. This API call requires system/admin or system/manager role.",
            response = AccountDescriptor.class,
            position = 5)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Not Found"),
            @ApiResponse(code = 409, message = "No account name specified"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/find")
    @GenerateLink(rel = Constants.LINK_REL_GET_ACCOUNT_BY_NAME)
    @RolesAllowed({"system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public AccountDescriptor getByName(@ApiParam(value = "Account name", required = true)
                                       @Required @QueryParam("name") String name,
                                       @Context SecurityContext securityContext) throws NotFoundException,
                                                                                        ServerException,
                                                                                        ConflictException {
        requiredNotNull(name, "Account name");
        final Account account = accountDao.getByName(name);
        return toDescriptor(account, securityContext);
    }

    /**
     * Creates new account member with role <i>"account/member"</i>.
     *
     * @param accountId
     *         account identifier
     * @param membership
     *         new membership
     * @return descriptor of created member
     * @throws ConflictException
     *         when user identifier is {@code null}
     * @throws NotFoundException
     *         when user or account with given identifier doesn't exist
     * @throws ServerException
     *         when some error occurred while getting user or adding new account member
     * @see MemberDescriptor
     * @see #removeMember(String, String)
     * @see #getMembers(String, SecurityContext)
     */
    @ApiOperation(value = "Add a new member to account",
            notes = "Add a new user to an account. This user will have account/member role. This API call requires account/owner, system/admin or system/manager role.",
            response = MemberDescriptor.class,
            position = 6)
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "OK"),
            @ApiResponse(code = 404, message = "Not Found"),
            @ApiResponse(code = 409, message = "No user ID specified"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @POST
    @Path("/{id}/members")
    @RolesAllowed({"account/owner", "system/admin"})
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addMember(@ApiParam(value = "Account ID")
                              @PathParam("id")
                              String accountId,
                              @ApiParam(value = "New membership", required = true)
                              @Required
                              NewMembership membership,
                              @Context SecurityContext context) throws ConflictException,
                                                                       NotFoundException,
                                                                       ServerException {
        requiredNotNull(membership, "New membership");
        requiredNotNull(membership.getUserId(), "User ID");
        requiredNotNull(membership.getRoles(), "Roles");
        if (membership.getRoles().isEmpty()) {
            throw new ConflictException("Roles should not be empty");
        }
        userDao.getById(membership.getUserId());//check user exists
        final Member newMember = new Member().withAccountId(accountId)
                                             .withUserId(membership.getUserId())
                                             .withRoles(membership.getRoles());
        accountDao.addMember(newMember);
        return Response.status(Response.Status.CREATED)
                       .entity(toDescriptor(newMember, accountDao.getById(accountId), context))
                       .build();
    }

    /**
     * Returns all members of certain account.
     *
     * @param id
     *         account identifier
     * @return account members
     * @throws NotFoundException
     *         when account with given identifier doesn't exist
     * @throws ServerException
     *         when some error occurred while retrieving accounts or members
     * @see MemberDescriptor
     * @see #addMember(String, NewMembership, SecurityContext)
     * @see #removeMember(String, String)
     */
    @ApiOperation(value = "Get account members",
            notes = "Get all members for a specific account. This API call requires account/owner, system/admin or system/manager role.",
            response = MemberDescriptor.class,
            responseContainer = "List",
            position = 7)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Account ID not found"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/{id}/members")
    @RolesAllowed({"account/owner", "system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public List<MemberDescriptor> getMembers(@ApiParam(value = "Account ID")
                                             @PathParam("id") String id,
                                             @Context SecurityContext securityContext) throws NotFoundException, ServerException {
        final Account account = accountDao.getById(id);
        final List<Member> members = accountDao.getMembers(id);
        final List<MemberDescriptor> result = new ArrayList<>(members.size());
        for (Member member : members) {
            result.add(toDescriptor(member, account, securityContext));
        }
        return result;
    }

    /**
     * Removes user with given identifier as member from certain account.
     *
     * @param accountId
     *         account identifier
     * @param userId
     *         user identifier
     * @throws NotFoundException
     *         when user or account with given identifier doesn't exist
     * @throws ServerException
     *         when some error occurred while retrieving account members or removing certain member
     * @throws ConflictException
     *         when removal member is last <i>"account/owner"</i>
     * @see #addMember(String, NewMembership, SecurityContext)
     * @see #getMembers(String, SecurityContext)
     */
    @ApiOperation(value = "Remove user from account",
            notes = "Remove user from a specific account. This API call requires account/owner, system/admin or system/manager role.",
            position = 8)
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "OK"),
            @ApiResponse(code = 404, message = "Account ID not found"),
            @ApiResponse(code = 409, message = "Account should have at least 1 owner"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @DELETE
    @Path("/{id}/members/{userid}")
    @RolesAllowed({"account/owner", "system/admin", "system/manager"})
    public void removeMember(@ApiParam(value = "Account ID", required = true)
                             @PathParam("id") String accountId,
                             @ApiParam(value = "User ID")
                             @PathParam("userid") String userId) throws NotFoundException, ServerException, ConflictException {
        final List<Member> members = accountDao.getMembers(accountId);
        //search for member
        Member target = null;
        int owners = 0;
        for (Member member : members) {
            if (member.getRoles().contains("account/owner")) owners++;
            if (member.getUserId().equals(userId)) target = member;
        }
        if (target == null) {
            throw new ConflictException(format("User %s doesn't have membership with account %s", userId, accountId));
        }
        //account should have at least 1 owner
        if (owners == 1 && target.getRoles().contains("account/owner")) {
            throw new ConflictException("Account should have at least 1 owner");
        }
        accountDao.removeMember(target);
    }

    /**
     * <p>Updates account.</p>
     * <strong>Note:</strong> existed account attributes with same names as
     * update attributes will be replaced with update attributes.
     *
     * @param accountId
     *         account identifier
     * @param update
     *         account update
     * @return descriptor of updated account
     * @throws NotFoundException
     *         when account with given identifier doesn't exist
     * @throws ConflictException
     *         when account update is {@code null}
     *         or when account with given name already exists
     * @throws ServerException
     *         when some error occurred while retrieving/persisting account
     * @see AccountDescriptor
     */
    @ApiOperation(value = "Update account",
            notes = "Update account. This API call requires account/owner role.",
            response = AccountDescriptor.class,
            position = 9)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Account ID not found"),
            @ApiResponse(code = 409, message = "Invalid account ID or account name already exists"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @POST
    @Path("/{id}")
    @RolesAllowed({"account/owner"})
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public AccountDescriptor update(@ApiParam(value = "Account ID", required = true)
                                    @PathParam("id") String accountId,
                                    AccountUpdate update,
                                    @Context SecurityContext securityContext) throws NotFoundException,
                                                                                     ConflictException,
                                                                                     ServerException {
        requiredNotNull(update, "Account update");
        final Account account = accountDao.getById(accountId);
        //current user should be account owner to update it
        if (update.getName() != null) {
            if (!account.getName().equals(update.getName()) && accountDao.getByName(update.getName()) != null) {
                throw new ConflictException(format("Account with name %s already exists", update.getName()));
            } else {
                account.setName(update.getName());
            }
        }
        if (update.getAttributes() != null) {
            for (String attributeName : update.getAttributes().keySet()) {
                validateAttributeName(attributeName);
            }
            account.getAttributes().putAll(update.getAttributes());
        }
        accountDao.update(account);
        return toDescriptor(account, securityContext);
    }

    /**
     * Returns list of subscriptions descriptors for certain account.
     * If service identifier is provided returns subscriptions that matches provided service.
     *
     * @param accountId
     *         account identifier
     * @param serviceId
     *         service identifier
     * @return subscriptions descriptors
     * @throws NotFoundException
     *         when account with given identifier doesn't exist
     * @throws ServerException
     *         when some error occurred while retrieving subscriptions
     * @see SubscriptionDescriptor
     */
    @ApiOperation(value = "Get account subscriptions",
            notes = "Get information on account subscriptions. This API call requires account/owner, account/member, system/admin or system/manager role.",
            response = SubscriptionDescriptor.class,
            responseContainer = "List",
            position = 10)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Account ID not found"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/{accountId}/subscriptions")
    @RolesAllowed({"account/member", "account/owner", "system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public List<SubscriptionDescriptor> getSubscriptions(@ApiParam(value = "Account ID", required = true)
                                                         @PathParam("accountId") String accountId,
                                                         @ApiParam(value = "Service ID", required = false)
                                                         @QueryParam("service") String serviceId,
                                                         @Context SecurityContext securityContext) throws NotFoundException,
                                                                                                          ServerException {
        final List<Subscription> subscriptions = new ArrayList<>();
        if (serviceId == null || serviceId.isEmpty()) {
            subscriptions.addAll(accountDao.getActiveSubscriptions(accountId));
        } else {
            final Subscription activeSubscription = accountDao.getActiveSubscription(accountId, serviceId);
            if (activeSubscription != null) {
                subscriptions.add(activeSubscription);
            }
        }
        final List<SubscriptionDescriptor> result = new ArrayList<>(subscriptions.size());
        for (Subscription subscription : subscriptions) {
            result.add(toDescriptor(subscription, securityContext, null));
        }
        return result;
    }

    /**
     * Returns {@link SubscriptionDescriptor} for subscription with given identifier.
     *
     * @param subscriptionId
     *         subscription identifier
     * @return descriptor of subscription
     * @throws NotFoundException
     *         when subscription with given identifier doesn't exist
     * @throws ForbiddenException
     *         when user hasn't access to call this method
     * @see SubscriptionDescriptor
     * @see #getSubscriptions(String, String serviceId, SecurityContext)
     * @see #removeSubscription(String, SecurityContext)
     */
    @ApiOperation(value = "Get subscription details",
            notes = "Get information on a particular subscription by its unique ID.",
            response = SubscriptionDescriptor.class,
            position = 11)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 403, message = "User not authorized to call this method"),
            @ApiResponse(code = 404, message = "Account ID not found"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/subscriptions/{subscriptionId}")
    @RolesAllowed({"user", "system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    public SubscriptionDescriptor getSubscriptionById(@ApiParam(value = "Subscription ID", required = true)
                                                      @PathParam("subscriptionId") String subscriptionId,
                                                      @Context SecurityContext securityContext) throws NotFoundException,
                                                                                                       ServerException,
                                                                                                       ForbiddenException {
        final Subscription subscription = accountDao.getSubscriptionById(subscriptionId);
        Set<String> roles = null;
        if (securityContext.isUserInRole("user")) {
            roles = resolveRolesForSpecificAccount(subscription.getAccountId());
            if (!roles.contains("account/owner") && !roles.contains("account/member")) {
                throw new ForbiddenException("Access denied");
            }
        }
        return toDescriptor(subscription, securityContext, roles);
    }

    /**
     * <p>Creates new subscription. Returns {@link SubscriptionDescriptor}
     * when subscription has been created successfully.
     * <p>Each new subscription should contain plan id and account id </p>
     *
     * @param newSubscription
     *         new subscription
     * @return descriptor of created subscription
     * @throws ConflictException
     *         when new subscription is {@code null}
     *         or new subscription plan identifier is {@code null}
     *         or new subscription account identifier is {@code null}
     * @throws NotFoundException
     *         if plan with certain identifier is not found
     * @throws org.eclipse.che.api.core.ApiException
     * @see SubscriptionDescriptor
     * @see #getSubscriptionById(String, SecurityContext)
     * @see #removeSubscription(String, SecurityContext)
     */
    @ApiOperation(value = "Add new subscription",
            notes = "Add a new subscription to an account. JSON with subscription details is sent. Roles: account/owner, system/admin.",
            response = SubscriptionDescriptor.class,
            position = 12)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "CREATED"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "Invalid subscription parameter"),
            @ApiResponse(code = 409, message = "Unknown ServiceID is used or payment token is invalid"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @POST
    @Path("/subscriptions")
    @GenerateLink(rel = Constants.LINK_REL_ADD_SUBSCRIPTION)
    @RolesAllowed({"user", "system/admin", "system/manager"})
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addSubscription(@ApiParam(value = "Subscription details", required = true)
                                    @Required NewSubscription newSubscription,
                                    @Context SecurityContext securityContext)
            throws ApiException {
        requiredNotNull(newSubscription, "New subscription");
        requiredNotNull(newSubscription.getAccountId(), "Account identifier");
        requiredNotNull(newSubscription.getPlanId(), "Plan identifier");
        requiredNotNull(newSubscription.getUsePaymentSystem(), "Use payment system");

        //check user has access to add subscription
        final Set<String> roles = new HashSet<>();
        if (securityContext.isUserInRole("user")) {
            roles.addAll(resolveRolesForSpecificAccount(newSubscription.getAccountId()));
            if (!roles.contains("account/owner")) {
                throw new ForbiddenException("Access denied");
            }
        }

        final Plan plan = planDao.getPlanById(newSubscription.getPlanId());

        // check service exists
        final SubscriptionService service = registry.get(plan.getServiceId());
        if (null == service) {
            throw new ConflictException("Unknown serviceId is used");
        }

        //Not admin has additional restrictions
        if (!securityContext.isUserInRole("system/admin") && !securityContext.isUserInRole("system/manager")) {
            // check that subscription is allowed for not admin
            if (plan.getSalesOnly()) {
                throw new ForbiddenException("User not authorized to add this subscription, please contact support");
            }

            // only admins are allowed to disable payment on subscription addition
            if (!newSubscription.getUsePaymentSystem().equals(plan.isPaid())) {
                throw new ConflictException("Given value of attribute usePaymentSystem is not allowed");
            }

            // check trial
            if (newSubscription.getTrialDuration() != null && newSubscription.getTrialDuration() != 0) {
                // allow regular user use subscription without trial or with trial which duration equal to duration from the plan
                if (!newSubscription.getTrialDuration().equals(plan.getTrialDuration())) {
                    throw new ConflictException("User not authorized to add this subscription, please contact support");
                }
            }

            //only admins can override properties
            if (!newSubscription.getProperties().isEmpty()) {
                throw new ForbiddenException("User not authorized to add subscription with custom properties, please contact support");
            }
        }

        // disable payment if subscription is free
        if (!plan.isPaid()) {
            newSubscription.setUsePaymentSystem(false);
        }

        //preparing properties
        Map<String, String> properties = plan.getProperties();
        Map<String, String> customProperties = newSubscription.getProperties();
        for (Map.Entry<String, String> propertyEntry : customProperties.entrySet()) {
            if (properties.containsKey(propertyEntry.getKey())) {
                properties.put(propertyEntry.getKey(), propertyEntry.getValue());
            } else {
                throw new ForbiddenException("Forbidden overriding of non-existent plan properties");
            }
        }

        //create new subscription
        Subscription subscription = new Subscription()
                .withId(NameGenerator.generate(Subscription.class.getSimpleName().toLowerCase(), Constants.ID_LENGTH))
                .withAccountId(newSubscription.getAccountId())
                .withUsePaymentSystem(newSubscription.getUsePaymentSystem())
                .withServiceId(plan.getServiceId())
                .withPlanId(plan.getId())
                .withProperties(properties)
                .withDescription(plan.getDescription())
                .withBillingCycleType(plan.getBillingCycleType())
                .withBillingCycle(plan.getBillingCycle())
                .withBillingContractTerm(plan.getBillingContractTerm())
                .withState(SubscriptionState.ACTIVE);

        if (newSubscription.getTrialDuration() != null && newSubscription.getTrialDuration() != 0) {
            Calendar calendar = Calendar.getInstance();
            subscription.setTrialStartDate(calendar.getTime());
            calendar.add(Calendar.DATE, newSubscription.getTrialDuration());
            subscription.setTrialEndDate(calendar.getTime());
        }

        service.beforeCreateSubscription(subscription);

        LOG.info("Add subscription# id#{}# userId#{}# accountId#{}# planId#{}#",
                 subscription.getId(),
                 EnvironmentContext.getCurrent().getUser().getId(),
                 subscription.getAccountId(),
                 subscription.getPlanId());

        accountDao.addSubscription(subscription);

        service.afterCreateSubscription(subscription);

        LOG.info("Added subscription. Subscription ID #{}# Account ID #{}#", subscription.getId(), subscription.getAccountId());

        return Response.status(Response.Status.CREATED)
                       .entity(toDescriptor(subscription, securityContext, roles))
                       .build();
    }

    /**
     * Removes subscription by id. Actually makes it inactive.
     *
     * @param subscriptionId
     *         id of the subscription to remove
     * @throws NotFoundException
     *         if subscription with such id is not found
     * @throws ForbiddenException
     *         if user hasn't permissions
     * @throws ServerException
     *         if internal server error occurs
     * @throws org.eclipse.che.api.core.ApiException
     * @see #addSubscription(NewSubscription, SecurityContext)
     * @see #getSubscriptions(String, String, SecurityContext)
     */
    @ApiOperation(value = "Remove subscription",
            notes = "Remove subscription from account. Roles: account/owner, system/admin.",
            position = 13)
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "OK"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "Invalid subscription ID"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @DELETE
    @Path("/subscriptions/{subscriptionId}")
    @RolesAllowed({"user", "system/admin", "system/manager"})
    public void removeSubscription(@ApiParam(value = "Subscription ID", required = true)
                                   @PathParam("subscriptionId") String subscriptionId, @Context SecurityContext securityContext)
            throws ApiException {
        final Subscription toRemove = accountDao.getSubscriptionById(subscriptionId);
        if (securityContext.isUserInRole("user") && !resolveRolesForSpecificAccount(toRemove.getAccountId()).contains("account/owner")) {
            throw new ForbiddenException("Access denied");
        }
        if (SubscriptionState.INACTIVE == toRemove.getState()) {
            throw new ForbiddenException("Subscription is inactive already " + subscriptionId);
        }

        LOG.info("Remove subscription# id#{}# userId#{}# accountId#{}#", subscriptionId, EnvironmentContext.getCurrent().getUser().getId(),
                 toRemove.getAccountId());

        toRemove.setState(SubscriptionState.INACTIVE);
        accountDao.updateSubscription(toRemove);
        final SubscriptionService service = registry.get(toRemove.getServiceId());
        service.onRemoveSubscription(toRemove);

    }


    @ApiOperation(value = "Remove account",
            notes = "Remove subscription from account. JSON with subscription details is sent. Can be performed only by system/admin.",
            position = 16)
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "OK"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "Invalid account ID"),
            @ApiResponse(code = 409, message = "Cannot delete account with associated workspaces"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @DELETE
    @Path("/{id}")
    @RolesAllowed("system/admin")
    public void remove(@ApiParam(value = "Account ID", required = true)
                       @PathParam("id") String id) throws NotFoundException, ServerException, ConflictException {
        accountDao.remove(id);
    }

    /**
     * Redistributes resources between workspaces
     *
     * @param id
     *         account id
     * @param updateResourcesDescriptors
     *         descriptor of resources for updating
     * @throws ForbiddenException
     *         when account hasn't permission for setting attribute in workspace
     * @throws NotFoundException
     *         when account or workspace with given id doesn't exist
     * @throws ConflictException
     *         when account hasn't required Saas subscription
     *         or user want to use more RAM than he has
     * @throws ServerException
     */
    @ApiOperation(value = "Redistributes resources",
            notes = "Redistributes resources between workspaces. Roles: account/owner, system/manager, system/admin.",
            position = 17)
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "OK"),
            @ApiResponse(code = 403, message = "Access denied"),
            @ApiResponse(code = 404, message = "Not found"),
            @ApiResponse(code = 409, message = "Conflict Error"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @POST
    @Path("/{id}/resources")
    @RolesAllowed({"account/owner", "system/manager", "system/admin"})
    @Consumes(MediaType.APPLICATION_JSON)
    public void redistributeResources(@ApiParam(value = "Account ID", required = true)
                                      @PathParam("id") String id,
                                      @ApiParam(value = "Resources description", required = true)
                                      @Required
                                      List<UpdateResourcesDescriptor> updateResourcesDescriptors) throws ForbiddenException,
                                                                                                         ConflictException,
                                                                                                         NotFoundException,
                                                                                                         ServerException {
        resourcesManager.redistributeResources(id, updateResourcesDescriptors);
    }

    /**
     * Returns used resources, provided by subscriptions
     *
     * @param accountId
     *         account id
     */
    @ApiOperation(value = "Get used resources, provided by subscriptions",
            notes = "Returns used resources, provided by subscriptions. Roles: account/owner, account/member, system/manager, system/admin.",
            position = 17)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Not found"),
            @ApiResponse(code = 500, message = "Internal Server Error")})
    @GET
    @Path("/{id}/resources")
    @RolesAllowed({"account/owner", "account/member", "system/manager", "system/admin"})
    @Produces(MediaType.APPLICATION_JSON)
    public List<SubscriptionResourcesUsed> getResources(@ApiParam(value = "Account ID", required = true)
                                                        @PathParam("id") String accountId,
                                                        @QueryParam("serviceId") String serviceId)
            throws ServerException, NotFoundException, ConflictException {
        Set<SubscriptionService> subscriptionServices = new HashSet<>();
        if (serviceId == null) {
            subscriptionServices.addAll(registry.getAll());
        } else {
            final SubscriptionService subscriptionService = registry.get(serviceId);
            if (subscriptionService == null) {
                throw new ConflictException("Unknown serviceId is used");
            }
            subscriptionServices.add(subscriptionService);
        }

        List<SubscriptionResourcesUsed> result = new ArrayList<>();
        for (SubscriptionService subscriptionService : subscriptionServices) {
            Subscription activeSubscription = accountDao.getActiveSubscription(accountId, subscriptionService.getServiceId());
            if (activeSubscription != null) {
                //For now account can have only one subscription for each service
                UsedAccountResources usedAccountResources = subscriptionService.getAccountResources(activeSubscription);
                result.add(DtoFactory.getInstance().createDto(SubscriptionResourcesUsed.class)
                                     .withUsed(usedAccountResources.getUsed())
                                     .withSubscriptionReference(toReference(activeSubscription)));
            }
        }

        return result;
    }

    /**
     * Can be used only in methods that is restricted with @RolesAllowed. Require "user" role.
     *
     * @param currentAccountId
     *         account id to resolve roles for
     * @return set of user roles
     */
    private Set<String> resolveRolesForSpecificAccount(String currentAccountId) {
        try {
            final String userId = EnvironmentContext.getCurrent().getUser().getId();
            for (Member membership : accountDao.getByMember(userId)) {
                if (membership.getAccountId().equals(currentAccountId)) {
                    return new HashSet<>(membership.getRoles());
                }
            }
        } catch (ApiException ignored) {
        }
        return Collections.emptySet();
    }

    private void validateAttributeName(String attributeName) throws ConflictException {
        if (attributeName == null || attributeName.isEmpty() || attributeName.toLowerCase().startsWith("codenvy")) {
            throw new ConflictException(format("Attribute name '%s' is not valid", attributeName));
        }
    }

    /** Converts {@link Account} to {@link AccountDescriptor} */
    private AccountDescriptor toDescriptor(Account account, SecurityContext securityContext) {
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        final List<Link> links = new LinkedList<>();
        links.add(LinksHelper.createLink(HttpMethod.GET,
                                         uriBuilder.clone()
                                                   .path(getClass(), "getMemberships")
                                                   .build()
                                                   .toString(),
                                         null,
                                         MediaType.APPLICATION_JSON,
                                         Constants.LINK_REL_GET_ACCOUNTS));
        links.add(LinksHelper.createLink(HttpMethod.GET,
                                         uriBuilder.clone()
                                                   .path(getClass(), "getSubscriptions")
                                                   .build(account.getId())
                                                   .toString(),
                                         null,
                                         MediaType.APPLICATION_JSON,
                                         Constants.LINK_REL_GET_SUBSCRIPTIONS));
        links.add(LinksHelper.createLink(HttpMethod.GET,
                                         uriBuilder.clone()
                                                   .path(getClass(), "getMembers")
                                                   .build(account.getId())
                                                   .toString(),
                                         null,
                                         MediaType.APPLICATION_JSON,
                                         Constants.LINK_REL_GET_MEMBERS));
        links.add(LinksHelper.createLink(HttpMethod.GET,
                                         uriBuilder.clone()
                                                   .path(getClass(), "getById")
                                                   .build(account.getId())
                                                   .toString(),
                                         null,
                                         MediaType.APPLICATION_JSON,
                                         Constants.LINK_REL_GET_ACCOUNT_BY_ID));
        links.add(LinksHelper.createLink(HttpMethod.GET,
                                         uriBuilder.clone()
                                                   .path(getClass(), "getResources")
                                                   .build(account.getId())
                                                   .toString(),
                                         null,
                                         MediaType.APPLICATION_JSON,
                                         Constants.LINK_REL_GET_ACCOUNT_RESOURCES));
        if (securityContext.isUserInRole("system/admin") || securityContext.isUserInRole("system/manager")) {
            links.add(LinksHelper.createLink(HttpMethod.GET,
                                             uriBuilder.clone()
                                                       .path(getClass(), "getByName")
                                                       .queryParam("name", account.getName())
                                                       .build()
                                                       .toString(),
                                             null,
                                             MediaType.APPLICATION_JSON,
                                             Constants.LINK_REL_GET_ACCOUNT_BY_NAME));
        }
        if (securityContext.isUserInRole("system/admin")) {
            links.add(LinksHelper.createLink(HttpMethod.DELETE,
                                             uriBuilder.clone().path(getClass(), "remove")
                                                       .build(account.getId())
                                                       .toString(),
                                             null,
                                             null,
                                             Constants.LINK_REL_REMOVE_ACCOUNT));
        }

        if (!securityContext.isUserInRole("account/owner") &&
            !securityContext.isUserInRole("account/member") &&
            !securityContext.isUserInRole("system/admin") &&
            !securityContext.isUserInRole("system/manager")) {
            account.getAttributes().clear();
        }
        account.getAttributes().remove("codenvy:creditCardToken");
        account.getAttributes().remove("codenvy:billing.date");
        return DtoFactory.getInstance().createDto(AccountDescriptor.class)
                         .withId(account.getId())
                         .withName(account.getName())
                         .withAttributes(account.getAttributes())
                         .withLinks(links);
    }

    /**
     * Converts {@link Member} to {@link MemberDescriptor}
     */
    private MemberDescriptor toDescriptor(Member member, Account account, SecurityContext securityContext) {
        final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
        final Link removeMember = LinksHelper.createLink(HttpMethod.DELETE,
                                                         uriBuilder.clone()
                                                                   .path(getClass(), "removeMember")
                                                                   .build(account.getId(), member.getUserId())
                                                                   .toString(),
                                                         null,
                                                         null,
                                                         Constants.LINK_REL_REMOVE_MEMBER);
        final Link allMembers = LinksHelper.createLink(HttpMethod.GET,
                                                       uriBuilder.clone()
                                                                 .path(getClass(), "getMembers")
                                                                 .build(account.getId())
                                                                 .toString(),
                                                       null,
                                                       MediaType.APPLICATION_JSON,
                                                       Constants.LINK_REL_GET_MEMBERS);
        final AccountReference accountRef = DtoFactory.getInstance().createDto(AccountReference.class)
                                                      .withId(account.getId())
                                                      .withName(account.getName());
        if (member.getRoles().contains("account/owner") ||
            securityContext.isUserInRole("system/admin") ||
            securityContext.isUserInRole("system/manager")) {
            accountRef.setLinks(singletonList(LinksHelper.createLink(HttpMethod.GET,
                                                                     uriBuilder.clone()
                                                                               .path(getClass(), "getById")
                                                                               .build(account.getId())
                                                                               .toString(),
                                                                     null,
                                                                     MediaType.APPLICATION_JSON,
                                                                     Constants.LINK_REL_GET_ACCOUNT_BY_ID)));
        }
        return DtoFactory.getInstance().createDto(MemberDescriptor.class)
                         .withUserId(member.getUserId())
                         .withRoles(member.getRoles())
                         .withAccountReference(accountRef)
                         .withLinks(Arrays.asList(removeMember, allMembers));
    }

    /**
     * Checks object reference is not {@code null}
     *
     * @param object
     *         object reference to check
     * @param subject
     *         used as subject of exception message "{subject} required"
     * @throws ConflictException
     *         when object reference is {@code null}
     */
    private void requiredNotNull(Object object, String subject) throws ConflictException {
        if (object == null) {
            throw new ConflictException(subject + " required");
        }
    }

    /**
     * Create {@link SubscriptionDescriptor} from {@link Subscription}.
     * Set with roles should be used if account roles can't be resolved with {@link SecurityContext}
     * (If there is no id of the account in the REST path.)
     *
     * @param subscription
     *         subscription that should be converted to {@link SubscriptionDescriptor}
     * @param resolvedRoles
     *         resolved roles. Do not use if id of the account presents in REST path.
     */
    private SubscriptionDescriptor toDescriptor(Subscription subscription, SecurityContext securityContext, Set resolvedRoles) {
        List<Link> links = new ArrayList<>(0);
        // community subscriptions should not use urls
        if (!"sas-community".equals(subscription.getPlanId())) {
            final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
            links.add(LinksHelper.createLink(HttpMethod.GET,
                                             uriBuilder.clone()
                                                       .path(getClass(), "getSubscriptionById")
                                                       .build(subscription.getId())
                                                       .toString(),
                                             null,
                                             MediaType.APPLICATION_JSON,
                                             Constants.LINK_REL_GET_SUBSCRIPTION));
            boolean isUserPrivileged = (resolvedRoles != null && resolvedRoles.contains("account/owner")) ||
                                       securityContext.isUserInRole("account/owner") ||
                                       securityContext.isUserInRole("system/admin") ||
                                       securityContext.isUserInRole("system/manager");
            if (SubscriptionState.ACTIVE.equals(subscription.getState()) && isUserPrivileged) {
                links.add(LinksHelper.createLink(HttpMethod.DELETE,
                                                 uriBuilder.clone()
                                                           .path(getClass(), "removeSubscription")
                                                           .build(subscription.getId())
                                                           .toString(),
                                                 null,
                                                 null,
                                                 Constants.LINK_REL_REMOVE_SUBSCRIPTION));
            }
        }

        // Do not send with REST properties that starts from 'codenvy:'
        LinkedHashMap<String, String> filteredProperties = new LinkedHashMap<>();
        for (Map.Entry<String, String> property : subscription.getProperties().entrySet()) {
            if (!property.getKey().startsWith("codenvy:") || securityContext.isUserInRole("system/admin") ||
                securityContext.isUserInRole("system/manager")) {
                filteredProperties.put(property.getKey(), property.getValue());
            }
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
        dateFormat.setLenient(false);

        return DtoFactory.getInstance().createDto(SubscriptionDescriptor.class)
                         .withId(subscription.getId())
                         .withAccountId(subscription.getAccountId())
                         .withServiceId(subscription.getServiceId())
                         .withProperties(filteredProperties)
                         .withPlanId(subscription.getPlanId())
                         .withState(subscription.getState())
                         .withDescription(subscription.getDescription())
                         .withStartDate(null == subscription.getStartDate() ? null : dateFormat.format(subscription.getStartDate()))
                         .withEndDate(null == subscription.getEndDate() ? null : dateFormat.format(subscription.getEndDate()))
                         .withTrialStartDate(
                                 null == subscription.getTrialStartDate() ? null : dateFormat.format(subscription.getTrialStartDate()))
                         .withTrialEndDate(
                                 null == subscription.getTrialEndDate() ? null : dateFormat.format(subscription.getTrialEndDate()))
                         .withUsePaymentSystem(subscription.getUsePaymentSystem())
                         .withBillingStartDate(
                                 null == subscription.getBillingStartDate() ? null : dateFormat.format(subscription.getBillingStartDate()))
                         .withBillingEndDate(
                                 null == subscription.getBillingEndDate() ? null : dateFormat.format(subscription.getBillingEndDate()))
                         .withNextBillingDate(
                                 null == subscription.getNextBillingDate() ? null : dateFormat.format(subscription.getNextBillingDate()))
                         .withBillingCycle(subscription.getBillingCycle())
                         .withBillingCycleType(subscription.getBillingCycleType())
                         .withBillingContractTerm(subscription.getBillingContractTerm())
                         .withLinks(links);
    }

    /**
     * Create {@link SubscriptionReference} from {@link Subscription}.
     *
     * @param subscription
     *         subscription that should be converted to {@link SubscriptionReference}
     */
    private SubscriptionReference toReference(Subscription subscription) {
        List<Link> links = new ArrayList<>(0);
        // community subscriptions should not use urls
        if (!"sas-community".equals(subscription.getPlanId())) {
            final UriBuilder uriBuilder = getServiceContext().getServiceUriBuilder();
            links.add(LinksHelper.createLink(HttpMethod.GET,
                                             uriBuilder.clone()
                                                       .path(getClass(), "getSubscriptionById")
                                                       .build(subscription.getId())
                                                       .toString(),
                                             null,
                                             MediaType.APPLICATION_JSON,
                                             Constants.LINK_REL_GET_SUBSCRIPTION));
        }

        return DtoFactory.getInstance().createDto(SubscriptionReference.class)
                         .withSubscriptionId(subscription.getId())
                         .withServiceId(subscription.getServiceId())
                         .withDescription(subscription.getDescription())
                         .withPlanId(subscription.getPlanId())
                         .withLinks(links);
    }
}
