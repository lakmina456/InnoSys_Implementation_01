package com.example.signme;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

public class ViewImageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_image);

        // Initialize views
        ImageView imageView = findViewById(R.id.imageView);

        // Receive image URL from intent
        if (getIntent() != null) {
            String imageUrl = getIntent().getStringExtra("imageUrl");

            // Load full-size image using Picasso
            Picasso.get()
                    .load(imageUrl)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .into(imageView, new Callback() {
                        @Override
                        public void onSuccess() {
                            // Image loaded successfully
                        }

                        @Override
                        public void onError(Exception e) {
                            // Error loading image
                            e.printStackTrace();
                        }
                    });
        }
    }
}
