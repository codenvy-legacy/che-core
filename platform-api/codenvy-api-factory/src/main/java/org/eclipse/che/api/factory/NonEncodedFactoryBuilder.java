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
package org.eclipse.che.api.factory;

import org.eclipse.che.api.factory.dto.Action;
import org.eclipse.che.api.factory.dto.Actions;
import org.eclipse.che.api.factory.dto.Author;
import org.eclipse.che.api.factory.dto.Factory;
import org.eclipse.che.api.factory.dto.FactoryV2_0;
import org.eclipse.che.api.factory.dto.FactoryV2_1;
import org.eclipse.che.api.factory.dto.Ide;
import org.eclipse.che.api.factory.dto.OnAppClosed;
import org.eclipse.che.api.factory.dto.OnAppLoaded;
import org.eclipse.che.api.factory.dto.OnProjectOpened;
import org.eclipse.che.api.factory.dto.Policies;
import org.eclipse.che.api.factory.dto.Workspace;
import org.eclipse.che.api.project.shared.dto.ImportSourceDescriptor;
import org.eclipse.che.api.project.shared.dto.NewProject;
import org.eclipse.che.api.project.shared.dto.RunnerConfiguration;
import org.eclipse.che.api.project.shared.dto.RunnerSource;
import org.eclipse.che.api.project.shared.dto.RunnersDescriptor;
import org.eclipse.che.api.project.shared.dto.Source;
import org.eclipse.che.api.vfs.shared.dto.ReplacementSet;

import java.util.List;
import java.util.Map;

/**
 * Convert factory to non encode url.
 * This class is used in GWT code directly.
 *
 * @author Sergii Kabashniuk
 */
public abstract class NonEncodedFactoryBuilder {

    /**
     * Convert factory to nonencoded version.
     *
     * @param factory
     *         - factory object.
     * @return - query part of url of nonencoded version
     * @throws java.lang.RuntimeException
     *         if v is null, empty or illegal.
     */
    // TODO affiliateId
    public String buildNonEncoded(Factory factory) {
        if (null == factory.getV() || factory.getV().isEmpty()) {
            throw new RuntimeException("Factory version can't be null or empty");
        }
        StringBuilder result = new StringBuilder();
        switch (factory.getV()) {
            case "2.0":
                buildNonEncoded((FactoryV2_0)factory, result);
                break;
            case "2.1":
                buildNonEncoded((FactoryV2_1)factory, result);
                break;
            default:
                throw new RuntimeException("Factory version '" + factory.getV() + "' not found");
        }
        return result.toString();
    }


    private void buildNonEncoded(FactoryV2_0 factory, StringBuilder builder) {
        appendIfNotNull(builder, "v=", factory.getV(), false);
        final Source source = factory.getSource();
        if (null != source) {
            final ImportSourceDescriptor sourceDescriptor = source.getProject();
            if (null != sourceDescriptor) {
                appendIfNotNull(builder, "&source.project.type=", sourceDescriptor.getType(), false);
                appendIfNotNull(builder, "&source.project.location=", sourceDescriptor.getLocation(), true);
                if (sourceDescriptor.getParameters() != null) {
                    for (Map.Entry<String, String> entry : sourceDescriptor.getParameters().entrySet()) {
                        builder.append("&source.project.parameters.")
                               .append(encode(entry.getKey()))
                               .append("=")
                               .append(encode(entry.getValue()));
                    }
                }
            }
            if (source.getRunners() != null) {
                for (Map.Entry<String, RunnerSource> runnerSource : source.getRunners().entrySet()) {
                    final String prefix = "&source.runners." + encode(runnerSource.getKey());
                    builder.append(prefix)
                           .append(".location=")
                           .append(encode(runnerSource.getValue().getLocation()));
                    if (runnerSource.getValue().getParameters() != null) {
                        for (Map.Entry<String, String> parameter : runnerSource.getValue().getParameters().entrySet()) {
                            builder.append(prefix)
                                   .append(".parameters.")
                                   .append(encode(parameter.getKey()))
                                   .append("=").append(encode(parameter.getValue()));
                        }
                    }
                }
            }
        }

        final Author creator = factory.getCreator();
        if (creator != null) {
            appendIfNotNull(builder, "&creator.name=", creator.getName(), true);
            appendIfNotNull(builder, "&creator.email=", creator.getEmail(), true);
            appendIfNotNull(builder, "&creator.accountId=", creator.getAccountId(), false);
        }

        final Workspace workspace = factory.getWorkspace();
        if (workspace != null) {
            appendIfNotNull(builder, "&workspace.type=", workspace.getType(), false);
        }

        final NewProject project = factory.getProject();
        if (project != null) {
            appendIfNotNull(builder, "&project.name=", project.getName(), true);
            appendIfNotNull(builder, "&project.description=", project.getDescription(), true);
            appendIfNotNull(builder, "&project.type=", project.getType(), true);
            appendIfNotNull(builder, "&project.visibility=", project.getVisibility(), false);
            if (project.getBuilders() != null) {
                appendIfNotNull(builder, "&project.builders.default=", project.getBuilders().getDefault(), true);
            }
            final RunnersDescriptor rDescriptor = project.getRunners();
            if (null != rDescriptor) {
                appendIfNotNull(builder, "&project.runners.default=", rDescriptor.getDefault(), true);
                if (rDescriptor.getConfigs() != null) {
                    for (Map.Entry<String, RunnerConfiguration> rConf : rDescriptor.getConfigs().entrySet()) {
                        final String prefix = "&project.runners.configs." + encode(rConf.getKey());
                        if (rConf.getValue().getRam() > 0) {
                            builder.append(prefix)
                                   .append(".ram=")
                                   .append(rConf.getValue().getRam());
                        }
                        if (rConf.getValue().getVariables() != null) {
                            final String vPrefix = prefix + ".variables";
                            for (Map.Entry<String, String> vars : rConf.getValue().getVariables().entrySet()) {
                                builder.append(vPrefix)
                                       .append(".")
                                       .append(encode(vars.getKey()))
                                       .append("=")
                                       .append(encode(vars.getValue()));
                            }
                        }
                        if (rConf.getValue().getOptions() != null) {
                            final String oPrefix = prefix + ".options";
                            for (Map.Entry<String, String> options : rConf.getValue().getOptions().entrySet()) {
                                builder.append(oPrefix)
                                       .append(".")
                                       .append(encode(options.getKey()))
                                       .append("=")
                                       .append(encode(options.getValue()));
                            }
                        }
                    }
                }
            }
            if (project.getAttributes() != null) {
                for (Map.Entry<String, List<String>> attribute : project.getAttributes().entrySet()) {
                    final String prefix = "&project.attributes." + encode(attribute.getKey());
                    for (String attrValue : attribute.getValue()) {
                        builder.append(prefix)
                               .append("=")
                               .append(encode(attrValue));
                    }
                }
            }
        }

        final Policies policies = factory.getPolicies();
        if (policies != null) {
            appendIfNotNull(builder, "&policies.validSince=", policies.getValidSince(), false);
            appendIfNotNull(builder, "&policies.validUntil=", policies.getValidUntil(), false);
            appendIfNotNull(builder, "&policies.refererHostname=", policies.getRefererHostname(), true);
            appendIfNotNull(builder, "&policies.requireAuthentication=", policies.getRequireAuthentication(), true);
        }

        final Actions actions = factory.getActions();
        if (actions != null) {
            appendIfNotNull(builder, "&actions.openFile=", actions.getOpenFile(), true);
            appendIfNotNull(builder, "&actions.warnOnClose=", actions.getWarnOnClose(), false);
            if (actions.getFindReplace() != null && !actions.getFindReplace().isEmpty()) {
                builder.append("&actions.findReplace=")
                       .append(encode(toJson(actions.getFindReplace())));
            }
        }

    }

