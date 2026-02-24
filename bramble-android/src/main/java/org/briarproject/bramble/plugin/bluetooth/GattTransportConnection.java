package org.briarproject.bramble.plugin.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;

import org.briarproject.android.dontkillmelib.wakelock.AndroidWakeLock;
import org.briarproject.android.dontkillmelib.wakelock.AndroidWakeLockManager;
import org.briarproject.bramble.api.plugin.Plugin;
import org.briarproject.bramble.api.plugin.duplex.AbstractDuplexTransportConnection;
import org.briarproject.nullsafety.NotNullByDefault;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.UUID;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import static java.util.logging.Logger.getLogger;
import static org.briarproject.bramble.api.plugin.BluetoothLeConstants.CHAR_RX_UUID;
import static org.briarproject.bramble.api.plugin.BluetoothLeConstants.CHAR_TX_UUID;
import static org.briarproject.bramble.api.plugin.BluetoothLeConstants.PROP_ADDRESS;

@NotNullByDefault
class GattTransportConnection extends AbstractDuplexTransportConnection {

	private static final Logger LOG = getLogger(GattTransportConnection.class.getName());

	private final BluetoothDevice device;
	@Nullable private final BluetoothGatt gattClient; // Present if we are the client
	@Nullable private final BluetoothGattServer gattServer; // Present if we are the server
	private final AndroidWakeLock wakeLock;

	private final PipedInputStream inputStream;
	private final PipedOutputStream outputToInput;
	private final GattOutputStream outputStream;

	GattTransportConnection(Plugin plugin,
			AndroidWakeLockManager wakeLockManager,
			BluetoothDevice device,
			@Nullable BluetoothGatt gattClient,
			@Nullable BluetoothGattServer gattServer) throws IOException {
		super(plugin);
		this.device = device;
		this.gattClient = gattClient;
		this.gattServer = gattServer;

		this.inputStream = new PipedInputStream(1024 * 16);
		this.outputToInput = new PipedOutputStream(inputStream);
		this.outputStream = new GattOutputStream();

		this.wakeLock = wakeLockManager.createWakeLock("GattConnection");
		this.wakeLock.acquire();

		remote.put(PROP_ADDRESS, device.getAddress());
	}

	@Override
	protected InputStream getInputStream() {
		return inputStream;
	}

	@Override
	protected OutputStream getOutputStream() {
		return outputStream;
	}

	@Override
	protected void closeConnection(boolean exception) throws IOException {
		try {
			if (gattClient != null) gattClient.disconnect();
			inputStream.close();
			outputToInput.close();
		} finally {
			wakeLock.release();
		}
	}

	/**
	 * Called when data is received via GATT (Write request on server, or Notify on client).
	 */
	void receiveData(byte[] data) {
		try {
			outputToInput.write(data);
			outputToInput.flush();
		} catch (IOException e) {
			LOG.warning("Error writing GATT data to input stream: " + e.getMessage());
		}
	}

	private class GattOutputStream extends OutputStream {
		@Override
		public void write(int b) throws IOException {
			write(new byte[]{(byte) b});
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			if (len == 0) return;
			
			byte[] chunk = new byte[len];
			System.arraycopy(b, off, chunk, 0, len);

			if (gattClient != null) {
				// We are client, write to server's TX char
				BluetoothGattCharacteristic txChar = gattClient.getService(
						UUID.fromString(org.briarproject.bramble.api.plugin.BluetoothLeConstants.SERVICE_UUID))
						.getCharacteristic(UUID.fromString(CHAR_TX_UUID));
				if (txChar != null) {
					txChar.setValue(chunk);
					gattClient.writeCharacteristic(txChar);
				}
			} else if (gattServer != null) {
				// We are server, notify client on RX char
				BluetoothGattCharacteristic rxChar = gattServer.getService(
						UUID.fromString(org.briarproject.bramble.api.plugin.BluetoothLeConstants.SERVICE_UUID))
						.getCharacteristic(UUID.fromString(CHAR_RX_UUID));
				if (rxChar != null) {
					rxChar.setValue(chunk);
					gattServer.notifyCharacteristicChanged(device, rxChar, false);
				}
			}
		}
	}
}
