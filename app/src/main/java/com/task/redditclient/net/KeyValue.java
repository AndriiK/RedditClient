package com.task.redditclient.net;

/**
 * Container for simple key value pair.
 */
class KeyValue {
    String key;
    String value;

    /**
     * Constructor.
     * @param key Key field.
     * @param value Value field.
     */
    KeyValue(String key, String value) {
        this.key = key;
        this.value = value;
    }
}
