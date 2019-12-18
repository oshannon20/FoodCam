package com.victu.foodatory.gallery;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.SparseBooleanArray;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.victu.foodatory.R;
import com.victu.foodatory.gallery.model.DateItem;
import com.victu.foodatory.gallery.model.GeneralItem;
import com.victu.foodatory.gallery.model.ImageData;
import com.victu.foodatory.gallery.model.ListItem;

import java.util.ArrayList;
import java.util.List;

public class GalleryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Context mContext;
    List<ListItem> consolidatedList = new ArrayList<>();

    private GalleryAdapterListener listener;
    private SparseBooleanArray selectedItems;

    // index is used to animate only the selected row
    // dirty fix, find a better solution
    private static int currentSelectedIndex = -1;


    public interface GalleryAdapterListener {
        void onIconClicked(int position);

        void onPhotoClicked(int position);

        void onPhotoLongClicked(int position);
    }


    public GalleryAdapter(Context context, List<ListItem> consolidatedList, GalleryAdapterListener listener) {
        this.consolidatedList = consolidatedList;
        this.mContext = context;
        this.listener = listener;
        selectedItems = new SparseBooleanArray();
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder viewHolder = null;
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        switch (viewType) {

            case ListItem.TYPE_GENERAL:
                View v1 = inflater.inflate(R.layout.item_gallery, parent, false);
                viewHolder = new ImageViewHolder(v1);
                break;

            case ListItem.TYPE_DATE:
                View v2 = inflater.inflate(R.layout.item_image_date, parent, false);
                viewHolder = new DateViewHolder(v2);
                break;
        }

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        switch (holder.getItemViewType()) {

            // 일반 사진
            case ListItem.TYPE_GENERAL:

                GeneralItem generalItem = (GeneralItem) consolidatedList.get(position);
                ImageViewHolder imageViewHolder = (ImageViewHolder) holder;

                Glide.with(mContext).load(generalItem.getImageData().getFilePath()).into(imageViewHolder.photoImage);

                // change the row state to activated
                holder.itemView.setActivated(selectedItems.get(position, false));


                // apply click events
                applyClickEvents(imageViewHolder, position);

                applySelectionMode(imageViewHolder, position);

                break;

            case ListItem.TYPE_DATE:
                DateItem dateItem = (DateItem) consolidatedList.get(position);
                DateViewHolder dateViewHolder = (DateViewHolder) holder;

                dateViewHolder.textView.setText(dateItem.getDate());
                // Populate date item data here

                break;
        }
    }


    private void applyClickEvents(ImageViewHolder holder, final int position) {
        holder.checkImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onIconClicked(position);

            }
        });

        holder.photoImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onPhotoClicked(position);
            }
        });

        holder.photoImage.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                listener.onPhotoLongClicked(position);
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                return true;
            }
        });
    }


    private void applySelectionMode(ImageViewHolder holder, int position) {
        // 선택 아이콘 보임
        if (selectedItems.get(position, false)) {
            holder.checkImage.setVisibility(View.VISIBLE);
            holder.photoContainer.setForeground(new ColorDrawable(mContext.getColor(R.color.select_photo)));

            if (currentSelectedIndex == position) {
                resetCurrentIndex();
            }
        } else {
            holder.checkImage.setVisibility(View.GONE);

            if (currentSelectedIndex == position) {
                resetCurrentIndex();
            }

        }
    }


    @Override
    public int getItemCount() {
        return consolidatedList != null ? consolidatedList.size() : 0;
    }

    @Override
    public int getItemViewType(int position) {
        return consolidatedList.get(position).getType();
    }


    /**
     * 선택
     */

    public void toggleSelection(int pos) {
        currentSelectedIndex = pos;
        if (selectedItems.get(pos, false)) {
            selectedItems.delete(pos);
        } else {
            selectedItems.put(pos, true);
        }
        notifyItemChanged(pos);
    }

    public void clearSelections() {
        selectedItems.clear();
        notifyDataSetChanged();
    }

    public int getSelectedItemCount() {
        return selectedItems.size();
    }


    public List<Integer> getSelectedItems() {
        List<Integer> items =
                new ArrayList<>(selectedItems.size());
        for (int i = 0; i < selectedItems.size(); i++) {
            items.add(selectedItems.keyAt(i));
        }
        return items;
    }

    public void removeData(int position) {
        consolidatedList.remove(position);
        resetCurrentIndex();
    }


//    @Override
//    public long getItemId(int position) {
//        return messages.get(position).getId();
//    }

    private void resetCurrentIndex() {
        currentSelectedIndex = -1;
    }

    /**
     * 뷰홀더클래스
     */

    public class ImageViewHolder extends RecyclerView.ViewHolder implements View.OnLongClickListener {

        ImageView photoImage, checkImage, uncheckImage;
        RelativeLayout photoContainer;

        public ImageViewHolder(View view) {
            super(view);
            photoImage = view.findViewById(R.id.img_photo);
            checkImage = view.findViewById(R.id.img_check);
            uncheckImage = view.findViewById(R.id.img_unchecked);
            photoContainer = view.findViewById(R.id.photo_container);

            view.setOnLongClickListener(this);
        }

        @Override
        public boolean onLongClick(View view) {
            listener.onPhotoLongClicked(getAdapterPosition());
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            return true;
        }
    }


    public class DateViewHolder extends RecyclerView.ViewHolder {

        TextView textView;


        public DateViewHolder(View view) {
            super(view);
            textView = view.findViewById(R.id.txt_header);
        }
    }


}