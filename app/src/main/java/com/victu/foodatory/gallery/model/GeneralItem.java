package com.victu.foodatory.gallery.model;


public class GeneralItem extends ListItem {
    private ImageData imageData;

    public ImageData getImageData() {
        return imageData;
    }

    public void setImageData(ImageData imageData) {
        this.imageData = imageData;
    }

    @Override
    public int getType() {
        return TYPE_GENERAL;
    }


}