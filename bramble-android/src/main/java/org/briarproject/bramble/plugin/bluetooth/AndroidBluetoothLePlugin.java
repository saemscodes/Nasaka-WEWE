package org.briarproject.bramble.plugin.bluetooth;

import android.annotation.SuppressLint;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Build;
import android.os.ParcelUuid;

import org.briarproject.android.dontkillmelib.wakelock.AndroidWakeLockManager;
import org.briarproject.bramble.api.account.AccountManager;
import org.briarproject.bramble.api.io.TimeoutMonitor;
import org.briarproject.bramble.api.plugin.Backoff;
import org.briarproject.bramble.api.plugin.BluetoothLeConstants;
import org.briarproject.bramble.api.plugin.PluginCallback;
import org.briarproject.bramble.api.plugin.PluginException;
import org.briarproject.bramble.api.plugin.TransportId;
import org.briarproject.bramble.api.plugin.duplex.DuplexTransportConnection;
import org.briarproject.bramble.api.system.AndroidExecutor;
import org.briarproject.bramble.api.system.Clock;
import org.briarproject.nullsafety.MethodsNotNullByDefault;
import org.briarproject.nullsafety.ParametersNotNullByDefault;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import static android.bluetooth.le.AdvertiseSettings.ADVERTISE_MODE_LOW_POWER;
import static android.bluetooth.le.AdvertiseSettings.ADVERTISE_TX_POWER_LOW;
import static android.bluetooth.le.ScanSettings.CALLBACK_TYPE_ALL_MATCHES;
import static android.bluetooth.le.ScanSettings.MATCH_MODE_AGGRESSIVE;
import static android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_POWER;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.WARNING;
import static java.util.logging.Logger.getLogger;
import static org.briarproject.bramble.util.AndroidUtils.hasBtConnectPermission;

@MethodsNotNullByDefault
@ParametersNotNullByDefault
@SuppressLint("MissingPermission")
class AndroidBluetoothLePlugin implements BluetoothLePlugin {

	private static final Logger LOG =
			getLogger(AndroidBluetoothLePlugin.class.getName());

	private final Executor ioExecutor;
	private final AndroidExecutor androidExecutor;
	private final AndroidWakeLockManager wakeLockManager;
	private final Application app;
	private final Clock clock;
	private final TimeoutMonitor timeoutMonitor;
	private final Backoff backoff;
	private final PluginCallback callback;
	private final AccountManager accountManager;
	private final SecureRandom secureRandom;

	private final Map<BluetoothDevice, GattTransportConnection> connections =
			new ConcurrentHashMap<>();

	private volatile BluetoothAdapter adapter = null;
	private volatile BluetoothLeScanner scanner = null;
	private volatile BluetoothLeAdvertiser advertiser = null;
	private volatile BluetoothGattServer gattServer = null;
	private volatile boolean running = false;
	private volatile boolean advertising = false;

