/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wusasmart.vas;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.icu.util.Calendar;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.android.material.navigation.NavigationView;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static com.google.common.reflect.Reflection.initialize;

//import android.support.v4.app.Fragment;



//WEN//
//WEN//


public class MainActivity extends AppCompatActivity implements RadioGroup.OnCheckedChangeListener {
    private static final int REQUEST_SELECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int UART_PROFILE_READY = 10;
    public static final String TAG = "image_transfer_main";
    private static final int UART_PROFILE_CONNECTED = 20;
    private static final int UART_PROFILE_DISCONNECTED = 21;
    private static final int STATE_OFF = 10;
    private static final int SETTINGS_ACTIVITY = 100;

    private static final String FONT_LABEL_APP_NORMAL = "<font color='#EE0000'>";
    private static final String FONT_LABEL_APP_ERROR = "<font color='#EE0000'>";
    private static final String FONT_LABEL_PEER_NORMAL = "<font color='#EE0000'>";
    private static final String FONT_LABEL_PEER_ERROR = "<font color='#EE0000'>";



    public enum AppLogFontType {APP_NORMAL, APP_ERROR, PEER_NORMAL, PEER_ERROR};
    private String mLogMessage = "";

    private TextView mTextViewLog, mTextViewFileLabel, mTextViewPictureStatus, mTextViewPictureFpsStatus, mTextViewConInt, mTextViewMtu;
    private Button mBtnTakePicture, mBtnStartStream;
    private ProgressBar mProgressBarFileStatus;
    private ImageView mMainImage;
    private Spinner mSpinnerResolution, mSpinnerPhy;

    private int mState = UART_PROFILE_DISCONNECTED;
    private ImageTransferService mService = null;

    private BluetoothDevice mDevice = null;
    private BluetoothAdapter mBtAdapter = null;
    private Button btnConnectDisconnect;
    private boolean mMtuRequested;
    private byte []mUartData = new byte[6];
    private long mStartTimeImageTransfer;


    // File transfer variables
    private int mBytesTransfered = 0, mBytesTotal = 0;
    private int mBytesBeeper=0;
    private byte []mDataBuffer;
    private byte []mBeeperBuffer;
    private boolean mStreamActive = false;

    private ProgressDialog mConnectionProgDialog;

    public enum AppRunMode {Disconnected, Connected, ConnectedDuringSingleTransfer, ConnectedDuringStream};
    public enum BleCommand {NoCommand, StartSingleCapture, StartStreaming, StopStreaming, ChangeResolution, ChangePhy, GetBleParams,SendMP3};


    //WEN_SOUNDPOOL//
    private SoundPool soundpool;
    private Map<Integer, Integer> soundmap = new HashMap<Integer, Integer>();
    //WEN_SOUNDPOOL//

    //KENNY*************************//


    //detectlabel service
    private detectlabel detectService = null;
    //passing intent
    //Intent detectintent = new Intent(this, com.wusasmart.vas.detectlabel.class);

    //login google drive parameter
    private static final int REQUEST_CODE_SIGN_IN = 3;
    public DriveServiceHelper mDriveServiceHelper;
    public String mOpenFileId;

    //tts service parameter
    private static TextToSpeech maintts;

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    //KENNY*************************//


    Handler guiUpdateHandler = new Handler();
    Runnable guiUpdateRunnable = new Runnable(){
        @Override
        public void run(){
            if(mTextViewFileLabel != null) {
                mTextViewFileLabel.setText("Incoming: " + String.valueOf(mBytesTransfered) + "/" + String.valueOf(mBytesTotal));
                if(mBytesTotal > 0) {
                    mProgressBarFileStatus.setProgress(mBytesTransfered * 100 / mBytesTotal);
                }
            }
            guiUpdateHandler.postDelayed(this, 50);
        }
    };
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        if (!ensureBLEExists())//Same as check BluetoothAdapter.getDefaultAdapter()
            finish();
/*
            NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
            AppBarConfiguration appBarConfiguration =
                    new AppBarConfiguration.Builder(navController.getGraph()).build();
            Toolbar toolbar = findViewById(R.id.toolbar);
            NavigationUI.setupWithNavController(toolbar, navController, appBarConfiguration);


 */

/*
        final DrawerLayout drawer = drawerLayout = findViewById(R.id.drawer_layout);
        drawer.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        // Set the drawer toggle as the DrawerListener
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.drawer_open, R.string.drawer_close) {
            @Override
            public void onDrawerSlide(final View drawerView, final float slideOffset) {
                // Disable the Hamburger icon animation
                super.onDrawerSlide(drawerView, 0);
            }
        };
        drawer.addDrawerListener(drawerToggle);

 */
/*
        try {
           // checkPermissions();

        }catch(Exception e){
            Log.w("Permissions","requestfail");

        }

 */

       // toolbar.showOverflowMenu();
       // MainActivity.this.openOptionsMenu();
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //getSupportActionBar().setHomeAsUpIndicator(R.drawable.icon_cam);
        /*
        ActionBar actionBar= getSupportActionBar();
        actionBar.setIcon(R.drawable.icon_cam);
        actionBar.setDisplayShowHomeEnabled(true);
        */

        //getSupportActionBar().setIcon(R.mipmap.ic_launcher);
       // getSupportActionBar().setTitle("Vas");
        //change to chinese
        //cant use due to settext
        /*
        Resources resources = getResources();
        Configuration config = resources.getConfiguration();
        DisplayMetrics dm = resources.getDisplayMetrics();
        config.locale = Locale.ENGLISH;
        //config.locale= Locale.TRADITIONAL_CHINESE;
        resources.updateConfiguration(config, dm);
         */

        //change to chinese

        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        btnConnectDisconnect    = (Button) findViewById(R.id.btn_select);
        mTextViewLog = (TextView)findViewById(R.id.textViewLog);
        mTextViewFileLabel = (TextView)findViewById(R.id.textViewFileLabel);
        mTextViewPictureStatus = (TextView)findViewById(R.id.textViewImageStatus);
        mTextViewPictureFpsStatus = (TextView)findViewById(R.id.textViewImageFpsStatus);
        mTextViewConInt = (TextView)findViewById(R.id.textViewCI);
        mTextViewMtu = (TextView)findViewById(R.id.textViewMTU);
        mProgressBarFileStatus = (ProgressBar)findViewById(R.id.progressBarFile);
        mBtnTakePicture = (Button)findViewById(R.id.buttonTakePicture);
        mBtnStartStream = (Button)findViewById(R.id.buttonStartStream);
        mMainImage = (ImageView)findViewById(R.id.imageTransfered);
        mSpinnerResolution = (Spinner)findViewById(R.id.spinnerResolution);
        mSpinnerResolution.setSelection(1);
        mSpinnerPhy = (Spinner)findViewById(R.id.spinnerPhy);
        //mConnectionProgDialog = new ProgressDialog(this);
        //mConnectionProgDialog=DialogUtils.showProgressDialog(this,"Connecting...");


