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
package org.eclipse.che.git.impl.jgit;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;

import org.eclipse.che.api.git.DiffPage;
import org.eclipse.che.api.git.shared.DiffRequest;
import org.eclipse.che.api.git.shared.DiffRequest.DiffType;
import org.eclipse.jgit.diff.ContentSource;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RenameDetector;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.NullProgressMonitor;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.PathFilterGroup;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

/**
 * @author <a href="mailto:andrey.parfonov@exoplatform.com">Andrey Parfonov</a>
 * @version $Id: DiffPage.java 22811 2011-03-22 07:28:35Z andrew00x $
 */
class JGitDiffPage extends DiffPage {
    private final DiffRequest request;
    private final Repository repository;

    JGitDiffPage(DiffRequest request, Repository repository) {
        this.request = request;
        this.repository = repository;
    }

    @Override
    public final void writeTo(OutputStream out) throws IOException {
        DiffFormatter formatter = new DiffFormatter(new BufferedOutputStream(out));
        formatter.setRepository(repository);
        List<String> rawFileFilter = request.getFileFilter();
        TreeFilter pathFilter = (rawFileFilter != null && rawFileFilter.size() > 0)
                ? PathFilterGroup.createFromStrings(rawFileFilter) : TreeFilter.ALL;
        formatter.setPathFilter(AndTreeFilter.create(TreeFilter.ANY_DIFF, pathFilter));

        try {
            String a = request.getCommitA();
            String b = request.getCommitB();
            boolean cached = request.isCached();

            List<DiffEntry> diff;
            if (a == null && b == null && !cached) {
                diff = indexToWorkingTree(formatter);
            } else if (a != null && b == null && !cached) {
                diff = commitToWorkingTree(a, formatter);
            } else if (b == null && cached) {
                diff = commitToIndex(a, formatter);
            } else {
                diff = commitToCommit(a, b, formatter);
            }

            DiffType type = request.getType();
            if (type == DiffType.NAME_ONLY) {
                writeNames(diff, out);
            } else if (type == DiffType.NAME_STATUS) {
                writeNamesAndStatus(diff, out);
            } else {
                writeRawDiff(diff, formatter);
            }
        } finally {
            formatter.close();
            repository.close();
        }
    }

    /**
     * Show changes between index and working tree.
     * 
     * @param formatter
     *            diff formatter
     * @return list of diff entries
     * @throws IOException
     *             if any i/o errors occurs
     */
    List<DiffEntry> indexToWorkingTree(DiffFormatter formatter) throws IOException {
        DirCache dirCache = null;
        ObjectReader reader = repository.newObjectReader();
        List<DiffEntry> diff;
        try {
            dirCache = repository.lockDirCache();
            DirCacheIterator iterA = new DirCacheIterator(dirCache);
            FileTreeIterator iterB = new FileTreeIterator(repository);
            // Seems bug in DiffFormatter when work with working. Disable detect
            // renames by formatter and do it later.
            formatter.setDetectRenames(false);
            diff = formatter.scan(iterA, iterB);
            if (!request.isNoRenames()) {
                // Detect renames.
                RenameDetector renameDetector = createRenameDetector();
                ContentSource.Pair sourcePairReader = new ContentSource.Pair(ContentSource.create(reader),
                        ContentSource.create(iterB));
                renameDetector.addAll(diff);
                diff = renameDetector.compute(sourcePairReader, NullProgressMonitor.INSTANCE);
            }
        } finally {
            reader.close();
            if (dirCache != null) {
                dirCache.unlock();
            }
        }
        return diff;
    }

    /**
     * Show changes between specified revision and working tree.
     * 
     * @param a
     *            commit
     * @param formatter
     *            diff formatter
     * @return list of diff entries
     * @throws IOException
     *             if any i/o errors occurs
     */
    List<DiffEntry> commitToWorkingTree(String a, DiffFormatter formatter) throws IOException {
        ObjectId commitA = repository.resolve(a);
        if (commitA == null) {
            throw new IllegalArgumentException("Invalid commit id " + a);
        }
        RevWalk revWalkA = new RevWalk(repository);
        RevTree treeA;
        try {
            treeA = revWalkA.parseTree(commitA);
        } finally {
            revWalkA.close();
        }

        ObjectReader reader = repository.newObjectReader();
        List<DiffEntry> diff;
        try {
            CanonicalTreeParser iterA = new CanonicalTreeParser();
            iterA.reset(reader, treeA);
            FileTreeIterator iterB = new FileTreeIterator(repository);
            // Seems bug in DiffFormatter when work with working. Disable detect
            // renames by formatter and do it later.
            formatter.setDetectRenames(false);
            diff = formatter.scan(iterA, iterB);
            if (!request.isNoRenames()) {
                // Detect renames.
                RenameDetector renameDetector = createRenameDetector();
                ContentSource.Pair sourcePairReader = new ContentSource.Pair(ContentSource.create(reader),
                        ContentSource.create(iterB));
                renameDetector.addAll(diff);
                diff = renameDetector.compute(sourcePairReader, NullProgressMonitor.INSTANCE);
            }
        } finally {
            reader.close();
        }
        return diff;
    }

