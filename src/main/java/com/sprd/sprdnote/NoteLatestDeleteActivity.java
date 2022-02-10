package com.sprd.sprdnote;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.sprd.sprdnote.data.FolderItem;
import com.sprd.sprdnote.data.NoteDataManagerImpl;
import com.sprd.notejar.view.data.NoteItem;
import com.sprd.sprdnote.folder.FolderUtil;
import com.sprd.sprdnote.util.DLog;
import com.sprd.notejar.view.util.NoteCategory;
import com.sprd.notejar.view.util.NoteUtils;
import com.sprd.notejar.view.ListItemView;
import com.sprd.sprdnote.widget.NoteAppWidgetProvider;

import java.util.ArrayList;
import java.util.List;

public class NoteLatestDeleteActivity extends AppCompatActivity implements OnItemClickListener, AdapterView.OnItemLongClickListener {

    private static final String TAG = "NoteLatestDelete";
    private static final int TASK_TYPE_DELETE = 0;
    private static final int TASK_TYPE_MOVE = 1;
    private static final int TASK_TYPE_RECOVERY = 2;

    public static final int DIALOG_DELTE_SOME_NOTES = 4;
    public static final int DIALOG_DELTE_PROGRESS = 88;

    public static final int MSG_PROGRESS_DIALOG_SHOW = 1;
    public static final int MSG_PROGRESS_DIALOG_DISMISS = 2;
    public static final int MSG_TOAST_SUCCESSED = 3;
    private static final int DIALOG_KEY_DELETE = 4;
    private static final int DIALOG_MOVE_FOLDER = 5;
    private static final int DIALOG_MOVE_PROGRESS = 99;
    private static final String DIALOG_KEY_MOVE_TO = "moveToFolder";
    private static final int LOADER_NOTES = 1;

    private ListView mListView;
    private View emptyView;
    private Toolbar toolbar;
    private FloatingActionButton fab;
    private MenuItem mDelete;
    private MenuItem mSelectAll;
    private MenuItem mRecovery;
    private TextView mRecycle;

    private Context mContext;
    private PowerManager.WakeLock mWakeLock;
    private NoteDataManagerImpl mNoteDataManager;
    private List<NoteItem> mItem = null;
    private ArrayList<NoteCategory> mCategories = null;
    private NoteAdapter mAdapter = null;
    private ArrayList<FolderItem> folders;
    //UNISOC: Add for bug 1240753
    private AlertDialogFragment mDialogFragment;
    private String title;
    private int FILTER_TYPE = 0;
    private int ID = 0;
    //How many NoteItem were selected.
    private int selectedCount = 0;
    private int id;
    private int showType = NoteAdapter.TYPE_SHOW_NORMAL;
    private boolean isDeleting = false;
    private boolean isMoving = false;
    private boolean isRecovering = false;
    private boolean isSearchMode = false;
    private static String folderName;

    /*Handler mHander = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            refreshUi();
        }
    };*/
    NoteDataManagerImpl.DataManagerCallback mCallback = new NoteDataManagerImpl.DataManagerCallback() {
        @Override
        public void onFinishInitialization() {
            loadNotes(FILTER_TYPE, ID);
        }
    };

