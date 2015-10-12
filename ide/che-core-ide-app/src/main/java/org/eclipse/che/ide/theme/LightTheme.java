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
public class LightTheme implements Theme {

    public static final String LIGHT_THEME_ID = "LightTheme";

    @Override
    public String getId() {
        return LIGHT_THEME_ID;
    }

    @Override
    public String getDescription() {
        return "Light Theme";
    }

    @Override
    public String getHoverBackgroundColor() {
        return "#D4E8FF";
    }

    @Override
    public String getKeyboardSelectionBackgroundColor() {
        return "#C3DEFF";
    }

    @Override
    public String getSelectionBackground() {
        return "#C3DEFF";
    }

    @Override
    public String getInactiveTabBackground() {
        return "#D6D6D9";
    }

    @Override
    public String getInactiveTabBorderColor() {
        return "#D6D6D9";
    }

    @Override
    public String getActiveTabBackground() {
        return "#ffffff";
    }

    @Override
    public String getActiveTabBorderColor() {
        return "#8E8E8E";
    }

    @Override
    public String getActiveTabTextColor() {
        return "#555555";
    }

    @Override
    public String getActiveTabTextShadow() {
        return "none";
    }

    @Override
    public String getActiveTabIconColor() {
        return "#1A68AF";
    }

    @Override
    public String getTabTextColor() {
        return "#555555";
    }

    @Override
    public String getHoveredTabTextColor() {
        return "#333333";
    }

    @Override
    public String getTabsPanelBackground() {
        return "#D6D6D9";
    }

    @Override
    public String getTabBorderColor() {
        return "#8E8E8E";
    }

    @Override
    public String getTabUnderlineColor() {
        return "rgb(70,102,149)";
    }

    @Override
    public String getActiveEditorTabBackgroundColor() {
        return "rgb(197, 197, 197)";
    }

    @Override
    public String getEditorTabIconColor() {
        return "#555555";
    }

    @Override
    public String getFocusedEditorTabBackgroundColor() {
        return "#FFFFFF";
    }

    @Override
    public String getFocusedEditorTabBorderBottomColor() {
        return "#4EABFF";
    }

    @Override
    public String getPartBackground() {
        return "#ffffff";
    }

    @Override
    public String getPartToolbar() {
        return "#D6D6D6";
    }

    @Override
    public String getPartToolbarActive() {
        return "rgba(195,222,255,1)";
    }

    @Override
    public String getPartToolbarShadow() {
        return "#bdbdbd";
    }

    @Override
    public String getPartToolbarSeparatorTopColor() {
        return "#FFFFFF";
    }

    @Override
    public String getPartToolbarSeparatorBottomColor() {
        return "#AAAAAA";
    }

    @Override
    public String getMainFontColor() {
        return "#222222";
    }

    @Override
    public String getRadioButtonBackgroundColor() {
        return "#BDBDBD";
    }

    @Override
    public String getDisabledMenuColor() {
        return "#AAAAAA";
    }

    @Override
    public String getDialogContentBackground() {
        return "#FFFFFF";
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
        return "#ECECEC";
    }

    @Override
    public String getButtonHoverBackground() {
        return "rgba(0,0,0,0.7)";
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
        return "rgba(0,0,0,0.8)";
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
        return "rgba(129, 129, 129, 0.2)";
    }

    @Override
    public String getButtonDisabledBorderColor() {
        return "#CECBCB";
    }

    @Override
    public String getButtonDisabledFontColor() {
        return "#999595";
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
        return "#ECECEC";
    }

    @Override
    public String getPrimaryButtonHoverBackground() {
        return "#4484D0";
    }

    @Override
    public String getPrimaryButtonHoverBorderColor() {
        return "#161819";
    }

    @Override
    public String getPrimaryButtonHoverFontColor() {
        return "#FFFFFF";
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
        return "#FFFFFF";
    }

    @Override
    public String getPrimaryButtonDisabledBackground() {
        return "rgba(128, 177, 234, 0.62)";
    }

    @Override
    public String getPrimaryButtonDisabledBorderColor() {
        return "#A8C6EA";
    }

    @Override
    public String getPrimaryButtonDisabledFontColor() {
        return "rgba(120, 120, 119, 0.6)";
    }

    @Override
    public String getSocialButtonColor() {
        return "#ffffff";
    }

    @Override
    public String getEditorPanelBackgroundColor() {
        return "#D6D6D9";
    }

    @Override
    public String getEditorBackgroundColor() {
        return "white";
    }

    @Override
    public String getEditorCurrentLineColor() {
        return "#E8F2FF";
    }

    @Override
    public String getEditorDefaultFontColor() {
        return "black";
    }

    @Override
    public String getEditorSelectionColor() {
        return "#d4e2ff";
    }

    @Override
    public String getEditorInactiveSelectionColor() {
        return "#d4d4d4";
    }

    @Override
    public String getEditorCursorColor() {
        return "black";
    }

    @Override
    public String getEditorGutterColor() {
        return "#eee";
    }

    @Override
    public String getEditorKeyWord() {
        return "#708";
    }

