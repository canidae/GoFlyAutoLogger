package net.exent.goflyautologger;

import android.app.Application;
import android.test.ApplicationTestCase;

import net.exent.goflyautologger.tracklogger.LiveTrack24Logger;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {
    public ApplicationTest() {
        super(Application.class);
        LiveTrack24Logger logger = new LiveTrack24Logger();
        logger.start(null);
        logger.nmeaEntry(null, "$GPRMC,123519,A,4859.038,N,01131.000,E,022.4,084.4,230394,003.1,W*6A");
        logger.nmeaEntry(null, "$GPGGA,123519,4807.038,N,01131.000,E,1,08,0.9,545.4,M,46.9,M,,*47");
        logger.nmeaEntry(null, "$GPRMC,220516,A,5133.82,S,00042.24,W,173.8,231.8,130694,004.2,W*70");
        logger.nmeaEntry(null, "$GPRMC,220516,,,,,,,,130694,,W*70");
    }
}