        //mConnectionProgDialog.setTitle("Connecting...");
        //mConnectionProgDialog.setCancelable(false);
       // mMenu=findViewById(R.id.action_settings);
        /*
        NavigationView navigationView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        private AppBarConfiguration mAppBarConfiguration;
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery)
                .setDrawerLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

         */
        //mainActivity tts initial
        maintts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = maintts.setLanguage(Locale.getDefault());//as system language
                    startApp();
                    if (result == TextToSpeech.LANG_MISSING_DATA ||
                            result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    }

                }
            }
        });






        //WEN_SOUNDPOOL//
        if(Build.VERSION.SDK_INT > 21){
            SoundPool.Builder builder = new SoundPool.Builder();
            //传入音频数量
            builder.setMaxStreams(5);
            //AudioAttributes是一个封装音频各种属性的方法
            AudioAttributes.Builder attrBuilder = new AudioAttributes.Builder();
            //设置音频流的合适的属性
            attrBuilder.setLegacyStreamType(AudioManager.STREAM_SYSTEM);//STREAM_MUSIC
            //加载一个AudioAttributes
            builder.setAudioAttributes(attrBuilder.build());
            soundpool = builder.build();
        }else{
            soundpool = new SoundPool(5, AudioManager.STREAM_SYSTEM, 0);
        }
        //
        soundmap.put(1, soundpool.load(this, R.raw.connected, 1));
        //WEN_SOUNDPOOL//





        // create detectlabel service intent
        Intent detectintent = new Intent(this, com.wusasmart.vas.detectlabel.class);

        service_init();// nordic image transfer service
        requestStoragePremission();
        requestLocationPremission();
        requestSignIn();//request google drive login function


        //Intent detectintent = new Intent(this, detectlabel.class);
        //Bundle b=new Bundle();
        //b.putSerializable("key",mDriveServiceHelper);
        //intent.putExtra("test",mDriveServiceHelper);

        /*
        Bundle b = new Bundle();
        b.putParcelable("data", (Parcelable) mDriveServiceHelper);
        intent.putExtras(b);
         */

        //pass google Drivehelper to detectlabel service
        detectintent.putExtra("input", (Parcelable) mDriveServiceHelper);

//same as service_init() but it's for detectlabel service


        bindService(detectintent, mlabelServiceConnection, BIND_AUTO_CREATE);
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(messageReceiver, new IntentFilter("testdata"));
        //read action testdata from  detectlabel,detectlabel will send the data back to main via output parameter
        startService(detectintent);//same as startActivity but for service

//same as service_init() but it's for detectlabel service
        Timer myTimer = new Timer ();
        TimerTask myTask = new TimerTask() {
            @Override
            public void run () {
                // your code
                String timeLog = "";
                int week = Calendar.getInstance().get(Calendar.WEEK_OF_YEAR);
                String strweek = String.valueOf(Calendar.getInstance().get(Calendar.WEEK_OF_YEAR));
                int day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1;
                int time = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

                String myDevice = Build.DEVICE;

                if (week < 10) {
                    strweek = "0" + strweek;
                    timeLog = "1" + strweek + day + time + myDevice;
                } else {
                    timeLog = "1" + week + day + time + myDevice;
                }
                File timelogpath = new File("/storage/emulated/0/VAS/" + timeLog + ".txt");
                if(timelogpath.exists()){
                try (BufferedReader br = new BufferedReader(new FileReader("/storage/emulated/0/VAS/" + timeLog + ".txt"))) {
                    StringBuilder sb = new StringBuilder();
                    String line = br.readLine();

                    while (line != null) {
                        sb.append(line);
                        sb.append(System.lineSeparator());
                        line = br.readLine();
                    }
                    String everything = sb.toString();
                    createFile(everything);
                   // saveFile(everything,mOpenFileId);
                    try {
                        String deleteCmd = "rm -r " + timelogpath;
                        Runtime runtime = Runtime.getRuntime();
                        runtime.exec(deleteCmd);
                    } catch (FileNotFoundException e) {
                        Log.e("NOTFOUND", "file notfound");
                    } catch (IOException e) {
                        Log.e("IOERROR", "some IO error");
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
                else{
                    Log.e("savefile","file doesn't exist");
                }
            }
        };
        myTimer.scheduleAtFixedRate(myTask , 0l, 5 * (60*1000)); // Runs every 5 mins

        //Intent intent = new Intent(this, detectlabel.class);
        //startActivity(intent);
        for(int i = 0; i < 6; i++) mUartData[i] = 0;

        // Handler Disconnect & Connect button
        btnConnectDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Utils.isInvalidClick(v, 5000))
                    return;
            btnConnectDisconnect.setEnabled(false);
            btnConnectDisconnect.postDelayed(new Runnable() {
                @Override
                        public void run(){
                    btnConnectDisconnect.setEnabled(true);
                    }
                }, 10000);
                if (!mBtAdapter.isEnabled()) {
                    Log.i(TAG, "onClick - BT not enabled yet");
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                } else {
                    if (btnConnectDisconnect.getText().equals("Connect")) {
                        //Connect button pressed, open DeviceListActivity class, with popup windows that scan for devices
                        Intent newIntent = new Intent(com.wusasmart.vas.MainActivity.this, com.wusasmart.vas.DeviceListActivity.class);
                        startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
                        //
                        //mConnectionProgDialog.cancel();
                    } else {
                        //Disconnect button pressed
                        if (mDevice != null) {
                            mService.disconnect();
                        }
                    }
                }
            }
        });
