package net.exent.goflyautologger;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import net.exent.goflyautologger.fragment.Overview;

public class GoFlyAutoLogger extends Activity {
    public static final String SHARED_PREFERENCES_BLUETOOTH_DEVICES = "bluetooth_devices";

    private BroadcastReceiver broadcastReceiver = new GoFlyAutoLoggerBroadcastReceiver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_go_fly_auto_logger);
        getFragmentManager().beginTransaction().add(R.id.fragment_container, new Overview()).commit();
        Log.d(getClass().getName(), "Sending broadcast");
        sendBroadcast(new Intent(GoFlyAutoLoggerBroadcastReceiver.ACTION_ATTEMPT_CONNECTION));

        // TODO: fragments are overlapping each other when rotating screen... why?
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(broadcastReceiver, new IntentFilter());
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(broadcastReceiver);
    }
}
