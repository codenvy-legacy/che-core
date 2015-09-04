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
package org.eclipse.che.ide.client;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import org.eclipse.che.ide.CoreLocalizationConstant;
import org.eclipse.che.ide.api.extension.ExtensionDescription;
import org.eclipse.che.ide.api.extension.ExtensionRegistry;
import org.eclipse.che.ide.api.preferences.PreferencesManager;
import org.eclipse.che.ide.collections.Jso;
import org.eclipse.che.ide.ui.loaders.initializationLoader.LoaderPresenter;
import org.eclipse.che.ide.ui.loaders.initializationLoader.OperationInfo;
import org.eclipse.che.ide.util.loging.Log;

import java.util.Map;

/**
 * {@link ExtensionInitializer} responsible for bringing up Extensions. It uses ExtensionRegistry to acquire
 * Extension description and dependencies.
 *
 * @author Nikolay Zamosenchuk
 * @author Dmitry Shnurenko
 */
@Singleton
public class ExtensionInitializer {
    protected final ExtensionRegistry extensionRegistry;

    private final ExtensionManager         extensionManager;
    private       PreferencesManager       preferencesManager;
    private       CoreLocalizationConstant localizedConstants;
    private       LoaderPresenter          loader;

    /**
     *
     */
    @Inject
    public ExtensionInitializer(final ExtensionRegistry extensionRegistry,
                                final ExtensionManager extensionManager,
                                PreferencesManager preferencesManager,
                                CoreLocalizationConstant localizedConstants,
                                LoaderPresenter loader) {
        this.extensionRegistry = extensionRegistry;
        this.extensionManager = extensionManager;
        this.preferencesManager = preferencesManager;
        this.localizedConstants = localizedConstants;
        this.loader = loader;
    }

    /** {@inheritDoc} */
    public void startExtensions() {
        OperationInfo startExtensionsOoeration = new OperationInfo(localizedConstants.startingOperation("extensions..."), OperationInfo.Status.IN_PROGRESS, loader);
        loader.print(startExtensionsOoeration);
        String value = preferencesManager.getValue("ExtensionsPreferences");
        final Jso jso = Jso.deserialize(value == null ? "{}" : value);
        Map<String, Provider> providers = extensionManager.getExtensions();
        for (String extensionFqn : providers.keySet()) {
            Provider extensionProvider = providers.get(extensionFqn);
            boolean enabled = !jso.hasOwnProperty(extensionFqn) || jso.getBooleanField(extensionFqn);
            OperationInfo operationInfo = null;
            try {
                String extName = extensionFqn.substring(extensionFqn.lastIndexOf(".") + 1);
                operationInfo = new OperationInfo(localizedConstants.startingOperation(extName), OperationInfo.Status.IN_PROGRESS, loader);
                loader.print(operationInfo);

                if (enabled) {
                    // this will instantiate extension so it's get enabled
                    // Order of startup is managed by GIN dependency injection framework
                    extensionProvider.get();
                }
                // extension has been enabled
                extensionRegistry.getExtensionDescriptions().get(extensionFqn).setEnabled(enabled);
                operationInfo.setStatus(OperationInfo.Status.FINISHED);
            } catch (Throwable e) {
                Log.error(ExtensionInitializer.class, "Can't initialize extension: " + extensionFqn, e);
                if (operationInfo != null) {
                    operationInfo.setStatus(OperationInfo.Status.ERROR);
                }
            }
        }
        startExtensionsOoeration.setStatus(OperationInfo.Status.FINISHED);
    }

    /** {@inheritDoc} */
    public Map<String, ExtensionDescription> getExtensionDescriptions() {
        return extensionRegistry.getExtensionDescriptions();
    }

}