//uartsendcommand starts from here   send command to nrf52832 via enum 0~7 device do something via command
// ************************************************* * * * * *
        mBtnTakePicture.setOnClickListener(new View.OnClickListener() { //when take picture button pressed
            @Override
            public void onClick(View v) {
                if (Utils.isInvalidClick(v, 5000))
                    return;
                if(mService != null){
                    mService.sendCommand(BleCommand.StartSingleCapture.ordinal(), null);
                    setGuiByAppMode(AppRunMode.ConnectedDuringSingleTransfer);
                }
            }
        });

        mBtnStartStream.setOnClickListener(new View.OnClickListener() {//when start button pressed
            @Override
            public void onClick(View v) {
                if (Utils.isInvalidClick(v, 5000))
                    return;
                if(mService != null){
                    if(!mStreamActive) {
                        mStreamActive = true;

                        mService.sendCommand(BleCommand.StartStreaming.ordinal(), null);
                        setGuiByAppMode(AppRunMode.ConnectedDuringStream);
                    }
                    else {
                        mStreamActive = false;

                        mService.sendCommand(BleCommand.StopStreaming.ordinal(), null);
                        setGuiByAppMode(AppRunMode.Connected);
                    }
                }
            }
        });

        mSpinnerResolution.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {//when resolution item selected
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if(mService != null && mService.isConnected()){
                    byte []cmdData = new byte[1];
                    cmdData[0] = (byte)position;
                    mService.sendCommand(BleCommand.ChangeResolution.ordinal(), cmdData);
                    Log.w("sendcommand", String.valueOf(BleCommand.ChangeResolution.ordinal()));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        mSpinnerPhy.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {// when phy change
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if(mService != null && mService.isConnected()){
                    byte []cmdData = new byte[1];
                    cmdData[0] = (byte)position;
                    mService.sendCommand(BleCommand.ChangePhy.ordinal(), cmdData);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });
        /*
        @Override
        protected boolean onOptionsItemSelected(final int itemId) {
            switch (itemId) {
                case R.id.action_settings:
                    final Intent intent3 = new Intent(this, SettingsActivity.class);
                    startActivity(intent);
                    break;
            }
        }
        */

        // Set initial UI state
        guiUpdateHandler.postDelayed(guiUpdateRunnable, 0);

        setGuiByAppMode(AppRunMode.Disconnected);
    }
    //first open broadcast an introduction else welcome back
    private void startApp() {

        SharedPreferences ratePrefs=getSharedPreferences("First Update", 0);

        if(!ratePrefs.getBoolean("FirstTime",false)){
            mobile_speak(getString(R.string.install_speeh));
            SharedPreferences.Editor edit=ratePrefs.edit();
            edit.putBoolean("FirstTime",true);
            edit.commit();
        }
        else {
            //AudioManager.
            /*



             */
            //test tone_speak
            //tone_speak(100,125);


            mobile_speak("主人，歡迎您回來");
        }
    }
    public static void mobile_speak(String str) {//tts service packed

        //maintts.setPitch((float) 1.2);
        maintts.setSpeechRate((float) 0.95);
        maintts.speak(str, TextToSpeech.QUEUE_FLUSH, null);
    }
    //for tone bebebe
    public static void tone_speak(int volume,int duration ){
        ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_SYSTEM, volume);
        //ToneGenerator toneGen2 = new ToneGenerator(AudioManager.STREAM_MUSIC,volume);
        //volume default 100
       // toneGen1.startTone(ToneGenerator.TONE_PROP_BEEP2,duration);
        //toneGen1.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT,duration);
        toneGen1.startTone(ToneGenerator.TONE_SUP_RINGTONE,duration);
        /*
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                toneGen1.startTone(ToneGenerator.TONE_SUP_RINGTONE,duration);
                // Do something after 5s = 5000ms

            }
        }, 50);

         */

        //toneGen1.startTone(ToneGenerator.TONE_CDMA_DIAL_TONE_LITE,duration);



        //duration /ms
    }

    //UART service connected/disconnected
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            mService = ((ImageTransferService.LocalBinder) rawBinder).getService();
            Log.d(TAG, "onServiceConnected mService= " + mService);
            if (!mService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
        }




        public void onServiceDisconnected(ComponentName classname) {
       ////     mService.disconnect(mDevice);
        		mService = null;
        }
    };


    //detectlabel service
    private ServiceConnection mlabelServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            detectService = ((com.wusasmart.vas.detectlabel.detectBinder) service).getService();
            Log.d(TAG, "onServiceConnected mService= " + detectService);
            //createFile();
            //saveFile();
        }

        public void onServiceDisconnected(ComponentName classname) {
            ////     mService.disconnect(mDevice);
            detectService = null;
        }
    };


    private void setGuiByAppMode(AppRunMode appMode)
    {
        switch(appMode)
        {
            case Connected:
                mBtnTakePicture.setEnabled(true);
                mBtnStartStream.setEnabled(true);
                btnConnectDisconnect.setEnabled(false);
                btnConnectDisconnect.postDelayed(new Runnable() {
                    @Override
                    public void run(){
                        btnConnectDisconnect.setEnabled(true);
                    }
                }, 1000);
                btnConnectDisconnect.setText("Disconnect");
//                mBtnStartStream.setText("Start Stream");
                mBtnStartStream.setText("Zoom In");
                mSpinnerResolution.setEnabled(true);
                mSpinnerPhy.setEnabled(true);
                //btnConnectDisconnect.setEnabled(true);

                break;

            case Disconnected:

                btnConnectDisconnect.setText("Connect");
//                mBtnStartStream.setText("Start Stream");
                btnConnectDisconnect.setEnabled(false);
                btnConnectDisconnect.postDelayed(new Runnable() {
                    @Override
                    public void run(){
                        btnConnectDisconnect.setEnabled(true);
                    }
                }, 5000);
                mBtnTakePicture.setEnabled(false);
                mBtnStartStream.setEnabled(false);
                mBtnStartStream.setText("Zoom In");
                mTextViewPictureStatus.setVisibility(View.INVISIBLE);
                mTextViewPictureFpsStatus.setVisibility(View.INVISIBLE);
                mSpinnerResolution.setEnabled(false);
                mSpinnerResolution.setSelection(1);
                mSpinnerPhy.setEnabled(false);
                mSpinnerPhy.setSelection(0);
                break;

            case ConnectedDuringSingleTransfer:
//                mBtnTakePicture.setEnabled(false);
//                mBtnStartStream.setEnabled(false);
                mBtnTakePicture.setEnabled(true);
                mBtnStartStream.setEnabled(true);
                break;

            case ConnectedDuringStream:
//                mBtnTakePicture.setEnabled(false);
                mBtnTakePicture.setEnabled(true);
                mBtnStartStream.setEnabled(true);
//                mBtnStartStream.setText("Stop Stream");
                mBtnStartStream.setText("Normal");
                break;
        }
    }

    private void writeToLog(String message, AppLogFontType msgType){
        String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
        String newMessage = currentDateTimeString + " - " + message;
        String fontHtmlTag;
        switch(msgType){
            case APP_NORMAL:
                fontHtmlTag = "<font color='#000000'>";
                break;
            case APP_ERROR:
                fontHtmlTag = "<font color='#AA0000'>";
                break;
            case PEER_NORMAL:
                fontHtmlTag = "<font color='#0000AA'>";
                break;
            case PEER_ERROR:
                fontHtmlTag = "<font color='#FF00AA'>";
                break;
            default:
                fontHtmlTag = "<font>";
                break;
        }
        mLogMessage = fontHtmlTag + newMessage + "</font>" + "<br>" + mLogMessage;
        mTextViewLog.setText(Html.fromHtml(mLogMessage));
    }
    //detectlabel broadcast receiver
    private final BroadcastReceiver messageReceiver = new BroadcastReceiver() {


            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                final Intent mIntent = intent;
                // Extract data included in the Intent
                String test = intent.getStringExtra("output"); // -1 is going to be used as the default value

                 BufferedWriter writer = null;

                try {
                    String timeLog ="";
                    //create a temporary file
                    int week= Calendar.getInstance().get(Calendar.WEEK_OF_YEAR);
                    String strweek=String.valueOf(Calendar.getInstance().get(Calendar.WEEK_OF_YEAR));
                    int day=Calendar.getInstance().get(Calendar.DAY_OF_WEEK)-1;
                    int time=Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

                    String myDevice = Build.DEVICE;

                    if (week<10)
                    {
                        strweek="0"+strweek;
                        timeLog = "1"+strweek+day+time+myDevice;
                    }
                    else
                    {
                        timeLog = "1"+week+day+time+myDevice;
                    }

                    File logFile = new File("/storage/emulated/0/VAS/",timeLog+".txt");

                    // This will output the full path where the file will be written to...
                    System.out.println(logFile.getCanonicalPath());

                    writer = new BufferedWriter(new FileWriter(logFile,true));
                    writer.write(test+"\n");

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        // Close the writer regardless of what happens...
                        writer.close();
                    } catch (Exception e) {
                    }
                }


            }
        };

    private final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        final Intent mIntent = intent;
        //*********************//
            //line1013intent filters add all actions when GATT update
            // imagetransferservice line85 broadcastupdate in imagetransfer function is the update action function
        if (action.equals(ImageTransferService.ACTION_GATT_CONNECTED)) {
            runOnUiThread(new Runnable() {
                public void run() {
                    mMtuRequested = false;
                    //mConnectionProgDialog.hide();

                    mConnectionProgDialog.dismiss();
                    //mConnectionProgDialog=null;
                    Log.d(TAG, "UART_CONNECT_MSG");
                    writeToLog("Connected", AppLogFontType.APP_NORMAL);


                    btnConnectDisconnect.setText("Disconnect");
                    btnConnectDisconnect.setEnabled(false);
                    //WEN_SOUNDPOOL//
                    soundpool.play(soundmap.get(1), 1, 1, 0, 0, 1);
                    Log.e("WEN", "Connected");
                    //WEN_SOUNDPOOL//

                    mobile_speak("裝置已連線");

                }
            });
        }

          //*********************//
        if (action.equals(ImageTransferService.ACTION_GATT_DISCONNECTED)) {
            runOnUiThread(new Runnable() {
                public void run() {
                    setGuiByAppMode(AppRunMode.Disconnected);
                    mState = UART_PROFILE_DISCONNECTED;
                    mUartData[0] = mUartData[1] = mUartData[2] = mUartData[3] = mUartData[4] = mUartData[5] = 0;
                    mService.close();
                    mTextViewMtu.setText("-");
                    mTextViewConInt.setText("-");
                   //mConnectionProgDialog.hide();

                    mConnectionProgDialog.dismiss();
                    //mConnectionProgDialog=null;
                    Log.d(TAG, "UART_DISCONNECT_MSG");
                    writeToLog("Disconnected", AppLogFontType.APP_NORMAL);


                    mobile_speak("裝置已斷線");

                }
            });
        }

        //*********************//
        if (action.equals(ImageTransferService.ACTION_GATT_SERVICES_DISCOVERED)) {
            mService.enableTXNotification();
            mService.sendCommand(BleCommand.GetBleParams.ordinal(), null);
            setGuiByAppMode(AppRunMode.Connected);
        }

        //*********************//
        if (action.equals(ImageTransferService.ACTION_DATA_AVAILABLE)) {

            final byte[] txValue = intent.getByteArrayExtra(ImageTransferService.EXTRA_DATA);
            runOnUiThread(new Runnable() {
            public void run() {
                try {
                    System.arraycopy(txValue, 0, mDataBuffer, mBytesTransfered, txValue.length);
                    //Log.w("Imageinfo", Arrays.toString(Arrays.copyOfRange(mDataBuffer, 0, txValue.length))+"end");
                    if(mBytesTransfered == 0){
                        Log.w(TAG, "First packet received: " + String.valueOf(txValue.length) + " bytes");
                    }
                    mBytesTransfered += txValue.length;
                    if(mBytesTransfered >= mBytesTotal) {
                        long elapsedTime = System.currentTimeMillis() - mStartTimeImageTransfer;
                        float elapsedSeconds = (float)elapsedTime / 1000.0f;
                        DecimalFormat df = new DecimalFormat("0.0");
                        df.setMaximumFractionDigits(1);
                        String elapsedSecondsString = df.format(elapsedSeconds);
                        String kbpsString = df.format((float)mDataBuffer.length / elapsedSeconds * 8.0f / 1000.0f);
                        //writeToLog("Completed in " + elapsedSecondsString + " seconds. " + kbpsString + " kbps", AppLogFontType.APP_NORMAL);
                        mTextViewPictureStatus.setText(String.valueOf(mDataBuffer.length / 1024) + "kB - " + elapsedSecondsString + " seconds - " + kbpsString + " kbps");
                        mTextViewPictureStatus.setVisibility(View.VISIBLE);
                        mTextViewPictureFpsStatus.setText(df.format(1.0f / elapsedSeconds)  + " FPS");
                        mTextViewPictureFpsStatus.setVisibility(View.VISIBLE);
                        Bitmap bitmap;
                        Log.w(TAG, "attempting JPEG decode");
                        try {
                            byte[] jpgHeader = new byte[]{-1, -40, -1, -32};
                            if(Arrays.equals(jpgHeader, Arrays.copyOfRange(mDataBuffer, 0, 4))) {
                                //Log.w("UART", String.valueOf(jpgHeader));
                                //Log.w("UART", String.valueOf(mDataBuffer[0]));
                               // Log.w("UART", String.valueOf(mDataBuffer[1]));
                                //Log.w("UART", String.valueOf(mDataBuffer[2]));
                                //Log.w("UART", String.valueOf(mDataBuffer[3]));
                                //Log.w("UART", String.valueOf(mDataBuffer[4]));
                               // Log.w("UART", String.valueOf(mDataBuffer[5]));

                                // New plus version of the Arducam mini 2MP module
                                //Thread.sleep(100);
                                bitmap = BitmapFactory.decodeByteArray(mDataBuffer, 0, mDataBuffer.length);
                                Log.w("UART", String.valueOf(bitmap));
                                mMainImage.setImageBitmap(bitmap);



                                File saveFile = new File("/storage/emulated/0/VAS/", "READY.txt");
                                if (!saveFile.exists()) {

                                    Log.e("WEN", "READY.txt is not existed");

                                    //WEN_SAVEPIC//
                                    File PICDir = new File("/storage/emulated/0/VAS/");
                                    if (!PICDir.exists()) {
                                        Log.e("WEN", "MKDIRVAS");
                                        PICDir.mkdir();
                                    }

//                                String fileName = System.currentTimeMillis() + ".jpg";
                                    String fileName = "text1.jpg";
                                    File file = new File(PICDir, fileName);
                                    try {
                                        Log.e("WEN", "SAVEPIC");
                                        FileOutputStream fos = new FileOutputStream(file);
                                        bitmap.compress(CompressFormat.JPEG, 100, fos);
                                        fos.flush();
                                        fos.close();
                                    } catch (FileNotFoundException e) {
//                                      e.printStackTrace();
                                    } catch (IOException e) {
//                                      e.printStackTrace();
                                    }
                                    //WEN_SAVEPIC//

                                    //WEN_READY.txt//
                                    Log.e("WEN", "save READY.txt");

                                    FileOutputStream outStream = new FileOutputStream(saveFile);
                                    outStream.write("0".getBytes());
                                    outStream.close();
                                    //WEN_READY.txt//

                                }else{
                                    Log.e("WEN", "READY.txt is existed");
                                }


//                                //WEN_READY.txt//
//                                File saveFile = new File("/storage/emulated/0/VAS/", "READY.txt");
//                                if (!saveFile.exists()) {
//                                    Log.e("WEN", "txt");
//                                    FileOutputStream outStream = new FileOutputStream(saveFile);
//                                    outStream.write("0".getBytes());
//                                    outStream.close();
//                                }
//                                //WEN_READY.txt//


//                                //WEN_SAVEPIC//
//                                File appDir = new File("/storage/emulated/0/VAS/");
//                                if (!appDir.exists()) {
//                                    Log.e("WEN", "MKDIRVAS");
//                                    appDir.mkdir();
//                                }
////                                String fileName = System.currentTimeMillis() + ".jpg";
//                                String fileName = "text1.jpg";
//                                File file = new File(appDir, fileName);
//                                try {
//                                    Log.e("WEN", "SAVEPIC");
//                                    FileOutputStream fos = new FileOutputStream(file);
//                                    bitmap.compress(CompressFormat.JPEG, 100, fos);
//                                    fos.flush();
//                                    fos.close();
//                                    } catch (FileNotFoundException e) {
////                                      e.printStackTrace();
//                                    } catch (IOException e) {
////                                      e.printStackTrace();
//                                    }
//                                //WEN_SAVEPIC//


                            }
                            else if(Arrays.equals(jpgHeader, Arrays.copyOfRange(mDataBuffer, 1, 5))){
                                // Old version of the Arducam mini 2MP module
                                bitmap = BitmapFactory.decodeByteArray(mDataBuffer, 1, mDataBuffer.length-1);
                                mMainImage.setImageBitmap(bitmap);
                            }
                            else {
                                Log.w(TAG, "JPG header missing!! Image data corrupt.");
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "Bitmapfactory fail :(");
                        }
                        if(!mStreamActive) {
                            setGuiByAppMode(AppRunMode.Connected);
                        }
                    }
/*
                    File MP3READY = new File("/storage/emulated/0/VAS/", "MP3READY.txt");
                    File MP3READYpath = new File("/storage/emulated/0/VAS/");

                    Bitmap bitmap2;
                    if(MP3READY.exists()){

                        String fileName = "myData.mp3";
                        File file = new File(MP3READYpath, fileName);
                        try {
                            ByteBuffer test3 = null;
                            byte[] bytes = Files.readAllBytes(file.toPath());
                            byte[] test4=null;
                            Log.e("MP3", "SAVEPIC");
                            //FileOutputStream fos = new FileOutputStream(file);

                           // fos.getChannel().read(test3);
                            Log.w("MP3", Arrays.toString(bytes));
                            //test3.wrap(test4);
                           // Log.w("MP3", String.valueOf(test4));
                           // System.arraycopy(fos, 0, mDataBuffer, mBytesTransfered, txValue.length);
                            mService.sendCommand(BleCommand.SendMP3.ordinal(),bytes);
                        } catch (FileNotFoundException e) {
//                                      e.printStackTrace();
                        } catch (IOException e) {
//                                      e.printStackTrace();
                        }



                    }

 */


                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                }
            }
            });
        }
        //*********************//
        if (action.equals(ImageTransferService.ACTION_IMG_INFO_AVAILABLE)) {
            final byte[] txValue = intent.getByteArrayExtra(ImageTransferService.EXTRA_DATA);
            runOnUiThread(new Runnable() {
                public void run() {
                    try {
                        switch(txValue[0]) {
                            case 1:
                                /*
                                File PIC =new File("/storage/emulated/0/VAS/");
                                //File dir = new File(root.getAbsolutePath() + "/download");
                                //dir.mkdirs();
                                File file = new File(MP3dir, "OK.txt");
                                if(file.exists()) {
                                    try {
                                        File MP3dir =new File("/storage/emulated/0/VAS/");
                                        //File dir = new File(root.getAbsolutePath() + "/download");
                                        //dir.mkdirs();
                                        File file = new File(MP3dir, "myData.mp3");
                                        byte[] mp3SoundByteArray = new byte[3000];
                                        Log.e("MP3", "SAVEuart");
                                        FileOutputStream fos = new FileOutputStream(file);
                                        fos.write(mp3SoundByteArray);
                                        fos.close();
                                        ByteBuffer mtuBB = ByteBuffer.wrap(Arrays.copyOfRange(mp3SoundByteArray, 0, mp3SoundByteArray.length));
                                        mService.sendCommand(BleCommand.NoCommand.ordinal(), mp3SoundByteArray);
                                        Log.w("MP3", Arrays.toString(mp3SoundByteArray));
                                    } catch (FileNotFoundException e) {
//                                      e.printStackTrace();
                                    } catch (IOException e) {
//                                      e.printStackTrace();
                                    }
                                }

                                 */
                                // Start a new file transfer
                                ByteBuffer byteBuffer = ByteBuffer.wrap(Arrays.copyOfRange(txValue, 1, 5));
                                //Log.w("Imageinfo", "case1"+ Arrays.toString(Arrays.copyOfRange(txValue, 0, 6)));
                                byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
                                //Log.w("Imageinfo", "case1afterendian"+ Arrays.toString(Arrays.copyOfRange(txValue, 0, 6)));
                                int fileSize = byteBuffer.getInt();
                                mBytesTotal = fileSize;
                                mDataBuffer = new byte[fileSize];
                                mTextViewFileLabel.setText("Incoming file: " + String.valueOf(fileSize) + " bytes.");
                                mBytesTransfered = 0;
                                mStartTimeImageTransfer = System.currentTimeMillis();

//                                FileOutputStream fOut;
//                                String directoryPath="";
//                                directoryPath=getFilesDir()+File.separator+dir;
//                                File file = new File(directoryPath);
//                                if(!file.exists()){//判断文件目录是否存在
//                                    file.mkdirs();
//                                    File dir = new File("/sdcard/demo/");
//
//                                if (!dir.exists()) {
//                                    dir.mkdir();
//                                    Log.i("file", "mkdir");
//                                }

//                                String tmp = "/sdcard/demo/takepicture.jpg";
//                                fOut = new FileOutputStream(tmp);
//                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
//

                                break;

                            case 2:

                                ByteBuffer mtuBB = ByteBuffer.wrap(Arrays.copyOfRange(txValue, 1, 3));
                               // Log.w("Imageinfo", "case2"+ Arrays.toString(Arrays.copyOfRange(txValue, 0, 6)));
                                mtuBB.order(ByteOrder.LITTLE_ENDIAN);
                                //Log.w("Imageinfo", "case2afterendian"+ Arrays.toString(Arrays.copyOfRange(txValue, 0, 6)));
                                short mtu = mtuBB.getShort();
                                mTextViewMtu.setText(String.valueOf(mtu) + " bytes");
                                if(!mMtuRequested && mtu < 64){
                                    mService.requestMtu(247);
                                    writeToLog("Requesting 247 byte MTU from app", AppLogFontType.APP_NORMAL);
                                    mMtuRequested = true;
                                }
                                ByteBuffer ciBB = ByteBuffer.wrap(Arrays.copyOfRange(txValue, 3, 5));
                                ciBB.order(ByteOrder.LITTLE_ENDIAN);
                                short conInterval = ciBB.getShort();
                                mTextViewConInt.setText(String.valueOf((float)conInterval * 1.25f) + "ms");
                                short txPhy = txValue[5];
                                short rxPhy = txValue[6];
                                if(txPhy == 0x0001 && mSpinnerPhy.getSelectedItemPosition() == 1) {
                                    mSpinnerPhy.setSelection(0);
                                    writeToLog("2Mbps not supported!", AppLogFontType.APP_ERROR);
                                }
                                else {
                                    writeToLog("Parameters updated.", AppLogFontType.APP_NORMAL);
                                }
                                break;
                             case 3:
                                 byte[] test = ByteBuffer.wrap(Arrays.copyOfRange(txValue, 0, 3)).array();
                                 //Log.w("test", Arrays.toString(Arrays.copyOfRange(txValue, 0, 5)));
                                 //Log.w("test", Arrays.toString(test.array()));

                                // detectintent.putExtra("distance", (Parcelable) test);
                                 //beeper when
                                     byte[] beeper = new byte[]{3, -86, 0};
                                // Log.w("test","beeper"+beeper);
                                 //Log.w("test", String.valueOf(Arrays.equals(beeper, test)));
                                    // System.arraycopy(txValue, 0, mBeeperBuffer, mBytesBeeper, txValue.length);
                                // Log.w("test","buffer"+ Arrays.toString(mBeeperBuffer));
                                 if(Arrays.equals(beeper, test)) {
                                     tone_speak(100, 125);
                                 }
                                 break;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, e.toString());
                    }
                }
            });
        }
        //*********************//
        if (action.equals(ImageTransferService.DEVICE_DOES_NOT_SUPPORT_IMAGE_TRANSFER)){
            //showMessage("Device doesn't support UART. Disconnecting");
            writeToLog("APP: Invalid BLE service, disconnecting!",  AppLogFontType.APP_ERROR);
            mService.disconnect();

        }
        }
    };

    public static class Utils {
        private final static long DEFAULT_TIME = 1000;

        public static boolean isInvalidClick(@NonNull View target) {
            return isInvalidClick(target, DEFAULT_TIME);
        }

        /**
         * @param target      防止多次點選的View
         * @param defaultTime 超時時間
         * @return
         */
        public static boolean isInvalidClick(@NonNull View target, @IntRange(from = 0) long defaultTime) {
            long curTimeStamp = System.currentTimeMillis();
            long lastClickTimeStamp = 0;
            Object o = target.getTag(R.id.invalid_click);
            if (o == null) {
                target.setTag(R.id.invalid_click, curTimeStamp);
                return false;
            }
            lastClickTimeStamp = (Long) o;
            boolean isInvalid = curTimeStamp - lastClickTimeStamp < defaultTime;
            if (!isInvalid)
                target.setTag(R.id.invalid_click, curTimeStamp);
            return isInvalid;
        }
    }

    private void service_init() {
        Intent bindIntent = new Intent(this, ImageTransferService.class);
        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);


        LocalBroadcastManager.getInstance(this).registerReceiver(UARTStatusChangeReceiver, makeGattUpdateIntentFilter());

    }
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ImageTransferService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(ImageTransferService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(ImageTransferService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(ImageTransferService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(ImageTransferService.ACTION_IMG_INFO_AVAILABLE);
        intentFilter.addAction(ImageTransferService.DEVICE_DOES_NOT_SUPPORT_IMAGE_TRANSFER);
        return intentFilter;
    }
    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");
        
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(UARTStatusChangeReceiver);
            String timeLog = "";
            int week = Calendar.getInstance().get(Calendar.WEEK_OF_YEAR);
            String strweek = String.valueOf(Calendar.getInstance().get(Calendar.WEEK_OF_YEAR));
            int day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1;
            int time = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

            String myDevice = Build.DEVICE;

            if (week < 10) {
                strweek = "0" + strweek;
                timeLog = "1" + strweek + day + time + myDevice;
            } else {
                timeLog = "1" + week + day + time + myDevice;
            }

            File timelogpath = new File("/storage/emulated/0/VAS/" + timeLog + ".txt");
            File PICDir = new File("/storage/emulated/0/VAS/");
            if(timelogpath.exists()){
            try (BufferedReader br = new BufferedReader(new FileReader(timelogpath))) {
                StringBuilder sb = new StringBuilder();
                String line = br.readLine();

                while (line != null) {
                    sb.append(line);
                    sb.append(System.lineSeparator());
                    line = br.readLine();
                }
                String everything = sb.toString();
                if (everything != null) {
                    createFile(everything);
                   // saveFile(everything, mOpenFileId);
                }
                for (File file : PICDir.listFiles()) {
                    if (!file.isDirectory()) {
                        file.delete();
                    }
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }
        unbindService(mServiceConnection);
        unbindService(mlabelServiceConnection);

        mService.stopSelf();
        //detectService.stopSelf();

       detectService=null;
        mService= null;



    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();

    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        if (!mBtAdapter.isEnabled()) {
            Log.i(TAG, "onResume - BT not enabled yet");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

 
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_SELECT_DEVICE:
                //When the DeviceListActivity return, with the selected device address
                if (resultCode == Activity.RESULT_OK && data != null) {
                    String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
                    mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);

                    Log.d(TAG, "... onActivityResultdevice.address==" + mDevice + "mserviceValue" + mService);
                    //((TextView) findViewById(R.id.deviceName)).setText(mD
                    //
                    // evice.getName()+ " - connecting");
                    mService.connect(deviceAddress);
                    mConnectionProgDialog=DialogUtils.showProgressDialog(this,"Connecting...");
                    //mConnectionProgDialog.show();
                }
                break;

            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "Bluetooth has turned on ", Toast.LENGTH_SHORT).show();

                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, "Problem in BT Turning ON ", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;

            case REQUEST_CODE_SIGN_IN:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    handleSignInResult(data);
                }
                break;


            default:
                Log.e(TAG, "wrong request code");
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
       
    }

    
    private void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
  
    }

    @Override
    public void onBackPressed() {
        if (mState == UART_PROFILE_CONNECTED) {
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);
            showMessage("nRFUART's running in background.\n             Disconnect to exit");
            Log.w("UART","uart running");
        }
        else {
            finish();
        }
    }

    //check permission

        // old check permissions
    public void requestStoragePremission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }
    }
    public void requestLocationPremission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        }
    }



    //google Sign in Function
    private void requestSignIn() {
        Log.d(TAG, "Requesting sign-in");
        //Sign in parameters
        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .requestScopes(new Scope(DriveScopes.DRIVE_FILE))//permission of google drive create read save
                        .build();
        //pass to client
        GoogleSignInClient client = GoogleSignIn.getClient(this, signInOptions);
        //start Auth
        startActivityForResult(client.getSignInIntent(), REQUEST_CODE_SIGN_IN);
    }


    //handling log in
    private void handleSignInResult(Intent result) {
        GoogleSignIn.getSignedInAccountFromIntent(result)
                .addOnSuccessListener(googleAccount -> {
                    Log.d(TAG, "Signed in as " + googleAccount.getEmail());
                    Log.d(TAG, String.valueOf(googleAccount.getAccount()));

                    // Use the authenticated account to sign in to the Drive service.
                    GoogleAccountCredential credential =
                            GoogleAccountCredential.usingOAuth2(
                                    this, Collections.singleton(DriveScopes.DRIVE_FILE));
                    credential.setSelectedAccount(googleAccount.getAccount());
                    //setting drive api
                    Drive googleDriveService =
                            new Drive.Builder(
                                    AndroidHttp.newCompatibleTransport(),
                                    new GsonFactory(),
                                    credential)
                                    .setApplicationName("Drive API Migration")
                                    .build();
                    // Do something in response to button
                    this.mDriveServiceHelper = new com.wusasmart.vas.DriveServiceHelper(googleDriveService);
                    Intent testintent = new Intent(this, com.wusasmart.vas.detectlabel.class);
                    //String message = EXTRA_MESSAGE;

                    // The DriveServiceHelper encapsulates all REST API and SAF functionality.
                    // Its instantiation is required before handling any onClick actions.

                    //this.startService(detectintent);

                })
                .addOnFailureListener(exception -> Log.e(TAG, "Unable to sign in.", exception));
    }

    /**
     * Creates a new file via the Drive REST API.
     */
    public void createFile(String test) {
        if (mDriveServiceHelper != null) {
            Log.d(TAG, "Creating a file.");

            mDriveServiceHelper.createFile()
                    .addOnSuccessListener(fileId -> readFile(fileId,test))
                    .addOnFailureListener(exception ->
                            Log.e(TAG, "Couldn't create file.", exception));
        }
    }

    /**
     * Retrieves the title and content of a file identified by {@code fileId} and populates the UI.
     */
    public void readFile(String fileId,String test) {
        if (mDriveServiceHelper != null) {
            Log.d(TAG, "Reading file " + fileId);
            this.mOpenFileId=fileId;
            mDriveServiceHelper.readFile(fileId)
                    .addOnSuccessListener(nameAndContent -> {
                        String name = "test";
                        String content = "cool";

                        // mFileTitleEditText.setText(name);
                        //mDocContentEditText.setText(content);

                        setReadWriteMode(fileId);
                    })
                    .addOnFailureListener(exception ->
                            Log.e(TAG, "Couldn't read file.", exception));
            Log.d(TAG, "Saving " + mOpenFileId);
            //save part
            int Number=0;
            String fileName;
            //String test =JSONObject.wrap(result).toString();
            // result.;
            int week= Calendar.getInstance().get(Calendar.WEEK_OF_YEAR);
            String strweek=String.valueOf(Calendar.getInstance().get(Calendar.WEEK_OF_YEAR));
            int day=Calendar.getInstance().get(Calendar.DAY_OF_WEEK)-1;
            int time=Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

            String myDevice = Build.DEVICE;

            if (week<10)
            {
                strweek="0"+strweek;
                fileName = "1"+strweek+day+time+myDevice;
            }
            else
            {
                fileName = "1"+week+day+time+myDevice;
            }
            String fileContent = test;



            int b=Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
            int d=Calendar.getInstance().get(Calendar.DAY_OF_WEEK_IN_MONTH);


            Log.w("date", String.valueOf(week));
            Log.w("date", String.valueOf(day));
            Log.w("date", String.valueOf(time));

            Log.w("date", String.valueOf(myDevice));

            //Log.e("savefile",result.toString());
            //Log.e("savefile", String.valueOf(result.getLabels()));
            Log.e("savefile", test);



            mDriveServiceHelper.saveFile(this.mOpenFileId, fileName, fileContent)
                    .addOnFailureListener(exception ->
                            Log.e(TAG, "Unable to save file via REST.", exception));
        }
    }

    /**
     * Saves the currently opened file created via {@link #createFile()} if one exists.
     */
    public void saveFile(String test,String fileId) {

        Log.d(TAG, "Saving " + mOpenFileId);

        int Number=0;
        String fileName;
        //String test =JSONObject.wrap(result).toString();
        // result.;
        int week= Calendar.getInstance().get(Calendar.WEEK_OF_YEAR);
        String strweek=String.valueOf(Calendar.getInstance().get(Calendar.WEEK_OF_YEAR));
        int day=Calendar.getInstance().get(Calendar.DAY_OF_WEEK)-1;
        int time=Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

        String myDevice = Build.DEVICE;

        if (week<10)
        {
            strweek="0"+strweek;
            fileName = "1"+strweek+day+time+myDevice;
        }
        else
        {
            fileName = "1"+week+day+time+myDevice;
        }
        String fileContent = test;


        int b=Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
        int d=Calendar.getInstance().get(Calendar.DAY_OF_WEEK_IN_MONTH);


        Log.w("date", String.valueOf(week));
        Log.w("date", String.valueOf(day));
        Log.w("date", String.valueOf(time));

        Log.w("date", String.valueOf(myDevice));

        //Log.e("savefile",result.toString());
        //Log.e("savefile", String.valueOf(result.getLabels()));
        Log.e("savefile", test);


        mDriveServiceHelper.saveFile(this.mOpenFileId, fileName, fileContent)
                .addOnFailureListener(exception ->
                        Log.e(TAG, "Unable to save file via REST.", exception));

    }
    /**
     * Updates the UI to read/write mode on the document identified by {@code fileId}.
     */
    private void setReadWriteMode(String fileId) {
        //mFileTitleEditText.setEnabled(true);
        //mDocContentEditText.setEnabled(true);
        this.mOpenFileId = fileId;
}
    private boolean ensureBLEExists() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.no_ble, Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.settings_and_about, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_settings:
                final Intent intent = new Intent(this, com.wusasmart.vas.AppSettingsActivity.class);
                startActivity(intent);
                break;
                // Add your code
            case R.id.action_about:



                //R.string.about_text 要顯示的文字    version  版本
                final AppHelpFragment fragment =AppHelpFragment.getInstance(R.string.about_text, true);
                /*
                SharedPreferences prefs = this.getSharedPreferences(
                        "com.wusasmart.vas", Context.MODE_PRIVATE);
                String teststring="";
                //teststring=prefs.getString("SoundDevice","");
                teststring=prefs.getString("SoundDevice",null);
                Log.w("item", teststring);

                 */
                /*
                fragment.getShowsDialog();
                fragment.showNow();*/
              //  fragment.showNow(fragment,R.string.about_text);
              fragment.show(getSupportFragmentManager(), null);
              //  fragment.show(getFragmentManager(),R.string.about_text);
              //  fragment.showNow(fragment.getActivity().getSupportFragmentManager(), fragment.getTag());
                break;





        }
        return true;
    }