	private final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());

	AndroidBluetoothLePlugin(Executor ioExecutor,
			AndroidExecutor androidExecutor,
			AndroidWakeLockManager wakeLockManager,
			Application app,
			Clock clock,
			TimeoutMonitor timeoutMonitor,
			Backoff backoff,
			PluginCallback callback,
			AccountManager accountManager,
			SecureRandom secureRandom) {
		this.ioExecutor = ioExecutor;
		this.androidExecutor = androidExecutor;
		this.wakeLockManager = wakeLockManager;
		this.app = app;
		this.clock = clock;
		this.timeoutMonitor = timeoutMonitor;
		this.backoff = backoff;
		this.callback = callback;
		this.accountManager = accountManager;
		this.secureRandom = secureRandom;
	}

	@Override
	public TransportId getId() {
		return BluetoothLeConstants.ID;
	}

	@Override
	public long getMaxLatency() {
		return 30000;
	}

	@Override
	public int getMaxIdleTime() {
		return 30000;
	}

	@Override
	public void start() throws PluginException {
		if (running) return;
		if (!hasBtConnectPermission(app)) {
			LOG.info("Bluetooth permissions not granted");
			return;
		}

		BluetoothManager bm = (BluetoothManager) app.getSystemService(
				Context.BLUETOOTH_SERVICE);
		adapter = bm.getAdapter();
		if (adapter == null || !adapter.isEnabled()) {
			LOG.info("Bluetooth adapter disabled or not supported");
			return;
		}

		scanner = adapter.getBluetoothLeScanner();
		advertiser = adapter.getBluetoothLeAdvertiser();

		if (scanner == null || advertiser == null) {
			LOG.info("BLE Scanning or Advertising not supported");
			return;
		}

		startGattServer();
		running = true;
		scheduleAdvertisingCycle();
		startScanning();
	}

	private void startGattServer() {
		BluetoothManager bm = (BluetoothManager) app.getSystemService(
				Context.BLUETOOTH_SERVICE);
		gattServer = bm.openGattServer(app, gattServerCallback);
		if (gattServer == null) {
			LOG.warning("Could not open GATT server");
			return;
		}

		BluetoothGattService service = new BluetoothGattService(
				UUID.fromString(BluetoothLeConstants.SERVICE_UUID),
				BluetoothGattService.SERVICE_TYPE_PRIMARY);

		BluetoothGattCharacteristic txChar = new BluetoothGattCharacteristic(
				UUID.fromString(BluetoothLeConstants.CHAR_TX_UUID),
				BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
				BluetoothGattCharacteristic.PERMISSION_WRITE);

		BluetoothGattCharacteristic rxChar = new BluetoothGattCharacteristic(
				UUID.fromString(BluetoothLeConstants.CHAR_RX_UUID),
				BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
				BluetoothGattCharacteristic.PERMISSION_READ);

		service.addCharacteristic(txChar);
		service.addCharacteristic(rxChar);
		gattServer.addService(service);
		LOG.info("GATT Server started for Nasaka WEWE");
	}

	private void stopGattServer() {
		if (gattServer != null) {
			gattServer.close();
			gattServer = null;
			LOG.info("GATT Server stopped");
		}
	}

	private void scheduleAdvertisingCycle() {
		if (!running) return;

		// 200ms on / 5s off framework requirement
		startAdvertising();
		handler.postDelayed(() -> {
			stopAdvertising();
			if (running) {
				handler.postDelayed(this::scheduleAdvertisingCycle, 5000);
			}
		}, 200);
	}

	@Override
	public void stop() {
		if (!running) return;
		running = false;
		handler.removeCallbacksAndMessages(null);
		stopAdvertising();
		stopScanning();
		stopGattServer();
	}

	private void startAdvertising() {
		if (advertiser == null || advertising) return;

		AdvertiseSettings settings = new AdvertiseSettings.Builder()
				.setAdvertiseMode(ADVERTISE_MODE_LOW_POWER)
				.setConnectable(true)
				.setTimeout(0)
				.setTxPowerLevel(ADVERTISE_TX_POWER_LOW)
				.build();

		int role = accountManager.getRole();
		byte[] roleData = ByteBuffer.allocate(4).putInt(role).array();

		AdvertiseData data = new AdvertiseData.Builder()
				.setIncludeDeviceName(false)
				.addServiceUuid(new ParcelUuid(UUID.fromString(BluetoothLeConstants.SERVICE_UUID)))
				// In Briar, Service Data is often used for discovery metadata
				.addServiceData(new ParcelUuid(UUID.fromString(BluetoothLeConstants.SERVICE_UUID)), roleData)
				.build();

		advertiser.startAdvertising(settings, data, advertiseCallback);
		advertising = true;
		LOG.info("BLE Advertising cycle: ON (Role: " + role + ")");
	}

	private void stopAdvertising() {
		if (advertiser != null && advertising) {
			advertiser.stopAdvertising(advertiseCallback);
			advertising = false;
			LOG.info("BLE Advertising cycle: OFF");
		}
	}

	private void startScanning() {
		if (scanner == null) return;

		ScanSettings settings = new ScanSettings.Builder()
				.setScanMode(SCAN_MODE_LOW_POWER)
				.setCallbackType(CALLBACK_TYPE_ALL_MATCHES)
				.setMatchMode(MATCH_MODE_AGGRESSIVE)
				.build();

		ScanFilter filter = new ScanFilter.Builder()
				.setServiceUuid(new ParcelUuid(UUID.fromString(BluetoothLeConstants.SERVICE_UUID)))
				.build();

		scanner.startScan(Collections.singletonList(filter), settings, scanCallback);
		LOG.info("BLE Scanning started for Nasaka WEWE discovery");
	}

	private void stopScanning() {
		if (scanner != null) {
			scanner.stopScan(scanCallback);
			LOG.info("BLE Scanning stopped");
		}
	}

	private final AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
		@Override
		public void onStartSuccess(AdvertiseSettings settingsInEffect) {
			LOG.info("BLE Advertisement successful");
		}

		@Override
		public void onStartFailure(int errorCode) {
			LOG.warning("BLE Advertisement failed: " + errorCode);
			advertising = false;
		}
	};

	private final BluetoothGattServerCallback gattServerCallback = new BluetoothGattServerCallback() {
		@Override
		public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
			if (newState == BluetoothGatt.STATE_CONNECTED) {
				LOG.info("GATT Client connected: " + device.getAddress());
			} else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
				LOG.info("GATT Client disconnected: " + device.getAddress());
			}
		}

		@Override
		public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
												BluetoothGattCharacteristic characteristic,
												boolean preparedWrite, boolean responseNeeded,
												int offset, byte[] value) {
			if (characteristic.getUuid().equals(UUID.fromString(BluetoothLeConstants.CHAR_TX_UUID))) {
				if (responseNeeded) {
					gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null);
				}
				
				GattTransportConnection conn = connections.get(device);
				if (conn == null) {
					try {
						conn = new GattTransportConnection(AndroidBluetoothLePlugin.this,
								wakeLockManager, device, null, gattServer);
						connections.put(device, conn);
						callback.incomingConnectionCreated(conn);
					} catch (IOException e) {
						LOG.warning("Failed to create incoming GATT connection: " + e.getMessage());
						return;
					}
				}
				conn.receiveData(value);
			}
		}
	};

	private final ScanCallback scanCallback = new ScanCallback() {
		@Override
		public void onScanResult(int callbackType, ScanResult result) {
			BluetoothDevice device = result.getDevice();
			byte[] serviceData = result.getScanRecord().getServiceData(
					new ParcelUuid(UUID.fromString(BluetoothLeConstants.SERVICE_UUID)));
			
			if (serviceData != null && serviceData.length >= 4) {
				int remoteRole = ByteBuffer.wrap(serviceData).getInt();
				if (LOG.isLoggable(INFO)) {
					LOG.info("Discovered Nasaka WEWE node: " + device.getAddress() + 
							" with Role: " + remoteRole);
				}
				connectToPeer(device);
			}
		}

		@Override
		public void onScanFailed(int errorCode) {
			LOG.warning("BLE Scan failed: " + errorCode);
		}
	};

	private void connectToPeer(BluetoothDevice device) {
		LOG.info("Connecting to GATT server on " + device.getAddress());
		device.connectGatt(app, false, gattClientCallback, BluetoothDevice.TRANSPORT_LE);
	}

	private final android.bluetooth.BluetoothGattCallback gattClientCallback = new android.bluetooth.BluetoothGattCallback() {
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
			if (newState == BluetoothGatt.STATE_CONNECTED) {
				LOG.info("Connected to GATT server: " + gatt.getDevice().getAddress());
				// Request MTU 512 as per framework metrics
				gatt.requestMtu(512);
			} else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
				LOG.info("Disconnected from GATT server: " + gatt.getDevice().getAddress());
				gatt.close();
			}
		}

		@Override
		public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				LOG.info("Negotiated MTU: " + mtu);
				gatt.discoverServices();
			} else {
				LOG.warning("MTU negotiation failed, status: " + status);
				gatt.discoverServices();
			}
		}

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				LOG.info("Services discovered on " + gatt.getDevice().getAddress());
				BluetoothGattService service = gatt.getService(UUID.fromString(BluetoothLeConstants.SERVICE_UUID));
				if (service != null) {
					LOG.info("Nasaka WEWE service found!");
					BluetoothDevice device = gatt.getDevice();
					try {
						GattTransportConnection conn = new GattTransportConnection(
								AndroidBluetoothLePlugin.this, wakeLockManager, device, gatt, null);
						connections.put(device, conn);
						// In real Briar, we might need to handle this differently
						// for discovery vs connection, but for now this is Full Ham discovery
					} catch (IOException e) {
						LOG.warning("Failed to create GATT connection: " + e.getMessage());
					}
				}
			}
		}
	};

	@Override
	public State getState() {
		return running ? State.ACTIVE : State.INACTIVE;
	}

	@Override
	public int getReasonsDisabled() {
		return 0;
	}

	@Override
	public boolean shouldPoll() {
		return false;
	}

	@Override
	public int getPollingInterval() {
		return 0;
	}

	@Override
	public void poll(List<DuplexTransportConnection> properties) {
		// Not used for BLE discovery
	}

	@Override
	public void poll(java.util.Collection<org.briarproject.bramble.api.Pair<org.briarproject.bramble.api.properties.TransportProperties, org.briarproject.bramble.api.plugin.ConnectionHandler>> properties) {
		// Not used for BLE discovery
	}

	@Override
	public DuplexTransportConnection createConnection(org.briarproject.bramble.api.properties.TransportProperties p) {
		return null; // Connections are handled via Bluetooth classic or future GATT support
	}

	@Override
	public boolean supportsKeyAgreement() {
		return false;
	}

	@Override
	public org.briarproject.bramble.api.keyagreement.KeyAgreementListener createKeyAgreementListener(byte[] commitment) {
		return null;
	}

	@Override
	public DuplexTransportConnection createKeyAgreementConnection(byte[] commitment, org.briarproject.bramble.api.data.BdfList descriptor) {
		return null;
	}

	@Override
	public boolean isDiscovering() {
		return running;
	}

	@Override
	public void disablePolling() {
	}

	@Override
	public void enablePolling() {
	}

	@Override
	public DuplexTransportConnection discoverAndConnectForSetup(String uuid) {
		return null;
	}

	@Override
	public boolean supportsRendezvous() {
		return false;
	}

	@Override
	public org.briarproject.bramble.api.rendezvous.RendezvousEndpoint createRendezvousEndpoint(org.briarproject.bramble.api.rendezvous.KeyMaterialSource k, boolean alice, org.briarproject.bramble.api.plugin.ConnectionHandler incoming) {
		return null;
	}
}
