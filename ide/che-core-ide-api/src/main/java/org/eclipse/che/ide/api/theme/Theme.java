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
package org.eclipse.che.ide.api.theme;

/**
 * Theme is a collection of colors, fonts and supporting data that may be used by plugins to help provide uniform look and feel to their
 * components. <b>Note:</b><br/>
 * In this interface color means CSS color i.e : #eeeeee, rgb(1,1,1), rgba(1,1,1,1), red etc.
 *
 * @author Evgen Vidolob
 */
public interface Theme {

    /**
     * @return the theme id
     */
    String getId();

    /**
     * @return the description of the theme
     */
    String getDescription();

    /**
     * This color used in toolbar for highlight hovered items
     *
     * @return the color
     */
    String getHoverBackgroundColor();

    /**
     * Background color for an item selected with the keyboard
     *
     * @return the color
     */
    String getKeyboardSelectionBackgroundColor();

    /**
     * Background color for selected items in CellWidgets, menu, toolbar
     *
     * @return the color
     */
    String getSelectionBackground();

    /**
     * Background color for inactive(not selected part button)
     *
     * @return the color
     */
    String getInactiveTabBackground();

    /**
     * Border color for inactive(not selected part button)
     *
     * @return the color
     */
    String getInactiveTabBorderColor();

    /**
     * Background color for active (selected) part button
     *
     * @return the color
     */
    String getActiveTabBackground();

    /**
     * Border color for active (selected) part button
     *
     * @return the color
     */
    String getActiveTabBorderColor();

    /**
     * Text color for active (selected) part button
     *
     * @return the color
     */
    String getActiveTabTextColor();

    /**
     * Text shadow for active tab.
     *
     * @return text shadow
     */
    String getActiveTabTextShadow();

    /**
     * Icon color in active (selected) tab.
     *
     * @return color for icon
     */
    String getActiveTabIconColor();

    /**
     * Text color for part button.
     *
     * @return the color
     */
    String getTabTextColor();

    /**
     * Text color for hovered part button.
     *
     * @return the color
     */
    String getHoveredTabTextColor();

    /**
     * Background color of part stack panel(where placed part button)
     *
     * @return the color
     */
    String getTabsPanelBackground();

    /**
     * Border color of the tab(part button)
     *
     * @return the color
     */
    String getTabBorderColor();

    /**
     * Color to underline active tab
     *
     * @return
     */
    String getTabUnderlineColor();

    /**
     * Background color of the tab(part button)
     *
     * @return
     */
    String getPartBackground();

    /**
     * Background color of the part toolbar panel
     *
     * @return the color
     */
    String getPartToolbar();

    /**
     * Background color for selected (active) part toolbar
     *
     * @return the color
     */
    String getPartToolbarActive();

    /**
     * Shadow color(css box-shadow property) of the part toolbar
     *
     * @return the color
     */
    String getPartToolbarShadow();

    /**
     * First top line's color of the part toolbar separator.
     *
     * @return the color
     */
    String getPartToolbarSeparatorTopColor();

    /**
     * Second bottom line's color of the part toolbar separator.
     *
     * @return the color
     */
    String getPartToolbarSeparatorBottomColor();

    /**
     * Color of the main font
     *
     * @return the color
     */
    String getMainFontColor();

    /**
     * Background color of the {@link com.google.gwt.user.client.ui.RadioButton}
     *
     * @return the color
     */
    String getRadioButtonBackgroundColor();

    /**
     * Background color of the disabled menu item
     *
     * @return the color
     */
    String getDisabledMenuColor();

    /**
     * Background color of the {@link com.google.gwt.user.client.ui.DialogBox}
     *
     * @return the color
     */
    String getDialogContentBackground();

    /**
     * Background color of the top line of {@link com.google.gwt.user.client.ui.Button} <br />
     * Button has gradient in background, so this color is star color
     *
     * @return the color
     */
    String getButtonTopColor();

    /**
     * Background color of the {@link com.google.gwt.user.client.ui.Button} Button has gradient in background, so this color is main color
     *
     * @return the color
     */
    String getButtonColor();

    /**
     * Color of the social button for sharing factory.
     *
     * @return the color
     */
    String getSocialButtonColor();

    /**
     * Background for editor panel ( place where the editor will be placed )
     *
     * @return color for editor panel
     */
    String getEditorPanelBackgroundColor();

    /**
     * Background color of the editor
     *
     * @return the color
     */
    String getEditorBackgroundColor();

    /**
     * Color for highlighted line in editor(where cursor placed)
     *
     * @return the color
     */
    String getEditorCurrentLineColor();

    /**
     * Main font color in the editor
     *
     * @return the color
     */
    String getEditorDefaultFontColor();

    /**
     * Editor selection background color.
     *
     * @return the color
     */
    String getEditorSelectionColor();

    /**
     * Editor inactive selection color(if focus not in browser)
     *
     * @return the color
     */
    String getEditorInactiveSelectionColor();

    /**
     * Color of the editor cursor
     *
     * @return the color
     */
    String getEditorCursorColor();

