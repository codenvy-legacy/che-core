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
package org.eclipse.che.api.auth;

import javax.inject.Singleton;
import java.security.SecureRandom;
import java.util.Random;

/**
 * Generator of tokens based on SecureRandom class.
 *
 * @author Andrey Parfonov
 * @author Sergey Kabashniuk
 */
@Singleton
public class SecureRandomTokenGenerator implements TokenGenerator {

    private final Random random = new SecureRandom();
    private final char[] chars  = new char[62];

    public SecureRandomTokenGenerator() {
        int i = 0;
        for (int c = 48; c <= 57; c++) {
            chars[i++] = (char)c;
        }
        for (int c = 65; c <= 90; c++) {
            chars[i++] = (char)c;
        }
        for (int c = 97; c <= 122; c++) {
            chars[i++] = (char)c;
        }
    }

    public String generate() {
        final char[] tokenChars = new char[255];
        for (int i = 0; i < tokenChars.length; i++) {
            tokenChars[i] = chars[random.nextInt() & (chars.length - 1)];
        }
        return new String(tokenChars);
    }
}
