package net.exent.goflyautologger;

import android.app.Activity;
import android.os.Bundle;

public class GoFlyAutoLogger extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_go_fly_auto_logger);
        getFragmentManager().beginTransaction().add(R.id.fragment_container, new Overview()).commit();
    }
}
