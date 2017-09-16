package com.task.redditclient.net;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.task.redditclient.application.App;
import com.task.redditclient.application.Common;
import com.task.redditclient.model.json.Entry;
import com.task.redditclient.model.json.ResponseGetEntries;
import com.task.redditclient.model.json.ResponseToken;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;

/**
 * Engine to handle back end api.
 */
public class Engine {
    /**
     * Engine action identifiers.
     */
    public enum Action {
        GET_TOKEN,
        GET_ENTRIES,
        DOWNLOAD_IMAGE
    }

    /**
     * Interface to receive engine events.
     */
    public interface Listener {
        /**
         * Called when request is completed successfully.
         * @param action Identifier of completed action.
         * @param data Data received for this request.
         *        Should be cast to the expected data type.
         */
        void onRequestCompleted(Action action, Object... data);

        /**
         * Called when request is failed.
         * @param action Identifier of completed action.
         * @param e Request exception.
         */
        void onRequestFailed(Action action, Exception e);
    }

    /** Reddit application client identifier */
    private static final String CLIENT_ID = "DuUW-KECgrqDjw";
    /** Reddit client secret. Since installed app types have no secret, it is just empty string */
    private static final String CLIENT_SECRET = "";

    /** User agent identifier */
    private static final String USER_AGENT = "android:com.task.redditclient:v1.0 by Andrii";

    /** Base Reddit endpoint */
    private static final String URL_BASE = "https://www.reddit.com";
    /** Authenticated Reddit endpoint */
    private static final String URL_OAUTH = "https://oauth.reddit.com";

    /** URL suffix for "Get token" request */
    private static final String GET_TOKEN   = "/api/v1/access_token";
    /** URL suffix for "Get entries" request */
    private static final String GET_ENTRIES = "/top";

    /** Body of "Get Token" request */
    private static final String GET_TOKEN_BODY = "grant_type=https://oauth.reddit.com/grants/installed_client&device_id=%s";

    /** Time URL parameter. */
    private static final String PARAM_TIME = "t";
    /** Limit URL parameter. The maximum number of items desired (default: 25, maximum: 100) */
    private static final String PARAM_LIMIT = "limit";
    /** After URL parameter. */
    private static final String PARAM_AFTER = "after";

    /** Listeners to receive callbacks. */
    private HashSet<Listener> mListeners;
    /** Map to store started requests */
    private HashMap<Action, RequestTask> mRequests;
    /** JSON parser. */
    private Gson mGson;
    /** Device ID for current session */
    private String mDeviceId;
    /** Reddit access token */
    private String mToken;

    /**
     * Constructor.
     */
    public Engine() {
        mListeners = new HashSet<>();
        mRequests  = new HashMap<>();
        mGson      = new GsonBuilder().create();
        mDeviceId  = UUID.randomUUID().toString();
    }

    /**
     * Adds a listener to receive engine events.
     * @param listener Listener to be added.
     */
    public void addListener(Listener listener) {
        mListeners.add(listener);
    }

    /**
     * Removes a listener from engine event receivers.
     * @param listener Listener to be removed.
     */
    public void removeListener(Listener listener) {
        mListeners.remove(listener);
    }

    /**
     * Informs either authentication is performed or not.
     * @return true if client is authenticated, otherwise false.
     */
    public boolean isAuthenticated() {
        return null != mToken;
    }

    /**
     * Informs either given action is executed or not.
     * @param action Action to check.
     * @return true if action is executed, otherwise - false.
     */
    public boolean isActionExecuted(Action action) {
        return mRequests.containsKey(action);
    }

    /**
     * Starts "Get token" request.
     * Callback data format: No data received.
     */
    public void getToken() {
        startRequest(Action.GET_TOKEN);
    }

    /**
     * Starts "Get entries" request.
     * @param after Entry ID to get the next page.
     *              if null request will fetch the first page.
     * Callback data format:
     *     {@link ArrayList<Entry>} data[0] - Array of received entries.
     *     {@link String} data[1] - after value to request the next page.
     */
    public void getEntries(String after) {
        startRequest(Action.GET_ENTRIES, after);
    }

    /**
     * Starts request to download remote image.
     * @param imageUrl remote image URL.
     * @param folder Destination folder to save file.
     * Callback data format:
     *     {@link String} data[0] - Full local path of downloaded image.
     */
    public void downloadImage(String imageUrl, String folder) {
        startRequest(Action.DOWNLOAD_IMAGE, imageUrl, folder);
    }

    /**
     * Starts request to the back end server.
     * @param action Identifier of action to be executed. Refer {@link Engine.Action}
     * @param params request parameters.
     */
    private void startRequest(Action action, Object... params) {
        RequestTask request = mRequests.get(action);
        if (null != request) {
            // Cancel previous request with the same action.
            request.cancel(true);
            mRequests.remove(action);
        }
        request = new RequestTask();
        request.execute(new RequestData(action, params));
        mRequests.put(action, request);
    }

