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
package org.eclipse.che.ide.theme;

import org.eclipse.che.ide.api.theme.Theme;

import com.google.inject.Singleton;

/**
 * @author Evgen Vidolob
 */
@Singleton
public class DarkTheme implements Theme {

    public static final String DARK_THEME_ID = "DarkTheme";

    @Override
    public String getId() {
        return DARK_THEME_ID;
    }

    @Override
    public String getDescription() {
        return "Dark Theme";
    }

    @Override
    public String hoverBackgroundColor() {
        return "rgba(215, 215, 215, 0.12)";
    }

    @Override
    public String keyboardSelectionBackgroundColor() {
        return "#2f65ca";
    }

    @Override
    public String selectionBackground() {
        return "#256c9f";
    }

    @Override
    public String tabsPanelBackground() {
        return "#33373B";
    }

    /************************************************************************************************
     *
     * Inactive tab button
     *
     ************************************************************************************************/

    @Override
    public String inactiveTabBackground() {
        return "#484848";
    }

    @Override
    public String tabBorderColor() {
        return "#121416";
    }

    @Override
    public String inactiveTabBorderColor() {
        return "#353535";
    }

    @Override
    public String tabUnderlineColor() {
        return "rgb(70,102,149)";
    }

    @Override
    public String tabTextColor() {
        return "#AAAAAA";
    }

    @Override
    public String hoveredTabTextColor() {
        return "#FFFFFF";
    }

    /************************************************************************************************
     *
     * Active tab button
     *
     ************************************************************************************************/

    @Override
    public String activeTabBackground() {
        return "#292C2F";
    }

    @Override
    public String activeTabBorderColor() {
        return "#121416";
    }

    @Override
    public String activeTabTextColor() {
        return "#FFFFFF";
    }

    @Override
    public String activeTabTextShadow() {
        return "0px 1px 1px rgba(0, 0, 0, 0.5)";
    }

    @Override
    public String activeTabIconColor() {
        return "#FFFFFF";
    }

    /************************************************************************************************
     *
     * Active editor tab button
     *
     ************************************************************************************************/

    @Override
    public String getEditorTabIconColor() {
        return "#FFFFFF";
    }

    @Override
    public String activeEditorTabBackgroundColor() {
        return "rgba(53, 62, 80, 0.5)";
    }

    @Override
    public String focusedEditorTabBackgroundColor() {
        return "#353E50";
    }

    @Override
    public String focusedEditorTabBorderBottomColor() {
        return "#4EABFF";
    }

    /************************************************************************************************
     *
     * Part toolbar
     *
     ************************************************************************************************/

    @Override
    public String partBackground() {
        return "#292C2F";
    }

    @Override
    public String partToolbar() {
        return "#33373B";
    }

    @Override
    public String partToolbarActive() {
        return "#414c5e";
    }

    @Override
    public String partToolbarShadow() {
        return "rgba(50,50,50, 0.75)";
    }

    @Override
    public String partToolbarSeparatorTopColor() {
        return "#232323";
    }

    @Override
    public String partToolbarSeparatorBottomColor() {
        return "#878787";
    }

    @Override
    public String getMainFontColor() {
        return "#FFFFFF";
    }

    @Override
    public String getDisabledMenuColor() {
        return "#808080";
    }

    @Override
    public String getDialogContentBackground() {
        return "#656565";
    }

    @Override
    public String getButtonBackground() {
        return "#5A5A5A";
    }

    @Override
    public String getButtonBorderColor() {
        return "#161819";
    }

    @Override
    public String getButtonFontColor() {
        return "#E9E9E9";
    }

    @Override
    public String getButtonHoverBackground() {
        return "#484848";
    }

    @Override
    public String getButtonHoverBorderColor() {
        return "#161819";
    }

    @Override
    public String getButtonHoverFontColor() {
        return "#FFFFFF";
    }

    @Override
    public String getButtonClickedBackground() {
        return "#3e3e3e";
    }

    @Override
    public String getButtonClickedBorderColor() {
        return "#161819";
    }

    @Override
    public String getButtonClickedFontColor() {
        return "#FFFFFF";
    }

    @Override
    public String getButtonDisabledBackground() {
        return "rgba(40, 40, 40, 0.4)";
    }

    @Override
    public String getButtonDisabledBorderColor() {
        return "#161819";
    }

    @Override
    public String getButtonDisabledFontColor() {
        return "#5A5A5A";
    }

    @Override
    public String getPrimaryButtonBackground() {
        return "#4A90E2";
    }

