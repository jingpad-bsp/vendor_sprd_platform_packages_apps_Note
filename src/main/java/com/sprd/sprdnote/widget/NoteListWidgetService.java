package com.sprd.sprdnote.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sprd.notejar.view.data.NoteItem;
import com.sprd.sprdnote.NoteEditorActivity;
import com.sprd.sprdnote.R;
import com.sprd.sprdnote.data.NoteDataManager;
import com.sprd.sprdnote.data.NoteDataManagerImpl;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by danny.liu on 2017/4/20.
 */

public class NoteListWidgetService extends RemoteViewsService {

    private static final String TAG = "NoteListWidgetService";
    public NoteListWidgetService() {
        super();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new ListViewRemoteViewsFactory(this, intent);
    }

    //UNISOC: Modify for bug 1235447
    private static class ListViewRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

        private Context mContext;
        private ArrayList<NoteItem> mItems = new ArrayList<>();

        public ListViewRemoteViewsFactory(Context context, Intent intent) {
            mContext = context;
        }

        @Override
        public void onCreate() {
            NoteDataManager mManager = NoteDataManagerImpl.getInstance(mContext);
            mItems = (ArrayList)mManager.getNotesForPresetFolder(2);
        }

        @Override
        public void onDataSetChanged() {
            NoteDataManager mManager = NoteDataManagerImpl.getInstance(mContext);
            mItems = (ArrayList) mManager.getNotesForPresetFolder(2);
            Log.d(TAG, "onDataSetChanged.........size = "+mItems.size());

        }

        @Override
        public void onDestroy() {
            mItems.clear();
        }

        @Override
        public int getCount() {
            return mItems.size();
        }

        @Override
        public RemoteViews getViewAt(int i) {

            if (mItems.size() != 0) {
                NoteItem item = mItems.get(i);
                RemoteViews itemView = new RemoteViews(mContext.getPackageName(), R.layout.widget_listview_item_note);
                itemView.setTextViewText(R.id.note_time, item.isToday()? mContext.getString(R.string.today)+" "+item.getTime(mContext, new Date(item.getLongDate())):item.getDisplayTime(mContext, new Date(item.getLongDate())));
                itemView.setTextViewText(R.id.note_title, item.getTitle());
                if (item.isContainPic()) {
                    itemView.setViewVisibility(R.id.picture,View.VISIBLE);

                    if (item.isCollected()) {
                        itemView.setViewVisibility(R.id.favorite, View.VISIBLE);
                        itemView.setViewVisibility(R.id.favorite2, View.GONE);
                    } else {
                        itemView.setViewVisibility(R.id.favorite, View.GONE);
                        itemView.setViewVisibility(R.id.favorite2, View.GONE);
                    }
                } else if (item.isCollected()) {
                    itemView.setViewVisibility(R.id.favorite2, View.VISIBLE);
                    itemView.setViewVisibility(R.id.picture, View.GONE);
                    itemView.setViewVisibility(R.id.favorite, View.GONE);
                } else {
                    itemView.setViewVisibility(R.id.favorite2, View.GONE);
                    itemView.setViewVisibility(R.id.picture, View.GONE);
                    itemView.setViewVisibility(R.id.favorite, View.GONE);
                }
                Intent fillInIntent = new Intent();
                fillInIntent.putExtra(NoteEditorActivity.ID, item.getId());
                fillInIntent.putExtra(NoteEditorActivity.OPEN_TYPE, NoteEditorActivity.TYPE_EDIT_NOTE);
                fillInIntent.putExtra("from_widget", 1);
                itemView.setOnClickFillInIntent(R.id.listview_linearlayout, fillInIntent);
                return itemView;
            }
            return null;
        }

        @Override
        public RemoteViews getLoadingView() {
            return null;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }
    }
}