    /**
     * Executes get token request.
     * @throws Exception if request is failed.
     */
    private void executeGetToken() throws Exception {
        final String credentials = CLIENT_ID + ":" + CLIENT_SECRET;
        final String basicAuth = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);

        ArrayList<KeyValue> header = new ArrayList<>(2);
        header.add(new KeyValue(NetManager.KEY_AUTHORIZATION, basicAuth));
        header.add(new KeyValue(NetManager.KEY_ACCEPT, NetManager.VAL_APPLICATION_JSON));

        String respStr = NetManager.post((URL_BASE + GET_TOKEN), header, String.format(Locale.getDefault(), GET_TOKEN_BODY, mDeviceId));
        ResponseToken token = mGson.fromJson(respStr, ResponseToken.class);
        mToken = token.access_token;
    }

    /**
     * Executes "get entries" request.
     * @param after Entry ID to get the next page.
     *              if null request will fetch the first page.
     * @return array with received entry list and "after" value.
     * @throws Exception if request is failed.
     */
    private Object[] executeGetEntries(String after) throws Exception {
        ArrayList<KeyValue> header = new ArrayList<>(3);
        header.add(new KeyValue(NetManager.KEY_AUTHORIZATION, "bearer " + mToken));
        header.add(new KeyValue(NetManager.KEY_USER_AGENT, USER_AGENT));
        header.add(new KeyValue(NetManager.KEY_ACCEPT, NetManager.VAL_APPLICATION_JSON));

        ArrayList<KeyValue> urlParams = new ArrayList<>(3);
        urlParams.add(new KeyValue(PARAM_TIME, Common.VALUE_TIME));
        urlParams.add(new KeyValue(PARAM_LIMIT, String.valueOf(Common.ENTRY_NUM)));
        if (null != after) {
            urlParams.add(new KeyValue(PARAM_AFTER, after));
        }

        String respStr = NetManager.get((URL_OAUTH + GET_ENTRIES), header, urlParams);
        ResponseGetEntries response = mGson.fromJson(respStr, ResponseGetEntries.class);

        ArrayList<Entry> entries = new ArrayList<>(Arrays.asList(response.data.children));
        App.getStorage().addEntries(entries, response.data.after, null == after);

        return new Object[]{entries, response.data.after};
    }

    /**
     * Executes request to download image.
     * @param imageUrl remote image URL.
     * @param folder Destination folder to save file.
     * @return Local image path in the first array item.
     * @throws Exception if request is failed.
     */
    private String[] executeDownloadImage(String imageUrl, String folder) throws Exception {
        String fileName = NetManager.download(imageUrl, folder);

        // Add downloaded image to the Media Provider's database
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri contentUri = Uri.fromFile(new File(fileName));
        mediaScanIntent.setData(contentUri);
        App.getInstance().sendBroadcast(mediaScanIntent);

        return new String[]{fileName};
    }

    /**
     * Asynchronous task to execute request in the background
     * and return result in the main thread.
     */
    private class RequestTask extends AsyncTask<RequestData, Void, Object[]> {
        //! Request input data.
        private RequestData mData;
        //! Request execution error.
        private Exception mException = null;

        /**
         * @see AsyncTask#doInBackground(Object...)
         */
        @Override
        protected Object[] doInBackground(RequestData... requestData) {
            mData = requestData[0];

            Object[] result = null;
            // Execute HTTP request
            try {
                switch(mData.action) {
                case GET_TOKEN:
                    executeGetToken();
                    break;
                case GET_ENTRIES:
                    result = executeGetEntries((String)mData.values[0]);
                    break;
                case DOWNLOAD_IMAGE:
                    result = executeDownloadImage((String)mData.values[0], (String)mData.values[1]);
                    break;
                default:
                    break;
                }
            } catch (Exception e) {
                Log.e("Engine", "Request failed", e);
                mException = e;
            }

            return result;
        }

        /**
         * @see AsyncTask#onPostExecute(Object)
         */
        protected void onPostExecute(Object[] result) {
            if (!isCancelled()) {
                if (null != mListeners) {
                    Iterator iterator = mListeners.iterator();
                    while (iterator.hasNext()) {
                        if (null == mException) {
                            ((Listener)iterator.next()).onRequestCompleted(mData.action, result);
                        } else {
                            ((Listener)iterator.next()).onRequestFailed(mData.action, mException);
                        }
                    }
                }

                mRequests.remove(mData.action);
            }
        }
    }

    /**
     * Internal container to store request data.
     */
    private class RequestData {
        /** Action identifier */
        Action action;
        /** Request parameters */
        Object values[];

        /**
         * Constructor.
         * @param actionId action identifier, refer {@link Action}.
         * @param params request parameters array.
         */
        RequestData(Action actionId, Object... params) {
            action = actionId;
            values = params;
        }
    }
}
