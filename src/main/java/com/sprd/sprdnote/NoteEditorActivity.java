package com.sprd.sprdnote;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.io.IOException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.sprd.notejar.view.data.NoteItem;
import com.sprd.sprdnote.data.NoteDataManager;
import com.sprd.sprdnote.data.NoteDataManagerImpl;
import com.sprd.sprdnote.folder.FolderUtil;
import com.sprd.sprdnote.util.BackgroundResource;
import com.sprd.sprdnote.util.DLog;
import com.sprd.notejar.view.util.NoteUtils;
//import com.sprd.sprdnote.view.CustomEditText;
import com.sprd.notejar.view.RichText;
import com.sprd.sprdnote.widget.NoteAppWidgetProvider;
import com.sprd.notejar.view.CustomEditText;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
/* SPRD: add for bug916643 {@ */
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.graphics.Matrix;
/* @ }*/

@SuppressLint("SimpleDateFormat")
public class NoteEditorActivity extends AppCompatActivity implements OnItemClickListener {

    private static final String TAG = "NoteEditorActivity";
    private static final int REQUEST_CODE_PICK_IMAGE = 1023;
    private static final int REQUEST_CODE_CAPTURE_CAMEIA = 1022;
    private static final int SAVE_PICTURE_SUCCESS = 2;
    private static final int SAVE_PICTURE_FAILED = 3;
    private static final int MAX_PIC_NUM = 10;
    public static final String OPEN_TYPE = "open_type";
    public static final String ID = "id";
    public static final int TYPE_EDIT_NOTE = 0;
    public static final int TYPE_NEW_NOTE = 1;

    private RichText mEditor;
    private NoteItem mNoteItem = new NoteItem();
    private NoteItem mOldItem = new NoteItem(); //Used to note update.
    private NoteItem mTempNote = new NoteItem();
    private ImageView mAgencyImg, mCameraImg, mPictureImg, mWallpaperImg;
    private View.OnClickListener mOperateListener;
    private View mNoteBgColorSelector;
    private LinearLayout mEditorLayout;
    private LinearLayout mEditor_content;
    private TextView mEditor_time;
    private File mImage = null;
    private ProgressDialog mProgressDialog;

