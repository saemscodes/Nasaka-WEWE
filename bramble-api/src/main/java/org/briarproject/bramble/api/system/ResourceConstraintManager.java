package org.briarproject.bramble.api.system;

import org.briarproject.nullsafety.NotNullByDefault;

@NotNullByDefault
public interface ResourceConstraintManager {

    /**
     * Returns true if resources (battery, data, etc.) are scarce.
     */
    boolean isResourceScarcity();
}
