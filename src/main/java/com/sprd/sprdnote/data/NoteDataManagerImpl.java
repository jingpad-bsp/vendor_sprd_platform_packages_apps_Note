package com.sprd.sprdnote.data;

import android.app.NotificationManager;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteFullException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import com.sprd.notejar.view.data.NoteItem;
import com.sprd.notejar.view.data.Item;
import com.sprd.sprdnote.R;
import com.sprd.sprdnote.util.DLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/*
 * Every Activity or Fragment must new NoteDataManager Object instead of using others.
 */
public class NoteDataManagerImpl implements NoteDataManager {

    private static final String TAG = "NoteDataManager";
    private static final String KEY_SQLITE_TAG = "SQLiteFull";
    private static final String CHANNEL_ID = TAG;
    public static final int COLLECTED_FOLDER = 0;
    public static final int PRIVATE_FOLDER = 1;
    public static final int ALL_NOTE_FOLDER = 2;
    public static final int DELETED_NOTE_FOLDER = 3;

    public static final int TYPE_DELETE_FOLDER_ONLY = 4;
    public static final int TYPE_DELETE_FOLDER_AND_NOTES = 5;

    public static final int SUCCESS = 0;
    public static final int FAIL_FOLDER_CONFLICT = -1;
    public static final int FAIL_SQLITE_FULL = -2;
    public static final int FAIL_OPERATE_DB = -3;
    public static final int FAIL_DELETE = -4;
    public static final int FAIL_RESTORE = -5;

    public static final int ID_ALLNOTE_FOLDER = -10;
    public static final int ID_COLLECT_FOLDER = -11;
    public static final int ID_PRIVATE_FOLDER = -12;

    private Context mContext;
    private DataManagerCallback mCallback;
    private List<NoteItem> mAllNotes;
    private List<FolderItem> mAllFolders;
    //Modified for bug 1179175
    private volatile static NoteDataManagerImpl sInstance;

    public interface DataManagerCallback {
        public void onFinishInitialization();
    }

    private class LoadCursorAsyncTask extends AsyncTask<Object, Object, Void> {

        public LoadCursorAsyncTask() {}

        @Override
        protected void onPostExecute(Void result) {
            // Notify finish load cursor
            mCallback.onFinishInitialization();
        }

        @Override
        protected Void doInBackground(Object... arg0) {
            // Load Cursor & Add into list for later query
            loadNoteCursor();
            loadFolderCursor();
            return null;
        }
    }

    //UNISOC: Modify for bug 1235447
    private static class ItemComparator implements Comparator<Item> {
        public ItemComparator() {}

        public int compare(Item o1, Item o2) {
            int result;
            if (o1.getLongDate() < o2.getLongDate()) {
                result = 1;
            } else {
                result = -1;
            }
            return result;
        }
    }

    public static NoteDataManagerImpl getInstance(Context context, DataManagerCallback callback) {
        if (sInstance == null) {
            synchronized (NoteDataManagerImpl.class) {
                if (sInstance == null) {
                    sInstance = new NoteDataManagerImpl(context, callback);
                }
            }
        }
        return sInstance;
    }

    public static NoteDataManagerImpl getInstance(Context context) {
        if (sInstance == null) {
            synchronized (NoteDataManagerImpl.class) {
                if (sInstance == null) {
                    sInstance = new NoteDataManagerImpl(context, null);
                }
            }
        }
        return sInstance;
    }
    /*
     * Instantiate NoteDataManager
     * Every Activity or Fragment must new NoteDataManager Object instead of using others.
     * Because each NoteDataManager has their own mAllNotes & mAllFolders
     */
    private NoteDataManagerImpl(Context context, DataManagerCallback callback) {
        mContext = context;
        mAllNotes = new ArrayList<NoteItem>();
        mAllFolders = new ArrayList<FolderItem>();
        if (callback == null) {
            initDataManager();
        } else {
            initDataManagerAsync(callback);
        }
    }

    /*
     * Load all note and folder cursor, then you can get data faster when use it
     * Use this func, you may wait a long time before finish loading
     */
    private void initDataManager() {
        loadNoteCursor();
        loadFolderCursor();
    }

