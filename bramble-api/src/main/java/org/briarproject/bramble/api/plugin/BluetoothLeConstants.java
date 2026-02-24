package org.briarproject.bramble.api.plugin;

import org.briarproject.nullsafety.NotNullByDefault;

@NotNullByDefault
public interface BluetoothLeConstants {

	TransportId ID = new TransportId("org.briarproject.bramble.bluetooth.le");

	// Service UUID for Discovery (Wireless Encrypted Wide Exchange)
	// Derived from "Nasaka WEWE"
	String SERVICE_UUID = "0000aaaa-0000-1000-8000-00805f9b34fb";
	String CHAR_TX_UUID = "0000aaab-0000-1000-8000-00805f9b34fb";
	String CHAR_RX_UUID = "0000aaac-0000-1000-8000-00805f9b34fb";

	// Manufacturer ID for Briar/Nasaka (Placeholder)
	int MANUFACTURER_ID = 0xFFFF;

	// Discovery metadata keys
	String PROP_ADDRESS = "address";
	String PROP_ROLE = "role";
}
