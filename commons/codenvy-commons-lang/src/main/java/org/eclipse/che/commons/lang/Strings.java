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
package org.eclipse.che.commons.lang;

import java.util.ArrayList;
import java.util.List;

/** Utility class to work with String */
public class Strings {
    private Strings() {
    }

    /**
     * Returns the given string if it is non-null; the empty string otherwise.
     *
     * @param string
     *         the string to test and possibly return
     * @return {@code string} itself if it is non-null; {@code ""} if it is null
     */
    public static String nullToEmpty(String string) {
        return (string == null) ? "" : string;
    }

    /**
     * Returns the given string if it is nonempty; {@code null} otherwise.
     *
     * @param string
     *         the string to test and possibly return
     * @return {@code string} itself if it is nonempty; {@code null} if it is
     *         empty or null
     */
    public static String emptyToNull(String string) {
        return isNullOrEmpty(string) ? null : string;
    }

    /**
     * Returns {@code true} if the given string is null or is the empty string.
     * <p/>
     * <p>Consider normalizing your string references with {@link #nullToEmpty}.
     * If you do, you can use {@link String#isEmpty()} instead of this
     * method, and you won't need special null-safe forms of methods like {@link
     * String#toUpperCase} either. Or, if you'd like to normalize "in the other
     * direction," converting empty strings to {@code null}, you can use {@link
     * #emptyToNull}.
     *
     * @param string
     *         a string reference to check
     * @return {@code true} if the string is null or is the empty string
     */
    public static boolean isNullOrEmpty(String string) {
        return string == null || string.length() == 0;
    }

    /**
     * Returns a string containing the string representation of each of parts,
     * using configured separator between each.
     *
     * @param delimiter
     *         separator placed between consecutive elements.
     * @param parts
     *         strings to concatenate
     * @return string containing the string representation of each of parts separated by delimiter
     */
    public static String join(String delimiter, String... parts) {
        if (delimiter == null) {
            throw new IllegalArgumentException("First argument can't be null.");
        }

        StringBuilder sb = new StringBuilder();
        for (String alias : parts) {
            if (sb.length() != 0) {
                sb.append(delimiter);
            }
            sb.append(alias);
        }
        return sb.toString();
    }

    /** Splits string by delimiter. */
    public static String[] split(String raw, char ch) {
        final List<String> list = new ArrayList<>(4);
        int n = 0;
        int p;
        while ((p = raw.indexOf(ch, n)) != -1) {
            list.add(raw.substring(n, p).trim());
            n = p + 1;
        }
        list.add(raw.substring(n).trim());
        return list.toArray(new String[list.size()]);
    }

    /**
     * Search longest common prefix.
     *
     * @param input
     *         - input array.
     * @return - longest common prefix of the input array of the string
     */
    public static String longestCommonPrefix(String... input) {
        String prefix = "";
        if (input.length > 0) {
            prefix = input[0];
        }
        for (int i = 1; i < input.length; ++i) {
            String s = input[i];
            int j = 0;
            for (int length = Math.min(prefix.length(), s.length()); j < length; ++j) {
                if (prefix.charAt(j) != s.charAt(j)) {
                    break;
                }
            }
            prefix = prefix.substring(0, j);
        }
        return prefix;
    }

}