    /**
     * Background color of the gutter (left or right vertical panels in editor)
     *
     * @return the color
     */
    String getEditorGutterColor();

    /**
     * Color of key word token, produced by Codemirror parser
     *
     * @return the color
     */
    String getEditorKeyWord();

    /**
     * Color of atom token, produced by Codemirror parser
     *
     * @return the color
     */
    String getEditorAtom();

    /**
     * Color of number token, produced by Codemirror parser
     *
     * @return the color
     */
    String getEditorNumber();

    /**
     * Color of def token, produced by Codemirror parser
     *
     * @return the color
     */
    String getEditorDef();

    /**
     * Color of variable token, produced by Codemirror parser
     *
     * @return the color
     */
    String getEditorVariable();

    /**
     * Color of variable2 token, produced by Codemirror parser
     *
     * @return the color
     */
    String getEditorVariable2();

    /**
     * Color of property token, produced by Codemirror parser
     *
     * @return the color
     */
    String getEditorProperty();

    /**
     * Color of operator token, produced by Codemirror parser
     *
     * @return the color
     */
    String getEditorOperator();

    /**
     * Color of comment token, produced by Codemirror parser
     *
     * @return the color
     */
    String getEditorComment();

    /**
     * Color of string token, produced by Codemirror parser
     *
     * @return the color
     */
    String getEditorString();

    /**
     * Color of meta token, produced by Codemirror parser
     *
     * @return the color
     */
    String getEditorMeta();

    /**
     * Color of error token, produced by Codemirror parser
     *
     * @return the color
     */
    String getEditorError();

    /**
     * Color of builtin token, produced by Codemirror parser
     *
     * @return the color
     */
    String getEditorBuiltin();

    /**
     * Color of tag token, produced by Codemirror parser
     *
     * @return the color
     */
    String getEditorTag();

    /**
     * Color of attribute token, produced by Codemirror parser
     *
     * @return the color
     */
    String getEditorAttribute();

    /**
     * Color of string2 token, produced by Codemirror parser
     *
     * @return the color
     */
    String getEditorString2();

    /**
     * Background color of completion popup.
     *
     * @return the color
     */
    String getCompletionPopupBackgroundColor();

    /**
     * Border color of completion popup.
     *
     * @return the color
     */
    String getCompletionPopupBorderColor();

    /**
     * Background color of the window widget(used in new project wizard)
     *
     * @return the color
     */
    String getWindowContentBackground();

    /**
     * Background color of the window header (used in new project wizard)
     *
     * @return the color
     */
    String getWindowHeaderBackground();

    /**
     * Color of the line separating elements in Window (for footer).
     *
     * @return the color
     */
    String getWindowSeparatorColor();

    /**
     * New Project wizard steps background color(used in new project wizard, left vertical panel)
     *
     * @return the color
     */
    String getWizardStepsColor();

    /**
     * Border color of the steps panel in new project wizard
     *
     * @return the color
     */
    String getWizardStepsBorderColor();

    /**
     * Color of the Factory link.
     *
     * @return the color
     */
    String getFactoryLinkColor();

    /**
     * Font color for welcome page text
     *
     * @return the color
     */
    String getWelcomeFontColor();

    /**
     * Font color for group captions on view (Example, Share Factory view).
     *
     * @return the color
     */
    String getCaptionFontColor();

    /**
     * Font color for console text
     *
     * @return the color
     */
    String getConsolePanelColor();

    /**
     * Font color for status panel
     *
     * @return the color
     */
    String getStatusPanelColor();

    /**
     * Background color for odd rows in Cell Widgets
     *
     * @return the color
     */
    String getCellOddRowColor();

    /**
     * Background color for odd rows in Cell Widgets
     *
     * @return the color
     */
    String getCellOddEvenColor();

    /**
     * Background color for keyboard selected rows in Cell Widgets
     *
     * @return the color
     */
    String getCellKeyboardSelectedRowColor();

    /**
     * Background color for hovered rows in Cell Widgets
     *
     * @return
     */
    String getCellHoveredRow();

    /**
     * Background color of menu items
     *
     * @return the color
     */
    String getMainMenuBkgColor();

    /**
     * Background color of selected menu items
     *
     * @return the color
     */
    String getMainMenuSelectedBkgColor();

    /**
     * Border color of selected menu items
     *
     * @return the color
     */
    String getMainMenuSelectedBorderColor();

    /**
     * Font color for menu item text
     *
     * @return the color
     */
    String getMainMenuFontColor();

    /**
     * Font color for menu item hover text
     *
     * @return the color
     */
    String getMainMenuFontHoverColor();

    /**
     * Font color for menu item selected text
     *
     * @return the color
     */
    String getMainMenuFontSelectedColor();

    String getNotableButtonTopColor();

    String getNotableButtonColor();

    /**
     * @return the color of border shadow
     */
    String getTabBorderShadow();

    /**
     * @return the color of button text-shadow
     */
    String getButtonTextShadow();

    /**
     * @return the color of tree file text
     */
    String getTreeTextFileColor();

