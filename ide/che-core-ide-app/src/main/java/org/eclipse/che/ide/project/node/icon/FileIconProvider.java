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
package org.eclipse.che.ide.project.node.icon;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import org.eclipse.che.ide.api.filetypes.FileType;
import org.eclipse.che.ide.api.filetypes.FileTypeRegistry;
import org.vectomatic.dom.svg.ui.SVGResource;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Resolve icon based on registered file type.
 *
 * @author Vlad Zhukovskiy
 */
@Singleton
public class FileIconProvider implements NodeIconProvider {

    private final FileTypeRegistry fileTypeRegistry;
    private final FileType         unknownFileType;

    @Inject
    public FileIconProvider(FileTypeRegistry fileTypeRegistry,
                            @Named("defaultFileType") FileType unknownFileType) {
        this.fileTypeRegistry = fileTypeRegistry;
        this.unknownFileType = unknownFileType;
    }

    @Override
    public SVGResource getIcon(String fileName) {
        final String extension = getFileExtension(fileName);

        if (Strings.isNullOrEmpty(extension)) {
            return null;
        }

        FileType fileType = fileTypeRegistry.getFileTypeByExtension(extension);

        if (fileType == unknownFileType || fileType.getSVGImage() == null) {
            return null;
        }

        return fileType.getSVGImage();
    }

    public static String getFileExtension(String fullName) {
        checkNotNull(fullName);
        int dotIndex = fullName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fullName.substring(dotIndex + 1);
    }
}
