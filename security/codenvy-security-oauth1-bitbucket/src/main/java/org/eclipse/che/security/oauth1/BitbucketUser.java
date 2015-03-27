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
package org.eclipse.che.security.oauth1;


import org.eclipse.che.security.oauth1.shared.User;

/**
 * Represents Bitbucket user.
 *
 * @author Kevin Pollet
 */
public class BitbucketUser implements User {
    private String username;
    private String email;

    @Override
    public final String getId() {
        return email;
    }

    @Override
    public final void setId(String id) {
        //nothing to do there is no id field in Bitbucket response
    }

    @Override
    public String getName() {
        return username;
    }

    @Override
    public void setName(String name) {
        //nothing to do there is no name field in Bitbucket response
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "BitbucketUser{" +
               "id='" + getId() + '\'' +
               ", name='" + username + '\'' +
               ", email='" + email + '\'' +
               '}';
    }
}