    @Override
    public String getPrimaryButtonBorderColor() {
        return "#161819";
    }

    @Override
    public String getPrimaryButtonFontColor() {
        return "#E9E9E9";
    }

    @Override
    public String getPrimaryButtonHoverBackground() {
        return "#4484d0";
    }

    @Override
    public String getPrimaryButtonHoverBorderColor() {
        return "#161819";
    }

    @Override
    public String getPrimaryButtonHoverFontColor() {
        return "#EEEEEE";
    }

    @Override
    public String getPrimaryButtonClickedBackground() {
        return "#3b73b4";
    }

    @Override
    public String getPrimaryButtonClickedBorderColor() {
        return "#14354C";
    }

    @Override
    public String getPrimaryButtonClickedFontColor() {
        return "#EEEEEE";
    }

    @Override
    public String getPrimaryButtonDisabledBackground() {
        return "rgba(26, 104, 175, 0.4)";
    }

    @Override
    public String getPrimaryButtonDisabledBorderColor() {
        return "#161819";
    }

    @Override
    public String getPrimaryButtonDisabledFontColor() {
        return "rgba(165, 165, 165, 0.4)";
    }

    @Override
    public String getRadioButtonBackgroundColor() {
        return "#BDBDBD";
    }

    @Override
    public String editorPanelBackgroundColor() {
        return "#21252b";
    }

    @Override
    public String getEditorBackgroundColor() {
        return "#272b33";
    }

    @Override
    public String getEditorCurrentLineColor() {
        return "#424242";
    }

    @Override
    public String getEditorDefaultFontColor() {
        return "#A9B7C6";
    }

    @Override
    public String getEditorSelectionColor() {
        return "#256c9f";
    }

    @Override
    public String getEditorInactiveSelectionColor() {
        return "#d4d4d4";
    }

    @Override
    public String getEditorCursorColor() {
        return getEditorDefaultFontColor();
    }

    @Override
    public String getEditorGutterColor() {
        return "#313335";
    }

    @Override
    public String getEditorKeyWord() {
        return "#cc7832";
    }

    @Override
    public String getEditorAtom() {
        return "#9876aa";
    }

    @Override
    public String getEditorNumber() {
        return "#6897bb";
    }

    @Override
    public String getEditorDef() {
        return "#A7E600";
    }

    @Override
    public String getEditorVariable() {
        return getEditorDefaultFontColor();
    }

    @Override
    public String getEditorVariable2() {
        return "#0ab";
    }

    @Override
    public String getEditorProperty() {
        return getEditorDefaultFontColor();
    }

    @Override
    public String getEditorOperator() {
        return getEditorDefaultFontColor();
    }

    @Override
    public String getEditorComment() {
        return "#629755";
    }

    @Override
    public String getEditorString() {
        return "#6AAF32";
    }

    @Override
    public String getEditorMeta() {
        return "#BBB529";
    }

    @Override
    public String getEditorError() {
        return "#f00";
    }

    @Override
    public String getEditorBuiltin() {
        return "#30a";
    }

    @Override
    public String getEditorTag() {
        return "#E8BF6A";
    }

    @Override
    public String getEditorAttribute() {
        return "rgb(152,118,170)";
    }

    @Override
    public String getEditorString2() {
        return "#CC7832";
    }

    @Override
    public String completionPopupBackgroundColor() {
        return "#292C2F";
    }

    @Override
    public String completionPopupBorderColor() {
        return "#121416";
    }

    @Override
    public String completionPopupHeaderBackgroundColor() {
        return "#222222";
    }

    @Override
    public String completionPopupHeaderTextColor() {
        return "#A5A5A5";
    }

    @Override
    public String completionPopupSelectedItemBackgroundColor() {
        return "rgba(215, 215, 215, 0.12)";
    }

    @Override
    public String completionPopupItemTextColor() {
        return "#E4E4E4";
    }

    @Override
    public String completionPopupItemSubtitleTextColor() {
        return "#727272";
    }

    @Override
    public String getWindowContentBackground() {
        return "#292C2F";
    }

    @Override
    public String getWindowContentFontColor() {
        return "#AAAAAA";
    }

    @Override
    public String getWindowShadowColor() {
        return "rgba(0, 0, 0, 0.50)";
    }

    @Override
    public String getWindowHeaderBackground() {
        return "#2E3A45";
    }

    @Override
    public String getWindowHeaderBorderColor() {
        return "#1A68AF";
    }

