package com.wusasmart.vas;


import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;

/**
 * Created by too1 on 10.01.14.
 */
//didnt use in vas
public class SettingsActivity  extends AppCompatActivity {
    public static final String SETTINGS_PARAM_PWM_MIN = "SPAR_PWMMIN";
    public static final String SETTINGS_PARAM_PWM_MAX = "SPAR_PWMMAX";
    public static final String SETTINGS_PARAM_RED_THROTTLE = "SPAR_REDTHROTTLE";
    public int mPwmMin = 50, mPwmMax = 100;
    public boolean mReduceThrottle = true;
    private TextView mTextViewPwmMin, mTextViewPwmMax;
    private SeekBar mSeekBarPwmMin, mSeekBarPwmMax;
    private CheckBox mCheckBoxRedThrottle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
        mTextViewPwmMin = (TextView)findViewById(R.id.text_view_pwm_min);
        mTextViewPwmMax = (TextView)findViewById(R.id.text_view_pwm_max);
        mSeekBarPwmMin = (SeekBar)findViewById(R.id.seekbar_pwm_min);
        mSeekBarPwmMax = (SeekBar)findViewById(R.id.seekbar_pwm_max);
        mCheckBoxRedThrottle = (CheckBox)findViewById(R.id.checkbox_redthrottle);
        mPwmMin = getIntent().getExtras().getInt(SETTINGS_PARAM_PWM_MIN);
        mPwmMax = getIntent().getExtras().getInt(SETTINGS_PARAM_PWM_MAX);
        mReduceThrottle = getIntent().getExtras().getBoolean(SETTINGS_PARAM_RED_THROTTLE);
        mSeekBarPwmMin.setProgress(mPwmMin);
        mSeekBarPwmMax.setProgress(mPwmMax);
        mCheckBoxRedThrottle.setChecked(mReduceThrottle);
        setGuiText();

        mSeekBarPwmMin.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                mPwmMin = i;
                setGuiText();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        mSeekBarPwmMax.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                mPwmMax = i;
                setGuiText();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }
    private void setGuiText(){
        mTextViewPwmMin.setText(getString(R.string.settings_text_pwm_min_base) + " (" + String.valueOf(mPwmMin) + "%)");
        mTextViewPwmMax.setText(getString(R.string.settings_text_pwm_max_base) + " (" + String.valueOf(mPwmMax) + "%)");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onCheckboxThrottleClicked(View view) {
        mReduceThrottle = ((CheckBox)view).isChecked();
    }

    public void onOkPressed(View view) {
        Intent output = new Intent();
        output.putExtra(SETTINGS_PARAM_PWM_MIN, mPwmMin);
        output.putExtra(SETTINGS_PARAM_PWM_MAX, mPwmMax);
        output.putExtra(SETTINGS_PARAM_RED_THROTTLE, mReduceThrottle);
        setResult(RESULT_OK, output);
        finish();
    }
}