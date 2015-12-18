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
package org.eclipse.che.api.vfs.server.impl.file;

import com.google.common.collect.ImmutableMap;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import com.google.common.io.ByteStreams;

import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.vfs.server.Path;
import org.eclipse.che.api.vfs.server.VirtualFile;
import org.eclipse.che.api.vfs.server.VirtualFileFilter;
import org.eclipse.che.api.vfs.server.VirtualFileVisitor;
import org.eclipse.che.api.vfs.server.impl.memory.MemoryLuceneSearcherProvider;
import org.eclipse.che.api.vfs.server.impl.memory.MemoryMountPoint;
import org.eclipse.che.commons.lang.NameGenerator;
import org.eclipse.che.commons.lang.Pair;
import org.eclipse.che.dto.server.DtoFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class LocalVirtualFileTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private final String DEFAULT_CONTENT       = "__TEST__";
    private final byte[] DEFAULT_CONTENT_BYTES = DEFAULT_CONTENT.getBytes();

    private File            testDirectory;
    private LocalMountPoint mountPoint;

    @Before
    public void setUp() throws Exception {
        File targetDir = new File(Thread.currentThread().getContextClassLoader().getResource(".").getPath()).getParentFile();
        testDirectory = new File(targetDir, NameGenerator.generate("watcher-", 4));
        assertTrue(testDirectory.mkdir());

        mountPoint = new LocalMountPoint(testDirectory, new EventService(), new MemoryLuceneSearcherProvider());
    }

    @Test
    public void getsName() throws Exception {
        String name = generateFileName();
        VirtualFile root = getRoot();

        VirtualFile file = root.createFile(name, "");

        assertEquals(name, file.getName());
    }

    @Test
    public void getsPath() throws Exception {
        String name = generateFileName();
        VirtualFile root = getRoot();

        VirtualFile file = root.createFile(name, "");

        assertEquals("/" + file.getName(), file.getPath().toString());
    }

    @Test
    public void getsRootPath() throws Exception {
        VirtualFile root = getRoot();
        assertEquals("/", root.getPath().toString());
    }

    @Test
    public void checksIsFile() throws Exception {
        VirtualFile root = getRoot();

        VirtualFile file = root.createFile(generateFileName(), "");
        assertTrue(file.isFile());

        VirtualFile folder = root.createFolder(generateFolderName());
        assertFalse(folder.isFile());
    }

    @Test
    public void checksIsFolder() throws Exception {
        VirtualFile root = getRoot();

        VirtualFile file = root.createFile(generateFileName(), "");
        assertFalse(file.isFolder());

        VirtualFile folder = root.createFolder(generateFolderName());
        assertTrue(folder.isFolder());
    }

    @Test
    public void checksFileExistence() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile file = root.createFile(generateFileName(), "");
        assertTrue(file.exists());
    }

    @Test
    public void checksDeletedFileExistence() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile file = root.createFile(generateFileName(), "");

        file.delete();

        assertFalse(file.exists());
    }

    @Test
    public void checksIsRoot() throws Exception {
        VirtualFile root = getRoot();

        VirtualFile file = root.createFile(generateFileName(), "");
        VirtualFile folder = root.createFolder(generateFolderName());

        assertFalse(file.isRoot());
        assertFalse(folder.isRoot());
        assertTrue(root.isRoot());
    }

    @Test
    public void getsCreationDate() throws Exception {
        // todo
    }

    @Test
    public void getsLastModificationDate() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile file = root.createFile(generateFileName(), "");
        long beforeUpdate = file.getLastModificationDate();

        file.updateContent("updated content");

        long afterUpdate = file.getLastModificationDate();
        assertTrue(afterUpdate > beforeUpdate);
    }

    @Test
    public void getsParent() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile file = root.createFile(generateFileName(), "");
        assertSame(root, file.getParent());
    }

    @Test
    public void getsRootParent() throws Exception {
        VirtualFile root = getRoot();
        assertNull(root.getParent());
    }

    @Test
    public void getsEmptyPropertiesMapIfFileDoesNotHaveAny() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile file = root.createFile(generateFileName(), "");
        assertTrue(file.getProperties().isEmpty());
    }

    @Test
    public void getsPropertiesMap() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile file = root.createFile(generateFileName(), "");
        Map<String, List<String>> properties = ImmutableMap.of("property1", newArrayList("value1", "value2"));
        file.updateProperties(properties);

        assertEquals(properties, file.getProperties());
    }

    @Test
    public void getsSingleValueOfMultivaluedProperty() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile file = root.createFile(generateFileName(), "");
        file.updateProperties(ImmutableMap.of("property1", newArrayList("value1", "value2")));

        assertEquals("value1", file.getProperty("property1"));
    }

    @Test
    public void getsValuesOfMultivaluedProperty() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile file = root.createFile(generateFileName(), "");
        file.updateProperties(ImmutableMap.of("property1", newArrayList("value1", "value2")));

        assertEquals(newArrayList("value1", "value2"), file.getProperties("property1"));
    }

    @Test
    public void updatesProperties() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile file = root.createFile(generateFileName(), "");
        file.updateProperties(ImmutableMap.of("property1", newArrayList("value1", "value2")));
        Map<String, List<String>> expected = ImmutableMap.of("property1", newArrayList("valueX", "valueY"),
                                                             "new property1", newArrayList("value3", "value4"));
        Map<String, List<String>> update = ImmutableMap.of("property1", newArrayList("valueX", "valueY"),
                                                           "new property1", newArrayList("value3", "value4"));

        file.updateProperties(update);

        assertEquals(expected, file.getProperties());
    }

    @Test
    public void setsProperties() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile file = root.createFile(generateFileName(), "");
        Map<String, List<String>> expected = ImmutableMap.of("property1", newArrayList("value1", "value2"));

        file.setProperties("property1", newArrayList("value1", "value2"));

        assertEquals(expected, file.getProperties());
    }

    @Test
    public void setsProperty() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile file = root.createFile(generateFileName(), "");
        Map<String, List<String>> expected = ImmutableMap.of("property1", newArrayList("value1"));

        file.setProperty("property1", "value1");

        assertEquals(expected, file.getProperties());
    }

    @Test
    public void acceptsVisitor() throws Exception {
        VirtualFile root = getRoot();
        boolean[] b = new boolean[]{false};
        VirtualFileVisitor visitor = new VirtualFileVisitor() {
            @Override
            public void visit(VirtualFile virtualFile) {
                assertSame(root, virtualFile);
                b[0] = true;
            }
        };
        root.accept(visitor);
        assertTrue("visit(VirtualFile) method was not invoked", b[0]);
    }

    @Test
    public void countsMd5Sums() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile folder = root.createFolder(generateFolderName());
        VirtualFile file1 = folder.createFile(generateFileName(), DEFAULT_CONTENT);
        VirtualFile file2 = folder.createFile(generateFileName(), DEFAULT_CONTENT);
        root.createFolder(generateFolderName());
        Set<Pair<String, String>> expected = newHashSet(Pair.of(countMd5Sum(file1), file1.getPath().subPath(1).toString()),
                                                        Pair.of(countMd5Sum(file2), file2.getPath().subPath(1).toString()));

        assertEquals(expected, newHashSet(folder.countMd5Sums()));
    }

    @Test
    public void getsChildren() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile file1 = root.createFile(generateFileName(), "");
        VirtualFile file2 = root.createFile(generateFileName(), "");
        VirtualFile folder1 = root.createFolder(generateFolderName());

        List<VirtualFile> expectedResult = newArrayList(file1, file2, folder1);
        Collections.sort(expectedResult);

        assertEquals(expectedResult, root.getChildren());
    }

    @Test
    public void getsChildrenWithFilter() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile file1 = root.createFile(generateFileName(), "");
        VirtualFile file2 = root.createFile(generateFileName(), "");
        root.createFolder(generateFolderName());

        List<VirtualFile> expectedResult = newArrayList(file1, file2);
        Collections.sort(expectedResult);

        List<VirtualFile> children = root.getChildren(new VirtualFileFilter() {
            @Override
            public boolean accept(VirtualFile file) {
                return file.equals(file1) || file.equals(file2);
            }
        });

        assertEquals(expectedResult, children);
    }

    @Test
    public void getsChild() throws Exception {
        VirtualFile root = getRoot();
        String name = generateFileName();
        VirtualFile file = root.createFile(name, "");

        assertSame(file, root.getChild(Path.fromString(name)));
    }

    @Test
    public void getsContentOfFileAsStream() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile file = root.createFile(generateFileName(), DEFAULT_CONTENT);

        InputStream content = file.getContent();
        byte[] bytes = ByteStreams.toByteArray(content);

        assertEquals(DEFAULT_CONTENT, new String(bytes));
    }

    @Test
    public void getsContentOfFileAsBytes() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile file = root.createFile(generateFileName(), DEFAULT_CONTENT);

        byte[] content = file.getContentAsBytes();

        assertEquals(DEFAULT_CONTENT, new String(content));
    }

    @Test
    public void getsContentOfFileAsString() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile file = root.createFile(generateFileName(), DEFAULT_CONTENT);

        String content = file.getContentAsString();

        assertEquals(DEFAULT_CONTENT, content);
    }

    @Test
    public void failsGetContentOfFolder() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile folder = root.createFolder(generateFolderName());

        thrown.expect(ForbiddenException.class);

        folder.getContent();
    }

    @Test
    public void failsGetContentOfFolderAsBytes() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile folder = root.createFolder(generateFolderName());

        thrown.expect(ForbiddenException.class);

        folder.getContentAsBytes();
    }

    @Test
    public void failsGetContentOfFolderAsString() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile folder = root.createFolder(generateFolderName());

        thrown.expect(ForbiddenException.class);

        folder.getContentAsString();
    }

    @Test
    public void updatesContentOfFileByStream() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile file = root.createFile(generateFileName(), DEFAULT_CONTENT);

        file.updateContent(new ByteArrayInputStream("updated content".getBytes()));

        assertEquals("updated content", file.getContentAsString());
    }

    @Test
    public void updatesContentOfFileByBytes() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile file = root.createFile(generateFileName(), DEFAULT_CONTENT);

        file.updateContent("updated content".getBytes());

        assertEquals("updated content", file.getContentAsString());
    }

    @Test
    public void updatesContentOfFileByString() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile file = root.createFile(generateFileName(), DEFAULT_CONTENT);

        file.updateContent("updated content");

        assertEquals("updated content", file.getContentAsString());
    }

    @Test
    public void failsUpdateContentOfLockedFileByStreamWithoutLockToken() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile file = root.createFile(generateFileName(), DEFAULT_CONTENT);
        file.lock(0);

        thrown.expect(ForbiddenException.class);

        file.updateContent(new ByteArrayInputStream("updated content".getBytes()));
    }

    @Test
    public void failsUpdateContentOfLockedFileByBytesWithoutLockToken() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile file = root.createFile(generateFileName(), DEFAULT_CONTENT);
        file.lock(0);

        thrown.expect(ForbiddenException.class);

        file.updateContent("updated content".getBytes());
    }

    @Test
    public void failsUpdateContentOfLockedFileByStringWithoutLockToken() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile file = root.createFile(generateFileName(), DEFAULT_CONTENT);
        file.lock(0);

        thrown.expect(ForbiddenException.class);

        file.updateContent("updated content");
    }

    @Test
    public void getsFileContentLength() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile file = root.createFile(generateFileName(), DEFAULT_CONTENT);
        assertEquals(DEFAULT_CONTENT_BYTES.length, file.getLength());
    }

    @Test
    public void folderContentLengthIsZero() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile folder = root.createFolder(generateFolderName());
        assertEquals(0, folder.getLength());
    }

    @Test
    public void copiesFile() throws Exception {
        VirtualFile root = getRoot();
        String fileName = generateFileName();
        VirtualFile file = root.createFile(fileName, DEFAULT_CONTENT);
        VirtualFile targetFolder = root.createFolder(generateFolderName());

        file.copyTo(targetFolder);

        VirtualFile copy = targetFolder.getChild(Path.fromString(fileName));
        assertNotNull(copy);
        assertEquals(file.getContentAsString(), copy.getContentAsString());
    }

    @Test
    public void copiesFileWithNewName() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile file = root.createFile(generateFileName(), DEFAULT_CONTENT);
        VirtualFile targetFolder = root.createFolder(generateFolderName());

        file.copyTo(targetFolder, "new name", false);

        VirtualFile copy = targetFolder.getChild(Path.fromString("new name"));
        assertNotNull(copy);
        assertEquals(file.getContentAsString(), copy.getContentAsString());
    }

    @Test
    public void copiesFileWithNewNameAndOverwritesExistedFile() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile file = root.createFile(generateFileName(), DEFAULT_CONTENT);
        VirtualFile targetFolder = root.createFolder(generateFolderName());
        targetFolder.createFile("existed_name", "existed content");

        file.copyTo(targetFolder, "existed_name", true);

        VirtualFile copy = targetFolder.getChild(Path.fromString("existed_name"));
        assertNotNull(copy);
        assertEquals(file.getContentAsString(), copy.getContentAsString());
    }

    @Test
    public void failsCopyFileWhenFileWithTheSameNameExistsInTargetFolder() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile file = root.createFile(generateFileName(), DEFAULT_CONTENT);
        VirtualFile targetFolder = root.createFolder(generateFolderName());
        VirtualFile existedFile = targetFolder.createFile("existed_name", "existed content");

        try {
            file.copyTo(targetFolder, "existed_name", false);
            thrown.expect(ConflictException.class);
        } catch (ConflictException e) {
            assertEquals("existed content", existedFile.getContentAsString());
        }
    }

    @Test
    public void movesFile() throws Exception {
        VirtualFile root = getRoot();
        String fileName = generateFileName();
        VirtualFile file = root.createFile(fileName, DEFAULT_CONTENT);
        Path filePath = file.getPath();
        VirtualFile targetFolder = root.createFolder(generateFolderName());

        file.moveTo(targetFolder);

        VirtualFile moved = targetFolder.getChild(Path.fromString(fileName));
        assertNotNull(moved);
        assertEquals(file.getContentAsString(), moved.getContentAsString());
        assertNull(root.getChild(filePath));
    }

    @Test
    public void movesFileWithNewName() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile file = root.createFile(generateFileName(), DEFAULT_CONTENT);
        Path filePath = file.getPath();
        VirtualFile targetFolder = root.createFolder(generateFolderName());

        file.moveTo(targetFolder, "new name", false, null);

        VirtualFile moved = targetFolder.getChild(Path.fromString("new name"));
        assertNotNull(moved);
        assertEquals(file.getContentAsString(), moved.getContentAsString());
        assertNull(root.getChild(filePath));
    }

    @Test
    public void movesFileWithNewNameAndOverwriteExistedFile() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile file = root.createFile(generateFileName(), DEFAULT_CONTENT);
        Path filePath = file.getPath();
        VirtualFile targetFolder = root.createFolder(generateFolderName());
        targetFolder.createFile("existed_name", DEFAULT_CONTENT);

        file.moveTo(targetFolder, "existed_name", true, null);

        VirtualFile moved = targetFolder.getChild(Path.fromString("existed_name"));
        assertNotNull(moved);
        assertEquals(file.getContentAsString(), moved.getContentAsString());
        assertNull(root.getChild(filePath));
    }

    @Test
    public void movesLockedFileWithLockToken() throws Exception {
        VirtualFile root = getRoot();
        String fileName = generateFileName();
        VirtualFile file = root.createFile(fileName, DEFAULT_CONTENT);
        String lockToken = file.lock(0);
        Path filePath = file.getPath();
        VirtualFile targetFolder = root.createFolder(generateFolderName());

        file.moveTo(targetFolder, null, false, lockToken);

        VirtualFile moved = targetFolder.getChild(Path.fromString(fileName));
        assertNotNull(moved);
        assertEquals(file.getContentAsString(), moved.getContentAsString());
        assertNull(root.getChild(filePath));
    }

    @Test
    public void failsMoveLockedFileWithoutLockToken() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile file = root.createFile(generateFileName(), DEFAULT_CONTENT);
        VirtualFile targetFolder = root.createFolder(generateFolderName());
        Path filePath = file.getPath();
        Path movedFilePath = targetFolder.getPath().newPath(file.getName());
        file.lock(0);

        try {
            file.moveTo(targetFolder);
            thrown.expect(ForbiddenException.class);
        } catch (ForbiddenException e) {
            assertEquals(file, root.getChild(filePath));
            assertNull(root.getChild(movedFilePath));
        }
    }

    @Test
    public void failsMoveLockedFileWhenLockTokenIsInvalid() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile file = root.createFile(generateFileName(), DEFAULT_CONTENT);
        VirtualFile targetFolder = root.createFolder(generateFolderName());
        Path filePath = file.getPath();
        Path movedFilePath = targetFolder.getPath().newPath(file.getName());
        String lockToken = file.lock(0);

        try {
            file.moveTo(targetFolder, null, false, invalidateLockToken(lockToken));
            thrown.expect(ForbiddenException.class);
        } catch (ForbiddenException e) {
            assertEquals(file, root.getChild(filePath));
            assertNull(root.getChild(movedFilePath));
        }
    }

    @Test
    public void failsMoveFileWhenTargetFolderContainsItemWithTheSameName() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile file = root.createFile(generateFileName(), DEFAULT_CONTENT);
        VirtualFile targetFolder = root.createFolder(generateFolderName());
        Path filePath = file.getPath();
        VirtualFile existedFile = targetFolder.createFile("existed_name", "existed content");

        try {
            file.moveTo(targetFolder, "existed_name", false, null);
            thrown.expect(ConflictException.class);
        } catch (ConflictException e) {
            assertEquals(file, root.getChild(filePath));
            assertEquals("existed content", existedFile.getContentAsString());
        }
    }

    @Test
    public void movesFolder() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile folder = root.createFolder(generateFolderName());
        VirtualFile file = folder.createFile(generateFileName(), DEFAULT_CONTENT);
        VirtualFile targetFolder = root.createFolder(generateFolderName());
        Path folderPath = folder.getPath();
        Path filePath = file.getPath();
        Path movedFolderPath = targetFolder.getPath().newPath(folder.getName());
        Path movedFilePath = movedFolderPath.newPath(file.getName());

        folder.moveTo(targetFolder);

        assertNotNull(root.getChild(movedFolderPath));
        assertNotNull(root.getChild(movedFilePath));
        assertEquals(DEFAULT_CONTENT, root.getChild(movedFilePath).getContentAsString());
        assertNull(root.getChild(folderPath));
        assertNull(root.getChild(filePath));
    }

    @Test
    public void failsMoveFolderThatContainsLockedFile() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile folder = root.createFolder(generateFolderName());
        VirtualFile file = folder.createFile(generateFileName(), DEFAULT_CONTENT);
        VirtualFile targetFolder = root.createFolder(generateFolderName());
        Path folderPath = folder.getPath();
        Path filePath = file.getPath();
        Path movedFolderPath = targetFolder.getPath().newPath(folder.getName());
        file.lock(0);

        try {
            folder.moveTo(targetFolder);
            thrown.expect(ForbiddenException.class);
        } catch (ForbiddenException e) {
            assertNull(root.getChild(movedFolderPath));
            assertEquals(folder, root.getChild(folderPath));
            assertEquals(file, root.getChild(filePath));
        }
    }

    @Test
    public void failsMoveFolderWhenTargetFolderContainsItemWithTheSameName() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile folder = root.createFolder(generateFolderName());
        VirtualFile file = folder.createFile(generateFileName(), DEFAULT_CONTENT);
        VirtualFile targetFolder = root.createFolder(generateFolderName());
        Path folderPath = folder.getPath();
        Path filePath = file.getPath();
        Path movedFolderPath = targetFolder.getPath().newPath(folder.getName());
        targetFolder.createFile("existed_name", "");

        try {
            folder.moveTo(targetFolder, "existed_name", false, null);
            thrown.expect(ConflictException.class);
        } catch (ConflictException e) {
            assertNull(root.getChild(movedFolderPath));
            assertEquals(folder, root.getChild(folderPath));
            assertEquals(file, root.getChild(filePath));
        }
    }

    @Test
    public void renamesFile() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile folder = root.createFolder(generateFolderName());
        VirtualFile file = folder.createFile(generateFileName(), DEFAULT_CONTENT);
        Path filePath = file.getPath();
        Path newPath = folder.getPath().newPath("new name");

        file.rename("new name");

        assertNull(root.getChild(filePath));
        assertEquals(file, root.getChild(newPath));
    }

    @Test
    public void renamesLockedFileWithLockToken() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile folder = root.createFolder(generateFolderName());
        VirtualFile file = folder.createFile(generateFileName(), DEFAULT_CONTENT);
        Path filePath = file.getPath();
        Path newPath = folder.getPath().newPath("new name");
        String lockToken = file.lock(0);

        file.rename("new name", lockToken);

        assertNull(root.getChild(filePath));
        assertEquals(file, root.getChild(newPath));
    }

    @Test
    public void failsRenameLockedFileWhenLockTokenIsInvalid() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile folder = root.createFolder(generateFolderName());
        VirtualFile file = folder.createFile(generateFileName(), DEFAULT_CONTENT);
        Path filePath = file.getPath();
        Path newPath = folder.getPath().newPath("new name");
        String lockToken = file.lock(0);

        try {
            file.rename("new name", invalidateLockToken(lockToken));
            thrown.expect(ForbiddenException.class);
        } catch (ForbiddenException e) {
            assertNotNull(root.getChild(filePath));
            assertNull(root.getChild(newPath));
        }
    }

    @Test
    public void renamesFolder() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile folder = root.createFolder(generateFolderName());
        VirtualFile file = folder.createFile(generateFileName(), DEFAULT_CONTENT);
        Path folderPath = folder.getPath();
        String fileName = file.getName();
        Path newPath = root.getPath().newPath("new name");

        folder.rename("new name");

        assertEquals(folder, root.getChild(newPath));
        assertEquals(file, root.getChild(newPath.newPath(fileName)));
        assertNull(root.getChild(folderPath));
        assertNull(root.getChild(folderPath.newPath(fileName)));
    }

    @Test
    public void failsRenamesFolderThatContainsLockedFile() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile folder = root.createFolder(generateFolderName());
        VirtualFile file = folder.createFile(generateFileName(), DEFAULT_CONTENT);
        Path folderPath = folder.getPath();
        String fileName = file.getName();
        Path newPath = root.getPath().newPath("new name");
        file.lock(0);

        try {
            folder.rename("new name");
            thrown.expect(ForbiddenException.class);
        } catch (ForbiddenException e) {
            assertNull(root.getChild(newPath));
            assertNull(root.getChild(newPath.newPath(fileName)));
            assertEquals(folder, root.getChild(folderPath));
            assertEquals(file, root.getChild(folderPath.newPath(fileName)));
        }
    }

    @Test
    public void deletesFile() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile folder = root.createFolder(generateFolderName());
        VirtualFile file = folder.createFile(generateFileName(), DEFAULT_CONTENT);
        Path filePath = file.getPath();

        file.delete();

        assertFalse(file.exists());
        assertNull(root.getChild(filePath));
    }

    @Test
    public void deletesLockedFileWithLockToken() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile folder = root.createFolder(generateFolderName());
        VirtualFile file = folder.createFile(generateFileName(), DEFAULT_CONTENT);
        Path filePath = file.getPath();
        String lockToken = file.lock(0);

        file.delete(lockToken);

        assertFalse(file.exists());
        assertNull(root.getChild(filePath));
    }

    @Test
    public void failsDeleteLockedFileWhenLockTokenIsInvalid() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile folder = root.createFolder(generateFolderName());
        VirtualFile file = folder.createFile(generateFileName(), DEFAULT_CONTENT);
        Path filePath = file.getPath();
        String lockToken = file.lock(0);

        try {
            file.delete(invalidateLockToken(lockToken));
            thrown.expect(ForbiddenException.class);
        } catch (ForbiddenException e) {
            assertTrue(file.exists());
            assertEquals(file, root.getChild(filePath));
        }
    }

    @Test
    public void deletesFolder() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile folder = root.createFolder(generateFolderName());
        VirtualFile file = folder.createFile(generateFileName(), DEFAULT_CONTENT);
        Path folderPath = folder.getPath();
        Path filePath = file.getPath();

        folder.delete();

        assertFalse(folder.exists());
        assertFalse(file.exists());
        assertNull(root.getChild(folderPath));
        assertNull(root.getChild(filePath));
    }

    @Test
    public void failsDeleteFolderThatContainsLockedFile() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile folder = root.createFolder(generateFolderName());
        VirtualFile file = folder.createFile(generateFileName(), DEFAULT_CONTENT);
        Path folderPath = folder.getPath();
        Path filePath = file.getPath();
        file.lock(0);

        try {
            folder.delete();
            thrown.expect(ForbiddenException.class);
        } catch (ForbiddenException e) {
            assertTrue(folder.exists());
            assertTrue(file.exists());
            assertEquals(folder, root.getChild(folderPath));
            assertEquals(file, root.getChild(filePath));
        }
    }

    @Test
    public void zipsFolder() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile folder = root.createFolder(generateFolderName());
        VirtualFile folderA = folder.createFolder(generateFolderName());
        VirtualFile folderB = folder.createFolder(generateFolderName());
        VirtualFile fileA = folder.createFile(generateFileName(), DEFAULT_CONTENT);
        VirtualFile fileB = folder.createFile(generateFileName(), DEFAULT_CONTENT);
        VirtualFile folderAA = folderA.createFolder(generateFolderName());
        VirtualFile folderBB = folderB.createFolder(generateFolderName());
        VirtualFile fileAA = folderA.createFile(generateFileName(), DEFAULT_CONTENT);
        VirtualFile fileBB = folderB.createFile(generateFileName(), DEFAULT_CONTENT);
        Set<String> expectedItemsInZip = newHashSet(folderA.getPath().subPath(folder.getPath()).toString() + "/",
                                                    folderB.getPath().subPath(folder.getPath()).toString() + "/",
                                                    fileA.getPath().subPath(folder.getPath()).toString(),
                                                    fileB.getPath().subPath(folder.getPath()).toString(),
                                                    folderAA.getPath().subPath(folder.getPath()).toString() + "/",
                                                    folderBB.getPath().subPath(folder.getPath()).toString() + "/",
                                                    fileAA.getPath().subPath(folder.getPath()).toString(),
                                                    fileBB.getPath().subPath(folder.getPath()).toString());
        InputStream zip = folder.zip();
        checkZipItems(expectedItemsInZip, new ZipInputStream(zip));
    }

    @Test
    public void failsZipFile() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile file = root.createFile(generateFileName(), DEFAULT_CONTENT);

        thrown.expect(ForbiddenException.class);

        file.zip();
    }

    @Test
    public void unzipsInFolder() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile folder = root.createFolder(generateFolderName());
        byte[] zip = createTestZipArchive();
        folder.unzip(new ByteArrayInputStream(zip), false, 0);

        assertNotNull(folder.getChild(Path.fromString("arc-root/folderA")));
        assertNotNull(folder.getChild(Path.fromString("arc-root/folderB")));
        assertNotNull(folder.getChild(Path.fromString("arc-root/folderC")));

        assertNotNull(folder.getChild(Path.fromString("arc-root/folderA/fileA.txt")));
        assertNotNull(folder.getChild(Path.fromString("arc-root/folderB/fileB.txt")));
        assertNotNull(folder.getChild(Path.fromString("arc-root/folderC/fileC.txt")));

        assertEquals(DEFAULT_CONTENT, folder.getChild(Path.fromString("arc-root/folderA/fileA.txt")).getContentAsString());
        assertEquals(DEFAULT_CONTENT, folder.getChild(Path.fromString("arc-root/folderB/fileB.txt")).getContentAsString());
        assertEquals(DEFAULT_CONTENT, folder.getChild(Path.fromString("arc-root/folderC/fileC.txt")).getContentAsString());
    }

    @Test
    public void unzipsInFolderAndOverWritesExistedFiles() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile folder = root.createFolder(generateFolderName());
        VirtualFile folderA = folder.createFolder("arc-root/folderA");
        VirtualFile folderB = folder.createFolder("arc-root/folderB");
        VirtualFile folderX = folder.createFolder("folderX");
        folderA.createFile("fileA.txt", "should be updated");
        folderX.createFile("fileX.txt", "untouched");

        byte[] zip = createTestZipArchive();
        folder.unzip(new ByteArrayInputStream(zip), true, 0);

        assertNotNull(folder.getChild(Path.fromString("arc-root/folderA")));
        assertNotNull(folder.getChild(Path.fromString("arc-root/folderB")));
        assertNotNull(folder.getChild(Path.fromString("arc-root/folderC")));
        assertNotNull(folder.getChild(Path.fromString("folderX")));

        assertNotNull(folder.getChild(Path.fromString("arc-root/folderA/fileA.txt")));
        assertNotNull(folder.getChild(Path.fromString("arc-root/folderB/fileB.txt")));
        assertNotNull(folder.getChild(Path.fromString("arc-root/folderC/fileC.txt")));
        assertNotNull(folder.getChild(Path.fromString("folderX/fileX.txt")));

        assertEquals(DEFAULT_CONTENT, folder.getChild(Path.fromString("arc-root/folderA/fileA.txt")).getContentAsString());
        assertEquals(DEFAULT_CONTENT, folder.getChild(Path.fromString("arc-root/folderB/fileB.txt")).getContentAsString());
        assertEquals(DEFAULT_CONTENT, folder.getChild(Path.fromString("arc-root/folderC/fileC.txt")).getContentAsString());
        assertEquals("untouched", folder.getChild(Path.fromString("folderX/fileX.txt")).getContentAsString());
    }

    @Test
    public void failsUnzipInFolderWhenFileExistsAndOverwritingDisabled() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile folder = root.createFolder(generateFolderName());
        VirtualFile folderA = folder.createFolder("arc-root/folderA");
        folderA.createFile("fileA.txt", "conflict file");

        byte[] zip = createTestZipArchive();
        try {
            folder.unzip(new ByteArrayInputStream(zip), false, 0);
            thrown.expect(ConflictException.class);
        } catch (ConflictException e) {
            assertNotNull(folder.getChild(Path.fromString("arc-root/folderA")));
            assertNotNull(folder.getChild(Path.fromString("arc-root/folderA/fileA.txt")));

            assertEquals("conflict file", folder.getChild(Path.fromString("arc-root/folderA/fileA.txt")).getContentAsString());
        }
    }

    @Test
    public void failsUnzipInFolderWhenFolderContainsLockedFile() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile folder = root.createFolder(generateFolderName());
        VirtualFile folderA = folder.createFolder("arc-root/folderA");
        VirtualFile fileAA = folderA.createFile("fileA.txt", "locked file");
        fileAA.lock(0);

        byte[] zip = createTestZipArchive();
        try {
            folder.unzip(new ByteArrayInputStream(zip), false, 0);
            thrown.expect(ForbiddenException.class);
        } catch (ForbiddenException e) {
            assertNotNull(folder.getChild(Path.fromString("arc-root/folderA")));
            assertNotNull(folder.getChild(Path.fromString("arc-root/folderA/fileA.txt")));

            assertEquals("locked file", folder.getChild(Path.fromString("arc-root/folderA/fileA.txt")).getContentAsString());
        }
    }

    @Test
    public void unzipsInFolderAndSkipsRootFolderFromArchive() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile folder = root.createFolder(generateFolderName());
        byte[] zip = createTestZipArchive();
        folder.unzip(new ByteArrayInputStream(zip), false, 1);

        assertNotNull(folder.getChild(Path.fromString("folderA")));
        assertNotNull(folder.getChild(Path.fromString("folderB")));
        assertNotNull(folder.getChild(Path.fromString("folderC")));

        assertNotNull(folder.getChild(Path.fromString("folderA/fileA.txt")));
        assertNotNull(folder.getChild(Path.fromString("folderB/fileB.txt")));
        assertNotNull(folder.getChild(Path.fromString("folderC/fileC.txt")));

        assertEquals(DEFAULT_CONTENT, folder.getChild(Path.fromString("folderA/fileA.txt")).getContentAsString());
        assertEquals(DEFAULT_CONTENT, folder.getChild(Path.fromString("folderB/fileB.txt")).getContentAsString());
        assertEquals(DEFAULT_CONTENT, folder.getChild(Path.fromString("folderC/fileC.txt")).getContentAsString());
    }

    @Test
    public void failsUnzipInFile() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile file = root.createFile(generateFileName(), DEFAULT_CONTENT);
        byte[] zip = createTestZipArchive();
        thrown.expect(ForbiddenException.class);
        file.unzip(new ByteArrayInputStream(zip), false, 0);
    }

    @Test
    public void locksFile() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile file = root.createFile(generateFileName(), "");
        file.lock(0);
        assertTrue(file.isLocked());
    }

    @Test
    public void failsLockFolder() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile folder = root.createFolder(generateFolderName());
        try {
            folder.lock(0);
            thrown.expect(ForbiddenException.class);
        } catch (ForbiddenException e) {
            assertFalse(folder.isLocked());
        }
    }

    @Test
    public void unlocksFile() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile file = root.createFile(generateFileName(), "");
        String lockToken = file.lock(0);
        file.unlock(lockToken);
        assertFalse(file.isLocked());
    }

    @Test
    public void failsUnlockFileWhenLockTokenIsInvalid() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile file = root.createFile(generateFileName(), "");
        String lockToken = file.lock(0);
        try {
            file.unlock(invalidateLockToken(lockToken));
            thrown.expect(ForbiddenException.class);
        } catch (ForbiddenException e) {
            assertTrue(file.isLocked());
        }
    }

    @Test
    public void createsFileWithStringContent() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile file = root.createFile("new file", DEFAULT_CONTENT);
        assertEquals(file, root.getChild(Path.fromString("new file")));
        assertEquals(DEFAULT_CONTENT, file.getContentAsString());
    }

    @Test
    public void createsFileWithBytesContent() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile file = root.createFile("new file", DEFAULT_CONTENT_BYTES);
        assertEquals(file, root.getChild(Path.fromString("new file")));
        assertEquals(DEFAULT_CONTENT, file.getContentAsString());
    }

    @Test
    public void createsFileWithStreamContent() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile file = root.createFile("new file", new ByteArrayInputStream(DEFAULT_CONTENT_BYTES));
        assertEquals(file, root.getChild(Path.fromString("new file")));
        assertEquals(DEFAULT_CONTENT, file.getContentAsString());
    }

    @Test
    public void failsCreateFileWhenNameOfNewFileConflictsWithExistedFile() throws Exception {
        VirtualFile root = getRoot();
        root.createFile("file", DEFAULT_CONTENT);
        thrown.expect(ConflictException.class);
        root.createFile("file", DEFAULT_CONTENT);
    }

    @Test
    public void failsCreateFileWhenParenIsNotFolder() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile parent = root.createFile("parent", "");
        thrown.expect(ForbiddenException.class);
        parent.createFile("file", DEFAULT_CONTENT);
    }

    @Test
    public void createsFolder() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile folder = root.createFolder("new folder");
        assertEquals(folder, root.getChild(Path.fromString("new folder")));
    }

    @Test
    public void createsFolderHierarchy() throws Exception {
        VirtualFile root = getRoot();
        root.createFolder("a/b");
        assertNotNull(root.getChild(Path.fromString("a")));
        assertNotNull(root.getChild(Path.fromString("a/b")));
    }

    @Test
    public void convertsToIoFile() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile file = root.createFile(generateFileName(), "");
        assertNull(file.toIoFile());
    }

    @Test
    public void comparesFileAndFolder() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile file = root.createFile(generateFileName(), "");
        VirtualFile folder = root.createFolder(generateFolderName());
        assertTrue(folder.compareTo(file) < 0);
    }

    @Test
    public void comparesTwoFiles() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile fileA = root.createFile("a", "");
        VirtualFile fileB = root.createFile("b", "");
        assertTrue(fileA.compareTo(fileB) < 0);
    }

    @Test
    public void comparesTwoFolders() throws Exception {
        VirtualFile root = getRoot();
        VirtualFile folderA = root.createFolder("a");
        VirtualFile folderB = root.createFolder("b");
        assertTrue(folderA.compareTo(folderB) < 0);
    }

    private VirtualFile getRoot() {
        return mountPoint.getRoot();
    }

    private String generateFileName() {
        return NameGenerator.generate("file", 8);
    }

    private String generateFolderName() {
        return NameGenerator.generate("folder", 8);
    }

    private String countMd5Sum(VirtualFile file) throws Exception {
        return ByteSource.wrap(file.getContentAsBytes()).hash(Hashing.md5()).toString();
    }

    private String invalidateLockToken(String lockToken) {
        return new StringBuilder(lockToken).reverse().toString();
    }

    private void checkZipItems(Set<String> expected, ZipInputStream zip) throws Exception {
        ZipEntry zipEntry;
        while ((zipEntry = zip.getNextEntry()) != null) {
            String name = zipEntry.getName();
            zip.closeEntry();
            assertTrue(String.format("Unexpected entry %s in zip", name), expected.remove(name));
        }
        zip.close();
        assertTrue(String.format("Expected but were not found in zip %s", expected), expected.isEmpty());
    }

    private byte[] createTestZipArchive() throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ZipOutputStream zipOut = new ZipOutputStream(bout);
        zipOut.putNextEntry(new ZipEntry("/arc-root/"));
        zipOut.putNextEntry(new ZipEntry("/arc-root/folderA/"));
        zipOut.putNextEntry(new ZipEntry("/arc-root/folderB/"));
        zipOut.putNextEntry(new ZipEntry("/arc-root/folderC/"));
        zipOut.putNextEntry(new ZipEntry("/arc-root/folderA/fileA.txt"));
        zipOut.write(DEFAULT_CONTENT_BYTES);
        zipOut.putNextEntry(new ZipEntry("/arc-root/folderB/fileB.txt"));
        zipOut.write(DEFAULT_CONTENT_BYTES);
        zipOut.putNextEntry(new ZipEntry("/arc-root/folderC/fileC.txt"));
        zipOut.write(DEFAULT_CONTENT_BYTES);
        zipOut.close();
        return bout.toByteArray();
    }
}