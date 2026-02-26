package org.briarproject.briar.android.util;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import org.briarproject.bramble.api.system.ResourceConstraintManager;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ResourceManager implements ResourceConstraintManager {

    private final Context context;

    @Inject
    public ResourceManager(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Returns true if the battery is low (below 15% and not charging).
     */
    public boolean isBatteryScarcity() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);
        if (batteryStatus == null)
            return false;

        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        float batteryPct = level * 100 / (float) scale;

        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;

        return batteryPct < 15 && !isCharging;
    }

    /**
     * Returns true if the device is on a metered connection (Mobile Data).
     */
    public boolean isDataScarcity() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null)
            return false;

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork == null)
            return false;

        return activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE;
    }

    /**
     * Comprehensive scarcity check.
     */
    public boolean isResourceScarcity() {
        return isBatteryScarcity() || isDataScarcity();
    }
}
