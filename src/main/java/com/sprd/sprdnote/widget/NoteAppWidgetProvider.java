package com.sprd.sprdnote.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.util.Log;

import com.sprd.sprdnote.RequestPermissionsActivity;
import com.sprd.sprdnote.NoteActivity;
import com.sprd.sprdnote.NoteEditorActivity;
import com.sprd.sprdnote.R;

/**
 * Created by danny.liu on 2017/4/19.
 */

public class NoteAppWidgetProvider extends AppWidgetProvider {
    private static final String TAG="NoteAppWidgetProvider";
    public static final String UPDATE_LIST_ACTION = "com.sprd.note.widget.UPDATE_NOTE";
    public static final String UPDATE_PERMISSION_ACTION = "com.sprd.note.widget.UPDATE_PERMISSION_WIDGET";
    @Override
    public void onReceive(Context context, Intent intent) {
       Log.d(TAG, "intent action =  "+intent.getAction());
       if (UPDATE_LIST_ACTION.equals(intent.getAction())) {
           final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
           final int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context,
                   this.getClass()));
           if (appWidgetIds.length > 0) {
               // We need to update all Bugle app widgets on the home screen.
               appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, getListId());
           }
       } else if (UPDATE_PERMISSION_ACTION.equals(intent.getAction())) {
           final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
           final int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context,
                  this.getClass()));
           if (appWidgetIds.length > 0) {
              onUpdate(context, appWidgetManager, appWidgetIds);
           }
       } else {
           super.onReceive(context, intent);
       }

    }

    public static void updateWidget(Context context) {
        context.sendBroadcast(new Intent(UPDATE_LIST_ACTION, null, context, NoteAppWidgetProvider.class));
    }

    public static void updatePermissionWidget(Context context) {
        context.sendBroadcast(new Intent(UPDATE_PERMISSION_ACTION, null, context, NoteAppWidgetProvider.class));
    }

    protected int getListId() {
        return R.id.note_list;
    }
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            int appWidgetId = appWidgetIds[i];

            // Create an Intent to launch NoteActivity
            Intent intent = new Intent(context, NoteActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
            //Create an Intent to launch NoteEditorActivity for add new note.
            Intent composeIntent = new Intent(context, NoteEditorActivity.class);
            composeIntent.putExtra("open_type", 1);
            composeIntent.putExtra("FOLDER_ID", 0);
            composeIntent.putExtra("FILTER_TYPE", 0);
            composeIntent.putExtra("from_widget", 1);
            PendingIntent composeNoteIntent = PendingIntent.getActivity(context, 0, composeIntent, 0);

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_note_list);
            views.setOnClickPendingIntent(R.id.widget_header, pendingIntent);
            views.setOnClickPendingIntent(R.id.widget_compose, composeNoteIntent);

            //Display All notes in listview.
            Intent serviceIntent = new Intent(context, NoteListWidgetService.class);
            serviceIntent.putExtra(appWidgetManager.EXTRA_APPWIDGET_ID,
                    appWidgetIds[i]);
            views.setRemoteAdapter(R.id.note_list, serviceIntent);

          //  PendingIntent pendingIntentTemplate = PendingIntent.getActivity(context, 1, intent2, PendingIntent.FLAG_UPDATE_CURRENT);
            Intent itemIntent = new Intent(context, NoteEditorActivity.class);

            PendingIntent listPendingIntent = PendingIntent.getActivity(context, 1, itemIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setPendingIntentTemplate(R.id.note_list, listPendingIntent);
            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds[i], R.id.note_list);
        }
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
    }

    @Override
    public void onRestored(Context context, int[] oldWidgetIds, int[] newWidgetIds) {
        super.onRestored(context, oldWidgetIds, newWidgetIds);
    }
}