    /**
     * Show changes between specified revision and index. If
     * <code>a == null</code> then view changes between HEAD and index.
     * 
     * @param a
     *            commit, pass <code>null</code> is the same as pass HEAD
     * @param formatter
     *            diff formatter
     * @return list of diff entries
     * @throws IOException
     *             if any i/o errors occurs
     */
    List<DiffEntry> commitToIndex(String a, DiffFormatter formatter) throws IOException {
        if (a == null) {
            a = Constants.HEAD;
        }

        ObjectId commitA = repository.resolve(a);
        if (commitA == null) {
            throw new IllegalArgumentException("Invalid commit id " + a);
        }
        RevWalk revWalkA = new RevWalk(repository);
        RevTree treeA;
        try {
            treeA = revWalkA.parseTree(commitA);
        } finally {
            revWalkA.close();
        }

        DirCache dirCache = null;
        ObjectReader reader = repository.newObjectReader();
        List<DiffEntry> diff;
        try {
            dirCache = repository.lockDirCache();
            CanonicalTreeParser iterA = new CanonicalTreeParser();
            iterA.reset(reader, treeA);
            DirCacheIterator iterB = new DirCacheIterator(dirCache);
            if (!request.isNoRenames()) {
                // Use embedded RenameDetector it works well with index and
                // revision history.
                formatter.setDetectRenames(true);
                int renameLimit = request.getRenameLimit();
                if (renameLimit > 0) {
                    formatter.getRenameDetector().setRenameLimit(renameLimit);
                }
            }
            diff = formatter.scan(iterA, iterB);
        } finally {
            reader.close();
            if (dirCache != null) {
                dirCache.unlock();
            }
        }
        return diff;
    }

    /**
     * Show changes between specified two revisions and index. If
     * <code>a == null</code> then view changes between HEAD and revision b.
     * 
     * @param a
     *            commit a, pass <code>null</code> is the same as pass HEAD
     * @param b
     *            commit b
     * @param formatter
     *            diff formatter
     * @return list of diff entries
     * @throws IOException
     *             if any i/o errors occurs
     */
    List<DiffEntry> commitToCommit(String a, String b, DiffFormatter formatter) throws IOException {
        if (a == null) {
            a = Constants.HEAD;
        }

        ObjectId commitA = repository.resolve(a);
        if (commitA == null) {
            throw new IllegalArgumentException("Invalid commit id " + a);
        }
        ObjectId commitB = repository.resolve(b);
        if (commitB == null) {
            throw new IllegalArgumentException("Invalid commit id " + b);
        }

        RevWalk revWalkA = new RevWalk(repository);
        RevTree treeA;
        try {
            treeA = revWalkA.parseTree(commitA);
        } finally {
            revWalkA.close();
        }

        RevWalk revWalkB = new RevWalk(repository);
        RevTree treeB;
        try {
            treeB = revWalkB.parseTree(commitB);
        } finally {
            revWalkB.close();
        }

        if (!request.isNoRenames()) {
            // Use embedded RenameDetector it works well with index and revision
            // history.
            formatter.setDetectRenames(true);
            int renameLimit = request.getRenameLimit();
            if (renameLimit > 0) {
                formatter.getRenameDetector().setRenameLimit(renameLimit);
            }
        }
        return formatter.scan(treeA, treeB);
    }

    private RenameDetector createRenameDetector() {
        RenameDetector renameDetector = new RenameDetector(repository);
        int renameLimit = request.getRenameLimit();
        if (renameLimit > 0) {
            renameDetector.setRenameLimit(renameLimit);
        }
        return renameDetector;
    }

    void writeRawDiff(List<DiffEntry> diff, DiffFormatter formatter) throws IOException {
        formatter.format(diff);
        formatter.flush();
    }

    void writeNames(List<DiffEntry> diff, OutputStream out) throws IOException {
        PrintWriter writer = new PrintWriter(out);
        for (DiffEntry de : diff) {
            writer.println(de.getChangeType() == ChangeType.DELETE ? de.getOldPath() : de.getNewPath());
        }
        writer.flush();
    }

    void writeNamesAndStatus(List<DiffEntry> diff, OutputStream out) throws IOException {
        PrintWriter writer = new PrintWriter(out);
        for (DiffEntry de : diff) {
            if (de.getChangeType() == ChangeType.ADD) {
                writer.println("A\t" + de.getNewPath());
            } else if (de.getChangeType() == ChangeType.DELETE) {
                writer.println("D\t" + de.getOldPath());
            } else if (de.getChangeType() == ChangeType.MODIFY) {
                writer.println("M\t" + de.getNewPath());
            } else if (de.getChangeType() == ChangeType.COPY) {
                writer.println("C\t" + de.getOldPath() + '\t' + de.getNewPath());
            } else if (de.getChangeType() == ChangeType.RENAME) {
                writer.println("R\t" + de.getOldPath() + '\t' + de.getNewPath());
            }
        }
        writer.flush();
    }
}
