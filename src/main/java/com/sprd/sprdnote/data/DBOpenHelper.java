package com.sprd.sprdnote.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.sprd.sprdnote.util.DLog;

public class DBOpenHelper extends SQLiteOpenHelper {
    private static final String TAG = "DBOpenHelper";

    private static final int DB_VERSION = 1;
    private static final String DB_NAME = "Notes.db";
    public static final String NOTE_TABLE_NAME = "notes";
    public static final String FOLDER_TABLE_NAME = "folders";

    public static final String ID = "_id";
    public static final String NOTE_TITLE = "title";
    public static final String NOTE_CONTENT = "content";
    public static final String NOTE_DATE = "longdate";
    public static final String NOTE_IS_COLLECTED = "iscollect";
    public static final String NOTE_IS_CONTAIN_PIC = "iscontainpic";
    public static final String NOTE_IS_PRIVATE = "isprivate";
    public static final String NOTE_IS_DELETED = "isdeleted";
    public static final String NOTE_BACKGROUND_ID = "backgroundid";
    public static final String NOTE_PARENT_FOLDER_ID = "parentfolderid";

    public static final String FOLDER_NAME = "name";
    public static final String FOLDER_DATE = "longdate";
    public static final String FOLDER_IS_DELETED = "isdeleted";

    public static final String[] NOTE_ALL_COLUMS = new String[] {
            ID, NOTE_TITLE, NOTE_CONTENT, NOTE_DATE,
            NOTE_IS_COLLECTED, NOTE_IS_CONTAIN_PIC, NOTE_IS_PRIVATE, NOTE_IS_DELETED,
            NOTE_BACKGROUND_ID, NOTE_PARENT_FOLDER_ID
    };

    public static final String[] FOLDER_ALL_COLUMS = new String[] {
            ID, FOLDER_NAME, FOLDER_DATE, FOLDER_IS_DELETED
    };

    public static interface NoteColumnsIndex {
        public int ID = 0;
        public int NOTE_TITLE = 1;
        public int NOTE_CONTENT = 2;
        public int NOTE_UPDATE_DATE = 3;
        public int NOTE_IS_COLLECTED = 4;
        public int NOTE_IS_CONTAIN_PIC = 5;
        public int NOTE_IS_PRIVATE = 6;
        public int NOTE_IS_DELETED = 7;
        public int NOTE_BACKGROUND_ID = 8;
        public int NOTE_PARENT_FOLDER_ID = 9;
    }

    public static interface FolderColumnsIndex {
        public int ID = 0;
        public int FOLDER_NAME = 1;
        public int FOLDER_UPDATE_DATE = 2;
        public int FOLDER_IS_DELETED = 3;
    }

    private static DBOpenHelper helper = null;

    public static synchronized DBOpenHelper getInstance(Context context){
        if (helper == null) {
            helper = new DBOpenHelper(context);
        }
        return helper;
    }

    private DBOpenHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(" CREATE TABLE IF NOT EXISTS " + NOTE_TABLE_NAME + " ( "
                + ID + " integer primary key autoincrement , "
                + NOTE_TITLE + " text , "
                + NOTE_CONTENT + " text , "
                + NOTE_DATE + " long , "
                + NOTE_IS_COLLECTED + " int, "
                + NOTE_IS_CONTAIN_PIC + " int, "
                + NOTE_IS_PRIVATE + " int, "
                + NOTE_IS_DELETED + " int, "
                + NOTE_BACKGROUND_ID + " int, "
                + NOTE_PARENT_FOLDER_ID + " int);");
        DLog.v(TAG, "Create Table: " + NOTE_TABLE_NAME);

        db.execSQL(" CREATE TABLE IF NOT EXISTS " + FOLDER_TABLE_NAME + " ( "
                + ID + " integer primary key autoincrement , "
                + FOLDER_NAME + " text , "
                + FOLDER_DATE + " long , "
                + FOLDER_IS_DELETED + " int);");
        DLog.v(TAG, "Create Table: " + FOLDER_TABLE_NAME);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(" DROP TABLE IF EXISTS " + NOTE_TABLE_NAME);
        db.execSQL(" DROP TABLE IF EXISTS " + FOLDER_TABLE_NAME);
        onCreate(db);
    }
}