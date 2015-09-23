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
 * This class contains constants for style. Fields initialized from user preferences. Static methods used for bridging with CssResources
 *
 * @author Evgen Vidolob
 */
public class Style {

    public static Theme theme;

    public static void setTheme(Theme theme) {
        Style.theme = theme;
    }

    public static String getHoverBackgroundColor() {
        return theme.getHoverBackgroundColor();
    }

    public static String getKeyboardSelectionBackgroundColor() {
        return theme.getKeyboardSelectionBackgroundColor();
    }

    public static String getSelectionBackground() {
        return theme.getSelectionBackground();
    }

    public static String getInactiveTabBackground() {
        return theme.getInactiveTabBackground();
    }

    public static String getInactiveTabBorderColor() {
        return theme.getInactiveTabBorderColor();
    }

    public static String getActiveTabBackground() {
        return theme.getActiveTabBackground();
    }

    public static String getTabTextColor() {
        return theme.getTabTextColor();
    }

    public static String getHoveredTabTextColor() {
        return theme.getHoveredTabTextColor();
    }

    public static String getActiveTabBorderColor() {
        return theme.getActiveTabBorderColor();
    }

    public static String getActiveTabTextColor() {
        return theme.getActiveTabTextColor();
    }

    public static String getActiveTabTextShadow() {
        return theme.getActiveTabTextShadow();
    }

    public static String getActiveTabIconColor() {
        return theme.getActiveTabIconColor();
    }

    public static String getTabsPanelBackground() {
        return theme.getTabsPanelBackground();
    }

    public static String getTabBorderColor() {
        return theme.getTabBorderColor();
    }

    public static String getTabUnderlineColor() {
        return theme.getTabUnderlineColor();
    }

    public static String getPartBackground() {
        return theme.getPartBackground();
    }

    public static String getPartToolbar() {
        return theme.getPartToolbar();
    }

    public static String getPartToolbarActive() {
        return theme.getPartToolbarActive();
    }

    public static String getPartToolbarShadow() {
        return theme.getPartToolbarShadow();
    }

    public static String getPartToolbarSeparatorTopColor() {
        return theme.getPartToolbarSeparatorTopColor();
    }

    public static String getPartToolbarSeparatorBottomColor() {
        return theme.getPartToolbarSeparatorBottomColor();
    }

    public static String getMainFontColor() {
        return theme.getMainFontColor();
    }

    public static String getRadioButtonBackgroundColor() {
        return theme.getRadioButtonBackgroundColor();
    }

    public static String getDeisabledMenuColor() {
        return theme.getDisabledMenuColor();
    }

    public static String getDialogContentBackground() {
        return theme.getDialogContentBackground();
    }

    public static String getButtonTopColor() {
        return theme.getButtonTopColor();
    }

    public static String getButtonColor() {
        return theme.getButtonColor();
    }

    public static String getNotableButtonTopColor() {
        return theme.getNotableButtonTopColor();
    }

    public static String getNotableButtonColor() {
        return theme.getNotableButtonColor();
    }

    public static String getSocialButtonColor() {
        return theme.getSocialButtonColor();
    }

    public static String getEditorPanelBackgroundColor() {
        return theme.getEditorPanelBackgroundColor();
    }

    public static String getEditorBackgroundColor() {
        return theme.getEditorBackgroundColor();
    }

    public static String getEditorCurrentLineColor() {
        return theme.getEditorCurrentLineColor();
    }

    public static String getEditorDefaultFontColor() {
        return theme.getEditorDefaultFontColor();
    }

    public static String getEditorSelectionColor() {
        return theme.getEditorSelectionColor();
    }

    public static String getEditorInactiveSelectionColor() {
        return theme.getEditorInactiveSelectionColor();
    }

    public static String getEditorCursorColor() {
        return theme.getEditorCursorColor();
    }

    public static String getEditorGutterColor() {
        return theme.getEditorGutterColor();
    }

    // syntax
    public static String getEditorKeyWord() {
        return theme.getEditorKeyWord();
    }

    public static String getEditorAtom() {
        return theme.getEditorAtom();
    }

    public static String getEditorNumber() {
        return theme.getEditorNumber();
    }

    public static String getEditorDef() {
        return theme.getEditorDef();
    }

