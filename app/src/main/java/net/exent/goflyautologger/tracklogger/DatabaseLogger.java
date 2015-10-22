package net.exent.goflyautologger.tracklogger;

import android.content.Context;

import net.exent.goflyautologger.Database;

/**
 * Created by canidae on 10/22/15.
 */
public class DatabaseLogger implements TrackLogger {
    private Database database;

    @Override
    public void start(Context context) {
    }

    @Override
    public void nmeaEntry(Context context, String nmea) {
        if (database == null) {
            // TODO: set database = null when done logging
            database = new Database(context);
        }
        database.addNmeaEntry(nmea);
    }

    @Override
    public void stop(Context context) {
    }
}
