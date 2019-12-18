package com.victu.foodatory.temp;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public interface OnItemInteractionListener {

    void onItemClicked(RecyclerView recyclerView, RecyclerView.ViewHolder mViewHolderTouched, int position);

    void onMultipleViewHoldersSelected(RecyclerView recyclerView, List<RecyclerView.ViewHolder> selection);

    void onLongItemClicked(RecyclerView recyclerView, RecyclerView.ViewHolder mViewHolderTouched, int position);

    void onViewHolderHovered(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder);
}
