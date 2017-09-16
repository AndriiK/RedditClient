package com.task.redditclient.ui;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.task.redditclient.R;
import com.task.redditclient.application.App;
import com.task.redditclient.model.json.Entry;
import com.task.redditclient.model.json.EntryData;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Adapter to display Reddit entries.
 */
class AdapterEntry extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    /**
     * Listener to receive adapter events.
     */
    interface Listener {
        /**
         * Called if scroll is reached to end.
         */
        void onScrolledToEnd();

        /**
         * Called on click by thumbnail image containing URL.
         * @param thumbnailUrl URL of clicked thumbnail.
         */
        void onThumbnailClick(String thumbnailUrl);
    }

    /** View type identifier for usual item */
    private static final int ENTRY = 0;
    /** View type identifier for loader item */
    private static final int LOADER = 1;

    /** Entries handled by this adapter */
    private List<Entry> mEntries;
    /** Listener to receive adapter events */
    private Listener mListener;
    /** Flag to show loader item */
    private boolean mShowLoader = false;

    /**
     * Constructor.
     * @param listener Listener to receive adapter events.
     */
    AdapterEntry(Listener listener) {
        mListener = listener;
    }

    /**
     * Sets entries to display.
     * @param entries Entry list.
     */
    void setEntries(List<Entry> entries) {
        mEntries = entries;
        notifyDataSetChanged();
    }

    /**
     * Shows/hide loader item.
     * @param show true to show loader item, false to hide.
     */
    void showLoader(boolean show) {
        mShowLoader = show;
    }

    /**
     * @see RecyclerView.Adapter#getItemCount()
     */
    @Override
    public int getItemCount() {
        return (null == mEntries ? 0 : mShowLoader ? (mEntries.size() + 1) : mEntries.size());
    }

    /**
     * @see RecyclerView.Adapter#getItemViewType(int)
     */
    @Override
    public int getItemViewType(int position) {
        return (null == mEntries || position < mEntries.size() ? ENTRY : LOADER);
    }

    /**
     * @see RecyclerView.Adapter#onCreateViewHolder(ViewGroup, int)
     */
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
        case ENTRY:
            return new EntryViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_entry, parent, false));
        case LOADER:
            return new LoaderViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_loader, parent, false));
        default:
            return null;
        }
    }

    /**
     * @see RecyclerView.Adapter#onBindViewHolder(RecyclerView.ViewHolder, int)
     */
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (position < mEntries.size()) {
            EntryData entryData = mEntries.get(position).data;
            if (null != entryData) {
                ((EntryViewHolder)holder).displayItem(entryData);
            }
        } else if (null != mListener) {
            mListener.onScrolledToEnd();
        }
    }

    /**
     * View holder to display regular entry.
     */
    private class EntryViewHolder extends    RecyclerView.ViewHolder
                                  implements View.OnClickListener {
        // UI controls.
        private final ViewGroup mContainerThumbnail;
        private final ImageView mImgThumbnail;
        private final TextView  mLblTitle;
        private final TextView  mLblAuthor;
        private final TextView  mLblCommentNum;
        private final TextView  mLblTime;

        /**
         * Constructor required by RecyclerView.ViewHolder
         * @param view Item view.
         */
        EntryViewHolder(View view) {
            super(view);
            mContainerThumbnail = view.findViewById(R.id.containerThumbnail);
            mContainerThumbnail.setOnClickListener(this);
            mImgThumbnail = mContainerThumbnail.findViewById(R.id.imgThumbnail);
            mLblTitle = view.findViewById(R.id.lblTitle);
            mLblAuthor = view.findViewById(R.id.lblAuthor);
            mLblCommentNum = view.findViewById(R.id.lblCommentNum);
            mLblTime = view.findViewById(R.id.lblTime);
        }

        /**
         * Displays single item.
         * @param item Entry to display.
         */
        private void displayItem(EntryData item) {
            App app = App.getInstance();
            Locale locale = Locale.getDefault();

            mContainerThumbnail.setTag(item.thumbnail);
            Glide.with(app).load(item.thumbnail).apply(new RequestOptions().centerCrop()).into(mImgThumbnail);

            mLblTitle.setText(item.title);
            mLblAuthor.setText(String.format(locale, app.getString(R.string.author), item.author));
            mLblCommentNum.setText(String.format(locale, app.getString(R.string.numComments), item.num_comments));
            long timeDiff = ((Calendar.getInstance().getTimeInMillis() / 1000) - item.created_utc) / 3600;
            mLblTime.setText(String.format(locale, app.getString(R.string.hoursAgo), timeDiff));
        }

        /**
         * @see View.OnClickListener#onClick(View)
         */
        @Override
        public void onClick(View v) {
            if (null != mListener) {
                String thumbnailUrl = (String)v.getTag();
                if (null != thumbnailUrl) {
                    mListener.onThumbnailClick(thumbnailUrl);
                }
            }
        }
    }

    /**
     * View holder to display loader item.
     */
    private class LoaderViewHolder extends RecyclerView.ViewHolder {
        /**
         * Constructor required by RecyclerView.ViewHolder.
         * @param view Item view.
         */
        private LoaderViewHolder(View view) {
            super(view);
        }
    }
}