    /*
     * Load all note and folder cursor, then you can get data faster when use it
     * Use this func, a new thread will be created for loading cursor
     * So, callback is needed, inorder to notify caller finish loading
     */
    public void initDataManagerAsync(DataManagerCallback callback) {
        mCallback = callback;
        LoadCursorAsyncTask task = new LoadCursorAsyncTask();
        task.execute();
    }

    @Override
    public int addNewNote(NoteItem item) {
        ContentValues cv = buildNoteValuesNoID(item);
        Uri uri = mContext.getApplicationContext().getContentResolver().insert(
                    NoteProvider.NOTE_CONTENT_URI, cv);
        if (uri == null) {
            return FAIL_OPERATE_DB;
        }

        DLog.d(TAG, "uri.LastPath=" + uri.getLastPathSegment());
        int id = Integer.valueOf(uri.getLastPathSegment());
        item.setId(id);
        /*Modified for bug 741244 @{*/
        int location = getInsertNoteLocation(item);
        mAllNotes.add(location, item);
        /*}@*/
        return id;
    }

    @Override
    public void deleteNoteIntoDeletedFolder(int id) {
        NoteItem item = getNoteItem(id);
        if (item != null) {
            deleteNoteIntoDeletedFolder(item);
        }
    }

    @Override
    public void deleteNoteIntoDeletedFolder(NoteItem item) {
        if (item.isPrivate()) {
            deleteNoteFromDeletedFolder(item);
            return;
        }
        boolean oldValue = item.isDeleted();
        item.setDeleted(true);
        if (updateNote(item) < 0) {
            item.setDeleted(oldValue);
        }
    }

    @Override
    public void deleteNoteFromDeletedFolder(int id) {
        NoteItem item = getNoteItem(id);
        if (item != null) {
            deleteNoteFromDeletedFolder(item);
        }
    }

    @Override
    public void deleteNoteFromDeletedFolder(NoteItem item) {
        try {
            int count = mContext.getApplicationContext().getContentResolver().delete(NoteProvider.NOTE_CONTENT_URI,
                    DBOpenHelper.ID + "=?", new String[] {item.getId() + ""});
            if (count < 0) {
                DLog.e(TAG, "delete note fail");
                return;
            }
        } catch (SQLException e) {
            DLog.e(TAG, "delete note exception : " + e);
            if (e instanceof SQLiteFullException) {
                showSqliteFullNotification();
            }
            return;
        }
        mAllNotes.remove(item);
    }

    @Override
    public void restoreNoteFromDeletedFolder(int id) {
        NoteItem item = getNoteItem(id);
        if (item != null) {
            restoreNoteFromDeletedFolder(item);
        }
    }

    @Override
    public void restoreNoteFromDeletedFolder(NoteItem item) {
        long oldTime = item.getLongDate();
        boolean oldValue = item.isDeleted();
        item.setDeleted(false);
        item.setLongDate(new Date().getTime());
        if (updateNote(item) < 0) {
            item.setDeleted(oldValue);
            item.setLongDate(oldTime);
        }
    }

    @Override
    public int updateNote(NoteItem item) {
        ContentValues cv = buildNoteValuesNoID(item);
        int count;
        try {
            count = mContext.getApplicationContext().getContentResolver().update(NoteProvider.NOTE_CONTENT_URI,
                    cv, DBOpenHelper.ID + "=?", new String[] {item.getId() + ""});
            if (count < 0) {
                return FAIL_OPERATE_DB;
            }
        } catch (SQLException e) {
            DLog.e(TAG, "update note exception : " + e);
            if (e instanceof SQLiteFullException) {
                showSqliteFullNotification();
                return FAIL_SQLITE_FULL;
            } else {
                return FAIL_OPERATE_DB;
            }
        }
        mAllNotes.remove(item);
        int location = getInsertNoteLocation(item);
        mAllNotes.add(location, item);
        return count;
    }

    @Override
    public NoteItem getNoteItem(int id) {
        for (NoteItem item : mAllNotes) {
            if (item.getId() == id) {
                return item;
            }
        }
        return null;
    }

    @Override
    public List<NoteItem> getNotesForCustomFolder(int folderId) {
        List<NoteItem> result = new ArrayList<NoteItem>();
        for (NoteItem noteItem : mAllNotes) {
            if (noteItem.getParentFolderId() == folderId && !noteItem.isDeleted()) {
                result.add(noteItem);
            }
        }
        return result;
    }

