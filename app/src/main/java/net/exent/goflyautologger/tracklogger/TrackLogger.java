package net.exent.goflyautologger.tracklogger;

import android.content.Context;

/**
 * Created by canidae on 10/22/15.
 */
public interface TrackLogger {
    void start(Context context);
    void nmeaEntry(Context context, String nmea);
    void stop(Context context);
}