    /**
     * @return the color of tree folder text
     */
    String getTreeTextFolderColor();

    /**
     * @return the color of tree text-shadow
     */
    String getTreeTextShadow();

    /**
     * @return the color of tree icon file
     */
    String getTreeIconFileColor();

    /**
     * @return the color of button text if hover
     */
    String getButtonHoverTextColor();

    /**
     * @return the color of button if hover
     */
    String getButtonHoverColor();

    /**
     * The color of the action group shadow.
     *
     * @return {@link String} color
     */
    String getToolbarActionGroupShadowColor();

    /**
     * The color of the action group background.
     *
     * @return {@link String} color
     */
    String getToolbarActionGroupBackgroundColor();

    /**
     * The color of the action group border.
     *
     * @return {@link String} color
     */
    String getToolbarActionGroupBorderColor();

    /**
     * The background image of the toolbar.
     *
     * @return {@link String} background image
     */
    String getToolbarBackgroundImage();

    /**
     * The color of the toolbar background.
     *
     * @return {@link String} color
     */
    String getToolbarBackgroundColor();

    /**
     * The color of the toolbar icons.
     *
     * @return {@link String} color
     */
    String getToolbarIconColor();

    /**
     * The color of the toolbar icons if hover.
     *
     * @return {@link String} hover color
     */
    String getToolbarHoverIconColor();

    /**
     * The color of the tooltip background.
     *
     * @return {@link String} color
     */
    String getTooltipBackgroundColor();

    /**
     * The background color of the perspective switcher if checked.
     *
     * @return {@link String} color
     */
    String getPerspectiveSwitcherBackgroundColor();

    /**
     * The icon color for the selectCommandAction.
     *
     * @return {@link String} color
     */
    String getSelectCommandActionIconColor();

    /**
     * The background color for selectCommandAction icon.
     *
     * @return {@link String} color
     */
    String getSelectCommandActionIconBackgroundColor();

    /**
     * The command action color.
     *
     * @return {@link String} color
     */
    String getSelectCommandActionColor();

    /**
     * The command action hover color.
     *
     * @return {@link String} color
     */
    String getSelectCommandActionHoverColor();

    /**
     * @return the progress icon color
     */
    String getProgressColor();

    /**
     * @return the color of the success event
     */
    String getSuccessEventColor();

    /**
     * @return the color of the error event
     */
    String getErrorEventColor();

    /**
     * @return the color of the links
     */
    String getLinkColor();

    /**
     * @return the color of the event items delimeter
     */
    String getDelimeterColor();

    /**
     * @return the color of part minimize button
     */
    String getMinimizeIconColor();

    /**
     * @return the hovering color of part minimize button
     */
    String getMinimizeIconHoverColor();

    /**
     * @return the color of the output font.
     */
    String getOutputFontColor();

    /**
     * @return the color of output link.
     */
    String getOutputLinkColor();

    /**
     * @return the background color for editor info panel.
     */
    String getEditorInfoBackgroundColor();

    /**
     * @return the color of border for editor info panel.
     */
    String getEditorInfoBorderColor();

    /**
     * @return the color of border shadow for editor info panel.
     */
    String getEditorInfoBorderShadowColor();

    /**
     * @return the color of the line numbers in the editor gutter.
     */
    String getEditorLineNumberColor();

    /**
     * @return the color of the separator line between the gutter and the editor.
     */
    String getEditorSeparatorColor();

    /**
     * Splitter small border color
     *
     * @return
     */
    String getSplitterSmallBorderColor();

    /**
     * Splitter large border color
     *
     * @return
     */
    String getSplitterLargeBorderColor();

    /**
     * Color of badge background.
     *
     * @return badge background color
     */
    String getBadgeBackgroundColor();

    /**
     * Color of badge font.
     *
     * @return badge font color
     */
    String getBadgeFontColor();


    /**
     * @return the blue color for icon.
     */
    String getBlueIconColor();

    /**
    * @return the color of the popup background.
    */
    String getPopupBkgColor();

    /**
     * @return the color of the popup border.
     */
    String getPopupBorderColor();

    /**
     * @return the color of the popup shadow.
     */
    String getPopupShadowColor();

    /**
     * @return the color of the popup hover.
     */
    String getPopupHoverColor();

    /**
     * @return the font color of the popup hot key.
     */
    String getPopupHotKeyColor();

    /**
     * @return the text field title color.
     */
    String getTextFieldTitleColor();

    /**
     * @return the text field color.
     */
    String getTextFieldColor();

    /**
     * @return the text field background color.
     */
    String getTextFieldBackgroundColor();

    /**
     * @return the text field focused color.
     */
    String getTextFieldFocusedColor();

    /**
     * @return the text field focused background color.
     */
    String getTextFieldFocusedBackgroundColor();

    /**
     * @return the text field disabled color.
     */
    String getTextFieldDisabledColor();

    /**
     * @return the text field disabled background color.
     */
    String getTextFieldDisabledBackgroundColor();

    /**
     * @return the text field border color.
     */
    String getTextFieldBorderColor();
}
