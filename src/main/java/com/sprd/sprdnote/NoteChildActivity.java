package com.sprd.sprdnote;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.ImageView;
import android.text.Editable;
import android.text.TextWatcher;

import com.sprd.sprdnote.data.FolderItem;
import com.sprd.sprdnote.data.NoteDataManagerImpl;
import com.sprd.notejar.view.data.NoteItem;
import com.sprd.sprdnote.folder.FolderUtil;
import com.sprd.sprdnote.util.DLog;
import com.sprd.notejar.view.util.NoteCategory;
import com.sprd.notejar.view.util.NoteUtils;
import com.sprd.notejar.view.ListItemView;
import com.sprd.sprdnote.widget.NoteAppWidgetProvider;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class NoteChildActivity extends AppCompatActivity implements OnItemClickListener, AdapterView.OnItemLongClickListener,
        SearchView.OnQueryTextListener, SearchView.OnCloseListener, SearchView.OnClickListener {

    private static final String TAG = "NoteChildActivity";
    private static final int TASK_TYPE_DELETE = 0;
    private static final int TASK_TYPE_MOVE = 1;
    private static final int TASK_TYPE_RECOVERY = 2;
    private static final int MAX_INPUT_LENGTH = 100;

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
    private View emptySearchView;
    private Toolbar toolbar;
    private SearchView mSearchView;
    private FloatingActionButton fab;
    private MenuItem mSearch;
    private MenuItem mDelete;
    private MenuItem mMove;
    private MenuItem mSelectAll;
    private MenuItem mRecovery;

    private Context mContext;
    private PowerManager.WakeLock mWakeLock;
    private NoteDataManagerImpl mNoteDataManager;
    private List<NoteItem> mItem = new ArrayList<NoteItem>();
    private ArrayList<NoteItem> mSearchItems = new ArrayList<>();
    private ArrayList<NoteCategory> mCategories = null;
    private NoteAdapter mAdapter = null;
    private ArrayList<FolderItem> folders;
    private AlertDialogFragment mDialogFragment;

    private String title;
    private String backupTitle;
    private String searchKey;
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
    private int fragmentTag;

    Handler mHander = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            refreshUi();
        }
    };
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
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("fragment_tag", fragmentTag);
        fragmentTag = -1;
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        fragmentTag = savedInstanceState.getInt("fragment_tag");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);
        Intent intent = getIntent();
        FILTER_TYPE = intent.getIntExtra("FILTER", 0);
        id = intent.getIntExtra("ID", 0);
        Log.d(TAG, "FILTER_TYPE = "+FILTER_TYPE+" : id = "+id);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        /*Modified for 712255: start*/
        if (id == NoteDataManagerImpl.ID_COLLECT_FOLDER) {
            title = getString(R.string.my_favorite);
        } else if (id == NoteDataManagerImpl.ID_PRIVATE_FOLDER){
            title = getString(R.string.private_note);
        } else {
            title = (String)intent.getCharSequenceExtra("TITLE");
        }
        if (title != null && title.length() > 0) {
            toolbar.setTitle(title);
        }
        /*Modified for 712255: end*/
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_ab_folder_holo_light);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent FolderIntent = new Intent(NoteChildActivity.this, NoteFolderListActivity.class);
                startActivity(FolderIntent);
                finish();//UNISOC: Modify for bug 1399016
               // startActivity(new Intent(NoteActivity.this, TestActivity.class));
            }
        });
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent editorIntent = new Intent(NoteChildActivity.this, NoteEditorActivity.class);
                editorIntent.putExtra(NoteEditorActivity.OPEN_TYPE, NoteEditorActivity.TYPE_NEW_NOTE);
                editorIntent.putExtra("FOLDER_ID", id);
                editorIntent.putExtra("FILTER_TYPE", FILTER_TYPE);
                startActivity(editorIntent);
            }
        });
        if (title == null) {
            title = getString(R.string.all_notes);
        }
        backupTitle = title;
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
        emptySearchView = findViewById(R.id.note_empty_search);
        if (FILTER_TYPE == FolderUtil.FILTER_DELETE) {
            fab.setVisibility(View.GONE);
        } else {
            fab.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_SEARCH && mSearchView != null) {
            isSearchMode = true;
            mSearchView.requestFocus();
            mSearchView.setIconified(false);
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long l) {
        if (showType == NoteAdapter.TYPE_SHOW_CHECK || isSearchMode) {
            return false;
        }
        /* modify for bug 853761 @{ */
        int idx = position - NoteUtils.getOffset(position, mCategories);
        if (idx < 0 || idx >= mItem.size()) {
            return false;
        }
        NoteItem longClickItem = mItem.get(idx);
        /* @} */
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
                        Intent FolderIntent = new Intent(NoteChildActivity.this, NoteFolderListActivity.class);
                        startActivity(FolderIntent);
                        finish();//UNISOC: Modify for bug 1399016
                    }
                });
                if (mAdapter != null ) {
                    mAdapter.setShowType(NoteAdapter.TYPE_SHOW_NORMAL);
                    showType = NoteAdapter.TYPE_SHOW_NORMAL;
                    loadNotes(FILTER_TYPE, id);
                    mAdapter.setAllItemChecked(false);
                    selectedCount = 0;
                }
                fab.setVisibility(View.VISIBLE);
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
        Log.d(TAG, "onItemClick........position = = "+position+": mItem.size == "+mItem.size());
        NoteItem clickedItem = null;
        /* modify for bug 853761 @{ */
        int idx = position - NoteUtils.getOffset(position, mCategories);
        if (idx < 0) {
            return;
        }
        if (!isSearchMode) {
            clickedItem = ((idx < mItem.size()) ? mItem.get(idx) : null);
        } else {
            if (searchKey != null && searchKey.length() != 0) {
                if (mSearchItems.size() != 0) {
                    clickedItem = ((idx < mSearchItems.size()) ? mSearchItems.get(idx) : null);
                }
            }else {
                Log.d(TAG, "Is searchKey ============== 00000000000000000000000");
                clickedItem = ((idx < mItem.size()) ? mItem.get(idx) : null);
            }
        }
        /* @} */
        Log.d(TAG, "onItemClick........position = = "+position+"::: clickItem = ="+clickedItem +": offset = "+ NoteUtils.getOffset(position, mCategories));
        if (clickedItem == null) {
            return;
        }
        if (showType == NoteAdapter.TYPE_SHOW_NORMAL) {
            Intent intent = new Intent();
            intent.setClass(this, NoteEditorActivity.class);
            intent.putExtra(NoteEditorActivity.ID, clickedItem.getId());
            intent.putExtra(NoteEditorActivity.OPEN_TYPE, NoteEditorActivity.TYPE_EDIT_NOTE);
            intent.putExtra("FILTER_TYPE", FILTER_TYPE);
            startActivity(intent);
        } else if(showType == NoteAdapter.TYPE_SHOW_CHECK){
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
        FILTER_TYPE = intent.getIntExtra("FILTER", 0);
        id = intent.getIntExtra("ID", 0);
        /*Modified for 712255: start*/
        if (id == NoteDataManagerImpl.ID_COLLECT_FOLDER) {
            title = getString(R.string.my_favorite);
        } else if (id == NoteDataManagerImpl.ID_PRIVATE_FOLDER){
            title = getString(R.string.private_note);
        } else {
            title = (String)intent.getCharSequenceExtra("TITLE");
        }
        if (title != null && title.length() > 0) {
            toolbar.setTitle(title);
        }
        /*Modified for 712255: end*/
        Log.d(TAG, "onNewIntent FILTER_TYPE = "+FILTER_TYPE+" : id = "+id+ ": title = "+title);
        fab.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(RequestPermissionsActivity.startPermissionActivity(this)){
            return;
        }
        mDialogFragment = (AlertDialogFragment) getFragmentManager().findFragmentByTag("" + fragmentTag);
        if (mDialogFragment != null) {
            mDialogFragment.dismiss();
        }
        if (isSearchMode) {
            Log.d(TAG, "mSear size === "+mSearchItems.size());
            fab.setVisibility(View.GONE);
            if (searchKey != null && searchKey.length() != 0) {
                if (mSearchItems.size() != 0) {
                    //mSearchItems = results;
                    if (mListView != null) {
                        if (mListView.getAdapter() == null) {
                            mListView.setAdapter(mAdapter);
                        }
                        clearItems();
                        loadNotes(FILTER_TYPE, id);
                        if (mItem.size() != 0 && searchKey != null && searchKey.length() != 0) {
                            ArrayList<NoteItem> results = new ArrayList<>();
                            ArrayList<Integer> results_id = new ArrayList<>();
                            for (int i = 0; i < mItem.size(); i++) {
                                String title = mItem.get(i).getTitle();
                                String content = mItem.get(i).getContent();
                                content = NoteUtils.parseXml(content);
                                if (title != null && title.length() != 0) {
                                    if (title.toLowerCase().contains(searchKey.toLowerCase())) {
                                        results.add(mItem.get(i));
                                        results_id.add(mItem.get(i).getId());
                                    }
                                }
                                if (content != null && content.length() != 0) {
                                    if (content.toLowerCase().contains(searchKey.toLowerCase())) {
                                        Log.d(TAG, "search content = === " + content);
                                        if (!results_id.contains(mItem.get(i).getId())) {
                                            results.add(mItem.get(i));
                                            results_id.add(mItem.get(i).getId());
                                        }
                                    }
                                }
                            }
                            mSearchItems = results;
                            /*Modified for 727920: start*/
                            if (mSearchItems.size() != 0) {
                                if (mCategories != null) {
                                    mCategories.clear();
                                }
                                mCategories = NoteCategory.categoryNote(mSearchItems);
                                mAdapter.setCategoryList(mCategories);
                                mAdapter.notifyDataSetChanged();
                            } else {
                                mListView.setAdapter(null);
                                mListView.setEmptyView(emptySearchView);
                            }
                        } else if (mItem.size() == 0) {
                            mListView.setAdapter(null);
                            mListView.setEmptyView(emptySearchView);
                        }
                        /*Modified for 727920: end*/
                    }
                } else {
                    if (mListView != null) {
                        mListView.setAdapter(null);
                        mListView.setEmptyView(emptySearchView);
                    }
                }
            } else {
                clearItems();
                loadNotes(FILTER_TYPE, id);
            }
        } else {
            searchKey = "";
            if (FILTER_TYPE != FolderUtil.FILTER_DELETE || showType != NoteAdapter.TYPE_SHOW_CHECK) {
                fab.setVisibility(View.VISIBLE);
            }
            if(FILTER_TYPE == FolderUtil.FILTER_PRIVATE) {
                SharedPreferences preferences = getSharedPreferences("security", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor= preferences.edit();
                editor.putLong("enter_time",System.currentTimeMillis());
                editor.commit();
            }

            clearItems();
            DLog.d(TAG, "FILTER_TYTP === "+FILTER_TYPE+": id = "+id);

            loadNotes(FILTER_TYPE, id);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_note, menu);
        mSearch = menu.getItem(0);
        mSelectAll = menu.getItem(1);
        mRecovery = menu.getItem(2);
        mDelete = menu.getItem(3);
        mMove = menu.getItem(4);
        mSearchView =
                (SearchView) mSearch.getActionView();
        if (mSearchView != null) {
            try {
                Class<?> argClass = mSearchView.getClass();
                Field mSearchHintIconField = argClass.getDeclaredField("mSearchHintIcon");
                mSearchHintIconField.setAccessible(true);
                mSearchHintIconField.set(mSearchView, null);

                Field searchTextView = argClass.getDeclaredField("mSearchSrcTextView");
                searchTextView.setAccessible(true);
                TextView searchText = (TextView)searchTextView.get(mSearchView);
                Field mCursorDrawableRes = TextView.class.getDeclaredField("mCursorDrawableRes");
                /*Add for bug 715922 :end*/
                searchText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {
                        int length = editable.toString().length();
                        if (length > MAX_INPUT_LENGTH ) {
                            editable.delete(MAX_INPUT_LENGTH, length);
                        }
                    }

                });
                /*Add for bug 715922 :end*/
                mCursorDrawableRes.setAccessible(true);
                mCursorDrawableRes.set(searchText, R.drawable.cursor_shape_normal);
            }catch (NoSuchFieldException e){
                e.printStackTrace();
            }catch (IllegalAccessException ex) {
                ex.printStackTrace();
            }
            mSearchView.setOnQueryTextListener(this);
            mSearchView.setOnCloseListener(this);
            mSearchView.setOnSearchClickListener(this);
            mSearchView.setQueryHint(mContext.getString(R.string.query_hint));
            setCloseBtnGone(true);
            mSearchView.setOnQueryTextFocusChangeListener(new SearchView.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (!hasFocus) {
                        mSearchView.setIconified(true);
                    }
                }
            });

            mSearchView.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View view, int i, KeyEvent keyEvent) {
                    invalidateOptionsMenu();
                    return false;
                }
            });
        }
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
            mSearch.setVisible(false);
            mSelectAll.setVisible(true);
            mDelete.setVisible(true);
            if (FILTER_TYPE != FolderUtil.FILTER_DELETE) {
                mMove.setVisible(true);
                mRecovery.setVisible(false);
            } else {
                mRecovery.setVisible(true);
                mMove.setVisible(false);
            }
            if(selectedCount == 0) {
                mDelete.setEnabled(true);
                mMove.setEnabled(true);
                mSelectAll.setTitle(R.string.select_all);
            } else {
                mDelete.setEnabled(true);
                if (FILTER_TYPE != FolderUtil.FILTER_DELETE) {
                    mMove.setEnabled(true);
                } else {
                    mRecovery.setEnabled(true);
                }
                if (selectedCount == mItem.size()) {
                    mSelectAll.setTitle(R.string.unselect_all);
                } else {
                    mSelectAll.setTitle(R.string.select_all);
                }
            }
        } else {//normal
            mSearch.setVisible(true);
            mSelectAll.setVisible(false);
            mDelete.setVisible(false);
            mMove.setVisible(false);
            mRecovery.setVisible(false);
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
            case R.id.action_search:
                Log.d(TAG, "action_search......................");
                break;
            case R.id.action_selected_all:
                if (mAdapter != null) {
                    if (selectedCount != mItem.size()) {
                        mAdapter.setItems((ArrayList) mItem);
                        mAdapter.setAllItemChecked(true);
                        selectedCount = mItem.size();
                        /* SPRD: modified for bug900064 @{ */
                        //mSelectAll.setTitle(R.string.unselect_all);
                        /* @} */
                    } else {
                        mAdapter.setItems((ArrayList) mItem);
                        mAdapter.setAllItemChecked(false);
                        selectedCount = 0;
                        /* SPRD: modified for bug900064 @{ */
                        //mSelectAll.setTitle(R.string.select_all);
                        /* @} */
                    }
                    toolbar.setTitle(Integer.toString(selectedCount));
                }
                break;
            case R.id.action_recovery:
                if (selectedCount == 0) {
                    Toast.makeText(getApplicationContext(), R.string.selected_is_empty, Toast.LENGTH_SHORT).show();
                    break;
                }
                recoverySelectedNotes();
                break;
            case R.id.action_delete:
                if (selectedCount == 0) {
                    Toast.makeText(getApplicationContext(), R.string.selected_is_empty, Toast.LENGTH_SHORT).show();
                    break;
                }
                Bundle bundle = new Bundle();
                bundle.putInt("tag", DIALOG_DELTE_SOME_NOTES);
                fragmentTag = DIALOG_DELTE_SOME_NOTES;
                showDialog(bundle);
                break;
            case R.id.action_move:
                if (selectedCount == 0) {
                    Toast.makeText(getApplicationContext(), R.string.selected_is_empty, Toast.LENGTH_SHORT).show();
                    break;
                }
                folders = (ArrayList)mNoteDataManager.getFolderListForNoteMove();
                if (folders.size() == 0) {
                    Toast.makeText(NoteChildActivity.this,R.string.valid_folder_null, Toast.LENGTH_SHORT).show();
                    break;
                }
                String[] folderNameStr = new String[folders.size()];
                for (int i = 0; i < folders.size(); i++){
                    folderNameStr[i] = folders.get(i).getName();
                    Log.d(TAG, "filder name str ["+i+"] = "+folderNameStr[i] + " id = "+folders.get(i).getId());
                }
                Bundle bundles = new Bundle();
                bundles.putStringArray(DIALOG_KEY_MOVE_TO, folderNameStr);
                bundles.putInt("tag", DIALOG_MOVE_FOLDER);
                fragmentTag = DIALOG_MOVE_FOLDER;
                showDialog(bundles);
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
          //  mListView.setAdapter(null);//删除不刷新待定。。。。。
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

    private void moveSelectedNotes(int which) {
        OperatorTask moveTask = new OperatorTask();
        moveTask.setActionType(TASK_TYPE_MOVE);
        moveTask.execute(which);
    }

    private void recoverySelectedNotes() {
        OperatorTask recoveryTask = new OperatorTask();
        recoveryTask.setActionType(TASK_TYPE_RECOVERY);
        recoveryTask.execute();
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        fab.setVisibility(View.GONE);
        Log.d(TAG, "onQueryTextChange....................."+newText);
        if (mAdapter != null) {
            mAdapter.setSearchKey(newText);
        }
        searchKey = newText;

        if (newText.length() != 0) {
            ArrayList<NoteItem> results = new ArrayList<>();
            ArrayList<Integer> results_id = new ArrayList<>();
            if (mItem.size() != 0) {
                for (int i = 0; i < mItem.size(); i++) {
                    String title = mItem.get(i).getTitle();
                    String content = mItem.get(i).getContent();
                    content = NoteUtils.parseXml(content);
                    if (title != null && title.length() != 0) {
                        if (title.toLowerCase().contains(newText.toLowerCase())) {
                            results.add(mItem.get(i));
                            results_id.add(mItem.get(i).getId());
                        }
                    }
                    if (content != null && content.length() != 0) {
                        if (content.toLowerCase().contains(newText.toLowerCase())) {
                            if (!results_id.contains(mItem.get(i).getId())) {
                                results.add(mItem.get(i));
                                results_id.add(mItem.get(i).getId());
                            }
                        }
                    }
                }
            } else {
                return false;
            }
            if (results.size() != 0) {
                mSearchItems = results;
                if (mListView != null) {
                    if (mListView.getAdapter() == null) {
                        mListView.setAdapter(mAdapter);
                    }
                    if (mCategories != null) {
                        mCategories.clear();
                    }
                    mCategories = NoteCategory.categoryNote(results);
                    mAdapter.setCategoryList(mCategories);
                    mAdapter.notifyDataSetChanged();
                }
            } else {
                if (mListView != null) {
                    mListView.setAdapter(null);
                    mListView.setEmptyView(emptySearchView);
                }
                mSearchItems = new ArrayList<NoteItem>();
            }
        } else {
            if (mAdapter != null) {
                mAdapter.setSearchKey("");
            }
            setCloseBtnGone(true);
            loadNotes(FILTER_TYPE, id);
        }


        return false;
    }

    @Override
    public boolean onClose() {
        DLog.d(TAG, "onClose......");
        if (mAdapter != null) {
            mAdapter.setSearchKey(null);
        }
        updateToolBar(title, R.drawable.ic_ab_folder_holo_light, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent FolderIntent = new Intent(NoteChildActivity.this, NoteFolderListActivity.class);
                startActivity(FolderIntent);
                finish();//UNISOC: Modify for bug 1399016
            }
        });

        refreshUi();
        fab.setVisibility(View.VISIBLE);
        isSearchMode = false;
        return false;
    }

    @Override
    public void onClick(View view) {
        DLog.d(TAG, "onClick..............");
        isSearchMode = true;
        mSearchItems =(ArrayList)mItem;
        fab.setVisibility(View.GONE);
        setCloseBtnGone(true);
        updateToolBar(null, R.drawable.ic_ab_back_holo_light_up, new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                updateToolBar(title, R.drawable.ic_ab_folder_holo_light, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent FolderIntent = new Intent(NoteChildActivity.this, NoteFolderListActivity.class);
                        startActivity(FolderIntent);
                        finish();//UNISOC: Modify for bug 1399016
                    }
                });

                if (mAdapter != null) {
                    mAdapter.setSearchKey(null);
                }

                refreshUi();
                fab.setVisibility(View.VISIBLE);
                isSearchMode = false;
                invalidateOptionsMenu();
                //Hide keyboard.
                InputMethodManager imm = (InputMethodManager) NoteChildActivity.this
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(mSearchView.getWindowToken(), 0);
            }
        });
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
            } else if (actionType == TASK_TYPE_MOVE) {
                isMoving = true;
                DLog.d(TAG, "id ============="+params[0]);
                for (NoteItem item : mItem) {
                    DLog.d(TAG, "item.parentId = "+item.getParentFolderId()+" : isSelected = = "+item.getIsSelected());
                    if (item.getIsSelected()) {
                        int parentFolderId = item.getParentFolderId();
                        DLog.i(TAG, "parentFolderId ===== "+parentFolderId);
                        int futureFolderId = params[0];//folder.getId();
                        if (parentFolderId != futureFolderId) {
                            if (parentFolderId == NoteDataManagerImpl.ID_COLLECT_FOLDER) {
                                item.setCollected(false);
                            }
                            if (futureFolderId == NoteDataManagerImpl.ID_COLLECT_FOLDER) {
                                item.setCollected(true);
                            }
                            if (futureFolderId == NoteDataManagerImpl.ID_PRIVATE_FOLDER) {
                                item.setPrivate(true);
                                if (item.isCollected()) {
                                    item.setCollected(false);
                                }
                            } else if (parentFolderId == NoteDataManagerImpl.ID_PRIVATE_FOLDER){
                                item.setPrivate(false);
                            }
                            item.setParentFolderId(futureFolderId);
                        }
                        item.setSelected(false);
                        mNoteDataManager.updateNote(item);
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
                    fragmentTag = -1;
                    isMoving = false;
                }
            } else if (actionType == TASK_TYPE_DELETE){
                dialogFragment = (AlertDialogFragment) getFragmentManager().findFragmentByTag("" + DIALOG_DELTE_PROGRESS);
                if (dialogFragment != null) {
                    dialogFragment.dismiss();
                    fragmentTag = -1;
                    isDeleting = false;
                }
            } else {
                isRecovering = false;
                Toast.makeText(NoteChildActivity.this, R.string.recovery_sucess, Toast.LENGTH_SHORT).show();
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
        Bundle bundle = null;
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            bundle = getArguments();
            final int fragmentTag = bundle.getInt("tag");
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            AlertDialog dialog = null;
            switch (fragmentTag) {
                case DIALOG_MOVE_FOLDER:
                    builder.setTitle(R.string.movetoFolder);

                    final String[] folderTitle = getArguments().getStringArray(NoteChildActivity.DIALOG_KEY_MOVE_TO);
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
                                   DLog.d(TAG, "Move..........which = "+which);
                                   ((NoteChildActivity)getActivity()).doPositiveClick(which, dialog, getArguments());
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
                    builder.setMessage(R.string.delete_selected_items);
                    builder.setPositiveButton(R.string.Ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    DLog.d(TAG, "delete som notes which = "+which);
                                    ((NoteChildActivity)getActivity()).doPositiveClick(which, dialog, getArguments());
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
            dialog = builder.create();
            dialog.setCanceledOnTouchOutside(false);
            return dialog;
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            if (bundle.getInt("tag") == DIALOG_MOVE_FOLDER && getActivity() != null) {
                folderChoicedIndex = -1;
                folderName = null;
            }
            super.onDismiss(dialog);
        }
    }

    public void doPositiveClick (int which, DialogInterface dialog, Bundle bundle) {

        switch (bundle.getInt("tag")) {
            case DIALOG_DELTE_SOME_NOTES:
                DLog.d("doPositiveClick", "deleteTask.execute");
                deleteSelectedNotes();
                break;
            case DIALOG_MOVE_FOLDER:
                DLog.d("doPositiveClick", "moveTask.execute");
                if (folderName != null && folderName.length() != 0 && folders != null && folders.size() != 0) {
                    for (FolderItem item: folders) {
                        if (folderName.equals(item.getName())) {
                            which = item.getId();
                        }
                    }
                    moveSelectedNotes(which);
                }
                break;
            default:
                break;
        }
    }
    void showDialog (Bundle bundle) {
        /*Modified for bug 725277: start*/
        if(mDialogFragment!=null &&  mDialogFragment.getDialog()!=null
                && mDialogFragment.getDialog().isShowing()
                && mDialogFragment.getArguments().getInt("tag") == (DIALOG_MOVE_FOLDER)) {
            return;
        }
        mDialogFragment = new AlertDialogFragment();
        mDialogFragment.setArguments(bundle);
        if (bundle.getInt("tag") == DIALOG_DELTE_PROGRESS ) {
            mDialogFragment.setCancelable(false);
        }else {
            mDialogFragment.setCancelable(true);
        }
        mDialogFragment.show(getFragmentManager(), "" + bundle.getInt("tag"));
        /*Modified for bug 725277: end*/
    }

    private boolean backMainPage() {
        if (showType == NoteAdapter.TYPE_SHOW_CHECK) {
            updateToolBar(title, R.drawable.ic_ab_folder_holo_light, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent FolderIntent = new Intent(NoteChildActivity.this, NoteFolderListActivity.class);
                    startActivity(FolderIntent);
                    finish();//UNISOC: Modify for bug 1399016
                }
            });
            if (mAdapter != null ) {
                mAdapter.setShowType(NoteAdapter.TYPE_SHOW_NORMAL);
                showType = NoteAdapter.TYPE_SHOW_NORMAL;
                loadNotes(FILTER_TYPE, id);
                selectedCount = 0;
            }
            invalidateOptionsMenu();
            fab.setVisibility(View.VISIBLE);
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

    /*Add for bug 717064: start*/
    public void setCloseBtnGone(boolean bool){
        int closeBtnId = getResources().getIdentifier(
                "android:id/search_close_btn", null, null);
        ImageView closeButton = null;
        if (mSearchView!=null) {
            closeButton = (ImageView) mSearchView
                    .findViewById(closeBtnId);
            if (closeButton != null) {
                closeButton.setImageDrawable(getResources().getDrawable(
                    R.drawable.ic_clear_material));
                if (bool) {
                    closeButton.setVisibility(View.GONE);
                } else {
                    closeButton.setVisibility(View.VISIBLE);
                }
           }

        }
    }
    /*Add for bug 717064: end*/

}
