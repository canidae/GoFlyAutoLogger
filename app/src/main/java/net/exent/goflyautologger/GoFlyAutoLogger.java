package net.exent.goflyautologger;

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
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import java.util.List;

public class GoFlyAutoLogger extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_go_fly_auto_logger);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, 1);
                } else {
                    final BluetoothLeScanner leScanner = bluetoothAdapter.getBluetoothLeScanner();
                    leScanner.startScan(new ScanCallback() {
                        @Override
                        public void onScanResult(int callbackType, ScanResult result) {
                            BluetoothDevice device = result.getDevice();
                            Log.d(getClass().getName(), device.getName() + ": " + device.getAddress());
                            if ("GoFly".equals(device.getName())) {
                                leScanner.stopScan(this);
                                device.connectGatt(getApplicationContext(), false, new BluetoothGattCallback() {
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
                                                Log.d(getClass().getName(), "setCharacteristicNotification (" + characteristic.getUuid().toString() + "): " + gatt.setCharacteristicNotification(characteristic, true));
                                            }
                                        }
                                        gatt.readCharacteristic(services.get(0).getCharacteristics().get(0));
                                    }

                                    @Override
                                    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                                        Log.d(getClass().getName(), "onCharacteristicRead: " + characteristic.getUuid().toString() + " - " + status);
                                    }

                                    @Override
                                    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                                        String text = characteristic.getStringValue(0).replace("\r", "").replace("\n", "");
                                        if (text.length() > 0 && text.charAt(0) == '$') {
                                            Log.d(getClass().getName(), data.toString());
                                            data.setLength(0);
                                        }
                                        data.append(text);
                                        //Log.d(getClass().getName(), characteristic.getUuid().toString() + " - " + characteristic.getStringValue(0).replace("\r", "").replace("\n", ""));
                                    }
                                });
                            }
                        }
                    });
                }
                Snackbar.make(view, "Bluetooth enabled: " + bluetoothAdapter.isEnabled(), Snackbar.LENGTH_LONG).setAction("Action", null).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_go_fly_auto_logger, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
