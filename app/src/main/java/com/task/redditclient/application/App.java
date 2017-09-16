package com.task.redditclient.application;

import android.app.Application;

import com.task.redditclient.net.Engine;

/**
 * The Application class to maintain global application state.
 */
public class App extends Application {
    /** Application to access application resources (except graphical) everywhere. */
    private static App sSelf;

    /** Shared data storage */
    private Storage mStorage;

    /** Network engine. */
    private Engine mEngine;

    /**
     * @see Application#onCreate()
     */
    @Override
    public void onCreate() {
        super.onCreate();
        sSelf = this;

        mStorage = new Storage();
        mEngine = new Engine();
    }

    /**
     * Gets itself to access application resources
     * @return application instance
     */
    public static App getInstance() {
        return sSelf;
    }

    /**
     * Gets shared data storage
     * @return shared data storage
     */
    public static Storage getStorage() {
        return sSelf.mStorage;
    }

    /**
     * Gets network engine
     * @return network engine
     */
    public static Engine getEngine() {
        return sSelf.mEngine;
    }
}
