package net.exent.goflyautologger;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import net.exent.goflyautologger.tracklogger.TrackLogger;

import java.util.ArrayList;
import java.util.List;

/* TODO:
 * - detect when track start and end, and call TrackLogger.start() and TrackLogger.stop()
 */
public class GoFlyAutoLoggerBroadcastReceiver extends BroadcastReceiver {
    public static final String ACTION_ATTEMPT_CONNECTION = "net.exent.goflyautologger.action.ATTEMPT_CONNECTION";
    public static final String ACTION_UPDATE_CONFIG = "net.exent.goflyautologger.action.UPDATE_CONFIG";

    private List<TrackLogger> trackLoggers = new ArrayList<>();
    private BluetoothGatt bluetoothGatt;
    private StringBuilder nmeaLog;

    @Override
    public void onReceive(Context context, Intent intent) {
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction()) || ACTION_ATTEMPT_CONNECTION.equals(intent.getAction())) {
            if (bluetoothGatt != null)
                bluetoothGatt.close();
            Log.d(getClass().getName(), "Attempting to connect to a known device");
            connectToDevice(context);
        } else if (ACTION_UPDATE_CONFIG.equals(intent.getAction())) {
            // TODO (add/remove trackloggers, etc)
        }
    }

    private void connectToDevice(final Context context) {
        SharedPreferences devicesPreferences = context.getSharedPreferences(GoFlyAutoLogger.SHARED_PREFERENCES_BLUETOOTH_DEVICES, Context.MODE_PRIVATE);
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.i(getClass().getName(), "Bluetooth not enabled, not attempting to connect to device");
            return;
        }
        for (String key : devicesPreferences.getAll().keySet()) {
            final String[] parts = key.split(";");
            if (parts.length != 3) {
                Log.i(getClass().getName(), "Don't know how to handle this device, removing from list: " + key);
                devicesPreferences.edit().remove(key).apply();
                continue;
            }
            if (!devicesPreferences.getBoolean(key, false)) {
                Log.i(getClass().getName(), "Device not enabled, skipping: " + key);
                continue;
            }

            final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(parts[1]);
            bluetoothGatt = device.connectGatt(context, true, new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        Log.d(getClass().getName(), "STATE_CONNECTED: " + status);
                        nmeaLog = new StringBuilder();
                        gatt.discoverServices();
                    } else {
                        Log.d(getClass().getName(), "STATE_DISCONNECTED: " + status);
                        // YAAH: http://stackoverflow.com/questions/17870189/android-4-3-bluetooth-low-energy-unstable
                        // having an issue where bluetooth won't always reconnect
                        // code below might fix it, but it seem to do some extra work (meaning more power drain)
                        //gatt.close();
                        //bluetoothGatt = device.connectGatt(context, false, this);
                    }
                }

                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    Log.d(getClass().getName(), "onServicesDiscovered: " + status);
                    for (BluetoothGattService service : gatt.getServices()) {
                        for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                            if (parts[2].equals(characteristic.getUuid().toString())) {
                                gatt.setCharacteristicNotification(characteristic, true);
                                return;
                            }
                        }
                    }
                }

                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                    nmeaLog.append(characteristic.getStringValue(0).replace("\r", "").replace("\n", ""));
                    int startPos;
                    int endPos;
                    while ((startPos = nmeaLog.indexOf("$")) >= 0 && (endPos = nmeaLog.indexOf("$", startPos + 1)) > 0) {
                        for (TrackLogger trackLogger : trackLoggers)
                            trackLogger.nmeaEntry(context, nmeaLog.substring(startPos, endPos));
                        nmeaLog.delete(0, endPos);
                    }
                }
            });
        }
    }
}
