package com.sprd.sprdnote;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.app.AlertDialog;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.inputmethod.InputMethodManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;
import android.util.Log;

import com.sprd.sprdnote.data.FolderItem;
import com.sprd.sprdnote.data.NoteDataManagerImpl;
import com.sprd.sprdnote.folder.FolderPreference;
import com.sprd.sprdnote.folder.FolderUtil;
import java.lang.reflect.Field;
import java.util.List;

import com.sprd.sprdnote.R;

public class NoteFolderListActivity extends PreferenceActivity implements Preference.OnPreferenceClickListener{

    private static final String KEY_ALL = "all";
    private static final String KEY_PRIVATE = "favorite";
    private static final String KEY_FAVORITE = "private";
    private static final String KEY_DELETE = "delete";
    private static final String KEY_DEFAULT = "default";
    private static final String KEY_CUSTOM = "custom";
    private static final int MENU_ADD_FOLDER  = 0;
    private static final int MENU_RENAME  = 1;
    private static final int MENU_DELETE  = 2;
    /* UNISOC: Add for bug 1228989 & Default folder order @{ */
    private static final int ORDER_ALL = -1;
    private static final int ORDER_FAVORITE = -2;
    private static final int ORDER_PRIVATE = -3;
    private static final int ORDER_DELETE = -4;
    /* }@ */