    @Override
    public synchronized List<NoteItem> getNotesForPresetFolder(int type) {
        List<NoteItem> result = new ArrayList<NoteItem>();
        switch (type) {
            case ALL_NOTE_FOLDER:
            {
                for (NoteItem noteItem : mAllNotes) {
                    if (!noteItem.isDeleted() && !noteItem.isPrivate()) {
                        result.add(noteItem);
                    }
                }
                Log.d(TAG, "getNotesForPresetFolder ==" + type+ " content = "
                        + (result.size() == 1? result.get(0).getContent(): null));
                return result;
            }

            case COLLECTED_FOLDER:
            {
                for (NoteItem noteItem : mAllNotes) {
                    if (noteItem.isCollected() && !noteItem.isDeleted()) {
                        result.add(noteItem);
                    }
                }
                return result;
            }

            case PRIVATE_FOLDER:
            {
                for (NoteItem noteItem : mAllNotes) {
                    if (noteItem.isPrivate() && !noteItem.isDeleted()) {
                        result.add(noteItem);
                    }
                }
                return result;
            }

            case DELETED_NOTE_FOLDER:
            {
                for (NoteItem noteItem : mAllNotes) {
                    if (noteItem.isDeleted()) {
                        result.add(noteItem);
                    }
                }
                return result;
            }

            default:
                return null;
        }
    }

    @Override
    public List<NoteItem> getNotesIncludeContent(String content) {
        List<NoteItem> result = new ArrayList<NoteItem>();
        for (NoteItem noteItem : mAllNotes) {
            String sourceContent = noteItem.getContent();
            String sourceTitle = noteItem.getTitle();
            // Not show deleted or private notes
            if (!noteItem.isDeleted() && !noteItem.isPrivate()) {
                if (sourceContent != null &&
                        sourceContent.toLowerCase().contains(content.toLowerCase())) {
                    result.add(noteItem);
                } else if (sourceTitle != null &&
                        sourceTitle.toLowerCase().contains(content.toLowerCase())) {
                    result.add(noteItem);
                }
            }
        }
        return result;
    }

    @Override
    public int addNewFolder(FolderItem item) {
        if (isFolderConflict(item)) {
            return FAIL_FOLDER_CONFLICT;
        }
        ContentValues cv = buildFolderValuesNoID(item);
        Uri uri = mContext.getApplicationContext().getContentResolver().insert(
                NoteProvider.FOLDER_CONTENT_URI, cv);
        if (uri == null) {
            return FAIL_OPERATE_DB;
        }

        DLog.d(TAG, "uri.LastPath=" + uri.getLastPathSegment());
        int id = Integer.valueOf(uri.getLastPathSegment());
        item.setId(id);
        mAllFolders.add(0, item);
        return id;
    }

    @Override
    public void deleteFolderIntoDeletedFolder(int id, int type) {
        FolderItem item = getFolderItem(id);
        if (item != null) {
            deleteFolderIntoDeletedFolder(item, type);
        }
    }

    private int pDeleteNoteIntoDeletedFolder(NoteItem item) {
        boolean oldValue = item.isDeleted();
        item.setDeleted(true);
        if (updateNote(item) < 0) {
            item.setDeleted(oldValue);
            return FAIL_DELETE;
        }
        return SUCCESS;
    }

    @Override
    public void deleteFolderIntoDeletedFolder(FolderItem item, int type) {
        List<NoteItem> noteItems = getNotesForCustomFolder(item.getId());
        for (NoteItem nitem : noteItems) {
            if (type == TYPE_DELETE_FOLDER_AND_NOTES) {
                if (pDeleteNoteIntoDeletedFolder(nitem) < 0) {
                    return;
                }
            } else if (type == TYPE_DELETE_FOLDER_ONLY) {
                int oldParentFolderId = nitem.getParentFolderId();
                nitem.setParentFolderId(NoteItem.DEFAULT_PARENT_FOLDER_ID);
                if (updateNote(nitem) < 0) {
                    nitem.setParentFolderId(oldParentFolderId);
                    return;
                }
            }
        }

        boolean oldValue = item.isDeleted();
        item.setDeleted(true);
        if (updateFolder(item) < 0) {
            item.setDeleted(oldValue);
        }
    }

