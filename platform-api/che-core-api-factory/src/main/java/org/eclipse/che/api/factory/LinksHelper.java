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

import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.core.rest.shared.dto.LinkParameter;
import org.eclipse.che.api.factory.dto.Factory;
import org.eclipse.che.dto.server.DtoFactory;

import javax.inject.Singleton;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.*;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

/** Helper class for creation links. */
@Singleton
public class LinksHelper {

    private static List<String> snippetTypes = Collections.unmodifiableList(Arrays.asList("markdown", "url", "html", "iframe"));

    public List<Link> createLinks(Factory factory, Set<FactoryImage> images, UriInfo uriInfo)
            throws UnsupportedEncodingException {
        List<Link> links = new LinkedList<>();

        final UriBuilder baseUriBuilder;
        if (uriInfo != null) {
            baseUriBuilder = UriBuilder.fromUri(uriInfo.getBaseUri());
        } else {
            baseUriBuilder = UriBuilder.fromUri("/");
        }
        // add path to factory service
        UriBuilder factoryUriBuilder = baseUriBuilder.clone().path(FactoryService.class);
        String factoryId = factory.getId();
        Link createProject;

        // uri to retrieve factory
        links.add(createLink(HttpMethod.GET, "self", null, MediaType.APPLICATION_JSON,
                             factoryUriBuilder.clone().path(FactoryService.class, "getFactory").build(factoryId).toString(), null));

        // uri's to retrieve images
        for (FactoryImage image : images) {
            links.add(createLink(HttpMethod.GET, "image", null, image.getMediaType(),
                                 factoryUriBuilder.clone().path(FactoryService.class, "getImage").queryParam("imgId", image.getName())
                                                  .build(factoryId).toString(), null));
        }

        // uri's of snippets
        for (String snippetType : snippetTypes) {
            links.add(createLink(HttpMethod.GET, "snippet/" + snippetType, null, MediaType.TEXT_PLAIN,
                                 factoryUriBuilder.clone().path(FactoryService.class, "getFactorySnippet").queryParam("type", snippetType)
                                                  .build(factoryId).toString(), null));
        }

        // uri to accept factory
        createProject = createLink(HttpMethod.GET, "create-project", null, MediaType.TEXT_HTML,
                                   baseUriBuilder.clone().replacePath("f").queryParam("id", factoryId).build().toString(), null);
        links.add(createProject);

        // links of analytics
        links.add(createLink(HttpMethod.GET, "accepted", null, MediaType.TEXT_PLAIN,
                             baseUriBuilder.clone().path("analytics").path("public-metric/factory_used")
                                           .queryParam("factory", URLEncoder.encode(createProject.getHref(), "UTF-8")).build().toString(),
                             null));
        return links;
    }

    /**
     * Find links with given relation.
     *
     * @param links
     *         - links for searching
     * @param relation
     *         - searching relation
     * @return - set of links with relation equal to desired, empty set if there is no such links
     */
    public List<Link> getLinkByRelation(List<Link> links, String relation) {
        if (relation == null || links == null) {
            throw new IllegalArgumentException("Value of parameters can't be null.");
        }
        List<Link> result = new LinkedList<>();
        for (Link link : links) {
            if (relation.equals(link.getRel())) {
                result.add(link);
            }
        }

        return result;
    }

    private Link createLink(String method, String rel, String consumes, String produces, String href, List<LinkParameter> params) {
        return DtoFactory.getInstance().createDto(Link.class)
                         .withMethod(method)
                         .withRel(rel)
                         .withProduces(produces)
                         .withConsumes(consumes)
                         .withHref(href)
                         .withParameters(params);
    }
}