    private FolderPreference mAll;
    private FolderPreference mFavorite;
    private FolderPreference mPrivate;
    private FolderPreference mDelete;
    private PreferenceCategory mCustom;
    private PreferenceCategory mDefault;
    private ImageButton addFolder;
    private int selectId;
    private Toolbar toolbar;
    private NoteDataManagerImpl mNoteDataManager;
    private ListView list;
    private AlertDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.folder_list);
        addPreferencesFromResource(R.xml.activity_note_folder_list);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.folder);
        toolbar.setNavigationIcon(R.drawable.ic_ab_back_holo_light_up);    //Unisoc:Modify for Bug1164437
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
                // startActivity(new Intent(NoteActivity.this, TestActivity.class));
            }
        });
        //setSupportActionBar(toolbar);
        //getActionBar().setDisplayHomeAsUpEnabled(true);
        //getActionBar().setHomeButtonEnabled(true);
        mCustom = (PreferenceCategory)findPreference(KEY_CUSTOM);
        mDefault = (PreferenceCategory)findPreference(KEY_DEFAULT);
        mNoteDataManager = NoteDataManagerImpl.getInstance(this);
        loadDefaultPreference();
        //loadCustomPreference();
        list = getListView();
        list.setOnCreateContextMenuListener(mCustomMenu);
        addFolder = (ImageButton) findViewById(R.id.addFolder);
        addFolder.setImageResource(R.drawable.folder_new_sprd);
        addFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog = buildEditNameDialog(MENU_ADD_FOLDER);
                dialog.show();
                final Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                if (positive != null) positive.setEnabled(false);
            }
        });
    }

    View.OnCreateContextMenuListener mCustomMenu = new View.OnCreateContextMenuListener() {
        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            AdapterView.AdapterContextMenuInfo info;
            try {
                info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            } catch (ClassCastException e){
                return ;
            }
            final int position = info.position;
            Object obj = list.getItemAtPosition(position);
            if (obj != null && obj instanceof Preference) {
                Preference p = (Preference)obj;
                CharSequence key  = p.getKey();
                selectId = p.getOrder();
                switch(key.toString()) {
                    case KEY_ALL:
                    case KEY_PRIVATE:
                    case KEY_FAVORITE:
                    case KEY_DELETE:
                        break;
                    default:
                        menu.add(0, MENU_RENAME, 0, R.string.rename).setOnMenuItemClickListener(mMenuClickListener);
                        menu.add(0, MENU_DELETE, 0, R.string.delete).setOnMenuItemClickListener(mMenuClickListener);
                }
                return;
            }
        }
    };

    public void loadCustomPreference(){
        mCustom.removeAll();
        List<FolderItem> mFolders =  mNoteDataManager.getAllValidFolders();
        for(FolderItem folder:mFolders) {
            addHorizontalPreference(mCustom, folder.getName(), folder.getId());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateFolderSize();
        loadCustomPreference();
    }

    /*SPRD: modified for 762198 @{*/
    @Override
    protected void onStop() {
        super.onStop();
    }
    /*}@*/

    public void updateFolderSize(){
        updateDefaultFolderSize();
        //updateCustomFolderSize();
    }
    public void updateDefaultFolderSize(){
        mAll.setSize(mNoteDataManager.getNotesForPresetFolder(NoteDataManagerImpl.ALL_NOTE_FOLDER).size());
        mFavorite.setSize(mNoteDataManager.getNotesForPresetFolder(NoteDataManagerImpl.COLLECTED_FOLDER).size());
        mPrivate.setSize(mNoteDataManager.getNotesForPresetFolder(NoteDataManagerImpl.PRIVATE_FOLDER).size());
        mDelete.setSize(mNoteDataManager.getNotesForPresetFolder(NoteDataManagerImpl.DELETED_NOTE_FOLDER).size());
    }
    public void updateCustomFolderSize(){
        List<FolderItem> mFolders =  mNoteDataManager.getAllValidFolders();
        for(FolderItem folder:mFolders) {
            FolderPreference p = (FolderPreference)mCustom.findPreference(folder.getName());
            if (p != null) p.setSize(mNoteDataManager.getNotesForCustomFolder(folder.getId()).size());
        }
    }
    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mAll) {
            startActivityForFilter(preference.getTitle(), FolderUtil.FILTER_ALL, NoteDataManagerImpl.ID_ALLNOTE_FOLDER);
        } else if (preference == mFavorite){
            startActivityForFilter(preference.getTitle(), FolderUtil.FILTER_FAVORITE, NoteDataManagerImpl.ID_COLLECT_FOLDER);
        } else if (preference == mPrivate) {
            startActivityForFilter(preference.getTitle(), FolderUtil.FILTER_PRIVATE, NoteDataManagerImpl.ID_PRIVATE_FOLDER);
        } else if (preference == mDelete) {
            startActivityForFilter(preference.getTitle(), FolderUtil.FILTER_DELETE, 0);
        } else {
            startActivityForFilter(preference.getTitle(), FolderUtil.FILTER_CUSTOM, preference.getOrder());
        }
        return true;
    }

    private MenuItem.OnMenuItemClickListener mMenuClickListener = new MenuItem.OnMenuItemClickListener(){
        public boolean onMenuItemClick(MenuItem item){
            if (item.getItemId() == MENU_DELETE) {
                dialog = buildDeleteFolderDialog();
                dialog.show();
            } else {
                dialog = buildEditNameDialog(item.getItemId());
                dialog.show();
                if (item.getItemId() == MENU_ADD_FOLDER) {
                    final Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                    if (positive != null) positive.setEnabled(false);
                }
            }
            return  true;
        }
    };

    private AlertDialog buildDeleteFolderDialog() {
        final int[] type = new int[1];
        final AlertDialog dialog = new AlertDialog.Builder(NoteFolderListActivity.this)
                    .setTitle(R.string.delete)
                    .setSingleChoiceItems(R.array.delete_folder_entries, 0, new DialogInterface.OnClickListener() {
                          @Override
                          public void onClick(DialogInterface dialog, int which) {
                              switch (which) {
                                  case 0:
                                      type[0] = NoteDataManagerImpl.TYPE_DELETE_FOLDER_ONLY;
                                      break;
                                  case 1:
                                      type[0] = NoteDataManagerImpl.TYPE_DELETE_FOLDER_AND_NOTES;
                                      break;
                              }
                          }
                    })
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                                //UNISOC: Modify for bug 1235487
                                if (mNoteDataManager.getFolderItem(selectId) == null) {
                                    return;
                                }

                                CharSequence title = mNoteDataManager.getFolderItem(selectId).getName();
                                deletePreference(mCustom, title);
                                mNoteDataManager.deleteFolderIntoDeletedFolder(selectId, type[0]);
                                dialog.dismiss();
                                updateFolderSize();
                            }
                        })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .create();
        return dialog;
    }

    private AlertDialog buildEditNameDialog(final int type) {
        View nameView = LayoutInflater.from(NoteFolderListActivity.this).inflate(R.layout.edit_name, null);
        final EditText newName = (EditText)nameView.findViewById(R.id.name_editor);
        AlertDialog.Builder builder = new AlertDialog.Builder(NoteFolderListActivity.this);
        builder.setPositiveButton(android.R.string.ok, null)
               .setNegativeButton(android.R.string.cancel, null);
        final AlertDialog alertDialog = builder.create();
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {

                Button positionButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                Button negativeButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);

                positionButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String name = newName.getText().toString().trim();
                        if (name == null || name.equals("")) {
                            showMsg(R.string.msg_folder_input_empty, NoteFolderListActivity.this);
                            return;
                        }
                        // write to database
                        if (type == MENU_ADD_FOLDER) {
                            long data = System.currentTimeMillis();
                            FolderItem mFolder = new FolderItem(name, data, false);
                            int returnCode = mNoteDataManager.addNewFolder(mFolder);
                            switch (returnCode) {
                                case NoteDataManagerImpl.FAIL_FOLDER_CONFLICT:
                                    showMsg(R.string.name_conflict, NoteFolderListActivity.this);
                                    break;
                                case NoteDataManagerImpl.FAIL_OPERATE_DB:
                                    showMsg(R.string.add_fail, NoteFolderListActivity.this);
                                    break;
                                default:
                                    addHorizontalPreference(mCustom, name, returnCode);
                                    dialog.dismiss();
                            }
                        } else if (type == MENU_RENAME) {
                            FolderItem mFolderItem = mNoteDataManager.getFolderItem(selectId);
                            /*UNISOC: Modify for bug 1235487 @{*/
                            if (mFolderItem == null) {
                                return ;
                            }
                            /*}@*/
                            String originName = mFolderItem.getName();
                            if (name.toString().equals(mFolderItem.getName())) {
                                alertDialog.dismiss();
                            } else {
                                mFolderItem.setName(name);
                                int renameReturnCode = mNoteDataManager.updateFolder(mFolderItem);
                                switch (renameReturnCode) {
                                    case NoteDataManagerImpl.FAIL_FOLDER_CONFLICT:
                                        showMsg(R.string.name_conflict, NoteFolderListActivity.this);
                                        mFolderItem.setName(originName);
                                        break;
                                    case NoteDataManagerImpl.FAIL_SQLITE_FULL:
                                    case NoteDataManagerImpl.FAIL_OPERATE_DB:
                                        showMsg(R.string.add_fail, NoteFolderListActivity.this);
                                        mFolderItem.setName(originName);
                                        break;
                                    default:
                                        updateHorizontalPreference(mCustom, name, originName);
                                        alertDialog.dismiss();
                                }
                            }
                        }
                    }
                });
                negativeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        alertDialog.dismiss();
                    }
                });
            }
        });
        if (type == MENU_ADD_FOLDER) {
            newName.setHint(NoteFolderListActivity.this.getResources().getString(R.string.folder_name));
            alertDialog.setTitle(R.string.add_folder);
            newName.requestFocus();
        } else if (type == MENU_RENAME) {
            alertDialog.setTitle(NoteFolderListActivity.this.getResources().getString(R.string.rename));
            newName.setText(mNoteDataManager.getFolderItem(selectId).getName());
            newName.requestFocus();
        }
        alertDialog.setView(nameView);
        alertDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if ((keyCode == KeyEvent.KEYCODE_BACK) && (event.getAction() == KeyEvent.ACTION_UP)) {
                    alertDialog.dismiss();
                }
                return false;
            }
        });

        newName.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String str = s.toString().trim();
                final Button positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                if (positiveButton != null) {
                    if (str.length() <= 0) {
                        positiveButton.setEnabled(false);
                    } else {
                        positiveButton.setEnabled(true);
                    }
                }
                /* SPRD: modified for bug900014 @{ */
                final int lengthLimit = getResources().getInteger(R.integer.limit_folder_note);
                if (lengthLimit == s.toString().length()) {
                    Toast.makeText(NoteFolderListActivity.this, R.string.input_limit, Toast.LENGTH_SHORT).show();
                }
                /* @} */
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }

        });
        alertDialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        return alertDialog;
    }

    /* UNISOC: Modify for bug 1228989 & Default folder order @{ */
    public void loadDefaultPreference(){
        //all notes
        mAll = addDefaultPreference(mDefault, KEY_ALL, getString(R.string.all_notes), R.drawable.ic_folderlist_all, NoteDataManagerImpl.ALL_NOTE_FOLDER, ORDER_ALL);
        mFavorite = addDefaultPreference(mDefault, KEY_FAVORITE, getString(R.string.my_favorite),
                        R.drawable.ic_folderlist_star, NoteDataManagerImpl.COLLECTED_FOLDER, ORDER_FAVORITE);
        mPrivate = addDefaultPreference(mDefault, KEY_PRIVATE, getString(R.string.private_note),
                       R.drawable.ic_folderlist_lock, NoteDataManagerImpl.PRIVATE_FOLDER, ORDER_PRIVATE);
        mDelete = addDefaultPreference(mDefault, KEY_DELETE, getString(R.string.lastest_delete),
                      R.drawable.ic_folderlist_delete, NoteDataManagerImpl.DELETED_NOTE_FOLDER, ORDER_DELETE);
    }

    private FolderPreference addDefaultPreference(PreferenceCategory parent, String key, CharSequence title, int resId, int type, int order) {
        FolderPreference pref = new FolderPreference(this, String.valueOf(title), resId);
        pref.setKey(key);
        pref.setTitle(title);
        pref.setIcon(resId);
        pref.setOrder(order);
        pref.setOnPreferenceClickListener(this);
        pref.setSize(mNoteDataManager.getNotesForPresetFolder(type).size());
        parent.addPreference(pref);
        return pref;
    }
    /* }@ */

    private void addHorizontalPreference(PreferenceCategory parent, CharSequence title, int id) {
        FolderPreference pref = new FolderPreference(this, title.toString(), R.drawable.ic_folderlist_folder);
        pref.setLayoutResource(R.layout.horizontal_preference);
        pref.setKey(title.toString());
        pref.setTitle(title);
        pref.setOnPreferenceClickListener(this);
        pref.setOrder(id);
        pref.setSize(mNoteDataManager.getNotesForCustomFolder(id).size());
        parent.addPreference(pref);
    }

    private void updateHorizontalPreference(PreferenceCategory parent, CharSequence title, CharSequence oldTitle) {
        Log.d("NoteFolderListActivity", "oldtitle = "+oldTitle.toString());
        FolderPreference p = (FolderPreference)parent.findPreference(oldTitle);
        if (p == null) {
            // add debug code
            // dump all preference
            int length = parent.getPreferenceCount();
            for(int i= 0;i<length;i++){
                FolderPreference fp = (FolderPreference)parent.getPreference(i);
                Log.d("NoteFolderListActivity","fp name = "+fp.getTitle().toString());
            }
        /*UNISOC: Modify for bug 1235503 @{*/
        } else {
            p.setKey(title.toString());
            p.setTitle(title.toString());
        }
        /*}@*/
    }

    private void deletePreference(PreferenceCategory parent, CharSequence title) {
        Preference p = parent.findPreference(title);
        parent.removePreference(p);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            default:
                break;
        }
        //UNISOC: Modify for bug 1235508
        return super.onOptionsItemSelected(item);
    }

    private static void showMsg(int resId, Context context) {
        Toast.makeText(context, resId, Toast.LENGTH_SHORT).show();
    }

    void startActivityForFilter(CharSequence title, int filter, int id) {
        Intent intent = new Intent();
        intent.putExtra("TITLE", title);
        intent.putExtra("FILTER", filter);
        intent.putExtra("ID", id);

        if (filter == FolderUtil.FILTER_PRIVATE) {
            SharedPreferences preferences = getSharedPreferences("security", Context.MODE_PRIVATE);
            String pwd = preferences.getString("pwd","");
            String quest = preferences.getString("question","");
            intent.putExtra("START_TYPE", pwd.isEmpty() ? SecurityActivity.START_PASSWORD : (quest.isEmpty() ?
                   SecurityActivity.START_PASSWORD : SecurityActivity.START_ENTER_PWD));
            intent.setClass(this, SecurityActivity.class);
        } else {
            if(filter == FolderUtil.FILTER_ALL) {
                intent.setClass(this, NoteActivity.class);
            } else  if (filter != FolderUtil.FILTER_DELETE){
                intent.setClass(this, NoteChildActivity.class);
            } else {
                intent.setClass(this, NoteLatestDeleteActivity.class);
            }
        }
        startActivity(intent);
    }
}
