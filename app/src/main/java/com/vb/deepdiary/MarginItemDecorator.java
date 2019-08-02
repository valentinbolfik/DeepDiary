package com.vb.deepdiary;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

class MarginItemDecorator extends RecyclerView.ItemDecoration {
    private final int spaceHeight;

    @SuppressWarnings("SameParameterValue")
    MarginItemDecorator(int spaceHeight) {
        this.spaceHeight = spaceHeight;
    }


    @SuppressWarnings("SuspiciousNameCombination")
    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        outRect.set(spaceHeight, spaceHeight, spaceHeight, spaceHeight);
        super.getItemOffsets(outRect, view, parent, state);
    }
}
