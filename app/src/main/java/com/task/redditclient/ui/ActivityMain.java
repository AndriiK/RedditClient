package com.task.redditclient.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.task.redditclient.R;
import com.task.redditclient.application.App;
import com.task.redditclient.net.Engine;

import static com.task.redditclient.net.Engine.Action.GET_TOKEN;

/**
 * Single application activity manages fragments.
 */
public class ActivityMain extends    Activity
                          implements DialogInterface.OnClickListener,
                                     Engine.Listener {
    /** Progress indicator view */
    private View mProgress;
    /** Progress label */
    private TextView mLblProgress;

    /**
     * Displays alert with given message.
     * @param listener Listener to handle dialog button click event.
     */
    public void showMessage(int textResId, DialogInterface.OnClickListener listener) {
        AlertDialog alert = new AlertDialog.Builder(this).create();
        alert.setMessage(getString(textResId));
        alert.setCanceledOnTouchOutside(false);
        alert.setButton(AlertDialog.BUTTON_NEUTRAL, getString(android.R.string.ok), listener);
        alert.show();
    }

    /**
     * Displays progress view.
     * @param textResId Text resource identifier.
     */
    public void showProgress(int textResId) {
        mLblProgress.setText(textResId);
        mProgress.setVisibility(View.VISIBLE);
    }

    /**
     * Hides progress view.
     */
    public void hideProgress() {
        mProgress.setVisibility(View.GONE);
    }

    /**
     * Opens fragment to preview image.
     */
    public void previewImage(String thumbnailURL) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, FragmentImagePreview.newInstance(thumbnailURL), FragmentImagePreview.class.getName());
        transaction.addToBackStack(null);
        transaction.commit();
    }

    /**
     * @see Activity#onCreate(Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mProgress = findViewById(R.id.progressView);
        mLblProgress = mProgress.findViewById(R.id.lblProgress);

        Engine engine = App.getEngine();
        if (engine.isAuthenticated()) {
            if (null == getFragmentManager().findFragmentByTag(FragmentEntryList.class.getName())) {
                addEntityListFragment();
            }
        } else {
            showProgress(R.string.authenticating);
            engine.addListener(this);
            engine.getToken();
        }
    }

    /**
     * @see Activity#onPause()
     */
    @Override
    protected void onPause() {
        App.getEngine().removeListener(this);
        super.onPause();
    }

    /**
     * @see DialogInterface.OnClickListener#onClick(DialogInterface, int)
     */
    @Override
    public void onClick(DialogInterface dialog, int which) {
        finish();
    }

    /**
     * @see Engine.Listener#onRequestCompleted(Engine.Action, Object...)
     */
    @Override
    public void onRequestCompleted(Engine.Action action, Object... data) {
        if (GET_TOKEN == action) {
            App.getEngine().removeListener(this);
            hideProgress();
            addEntityListFragment();
        }
    }

    /**
     * @see Engine.Listener#onRequestFailed(Engine.Action, Exception)
     */
    @Override
    public void onRequestFailed(Engine.Action action, Exception e) {
        if (GET_TOKEN == action) {
            App.getEngine().removeListener(this);
            showMessage(R.string.requestFailed, this);
        }
    }

    /**
     * Add fragment to show entity list.
     */
    private void addEntityListFragment() {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.add(R.id.fragmentContainer, new FragmentEntryList(), FragmentEntryList.class.getName());
        transaction.commit();
    }
}
