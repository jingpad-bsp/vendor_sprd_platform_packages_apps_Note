package com.sprd.sprdnote.folder;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.sprd.sprdnote.R;
import com.sprd.sprdnote.SecurityActivity;

/**
 * Created by qian.dai on 2017/3/6.
 */

public class PasswordFragment extends Fragment implements TextWatcher, TextView.OnEditorActionListener, TextView.OnClickListener{
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.forgetPwd:
                ((SecurityActivity)getActivity()).changeFragment(FolderUtil.INPUT_FORGET_PWD);
                 break;
            }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (currentIndex >= 3 ) {
            if (mNext != null) {
                mNext.setEnabled(true);
            }
            if (mStartType == FolderUtil.INPUT_PWD) {

                Message message = Message.obtain();
                SharedPreferences preferences = getActivity().getSharedPreferences("security", Context.MODE_PRIVATE);
                String password = preferences.getString("pwd", "");
                String temp = getPwd();
                if (!temp.isEmpty() && temp.equals(password)) {
                    //start private activity
                    message.what = INPUT_DONE;
                } else {
                    message.what = INPUT_ERROR;
                }

                mHandler.sendMessage(message);
            }
        } else {
            if (mNext != null) {
                mNext.setEnabled(false);
            }
        }
    }

    private Context mContext;
    private TextView mForgetPwd;
    private TextView mHeader;
    private TextView mError;
    private View view;
    private MenuItem mNext;
    private Keyboard k;
    private EditText[] tvList;
    private KeyboardView mKeyboard;
    private View v;
    private Animation shake;
    private int FORGET_PWD = 100;
    private int INPUT_NOTHING = 200;
    private int INPUT_DONE = 1;
    private int INPUT_ERROR = 2;
    private int currentIndex = -1;
    private int flag = FolderUtil.INPUT_INTRODUCTION;
    private String mFirstPwd = "";
    private int mStartType;
    private boolean buttonEnable = false;

    @SuppressLint({"NewApi", "ValidFragment"})
    public PasswordFragment(final Context context) {
        mContext = context;
    }

    @SuppressLint({"NewApi", "ValidFragment"})
    public PasswordFragment(final Context context, final int type) {
        mContext = context;
        mStartType = type;
    }
    public PasswordFragment() {
    }

    public void saveState() {
        Bundle outState = new Bundle();
        // Save State Here
        outState.putString("text1", tvList[0].getText().toString());
        outState.putString("text2", tvList[1].getText().toString());
        outState.putString("text3", tvList[2].getText().toString());
        outState.putString("text4", tvList[3].getText().toString());
        outState.putInt("currentIndex", currentIndex);
        outState.putString("mFirstPwd", mFirstPwd);
        outState.putInt("flag", flag);
        if (mNext != null) outState.putBoolean("next_enable", mNext.isEnabled());
        ((SecurityActivity)getActivity()).saveStateForPwd(outState);
    }
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    ((SecurityActivity) getActivity()).changeFragment(FolderUtil.INPUT_DONE);
                    break;
                case 2:
                    mError.setVisibility(View.VISIBLE);
                    v.startAnimation(shake);
                    initPassWordInputView();
                    break;
            }
        }
    };

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.passwordoption, menu);
        mNext = menu.findItem(R.id.action_next);
        mNext.setEnabled(buttonEnable);
        mNext.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_next:
                handleNext();
                break;
            default:
                break;
        }
        //UNSOC: Modify for bug 1235508
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if(mStartType == FolderUtil.INPUT_PWD) {
            getActivity().setTitle(R.string.enter_pwd);
        } else {
            getActivity().setTitle(R.string.set_password);
        }
        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContext = getActivity();
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        view = inflater.inflate(R.layout.choose_password, null);
        tvList = new EditText[4];
        tvList[0] = (EditText) view.findViewById(R.id.tv_pass1);
        tvList[1] = (EditText) view.findViewById(R.id.tv_pass2);
        tvList[2] = (EditText) view.findViewById(R.id.tv_pass3);
        tvList[3] = (EditText) view.findViewById(R.id.tv_pass4);
        for(int i = 0 ;i < 4;i++) {
            tvList[i].addTextChangedListener(this);
            tvList[i].setTransformationMethod(new MyPasswordTransformationMethod());

        }
        Bundle pwdState = ((SecurityActivity)getActivity()).getStateForPwd();
        if ( pwdState != null) {
            ((Editable)tvList[0].getText()).append(String.valueOf(pwdState.getString("text1")));
            ((Editable)tvList[1].getText()).append(String.valueOf(pwdState.getString("text2")));
            ((Editable)tvList[2].getText()).append(String.valueOf(pwdState.getString("text3")));
            ((Editable)tvList[3].getText()).append(String.valueOf(pwdState.getString("text4")));
            currentIndex = pwdState.getInt("currentIndex", -1);
            flag = pwdState.getInt("flag", FolderUtil.INPUT_INTRODUCTION);
            mFirstPwd = pwdState.getString("mFirstPwd");
            buttonEnable = pwdState.getBoolean("next_enable");
        }
        mHeader = (TextView) view.findViewById(R.id.headerText);
        mError = (TextView) view.findViewById(R.id.errorText);
        mKeyboard = (KeyboardView)view.findViewById(R.id.keyboard);
        mForgetPwd = (TextView) view.findViewById(R.id.forgetPwd);
        k = new Keyboard(getActivity(), R.xml.keyboard_no_forget);

        if(mStartType == FolderUtil.INPUT_PWD) {
            mForgetPwd.setVisibility(View.VISIBLE);
            mForgetPwd.setOnClickListener(this);
        }
        mKeyboard.setKeyboard(k);
        mKeyboard.setPreviewEnabled(false);
        mKeyboard.setEnabled(true);
        mKeyboard.setBackground(mContext.getResources().getDrawable(R.drawable.keyboard_bg));
        mKeyboard.setPadding(0,1,0,0);
        mKeyboard.setVisibility(View.VISIBLE);
        mKeyboard.setOnKeyboardActionListener(listener);
        v = view.findViewById(R.id.enter_pwd);
        shake = AnimationUtils.loadAnimation(mContext, R.anim.shake);
        shake.setDuration(150);
        updateUi();
        return view;
    }

    public void initPassWordInputView(){
        currentIndex = -1;
        for(int i = 0;i < 4;i++){
            tvList[i].setText("");
        }
    }

    private KeyboardView.OnKeyboardActionListener listener = new KeyboardView.OnKeyboardActionListener() {
        @Override
        public void onPress(int primaryCode) {
        }

        @Override
        public void onRelease(int primaryCode) {

        }

        @Override
        public void onKey(int primaryCode, int[] keyCodes) {
            if (primaryCode == Keyboard.KEYCODE_DELETE){
                if (currentIndex - 1 >= -1) {
                    tvList[currentIndex--].setText("");
                }
            } else if (primaryCode == INPUT_NOTHING) {
                //Do nothing
            } else if (primaryCode == FORGET_PWD) {
                initPassWordInputView();
                ((SecurityActivity)getActivity()).changeFragment(FolderUtil.INPUT_FORGET_PWD);
            } else {
                if (currentIndex >= -1 && currentIndex < 3) {
                    Editable edit = tvList[++currentIndex].getText();
                    edit.append(String.valueOf(primaryCode));
                }
            }

        }

        @Override
        public void onText(CharSequence text) {

        }

        @Override
        public void swipeLeft() {

        }

        @Override
        public void swipeRight() {

        }

        @Override
        public void swipeDown() {

        }

        @Override
        public void swipeUp() {

        }
    };

    public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_DONE
                || actionId == EditorInfo.IME_NULL) {
            handleNext();
            return true;
        }
        return false;
    }

    public String getPwd(){
        StringBuilder sb = new StringBuilder();
        for(int i = 0;i < 4;i++){
            sb.append(tvList[i].getText().toString());
        }
        return sb.toString();
    }
    public void handleNext() {
        String pwd = getPwd();
        if(pwd.isEmpty()) {
            return;
        }

            if (flag == FolderUtil.INPUT_INTRODUCTION) {
                mFirstPwd = pwd;
                initPassWordInputView();
                flag = FolderUtil.INPUT_CONFIRM;
                updateUi();
            } else if (flag == FolderUtil.INPUT_CONFIRM) {
                if (mFirstPwd.equals(pwd)) {
                    flag = FolderUtil.INPUT_QUEST;
                    startSaveAndQuest();
                } else {
                    mError.setVisibility(View.VISIBLE);
                    v.startAnimation(shake);
                    initPassWordInputView();
                }
            }
    }
    public void updateUi() {
        if(mStartType == FolderUtil.INPUT_PWD) {
            mHeader.setVisibility(View.INVISIBLE);
            mError.setText(R.string.wrong_pwd);
            setHasOptionsMenu(false);
        } else {
            if (flag == FolderUtil.INPUT_INTRODUCTION) {
                mHeader.setText(R.string.enter_pwd_hint);
            } else if (flag == FolderUtil.INPUT_CONFIRM) {
                mHeader.setText(R.string.enter_pwd_again_hint);
            }
        }
    }

    private void startSaveAndQuest() {
        SharedPreferences preferences = getActivity().getSharedPreferences("security", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor= preferences.edit();
        editor.putString("pwd",mFirstPwd);
        editor.commit();

        //start quest activity
        ((SecurityActivity)getActivity()).changeFragment(flag);
    }
}
