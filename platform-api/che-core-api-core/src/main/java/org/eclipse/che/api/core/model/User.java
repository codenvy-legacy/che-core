package org.eclipse.che.api.core.model;

import model.user.Role;

import java.util.Set;

/**
 * @author gazarenkov
 */
public interface User {

    String getId();

    String getName();

    Set<? extends Role> getRoles();
}
