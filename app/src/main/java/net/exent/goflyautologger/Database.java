package net.exent.goflyautologger;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by canidae on 10/22/15.
 */
public class Database extends SQLiteOpenHelper {
    public Database(Context context) {
        super(context, "goflyautologger", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createDatabaseV1(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // currently only one version of the database
    }

    public void addNmeaEntry(String nmea) {
        // TODO: we're doing multiple inserts per second, we probably need to batch this
        SQLiteDatabase db = getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("entry", nmea);
        db.insert("nmea", null, contentValues);
        db.close();
    }

    private void createDatabaseV1(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE nmea(timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, entry TEXT NOT NULL)");
    }
}
