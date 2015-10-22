package net.exent.goflyautologger.tracklogger;

import android.content.Context;
import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;

/**
 * Created by canidae on 10/22/15.
 */
public class LiveTrack24Logger implements TrackLogger {
    // http://[server].livetrack24.com/api/[dataFormat]/lt/[OpCode]/[AppKey]
    // /[AppVersion]/[SessionID]/[UserID]/[PasswordToken]/[OpCodeParameters]
    // /[OptionalParameters]
    private static final String LIVETRACK24_SERVER = "http://t0.livetrack24.com/api/"; // t0 = testing, t2 = production
    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("HHmmss ddMMyy", Locale.US);

    private SortedMap<Long, TrackPoint> trackPoints = new TreeMap<>();
    private TrackPoint nextTrackPoint = new TrackPoint();

    static {
        SIMPLE_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Override
    public void start(Context context) {
        /* TODO: much
        try {
            String appKey = "WhudO"; // TODO: get app key from livetrack24-people
            String deviceId = "unique_device_id"; // TODO: create unique id and store in SharedPreferences
            String passwordToken = getMd5Hash("password".toLowerCase());
            String username = "testUser";
            String url = LIVETRACK24_SERVER + "t/lt/getUserID/" + appKey + "/" + BuildConfig.VERSION_NAME + "/" + deviceId + "/0/0/" + passwordToken + "/" + username;
            BufferedInputStream bis = new BufferedInputStream(new URL(url).openStream());
            BufferedReader br = new BufferedReader(new InputStreamReader(bis));
            String line;
            while ((line = br.readLine()) != null)
                System.out.println(line);
        } catch (MalformedURLException e) {
            Log.w(getClass().getName(), "Malformed URL", e);
        } catch (IOException e) {
            Log.w(getClass().getName(), "Unable to connect", e);
        }
        */
    }

    @Override
    public void nmeaEntry(Context context, String nmea) {
        if (nmea.startsWith("$GPRMC,") && parseGprmc(nmea)) {
            trackPoints.put(nextTrackPoint.secondsSinceEpoch, nextTrackPoint);
            Log.d(getClass().getName(), nextTrackPoint.toString());
            int altitude = nextTrackPoint.altitude;
            nextTrackPoint = new TrackPoint();
            nextTrackPoint.altitude = altitude; // keeping altitude just in case we lose some GPGGA entries
            // TODO: write to livetrack24.com every now and then
        } else if (nmea.startsWith("$GPGGA")) {
            // only retrieving altitude from GPGGA, don't add trackpoint to map just yet
            parseGpgga(nmea);
        }
    }

    @Override
    public void stop(Context context) {
    }

    private boolean parseGprmc(String gprmc) {
        /*
            1  = UTC of position fix
            2  = Data status (V=navigation receiver warning)
            3  = Latitude of fix
            4  = N or S
            5  = Longitude of fix
            6  = E or W
            7  = Speed over ground in knots
            8  = Track made good in degrees True
            9  = UT date
            10 = Magnetic variation degrees (Easterly var. subtracts from true course)
            11 = E or W
            12 = Checksum
        */
        String[] parts = gprmc.split("[,\\*]");
        if (parts.length < 13) {
            Log.i(getClass().getName(), "Unable to parse GPRMC entry: " + gprmc);
            return false;
        }
        String dateAndTime = parts[1] + " " + parts[9];
        try {
            nextTrackPoint.secondsSinceEpoch = SIMPLE_DATE_FORMAT.parse(dateAndTime).getTime() / 1000;
        } catch (ParseException e) {
            Log.w(getClass().getName(), "Unable to parse date/time: " + dateAndTime, e);
            return false;
        }
        try {
            double nmeaLatitude = Double.parseDouble(parts[3]);
            nextTrackPoint.latitude = ((int) nmeaLatitude / 100) + (float) (nmeaLatitude % 100.0 / 60);
            if ("S".equalsIgnoreCase(parts[4]))
                nextTrackPoint.latitude = 0 - nextTrackPoint.latitude;
        } catch (NumberFormatException e) {
            Log.w(getClass().getName(), "Unable to parse latitude: " + parts[3], e);
            return false;
        }
        try {
            double nmeaLongitude = Double.parseDouble(parts[5]);
            nextTrackPoint.longitude = ((int) nmeaLongitude / 100) + (float) (nmeaLongitude % 100.0 / 60);
            if ("W".equalsIgnoreCase(parts[6]))
                nextTrackPoint.longitude = 0 - nextTrackPoint.longitude;
        } catch (NumberFormatException e) {
            Log.w(getClass().getName(), "Unable to parse longitude: " + parts[5], e);
            return false;
        }
        try {
            nextTrackPoint.velocity = (int) Math.round(Double.parseDouble(parts[7]) * 1.852);
        } catch (NumberFormatException e) {
            Log.w(getClass().getName(), "Unable to parse velocity: " + parts[7], e);
            // LiveTrack24 accepts 0 as NODATA
        }
        // $GPRMC,123519,A,4859.038,N,01131.000,E,022.4,084.4,230394,003.1,W*6A
        try {
            nextTrackPoint.heading = (int) Math.round(Double.parseDouble(parts[8]));
        } catch (NumberFormatException e) {
            Log.w(getClass().getName(), "Unable to parse heading: " + parts[8], e);
            // LiveTrack24 accepts 0 as NODATA
        }
        return true;
    }

    private boolean parseGpgga(String gpgga) {
        /*
            1  = UTC of Position
            2  = Latitude
            3  = N or S
            4  = Longitude
            5  = E or W
            6  = GPS quality indicator (0=invalid; 1=GPS fix; 2=Diff. GPS fix)
            7  = Number of satellites in use [not those in view]
            8  = Horizontal dilution of position
            9  = Antenna altitude above/below mean sea level (geoid)
            10 = Meters  (Antenna height unit)
            11 = Geoidal separation (Diff. between WGS-84 earth ellipsoid and
                 mean sea level.  -=geoid is below WGS-84 ellipsoid)
            12 = Meters  (Units of geoidal separation)
            13 = Age in seconds since last update from diff. reference station
            14 = Diff. reference station ID#
            15 = Checksum
        */
        String[] parts = gpgga.split("[,\\*]");
        if (parts.length < 13) {
            Log.i(getClass().getName(), "Unable to parse GPGGA entry: " + gpgga);
            return false;
        }
        // only using altitude from GPGGA
        try {
            nextTrackPoint.altitude = (int) Math.round(Double.parseDouble(parts[11]));
        } catch (NumberFormatException e) {
            Log.w(getClass().getName(), "Unable to parse altitude: " + parts[11], e);
            return false;
        }
        return true;
    }

    private String getMd5Hash(String md5) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] array = md.digest(md5.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte anArray : array)
                sb.append(Integer.toHexString((anArray & 0xFF) | 0x100).substring(1, 3));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.w(getClass().getName(), "MD5 hash algorithm doesn't exist", e);
        }
        return null;
    }

    private class TrackPoint {
        long secondsSinceEpoch;
        float latitude;
        float longitude;
        int altitude;
        int velocity;
        int heading;

        public String toString() {
            return "" + secondsSinceEpoch + "," + latitude + "," + longitude + "," + altitude + "," + velocity + "," + heading;
        }
    }
}
