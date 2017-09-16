package com.task.redditclient.application;

import com.task.redditclient.model.json.Entry;

import java.util.List;

/**
 * Shared storage to keep application data.
 */
public class Storage {
    /** Received entries */
    private List<Entry> mEntries;

    /** The next entry ID from the last "Get entry" response */
    private String mAfter;

    /**
     * Gets received entries.
     * @return Array of entries or null if not recived yet.
     */
    public List<Entry> getEntries() {
        return mEntries;
    }

    /**
     * Gets entry ID for the next page.
     * @return entry ID or null if it is not received yet.
     */
    public String getAfter() {
        return mAfter;
    }

    /**
     * Adds received entries.
     * @param entries Array of received entries.
     * @param after The next entry ID.
     * @param clearPrevious if true existing entries will be removed.
     */
    public synchronized void addEntries(List<Entry> entries, String after, boolean clearPrevious) {
        if (null == mEntries) {
            mEntries = entries;
        } else {
            if (clearPrevious) {
                mEntries.clear();
            }
            mEntries.addAll(entries);
        }
        mAfter = after;
    }
}