    @Override
    public void deleteFolderFromDeletedFolder(int id) {
        FolderItem item = getFolderItem(id);
        if (item != null) {
            deleteFolderFromDeletedFolder(item);
        }
    }

    private int pDeleteNoteFromDeletedFolder(NoteItem item) {
        try {
            int count = mContext.getApplicationContext().getContentResolver().delete(NoteProvider.NOTE_CONTENT_URI,
                    DBOpenHelper.ID + "=?", new String[] {item.getId() + ""});
            if (count < 0) {
                DLog.e(TAG, "delete note fail");
                return FAIL_DELETE;
            }
        } catch (SQLException e) {
            DLog.e(TAG, "delete note exception : " + e);
            if (e instanceof SQLiteFullException) {
                showSqliteFullNotification();
            }
            return FAIL_DELETE;
        }
        mAllNotes.remove(item);
        return SUCCESS;
    }

    @Override
    public void deleteFolderFromDeletedFolder(FolderItem item) {
        List<NoteItem> noteItems = getNotesForCustomFolder(item.getId());
        for (NoteItem nitem : noteItems) {
            if (pDeleteNoteFromDeletedFolder(nitem) < 0) {
                return;
            }
        }
        try {
            int count = mContext.getApplicationContext().getContentResolver().delete(NoteProvider.FOLDER_CONTENT_URI,
                    DBOpenHelper.ID + "=?", new String[] {item.getId() + ""});
            if (count < 0) {
                DLog.e(TAG, "delete folder fail");
                return;
            }
        } catch (SQLException e) {
            DLog.e(TAG, "delete folder exception : " + e);
            if (e instanceof SQLiteFullException) {
                showSqliteFullNotification();
            }
            return;
        }
        mAllFolders.remove(item);
    }

    @Override
    public void restoreFolderFromDeletedFolder(int id) {
        FolderItem item = getFolderItem(id);
        if (item != null) {
            restoreFolderFromDeletedFolder(item);
        }
    }

    private int pRestoreNoteFromDeletedFolder(NoteItem item) {
        long oldTime = item.getLongDate();
        boolean oldValue = item.isDeleted();
        item.setDeleted(false);
        item.setLongDate(new Date().getTime());
        if (updateNote(item) < 0) {
            item.setDeleted(oldValue);
            item.setLongDate(oldTime);
            return FAIL_RESTORE;
        }
        return SUCCESS;
    }

    @Override
    public void restoreFolderFromDeletedFolder(FolderItem item) {
        List<NoteItem> noteItems = getNotesForCustomFolder(item.getId());
        for (NoteItem nitem : noteItems) {
            if (pRestoreNoteFromDeletedFolder(nitem) < 0) {
                return;
            }
        }
        long oldTime = item.getLongDate();
        boolean oldValue = item.isDeleted();
        item.setDeleted(false);
        item.setLongDate(new Date().getTime());
        if (updateFolder(item) < 0) {
            item.setDeleted(oldValue);
            item.setLongDate(oldTime);
        }
    }

    @Override
    public int updateFolder(FolderItem item) {
        if (isFolderConflict(item)) {
            return FAIL_FOLDER_CONFLICT;
        }
        ContentValues cv = buildFolderValuesNoID(item);
        int count;
        try {
            count = mContext.getApplicationContext().getContentResolver().update(NoteProvider.FOLDER_CONTENT_URI,
                    cv, DBOpenHelper.ID + "=?", new String[] {item.getId() + ""});
            if (count < 0) {
                DLog.e(TAG, "update folder fail");
                return FAIL_OPERATE_DB;
            }
        } catch (SQLException e) {
            DLog.e(TAG, "update folder exception : " + e);
            if (e instanceof SQLiteFullException) {
                showSqliteFullNotification();
                return FAIL_SQLITE_FULL;
            } else {
                return FAIL_OPERATE_DB;
            }
        }
        mAllFolders.remove(item);
        int location = getInsertFolderLocation(item);
        mAllFolders.add(location, item);
        return count;
    }

    @Override
    public FolderItem getFolderItem(int id) {
        for (FolderItem item : mAllFolders) {
            if (item.getId() == id) {
                return item;
            }
        }
        return null;
    }

