package org.briarproject.bramble.api.identity;

import org.briarproject.nullsafety.NotNullByDefault;

@NotNullByDefault
public interface Role {
	int CITIZEN = 0;
	int OBSERVER = 1;
	int JOURNALIST = 2;
	int COORDINATOR = 3;
}
