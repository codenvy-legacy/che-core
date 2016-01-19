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
package org.eclipse.che.git.impl.nativegit;

import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.git.UserResolver;
import org.eclipse.che.api.git.shared.GitUser;
import org.eclipse.che.api.user.server.dao.PreferenceDao;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.eclipse.che.dto.server.DtoFactory.newDto;

/**
 * @author Max Shaposhnik
 *
 */
public class LocalUserResolver implements UserResolver {

    private static final Logger LOG = LoggerFactory.getLogger(LocalUserResolver.class);

    private PreferenceDao preferenceDao;


    @Inject
    public LocalUserResolver(PreferenceDao preferenceDao) {
        this.preferenceDao = preferenceDao;

    }

    @Override
    public GitUser getUser() {
        User user = EnvironmentContext.getCurrent().getUser();
        GitUser gitUser = newDto(GitUser.class);
        if (user.isTemporary()) {
            gitUser.setEmail("anonymous@noemail.com");
            gitUser.setName("Anonymous");
        } else {
            String name = null;
            String email = null;
            try {
                Map<String, String> preferences = preferenceDao.getPreferences(EnvironmentContext.getCurrent().getUser().getId(),
                                                                               "git.committer.\\w+");
                name = preferences.get("git.committer.name");
                email = preferences.get("git.committer.email");
            } catch (ServerException e) {
                LOG.error(e.getLocalizedMessage(), e);
            }

            gitUser.setName(isNullOrEmpty(name) ? "Anonymous" : name);
            gitUser.setEmail(isNullOrEmpty(email) ? "anonymous@noemail.com" : email);
        }

        return gitUser;
    }
}
