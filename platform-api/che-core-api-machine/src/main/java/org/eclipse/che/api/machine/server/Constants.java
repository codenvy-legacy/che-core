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
package org.eclipse.che.api.machine.server;

/**
 * @author Eugene Voevodin
 */
public class Constants {

    public static final String LINK_REL_REMOVE_RECIPE          = "remove recipe";
    public static final String LINK_REL_GET_RECIPE_SCRIPT      = "get recipe script";
    public static final String LINK_REL_CREATE_RECIPE          = "create recipe";
    public static final String LINK_REL_GET_RECIPES_BY_CREATOR = "get created recipes";
    public static final String LINK_REL_SEARCH_RECIPES         = "search recipes";
    public static final String LINK_REL_UPDATE_RECIPE          = "update recipe";
    public static final String LINK_REL_CREATE_COMMAND         = "create command";
    public static final String LINK_REL_REMOVE_COMMAND         = "remove command";
    public static final String LINK_REL_GET_COMMAND            = "get command";
    public static final String LINK_REL_GET_ALL_COMMANDS       = "get workspace commands";
    public static final String LINK_REL_UPDATE_COMMAND         = "update command";

    private Constants() {
    }
}
