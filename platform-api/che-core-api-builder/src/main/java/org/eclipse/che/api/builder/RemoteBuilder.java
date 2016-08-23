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
package org.eclipse.che.api.builder;

import org.eclipse.che.api.builder.dto.BuildTaskDescriptor;
import org.eclipse.che.api.builder.dto.BuilderEnvironment;
import org.eclipse.che.api.builder.internal.Constants;
import org.eclipse.che.api.builder.dto.BaseBuilderRequest;
import org.eclipse.che.api.builder.dto.BuildRequest;
import org.eclipse.che.api.builder.dto.BuilderDescriptor;
import org.eclipse.che.api.builder.dto.BuilderState;
import org.eclipse.che.api.builder.dto.DependencyRequest;
import org.eclipse.che.api.core.rest.HttpJsonRequestFactory;
import org.eclipse.che.api.core.rest.shared.Links;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.dto.server.DtoFactory;

import static org.eclipse.che.api.builder.BuilderUtils.builderRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Collections;

/**
 * Represents remote {@code Builder}.
 * <p/>
 * Usage:
 * <pre>
 *     String baseUrl = ...
 *     String builderName = ... // e.g. 'maven'
 *     RemoteBuilderServer factory = new RemoteBuilderServer(baseUrl);
 *     RemoteBuilder builder = factory.getRemoteBuilder(builderName);
 *     BuildRequest request = ...
 *     RemoteTask remote = builder.perform(request);
 *     // do something with RemoteTask
 *     // e.g. check status
 *     System.out.println(remote.getConfig());
 * </pre>
 *
 * @author andrew00x
 * @see RemoteBuilderServer
 */
public class RemoteBuilder {
    private final String     baseUrl;
    private final List<Link> links;
    private final String     name;
    private final String     description;
    private final int        hashCode;
    private final HttpJsonRequestFactory requestFactory;
    private final Map<String, BuilderEnvironment> environmentMap;

    private volatile long lastUsage = -1;

    /* Package visibility, not expected to be created by api users. They should use RemoteBuilderServer to get an instance of RemoteBuilder. */
    RemoteBuilder(String baseUrl, BuilderDescriptor builderDescriptor, List<Link> links, HttpJsonRequestFactory requestFactory) {
        this.baseUrl = baseUrl;
        name = builderDescriptor.getName();
        description = builderDescriptor.getDescription();
        this.environmentMap = builderDescriptor.getEnvironments();
        this.links = new ArrayList<>(links);
        int hashCode = 7;
        hashCode = hashCode * 31 + baseUrl.hashCode();
        hashCode = hashCode * 31 + name.hashCode();
        this.hashCode = hashCode;
        this.requestFactory = requestFactory;
    }

    public final String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Get name of this builder.
     *
     * @return name of this builder
     * @see org.eclipse.che.api.builder.internal.Builder#getName()
     */
    public final String getName() {
        return name;
    }

    /**
     * Get description of this builder.
     *
     * @return description of this builder
     * @see org.eclipse.che.api.builder.internal.Builder#getDescription()
     */
    public final String getDescription() {
        return description;
    }

    /**
     * Get last time of usage of this builder.
     *
     * @return last time of usage of this builder
     */
    public long getLastUsageTime() {
        return lastUsage;
    }

    /**
     * Get Builder Environment map
     * @return map of BuilderEnvironment
     */
    public Map<String, BuilderEnvironment> getBuilderEnvironment(){
        return Collections.unmodifiableMap(this.environmentMap);
    }


    /**
     * Stats new build process.
     *
     * @param request
     *         build request
     * @return build task
     * @throws BuilderException
     *         if an error occurs
     */
    public RemoteTask perform(BuildRequest request) throws BuilderException {
        final Link link = Links.getLink(Constants.LINK_REL_BUILD, links);
        if (link == null) {
            throw new BuilderException("Unable get URL for starting remote process");
        }
        return perform(DtoFactory.getInstance().clone(link), request);
    }

    /**
     * Stats new process of analysis dependencies.
     *
     * @param request
     *         analysis dependencies request
     * @return analysis dependencies task
     * @throws BuilderException
     *         if an error occurs
     */
    public RemoteTask perform(DependencyRequest request) throws BuilderException {
        final Link link = Links.getLink(Constants.LINK_REL_DEPENDENCIES_ANALYSIS, links);
        if (link == null) {
            throw new BuilderException("Unable get URL for starting remote process");
        }
        return perform(DtoFactory.getInstance().clone(link), request);
    }

    private RemoteTask perform(Link link, BaseBuilderRequest request) throws BuilderException {
        BuildTaskDescriptor build = builderRequest(requestFactory.fromLink(link).setBody(request)).asDto(BuildTaskDescriptor.class);
        lastUsage = System.currentTimeMillis();
        return new RemoteTask(baseUrl, request.getBuilder(), build.getTaskId(), requestFactory);
    }

    /**
     * Get description of current state of {@link org.eclipse.che.api.builder.internal.Builder}.
     *
     * @return description of current state of {@link org.eclipse.che.api.builder.internal.Builder}
     * @throws BuilderException
     *         if an error occurs
     */
    public BuilderState getBuilderState() throws BuilderException {
        final Link link = Links.getLink(Constants.LINK_REL_BUILDER_STATE, links);
        if (link == null) {
            throw new BuilderException("Unable get URL for getting state of a remote builder");
        }
        return builderRequest(requestFactory.fromLink(link).addQueryParam("builder", name).setTimeout(10000)).asDto(BuilderState.class);
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RemoteBuilder)) {
            return false;
        }
        RemoteBuilder other = (RemoteBuilder)o;
        return baseUrl.equals(other.baseUrl) && name.equals(other.name);
    }

    @Override
    public final int hashCode() {
        return hashCode;
    }
}