    @Override
    public String getEditorAtom() {
        return "#219";
    }

    @Override
    public String getEditorNumber() {
        return "#164";
    }

    @Override
    public String getEditorDef() {
        return "#00f";
    }

    @Override
    public String getEditorVariable() {
        return "black";
    }

    @Override
    public String getEditorVariable2() {
        return "#05a";
    }

    @Override
    public String getEditorProperty() {
        return "black";
    }

    @Override
    public String getEditorOperator() {
        return "black";
    }

    @Override
    public String getEditorComment() {
        return "#a50";
    }

    @Override
    public String getEditorString() {
        return "#a11";
    }

    @Override
    public String getEditorMeta() {
        return "#049";
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
        return "#170";
    }

    @Override
    public String getEditorAttribute() {
        return "#00c";
    }

    @Override
    public String getEditorString2() {
        return "#f50";
    }

    @Override
    public String getCompletionPopupBackgroundColor() {
        return "#303030";
    }

    @Override
    public String getCompletionPopupBorderColor() {
        return "#484848";
    }

    @Override
    public String getWindowContentBackground() {
        return "#ECECEC";
    }

    @Override
    public String getWindowContentFontColor() {
        return "#333333";
    }

    @Override
    public String getWindowShadowColor() {
        return "rgba(0, 0, 0, 0.50)";
    }

    @Override
    public String getWindowHeaderBackground() {
        return "#F5F5F5";
    }

    @Override
    public String getWindowHeaderBorderColor() {
        return "#1A68AF";
    }

    @Override
    public String getWindowFooterBackground() {
        return "#ECECEC";
    }

    @Override
    public String getWindowFooterBorderColor() {
        return "#A4A4A4";
    }

    @Override
    public String getWindowSeparatorColor() {
        return "#A4A4A4";
    }

    @Override
    public String getWindowTitleFontColor() {
        return "#555555";
    }

    @Override
    public String getWizardStepsColor() {
        return "#DBDBDB";
    }

    @Override
    public String getWizardStepsBorderColor() {
        return "#BDBDBD";
    }

    @Override
    public String getWelcomeFontColor() {
        return "#5E5E5E";
    }

    @Override
    public String getCaptionFontColor() {
        return "#888888";
    }

    @Override
    public String getConsolePanelColor() {
        return getTabsPanelBackground();
    }

    @Override
    public String getStatusPanelColor() {
        return getTabsPanelBackground();
    }

    @Override
    public String getCellOddRowColor() {
        return "#f3f7fb";
    }

    @Override
    public String getCellOddEvenColor() {
        return "#ffffff";
    }

    @Override
    public String getCellKeyboardSelectedRowColor() {
        return "#ffc";
    }

    @Override
    public String getCellHoveredRow() {
        return "#eee";
    }

    @Override
    public String getMainMenuBkgColor() {
        return "#cacacc";
    }

    @Override
    public String getMainMenuSelectedBkgColor() {
        return "#ffffff";
    }

    @Override
    public String getMainMenuSelectedBorderColor() {
        return "#a4a4a4";
    }

    @Override
    public String getMainMenuFontColor() {
        return "#555555";
    }

    @Override
    public String getMainMenuFontHoverColor() {
        return "#333333";
    }

