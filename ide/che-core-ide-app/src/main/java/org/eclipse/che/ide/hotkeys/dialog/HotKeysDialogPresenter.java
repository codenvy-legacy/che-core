/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.hotkeys.dialog;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.keybinding.KeyBindingAgent;
import org.eclipse.che.ide.hotkeys.HasHotKeyItems;
import org.eclipse.che.ide.hotkeys.HotKeyItem;
import org.eclipse.che.ide.util.StringUtils;
import org.eclipse.che.ide.util.input.CharCodeWithModifiers;
import org.eclipse.che.ide.util.input.KeyMapUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The class provides displaying list hotKeys for IDE and editor
 * @author Alexander Andrienko
 */
@Singleton
public class HotKeysDialogPresenter implements HotKeysDialogView.ActionDelegate {

    private static final String EDITOR_KEYBINDINGS = "Editor";
    private static final String IDE_KEYBINDINGS = "IDE";

    private final HotKeysDialogView view;
    private final KeyBindingAgent   keyBindingAgent;
    private final ActionManager     actionManager;
    private final EditorAgent       editorAgent;

    private List<HotKeyItem> ideHotKey;
    private List<HotKeyItem> editorHotKeys;

    private Map<String, List<HotKeyItem>> categories;

    @Inject
    public HotKeysDialogPresenter(HotKeysDialogView view,
                           KeyBindingAgent keyBindingAgent,
                           ActionManager actionManager,
                           EditorAgent editorAgent) {
        this.view = view;
        this.keyBindingAgent = keyBindingAgent;
        this.actionManager = actionManager;
        this.editorAgent = editorAgent;

        categories = new HashMap<>();
        view.setDelegate(this);
    }

    /** {@inheritDoc} */
    @Override
    public void showHotKeys() {
        ideHotKey = getIDEHotKey();
        editorHotKeys = getEditorHotKey();

        categories.clear();
        if (!ideHotKey.isEmpty()) {
            categories.put(IDE_KEYBINDINGS, ideHotKey);
        }
        if (!editorHotKeys.isEmpty()) {
            categories.put(EDITOR_KEYBINDINGS, editorHotKeys);
        }

        view.setData(categories);
        view.renderKeybindings();
        view.show();
    }

    private List<HotKeyItem> getIDEHotKey() {
        List<HotKeyItem> ideHotKeys = new ArrayList<>();
 
        for (String actionId : actionManager.getActionIds("")) {
            CharCodeWithModifiers charCodeWithModifiers = keyBindingAgent.getKeyBinding(actionId);
            if (charCodeWithModifiers != null) {
                String hotKey = KeyMapUtil.getShortcutText(keyBindingAgent.getKeyBinding(actionId));
                String description = actionManager.getAction(actionId).getTemplatePresentation().getDescription();
                ideHotKeys.add(new HotKeyItem(description, hotKey));
            }
        }
        return ideHotKeys;
    }

    private List<HotKeyItem> getEditorHotKey() {
        EditorPartPresenter activeEditor = editorAgent.getActiveEditor();
        if (activeEditor instanceof HasHotKeyItems) {
            return ((HasHotKeyItems)activeEditor).getHotKeys();
        }
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    public void onOkClicked() {
        view.hide();
    }

    @Override
    public void onFilterValueChanged(String filteredText) {
        categories.clear();

        List<HotKeyItem> ideFilteredHotKey = filterCategory(ideHotKey, filteredText);
        if (!ideFilteredHotKey.isEmpty()) {
            categories.put(IDE_KEYBINDINGS, ideFilteredHotKey);
        }

        List<HotKeyItem> editorFilteredHotKeys = filterCategory(editorHotKeys, filteredText);
        if (!editorFilteredHotKeys.isEmpty()) {
            categories.put(EDITOR_KEYBINDINGS, editorFilteredHotKeys);
        }

        view.setData(categories);
        view.renderKeybindings();
    }

    private List<HotKeyItem> filterCategory(List<HotKeyItem> hotKeyItems, String expectedText) {
        List<HotKeyItem> result = new ArrayList<>();
        for (HotKeyItem hotKeyItem: hotKeyItems) {
            String description = hotKeyItem.getActionDescription();
            String keyBindings = hotKeyItem.getHotKey();
            boolean isFound = description != null && (StringUtils.containsIgnoreCase(description, expectedText)
                              || StringUtils.containsIgnoreCase(keyBindings, expectedText));
            if (isFound) {
                result.add(hotKeyItem);
            }
        }
        return result;
    }
}
