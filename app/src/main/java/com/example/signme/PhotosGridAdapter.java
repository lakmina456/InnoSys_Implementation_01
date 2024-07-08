package com.example.signme;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.util.List;

public class PhotosGridAdapter extends BaseAdapter {

    private Context context;
    private List<String> imageUrlList;

    public PhotosGridAdapter(List<String> imageUrlList) {
        this.imageUrlList = imageUrlList;
    }

    @Override
    public int getCount() {
        return imageUrlList.size();
    }

    @Override
    public Object getItem(int position) {
        return imageUrlList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_photo_grid, parent, false);
        }

        ImageView imageView = convertView.findViewById(R.id.gridImageView);
        String imageUrl = imageUrlList.get(position);

        // Load image using Picasso
        Picasso.get()
                .load(imageUrl)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .into(imageView);

        return convertView;
    }
}