    public static String getEditorVariable() {
        return theme.getEditorVariable();
    }

    public static String getEditorVariable2() {
        return theme.getEditorVariable2();
    }

    public static String getEditorProperty() {
        return theme.getEditorProperty();
    }

    public static String getEditorOperator() {
        return theme.getEditorOperator();
    }

    public static String getEditorComment() {
        return theme.getEditorComment();
    }

    public static String getEditorString() {
        return theme.getEditorString();
    }

    public static String getEditorString2() {
        return theme.getEditorString2();
    }

    public static String getEditorMeta() {
        return theme.getEditorMeta();
    }

    public static String getEditorError() {
        return theme.getEditorError();
    }

    public static String getEditorBuiltin() {
        return theme.getEditorBuiltin();
    }

    public static String getEditorTag() {
        return theme.getEditorTag();
    }

    public static String getEditorAttribute() {
        return theme.getEditorAttribute();
    }

    public static String getCompletionPopupBorderColor() {
        return theme.getCompletionPopupBorderColor();
    }

    public static String getCompletionPopupBackgroundColor() {
        return theme.getCompletionPopupBackgroundColor();
    }

    public static String getWindowContentBackground() {
        return theme.getWindowContentBackground();
    }

    public static String getWindowHeaderBackground() {
        return theme.getWindowHeaderBackground();
    }

    public static String getWindowSeparatorColor() {
        return theme.getWindowSeparatorColor();
    }

    public static String getWizardStepsColor() {
        return theme.getWizardStepsColor();
    }

    public static String getWizardStepsBorderColor() {
        return theme.getWizardStepsBorderColor();
    }

    public static String getWelcomeFontColor() {
        return theme.getWelcomeFontColor();
    }

    public static String getCaptionFontColor() {
        return theme.getCaptionFontColor();
    }

    public static String getFactoryLinkColor() {
        return theme.getFactoryLinkColor();
    }

    public static String getConsolePanelColor() {
        return theme.getConsolePanelColor();
    }

    public static String getStatusPanelColor() {
        return theme.getStatusPanelColor();
    }

    public static String getCellOddRow() {
        return theme.getCellOddRowColor();
    }

    public static String getCellEvenRow() {
        return theme.getCellOddEvenColor();
    }

    public static String getCellKeyboardSelectedRow() {
        return theme.getCellKeyboardSelectedRowColor();
    }

    public static String getCellHoveredRow() {
        return theme.getCellHoveredRow();
    }

    public static String getMainMenuBkgColor() {
        return theme.getMainMenuBkgColor();
    }

    public static String getMainMenuSelectedBkgColor() {
        return theme.getMainMenuSelectedBkgColor();
    }

    public static String getMainMenuSelectedBorderColor() {
        return theme.getMainMenuSelectedBorderColor();
    }

    public static String getMainMenuFontColor() {
        return theme.getMainMenuFontColor();
    }

    public static String getMainMenuFontHoverColor() {
        return theme.getMainMenuFontHoverColor();
    }

    public static String getMainMenuFontSelectedColor() {
        return theme.getMainMenuFontSelectedColor();
    }

    public static String getTabBorderShadow() {
        return theme.getTabBorderShadow();
    }

    public static String getButtonTextShadow() {
        return theme.getButtonTextShadow();
    }

    public static String getTreeTextFileColor() {
        return theme.getTreeTextFileColor();
    }

    public static String getTreeTextFolderColor() {
        return theme.getTreeTextFolderColor();
    }

    public static String getTreeTextShadow() {
        return theme.getTreeTextShadow();
    }

    public static String getTreeIconFileColor() {
        return theme.getTreeIconFileColor();
    }


    public static String getButtonHoverTextColor() {
        return theme.getButtonHoverTextColor();
    }

    public static String getButtonHoverColor() {
        return theme.getButtonHoverColor();
    }

    public static String getToolbarBackgroundImage() {
        return theme.getToolbarBackgroundImage();
    }

    public static String getToolbarActionGroupShadowColor() {
        return theme.getToolbarActionGroupShadowColor();
    }

    public static String getToolbarActionGroupBackgroundColor() {
        return theme.getToolbarActionGroupBackgroundColor();
    }

