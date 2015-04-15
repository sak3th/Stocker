package com.bavya.stocker.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

public class StockProvider extends ContentProvider {
    private static final String TAG = "StockProvider";

    private static final String DATABASE_NAME = "stocker.db";
    private static final int DATABASE_VERSION = 1 << 16;

    private static final int URI_UNKNOWN = 0;
    private static final int URI_STOCKER = 1;
    private static final int URI_ID = 2;

    private static final String AUTHORITY = StockProvider.class.getPackage().getName();
    private static final String STOCKS_TABLE = "stocks";
    private static final UriMatcher s_uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        s_uriMatcher.addURI(AUTHORITY, "stocks", URI_STOCKER);
        s_uriMatcher.addURI(AUTHORITY, "stocks/#", URI_ID);
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {
        // Context to access resources with
        private Context mContext;
        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, getVersion(context));
            mContext = context;
        }
        private static int getVersion(Context context) {
            // FIXME read from file
            return 1;
        }
        @Override
        public void onCreate(SQLiteDatabase db) {
            createStocksTable(db);
        }
        @Override
        public void onOpen(SQLiteDatabase db) {
            try {
                db.query(STOCKS_TABLE, null, null, null, null, null, null);
            } catch (SQLiteException e) {
                Log.e(TAG, "Exception " + STOCKS_TABLE + " e=" + e);
                if (e.getMessage().startsWith("no such table")) {
                    createStocksTable(db);
                }
            }
        }
        private void createStocksTable(SQLiteDatabase db) {
            // Set up the database schema
            db.execSQL("CREATE TABLE " + STOCKS_TABLE +
                    "(_id INTEGER PRIMARY KEY," +
                    "name TEXT," +
                    "symbol TEXT," +
                    "currency TEXT," +
                    "price TEXT,"+
                    "change INTEGER);");
        }
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }
    }

    private DatabaseHelper mOpenHelper;

    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int count = 0;
        checkPermission();
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int match = s_uriMatcher.match(uri);
        switch (match) {
            case URI_STOCKER:
                count = db.delete(STOCKS_TABLE, selection, selectionArgs);
                break;
            case URI_ID:
                count = db.delete(STOCKS_TABLE, Stocker.Stocks._ID + "=?",
                        new String[] { uri.getLastPathSegment() });
                break;
            default:
                throw new UnsupportedOperationException("Cannot delete that Uri: " + uri);
        }
        if (count > 0) {
            Log.d(TAG, "deleted " + count + " entries");
            getContext().getContentResolver().notifyChange(Stocker.Stocks.CONTENT_URI, null);
        }
        return count;
    }

    @Override
    public String getType(Uri uri) {
        switch (s_uriMatcher.match(uri)) {
            case URI_STOCKER:
                return "vnd.android.cursor.dir/stocker-stock";
            case URI_ID:
                return "vnd.android.cursor.item/stocker-stock";
            default:
                throw new IllegalArgumentException("Unknown uri " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Uri result = null;
        checkPermission();
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int match = s_uriMatcher.match(uri);
        boolean notify = false;
        if (match == URI_STOCKER) {
            if (values != null) {
                values = new ContentValues(values);
                long rowID = db.insert(STOCKS_TABLE, null, values);
                if (rowID > 0) {
                    result = ContentUris.withAppendedId(Stocker.Stocks.CONTENT_URI, rowID);
                    notify = true;
                }
                if (notify) {
                    getContext().getContentResolver().notifyChange(Stocker.Stocks.CONTENT_URI, null);
                }
                Log.d(TAG, "inserted " + values.toString() + " rowID = " + rowID);
            }
        }
        return result;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setStrict(true); // a little protection from injection attacks
        qb.setTables(STOCKS_TABLE);
        int match = s_uriMatcher.match(uri);
        switch (match) {
            case URI_STOCKER:
                break;
            case URI_ID:
                qb.appendWhere("_id = " + uri.getPathSegments().get(1));
                break;
            default:
                return null;
        }
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor ret = null;
        try {
            ret = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
        } catch (SQLException e) {
            Log.e(TAG, "got exception when querying: " + e);
        }
        if (ret != null) {
            ret.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return ret;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int count = 0;
        checkPermission();
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int match = s_uriMatcher.match(uri);
        switch (match) {
            case URI_STOCKER:
                count = db.update(STOCKS_TABLE, values, selection, selectionArgs);
                break;
            case URI_ID:
                if (selection != null || selectionArgs != null) {
                    throw new UnsupportedOperationException(
                            "Cannot update uri " + uri + " with a where clause");
                }
                count = db.update(STOCKS_TABLE, values, Stocker.Stocks._ID + "=?",
                        new String[] { uri.getLastPathSegment() });
                break;
            default:
                throw new UnsupportedOperationException("Cannot update that uri: " + uri);
        }
    
        if (count > 0) {
            Log.d(TAG, "updated " + values.toString());
            getContext().getContentResolver().notifyChange(Stocker.Stocks.CONTENT_URI, null);
        }
        return count;
    }

    private void checkPermission() {
        int status = getContext().checkCallingOrSelfPermission(
                "android.permission.WRITE_STOCKS");
        status = PackageManager.PERMISSION_GRANTED; //FIXME
        if (status == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        throw new SecurityException("No permission to write to stocker");
    }
}
