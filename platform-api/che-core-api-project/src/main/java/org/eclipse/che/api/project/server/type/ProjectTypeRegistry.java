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
package org.eclipse.che.api.project.server.type;

import org.eclipse.che.api.project.server.ProjectTypeConstraintException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author gazarenkov
 */
@Singleton
public class ProjectTypeRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(ProjectTypeRegistry.class);

    public static final ProjectType BASE_TYPE = new BaseProjectType();

    public static final ChildToParentComparator CHILD_TO_PARENT_COMPARATOR = new ChildToParentComparator();

    private final static Pattern NAME_PATTERN = Pattern.compile("[^a-zA-Z0-9-_.]");

    private final Map<String, ProjectType> projectTypes = new HashMap<>();

    private Set <String> allIds = new HashSet<>();

    @Inject
    public ProjectTypeRegistry(Set<ProjectType> projTypes) {

        if(!projTypes.contains(BASE_TYPE)) {
            allIds.add(BASE_TYPE.getId());
            projectTypes.put(BASE_TYPE.getId(), BASE_TYPE);
        }

        for(ProjectType pt : projTypes) {
            if(pt.getId() != null && !pt.getId().isEmpty()) {
                allIds.add(pt.getId());

                // add Base Type as a parent if not pointed
                if (pt.getParents() != null && pt.getParents().isEmpty() && !pt.getId().equals(BASE_TYPE.getId())) {
                    LOG.debug("BASE added as parent of: " + pt.getId());
                    pt.getParents().add(BASE_TYPE);
                }
            }
        }

        for(ProjectType pt : projTypes) {
            if(pt.getId() != null && !pt.getId().isEmpty()) {
                try {
                    init(pt);
                    this.projectTypes.put(pt.getId(), pt);
                } catch (ProjectTypeConstraintException e) {
                    LOG.error(e.getMessage());
                }
            }
        }

    }

    public ProjectType getProjectType(String id) /*throws NotFoundException*/ {
        ProjectType pt = projectTypes.get(id);
        // TODO add this exception
//        if(pt == null)
//            throw new NotFoundException("Project Type "+id+" not found in the registry");
        return pt;
    }

    public Collection <ProjectType> getProjectTypes() {
        return projectTypes.values();
    }



    public List <ProjectType> getProjectTypes(Comparator<ProjectType> comparator) {

        List<ProjectType> list = new ArrayList<>();

        for(ProjectType pt : projectTypes.values()) {
            list.add(pt);
        }


        Collections.sort(list, comparator);

        return list;
    }

    // maybe for test only?
    public void registerProjectType(ProjectType projectType) throws ProjectTypeConstraintException {

        init(projectType);
        this.projectTypes.put(projectType.getId(), projectType);

    }


    private void init(ProjectType pt) throws ProjectTypeConstraintException {

        if(pt.getId() == null || pt.getId().isEmpty()) {
            throw new ProjectTypeConstraintException("Could not register Project Type with null or empty ID: " + pt.getClass().getName());
        }

        // ID spelling (no spaces, only alphanumeric)
        if(NAME_PATTERN.matcher(pt.getId()).find()) {
            throw new ProjectTypeConstraintException("Could not register Project Type with invalid ID (only Alphanumeric, dash, point and underscore allowed): " +pt.getClass().getName()+ " ID: '"+pt.getId()+"'");
        }

        if(pt.getDisplayName() == null || pt.getDisplayName().isEmpty()) {
            throw new ProjectTypeConstraintException("Could not register Project Type with null or empty display name: " +pt.getId());
        }

        for (Attribute attr : pt.getAttributes()) {

            // ID spelling (no spaces, only alphanumeric)
            if(NAME_PATTERN.matcher(attr.getName()).find()) {
                throw new ProjectTypeConstraintException("Could not register Project Type with invalid attribute Name (only Alphanumeric, dash and underscore allowed): " +attr.getClass().getName()+ " ID: '"+attr.getId()+"'");
            }
        }

        // look for parents
        for(ProjectType parent : pt.getParents()) {
            if(!allIds.contains(parent.getId())) {
                throw new ProjectTypeConstraintException("Could not register Project Type: "+pt.getId()+" : Unregistered parent Type: "+parent.getId());
            }
        }

        initAttributesRecursively(pt, pt);

        // Builders and Runners

    }


    private void initAttributesRecursively(ProjectType myType, ProjectType type)
            throws ProjectTypeConstraintException {

        for(ProjectType supertype : type.getParents()) {

            for(Attribute attr : supertype.getAttributes()) {

                // check attribute names
                for(Attribute attr2 : myType.getAttributes()) {
                    if(attr.getName().equals(attr2.getName()) && !attr.getProjectType().equals(attr2.getProjectType())) {
                        throw new ProjectTypeConstraintException("Attribute name conflict. Project type "+
                                myType.getId() + " could not be registered as attribute declaration "+ attr.getName()+
                                " is duplicated in its ancestor(s).");
                    }
                }
                ((ProjectType)myType).addAttributeDefinition(attr);
            }
            //myType.addParent(supertype);
            initAttributesRecursively(myType, supertype);
        }

    }


    public static class ChildToParentComparator implements Comparator<ProjectType> {
        @Override
        public int compare(ProjectType o1, ProjectType o2) {
            if(o1.isTypeOf(o2.getId())) {
                return -1;
            }
            if(o2.isTypeOf(o1.getId())) {
                return 1;
            }
            return 0;
        }
    }


}