    public static String getToolbarActionGroupBorderColor() {
        return theme.getToolbarActionGroupBorderColor();
    }

    public static String getToolbarBackgroundColor() {
        return theme.getToolbarBackgroundColor();
    }

    public static String getToolbarIconColor() {
        return theme.getToolbarIconColor();
    }

    public static String getToolbarHoverIconColor() {
        return theme.getToolbarHoverIconColor();
    }

    public static String getTooltipBackgroundColor() {
        return theme.getTooltipBackgroundColor();
    }

    public static String getPerspectiveSwitcherBackgroundColor() {
        return theme.getPerspectiveSwitcherBackgroundColor();
    }

    public static String getSelectCommandActionIconColor() {
        return theme.getSelectCommandActionIconColor();
    }

    public static String getSelectCommandActionIconBackgroundColor() {
        return theme.getSelectCommandActionIconBackgroundColor();
    }

    public static String getSelectCommandActionColor() {
        return theme.getSelectCommandActionColor();
    }

    public static String getSelectCommandActionHoverColor() {
        return theme.getSelectCommandActionHoverColor();
    }

    public static String getProgressColor() {
        return theme.getProgressColor();
    }

    public static String getSuccessEventColor() {
        return theme.getSuccessEventColor();
    }

    public static String getErrorEventColor() {
        return theme.getErrorEventColor();
    }

    public static String getLinkColor() {
        return theme.getLinkColor();
    }

    public static String getDelimeterColor() {
        return theme.getDelimeterColor();
    }

    public static String getMinimizeIconColor() {
        return theme.getMinimizeIconColor();
    }

    public static String getMinimizeIconHoverColor() {
        return theme.getMinimizeIconHoverColor();
    }

    public static String getOutputFontColor() {
        return theme.getOutputFontColor();
    }

    public static String getOutputLinkColor() {
        return theme.getOutputLinkColor();
    }

    public static String getEditorInfoBackgroundColor() {
        return theme.getEditorInfoBackgroundColor();
    }

    public static String getEditorInfoBorderColor() {
        return theme.getEditorInfoBorderColor();
    }

    public static String getEditorInfoBorderShadowColor() {
        return theme.getEditorInfoBorderShadowColor();
    }

    public static String getEditorLineNumberColor() {
        return theme.getEditorLineNumberColor();
    }

    public static String getEditorSeparatorColor() {
        return theme.getEditorSeparatorColor();
    }

    public static String getBlueIconColor() {
        return theme.getBlueIconColor();
    }

    public static String getSplitterSmallBorderColor() {
        return theme.getSplitterSmallBorderColor();
    }

    public static String getSplitterLargeBorderColor() {
        return theme.getSplitterLargeBorderColor();
    }

    public static String getBadgeBackgroundColor() {
        return theme.getBadgeBackgroundColor();
    }

    public static String getBadgeFontColor() {
        return theme.getBadgeFontColor();
    }

    public static String getPopupBkgColor() {
        return theme.getPopupBkgColor();
    }

    public static String getPopupBorderColor() {
        return theme.getPopupBorderColor();
    }

    public static String getPopupShadowColor() {
        return theme.getPopupShadowColor();
    }

    public static String getPopupHoverColor() {
        return theme.getPopupHoverColor();
    }

    public static String getPopupHotKeyColor() {
        return theme.getPopupHotKeyColor();
    }
    public static String getTextFieldTitleColor() {
        return theme.getTextFieldTitleColor();
    }

    public static String getTextFieldColor() {
        return theme.getTextFieldColor();
    }

    public static String getTextFieldBackgroundColor() {
        return theme.getTextFieldBackgroundColor();
    }

    public static String getTextFieldFocusedColor() {
        return theme.getTextFieldFocusedColor();
    }

    public static String getTextFieldFocusedBackgroundColor() {
        return theme.getTextFieldFocusedBackgroundColor();
    }

    public static String getTextFieldDisabledColor() {
        return theme.getTextFieldDisabledColor();
    }

    public static String getTextFieldDisabledBackgroundColor() {
        return theme.getTextFieldDisabledBackgroundColor();
    }

    public static String getTextFieldBorderColor() {
        return theme.getTextFieldBorderColor();
    }
}