/*
    private final static int REQUEST_CODE_ASK_PERMISSIONS = 1;

    /**
     * Permissions that need to be explicitly requested from end user.
     */
    /*

    private static final String[] REQUIRED_SDK_PERMISSIONS = new String[] {
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE };
    protected void checkPermissions() {
        final List<String> missingPermissions = new ArrayList<String>();
        // check all required dynamic permissions
        for (final String permission : REQUIRED_SDK_PERMISSIONS) {
            final int result = ContextCompat.checkSelfPermission(this, permission);
            if (result != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }
        if (!missingPermissions.isEmpty()) {
            // request all missing permissions
            final String[] permissions = missingPermissions
                    .toArray(new String[missingPermissions.size()]);
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_ASK_PERMISSIONS);
        } else {
            final int[] grantResults = new int[REQUIRED_SDK_PERMISSIONS.length];
            Arrays.fill(grantResults, PackageManager.PERMISSION_GRANTED);
            onRequestPermissionsResult(REQUEST_CODE_ASK_PERMISSIONS, REQUIRED_SDK_PERMISSIONS,
                    grantResults);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                for (int index = permissions.length - 1; index >= 0; --index) {
                    if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                        // exit the app if one permission is not granted
                        Toast.makeText(this, "Required permission '" + permissions[index]
                                + "' not granted, exiting", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                }
                // all permissions were granted
                initialize();
                break;
        }
    }
    */


}


