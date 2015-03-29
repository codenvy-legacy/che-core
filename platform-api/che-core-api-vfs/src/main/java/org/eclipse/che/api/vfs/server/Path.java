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
package org.eclipse.che.api.vfs.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Path of VirtualFile.
 *
 * @author andrew00x
 */
public final class Path {
    /** Create new path. */
    public static Path fromString(String path) {
        return new Path(parse(path));
    }

    private static final String[] EMPTY_PATH    = new String[0];
    private static final Pattern  PATH_SPLITTER = Pattern.compile("/");

    private static String[] parse(String raw) {
        String[] parsed = ((raw == null) || raw.isEmpty() || ((raw.length() == 1) && (raw.charAt(0) == '/')))
                          ? EMPTY_PATH : PATH_SPLITTER.split(raw.charAt(0) == '/' ? raw.substring(1) : raw);
        if (parsed.length == 0) {
            return parsed;
        }
        List<String> newTokens = new ArrayList<>(parsed.length);
        for (String token : parsed) {
            if ("..".equals(token)) {
                int size = newTokens.size();
                if (size == 0) {
                    throw new IllegalArgumentException(String.format("Invalid path '%s', '..' on root. ", raw));
                }
                newTokens.remove(size - 1);
            } else if (!".".equals(token)) {
                newTokens.add(token);
            }
        }
        return newTokens.toArray(new String[newTokens.size()]);
    }

    public static final Path ROOT = new Path();

    private final    String[] elements;
    private volatile int      hashCode;
    private volatile String   asString;

    private Path(String... elements) {
        this.elements = elements;
    }

    public Path getParent() {
        return isRoot() ? null : elements.length == 1 ? ROOT : subPath(0, elements.length - 1);
    }

    public Path subPath(int beginIndex) {
        return subPath(beginIndex, elements.length);
    }

    public Path subPath(int beginIndex, int endIndex) {
        if (beginIndex < 0 || beginIndex >= elements.length || endIndex > elements.length || beginIndex >= endIndex) {
            throw new IllegalArgumentException("Invalid end or begin index. ");
        }
        final int len = endIndex - beginIndex;
        final String[] subPath = new String[len];
        System.arraycopy(elements, beginIndex, subPath, 0, len);
        return new Path(subPath);
    }

    public String getName() {
        return isRoot() ? "" : element(elements.length - 1);
    }

    public String[] elements() {
        String[] copy = new String[elements.length];
        System.arraycopy(elements, 0, copy, 0, elements.length);
        return copy;
    }

    public int length() {
        return elements.length;
    }

    public String element(int index) {
        if (index < 0 || index >= elements.length) {
            throw new IllegalArgumentException("Invalid index. ");
        }
        return elements[index];
    }

    public boolean isRoot() {
        return elements.length == 0;
    }

    public boolean isChild(Path parent) {
        if (parent.elements.length >= this.elements.length) {
            return false;
        }
        for (int i = 0, parentLength = parent.elements.length; i < parentLength; i++) {
            if (!parent.elements[i].equals(this.elements[i])) {
                return false;
            }
        }
        return true;
    }

    public Path newPath(String name) {
        final String[] relative = parse(name);
        if (relative.length == 0) {
            return this; // It is safety to return this instance since it is immutable.
        }
        final String[] absolute = new String[elements.length + relative.length];
        System.arraycopy(elements, 0, absolute, 0, elements.length);
        System.arraycopy(relative, 0, absolute, elements.length, relative.length);
        return new Path(absolute);
    }

    public Path newPath(String... relative) {
        if (relative.length == 0) {
            return this; // It is safety to return this instance since it is immutable.
        }
        final String[] absolute = new String[elements.length + relative.length];
        System.arraycopy(elements, 0, absolute, 0, elements.length);
        System.arraycopy(relative, 0, absolute, elements.length, relative.length);
        return new Path(absolute);
    }

    public Path newPath(Path relative) {
        final String[] absolute = new String[elements.length + relative.elements.length];
        System.arraycopy(elements, 0, absolute, 0, elements.length);
        System.arraycopy(relative.elements, 0, absolute, elements.length, relative.elements.length);
        return new Path(absolute);
    }

    public String join(char separator) {
        StringBuilder builder = new StringBuilder();
        for (String element : elements) {
            builder.append(separator);
            builder.append(element);
        }
        return builder.toString();
    }

   /* ==================================================== */

    @Override
    public String toString() {
        if (isRoot()) {
            return "/";
        }
        if (asString == null) {
            asString = join('/');
        }
        return asString;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Path)) {
            return false;
        }
        Path path = (Path)o;
        return Arrays.equals(elements, path.elements);
    }

    @Override
    public int hashCode() {
        int hash = hashCode;
        if (hash == 0) {
            hash = 8;
            hash = 31 * hash + Arrays.hashCode(elements);
            hashCode = hash;
        }
        return hash;
    }
}
