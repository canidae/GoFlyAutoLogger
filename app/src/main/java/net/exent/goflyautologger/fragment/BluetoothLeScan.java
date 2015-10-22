package net.exent.goflyautologger.fragment;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;

import net.exent.goflyautologger.GoFlyAutoLogger;
import net.exent.goflyautologger.R;

import java.util.Iterator;
import java.util.List;

public class BluetoothLeScan extends Fragment {
    private DeviceListArrayAdapter deviceListArrayAdapter;
    private BluetoothLeScanner bluetoothLeScanner;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_bluetooth_le_scan, container, false);
        deviceListArrayAdapter = new DeviceListArrayAdapter(getActivity());
        ListView listView = (ListView) view.findViewById(R.id.bluetoothLeScan_deviceList);
        listView.setAdapter(deviceListArrayAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CheckBox checkBox = (CheckBox) view.findViewById(R.id.bluetoothLeScan_checkBox);
                checkBox.setChecked(!checkBox.isChecked());
                SharedPreferences sharedPreferences = getActivity().getSharedPreferences(GoFlyAutoLogger.SHARED_PREFERENCES_BLUETOOTH_DEVICES, Context.MODE_PRIVATE);
                sharedPreferences.edit().putBoolean(checkBox.getText().toString(), checkBox.isChecked()).apply();
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                CheckBox checkBox = (CheckBox) view.findViewById(R.id.bluetoothLeScan_checkBox);
                SharedPreferences sharedPreferences = getActivity().getSharedPreferences(GoFlyAutoLogger.SHARED_PREFERENCES_BLUETOOTH_DEVICES, Context.MODE_PRIVATE);
                sharedPreferences.edit().remove(checkBox.getText().toString()).apply();
                deviceListArrayAdapter.notifyDataSetChanged();
                return true;
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        // TODO: not working properly, throwing exceptions in all directions
        super.onResume();
        Log.d(getClass().getName(), "Started scanning for Bluetooth LE devices");
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
            // TODO: need to enable bluetooth before showing this fragment
        } else {
            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
            bluetoothLeScanner.startScan(new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    final BluetoothDevice device = result.getDevice();

                    Log.d(getClass().getName(), device.getName() + ": " + device.getAddress());
                    if ("GoFly".equals(device.getName())) {
                        device.connectGatt(getActivity(), false, new BluetoothGattCallback() {
                            StringBuilder data = new StringBuilder();
                            @Override
                            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                                Log.d(getClass().getName(), "onConnectionStateChange: " + status + " - " + newState);
                                switch (newState) {
                                    case BluetoothProfile.STATE_CONNECTED:
                                        Log.d(getClass().getName(), "STATE_CONNECTED");
                                        gatt.discoverServices();
                                        break;
                                    case BluetoothProfile.STATE_DISCONNECTED:
                                        Log.d(getClass().getName(), "STATE_DISCONNECTED");
                                        break;
                                    default:
                                        Log.d(getClass().getName(), "STATE_OTHER");
                                        break;
                                }
                            }

                            @Override
                            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                                Log.d(getClass().getName(), "onServicesDiscovered: " + status);
                                List<BluetoothGattService> services = gatt.getServices();
                                Log.d(getClass().getName(), "onServicesDiscovered: " + services.toString());
                                for (BluetoothGattService service : services) {
                                    for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                                        Log.d(getClass().getName(), "Characteristics: " + characteristic.getProperties() + " - " + characteristic.getPermissions());
                                        Log.d(getClass().getName(), "setCharacteristicNotification (" + characteristic.getUuid().toString() + "): " + gatt.setCharacteristicNotification(characteristic, true));
                                    }
                                }
                            }

                            @Override
                            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                                String text = characteristic.getStringValue(0);
                                if (text.contains("$GPGGA,") || text.contains("$GPRMC,") || text.contains("$PTAS1,")) {
                                    Log.d(getClass().getName(), "Discovered valid device: " + device.getName() + " (" + device.getAddress() + "): " + characteristic.getUuid() + " - " + text);
                                    SharedPreferences sharedPreferences = getActivity().getSharedPreferences(GoFlyAutoLogger.SHARED_PREFERENCES_BLUETOOTH_DEVICES, Context.MODE_PRIVATE);
                                    String key = device.getName() + ";" + device.getAddress() + ";" + characteristic.getUuid();
                                    if (!sharedPreferences.contains(key)) {
                                        sharedPreferences.edit().putBoolean(key, false).apply();
                                        deviceListArrayAdapter.notifyDataSetChanged(); // TODO: not allowed here (Only the original thread that created a view hierarchy can touch its views)
                                    }
                                    gatt.disconnect();
                                    gatt.close();
                                }
                            }
                        });
                    }
                }
            });
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (bluetoothLeScanner == null)
            return;
        bluetoothLeScanner.stopScan(new ScanCallback() {
            // TODO: create a proper class and use it in both scan and stopScan methods, perhaps?
        });
    }

    private class DeviceListArrayAdapter extends ArrayAdapter<BluetoothDevice> {
        private SharedPreferences sharedPreferences = getActivity().getSharedPreferences(GoFlyAutoLogger.SHARED_PREFERENCES_BLUETOOTH_DEVICES, Context.MODE_PRIVATE);

        public DeviceListArrayAdapter(Context context) {
            super(context, R.layout.list_entry_bluetooth_le_scan);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.list_entry_bluetooth_le_scan, parent, false);
            CheckBox checkBox = (CheckBox) rowView.findViewById(R.id.bluetoothLeScan_checkBox);
            Iterator<String> iterator = sharedPreferences.getAll().keySet().iterator();
            while (--position > 0)
                iterator.next();
            String text = iterator.next();
            checkBox.setText(text);
            checkBox.setChecked(sharedPreferences.getBoolean(text, false));
            return rowView;
        }

        @Override
        public int getCount() {
            return sharedPreferences.getAll().size();
        }
    }
}