    @Override
    public List<FolderItem> getFolderListForNoteMove() {
        List<FolderItem> result = new ArrayList<FolderItem>();
        result.add(buildSpecialFolderItem(
                ID_ALLNOTE_FOLDER, mContext.getResources().getString(R.string.all_notes)));
        result.add(buildSpecialFolderItem(
                ID_COLLECT_FOLDER, mContext.getResources().getString(R.string.my_favorite)));
        result.add(buildSpecialFolderItem(
                ID_PRIVATE_FOLDER, mContext.getResources().getString(R.string.private_note)));
        for (FolderItem item : mAllFolders) {
            if (!item.isDeleted()) {
                result.add(item);
            }
        }
        return result;
    }

    @Override
    public List<FolderItem> getAllValidFolders() {
        List<FolderItem> result = new ArrayList<FolderItem>();
        for (FolderItem item : mAllFolders) {
            if (!item.isDeleted()) {
                result.add(item);
            }
        }
        return result;
    }

    @Override
    public List<FolderItem> getDeletedFolders() {
        List<FolderItem> result = new ArrayList<FolderItem>();
        for (FolderItem item : mAllFolders) {
            if (item.isDeleted()) {
                result.add(item);
            }
        }
        return result;
    }

    private void loadNoteCursor() {
        Cursor cursor = null;
        NoteItem noteItem = null;
        cursor = mContext.getContentResolver().query(
                NoteProvider.NOTE_CONTENT_URI, DBOpenHelper.NOTE_ALL_COLUMS, null, null, null);
        if (cursor == null) {
            return;
        }
        //UNISOC: Modify for bug 1235503
        if (mAllNotes.size() != 0) {
            mAllNotes.clear();
        }
        while (cursor.moveToNext()) {
            noteItem = buildNoteItem(cursor);
            mAllNotes.add(noteItem);
            Log.d(TAG, "noteItem = = = "+noteItem.getContent());
        }

        Collections.sort(mAllNotes, new ItemComparator());
        cursor.close();
    }

    private void loadFolderCursor() {
        Cursor cursor = null;
        FolderItem folderItem = null;
        cursor = mContext.getContentResolver().query(
                NoteProvider.FOLDER_CONTENT_URI, DBOpenHelper.FOLDER_ALL_COLUMS, null, null, null);
        if (cursor == null) {
            return;
        }
        //UNISOC: Modify for bug 1235503
        if (mAllFolders.size() != 0) {
            mAllFolders.clear();
        }

        while (cursor.moveToNext()) {
            folderItem = buildFolderItem(cursor);
            mAllFolders.add(folderItem);
        }

        Collections.sort(mAllFolders, new ItemComparator());
        cursor.close();
    }

    private NoteItem buildNoteItem(Cursor cursor) {
        NoteItem noteItem = new NoteItem();
        noteItem.setId(cursor.getInt(DBOpenHelper.NoteColumnsIndex.ID));
        noteItem.setTitle(cursor.getString(DBOpenHelper.NoteColumnsIndex.NOTE_TITLE));
        noteItem.setContent(cursor.getString(DBOpenHelper.NoteColumnsIndex.NOTE_CONTENT));
        noteItem.setLongDate(cursor.getLong(DBOpenHelper.NoteColumnsIndex.NOTE_UPDATE_DATE));
        noteItem.setCollected(
                cursor.getInt(DBOpenHelper.NoteColumnsIndex.NOTE_IS_COLLECTED) == 1 ? true : false);
        noteItem.setHasPictures(
                cursor.getInt(DBOpenHelper.NoteColumnsIndex.NOTE_IS_CONTAIN_PIC) == 1 ? true : false);
        noteItem.setPrivate(
                cursor.getInt(DBOpenHelper.NoteColumnsIndex.NOTE_IS_PRIVATE) == 1 ? true : false);
        noteItem.setDeleted(
                cursor.getInt(DBOpenHelper.NoteColumnsIndex.NOTE_IS_DELETED) == 1 ? true : false);
        noteItem.setBackgroundId(cursor.getInt(DBOpenHelper.NoteColumnsIndex.NOTE_BACKGROUND_ID));
        noteItem.setParentFolderId(cursor.getInt(DBOpenHelper.NoteColumnsIndex.NOTE_PARENT_FOLDER_ID));

        return noteItem;
    }

