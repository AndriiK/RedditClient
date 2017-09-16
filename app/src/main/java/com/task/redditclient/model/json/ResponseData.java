package com.task.redditclient.model.json;

/**
 *  JSON container for "Get entries" response data.
 */
public class ResponseData {
    public String  modhash;
    public String  after;
    public String  before;
    public Entry[] children;
}
