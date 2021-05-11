package com.wusasmart.vas;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

//cool avtivity splashscreen
public class SplashscreenActivity extends AppCompatActivity {
    /** Splash screen duration time in milliseconds */
    private static final int DELAY = 1500;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splashscreen);

        // Jump to SensorsActivity after DELAY milliseconds
        new Handler().postDelayed(() -> {
            final Intent newIntent = new Intent(SplashscreenActivity.this, MainActivity.class);
            startActivity(newIntent);
            finish();
            overridePendingTransition(R.anim.stand,R.anim.splash);
        }, DELAY);
    }

    @Override
    public void onBackPressed() {
        // do nothing. Protect from exiting the application when splash screen is shown
    }


}
