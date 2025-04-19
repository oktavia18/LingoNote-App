package com.example.multilingualnotes;


import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 3000; // 3 seconds
    private ImageView logoImage;
    private TextView appNameText;
    private TextView taglineText;
    private ProgressBar progressBar;
    private TextView versionText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set fullscreen
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        // Hide status bar
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        setContentView(R.layout.splash_screen);

        // Initialize views
        logoImage = findViewById(R.id.logo_image);
        appNameText = findViewById(R.id.app_name);
        taglineText = findViewById(R.id.tagline);
        progressBar = findViewById(R.id.progress_bar);
        versionText = findViewById(R.id.version_text);

        // Apply animations
        startAnimations();

        // Navigate to main activity after delay
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        }, SPLASH_DURATION);
    }

    private void startAnimations() {
        // Fade in animation
        Animation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(1000);

        // Apply animation to logo
        logoImage.startAnimation(fadeIn);

        // Apply animation to text with delay
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                Animation textFadeIn = new AlphaAnimation(0.0f, 1.0f);
                textFadeIn.setDuration(800);

                appNameText.setVisibility(View.VISIBLE);
                appNameText.startAnimation(textFadeIn);

                // Show tagline after app name
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        taglineText.setVisibility(View.VISIBLE);
                        taglineText.startAnimation(textFadeIn);

                        // Show progress bar after tagline
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setVisibility(View.VISIBLE);
                                versionText.setVisibility(View.VISIBLE);
                            }
                        }, 300);
                    }
                }, 300);
            }
        }, 400);
    }
}

