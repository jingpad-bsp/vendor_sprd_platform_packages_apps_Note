package com.sprd.sprdnote;

import android.app.Application;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Log;
import com.sprd.sprdnote.data.NoteDataManagerImpl;
import com.sprd.notejar.view.data.NoteItem;
/*import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;*/

import java.util.ArrayList;

/**
 * Created by danny.liu on 2017/4/24.
 */

public class NoteApplication extends Application {
    private static final String TAG = "NoteApplication";
   // private RefWatcher mRefWatcher;

    @Override
    public void onCreate() {
        super.onCreate();
      // mRefWatcher = LeakCanary.install(this);
       new CheckDeletedTimer().execute();
    }

    class CheckDeletedTimer extends AsyncTask {

        NoteDataManagerImpl mNoteData = NoteDataManagerImpl.getInstance(NoteApplication.this);
        ArrayList <NoteItem> list = new ArrayList<>();
        long limit = 30*24*60*60*1000L;
        @Override
        protected Object doInBackground(Object[] objects) {
            if (mNoteData != null) {
                long time = System.currentTimeMillis();
                list = (ArrayList<NoteItem>) mNoteData.getNotesForPresetFolder(NoteDataManagerImpl.DELETED_NOTE_FOLDER);
                if (list.size() != 0) {

                    for (int i = 0; i < list.size(); i++) {
                        NoteItem temp = list.get(i);
                        Log.i(TAG, "delete..............overTimetime = .."+(time - temp.getLongDate()));
                        if (time - temp.getLongDate() > limit) {
                            mNoteData.deleteNoteFromDeletedFolder(temp);
                        }
                    }
                }
            }
            return null;
        }
    }
}