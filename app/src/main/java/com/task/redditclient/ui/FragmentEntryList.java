package com.task.redditclient.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.task.redditclient.R;
import com.task.redditclient.application.App;
import com.task.redditclient.application.Common;
import com.task.redditclient.model.json.Entry;
import com.task.redditclient.net.Engine;

import java.util.List;

import static com.task.redditclient.net.Engine.Action.GET_ENTRIES;

/**
 * Fragment to display list of received entities.
 */
public class FragmentEntryList extends    Fragment
                               implements SwipeRefreshLayout.OnRefreshListener,
                                          AdapterEntry.Listener,
                                          Engine.Listener {
    /** Adapter to handle entry array*/
    private AdapterEntry mAdapter;
    /** Layout to handle "Swipe to refresh" behaviour */
    private SwipeRefreshLayout mSwipeRefreshLayout;

    /**
     * @see Fragment#onCreateView(LayoutInflater, ViewGroup, Bundle)
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (null == container) {
            return null;
        }

        View view = inflater.inflate(R.layout.fragment_entry_list, container, false);

        RecyclerView list = view.findViewById(R.id.listEntries);
        list.setLayoutManager(new LinearLayoutManager(getActivity()));
        mAdapter = new AdapterEntry(this);
        list.setAdapter(mAdapter);

        mSwipeRefreshLayout = view.findViewById(R.id.swiperefresh);
        mSwipeRefreshLayout.setOnRefreshListener(this);

        return view;
    }

    /**
     * @see Fragment#onResume()
     */
    @Override
    public void onResume() {
        super.onResume();

        Engine engine = App.getEngine();
        engine.addListener(this);

        List<Entry> entries = App.getStorage().getEntries();
        if (null == entries) {
            mSwipeRefreshLayout.setRefreshing(true);
            engine.getEntries(null);
        } else {
            mAdapter.setEntries(entries);
            mAdapter.showLoader(entries.size() < Common.MAX_ENTRIES);
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
     * @see SwipeRefreshLayout.OnRefreshListener#onRefresh()
     */
    @Override
    public void onRefresh() {
        App.getEngine().getEntries(null);
        mAdapter.showLoader(false);
    }

    /**
     * @see AdapterEntry.Listener#onScrolledToEnd()
     */
    @Override
    public void onScrolledToEnd() {
        Engine engine = App.getEngine();
        if (!engine.isActionExecuted(GET_ENTRIES)) {
            engine.getEntries(App.getStorage().getAfter());
        }
    }

    /**
     * @see AdapterEntry.Listener#onThumbnailClick(String)
     */
    @Override
    public void onThumbnailClick(String thumbnailUrl) {
        ((ActivityMain)getActivity()).previewImage(thumbnailUrl);
    }

    /**
     * @see Engine.Listener#onRequestCompleted(Engine.Action, Object...)
     */
    @Override
    public void onRequestCompleted(Engine.Action action, Object... data) {
        if (GET_ENTRIES == action) {
            List<Entry> entries = App.getStorage().getEntries();
            mSwipeRefreshLayout.setRefreshing(false);
            mAdapter.showLoader(entries.size() < Common.MAX_ENTRIES);
            mAdapter.setEntries(entries);
        }
    }

    /**
     * @see Engine.Listener#onRequestFailed(Engine.Action, Exception)
     */
    @Override
    public void onRequestFailed(Engine.Action action, Exception e) {
        if (GET_ENTRIES == action) {
            mSwipeRefreshLayout.setRefreshing(false);
            mAdapter.showLoader(false);
            ((ActivityMain)getActivity()).showMessage(R.string.requestFailed, null);
        }
    }
}
