package com.example.signme;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashScreenActivity extends AppCompatActivity {

    private ImageView imageA, imageB, imageC;
    private TextView logoText, taglineText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);

        imageA = findViewById(R.id.imageA);
        imageB = findViewById(R.id.imageB);
        imageC = findViewById(R.id.imageC);
        logoText = findViewById(R.id.logoText);
        taglineText = findViewById(R.id.taglineText);

        // Set zooming animation for images a.png, b.png, c.png
        Animation zoomAnimation = AnimationUtils.loadAnimation(this, R.anim.zoom_animation);
        imageA.setVisibility(android.view.View.VISIBLE);
        imageA.startAnimation(zoomAnimation);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                imageB.setVisibility(android.view.View.VISIBLE);
                imageB.startAnimation(zoomAnimation);
            }
        }, 1000); // Delay for imageB

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                imageC.setVisibility(android.view.View.VISIBLE);
                imageC.startAnimation(zoomAnimation);
            }
        }, 2000); // Delay for imageC

        // Set fade-in animation for logoText and taglineText
        Animation fadeInAnimation = new AlphaAnimation(0, 1);
        fadeInAnimation.setDuration(2000); // Duration in milliseconds
        fadeInAnimation.setStartOffset(3000); // Delay before animation starts
        logoText.setVisibility(android.view.View.VISIBLE);
        logoText.startAnimation(fadeInAnimation);
        taglineText.setVisibility(android.view.View.VISIBLE);
        taglineText.startAnimation(fadeInAnimation);

        // Move to next activity after splash screen finishes
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent mainIntent = new Intent(SplashScreenActivity.this, MainActivity.class);
                startActivity(mainIntent);
                finish(); // Finish splash screen activity
            }
        }, 6000); // 6 seconds delay, adjust as needed
    }
}
