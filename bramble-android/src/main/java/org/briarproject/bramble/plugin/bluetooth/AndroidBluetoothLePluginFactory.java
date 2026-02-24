package org.briarproject.bramble.plugin.bluetooth;

import android.app.Application;

import org.briarproject.bramble.api.account.AccountManager;
import org.briarproject.android.dontkillmelib.wakelock.AndroidWakeLockManager;
import org.briarproject.bramble.api.io.TimeoutMonitor;
import org.briarproject.bramble.api.lifecycle.IoExecutor;
import org.briarproject.bramble.api.plugin.Backoff;
import org.briarproject.bramble.api.plugin.BackoffFactory;
import org.briarproject.bramble.api.plugin.BluetoothLeConstants;
import org.briarproject.bramble.api.plugin.PluginCallback;
import org.briarproject.bramble.api.plugin.TransportId;
import org.briarproject.bramble.api.plugin.duplex.DuplexPlugin;
import org.briarproject.bramble.api.plugin.duplex.DuplexPluginFactory;
import org.briarproject.bramble.api.system.AndroidExecutor;
import org.briarproject.bramble.api.system.Clock;
import org.briarproject.nullsafety.NotNullByDefault;

import java.security.SecureRandom;
import java.util.concurrent.Executor;

import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;

@Immutable
@NotNullByDefault
public class AndroidBluetoothLePluginFactory implements DuplexPluginFactory {

	private static final int MIN_POLLING_INTERVAL = 60 * 1000; // 1 minute
	private static final int MAX_POLLING_INTERVAL = 10 * 60 * 1000; // 10 mins
	private static final double BACKOFF_BASE = 1.2;

	private final Executor ioExecutor;
	private final AndroidExecutor androidExecutor;
	private final AndroidWakeLockManager wakeLockManager;
	private final Application app;
	private final Clock clock;
	private final TimeoutMonitor timeoutMonitor;
	private final BackoffFactory backoffFactory;
	private final AccountManager accountManager;
	private final SecureRandom secureRandom;

	@Inject
	AndroidBluetoothLePluginFactory(@IoExecutor Executor ioExecutor,
			AndroidExecutor androidExecutor,
			AndroidWakeLockManager wakeLockManager,
			Application app,
			Clock clock,
			TimeoutMonitor timeoutMonitor,
			BackoffFactory backoffFactory,
			AccountManager accountManager,
			SecureRandom secureRandom) {
		this.ioExecutor = ioExecutor;
		this.androidExecutor = androidExecutor;
		this.wakeLockManager = wakeLockManager;
		this.app = app;
		this.clock = clock;
		this.timeoutMonitor = timeoutMonitor;
		this.backoffFactory = backoffFactory;
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
	public DuplexPlugin createPlugin(PluginCallback callback) {
		Backoff backoff = backoffFactory.createBackoff(MIN_POLLING_INTERVAL,
				MAX_POLLING_INTERVAL, BACKOFF_BASE);
		return new AndroidBluetoothLePlugin(ioExecutor, androidExecutor,
				wakeLockManager, app, clock, timeoutMonitor, backoff,
				callback, accountManager, secureRandom);
	}
}