    @Override
    public String getMainMenuFontSelectedColor() {
        return "#1a68af";
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
    public String getTabBorderShadow() {
        return "rgba(198, 205, 209, 0.5)";
    }

    @Override
    public String getTreeTextFileColor() {
        return "#7b7b7b";
    }

    @Override
    public String getTreeTextFolderColor() {
        return "#606060";
    }

    @Override
    public String getTreeTextShadow() {
        return "rgba(255, 255, 255, 0.5)";
    }

    @Override
    public String getTreeIconFileColor() {
        return "#606060";
    }

    @Override
    public String getToolbarActionGroupShadowColor() {
        return "#c5c5c5";
    }

    @Override
    public String getToolbarActionGroupBackgroundColor() {
        return "#ffffff";
    }

    @Override
    public String getToolbarActionGroupBorderColor() {
        return "#afafaf";
    }

    @Override
    public String getToolbarBackgroundImage() {
        return this.getMenuBackgroundImage();
    }

    @Override
    public String getToolbarBackgroundColor() {
        return " #EAEAEA";
    }

    @Override
    public String getToolbarIconColor() {
        return this.getIconColor();
    }

    @Override
    public String getToolbarHoverIconColor() {
        return "#565656";
    }

    @Override
    public String getToolbarSelectedIconFilter() {
        return "brightness(80%)";
    }

    @Override
    public String getTooltipBackgroundColor() {
        return "#FFFFFF";
    }

    @Override
    public String getPerspectiveSwitcherBackgroundColor() {
        return "#1a68af";
    }

    @Override
    public String getSelectCommandActionIconColor() {
        return "#1a68af";
    }

    @Override
    public String getSelectCommandActionIconBackgroundColor() {
        return "#e9e9e9";
    }

    @Override
    public String getSelectCommandActionColor() {
        return "#555555";
    }

    @Override
    public String getSelectCommandActionHoverColor() {
        return "#565656";
    }

    @Override
    public String getProgressColor() {
        return "#000000";
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
    public String getFactoryLinkColor() {
        return "#60abe0";
    }

    @Override
    public String getMinimizeIconColor() {
        return "#7b7b7b";
    }

    @Override
    public String getMinimizeIconHoverColor() {
        return "#333333";
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
        return "#ddd";
    }

    @Override
    public String getEditorInfoBorderColor() {
        return "#bdbdbd";
    }

    @Override
    public String getEditorInfoBorderShadowColor() {
        return "#f0f0f0";
    }

    @Override
    public String getEditorLineNumberColor() {
        return "#888888";
    }

    @Override
    public String getEditorSeparatorColor() {
        return "#888888";
    }

    @Override
    public String getBlueIconColor() {
        return "#1a68af";
    }

    @Override
    public String getSplitterSmallBorderColor() {
        return "#8E8E8E";
    }

    @Override
    public String getSplitterLargeBorderColor() {
        return "#E1E1E1";
    }

    @Override
    public String getBadgeBackgroundColor() {
        return "rgb(78, 171, 255)";
    }

    @Override
    public String getBadgeFontColor() {
        return "white";
    }

    @Override
    public String getPopupBkgColor() {
        return "#ececec";
    }

    @Override
    public String getPopupBorderColor() {
        return "#a4a4a4";
    }

    @Override
    public String getPopupShadowColor() {
        return "rgba(0, 0, 0, 0.30)";
    }

    @Override
    public String getPopupHoverColor() {
        return "rgba(0, 0, 0, 0.12)";
    }

    @Override
    public String getPopupHotKeyColor() {
        return "#A2A2A2";
    }

    @Override
    public String getTextFieldTitleColor() {
        return "#555555";
    }

    @Override
    public String getTextFieldColor() {
        return "#909090";
    }

    @Override
    public String getTextFieldBackgroundColor() {
        return "#ffffff";
    }

    @Override
    public String getTextFieldFocusedColor() {
        return "#727272";
    }

    @Override
    public String getTextFieldFocusedBackgroundColor() {
        return "#ffffff";
    }

    @Override
    public String getTextFieldDisabledColor() {
        return "#b9b9b9";
    }

    @Override
    public String getTextFieldDisabledBackgroundColor() {
        return "#ffffff";
    }

    @Override
    public String getTextFieldBorderColor() {
        return "#e1e1e1";
    }

    @Override
    public String getMenuBackgroundColor() {
        return "inherit";
    }

    @Override
    public String getMenuBackgroundImage() {
        return "linear-gradient( -180deg, #d3d3d4 0%, #bdbec0 100%)";
    }

    @Override
    public String getPanelBackgroundColor() {
        return "#d6d6d9";
    }

    @Override
    public String getPrimaryHighlightColor() {
        return "#1a68af";
    }

    @Override
    public String getIconColor() {
        return "#7c7c7c";
    }

    @Override
    public String getSeparatorColor() {
        return "#8e8e8e";
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
        return "#ECECEC";
    }

    @Override
    public String listBoxDropdownShadowColor() {
        return "0 1px 1px 0 rgba(0, 0, 0, 0.1)";
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
        return "rgba(59, 59, 59, 0.15)";
    }

    @Override
    public String scrollbarHoverBackgroundColor() {
        return "rgba(27, 27, 27, 0.10)";
    }

    @Override
    public String scrollbarActiveBackgroundColor() {
        return "rgba(27, 27, 27, 0.15)";
    }

    @Override
    public String openedFilesDropdownButtonBackground() {
        return "#e4e4e4";
    }

    @Override
    public String openedFilesDropdownButtonBorderColor() {
        return "#8e8e8e";
    }

    @Override
    public String openedFilesDropdownButtonShadowColor() {
        return "rgba(0, 0, 0, 0.15)";
    }

    @Override
    public String openedFilesDropdownButtonIconColor() {
        return "#8E8E8E";
    }

    @Override
    public String openedFilesDropdownButtonHoverIconColor() {
        return "#5A5A5A";
    }

    @Override
    public String openedFilesDropdownButtonActiveBackground() {
        return "#FFFFFF";
    }

    @Override
    public String openedFilesDropdownButtonActiveBorderColor() {
        return "#8E8E8E";
    }

    @Override
    public String openedFilesDropdownListBackgroundColor() {
        return "#ECECEC";
    }

    @Override
    public String openedFilesDropdownListBorderColor() {
        return "#A4A4A4";
    }

    @Override
    public String openedFilesDropdownListShadowColor() {
        return "rgba(0, 0, 0, 0.30)";
    }

    @Override
    public String openedFilesDropdownListTextColor() {
        return "#555555";
    }

    @Override
    public String openedFilesDropdownListCloseButtonColor() {
        return "#8E8E8E";
    }

    @Override
    public String openedFilesDropdownListHoverBackgroundColor() {
        return "rgba(0, 0, 0, 0.12)";
    }

    @Override
    public String openedFilesDropdownListHoverTextColor() {
        return "#8E8E8E";
    }
}