    private void buildNonEncoded(FactoryV2_1 factory, StringBuilder builder) {
        buildNonEncoded((FactoryV2_0)factory, builder);

        final Ide ide = factory.getIde();
        if (ide != null) {
            final OnProjectOpened onProjectOpened = ide.getOnProjectOpened();
            if (onProjectOpened != null) {
                List<Action> ideActions = onProjectOpened.getActions();
                for (int i = 0; i < ideActions.size(); i++) {
                    Action action = ideActions.get(i);
                    builder.append("&ide.onProjectOpened.actions.").append(encode("[" + i + "]")).append(".id=").append(action.getId());
                    for (Map.Entry<String, String> property : action.getProperties().entrySet()) {
                        builder.append("&ide.onProjectOpened.actions.").append(encode("[" + i + "]")).append(".properties.")
                               .append(property.getKey()).append("=").append(encode(property.getValue()));
                    }
                }

            }
            final OnAppClosed onAppClosed = ide.getOnAppClosed();
            if (onAppClosed != null) {
                List<Action> ideActions = onAppClosed.getActions();
                for (int i = 0; i < ideActions.size(); i++) {
                    Action action = ideActions.get(i);
                    builder.append("&ide.onAppClosed.actions.").append(encode("[" + i + "]")).append(".id=").append(action.getId());
                    for (Map.Entry<String, String> property : action.getProperties().entrySet()) {
                        builder.append("&ide.onAppClosed.actions.").append(encode("[" + i + "]")).append(".properties.")
                               .append(property.getKey()).append("=").append(encode(property.getValue()));
                    }
                }
            }

            final OnAppLoaded onAppLoaded = ide.getOnAppLoaded();
            if (onAppLoaded != null) {
                List<Action> ideActions = onAppLoaded.getActions();
                for (int i = 0; i < ideActions.size(); i++) {
                    Action action = ideActions.get(i);
                    builder.append("&ide.onAppLoaded.actions.").append(encode("[" + i + "]")).append(".id=").append(action.getId());
                    for (Map.Entry<String, String> property : action.getProperties().entrySet()) {
                        builder.append("&ide.onAppLoaded.actions.").append(encode("[" + i + "]")).append(".properties.")
                               .append(property.getKey()).append("=").append(encode(property.getValue()));
                    }
                }
            }

        }
    }

    private void appendIfNotNull(StringBuilder sb, String key, Object value, boolean encodeValue) {
        if (value != null) {
            if (encodeValue) {
                value = encode(String.valueOf(value));
            }
            sb.append(key).append(String.valueOf(value));
        }
    }

    /**
     * Encode value to be used as a query parameter.
     *
     * @param value
     *         - string to encode.
     * @return - encoded value safe to use as query parameter.
     */
    protected abstract String encode(String value);

    /**
     * Convert object to json
     *
     * @param dto
     *         - initial object
     * @return - json representation of object.
     */
    protected abstract String toJson(List<ReplacementSet> dto);
}