    @Override
    public String getWindowFooterBackground() {
        return "#292C2F";
    }

    @Override
    public String getWindowFooterBorderColor() {
        return "#121416";
    }

    @Override
    public String getWindowSeparatorColor() {
        return "#121416";
    }

    @Override
    public String getWindowTitleFontColor() {
        return "#AAAAAA";
    }

    @Override
    public String getWizardStepsColor() {
        return "#222222";
    }

    @Override
    public String getWizardStepsBorderColor() {
        return "#000000";
    }

    @Override
    public String getWelcomeFontColor() {
        return getMainFontColor();
    }

    @Override
    public String getCaptionFontColor() {
        return "#888888";
    }

    @Override
    public String getFactoryLinkColor() {
        return "#60abe0";
    }

    @Override
    public String consolePanelColor() {
        return "#313131";
    }

    @Override
    public String getStatusPanelColor() {
        return "#404040";
    }

    @Override
    public String getCellOddRowColor() {
        return "#424242";
    }

    @Override
    public String getCellOddEvenColor() {
        return "#373737";
    }

    @Override
    public String getCellKeyboardSelectedRowColor() {
        return "#214283";
    }

    @Override
    public String getCellHoveredRow() {
        return hoverBackgroundColor();
    }

    @Override
    public String getMainMenuBkgColor() {
        return this.getMenuBackgroundColor();
    }

    @Override
    public String getMainMenuSelectedBkgColor() {
        return "#2e3a45";
    }

    @Override
    public String getMainMenuSelectedBorderColor() {
        return "#121416";
    }

    @Override
    public String getMainMenuFontColor() {
        return "#e4e4e4";
    }

    @Override
    public String getMainMenuFontHoverColor() {
        return "#ffffff";
    }

    @Override
    public String getMainMenuFontSelectedColor() {
        return "#4a90e2";
    }

    @Override
    public String getNotableButtonTopColor() {
        return "#dbdbdb";
    }

    @Override
    public String getNotableButtonColor() {
        return "#2d6ba3";
    }

    @Override
    public String tabBorderShadow() {
        return "rgba(188, 195, 199, 0.5)";
    }

    @Override
    public String treeTextFileColor() {
        return "#dbdbdb";
    }

    @Override
    public String treeTextFolderColor() {
        return "#b4b4b4";
    }

    @Override
    public String treeTextShadow() {
        return "rgba(0, 0, 0, 0.5)";
    }

    @Override
    public String treeIconFileColor() {
        return "#b4b4b4";
    }

    @Override
    public String getSocialButtonColor() {
        return "#ffffff";
    }

    @Override
    public String getToolbarBackgroundColor() {
        return "#292c2f";
    }

    @Override
    public String getToolbarActionGroupShadowColor() {
        return "#3c3c3c";
    }

    @Override
    public String getToolbarActionGroupBackgroundColor() {
        return "#33373b";
    }

    @Override
    public String getToolbarActionGroupBorderColor() {
        return "#24272c";
    }

    @Override
    public String getToolbarBackgroundImage() {
        return this.getMenuBackgroundImage();
    }

    @Override
    public String getToolbarIconColor() {
        return this.getIconColor();
    }

    @Override
    public String getToolbarHoverIconColor() {
        return "#e0e0e0";
    }

    @Override
    public String getToolbarSelectedIconFilter() {
        return "brightness(90%)";
    }

    @Override
    public String getTooltipBackgroundColor() {
        return "#202020";
    }

    @Override
    public String getPerspectiveSwitcherBackgroundColor() {
        return "#4eabff";
    }

    @Override
    public String getSelectCommandActionIconColor() {
        return "#4a90e2";
    }

    @Override
    public String getSelectCommandActionIconBackgroundColor() {
        return "#1e1e1e";
    }

    @Override
    public String getSelectCommandActionColor() {
        return "#e3e3e3";
    }

    @Override
    public String getSelectCommandActionHoverColor() {
        return "#e0e0e0";
    }

    @Override
    public String progressColor() {
        return "#ffffff";
    }

    @Override
    public String getSuccessEventColor() {
        return "#7dc878";
    }

    @Override
    public String getErrorEventColor() {
        return "#e25252";
    }

    @Override
    public String getDelimeterColor() {
        return "#2f2f2f";
    }

    @Override
    public String getLinkColor() {
        return "#acacac";
    }

    @Override
    public String minimizeIconColor() {
        return "#5D5D5D";
    }

