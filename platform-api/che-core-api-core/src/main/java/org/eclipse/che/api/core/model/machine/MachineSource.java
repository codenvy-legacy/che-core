package org.eclipse.che.api.core.model.machine;

/**
 * @author gazarenkov
 */
public interface MachineSource {

    /**
     * Recipe or Snapshot
     * @return
     */
    String getType();

    /**
     * URL or ID
     * @return
     */
    String getLocation();
}
