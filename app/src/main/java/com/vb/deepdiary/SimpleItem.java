package com.vb.deepdiary;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.items.AbstractItem;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

class SimpleItem extends AbstractItem<SimpleItem, SimpleItem.ViewHolder> implements Comparable<SimpleItem> {
    private String title;
    private String content;
    private String ID;
    private String dateTime;
    private LocalDateTime localDateTime;

    SimpleItem withTitle(String title) {
        this.title = title;
        return this;
    }

    SimpleItem withContent(String content) {
        this.content = content;
        return this;
    }

    SimpleItem withID(String ID) {
        this.ID = ID;
        return this;
    }

    SimpleItem withDateTime(String dateTime) {
        this.dateTime = dateTime;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy_HH:mm");
        this.localDateTime = LocalDateTime.parse(dateTime, formatter);
        return this;
    }

    String getTitle() {
        return title;
    }

    String getContent() {
        return content;
    }

    String getID() {
        return ID;
    }

    String getDateTime() {
        return dateTime;
    }

    private LocalDateTime getLocalDateTime() {
        return localDateTime;
    }

    //The unique ID for this type of item
    @Override
    public int getType() {
        return R.id.fastadapter_item_id;
    }

    //The layout to be used for this type of item
    @Override
    public int getLayoutRes() {
        return R.layout.recycler_view_item;
    }

    @NonNull
    @Override
    public ViewHolder getViewHolder(@NonNull View v) {
        return new ViewHolder(v);
    }

    //Compare two items, used for sorting
    @Override
    public int compareTo(@NonNull SimpleItem simpleItem) {
        return getLocalDateTime().compareTo(simpleItem.getLocalDateTime());
    }

    /**
     * our ViewHolder
     */
    protected static class ViewHolder extends FastAdapter.ViewHolder<SimpleItem> {
        @BindView(R.id.textView_content) TextView content;
        @BindView(R.id.textView_title) TextView title;

        ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }

        @Override
        public void bindView(@NonNull SimpleItem item, @NonNull List<Object> payloads) {
            content.setText(item.content);
            title.setText(item.title);
        }

        @Override
        public void unbindView(@NonNull SimpleItem item) {
            content.setText(null);
            title.setText(null);
        }
    }
}