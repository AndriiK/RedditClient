package com.task.redditclient.ui;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.task.redditclient.R;
import com.task.redditclient.application.App;
import com.task.redditclient.net.Engine;

import static com.task.redditclient.net.Engine.Action.DOWNLOAD_IMAGE;

/**
 * Fragment to preview and save image.
 */
public class FragmentImagePreview extends    Fragment
                                  implements View.OnClickListener,
                                             Engine.Listener {
    /** Tag to identify "image URL" parameter. */
    private static final String IMG_URL_TAG = "imgUrl";

    /** Identifier of "Write to external storage" permission request */
    private static final int PERMISSIONS_REQUEST_WRITE_EXT_STORAGE = 1;

    /**
     * Creates an instance of this fragment.
     * @param imgUrl URL of image to display.
     * @return fragment instance.
     */
    public static FragmentImagePreview newInstance(String imgUrl) {
        FragmentImagePreview fragment = new FragmentImagePreview();
        Bundle args = new Bundle(1);
        args.putString(IMG_URL_TAG, imgUrl);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * @see Fragment#onCreateView(LayoutInflater, ViewGroup, Bundle)
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (null == container) {
            return null;
        }

        View view = inflater.inflate(R.layout.fragment_image_preview, container, false);

        ImageView imageView = view.findViewById(R.id.imgThumbnail);
        String imgUrl = getArguments().getString(IMG_URL_TAG);
        if (null != imgUrl) {
            Glide.with(this).load(imgUrl).into(imageView);
        }

        view.findViewById(R.id.btnSave).setOnClickListener(this);

        return view;
    }

    /**
     * @see Fragment#onPause()
     */
    @Override
    public void onResume() {
        super.onResume();

        ActivityMain activity = ((ActivityMain)getActivity());
        Engine engine = App.getEngine();
        if (engine.isActionExecuted(DOWNLOAD_IMAGE)) {
            engine.addListener(this);
            activity.showProgress(R.string.saving);
        } else {
            activity.hideProgress();
        }
    }

    /**
     * @see Fragment#onPause()
     */
    @Override
    public void onPause() {
        App.getEngine().removeListener(this);
        super.onPause();
    }

    /**
     * @see Fragment#onRequestPermissionsResult(int, String[], int[])
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
        case PERMISSIONS_REQUEST_WRITE_EXT_STORAGE:
            if (grantResults.length > 0 && PackageManager.PERMISSION_GRANTED == grantResults[0]) {
                startDownload();
            }
            break;
        default:
            break;
        }
    }

    /**
     * @see View.OnClickListener#onClick(View)
     */
    @Override
    public void onClick(View v) {
        Activity activity = getActivity();
        if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            startDownload();
        } else {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_WRITE_EXT_STORAGE);
        }
    }

    /**
     * @see Engine.Listener#onRequestCompleted(Engine.Action, Object...)
     */
    @Override
    public void onRequestCompleted(Engine.Action action, Object... data) {
        if (DOWNLOAD_IMAGE == action) {
            App.getEngine().removeListener(this);
            ActivityMain activity = (ActivityMain)getActivity();
            activity.hideProgress();
            activity.showMessage(R.string.imageSaved, null);
        }
    }

    /**
     * @see Engine.Listener#onRequestFailed(Engine.Action, Exception)
     */
    @Override
    public void onRequestFailed(Engine.Action action, Exception e) {
        if (DOWNLOAD_IMAGE == action) {
            App.getEngine().removeListener(this);
            ActivityMain activity = (ActivityMain)getActivity();
            activity.hideProgress();
            activity.showMessage(R.string.requestFailed, null);
        }
    }

    /**
     * Starts image downloading.
     */
    private void startDownload() {
        String imgUrl = getArguments().getString(IMG_URL_TAG);
        if (null != imgUrl) {
            ((ActivityMain)getActivity()).showProgress(R.string.saving);
            Engine engine = App.getEngine();
            engine.addListener(this);
            engine.downloadImage(imgUrl, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath());
        }
    }
}
