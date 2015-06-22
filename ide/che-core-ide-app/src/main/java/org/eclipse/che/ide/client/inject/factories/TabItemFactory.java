package org.eclipse.che.ide.client.inject.factories;

import org.eclipse.che.ide.api.parts.PartStackView.TabItem;

import javax.annotation.Nonnull;

/**
 * @author Dmitry Shnurenko
 */
public interface TabItemFactory {

    TabItem createPartButton(@Nonnull String title);
}
