// Copyright 2012 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.eclipse.che.ide;

import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.CssResource.NotStrict;
import com.google.gwt.resources.client.ImageResource;

import org.eclipse.che.ide.api.parts.PartStackUIResources;
import org.eclipse.che.ide.menu.MenuResources;
import org.eclipse.che.ide.notification.NotificationResources;
import org.eclipse.che.ide.projecttype.wizard.ProjectWizardResources;
import org.eclipse.che.ide.ui.DialogBoxResources;
import org.eclipse.che.ide.ui.buttonLoader.ButtonLoaderResources;
import org.eclipse.che.ide.ui.cellview.CellTableResources;
import org.eclipse.che.ide.ui.cellview.CellTreeResources;
import org.eclipse.che.ide.ui.cellview.DataGridResources;
import org.eclipse.che.ide.ui.dropdown.DropDownHeaderWidgetImpl;
import org.eclipse.che.ide.ui.list.CategoriesList;
import org.eclipse.che.ide.ui.list.SimpleList;
import org.eclipse.che.ide.ui.tree.Tree;
import org.eclipse.che.ide.ui.zeroclipboard.ZeroClipboardResources;
import org.vectomatic.dom.svg.ui.SVGResource;

/**
 * Interface for resources, e.g., css, images, text files, etc.
 * <p/>
 * Tree.Resources,
 * Editor.Resources,
 * LineNumberRenderer.Resources,
 * EditableContentArea.Resources,
 * PartStackUIResources,
 */
public interface Resources extends Tree.Resources,
                                   PartStackUIResources,
                                   SimpleList.Resources,
                                   MenuResources,
                                   DialogBoxResources,
                                   ZeroClipboardResources,
                                   NotificationResources,
                                   DataGridResources,
                                   CellTableResources,
                                   CellTreeResources,
                                   CategoriesList.Resources,
                                   DropDownHeaderWidgetImpl.Resources,
                                   ButtonLoaderResources,
                                   ProjectWizardResources {

    /** Interface for css resources. */
    interface CoreCss extends CssResource {
        String simpleListContainer();

        String mainText();

        // wizard's styles
        String mainFont();

        String mainBoldFont();

        String defaultFont();

        String warningFont();

        String errorFont();

        String greyFontColor();

        String cursorPointer();

        String line();

        String editorFullScreen();

        String editorFullScreenSvgDown();

        String createWsTagsPopup();

        String tagsPanel();
    }

    @Source({"Core.css", "org/eclipse/che/ide/ui/constants.css", "org/eclipse/che/ide/api/ui/style.css"})
    @NotStrict
    CoreCss coreCss();

    @Source("workspace/recipe.svg")
    SVGResource recipe();

    @Source("part/projectexplorer/project_explorer.png")
    ImageResource projectExplorer();

    @Source("part/projectexplorer/project-closed.png")
    ImageResource projectClosed();

    @Source("actions/newProject.svg")
    SVGResource newProject();

    @Source("actions/showHiddenFiles.svg")
    SVGResource showHiddenFiles();

    @Source("wizard/arrow.svg")
    SVGResource wizardArrow();

    @Source("extension/extention.png")
    ImageResource extension();

    @Source("texteditor/save-all.png")
    ImageResource saveAll();

    @Source("texteditor/open-list.png")
    ImageResource listOpenedEditors();

    @Source("texteditor/multi-file-icon.svg")
    SVGResource multiFileIcon();

    @Source("xml/xml.svg")
    SVGResource xmlFile();

    @Source("about/logo.png")
    ImageResource logo();

    @Source("console/clear.svg")
    SVGResource clear();

    @Source("actions/about.svg")
    SVGResource about();

    @Source("actions/help.svg")
    SVGResource help();

    @Source("actions/find-actions.svg")
    SVGResource findActions();

    @Source("actions/undo.svg")
    SVGResource undo();

    @Source("actions/redo.svg")
    SVGResource redo();

    @Source("actions/project-configuration.svg")
    SVGResource projectConfiguration();

    @Source("actions/forums.svg")
    SVGResource forums();

    @Source("actions/feature-vote.svg")
    SVGResource featureVote();

    @Source("actions/close-project.svg")
    SVGResource closeProject();

    @Source("actions/delete.svg")
    SVGResource delete();

    @Source("actions/cut.svg")
    SVGResource cut();

    @Source("actions/copy.svg")
    SVGResource copy();

    @Source("actions/paste.svg")
    SVGResource paste();

    @Source("actions/new-resource.svg")
    SVGResource newResource();

    @Source("actions/navigate-to-file.svg")
    SVGResource navigateToFile();

    @Source("actions/open-project.svg")
    SVGResource openProject();

    @Source("actions/save.svg")
    SVGResource save();

    @Source("actions/preferences.svg")
    SVGResource preferences();

    @Source("actions/rename.svg")
    SVGResource rename();

    @Source("actions/format.svg")
    SVGResource format();

    @Source("actions/import.svg")
    SVGResource importProject();

    @Source("actions/importProjectFromLocation.svg")
    SVGResource importProjectFromLocation();

    @Source("actions/importGroup.svg")
    SVGResource importProjectGroup();

    @Source("actions/settings.svg")
    SVGResource settings();

    @Source("actions/upload-file.svg")
    SVGResource uploadFile();

    @Source("actions/upload-folder.svg")
    SVGResource uploadFolder();

    @Source("actions/zip-folder.svg")
    SVGResource downloadZip();

    @Source("actions/resize-icon.svg")
    SVGResource fullscreen();

    @Source("actions/refresh.svg")
    SVGResource refresh();

    @Source("workspace/perspectives/general/codenvy-placeholder.png")
    ImageResource codenvyPlaceholder();

    @Source("defaulticons/file.svg")
    SVGResource defaultFile();

    @Source("defaulticons/default.svg")
    SVGResource defaultIcon();

    @Source("defaulticons/folder.svg")
    SVGResource defaultFolder();

    @Source("defaulticons/project.svg")
    SVGResource defaultProject();

    @Source("defaulticons/image-icon.svg")
    SVGResource defaultImage();

    @Source("defaulticons/md.svg")
    SVGResource mdFile();

    @Source("defaulticons/json.svg")
    SVGResource jsonFile();

    @Source("part/outline/no-outline.svg")
    SVGResource noOutline();

    @Source("part/project-explorer-part-icon.svg")
    SVGResource projectExplorerPartIcon();

    @Source("part/events-part-icon.svg")
    SVGResource eventsPartIcon();

    @Source("part/output-part-icon.svg")
    SVGResource outputPartIcon();

    @Source("part/outline-part-icon.svg")
    SVGResource outlinePartIcon();
}