    @Override
    public String minimizeIconHoverColor() {
        return "#D8D8D8";
    }

    @Override
    public String getOutputFontColor() {
        return "#e6e6e6";
    }

    @Override
    public String getOutputLinkColor() {
        return "#61b7ef";
    }

    @Override
    public String getEditorInfoBackgroundColor() {
        return "#292C2F";
    }

    @Override
    public String editorInfoTextColor() {
        return "#AAAAAA";
    }

    @Override
    public String getEditorInfoBorderColor() {
        return "#282828";
    }

    @Override
    public String getEditorInfoBorderShadowColor() {
        return "#424242";
    }

    @Override
    public String getEditorLineNumberColor() {
        return "#888888";
    }

    @Override
    public String editorGutterLineNumberBackgroundColor() {
        return "#31353E";
    }

    @Override
    public String getEditorSeparatorColor() {
        return "#888888";
    }

    @Override
    public String getBlueIconColor() {
        return "#4eabff";
    }

    @Override
    public String getSplitterSmallBorderColor() {
        return "#0D0F10";
    }

    @Override
    public String getSplitterLargeBorderColor() {
        return "#2D2D2D";
    }

    @Override
    public String getBadgeBackgroundColor() {
        return "#4EABFF";
    }

    @Override
    public String getBadgeFontColor() {
        return "white";
    }

    public String getPopupBkgColor() {
        return "#292c2f";
    }

    @Override
    public String getPopupBorderColor() {
        return "#121416";
    }

    @Override
    public String getPopupShadowColor() {
        return "rgba(0, 0, 0, 0.50)";
    }

    @Override
    public String getPopupHoverColor() {
        return "rgba(215, 215, 215, 0.12)";
    }

    @Override
    public String getPopupHotKeyColor() {
        return "#727272";
    }

    @Override
    public String getTextFieldTitleColor() {
        return "#aaaaaa";
    }

    @Override
    public String getTextFieldColor() {
        return "#e4e4e4";
    }

    @Override
    public String getTextFieldBackgroundColor() {
        return "#212325";
    }

    @Override
    public String getTextFieldFocusedColor() {
        return "#e4e4e4";
    }

    @Override
    public String getTextFieldFocusedBackgroundColor() {
        return "#2d2f31";
    }

    @Override
    public String getTextFieldDisabledColor() {
        return "#727272";
    }

    @Override
    public String getTextFieldDisabledBackgroundColor() {
        return "#2d2f31";
    }

    @Override
    public String getTextFieldBorderColor() {
        return "#161819";
    }

    @Override
    public String getMenuBackgroundColor() {
        return "#292c2f";
    }

    @Override
    public String getMenuBackgroundImage() {
        return "inherit";
    }

    @Override
    public String getPanelBackgroundColor() {
        return "#33373b";
    }

    @Override
    public String getPrimaryHighlightColor() {
        return "#4a90e2";
    }

    @Override
    public String getIconColor() {
        return "#aaaaaa";
    }

    @Override
    public String getSeparatorColor() {
        return "#121416";
    }

    @Override
    public String getErrorColor() {
        return "#C34d4d";
    }

    @Override
    public String getSuccessColor() {
        return "#31b993";
    }

    @Override
    public String getListBoxHoverBackgroundColor() {
        return this.getPopupHoverColor();
    }

    @Override
    public String getListBoxColor() {
        return this.getTextFieldColor();
    }

    @Override
    public String getListBoxDisabledColor() {
        return this.getTextFieldDisabledColor();
    }

    @Override
    public String getListBoxDisabledBackgroundColor() {
        return this.getTextFieldDisabledBackgroundColor();
    }

    @Override
    public String getListBoxDropdownBackgroundColor() {
        return this.getMenuBackgroundColor();
    }

    @Override
    public String listBoxDropdownShadowColor() {
        return "0 2px 2px 0 rgba(0, 0, 0, 0.3)";
    }

    @Override
    public String categoriesListHeaderTextColor() {
        return this.getTextFieldTitleColor();
    }

    @Override
    public String categoriesListHeaderIconColor() {
        return this.getTextFieldTitleColor();
    }

    @Override
    public String categoriesListHeaderBackgroundColor() {
        return this.getPopupBkgColor();
    }

    @Override
    public String categoriesListItemTextColor() {
        return this.getTextFieldColor();
    }

    @Override
    public String categoriesListItemBackgroundColor() {
        return this.getTextFieldFocusedBackgroundColor();
    }