    private FolderItem buildFolderItem(Cursor cursor) {
        FolderItem folderItem = new FolderItem();
        folderItem.setId(cursor.getInt(DBOpenHelper.FolderColumnsIndex.ID));
        folderItem.setName(cursor.getString(DBOpenHelper.FolderColumnsIndex.FOLDER_NAME));
        folderItem.setLongDate(cursor.getLong(DBOpenHelper.FolderColumnsIndex.FOLDER_UPDATE_DATE));
        folderItem.setDeleted(
                cursor.getInt(DBOpenHelper.FolderColumnsIndex.FOLDER_IS_DELETED) == 1 ? true : false);

        return folderItem;
    }

    private FolderItem buildSpecialFolderItem(int id, String name) {
        FolderItem folderItem = new FolderItem();
        folderItem.setId(id);
        folderItem.setName(name);
        return folderItem;
    }

    private ContentValues buildNoteValuesNoID(NoteItem item) {
        ContentValues cv = new ContentValues();
        cv.put(DBOpenHelper.NOTE_TITLE, item.getTitle());
        cv.put(DBOpenHelper.NOTE_CONTENT, item.getContent());
        cv.put(DBOpenHelper.NOTE_DATE, item.getLongDate());
        cv.put(DBOpenHelper.NOTE_IS_COLLECTED, item.isCollected());
        cv.put(DBOpenHelper.NOTE_IS_CONTAIN_PIC, item.isContainPic());
        cv.put(DBOpenHelper.NOTE_IS_PRIVATE, item.isPrivate());
        cv.put(DBOpenHelper.NOTE_IS_DELETED, item.isDeleted());
        cv.put(DBOpenHelper.NOTE_BACKGROUND_ID, item.getBackgroundId());
        cv.put(DBOpenHelper.NOTE_PARENT_FOLDER_ID, item.getParentFolderId());

        return cv;
    }

    private ContentValues buildFolderValuesNoID(FolderItem item) {
        ContentValues cv = new ContentValues();
        cv.put(DBOpenHelper.FOLDER_NAME, item.getName());
        cv.put(DBOpenHelper.FOLDER_DATE, item.getLongDate());
        cv.put(DBOpenHelper.FOLDER_IS_DELETED, item.isDeleted());

        return cv;
    }

    private void showSqliteFullNotification() {
        NotificationManager mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(mContext);
        Resources mResources = mContext.getResources();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String nameChannel = mResources.getString(R.string.app_name);
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, nameChannel, NotificationManager.IMPORTANCE_LOW);
            mNotificationManager.createNotificationChannel(channel);
            mBuilder.setChannelId(CHANNEL_ID);
        }
        String title = mResources.getString(R.string.sqlite_full);
        String mTag = KEY_SQLITE_TAG;
        DLog.d(TAG, "showSqliteFullNotification");
        mNotificationManager.notify(
                mTag,
                0,
                mBuilder.setAutoCancel(true)
                        .setContentTitle(title)
                        .setContentText(title)
                        .setTicker(title)
                        .setOngoing(false)
                        .setProgress(0, 0, false)
                        .setSmallIcon(android.R.drawable.stat_sys_warning)
                        .setContentIntent(
                                PendingIntent.getActivity(mContext, mTag.hashCode(), new Intent(),
                                        PendingIntent.FLAG_CANCEL_CURRENT)).build());
    }

    private int getInsertNoteLocation(NoteItem item) {
        for (int location = 0; location < mAllNotes.size(); location++) {
            NoteItem sourceItem = mAllNotes.get(location);
            if (item.getLongDate() >= sourceItem.getLongDate()) {
                return location;
            }
        }
        return mAllNotes.size();
    }

    private int getInsertFolderLocation(FolderItem item) {
        for (int location = 0; location < mAllFolders.size(); location++) {
            FolderItem sourceItem = mAllFolders.get(location);
            if (item.getLongDate() >= sourceItem.getLongDate()) {
                return location;
            }
        }
        return mAllFolders.size();
    }

    private boolean isFolderConflict(FolderItem item) {
        for (FolderItem sourceItem : mAllFolders) {
            if (sourceItem.getName().equals(item.getName()) &&
                    !sourceItem.isDeleted() &&
                    sourceItem.getId() != item.getId()) {
                return true;
            }
        }
        return false;
    }
}
