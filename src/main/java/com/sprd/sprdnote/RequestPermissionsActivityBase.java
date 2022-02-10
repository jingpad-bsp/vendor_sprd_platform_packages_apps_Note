package com.sprd.sprdnote;

import java.util.ArrayList;
import java.util.Arrays;

import android.app.Activity;
import android.content.Context;
/* UNISOC: add for bug1076909 {@ */
import android.content.DialogInterface;
import android.app.AlertDialog;
/* @ }*/
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Trace;
import android.widget.Toast;
import com.sprd.sprdnote.widget.NoteAppWidgetProvider;

/**
 * Activity that asks the user for all {@link #getDesiredPermissions} if any of
 * {@link #getRequiredPermissions} are missing.
 *
 * NOTE: As a result of b/22095159, this can behave oddly in the case where the
 * final permission you are requesting causes an application restart.
 */
public abstract class RequestPermissionsActivityBase extends Activity {
    public static final String PREVIOUS_ACTIVITY_INTENT = "previous_intent";
    private static final int PERMISSIONS_REQUEST_ALL_PERMISSIONS = 1;
    private boolean mResumed = false;
    private static Activity mPreActivity;
    /* UNISOC: add for bug1076909,1194678 {@ */
    private AlertDialog mDialog;
    /* @ }*/

    /**
     * @return list of permissions that are needed in order for
     *         {@link #PREVIOUS_ACTIVITY_INTENT} to operate. You only need to
     *         return a single permission per permission group you care about.
     */
    protected abstract String[] getRequiredPermissions();

    /**
     * @return list of permissions that would be useful for
     *         {@link #PREVIOUS_ACTIVITY_INTENT} to operate. You only need to
     *         return a single permission per permission group you care about.
     */
    protected abstract String[] getDesiredPermissions();

    private Intent mPreviousActivityIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPreviousActivityIntent = (Intent) getIntent().getExtras().get(PREVIOUS_ACTIVITY_INTENT);

        // Only start a requestPermissions() flow when first starting this
        // activity the first time.
        // The process is likely to be restarted during the permission flow
        // (necessary to enable
        // permissions) so this is important to track.
        if (savedInstanceState == null) {
            requestPermissions();
        }
    }

    /**
     * If any permissions the Note app needs are missing, open an Activity
     * to prompt the user for these permissions. Moreover, finish the current
     * activity.
     *
     * This is designed to be called inside
     * {@link Activity#onCreate}
     */
    protected static boolean startPermissionActivity(Activity activity, String[] requiredPermissions,
            Class<?> newActivityClass) {
        if (!RequestPermissionsActivity.hasPermissions(activity, requiredPermissions)) {
            final Intent intent = new Intent(activity, newActivityClass);
            intent.putExtra(PREVIOUS_ACTIVITY_INTENT, activity.getIntent());
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);    //Unisoc: add for Bug1180195
            activity.startActivity(intent);
            if (!(activity instanceof NoteActivity)) {
                activity.finish();
            } else {
                mPreActivity = activity;
            }
            return true;
        }

        return false;
    }

    private static final int attachment_code = 0;

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == 0) {
            return;
        }
        if (permissions != null && permissions.length > 0 && isAllGranted(permissions, grantResults)) {
            /* UNISOC: add for bug1180195 {@ */
            mPreviousActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(mPreviousActivityIntent);
            /* @} */
            NoteAppWidgetProvider.updatePermissionWidget(this);
            finish();
            overridePendingTransition(0, 0);
        } else {
            /* UNISOC: add for bug1076909, 1194678{@ */
            //Toast.makeText(this, R.string.error_permissions, Toast.LENGTH_SHORT).show();
            mDialog = new AlertDialog.Builder(this)
                    .setMessage(R.string.error_permissions)
                    .setNegativeButton(R.string.Quit, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which){
                             finish();
                        }
                    }).setCancelable(false).create();
            mDialog.show();
            NoteAppWidgetProvider.updatePermissionWidget(this);
            /* @ }*/
            if ((mPreviousActivityIntent.getComponent().getClassName().contains("NoteActivity")) && mPreActivity instanceof NoteActivity){
                mPreActivity.finish();
            }
        }
    }

    private boolean isAllGranted(String permissions[], int[] grantResult) {
        for (int i = 0; i < permissions.length; i++) {
            if (grantResult[i] != PackageManager.PERMISSION_GRANTED && isPermissionRequired(permissions[i])) {
                return false;
            }
        }
        return true;
    }

    private boolean isPermissionRequired(String p) {
        return Arrays.asList(getRequiredPermissions()).contains(p);
    }

    /*UNISOC: Add for bug 1194678 @{*/
    @Override
    protected void onDestroy(){
        super.onDestroy();
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
    }
    /*}*/

    private void requestPermissions() {
        Trace.beginSection("requestPermissions");
        try {
            // Construct a list of missing permissions
            final ArrayList<String> unsatisfiedPermissions = new ArrayList<>();
            for (String permission : getDesiredPermissions()) {
                if (checkSelfPermission(permission)
                        != PackageManager.PERMISSION_GRANTED) {
                    unsatisfiedPermissions.add(permission);
                }
            }
            if (unsatisfiedPermissions.size() == 0) {
                finish();
                return;
            }
            requestPermissions(
                    unsatisfiedPermissions.toArray(new String[unsatisfiedPermissions.size()]),
                    PERMISSIONS_REQUEST_ALL_PERMISSIONS);
        } finally {
            Trace.endSection();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mResumed = true;
    }
    protected static boolean hasPermissions(Context context, String[] permissions) {
        Trace.beginSection("hasPermission");
        try {
            for (String permission : permissions) {
                if (context.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
            return true;
        } finally {
            Trace.endSection();
        }
    }

    /**
     * onBackPressed方法中使用的是系统hide的isResumed方法，因为暂时不是放在源码中编译，而且如何使用系统hide api的东西没有去搞，所以暂时写
     * 一个假的替代。后续去除即可。
     * @return
     */
    /* private boolean isResumed(){
        return mResumed;
    }*/
    @Override
    public void onBackPressed() {
        if (isResumed()) {
            super.onBackPressed();
        }
    }
}