    @Override
    public String scrollbarBorderColor() {
        return "rgba(235, 235, 235, 0.3)";
    }

    @Override
    public String scrollbarBackgroundColor() {
        return "rgba(215, 215, 215, 0.10)";
    }

    @Override
    public String scrollbarHoverBackgroundColor() {
        return "rgba(215, 215, 215, 0.3)";
    }
    
    @Override
    public String openedFilesDropdownButtonBackground() {
        return "rgba(51,55,59,0.50)";
    }

    @Override
    public String openedFilesDropdownButtonBorderColor() {
        return "#24272C";
    }

    @Override
    public String openedFilesDropdownButtonShadowColor() {
        return "#3C3C3C";
    }

    @Override
    public String openedFilesDropdownButtonIconColor() {
        return "#AAAAAA";
    }

    @Override
    public String openedFilesDropdownButtonHoverIconColor() {
        return "#FFFFFF";
    }

    @Override
    public String openedFilesDropdownButtonActiveBackground() {
        return "#292C2F";
    }

    @Override
    public String openedFilesDropdownButtonActiveBorderColor() {
        return "#121416";
    }

    @Override
    public String openedFilesDropdownListBackgroundColor() {
        return "#292C2f";
    }

    @Override
    public String openedFilesDropdownListBorderColor() {
        return "#121416";
    }

    @Override
    public String openedFilesDropdownListShadowColor() {
        return "rgba(0, 0, 0, 0.50)";
    }

    @Override
    public String openedFilesDropdownListTextColor() {
        return "#AAAAAA";
    }

    @Override
    public String openedFilesDropdownListCloseButtonColor() {
        return "#FFFFFF";
    }

    @Override
    public String openedFilesDropdownListHoverBackgroundColor() {
        return "rgba(215, 215, 215, 0.12)";
    }

    @Override
    public String openedFilesDropdownListHoverTextColor() {
        return "#FFFFFF";
    }

    @Override
    public String radioButtonIconColor() {
        return this.getBlueIconColor();
    }

    @Override
    public String radioButtonBorderColor() {
        return this.getTextFieldBorderColor();
    }

    @Override
    public String radioButtonBackgroundColor() {
        return this.getTextFieldBackgroundColor();
    }

    @Override
    public String radioButtonFontColor() {
        return this.getTextFieldColor();
    }

    @Override
    public String radioButtonDisabledFontColor() {
        return this.getTextFieldDisabledColor();
    }

    @Override
    public String radioButtonDisabledIconColor() {
        return this.getTextFieldDisabledColor();
    }

    @Override
    public String radioButtonDisabledBackgroundColor() {
        return this.getTextFieldDisabledBackgroundColor();
    }

    @Override
    public String checkBoxIconColor() {
        return this.getBlueIconColor();
    }

    @Override
    public String checkBoxFontColor() {
        return this.getTextFieldColor();
    }

    @Override
    public String checkBoxBorderColor() {
        return this.getTextFieldBorderColor();
    }

    @Override
    public String checkBoxBackgroundColor() {
        return this.getTextFieldBackgroundColor();
    }

    @Override
    public String checkBoxDisabledIconColor() {
        return this.getTextFieldDisabledColor();
    }

    @Override
    public String checkBoxDisabledFontColor() {
        return this.getTextFieldDisabledColor();
    }

    @Override
    public String checkBoxDisabledBackgroundColor() {
        return this.getTextFieldDisabledBackgroundColor();
    }

    @Override
    public String getProjectExplorerJointContainerFill() {
        return "#dbdbdb";
    }

    @Override
    public String getProjectExplorerJointContainerShadow() {
        return "drop-shadow(1px 1px 0 rgba(0, 0, 0, 0.4))";
    }

    @Override
    public String getProjectExplorerPresentableTextShadow() {
        return "1px 1px 0 rgba(0, 0, 0, 0.4)";
    }

    @Override
    public String getProjectExplorerInfoTextShadow() {
        return "1px 1px 0 rgba(0, 0, 0, 0.4)";
    }

    @Override
    public String getProjectExplorerSelectedRowBackground() {
        return "#256c9f";
    }

    @Override
    public String getProjectExplorerHoverRowBackground() {
        return "#555";
    }

    @Override
    public String loaderExpanderColor() {
        return "#e3e3e3";
    }

    @Override
    public String loaderIconBackgroundColor() {
        return "#1e1e1e";
    }

    @Override
    public String loaderProgressStatusColor() {
        return "#4a90e2";
    }
}