    private NoteDataManager mNoteDataManager;
    private boolean isFavorite = false;
    private int openType = 0;
    private int filterType = 0;
    private int fromWidget = 0;
    private int itemPosition = -1;
    int folderId ;
    private static final File PHOTO_DIR = new File(
            Environment.getExternalStorageDirectory() + "/DCIM/Camera");
    private File mCurrentPhotoFile;
    private AlertDialog.Builder mDialogBuilder;
    private AlertDialog mDialog;
    private Toolbar mToolbar;
    private int mGlMaxTextureSize = 0;  //Add for bug916643
    /*Add for bug711375 :start*/
    private String mTemporaryContent;
    private int saveType;
    private long saveTime;
    private int saveBgId = -1;
    private boolean isSaveState = false;
    private boolean isOnNewIntent = false;
    /*Add for bug711375 end*/
    private NoteBackGroundSettings mNoteSettingChangedListener;    //Add for bug1011301

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_editor_main);
        mToolbar = (Toolbar) findViewById(R.id.toolbar_editor);
        mToolbar.setTitle(R.string.note_editor);
        setSupportActionBar(mToolbar);
        Intent intent = getIntent();
        fromWidget = intent.getIntExtra("from_widget", 0);
        folderId = intent.getIntExtra("FOLDER_ID", NoteDataManagerImpl.ID_ALLNOTE_FOLDER);
        filterType = intent.getIntExtra("FILTER_TYPE", FolderUtil.FILTER_ALL);
        openType = intent.getIntExtra(OPEN_TYPE, 0);
        DLog.d(TAG, "fromwidget = "+ fromWidget+" :filder id ======="+folderId+": filterType = "+filterType+" : opentype = "+openType
                +": intent = "+intent+ ": bundle = "+intent.getExtras()+ "itemPosition = = "+itemPosition);
        /*Modified for bug 723261: start*/
        if (intent.getExtras() == null) {
            startActivity(new Intent(NoteEditorActivity.this, NoteActivity.class));
            finish();
        } else {/*Modified for bug 723261: end*/
            mToolbar.setNavigationIcon(R.drawable.ic_done_holo_light);
            mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // startActivity(new Intent(NoteActivity.this, NoteFolderListActivity.class));
                    if (!isFinishing()) {
                        saveContent(false);
                        // mEditor.hideKeyBoard();
                        if (fromWidget == 1) {
                            startActivity(new Intent(NoteEditorActivity.this, NoteActivity.class));
                        }
                        hideKeyBoard();
                        finish();
                    }
                }
            });

            mNoteDataManager = NoteDataManagerImpl.getInstance(this, null);
            mOperateListener = new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    if (v.getId() == mPictureImg.getId()) {
                        hideBgDrawableSelector();
                        if (isSupportImage()) {
                            if (mEditor.getPicCount() < MAX_PIC_NUM) {
                                Intent intent = new Intent(Intent.ACTION_PICK);
                                intent.setType("image/*");
                                try {
                                    startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
                                } catch(ActivityNotFoundException e) {
                                    Toast.makeText(NoteEditorActivity.this, R.string.no_activity, Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(NoteEditorActivity.this, R.string.picture_add_fail, Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(NoteEditorActivity.this, R.string.not_support_image, Toast.LENGTH_SHORT).show();
                        }
                    } else if (v.getId() == mCameraImg.getId()) {
                        hideBgDrawableSelector();
                        if (isSupportImage()){
                            if (mEditor.getPicCount() < MAX_PIC_NUM) {
                                openCamera();
                            } else {
                                Toast.makeText(NoteEditorActivity.this, R.string.picture_add_fail, Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(NoteEditorActivity.this, R.string.not_support_image, Toast.LENGTH_SHORT).show();
                        }
                    } else if (v.getId() == mAgencyImg.getId()){
                        hideBgDrawableSelector();
                        mEditor.insertTodo("", getWindow().getCurrentFocus(), NoteEditorActivity.this.getString(R.string.not_support_multiline_todo));
                    } else if (v.getId() == mWallpaperImg.getId()){
                        if (mNoteBgColorSelector.getVisibility() == View.VISIBLE) {
                            mNoteBgColorSelector.setVisibility(View.GONE);
                        } else {
                            mNoteBgColorSelector.setVisibility(View.VISIBLE);
                            if (mNoteItem.getBackgroundId() != -1) {
                                findViewById(BackgroundResource.sBgSelectorSelectionMap.get(mNoteItem.getBackgroundId())).setVisibility(
                                    View.VISIBLE);
                            }
                        }
                    } else if (BackgroundResource.sBgSelectorBtnsMap.containsKey(v.getId())) {
                        if (mNoteItem.getBackgroundId() != -1) {
                            findViewById(BackgroundResource.sBgSelectorSelectionMap.get(mNoteItem.getBackgroundId())).setVisibility(
                                View.GONE);
                        }
                        DLog.d(TAG, "v.getId ======"+v.getId());
                        mNoteItem.setBackgroundId(BackgroundResource.sBgSelectorBtnsMap.get(v.getId()));
                        mNoteBgColorSelector.setVisibility(View.GONE);
                    }
                }
            };

            initViews();        //UNISOC: update for bug1152664
            /*Add for bug 716715: start*/
            if(RequestPermissionsActivity.startPermissionActivity(this)){
                return;
            }
            /*Add for bug 716715: end*/

            if (openType == TYPE_EDIT_NOTE) {
                int id = getIntent().getIntExtra(ID, 0);
                mNoteItem = mNoteDataManager.getNoteItem(id);
                //UNISOC: Modify for bug 990577
                if (mNoteItem == null && fromWidget == 1) {
                    startActivity(new Intent(NoteEditorActivity.this, NoteActivity.class));
                    finish();
                    return;
                }
                mTempNote= new NoteItem(mNoteItem);
                mOldItem = new NoteItem(mNoteItem);
                DLog.d(TAG, "mNoteItem.content ==== "+mNoteItem.getContent()+" : "+"mOldItem.content ==== "+mOldItem.getContent());
                folderId = mNoteItem.getParentFolderId();
                if (mNoteItem.isCollected()) {
                    isFavorite = true;
                } else {
                    isFavorite = false;
                }
            }
            mEditor_time.setText(mNoteItem.getDisplayDateTime(NoteEditorActivity.this, mNoteItem.getLongDate()));

            /*UNISOC: Modify for bug 1011301 {@*/
            if (mNoteSettingChangedListener == null) {
                mNoteSettingChangedListener = new NoteBackGroundSettings();
            }
            //UNISOC: Modify for bug 1235570
            mNoteItem.setNoteSettingChangedListener(mNoteSettingChangedListener);
            /*@}*/

            if (openType == TYPE_NEW_NOTE ) {
                getWindow().setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                                 | WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                /* SPRD: Modify for bug900052 {@ */
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(0, InputMethodManager.SHOW_FORCED);
                /* @} */
                mEditor.addFirstEditor();
                mNoteItem.setLongDate(System.currentTimeMillis());
                mNoteItem.setBackgroundId(BackgroundResource.sBgSelectorBtnsMap.get(R.id.iv_bg_white));
                if (filterType == 1) {
                    mNoteItem.setCollected(true);
                    isFavorite = true;
                }
                mNoteItem.setParentFolderId(filterType);
                mEditor.setDisplayTime(mNoteItem.getDisplayDateTime(NoteEditorActivity.this, System.currentTimeMillis()));
            } else {
                getWindow().setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
                                | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            }
            if (openType == TYPE_EDIT_NOTE) {
                int bgId = mNoteItem.getBackgroundId();
                mEditor.setContent(mNoteItem.getContent(), BackgroundResource.sBgDrawableMap.get(bgId == -1 ? 4 : bgId), mNoteItem.getDisplayDateTime(NoteEditorActivity.this, mNoteItem.getLongDate()), this.getString(R.string.selected_image_failed));
            }
        }
    }

    private void initViews() {
        mEditorLayout = (LinearLayout)findViewById(R.id.title);
        mEditor_content = (LinearLayout)findViewById(R.id.editor_content);
        mAgencyImg = (ImageView) findViewById(R.id.checkbox_img);
        mPictureImg = (ImageView) findViewById(R.id.picture_img);
        mCameraImg = (ImageView) findViewById(R.id.camera_img);
        mWallpaperImg = (ImageView) findViewById(R.id.wallpaper_img);
        mAgencyImg.setOnClickListener(mOperateListener);
        mPictureImg.setOnClickListener(mOperateListener);
        mCameraImg.setOnClickListener(mOperateListener);
        mWallpaperImg.setOnClickListener(mOperateListener);
        mEditor = (RichText) findViewById(R.id.richEditor);
        mEditor_time = (TextView)findViewById(R.id.editor_time);
        mNoteBgColorSelector = findViewById(R.id.note_bg_color_selector);

        for (int id : BackgroundResource.sBgSelectorBtnsMap.keySet()) {
            ImageView iv = (ImageView) findViewById(id);
            iv.setOnClickListener(mOperateListener);
        }
    }

    /*Add for bug743537: @{*/
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        DLog.d(TAG, "onNewIntent...."+intent);
        isOnNewIntent = true;
        fromWidget = intent.getIntExtra("from_widget", 0);
        folderId = intent.getIntExtra("FOLDER_ID", NoteDataManagerImpl.ID_ALLNOTE_FOLDER);
        filterType = intent.getIntExtra("FILTER_TYPE", FolderUtil.FILTER_ALL);
        openType = intent.getIntExtra(OPEN_TYPE, 0);
        int id = intent.getIntExtra(NoteEditorActivity.ID, -1);
        if (mToolbar != null) {
            mToolbar.dismissPopupMenus();
        }
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
        hideBgDrawableSelector();
        mEditor.removeContent();
        if (openType == TYPE_EDIT_NOTE && id != 0) {
            mNoteItem = mNoteDataManager.getNoteItem(id);
            /*UNISOC: Modify for bug 1235487 @{*/
            if (mNoteItem == null) {
                DLog.d(TAG, "onNewIntent mNoteItem == null id = "+id);
                return;
            }
            /*}@*/
            mTempNote = new NoteItem(mNoteItem);
            mOldItem = new NoteItem(mNoteItem);
            DLog.d(TAG, "newIntent mNoteItem.content ==== " + mNoteItem.getContent() + " : " + "mOldItem.content ==== " + mOldItem.getContent());
            folderId = mNoteItem.getParentFolderId();
            if (mNoteItem.isCollected()) {
                isFavorite = true;
            } else {
                isFavorite = false;
            }

            mEditor_time.setText(mNoteItem.getDisplayDateTime(NoteEditorActivity.this, mNoteItem.getLongDate()));
            //UNISOC: Modify for bug 1235570
            mNoteItem.setNoteSettingChangedListener(new NoteBackGroundSettings());

            getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
                            | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            int bgId = mNoteItem.getBackgroundId();
            mEditor.setContent(mNoteItem.getContent(), BackgroundResource.sBgDrawableMap.get(bgId), mNoteItem.getDisplayDateTime(NoteEditorActivity.this, mNoteItem.getLongDate()), this.getString(R.string.selected_image_failed));

        } else {
            mNoteItem = new NoteItem();
            mEditor_time.setText(mNoteItem.getDisplayDateTime(NoteEditorActivity.this, mNoteItem.getLongDate()));
            if (mNoteItem != null) {
                mNoteItem.setNoteSettingChangedListener(new NoteBackGroundSettings());
            }
            getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                            | WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            mEditor.addFirstEditor();
            mNoteItem.setLongDate(System.currentTimeMillis());
            mNoteItem.setBackgroundId(BackgroundResource.sBgSelectorBtnsMap.get(R.id.iv_bg_white));
            if (filterType == 1) {
                mNoteItem.setCollected(true);
                isFavorite = true;
            }
            mNoteItem.setParentFolderId(filterType);
            mEditor.setDisplayTime(mNoteItem.getDisplayDateTime(NoteEditorActivity.this, System.currentTimeMillis()));
        }
    }
    /*}@*/

    private void saveContent(boolean isShare) {
        hideKeyBoard();        //UNISOC: add for bug1144942
        DLog.d(TAG, "folderId ==============="+folderId+ "is Share === "+isShare);
        if (!isShare) {
            mEditor.saveContent(folderId, mNoteItem, true, this.getString(R.string.picture_title));
            mNoteItem.setCollected(isFavorite);
            if (filterType == FolderUtil.FILTER_PRIVATE) {
                mNoteItem.setPrivate(true);
            }

            if (mNoteItem.getContent() != null && mNoteItem.getContent().length() != 0) {
                if (openType == TYPE_NEW_NOTE) {
                    //UNISOC: Add for bug 1228989
                    openType = TYPE_EDIT_NOTE;
                    mNoteItem.setLongDate(System.currentTimeMillis());
                    int resultCode = mNoteDataManager.addNewNote(mNoteItem);
                    if (resultCode >= 0) {
                        NoteAppWidgetProvider.updateWidget(NoteEditorActivity.this);
                        Toast.makeText(NoteEditorActivity.this, R.string.save_success, Toast.LENGTH_SHORT).show();
                        /* UNISOC: add for bug1153963 {@ */
                        /*if (fromWidget == 1) {
                            startActivity(new Intent(NoteEditorActivity.this, NoteActivity.class));
                        }*/
                        /* }@ */
                    } else {
                        Toast.makeText(NoteEditorActivity.this, R.string.save_fail, Toast.LENGTH_SHORT).show();
                    }
                } else if (openType == TYPE_EDIT_NOTE) {
                    Log.d(TAG, "mOldItem =  " + mOldItem + "  :mNoteItem = " + mNoteItem);
                    boolean needUpdate = false;
                    if ((mOldItem.getContent() != null && !mOldItem.getContent().equals(mNoteItem.getContent()))
                            || mOldItem.getBackgroundId() != mNoteItem.getBackgroundId()) {
                        mNoteItem.setLongDate(System.currentTimeMillis());
                        needUpdate = true;
                    } else if (mOldItem.isCollected() != mNoteItem.isCollected()) {
                        needUpdate = true;
                    }
                    if (needUpdate) {
                        if (mNoteDataManager.updateNote(mNoteItem) > 0) {
                            NoteAppWidgetProvider.updateWidget(NoteEditorActivity.this);
                            Toast.makeText(NoteEditorActivity.this, R.string.update_sucess, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(NoteEditorActivity.this, R.string.save_fail, Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            } else {
                if (openType == TYPE_EDIT_NOTE) {
                    /* UNISOC: Modified for bug 942357 @{*/
                    mNoteDataManager.deleteNoteFromDeletedFolder(mNoteItem);
                    /* }*/
                    NoteAppWidgetProvider.updateWidget(this);
                    if (fromWidget == 1) {
                        startActivity(new Intent(NoteEditorActivity.this, NoteActivity.class));
                    }
                    finish();
                }
            }
        } else {
            mEditor.saveContent(folderId, mTempNote, false, this.getString(R.string.picture_title));
            mTempNote.setCollected(isFavorite);
            if (filterType == FolderUtil.FILTER_PRIVATE) {
                mTempNote.setPrivate(true);
            }
        }
    }

    /*Add for bug711375 :start*/
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        /*Modified for 716352: start*/
        NoteItem temp = mEditor.saveContent(folderId, new NoteItem(), false, this.getString(R.string.picture_title));
        if (temp != null){
            outState.putString("now_content", temp.getContent());
        } else {
            outState.putString("now_content", "");
        }
         /*Modified for 716352: end*/
        outState.putInt("open_type", openType);
        outState.putBoolean("isSaveState", true);
        outState.putLong("saveTime", mNoteItem.getLongDate());
        outState.putInt("bg", mNoteItem.getBackgroundId());
        outState.putInt("id",mNoteItem.getId());
        outState.putBoolean("isCollected", mNoteItem.isCollected());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mTemporaryContent = savedInstanceState.getString("now_content");
        saveType = savedInstanceState.getInt("open_type");
        isSaveState = savedInstanceState.getBoolean("isSaveState");
        saveTime = savedInstanceState.getLong("saveTime");
        saveBgId = savedInstanceState.getInt("bg");
        isFavorite = savedInstanceState.getBoolean("isCollected");
        /*Add for 714730, 1235487: start*/
        if (savedInstanceState.getInt("id") != -1 && mNoteDataManager.getNoteItem(savedInstanceState.getInt("id")) != null) {
            mNoteItem = mNoteDataManager.getNoteItem(savedInstanceState.getInt("id"));
            mOldItem = new NoteItem(mNoteItem);

            /*UNISOC: Add for bug 1011301 {@ */
            mNoteItem.setNoteSettingChangedListener(mNoteSettingChangedListener);
            /* @} */
        }
        /*Add for 714730: end*/
        /*Modified for bug 732556: start*/
        if (saveType == TYPE_EDIT_NOTE || mTemporaryContent.length() != 0) {
            mEditor.removeContent();
        }
        /*Modified for bug 732556: end*/
        DLog.d(TAG, "onRestore......saveType = "+saveType+": isSaveState = "+isSaveState+" : saveBgId = "+saveBgId+ ": content = "+mTemporaryContent);
    }
    /*Add for bug711375 :end*/

    @Override
    protected void onResume(){
        super.onResume();
        DLog.d(TAG, "onResume.....................");
        //UNISOC: Modified for bug 1199503(Remove invalid code), 1235503
        if (mNoteItem.getBackgroundId() != -1) {
            DLog.d(TAG, "mNoteItem.getBackgroundId() === "+mNoteItem.getBackgroundId()+": ");
            mEditor.setBackgroundResource(BackgroundResource.sBgDrawableMap.get(mNoteItem.getBackgroundId()));

          //  mEditorLayout.setBackgroundResource(BackgroundResource.sBgDrawableMap.get(mNoteItem.getBackgroundId()));
        } else {
            mEditor.setBackgroundResource(BackgroundResource.sBgDrawableMap.get(4));
          //  mEditorLayout.setBackgroundResource(BackgroundResource.sBgDrawableMap.get(4));
        }
        /*Add for bug711375& 714730  & 855605 :start*/
        if (isSaveState && !isOnNewIntent) {
            DLog.d(TAG, "opentype == "+openType);
            isSaveState = false;
            openType = saveType;
            mEditor.setContent(mTemporaryContent, BackgroundResource.sBgDrawableMap.get(saveBgId == -1 ? 4 : saveBgId),
                    mNoteItem.getDisplayDateTime(NoteEditorActivity.this, saveTime), this.getString(R.string.selected_image_failed));
            getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
                            | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        /*Add for bug711375 & 714730 :end*/
        registerReceiver(mHomeKeyEventReceiver,
                new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
    }

    private boolean hideBgDrawableSelector() {
        if (mNoteBgColorSelector.getVisibility() == View.VISIBLE) {
            mNoteBgColorSelector.setVisibility(View.GONE);
            return true;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        if (hideBgDrawableSelector()) {
            return;
        } else {
            saveContent(false);
        }
        /*Add for bug 743537: @{*/
        if (fromWidget == 1 || isOnNewIntent) {
            startActivity(new Intent(NoteEditorActivity.this, NoteActivity.class));
            finish();
        }
        /*}@*/
        super.onBackPressed();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        MenuItem favoriteMenu = menu.getItem(0);
        MenuItem cancelMenu = menu.getItem(1);
        MenuItem deleteMenu = menu.getItem(2);
        if (openType == TYPE_EDIT_NOTE) {
            deleteMenu.setVisible(true);
            cancelMenu.setVisible(true);
        } else {
            deleteMenu.setVisible(true);
            cancelMenu.setVisible(false);
        }
        if (filterType == FolderUtil.FILTER_PRIVATE) {
            favoriteMenu.setVisible(false);
        } else {
            if (isFavorite) {
                favoriteMenu.setIcon(R.drawable.btn_star_on_normal_holo_light);
            } else {
                favoriteMenu.setIcon(R.drawable.btn_star_off_normal_holo_light);
            }
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.action_favorite:
                if (filterType != FolderUtil.FILTER_FAVORITE && openType == TYPE_NEW_NOTE || openType == TYPE_EDIT_NOTE) {
                    if (isFavorite) {
                        item.setIcon(R.drawable.btn_star_off_normal_holo_light);
                        isFavorite = false;
                    } else {
                        isFavorite = true;
                        item.setIcon(R.drawable.btn_star_on_normal_holo_light);
                    }
                } else {
                    Toast.makeText(this, R.string.create_tip_in_favorite, Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.action_cancel:
                saveContent(true);
                /*Modified for bug 729929: start*/
                if ((mOldItem.getContent() != null && !mOldItem.getContent().equals(mTempNote.getContent()))
                    || mOldItem.getBackgroundId() != mTempNote.getBackgroundId()
                        || mOldItem.isCollected() != mTempNote.isCollected()){
                /*Modified for bug 729929: end*/
                    mDialogBuilder = new AlertDialog.Builder(this)
                            .setTitle(R.string.prompt)
                            .setMessage(R.string.cancel_tip)
                            .setNegativeButton(R.string.Cancel, null)
                            .setPositiveButton(R.string.Ok, new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog, int which) {
                                    Log.d(TAG, "cancel..................."+fromWidget);
                                    if (fromWidget == 1) {
                                        startActivity(new Intent(NoteEditorActivity.this, NoteActivity.class));
                                    }
                                    hideKeyBoard();
                                    finish();
                                }
                            });

                            if (!isFinishing()) {
                                mDialog = mDialogBuilder.show();
                            }
                } else {
                    if (fromWidget == 1) {
                        startActivity(new Intent(NoteEditorActivity.this, NoteActivity.class));
                    }
                    hideKeyBoard();
                    finish();
                }
                break;
            case R.id.action_del:
                if (!(openType == TYPE_NEW_NOTE)) {
                    mDialogBuilder = new AlertDialog.Builder(this)
                            .setTitle(R.string.prompt)
                            .setMessage(R.string.delete_selected_item)
                            .setNegativeButton(R.string.Cancel, null)
                            .setPositiveButton(R.string.Ok, new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog, int which) {
                                    if (filterType == FolderUtil.FILTER_PRIVATE || filterType == FolderUtil.FILTER_DELETE) {
                                        mNoteDataManager.deleteNoteFromDeletedFolder(mNoteItem);
                                    } else {
                                        mNoteDataManager.deleteNoteIntoDeletedFolder(mNoteItem);
                                    }
                                    NoteAppWidgetProvider.updateWidget(NoteEditorActivity.this);
                                    if (fromWidget == 1) {
                                        startActivity(new Intent(NoteEditorActivity.this, NoteActivity.class));
                                    }
                                    finish();
                                }
                            });
                           if (!isFinishing()) {
                               mDialog = mDialogBuilder.show();
                           }
                } else {
                    mDialogBuilder = new AlertDialog.Builder(this)
                            .setTitle(R.string.prompt)
                            .setMessage(R.string.delete_selected_item)
                            .setNegativeButton(R.string.Cancel, null)
                            .setPositiveButton(R.string.Ok, new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog, int which) {
                                    /*Add for 727805: start*/
                                    if(openType == TYPE_EDIT_NOTE) {
                                        if (filterType == FolderUtil.FILTER_PRIVATE || filterType == FolderUtil.FILTER_DELETE) {
                                            mNoteDataManager.deleteNoteFromDeletedFolder(mNoteItem);
                                        } else {
                                            mNoteDataManager.deleteNoteIntoDeletedFolder(mNoteItem);
                                        }
                                        NoteAppWidgetProvider.updateWidget(NoteEditorActivity.this);
                                    }
                                    /*Add for 727805: end*/
                                    if (fromWidget == 1) {
                                        startActivity(new Intent(NoteEditorActivity.this, NoteActivity.class));
                                    }
                                    finish();
                                }
                            });

                            if (!isFinishing()) {
                                mDialog = mDialogBuilder.show();
                            }
                }
                break;
            case R.id.action_text_share:
               /* if (openType == TYPE_NEW_NOTE) {*/
                saveContent(true);
               // }
                sendSharedIntent(this, mTempNote, "text/plain", null);
                break;
            case R.id.action_picture_share:
                //if (openType == TYPE_NEW_NOTE) {
                saveContent(true);
               // }
                mImage = savePicture();
                break;
           /* case R.id.action_send_desktop:
                Toast.makeText(this, "...", Toast.LENGTH_SHORT).show();
                break;*/

        }
        return super.onOptionsItemSelected(item);
    }

    protected void openCamera() {
        try {
            // Launch camera to take photo for selected contact
            mCurrentPhotoFile = new File(PHOTO_DIR, getPhotoFileName());
            final Intent intent = getTakePickIntent(mCurrentPhotoFile);
            startActivityForResult(intent, REQUEST_CODE_CAPTURE_CAMEIA);
        }catch (SecurityException se) {
            Toast.makeText(this, R.string.permission_tip, Toast.LENGTH_SHORT).show();
        }catch (ActivityNotFoundException e) {
            Log.d(TAG, "exception = "+e);
        }
    }

    public  Intent getTakePickIntent(File f) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE, null);
        //For Android N in strictMode api.
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            Uri imgUri = FileProvider.getUriForFile(this,
                    "com.sprd.sprdnote"+ ".provider", f);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imgUri);
        } else {
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
        }

        return intent;
    }

    private String getPhotoFileName() {
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "'IMG'_yyyy-MM-dd HH:mm:ss");
        return dateFormat.format(date) + ".jpg";
    }

    private void sendSharedIntent(Context context, NoteItem item, String shareType, File image) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(shareType);
        StringBuilder sb = new StringBuilder();
        if (shareType.equals("text/plain")) {
            String titleKey = context.getString(R.string.share_title_key);
            String contentKey = context.getString(R.string.share_content_key);
            if (item != null && item.getContent()!=null
                    && item.getContent().length()!= 0) {
                String content = parseContent(item.getContent());
                if (content != null && content.length() != 0) {
                    sb.append(titleKey).append(item.getTitle()).append(',');
                    sb.append(contentKey).append(content);
                }
            } else {
                Toast.makeText(NoteEditorActivity.this, R.string.empty_conent_tip, Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            if (image != null && image.exists() && image.isFile()) {
                intent.setType("image/jpg");
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                    intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(image));
                } else {
                    Uri uri = FileProvider.getUriForFile(this,
                            "com.sprd.sprdnote.provider", image);
                    intent.putExtra(Intent.EXTRA_STREAM, uri);
                }

            }
        }
        intent.putExtra(Intent.EXTRA_TEXT, sb.toString());
        intent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share));
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share) + ":"
                + item.getTitle()));
    }

    /**
     * It's used to save note as picture while we want to share by as picture.
     */
    private File savePicture() {
        int captureHeight = 0;
        /*UNISOC: Modified for bug 1199503(Remove invalid code) @{*/
        /*UNISOC:Modified for 732734 @{*/
        if (mTempNote != null && mTempNote.getContent() != null && mTempNote.getContent().length() != 0) {
            mEditor.getContent(this.getString(R.string.todo_shared));//We will remove it.
            captureHeight = mEditor.getRichTextHeight();
            DLog.d(TAG, "captureHight = = " + captureHeight);
            if (captureHeight == 0) {
                return null;
            }
        /*}@*/
        } else {
            Toast.makeText(NoteEditorActivity.this, R.string.empty_conent_tip, Toast.LENGTH_SHORT).show();
            return null;
        }
        /*}@*/
        File folder = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/.NoteBook");
        if (!folder.exists()) {
            //UNISOC: Modify for bug 1235479
            if (!folder.mkdir()) {
                DLog.e(TAG, folder + " mkdir is failed.");
                return null;
            }
        }
        NoteUtils.deleteDirOrFile(folder);
        setCursorVisibility(false);

        /*SPRD: modify by bug916643 { @*/
        /* UNISOC: modify by bug1154900 {@ */
        Bitmap bmp = null;
        if (0 >= mGlMaxTextureSize) {
            //Actually read the Gl_MAX_TEXTURE_SIZE into the array.
            mGlMaxTextureSize = computeEglMaxTextureSize();
        }
        DLog.d(TAG,"captureWidth = "+mEditor.getWidth() +", captureHeight = "+captureHeight+", mGlMaxTextureSize = "+mGlMaxTextureSize);
        if (captureHeight < 10 * mGlMaxTextureSize) {
            bmp = Bitmap.createBitmap(mEditor.getWidth(), captureHeight, Bitmap.Config.ARGB_8888);
            mEditor.draw(new Canvas(bmp));
        } else {
            Toast.makeText(NoteEditorActivity.this, R.string.shared_pic_large, Toast.LENGTH_SHORT).show();
            return null;
        }
        /* @} */
        if (mGlMaxTextureSize > 0 && captureHeight > mGlMaxTextureSize) {
            int newWidth = mEditor.getWidth() * mGlMaxTextureSize / captureHeight;
            int newHeight = mGlMaxTextureSize;
            bmp = zoomImg(bmp,newWidth,newHeight);
            DLog.d(TAG,"zoomImg width = "+bmp.getWidth()+", height = "+bmp.getHeight());
        }
        /* @} */

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        String time = dateFormat.format(new Date());

        final String photoUrl =  "notebook_"+time+".png";
        final File file = new File(folder, photoUrl);
        /*Modified for bug 716352:start*/
        SavePictureTask task = new SavePictureTask();
        task.setFile(file);
        task.execute(bmp);
        /*Modified for bug 716352:end*/
        return file;
    }

    /*SPRD: add for bug916643 {@ */
    /**
     * Ridiculous way to read the devices maximum texture size because no other
     * way is provided.
     */
    private int computeEglMaxTextureSize() {
        EGLDisplay eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        int[] majorMinor = new int[2];
        EGL14.eglInitialize(eglDisplay, majorMinor, 0, majorMinor, 1);

        int[] configAttr = {
                EGL14.EGL_COLOR_BUFFER_TYPE, EGL14.EGL_RGB_BUFFER,
                EGL14.EGL_LEVEL, 0,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                EGL14.EGL_NONE
        };
        EGLConfig[] eglConfigs = new EGLConfig[1];
        int[] configCount = new int[1];
        EGL14.eglChooseConfig(eglDisplay, configAttr, 0,
                eglConfigs, 0, 1, configCount, 0);

        if (configCount[0] == 0) {
            DLog.d(TAG, "computeEglMaxTextureSize() -> No EGL configurations found!");
            return 0;
        }
        EGLConfig eglConfig = eglConfigs[0];

        // Create a tiny surface
        int[] eglSurfaceAttributes = {
                EGL14.EGL_WIDTH, 64,
                EGL14.EGL_HEIGHT, 64,
                EGL14.EGL_NONE
        };
        //
        EGLSurface eglSurface = EGL14.eglCreatePbufferSurface(eglDisplay, eglConfig,
                eglSurfaceAttributes, 0);

        int[] eglContextAttributes = {
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
        };

        // Create an EGL context.
        EGLContext eglContext = EGL14.eglCreateContext(eglDisplay, eglConfig, EGL14.EGL_NO_CONTEXT,
                eglContextAttributes, 0);
        EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext);

        // Actually read the Gl_MAX_TEXTURE_SIZE into the array.
        int[] maxSize = new int[1];
        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, maxSize, 0);
        int result = maxSize[0];

        // Tear down the surface, context, and display.
        EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_CONTEXT);
        EGL14.eglDestroySurface(eglDisplay, eglSurface);
        EGL14.eglDestroyContext(eglDisplay, eglContext);
        EGL14.eglTerminate(eglDisplay);

        // Return the computed max size.
        return result;
    }

    private Bitmap zoomImg(Bitmap bm, int newWidth ,int newHeight){
        // get the picture's width.
        int width = bm.getWidth();
        int height = bm.getHeight();
        // compute the zoom rate.
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // get the parameter of the matrix
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        // get the new picture.
        Bitmap newbm = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true);
        //UNISOC: Modify for bug 1235570
        if (!bm.isRecycled()) {
            bm.recycle();
            bm = null;
        }
        return newbm;
    }
    /* @} */

    private String parseContent(String content) {
        Document doc = NoteUtils.string2Doc(content);
        if (doc == null) {
            return null;
        }
        Element rootElement = doc.getDocumentElement();
        NodeList list = rootElement.getChildNodes();
        /*UNISOC: Modify for bug 1235573 @{*/
        StringBuffer sharedString = new StringBuffer();
        for (int i = 0; i < list.getLength(); i++) {
            Node n = list.item(i);
            if (n.getNodeName().equals("notetxt")) {
                sharedString.append(n.getTextContent()+"\n");
            } else if (n.getNodeName().equals("noteimg")) {
                sharedString.append("[" + NoteEditorActivity.this.getString(R.string.content_image) + "]\n");
            } else if (n.getNodeName().equals("notetodo")) {
                sharedString.append(". " + n.getTextContent()+"\n");
            }
        }
        if (sharedString.length() != 0) {
            return sharedString.toString();
        }
        /*}@*/
        return null;
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy..");
        super.onDestroy();
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause..");
        super.onPause();
        unregisterReceiver(mHomeKeyEventReceiver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK || isSaveState) {
            return;
        }
        if (requestCode == REQUEST_CODE_PICK_IMAGE) {
            Uri uri = data.getData();
            DLog.d(TAG, "the uri =  "+uri+"getRealFilePath(uri) = ="+getRealFilePath(uri));
            insertBitmap(getRealFilePath(uri));
        } else if (requestCode == REQUEST_CODE_CAPTURE_CAMEIA) {
            if (mCurrentPhotoFile != null){
                insertBitmap(mCurrentPhotoFile.getAbsolutePath());
            } else {
                Toast.makeText(this, R.string.get_picture_failed, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void insertBitmap(String imagePath) {
        mEditor.insertImage(imagePath, this.getString(R.string.selected_image_failed));
    }

    public String getRealFilePath(final Uri uri) {
        if (null == uri) {
            return null;
        }

        final String scheme = uri.getScheme();
        String data = null;
        if (scheme == null) {
            data = uri.getPath();
        } else if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            data = uri.getPath();
        } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            Cursor cursor = getContentResolver().query(uri,
                    new String[] { MediaStore.Images.ImageColumns.DATA }, null, null, null);
            if (null != cursor) {
                if (cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                    if (index > -1) {
                        data = cursor.getString(index);
                    }
                }
                cursor.close();
            }
        }
        return data;
    }

    /*Add for bug:716352: start*/
    private void showProgressDialog() {

        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(NoteEditorActivity.this);
            mProgressDialog.setTitle(R.string.shared_picture_tip);
            mProgressDialog.setMessage(getResources().getString(R.string.create_picture));
            mProgressDialog.setCanceledOnTouchOutside(false);
        }
        if (!isFinishing()) {
            mProgressDialog.show();
        }
    }

    private void dismissProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    /** Used change cursor visible in save as picture.
     * @param bool
     */
    private void setCursorVisibility(boolean bool) {
        CustomEditText lastEdit = mEditor.getLastFocusEdit();
        if (lastEdit != null) {
            if (bool) {
                lastEdit.setCursorVisible(true);
            } else {
                lastEdit.setCursorVisible(false);
            }
        }
    }

    private boolean isSupportImage() {
        boolean isSupport = true;
        View focusView = getWindow().getCurrentFocus();
        if (focusView != null && (focusView instanceof LinearLayout &&
                (int)focusView.getTag() == RichText.NORMAL_EDITTEXT || focusView instanceof  CustomEditText &&
                (int)((LinearLayout)focusView.getParent()).getTag() == RichText.NORMAL_EDITTEXT)
                || (focusView instanceof  LinearLayout && (int)focusView.getTag() == RichText.TODO_EDITTEXT || focusView instanceof  CustomEditText &&
                (int)((LinearLayout)focusView.getParent()).getTag() == RichText.TODO_EDITTEXT)) {
            isSupport = true;
        } else {
            isSupport = false;
        }
        return isSupport;
    }

    private void hideKeyBoard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mEditor.getWindowToken(), 0);
    }

    class SavePictureTask extends AsyncTask<Bitmap, String, String> {

        File file;
        boolean bitMapOk;
        public void setFile(File f) {
            file = f;
        }

        /**
         * Before starting background thread Show Progress Dialog
         * */
        @Override
        protected void onPreExecute() {
            showProgressDialog();
        }

        /**
         * Save picture
         * */
        protected String doInBackground(Bitmap... params)
        {
            /* UNISOC: add for bug1179177 {@ */
            FileOutputStream fs = null;
            try {
                if (file != null) {
                    fs = new FileOutputStream(file);
                    bitMapOk = params[0].compress(Bitmap.CompressFormat.PNG, 100, fs);
                }
            } catch (FileNotFoundException e){
                e.printStackTrace();
            } finally {
                if (fs != null) {
                    try {
                        fs.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            /* @} */
            }
            return null;
        }

        /**
         * After completing background task Dismiss the progress dialog and
         * **/
        protected void onPostExecute(String file_url) {
            if (NoteEditorActivity.this.isDestroyed()) {
                return;
            }
            if (bitMapOk) {
                setCursorVisibility(true);
                dismissProgressDialog();
                sendSharedIntent(NoteEditorActivity.this, mTempNote, "image/jpg", mImage);
            } else {
                Toast.makeText(NoteEditorActivity.this, R.string.save_picture_failed, Toast.LENGTH_SHORT).show();
                setCursorVisibility(true);
            }
        }
    }
    /*Add for bug 716352: end*/

    class NoteBackGroundSettings implements NoteItem.NoteSettingChangedListener{
        @Override
        public void onBackgroundColorChanged() {
            Log.d(TAG, "mNoteItem.getBackgournd == "+mNoteItem.getBackgroundId());
            if (mNoteItem.getBackgroundId() != -1) {
               /* findViewById(BackgroundResource.sBgSelectorSelectionMap.get(mNoteItem.getBackgroundId())).setVisibility(
                        View.VISIBLE);*/
            /*mEditor.setBackgroundColor(getResources().getColor(sBgColorMap.get(mNoteItem.getBackgroundId()), null));
            mEditorLayout.setBackgroundColor(getResources().getColor(sBgColorMap.get(mNoteItem.getBackgroundId()), null));*/
                mEditor.setBackgroundResource(BackgroundResource.sBgDrawableMap.get(mNoteItem.getBackgroundId()));
             //   mEditorLayout.setBackgroundResource(BackgroundResource.sBgDrawableMap.get(mNoteItem.getBackgroundId()));
            }
        }

        @Override
        public void onClockAlertChanged(long date, boolean set) {

        }

        @Override
        public void onWidgetChanged() {

        }

        @Override
        public void onCheckListModeChanged(int oldMode, int newMode) {

        }
    }

    private BroadcastReceiver mHomeKeyEventReceiver = new BroadcastReceiver() {
        String SYSTEM_REASON = "reason";
        String SYSTEM_HOME_KEY = "homekey";
        String SYSTEM_HOME_KEY_LONG = "recentapps";

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
                String reason = intent.getStringExtra(SYSTEM_REASON);
                if (TextUtils.equals(reason, SYSTEM_HOME_KEY)
                        || TextUtils.equals(reason, SYSTEM_HOME_KEY_LONG)) {
                    saveContent(false);
                    /*Modified for bug712898: start*/
                    if (openType == TYPE_NEW_NOTE && mNoteItem.getContent() != null
                      && mNoteItem.getContent().length() != 0) {
                        openType = TYPE_EDIT_NOTE;
                        mOldItem = new NoteItem(mNoteItem);
                    } else {/*Add for bug723548: start*/
                        if (mNoteItem != null && mNoteItem.getContent() != null
                                && mNoteItem.getContent().length() != 0) {
                            mOldItem = new NoteItem(mNoteItem);
                        }
                        /*Add for bug723548: end*/
                    }
                    /*Modified for bug712898: end*/
                }
            }
        }
    };
}
