package com.sprd.sprdnote.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.sprd.sprdnote.util.DLog;

public class NoteProvider extends ContentProvider {
    private static final String TAG = "NoteProvider";
    DBOpenHelper mHelper;
    SQLiteDatabase db;

    public static final String AUTHORITY = "com.sprd.sprdnote.data.NoteProvider";
    public static final Uri NOTES_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/note");
    public static final Uri NOTE_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/note/#");
    public static final Uri FOLDERS_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/folder");
    public static final Uri FOLDER_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/folder/#");

    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    private static final int NOTE = 1;
    private static final int NOTES = 2;
    private static final int FOLDER = 3;
    private static final int FOLDERS = 4;

    static {
        sURIMatcher.addURI(AUTHORITY, "note", NOTES);
        sURIMatcher.addURI(AUTHORITY, "note/#", NOTE);
        sURIMatcher.addURI(AUTHORITY, "folder", FOLDERS);
        sURIMatcher.addURI(AUTHORITY, "folder/#", FOLDER);
    }

    @Override
    public String getType(Uri uri) {
        switch (sURIMatcher.match(uri))
        {
            case NOTES:
                return "vnd.android.cursor.dir/note";
            case NOTE:
                return "vnd.android.cursor.item/note";
            case FOLDERS:
                return "vnd.android.cursor.dir/folder";
            case FOLDER:
                return "vnd.android.cursor.item/folder";
            default:
                return null;
        }
    }

    @Override
    public boolean onCreate() {
        DLog.d(TAG, "onCreate");
        mHelper = DBOpenHelper.getInstance(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        switch (sURIMatcher.match(uri)) {
            case NOTE:
            case NOTES:
            {
                sortOrder = DBOpenHelper.NOTE_DATE + " desc";
                return queryInTable(DBOpenHelper.NOTE_TABLE_NAME, uri, projection, selection, selectionArgs, sortOrder);
            }

            case FOLDER:
            case FOLDERS:
            {
                sortOrder = DBOpenHelper.FOLDER_DATE + " desc";
                return queryInTable(DBOpenHelper.FOLDER_TABLE_NAME, uri, projection, selection, selectionArgs, sortOrder);
            }

            default:
                throw new IllegalArgumentException("Unknown Uri: " + uri);
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        try {
            db = mHelper.getWritableDatabase();
        } catch (Exception e) {
            DLog.w(TAG, "update" + e.toString());
            return -1;
        }

        switch (sURIMatcher.match(uri)) {
            case NOTE:
            case NOTES: {
                long rowId = db.update(DBOpenHelper.NOTE_TABLE_NAME, values, selection, selectionArgs);
                getContext().getContentResolver().notifyChange(uri, null);
                return (int) rowId;
            }

            case FOLDER:
            case FOLDERS: {
                long rowId = db.update(DBOpenHelper.FOLDER_TABLE_NAME, values, selection, selectionArgs);
                getContext().getContentResolver().notifyChange(uri, null);
                return (int) rowId;
            }

            default:
                throw new IllegalArgumentException("Unknown Uri: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        try {
            db = mHelper.getWritableDatabase();
        } catch (Exception e) {
            DLog.w(TAG, "insert" + e.toString());
            return null;
        }

        switch (sURIMatcher.match(uri)) {
            case NOTE:
            case NOTES:
            {
                long rowId = db.insert(DBOpenHelper.NOTE_TABLE_NAME, null, values);
                if (rowId < 0) {
                    return null;
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return ContentUris.withAppendedId(uri, rowId);
            }

            case FOLDER:
            case FOLDERS:
            {
                long rowId = db.insert(DBOpenHelper.FOLDER_TABLE_NAME, null, values);
                if (rowId < 0) {
                    return null;
                }
                getContext().getContentResolver().notifyChange(uri, null);
                return ContentUris.withAppendedId(uri, rowId);
            }

            default:
                throw new IllegalArgumentException("Unknown Uri: " + uri);
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        try {
            db = mHelper.getWritableDatabase();
        } catch (Exception e) {
            DLog.w(TAG, "delete" + e.toString());
            return -1;
        }

        switch (sURIMatcher.match(uri)) {
            case NOTE:
            case NOTES:
            {
                long rowId = db.delete(DBOpenHelper.NOTE_TABLE_NAME, selection, selectionArgs);
                getContext().getContentResolver().notifyChange(uri, null);
                return (int) rowId;
            }

            case FOLDER:
            case FOLDERS:
            {
                long rowId = db.delete(DBOpenHelper.FOLDER_TABLE_NAME, selection, selectionArgs);
                getContext().getContentResolver().notifyChange(uri, null);
                return (int) rowId;
            }

            default:
                throw new IllegalArgumentException("Unknown Uri: " + uri);
        }
    }

    private Cursor queryInTable(String tableName, Uri uri, String[] projection, String selection,
                               String[] selectionArgs, String sortOrder) {
        Cursor c = null;
        try {
            c = mHelper.getReadableDatabase().query(tableName, null, selection, selectionArgs, null, null, sortOrder);
        } catch (Exception e) {
            DLog.w(TAG, "queryInTable: " + e.toString());
        }

        return c;
    }
}