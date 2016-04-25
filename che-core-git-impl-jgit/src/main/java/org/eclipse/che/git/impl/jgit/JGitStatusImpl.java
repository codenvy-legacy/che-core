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
package org.eclipse.che.git.impl.jgit;

import org.eclipse.che.api.git.GitException;
import org.eclipse.che.api.git.InfoPage;
import org.eclipse.che.api.git.shared.Status;
import org.eclipse.che.api.git.shared.StatusFormat;
import org.eclipse.jgit.api.StatusCommand;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * NativeGit implementation {@link Status}
 *
 * @author Igor Vinokur
 */
public class JGitStatusImpl implements Status, InfoPage {

    private String branchName;

    private StatusFormat format;

    private boolean clean;

    private List<String> added;

    private List<String> changed;

    private List<String> removed;

    private List<String> missing;

    private List<String> modified;

    private List<String> untracked;

    private List<String> untrackedFolders;

    private List<String> conflicting;

    private String repositoryState;

    /**
     * @param branchName
     *         current repository branch name
     * @param statusCommand
     *         Jgit status command
     * @param format
     *         the output format for the status
     * @throws GitException
     *         when any error occurs
     */
    public JGitStatusImpl(String branchName, StatusCommand statusCommand, StatusFormat format) throws GitException {
        this.branchName = branchName;
        this.format = format;

        org.eclipse.jgit.api.Status gitStatus;
        try {
            gitStatus = statusCommand.call();
        } catch (GitAPIException exception) {
            throw new GitException(exception.getMessage(), exception);
        }

        clean = gitStatus.isClean();
        added = new ArrayList<>(gitStatus.getAdded());
        changed = new ArrayList<>(gitStatus.getChanged());
        removed = new ArrayList<>(gitStatus.getRemoved());
        missing = new ArrayList<>(gitStatus.getMissing());
        modified = new ArrayList<>(gitStatus.getModified());
        untracked = new ArrayList<>(gitStatus.getUntracked());
        untrackedFolders = new ArrayList<>(gitStatus.getUntrackedFolders());
        conflicting = new ArrayList<>(gitStatus.getConflicting());
    }

    /** @see InfoPage#writeTo(OutputStream) */
    @Override
    public void writeTo(OutputStream out) throws IOException {
        StringBuilder status = new StringBuilder();

        status.append("On branch ").append(branchName).append("\n");
        if (isClean()) {
            status.append("\nnothing to commit, working directory clean");
        } else {
            if (!added.isEmpty() || !changed.isEmpty() || !removed.isEmpty()) {
                status.append("\nChanges to be committed:\n");
                added.forEach(file -> status.append("\n\tnew file:   ").append(file));
                changed.forEach(file -> status.append("\n\tmodified:   ").append(file));
                removed.forEach(file -> status.append("\n\tdeleted:    ").append(file));
                status.append("\n");
            }
            if (!untracked.isEmpty() || !modified.isEmpty() || !missing.isEmpty()) {
                status.append("\nChanges not staged for commit:\n");
                untracked.forEach(file -> status.append("\n\tnew file:   ").append(file));
                modified.forEach(file -> status.append("\n\tmodified:   ").append(file));
                missing.forEach(file -> status.append("\n\tdeleted:    ").append(file));
                status.append("\n");
            }
            if (!conflicting.isEmpty()) {
                status.append("\nUnmerged paths:\n");
                conflicting.forEach(file -> status.append("\n\tboth modified:   ").append(file));
            }
        }

        out.write(status.toString().getBytes());
    }

    /** @see Status#isClean() */
    @Override
    public boolean isClean() {
        return clean;
    }

    /** @see Status#setClean(boolean) */
    @Override
    public void setClean(boolean clean) {
        this.clean = clean;
    }

    /** @see Status#getFormat() */
    @Override
    public StatusFormat getFormat() {
        return this.format;
    }

    /** @see Status#setFormat(StatusFormat) */
    @Override
    public void setFormat(final StatusFormat format) {
        this.format = format;
    }

    /** @see Status#getBranchName() */
    @Override
    public String getBranchName() {
        return branchName;
    }

    /** @see Status#setBranchName(String) */
    @Override
    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    /** @see Status#getAdded() */
    @Override
    public List<String> getAdded() {
        return added;
    }

    /** @see Status#setAdded(List) */
    @Override
    public void setAdded(List<String> added) {
        this.added = added;
    }

    /** @see Status#getChanged() */
    @Override
    public List<String> getChanged() {
        return changed;
    }

    /** @see Status#setChanged(List) */
    @Override
    public void setChanged(List<String> changed) {
        this.changed = changed;
    }

    /** @see Status#getRemoved() */
    @Override
    public List<String> getRemoved() {
        return removed;
    }

    /** @see Status#setRemoved(List) */
    @Override
    public void setRemoved(List<String> removed) {
        this.removed = removed;
    }

    /** @see Status#getMissing() */
    @Override
    public List<String> getMissing() {
        return missing;
    }

    /** @see Status#setMissing(List) */
    @Override
    public void setMissing(List<String> missing) {
        this.missing = missing;
    }

    /** @see Status#getModified() */
    @Override
    public List<String> getModified() {
        return modified;
    }

    /** @see Status#setModified(List) */
    @Override
    public void setModified(List<String> modified) {
        this.modified = modified;
    }

    /** @see Status#getUntracked() */
    @Override
    public List<String> getUntracked() {
        return untracked;
    }

    /** @see Status#setUntracked(List) */
    @Override
    public void setUntracked(List<String> untracked) {
        this.untracked = untracked;
    }

    /** @see Status#getUntrackedFolders() */
    @Override
    public List<String> getUntrackedFolders() {
        return untrackedFolders;
    }

    /** @see Status#setUntrackedFolders(List) */
    @Override
    public void setUntrackedFolders(List<String> untrackedFolders) {
        this.untrackedFolders = untrackedFolders;
    }

    /** @see Status#getConflicting() */
    @Override
    public List<String> getConflicting() {
        return conflicting;
    }

    /** @see Status#setConflicting(List) */
    @Override
    public void setConflicting(List<String> conflicting) {
        this.conflicting = conflicting;
    }

    /** @see Status#getRepositoryState() */
    @Override
    public String getRepositoryState() {
        return this.repositoryState;
    }

    /** @see Status#setRepositoryState(String) */
    @Override
    public void setRepositoryState(String repositoryState) {
        this.repositoryState = repositoryState;
    }
}
