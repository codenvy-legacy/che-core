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
package org.eclipse.che.ide.rest;

/**
 * Created by The eXo Platform SARL        .<br/>
 * HTTP methods
 *
 * @author Gennady Azarenkov
 * @version $Id: $
 */
public interface HTTPMethod {

    public static final String GET = "GET";

    public static final String PUT = "PUT";

    public static final String POST = "POST";

    public static final String DELETE = "DELETE";

    public static final String SEARCH = "SEARCH";

    public static final String PROPFIND = "PROPFIND";

    public static final String PROPPATCH = "PROPPATCH";

    public static final String HEAD = "HEAD";

    public static final String CHECKIN = "CHECKIN";

    public static final String CHECKOUT = "CHECKOUT";

    public static final String COPY = "COPY";

    public static final String LOCK = "LOCK";

    public static final String MOVE = "MOVE";

    public static final String UNLOCK = "UNLOCK";

    public static final String OPTIONS = "OPTIONS";

    public static final String MKCOL = "MKCOL";

    public static final String REPORT = "REPORT";

    public static final String UPDATE = "UPDATE";

    public static final String ACL = "ACL";

}
