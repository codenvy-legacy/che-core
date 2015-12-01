/*******************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - API 
 *   SAP           - initial implementation
 *******************************************************************************/
package org.eclipse.che.git.impl.jgit.ssh;

import java.util.regex.Pattern;

/**
 * TODO This class exports two static util methods regarding SSH They exist in
 * org.eclipse.che.git.impl.nativegit.GitUrl but jGitImpl cannot depend on
 * nativegit... Need to refarctor in PR
 * 
 * 
 * @author I034528
 *
 */
public class SshUtil {

    public static final Pattern GIT_SSH_URL_PATTERN = Pattern
            .compile("((((git|ssh)://)(([^\\\\/@:]+@)??)[^\\\\/@:]+)|([^\\\\/@:]+@[^\\\\/@:]+))(:|/)[^\\\\@:]+");

    public static boolean isSSH(String url) {
        return url != null && GIT_SSH_URL_PATTERN.matcher(url).matches();
    }
}