   /* LoaderManager.LoaderCallbacks<Cursor> mNoteLoaderListener = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader onCreateLoader(int i, Bundle bundle) {
            return new CursorLoader(NoteActivity.this, NoteProvider.NOTE_CONTENT_URI, null, null,
                    null, null);
        }

        @Override
        public void onLoadFinished(Loader loader, Cursor data) {
            if (data != null && data.isClosed()) {
                data = null;
            }
            Log.d(TAG, "onLoadFinishedonLoadFinishedonLoadFinishedonLoadFinished");

        }

        @Override
        public void onLoaderReset(Loader loader) {

        }
    };*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_latest_delete);
        Intent intent = getIntent();
        /*Modified for 712255: start*/
        title = getString(R.string.lastest_delete);
        FILTER_TYPE = intent.getIntExtra("FILTER", 0);
        id = intent.getIntExtra("ID", 0);
        Log.d(TAG, "onNewIntent FILTER_TYPE = "+FILTER_TYPE+" : id = "+id);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(title);
        /*Modified for 712255: end*/
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_ab_folder_holo_light);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent FolderIntent = new Intent(NoteLatestDeleteActivity.this, NoteFolderListActivity.class);
                startActivity(FolderIntent);
               // startActivity(new Intent(NoteActivity.this, TestActivity.class));
            }
        });
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setVisibility(View.GONE);
        if(RequestPermissionsActivity.startPermissionActivity(this)){
            return;
        }
        mNoteDataManager = NoteDataManagerImpl.getInstance(this.getApplicationContext(), mCallback);
        initViews();
        mContext = this;
    }

    private void initViews() {
        mListView = (ListView) this.findViewById(R.id.page_list);
        mListView.setOnItemClickListener(this);
        mListView.setOnItemLongClickListener(this);
        mListView.setOnCreateContextMenuListener(this);
        emptyView = findViewById(R.id.note_empty_view);
        mRecycle = (TextView)findViewById(R.id.recycle_txt);
        mRecycle.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long l) {
        if (showType == NoteAdapter.TYPE_SHOW_CHECK || isSearchMode) {
            return false;
        }
        NoteItem longClickItem = mItem.get(position - NoteUtils.getOffset(position, mCategories));
        if (longClickItem == null) {
            return false;
        }

        longClickItem.setSelected(true);
        showType = NoteAdapter.TYPE_SHOW_CHECK;
        fab.setVisibility(View.GONE);
        selectedCount = 1;
        /* UNISOC: Add for bug 1186243 @{ */
        if (mAdapter != null) {
            mAdapter.setItems((ArrayList) mItem);
        }
        /* }@ */
        updateToolBar(Integer.toString(selectedCount), R.drawable.ic_ab_back_holo_light_up, new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                updateToolBar(title, R.drawable.ic_ab_folder_holo_light, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent FolderIntent = new Intent(NoteLatestDeleteActivity.this, NoteFolderListActivity.class);
                        startActivity(FolderIntent);
                    }
                });
                if (mAdapter != null ) {
                    mAdapter.setShowType(NoteAdapter.TYPE_SHOW_NORMAL);
                    showType = NoteAdapter.TYPE_SHOW_NORMAL;
                    //mNoteDataManager.initDataManager();
                    loadNotes(FILTER_TYPE, id);
                    mAdapter.setAllItemChecked(false);
                    selectedCount = 0;
                }
                invalidateOptionsMenu();
            }
        });
        invalidateOptionsMenu();
        if (mAdapter != null) {
            mAdapter.setShowType(NoteAdapter.TYPE_SHOW_CHECK);
        }
        refreshUi();
        return true;
    }
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        NoteItem clickedItem = mItem.get(position - NoteUtils.getOffset(position, mCategories));
        if (clickedItem == null) {
            return;
        }
        if(showType == NoteAdapter.TYPE_SHOW_CHECK){
            ListItemView itemView = (ListItemView)view.getTag(R.integer.listview_tag_2);
            itemView.mCheck.toggle();
            if (itemView.mCheck.isChecked()) {
                clickedItem.setSelected(true);
                selectedCount += 1;
            } else {
                clickedItem.setSelected(false);
                selectedCount -= 1;
            }
            toolbar.setTitle(Integer.toString(selectedCount));
        }

    }

    protected void onNewIntent(Intent intent) {
        DLog.i(TAG, "onNewIntent...."+intent);
        setIntent(intent);
        /*Modified for 712255: start*/
        title = getString(R.string.lastest_delete);
        toolbar.setTitle(title);
        /*Modified for 712255: end*/
        FILTER_TYPE = intent.getIntExtra("FILTER", 0);
        id = intent.getIntExtra("ID", 0);
        Log.d(TAG, "onNewIntent FILTER_TYPE = "+FILTER_TYPE+" : id = "+id);
        if (FILTER_TYPE == FolderUtil.FILTER_DELETE) {
            fab.setVisibility(View.GONE);
        } else {
            fab.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        /* if (RequestPermissionsActivity.startPermissionActivity(this)) {
            return;
        }*/
        /*UNISOC: Modify for bug 1240753 @{*/
        mDialogFragment = (AlertDialogFragment) getFragmentManager().findFragmentByTag(String.valueOf(DIALOG_DELTE_SOME_NOTES));
        if (mDialogFragment != null) {
            mDialogFragment.dismiss();
        }
        /*}@*/
        if (FILTER_TYPE != FolderUtil.FILTER_DELETE) {
            fab.setVisibility(View.VISIBLE);
        }

        clearItems();
        DLog.d(TAG, "FILTER_TYTP === " + FILTER_TYPE + ": id = " + id);

        loadNotes(FILTER_TYPE, id);


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_note_latest_delete, menu);
        mSelectAll = menu.getItem(0);
        mRecovery = menu.getItem(1);
        mDelete = menu.getItem(2);
        return true;
    }

    /**
     * Normal status: search menu
     * Editor normal folder menu status:selectAll,delete,move.
     * Editor lastest delete folder menu status: selectAll,delete,recovery.
     * @param menu
     * @return
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (showType == NoteAdapter.TYPE_SHOW_CHECK) {
            mSelectAll.setVisible(true);
            mDelete.setVisible(true);
            mRecovery.setVisible(true);
            if(selectedCount == 0) {
                mDelete.setEnabled(false);
                mRecovery.setEnabled(false);
                mSelectAll.setTitle(R.string.select_all);
            } else {
                mDelete.setEnabled(true);
                mRecovery.setEnabled(true);
                if (selectedCount == mItem.size()) {
                    mSelectAll.setTitle(R.string.unselect_all);
                } else {
                    mSelectAll.setTitle(R.string.select_all);
                }
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        return super.onContextItemSelected(item);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        switch(id) {
            case R.id.action_selected_all:
                if (mAdapter != null) {
                    if (selectedCount != mItem.size()) {
                        mAdapter.setItems((ArrayList) mItem);
                        mAdapter.setAllItemChecked(true);
                        selectedCount = mItem.size();

                    } else {
                        mAdapter.setItems((ArrayList) mItem);
                        mAdapter.setAllItemChecked(false);
                        selectedCount = 0;
                        mSelectAll.setTitle(R.string.select_all);
                    }
                    toolbar.setTitle(Integer.toString(selectedCount));
                }
                break;
            case R.id.action_recovery:
                if (selectedCount == 0) {
                    Toast.makeText(getApplicationContext(), R.string.selected_is_empty, Toast.LENGTH_LONG).show();
                    break;
                }
                recoverySelectedNotes();
                break;
            case R.id.action_delete:
                if (selectedCount == 0) {
                    Toast.makeText(getApplicationContext(), R.string.selected_is_empty, Toast.LENGTH_LONG).show();
                    break;
                }
                Bundle bundle = new Bundle();
                bundle.putInt("tag", DIALOG_DELTE_SOME_NOTES);
                showDialog(bundle);
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /*This method is used to category data.
    * because have two types for display.
    */
    public void loadNotes(final int filter, final int id) {
        switch (filter) {
            case FolderUtil.FILTER_ALL:
                mItem = mNoteDataManager.getNotesForPresetFolder(NoteDataManagerImpl.ALL_NOTE_FOLDER);
                break;
            case FolderUtil.FILTER_FAVORITE:
                mItem = mNoteDataManager.getNotesForPresetFolder(NoteDataManagerImpl.COLLECTED_FOLDER);
                break;
            case FolderUtil.FILTER_PRIVATE:
                mItem = mNoteDataManager.getNotesForPresetFolder(NoteDataManagerImpl.PRIVATE_FOLDER);
                break;
            case FolderUtil.FILTER_DELETE:
                mItem = mNoteDataManager.getNotesForPresetFolder(NoteDataManagerImpl.DELETED_NOTE_FOLDER);
                break;
            case FolderUtil.FILTER_CUSTOM:
                Log.d(TAG, "loadNotes..............");
                mItem = mNoteDataManager.getNotesForCustomFolder(id);
                break;
        }
        refreshUi();

    }

    private void refreshUi() {
        DLog.d(TAG, "refreshUI === mItem = "+mItem+":: size = "+(mItem != null ? mItem.size(): 0));
        if(mItem == null || mItem.size() <= 0 ) {
            clearItems();
            //mListView.setEmptyView(emptyView);
            if (mAdapter != null){
                mAdapter.setCategoryList(mCategories);
                mAdapter.notifyDataSetChanged();
            } else {
                mListView.setAdapter(null);
            }
            mListView.setEmptyView(emptyView);
        } else {
            clearItems();
            mCategories = NoteCategory.categoryNote((ArrayList)mItem);
            if (mAdapter == null) {
                mAdapter = new NoteAdapter(this, mCategories);
                mAdapter.setShowType(NoteAdapter.TYPE_SHOW_NORMAL);
                mListView.setAdapter(mAdapter);
                mListView.setTextFilterEnabled(true);
            } else {
                DLog.d(TAG, "refreshUi mAdapter != null.......");
                if (mListView.getAdapter() == null){
                    mListView.setAdapter(mAdapter);
                }
                mAdapter.setCategoryList(mCategories);
                mAdapter.notifyDataSetChanged();
            }
        }
    }

    /**
     * Update ToolBar display while long press list item and press back icon in editor page status.
     */
    private void updateToolBar(String title, int resId,View.OnClickListener listener) {
        Log.d(TAG, "title ==== "+title);
        if (title != null) {
            toolbar.setTitle(title);
        }
        toolbar.setNavigationIcon(resId);
        toolbar.setNavigationOnClickListener(listener);
    }

    private void clearItems() {
        if (mCategories != null) {
            mCategories.clear();
            mCategories = null;
        }
    }

    private void deleteSelectedNotes() {
        OperatorTask deleteTask = new OperatorTask();
        deleteTask.setActionType(TASK_TYPE_DELETE);
        deleteTask.execute();
    }

    private void recoverySelectedNotes() {
        OperatorTask recoveryTask = new OperatorTask();
        recoveryTask.setActionType(TASK_TYPE_RECOVERY);
        recoveryTask.execute();
    }
    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * AsyncTask is used to move or delete NoteItems,
     */
    private class OperatorTask extends AsyncTask<Integer, Integer, Boolean> {

        int actionType = 0;
        protected void setActionType(int type) {
            actionType = type;
        }
        @Override
        protected void onPreExecute() {
            acquireWakeLock();
            if (actionType == TASK_TYPE_DELETE) {
                Bundle bundle = new Bundle();
                bundle.putInt("tag", DIALOG_DELTE_PROGRESS);
                showDialog(bundle);
            } else if (actionType == TASK_TYPE_MOVE){
                Bundle bundle = new Bundle();
                bundle.putInt("tag", DIALOG_MOVE_PROGRESS);
                showDialog(bundle);
            }
        }
        @Override
        protected Boolean doInBackground(Integer... params) {
            if (actionType == TASK_TYPE_DELETE) {
                isDeleting = true;
                for (int i = 0; i < mItem.size(); i++) {
                    NoteItem note = mItem.get(i);
                    if (note.getIsSelected()) {
                        if (FILTER_TYPE == FolderUtil.FILTER_DELETE) {
                            mNoteDataManager.deleteNoteFromDeletedFolder(note);
                        } else {
                            mNoteDataManager.deleteNoteIntoDeletedFolder(note);
                        }
                        note.setSelected(false);
                        selectedCount -= 1;
                    }
                }
            } else { //Task recovery
                isRecovering = true;
                for (NoteItem item: mItem) {
                    if (item.getIsSelected()) {
                        mNoteDataManager.restoreNoteFromDeletedFolder(item);
                        item.setSelected(false);
                        selectedCount -= 1;
                    }
                }
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            AlertDialogFragment dialogFragment;
            if (actionType == TASK_TYPE_MOVE) {
                dialogFragment = (AlertDialogFragment) getFragmentManager().findFragmentByTag("" + DIALOG_MOVE_PROGRESS);
                if (dialogFragment != null) {
                    dialogFragment.dismiss();
                    isMoving = false;
                }
            } else if (actionType == TASK_TYPE_DELETE){
                dialogFragment = (AlertDialogFragment) getFragmentManager().findFragmentByTag("" + DIALOG_DELTE_PROGRESS);
                if (dialogFragment != null) {
                    dialogFragment.dismiss();
                    isDeleting = false;
                }
            } else {
                isRecovering = false;
                Toast.makeText(NoteLatestDeleteActivity.this, R.string.recovery_sucess, Toast.LENGTH_SHORT).show();
            }
            clearItems();
            loadNotes(FILTER_TYPE, id);
            NoteAppWidgetProvider.updateWidget(mContext);
            backMainPage();
            releaseWakeLock();

        }

        public void acquireWakeLock() {
            PowerManager powerManager = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
            if (mWakeLock == null) {
                mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "delete_notes");
                mWakeLock.acquire();
            }
        }

        public void releaseWakeLock() {
            if (mWakeLock != null) {
                mWakeLock.release();
                mWakeLock = null;
            }
        }

    }

    public static class AlertDialogFragment extends DialogFragment {

        int folderChoicedIndex = -1;
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final int fragmentTag = getArguments().getInt("tag");
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            switch (fragmentTag) {
                case DIALOG_MOVE_FOLDER:
                    builder.setTitle(R.string.movetoFolder);

                    final String[] folderTitle = getArguments().getStringArray(NoteLatestDeleteActivity.DIALOG_KEY_MOVE_TO);
                    builder.setSingleChoiceItems(folderTitle, -1,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    folderChoicedIndex = which;
                                    folderName = folderTitle[which];
                                }
                            });
                    builder.setNegativeButton(R.string.Cancel, null);
                    builder.setPositiveButton(R.string.Ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                   if (folderChoicedIndex != -1) {
                                       which = folderChoicedIndex;
                                   }
                                   Log.i(TAG, "Move..........which = "+which);
                                   ((NoteLatestDeleteActivity)getActivity()).doPositiveClick(which, dialog, getArguments());
                                }
                            });
                    break;
                case DIALOG_MOVE_PROGRESS:
                    ProgressDialog mProgressDialog = new ProgressDialog(getActivity());
                    mProgressDialog.setTitle(R.string.move);
                    mProgressDialog.setMessage(getActivity().getResources().getString(R.string.moving));
                    mProgressDialog.setCanceledOnTouchOutside(false);
                    return mProgressDialog;
                case DIALOG_DELTE_SOME_NOTES:
                    builder.setNegativeButton(R.string.Cancel, null);
                    builder.setTitle(R.string.prompt);
                    builder.setMessage(R.string.delete_forever_tip);
                    builder.setPositiveButton(R.string.Ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Log.d(TAG, "delete som notes which = "+which);
                                    ((NoteLatestDeleteActivity)getActivity()).doPositiveClick(which, dialog, getArguments());
                                }
                            });
                    break;
                case DIALOG_DELTE_PROGRESS:
                    mProgressDialog = new ProgressDialog(getActivity());
                    mProgressDialog.setTitle(R.string.delete);
                    mProgressDialog.setMessage(getActivity().getResources().getString(R.string.Deleting));
                    mProgressDialog.setCanceledOnTouchOutside(false);
                    return mProgressDialog;
                default:
                    break;
            }
            return builder.create();
        }
    }

    public void doPositiveClick (int which, DialogInterface dialog, Bundle bundle) {

        switch (bundle.getInt("tag")) {
            case DIALOG_DELTE_SOME_NOTES:
                Log.d("doPositiveClick", "deleteTask.execute");
                deleteSelectedNotes();
                break;
            default:
                break;
        }
    }

    //UNISOC: Modify for bug 1240753
    private void showDialog (Bundle bundle) {
        mDialogFragment = new AlertDialogFragment();
        mDialogFragment.setArguments(bundle);
        if (bundle.getInt("tag") == DIALOG_DELTE_PROGRESS ) {
            mDialogFragment.setCancelable(false);
        }else {
            mDialogFragment.setCancelable(true);
        }
        mDialogFragment.show(getFragmentManager(), String.valueOf(bundle.getInt("tag")));
    }

    private boolean backMainPage() {
        if (showType == NoteAdapter.TYPE_SHOW_CHECK) {
            updateToolBar(title, R.drawable.ic_ab_folder_holo_light, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent FolderIntent = new Intent(NoteLatestDeleteActivity.this, NoteFolderListActivity.class);
                    startActivity(FolderIntent);
                }
            });
            if (mAdapter != null ) {
                mAdapter.setShowType(NoteAdapter.TYPE_SHOW_NORMAL);
                showType = NoteAdapter.TYPE_SHOW_NORMAL;
                loadNotes(FILTER_TYPE, id);
                selectedCount = 0;
            }
            invalidateOptionsMenu();
            return true;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        if (backMainPage()) {
            return;
        }
        super.onBackPressed();
    }
}
