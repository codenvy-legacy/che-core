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
package org.eclipse.che.ide.api.parts;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;

import org.vectomatic.dom.svg.ui.SVGResource;

/** @author Nikolay Zamosenchuk */
public interface PartStackUIResources extends ClientBundle {

    interface PartStackCss extends CssResource {

        @ClassName("ide-PartStack-Tab")
        String idePartStackTab();

        @ClassName("ide-Base-Part-Toolbar")
        String ideBasePartToolbar();

        @ClassName("ide-PartStack-Content")
        String idePartStackContent();

        @ClassName("ide-Base-Part-Toolbar-Bottom")
        String idePartStackToolbarBottom();

        @ClassName("ide-Base-Part-Toolbar-Separator")
        String idePartStackToolbarSeparator();

        @ClassName("ide-Base-Part-Toolbar-Bottom-Icon")
        String idePartStackToolbarBottomIcon();

        @ClassName("ide-Base-Part-Toolbar-Bottom-Button")
        String idePartStackToolbarBottomButton();

        @ClassName("ide-Base-Part-Toolbar-Bottom-Button-Right")
        String idePartStackToolbarBottomButtonRight();

        @ClassName("ide-PartStack-Tool-Tab-selected")
        String idePartStackToolTabSelected();

        @ClassName("ide-Base-Part-Title-Label")
        String ideBasePartTitleLabel();

        @ClassName("ide-PartStack-Tab-Line-Warning")
        String lineWarning();

        @ClassName("ide-PartStack-Tab-Line-Error")
        String lineError();

        @ClassName("ide-part-stack-header-menu-button")
        String headerMenuButton();

        String leftTabs();

        String tabBordersDefault();

        String selectedRightOrLeftTab();

        String selectEditorTab();

        String opacity();

        String activeTabTextColor();

        String listItemPanel();

        String listShownButtonBackground();

        String unSelectEditorBorder();

        String rightTabs();
    }

    @Source({"partstack.css", "org/eclipse/che/ide/api/ui/style.css"})
    PartStackCss partStackCss();

    ImageResource close();

    @Source("squiggle.gif")
    ImageResource squiggle();

    @Source("squiggle-warning.png")
    ImageResource squiggleWarning();

    @Source("minimize.svg")
    SVGResource minimize();

    @Source("arrow.svg")
    SVGResource arrow();
}
