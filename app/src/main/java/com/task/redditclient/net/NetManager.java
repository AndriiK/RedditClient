package com.task.redditclient.net;

import android.net.Uri;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/**
 * Class to execute HTTP/HTTPS requests.
 * All requests are executed synchronously, therefore it 
 * should be used in background threads only.
 */
@SuppressWarnings("WeakerAccess")
public class NetManager {
    //! Standard HTTP field keys
    public static final String KEY_ACCEPT              = "Accept";
    public static final String KEY_AUTHORIZATION       = "Authorization";
    public static final String KEY_CONTENT_LENGTH      = "Content-Length";
    public static final String KEY_CONTENT_DISPOSITION = "Content-Disposition";
    public static final String KEY_USER_AGENT          = "User-Agent";

    //! Standard HTTP field values
    public static final String VAL_APPLICATION_JSON = "application/json";

    /** UTF-8 charset identifier. */
    private static final String UTF_8 = "UTF-8";

    /** Identifier of POST request method. */
    private static final String POST = "POST";

    //! URL parameter dividers.
    private static final String URL_PARAM_START             = "?";
    private static final String URL_PARAM_DIVIDER           = "&";
    private static final String URL_PARAM_KEY_VALUE_DIVIDER = "=";

    /** Size of temporary buffer during file downloading. */
    private static final int BUFFER_SIZE = 4096;

    /**
     * Executes HTTP GET request.
     * @param url Destination URL.
     * @param headerFields List contains custom fields to be added in header.
     * @param urlParams parameters to be passed with URL.
     * @return response string.
     * @throws Exception if request error occurs.
     */
    public static String get(final String url,
            final List<KeyValue> headerFields,
            final List<KeyValue> urlParams) throws Exception {
        URL connectUrl = new URL(urlWithParams(url, urlParams));
        HttpURLConnection urlConnection = (HttpURLConnection)connectUrl.openConnection();
        setHeaders(headerFields, urlConnection);
        String response = execute(urlConnection);
        urlConnection.disconnect();
        return response;
    }

    /**
     * Executes HTTP POST request.
     * @param url Destination URL.
     * @param headerFields List contains custom fields to be added in header.
     * @param body Text to be added as HTTP body. Pass null to create request without body.
     * @return response string.
     * @throws Exception if request error occurs.
     */
    public static String post(final String url,
            final List<KeyValue> headerFields,
            final String body) throws Exception {
        URL connectUrl = new URL(url);
        HttpURLConnection urlConnection = (HttpURLConnection)connectUrl.openConnection();
        urlConnection.setDoOutput(true);
        urlConnection.setRequestMethod(POST);

        setHeaders(headerFields, urlConnection);

        if (null != body) {
            byte[] buff = body.getBytes(UTF_8);
            urlConnection.setRequestProperty(KEY_CONTENT_LENGTH, String.valueOf(buff.length));
            DataOutputStream dos = new DataOutputStream(urlConnection.getOutputStream());
            dos.write(buff, 0, buff.length);
            dos.flush();
            dos.close();
        }

        String response = execute(urlConnection);
        urlConnection.disconnect();

        return response;
    }

    /**
     * Downloads a file from a URL
     * @param url HTTP URL of the file to be downloaded
     * @param saveDir path of the directory to save the file
     * @return Downloaded file path.
     * @throws Exception if request error occurs.
     */
    public static String download(String url, String saveDir) throws Exception {
        String filePath;

        URL connectUrl = new URL(url);
        HttpURLConnection urlConnection = (HttpURLConnection)connectUrl.openConnection();

        int statusCode = urlConnection.getResponseCode();
        if (HttpURLConnection.HTTP_OK == statusCode || HttpURLConnection.HTTP_ACCEPTED == statusCode) {
            String fileName = "";
            String disposition = urlConnection.getHeaderField(KEY_CONTENT_DISPOSITION);

            if (disposition != null) {
                // extracts file name from header field
                int index = disposition.indexOf("filename=");
                if (index > 0) {
                    fileName = disposition.substring(index + 10, disposition.length() - 1);
                }
            } else {
                // extracts file name from URL
                fileName = url.substring(url.lastIndexOf("/") + 1, url.length());
            }

            InputStream inStream = urlConnection.getInputStream();
            filePath = saveDir + File.separator + fileName;
            FileOutputStream outStream = new FileOutputStream(filePath);

            int bytesRead;
            byte[] buffer = new byte[BUFFER_SIZE];
            while ((bytesRead = inStream.read(buffer)) != -1) {
                outStream.write(buffer, 0, bytesRead);
            }

            outStream.close();
            inStream.close();
        } else {
            String response = readStream(new BufferedInputStream(urlConnection.getErrorStream()));
            throw new Exception("Request failed with status code " + String.valueOf(statusCode) + ", body: " + response);
        }

        urlConnection.disconnect();

        return filePath;
    }

    /**
     * Reads input stream into string.
     * @param inStream stream to read.
     * @return String with stream data.
     * @throws IOException if error occurs.
     */
    private static String readStream(InputStream inStream) throws IOException {
        InputStreamReader streamReader = new InputStreamReader(inStream);
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader br = new BufferedReader(streamReader);
        String read = br.readLine();
        while(null != read) {
            stringBuilder.append(read);
            read = br.readLine();
        }

        streamReader.close();
        inStream.close();

        return stringBuilder.toString();
    }

    /**
     * Sets request headers.
     * @param headerFields Array of headers to add to the request.
     * @param urlConnection Connection to execute request.
     */
    private static void setHeaders(final List<KeyValue> headerFields, HttpURLConnection urlConnection) {
        if (null != headerFields) { 
            for (KeyValue header : headerFields) {
                urlConnection.setRequestProperty(header.key, header.value);
            }
        }
    }

    /**
     * Executes HTTP request.
     * @param urlConnection Connection to execute request.
     * @return response string.
     * @throws Exception if request failed.
     */
    private static String execute(HttpURLConnection urlConnection) throws Exception {
        String response;
        int statusCode = urlConnection.getResponseCode();
        if (HttpURLConnection.HTTP_OK == statusCode || HttpURLConnection.HTTP_ACCEPTED == statusCode) {
            response = readStream(new BufferedInputStream(urlConnection.getInputStream()));
        }  else {
            response = readStream(new BufferedInputStream(urlConnection.getErrorStream()));
            throw new Exception("Request failed with status code " + String.valueOf(statusCode) + ", body: " + response);
        }
        return response;
    }

    /**
     * Extends base URL with given parameters.
     * @param baseUrl URL to be extended.
     * @param urlParams Parameters to add.
     * @return Modified URL.
     */
    private static String urlWithParams(String baseUrl, List<KeyValue> urlParams) {
        if (null != baseUrl && null != urlParams && urlParams.size() > 0) {
            KeyValue pair = urlParams.get(0);
            baseUrl += URL_PARAM_START + pair.key + URL_PARAM_KEY_VALUE_DIVIDER + Uri.encode(pair.value);
            for (int i = 1; i < urlParams.size(); ++i) {
                pair = urlParams.get(i);
                baseUrl += URL_PARAM_DIVIDER + pair.key + URL_PARAM_KEY_VALUE_DIVIDER + Uri.encode(pair.value);
            }
        }
        return baseUrl;
    }
}
