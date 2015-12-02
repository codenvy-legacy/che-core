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

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.io.ByteStreams;

import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.commons.lang.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.hash.Funnels.asOutputStream;

public class HashSumsCounter implements VirtualFileVisitor {
    private final VirtualFile                folder;
    private final HashFunction               hashFunction;
    private final List<Pair<String, String>> hashSums;


    public HashSumsCounter(VirtualFile folder, HashFunction hashFunction) {
        this.folder = folder;
        this.hashFunction = hashFunction;
        hashSums = newArrayList();
    }

    public List<Pair<String, String>> countHashSums() throws ServerException {
        folder.accept(this);
        return hashSums;
    }

    @Override
    public void visit(VirtualFile virtualFile) throws ServerException {
        if (virtualFile.isFile()) {
            try (InputStream in = virtualFile.getContent()) {
                final Hasher hasher = hashFunction.newHasher();
                ByteStreams.copy(in, asOutputStream(hasher));
                final String hexHash = hasher.hash().toString();
                hashSums.add(Pair.of(hexHash, virtualFile.getPath().subPath(folder.getPath()).toString()));
            } catch (IOException e) {
                throw new ServerException(e);
            } catch (ForbiddenException e) {
                throw new ServerException(e.getServiceError());
            }
        } else {
            for (VirtualFile child : virtualFile.getChildren()) {
                child.accept(this);
            }
        }
    }
}
