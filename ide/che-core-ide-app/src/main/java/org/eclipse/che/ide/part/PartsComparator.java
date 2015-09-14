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
package org.eclipse.che.ide.part;

import org.eclipse.che.ide.api.constraints.Anchor;
import org.eclipse.che.ide.api.constraints.Constraints;
import org.eclipse.che.ide.api.parts.PartPresenter;

import javax.validation.constraints.NotNull;
import java.util.Comparator;
import java.util.Map;

/**
 * @author Dmitry Shnurenko
 */
public class PartsComparator implements Comparator<PartPresenter> {

    private Map<PartPresenter, Constraints> constraints;

    public void setConstraints(@NotNull Map<PartPresenter, Constraints> constraints) {
        this.constraints = constraints;
    }

    @Override
    public int compare(PartPresenter part1, PartPresenter part2) {
        String title1 = part1.getTitle();
        String title2 = part2.getTitle();
        Constraints constr1 = constraints.get(part1);
        Constraints constr2 = constraints.get(part2);

        if (constr1 == null && constr2 == null) {
            return 0;
        }

        if ((constr1 != null && constr1.myAnchor == Anchor.FIRST) || (constr2 != null && constr2.myAnchor == Anchor.LAST)) {
            return -1;
        }

        if ((constr2 != null && constr2.myAnchor == Anchor.FIRST) || (constr1 != null && constr1.myAnchor == Anchor.LAST)) {
            return 1;
        }

        if (constr1 != null && constr1.myRelativeToActionId != null) {
            Anchor anchor1 = constr1.myAnchor;
            String relative1 = constr1.myRelativeToActionId;
            if (anchor1 == Anchor.BEFORE && relative1.equals(title2)) {
                return -1;
            }
            if (anchor1 == Anchor.AFTER && relative1.equals(title2)) {
                return 1;
            }
        }

        if (constr2 != null && constr2.myRelativeToActionId != null) {
            Anchor anchor2 = constr2.myAnchor;
            String relative2 = constr2.myRelativeToActionId;
            if (anchor2 == Anchor.BEFORE && relative2.equals(title1)) {
                return 1;
            }
            if (anchor2 == Anchor.AFTER && relative2.equals(title1)) {
                return -1;
            }
        }

        if (constr1 != null && constr2 == null) {
            return 1;
        }
        if (constr1 == null) {
            return -1;
        }
        return 0;

    }
}
