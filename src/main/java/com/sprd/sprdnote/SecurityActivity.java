package com.sprd.sprdnote;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;

import com.sprd.sprdnote.data.NoteDataManagerImpl;
import com.sprd.notejar.view.data.NoteItem;
import com.sprd.sprdnote.folder.FolderUtil;
import com.sprd.sprdnote.folder.PasswordFragment;
import com.sprd.sprdnote.folder.QuestFragment;
import android.support.v7.app.AppCompatActivity;
import java.util.List;

/**
 * Created by qian.dai on 2017/3/6.
 */

public class SecurityActivity extends AppCompatActivity {
    private static int START_TYPE = 0;
    public static final int START_PASSWORD = 0;
    public static final int START_ENTER_PWD = 1;
    public static final int START_QUEST = 2;
    public static final int START_FORGET = 3;
    private Intent mOriginIntent;
    private Fragment mFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.security);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        if(savedInstanceState != null) {
            //do something
            pwdState = savedInstanceState.getBundle("pwd");
            questState = savedInstanceState.getBundle("quest");
            current_fragment = savedInstanceState.getInt("current", START_PASSWORD);
        }
        mOriginIntent =  getIntent();
        initView(getIntent());
    }

    private void initView(Intent intent) {
        START_TYPE = intent.getIntExtra("START_TYPE", 0);
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        switch(START_TYPE) {
            case START_PASSWORD:
                mFragment = new PasswordFragment(this);
                break;
            case START_ENTER_PWD:
                mFragment = new PasswordFragment(this, FolderUtil.INPUT_PWD);
                break;
        }
        if (current_fragment == START_QUEST) {
            mFragment = new QuestFragment(this);
        } else if (current_fragment == START_FORGET) {
            mFragment = new QuestFragment(this, FolderUtil.INPUT_FORGET_PWD);
        }
        fragmentTransaction.replace(R.id.autoadjust, mFragment);
        fragmentTransaction.commit();
    }

    public void changeFragment(int flag) {
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        switch(flag) {
            case FolderUtil.INPUT_QUEST:
                mFragment = new QuestFragment(this);
                fragmentTransaction.replace(R.id.autoadjust, mFragment);
                current_fragment = START_QUEST;
                fragmentTransaction.commit();
                break;
            case FolderUtil.INPUT_DONE:
                //start private activity
                //record enter time
                SharedPreferences preferences = getSharedPreferences("security", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor= preferences.edit();
                editor.putLong("enter_time",System.currentTimeMillis());
                editor.commit();
                if(mOriginIntent != null) {
                    mOriginIntent.setClass(this, NoteChildActivity.class);
                    startActivity(mOriginIntent);
                    finish();
                }
                break;
            case FolderUtil.INPUT_FORGET_PWD:
                mFragment = new QuestFragment(this, FolderUtil.INPUT_FORGET_PWD);
                fragmentTransaction.replace(R.id.autoadjust, mFragment);
                current_fragment = START_FORGET;
                fragmentTransaction.commit();
                break;
            case FolderUtil.INPUT_PWD:
                mFragment = new PasswordFragment(this);
                fragmentTransaction.replace(R.id.autoadjust, mFragment);
                current_fragment = START_PASSWORD;
                fragmentTransaction.commit();
                break;
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
    }
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
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
    public void clear(){
        new AlertDialog.Builder(this, R.style.FolderDialog)
                .setTitle(R.string.clear_private)
                .setMessage(R.string.clear_warning_text)
                .setPositiveButton(R.string.Ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SharedPreferences preferences = SecurityActivity.this.getSharedPreferences("security", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor= preferences.edit();
                        editor.clear();
                        editor.commit();
                        //remove all private notes
                        new Thread(){
                            public void run(){
                                NoteDataManagerImpl nm = NoteDataManagerImpl.getInstance(SecurityActivity.this);
                                List<NoteItem> list = nm.getNotesForPresetFolder(NoteDataManagerImpl.PRIVATE_FOLDER);
                                for(NoteItem ni:list) {
                                    nm.deleteNoteIntoDeletedFolder(ni);
                                    nm.deleteNoteFromDeletedFolder(ni);
                                }
                            }
                        }.start();
                        finish();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .create()
                .show();
    }

    private Bundle pwdState;
    private Bundle questState;
    private int current_fragment;
    public void saveStateForPwd(Bundle outState){
        pwdState = outState;
    };
    public void saveStateForQuest(Bundle outState){
        questState = outState;
    };
    public Bundle getStateForPwd(){
       return pwdState;
    };
    public Bundle getStateForQuest(){
       return questState;
    };
    @Override
    public void onSaveInstanceState(Bundle outState) {
        if(mFragment instanceof QuestFragment) {
            ((QuestFragment)mFragment).saveState();
        } else if(mFragment instanceof PasswordFragment) {
            ((PasswordFragment)mFragment).saveState();
        }
        //super.onSaveInstanceState(outState);
        // Save State Here
        if (pwdState != null) outState.putBundle("pwd", pwdState);
        if (questState != null) outState.putBundle("quest", questState);
        outState.putInt("current",current_fragment);
    }
}
