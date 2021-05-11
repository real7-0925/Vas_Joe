package com.wusasmart.vas;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.media.ToneGenerator;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.AmplifyConfiguration;
import com.amplifyframework.predictions.aws.AWSPredictionsPlugin;
import com.amplifyframework.predictions.models.EntityDetails;
import com.amplifyframework.predictions.models.Gender;
import com.amplifyframework.predictions.models.IdentifyActionType;
import com.amplifyframework.predictions.models.LabelType;
import com.amplifyframework.predictions.models.LanguageType;
import com.amplifyframework.predictions.models.TextFormatType;
import com.amplifyframework.predictions.result.IdentifyEntitiesResult;
import com.amplifyframework.predictions.result.IdentifyLabelsResult;
import com.amplifyframework.predictions.result.IdentifyTextResult;
import com.amplifyframework.predictions.result.TranslateTextResult;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class detectlabel extends Service {
    private final MediaPlayer mp =new MediaPlayer();//in playAudio function
    private final MediaPlayer mp2 =new MediaPlayer();//in playAudio function

    //WEN_SOUNDPOOLYL CODE//
    private SoundPool soundpool;
    private Map<Integer, Integer> soundmap = new HashMap<Integer, Integer>();
    private  HashMap<String,String> stringmap=new HashMap<String,String>();
    private int meter=0, mode=0;
    private double distance = 0, CarDistance=0;
    private  boolean isText=false, isPerson=false, isCar=false, isStairCase=false, isHuman=false, isTransportation=false, isAutomobile=false
    , isStairCaseRight=false, isStairCaseMiddle=false, isStairCaseLeft=false, isHandrail=false, processstate=false, isRoad=false, isresume=false
    , isFloor=false, isCorridor=false, isHousing=false, isBuilding=false, isIndoors=false, isRoom=false, isOutdoors=false
    , isLobby=false, isFlagstone=false, isPath=false, isWalkway=false, isSlate=false, isSidewalk=false, isPavement=false
    , isFlooring=false, isWater=false, isFood=false, isDrink=false, isCultery=false, isBeverage=false, isElectronics=false
    , isBag=false, isFurniture=false, isCan=false, isTrashCan=false, isElevator=false, isBathroom=false, isTowel=false
    , isMarker=false, isPen=false, isBox=false, isBottle=false, isPot=false, isWok=false, isFryPan=false, isProjector=false
    , isKnife=false, isTree=false, isCup=false, isBanister=false, isTile=false, isElectricalDevice=false, isCooler=false
    , isTape=false, isBook=false, isScissors=false, isAdapter=false, isGray=false, isWhiteBoard=false, isMannequin=false
    , isRailing=false, isWindow=false, isUrban=false, isHighRise=false, isCity=false;
    private StringBuffer computertuned = new StringBuffer( "个人电脑");

    private double latitude, longitude;
    private static TextToSpeech t1;
    private static final int REQUEST_CODE_SIGN_IN = 1;
    public DriveServiceHelper mDriveServiceHelper;
    private String mOpenFileId;

    @Override
    public void onCreate() {
        super.onCreate();
        //initial Amplify plugin

        //  DriveServiceHelper mDriveServiceHelper =  b.getParcelable("data");
        try {
            Amplify.addPlugin(new AWSCognitoAuthPlugin());//without credential log in
            Amplify.addPlugin(new AWSPredictionsPlugin());//rekognition translate polly high level client

            AmplifyConfiguration config = AmplifyConfiguration.builder(getApplicationContext())
                    .devMenuEnabled(false)
                    .build();
            Amplify.configure(config, getApplicationContext());
        } catch (AmplifyException e) {
            Log.e("Tutorial", "Could not initialize Amplify", e);
        }




        //Creating Vas folder in android for picture and mp3
        String Vasdir = "/VAS/";
        File PrimaryStorage = Environment.getExternalStorageDirectory();
        //Log.e("test", String.valueOf(PrimaryStorage));
        //WEN_SOUNDPOOL//
        File PICDir = new File("/storage/emulated/0/VAS/");
        if (!PICDir.exists()) {
            Log.e("DIR", "MKDIRVAS");
            PICDir.mkdir();
        }
        if (Build.VERSION.SDK_INT > 21) {
            SoundPool.Builder builder = new SoundPool.Builder();
            //传入音频数量
            builder.setMaxStreams(8);
            //AudioAttributes是一个封装音频各种属性的方法
            AudioAttributes.Builder attrBuilder = new AudioAttributes.Builder();
            //设置音频流的合适的属性
            attrBuilder.setLegacyStreamType(AudioManager.STREAM_SYSTEM);//STREAM_MUSIC
            //加载一个AudioAttributes
            builder.setAudioAttributes(attrBuilder.build());
            soundpool = builder.build();
        } else {
            soundpool = new SoundPool(8, AudioManager.STREAM_SYSTEM, 0);
        }
        //
        soundmap.put(1,soundpool.load(this, R.raw.test1, 1));
        soundmap.put(2,soundpool.load(this,R.raw.detecting,0));
        Log.w("Language",Locale.getDefault().getCountry());
        Log.w("Language",Locale.getDefault().getISO3Country()) ;
        Log.w("Language",Locale.getDefault().getDisplayCountry()) ;
        Log.w("Language",Locale.getDefault().getCountry()) ;
        Log.w("Language", String.valueOf(Locale.getDefault())) ;
        Log.w("Language",Locale.getDefault().toString());
                t1=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.getDefault());
                }
            }
        });
                Configuration cofig;
        stringmap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "UniqueID");
        File ReadyPath =new File("/storage/emulated/0/VAS/"+"Ready.txt");
        try {
            String deleteCmd = "rm -r " + ReadyPath;
            Runtime runtime = Runtime.getRuntime();
            runtime.exec(deleteCmd);

        } catch (FileNotFoundException e) {
            Log.e("NOTFOUND", "file notfound");
        } catch (IOException e) {
            Log.e("IOERROR", "some IO error");
        }
/*
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Do something after 5s = 5000ms
                soundpool.play(soundmap.get(1), 1, 1, 0, 0, 1);
            }
        }, 2000);

 */





/*

        if (status == TextToSpeech.SUCCESS) {
            int result = mtts.setLanguage(new Locale("zh_TW"));
            startApp();
            if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED) {
            } else {
                microid.setEnabled(true);
            }
        } else {

        }

 */
        //decode .bmp picture
        //hardcode .bmp should be text1.bmp
        //moved in runnable notused
        /*
        File imageFile = new File(PrimaryStorage+"/Vas/text1.jpg");
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        Bitmap image = BitmapFactory.decodeFile(imageFile.getAbsolutePath(),bmOptions);
         */
        /*
        GPSTracker gpsTracker = new GPSTracker(this);


        this.latitude=gpsTracker.latitude;
        this.longitude=gpsTracker.longitude;

         */
        //start loop detect label function
        //handler.postDelayed(runnable, 10000);
        ScheduledThreadPoolExecutor executor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(0);
        Task task = new Task();


        executor.scheduleWithFixedDelay(task, 1, 1, TimeUnit.SECONDS);


// Create the text message with a string.







    }
    public class detectBinder extends Binder {
        public detectlabel getService() {
            return detectlabel.this;
        }
    }
    public int onStartCommand(Intent intent, int flags, int startId) {

        //DriveServiceHelper mDriveServiceHelper = (DriveServiceHelper) intent.getExtras().get("test");
        return START_STICKY;
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    @Override
    public void onDestroy() {
    }


    //note:need 10~15sec to get result from aws
    //without polly about 5sec
    //main loop
    /*
    private Handler handler = new Handler();
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            File sd = Environment.getExternalStorageDirectory();
            String Vasdir="/Vas/";
            File imageFile = new File(sd+Vasdir+"text1.bmp");
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            Bitmap image = BitmapFactory.decodeFile(imageFile.getAbsolutePath(),bmOptions);
            detectLabels(image);
            handler.postDelayed(this, 10000);
        }
    };

     */

    class Task implements Runnable{

        public void run() {

            File PrimaryStorage = Environment.getExternalStorageDirectory();
            String Vaspicdir="";
            String ReadyFil="READY.txt";
            //File imageFile = new File("/storage/emulated/0/VAS/"+"text1.jpg");
            File imageFile = new File("/storage/emulated/0/VAS/");
            //File imageFile = new File(System.currentTimeMillis() + ".jpg");
            File VASDir = new File("/storage/emulated/0/VAS/");


            File ReadyPath =new File("/storage/emulated/0/VAS/"+ReadyFil);

            if(ReadyPath.exists()) {
                if (!t1.isSpeaking()) {
                    //String toSpeak = "偵測場景中";
                   // String toSpeak = "測試";
                    //t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, stringmap);
                    //while (t1.isSpeaking()){}
                    //new Waiter().execute();
                    for (File f : VASDir.listFiles()) {
                        if (f.isFile()) {
                            String name = "/"+f.getName();
                            BitmapFactory.Options options = new BitmapFactory.Options();
                            options.inJustDecodeBounds = true;
                            Bitmap bitmap = BitmapFactory.decodeFile(VASDir+name, options);

                            if (options.outWidth != -1 && options.outHeight != -1) {
                                // This is an image file.
                                imageFile= new File(VASDir + name);
                                Vaspicdir=String.valueOf(imageFile);
                            }
                            else {
                                // This is not an image file.
                            }
                        }
                        // Do your stuff
                    }
                    isText=false; isPerson=false; isCar=false; isStairCase=false; isHuman=false;
                    isTransportation=false; isAutomobile=false;
                    isStairCaseRight=false; isStairCaseMiddle=false; isStairCaseLeft=false;;
                    isHandrail=false; isRoad=false; processstate=false;
                    isresume=false; isFloor=false; isCorridor=false; isHousing=false;
                    isBuilding=false; isIndoors=false; isRoom=false; isOutdoors=false;
                    isLobby=false; isFlagstone=false; isPath=false; isWalkway=false;
                    isSlate=false; isSidewalk=false; isPavement=false; isFlooring=false;
                    isWater=false; isFood=false; isDrink=false; isCultery=false;
                    isBeverage=false; isElectronics=false; isBag=false; isFurniture=false;
                    isCan=false; isTrashCan=false; isElevator=false; isBathroom=false;
                    isTowel=false; isMarker=false; isPen=false; isBox=false; isBottle=false;
                    isPot=false; isWok=false; isFryPan=false; isProjector=false; isKnife=false;
                    isTree=false; isCup=false; isBanister=false; isTile=false; isElectricalDevice=false;
                    isCooler=false; isTape=false; isBook=false; isScissors=false; isAdapter=false;
                    isGray=false; isWhiteBoard=false; isMannequin=false; isRailing=false; isWindow=false;
                    isUrban=false; isHighRise=false; isCity=false;
                    //soundpool.play(soundmap.get(2), 1, 1, 0, 0, 1);

                    ///////////
                    try {
                        String deleteCmd = "rm -r " + ReadyPath;
                        Runtime runtime = Runtime.getRuntime();
                        runtime.exec(deleteCmd);

                    } catch (FileNotFoundException e) {
                        Log.e("NOTFOUND", "file notfound");
                    } catch (IOException e) {
                        Log.e("IOERROR", "some IO error");
                    }
                    try {

                        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                        Bitmap image = BitmapFactory.decodeFile(String.valueOf(imageFile), bmOptions);

 //Log.w("Original dimensions", String.valueOf(imageFile));
                        //Bitmap image2=Bitmap.createScaledBitmap(image,160,120,true);

/*
                            Log.w("Original dimensions", Vaspicdir);
                            Bitmap original = BitmapFactory.decodeStream(getAssets().open(Vaspicdir));
                            Log.w("Original dimensions", String.valueOf(original));
                            ByteArrayOutputStream out = new ByteArrayOutputStream();
                            original.compress(Bitmap.CompressFormat.JPEG, 90,new FileOutputStream( "/storage/emulated/0/VAS/new.jpg"));
                            //Log.w("Original dimensions", String.valueOf(iscompress));
                            Bitmap decoded = BitmapFactory.decodeStream(new ByteArrayInputStream(out.toByteArray()));
                            Log.w("Original dimensions", String.valueOf(decoded));
                            Log.w("Original   dimensions", original.getWidth() + " " + original.getHeight());
                            Log.w("Compressed dimensions", decoded.getWidth() + " " + decoded.getHeight());
                            //image.compress(Bitmap.CompressFormat.JPEG,50,out);
 */

                           detectLabels(image);
                        //detectEntities(image);
                        isText = false;

                        Log.w("BOOLEAN", String.valueOf(isPerson));
                        Log.w("maindis", String.valueOf(meter));


                    } catch (Exception e) {
                        Log.e("DETECT", "detect error");
                    }
                }
            }
        }

        //handler.postDelayed(this, 10000);
    }
    //detectlabel function
    private void detectLabels(Bitmap image) {
        try{
            // createFile();
            Log.w("create","created");
            Amplify.Predictions.identify(
                    LabelType.LABELS,
                    image,
                    result -> LabelDataHold((IdentifyLabelsResult) result),//pass to LabelDataHold function
                    error -> Log.e("MyAmplifyApp", "Label detection failed", error)
            );
        }

        catch (Exception  e )
        {
            Log.e("DETECT","detectlabelfail");
        }


        //SystemClock.sleep(2000);
    }
/*
    public void detectEntities(Bitmap image) {
        Amplify.Predictions.identify(
                IdentifyActionType.DETECT_ENTITIES ,
                image,
                result -> {
                    test((IdentifyEntitiesResult) result);
                    //IdentifyEntitiesResult identifyResult = (IdentifyEntitiesResult) result;
                    //int metadata = identifyResult.getEntities().get(0).getAgeRange().getLow();
                   // Log.i("MyAmplifyApp", String.valueOf(metadata));
                },
                error -> Log.e("MyAmplifyApp", "Entity detection failed", error)
        );
    }
    private void test(IdentifyEntitiesResult result)
    {
        int metadata=result.getEntities().get(0).getAgeRange().getLow();
        Log.i("MyAmplifyApp", String.valueOf(metadata));
    }

 */



    //unused function
    //****************************************
    private void detectText(Bitmap image) {
        try {
            Amplify.Predictions.identify(
                    TextFormatType.PLAIN,
                    image,
                    result -> {
                        IdentifyTextResult identifyResult = (IdentifyTextResult) result;
                        textTranslate(identifyResult.getFullText());
                        //Log.i("MyAmplifyApp", identifyResult.getFullText());
                    },
                    error -> Log.e("MyAmplifyApp", "Identify text failed", error)
            );
            //textTranslate(identifyResult.getFullText());
        }
        catch (Exception  e )
        {
            Log.e("DETECT","detectlabelfail");
        }
    }
    //****************************************



    //holding label from detect label
    private void LabelDataHold(IdentifyLabelsResult result)
    {

        //final String[] printout =new String[result.getLabels().size()];
        //String boxout=new String();
        String test = "";

        String boxout=new String();
        float boxheight4,boxwidt4h,boxheight1,boxwidth1,boxheight2,boxwidth2,boxheight3,boxwidth3,A4,A1,A2,A3;
        float[] boxheight=new float[result.getLabels().size()];
        float[] boxwidth=new float[result.getLabels().size()];
        float boxX,boxY;
        boolean[] isbox=new boolean[result.getLabels().size()];
        double[] A=new double[result.getLabels().size()];
        double[] biggest=new double[result.getLabels().size()];
        double[][] Anumber=new double[result.getLabels().size()][20];
        double[][] Xnumber=new double[result.getLabels().size()][20];

        String box3=new String();
        //String[][] parentlabels.get(j)=new String[result.getLabels().size()][20];
        String[][] Datas=new String[15][20];
        String[][] scene=new String[15][20];
        int max=result.getLabels().size();
        int LabelNumber=0;
        //int boxmax=result.getLabels().get(LabelNumber).getBoxes().size();
        int[] boxmax=new int[result.getLabels().size()];
        //int[] parentlabelmax=new int[result.getLabels().size()];
        int parentlabelmax=0;
        String Car;
        final String output ;
        String save="";
        // result=result;
        //boxout= result.getLabels().get(0).getBoxes().toString();
        //boxheight=1800*result.getLabels().get(0).getBoxes().get(0).height();//result from detectlabel  result.get(0)first label  result.get(0).getboxes() bounding box of first object A=.height *.width
        //boxwidth=1280*result.getLabels().get(0).getBoxes().get(0).width();
        box3=result.toString();
        int PersonCount=0,Count=0,i=0,j=0,k=0,l=0,m=0,n=0,p=0;//note k=1 because k--; can'tbe-1
        int isperson=0x111;
        int person1=0,person2=0,person3=0,automobile=0,tranportation=0;
        int testint = 0;

        //A=boxheight*boxwidth;
        //box3=result.getLabels().get(0).getPolygon().toString();
        //Log.w("Labeldata", boxout);
        //Log.w("Labeldata", String.valueOf(boxheight));
        //Log.w("Labeldata", String.valueOf(boxwidth));
        // Log.w("Labeldata", String.valueOf(A));
        //Log.w("Labeldata",box3);

        ConcurrentHashMap<Integer,String> labels=new ConcurrentHashMap<>();
        ConcurrentHashMap<Integer,String> parentlabels=new ConcurrentHashMap<>();
        //.put(m,result.getLabels().get(m).getName());
        //labels.get(m);
        for (m = 0; m < max; m++) {
            labels.put(m,result.getLabels().get(m).getName());
            ///*******************************************************************************///
            //condition of scence
            if(labels.get(m).equals("Staircase")||labels.get(m).equals("Handrail")){ this.isHandrail=true; this.isBanister=false;}
            if (labels.get(m).equals("Car") || labels.get(m).equals("Sedan") || labels.get(m).equals("Limo")) { this.isCar = true;testint=2; }
            if(labels.get(m).equals("Automobile")) { this.isAutomobile=true;}
            if(labels.get(m).equals("Transportation")) { this.isTransportation=true;}
            if(labels.get(m).equals("Word")||labels.get(m).equals("Text")){ this.isText=true; }
            if(labels.get(m).equals("Human")) { this.isHuman=true;Log.e("person", String.valueOf(person2)); }
            if(labels.get(m).equals("Person")){ this.isPerson=true; }
            if(labels.get(m).equals("Road")) {this.isRoad=true;}//look front outdoor
            if(labels.get(m).equals("Floor")) {this.isFloor=true;}//lookdown inandout
            if(labels.get(m).equals("Corridor")){this.isCorridor=true;}//look front indoor
            if(labels.get(m).equals("Housing")){this.isHousing=true;}//look front indoor
            if(labels.get(m).equals("Building")){this.isBuilding=true;}//look front indoor outdoor
            if(labels.get(m).equals("Indoors")){this.isIndoors=true;}//look front indoor
            if(labels.get(m).equals("Room")){this.isRoom=true;}//look front indoor (parent indoor)
            if(labels.get(m).equals("Outdoors")&&!isCorridor){this.isOutdoors=true;}//look front outdoor and do not detect corridor
            if(labels.get(m).equals("Lobby")){this.isLobby=true;}//look front indoor
            if(labels.get(m).equals("Path")){this.isPath=true;}//look down outdoor
            if(labels.get(m).equals("Flagstone")){this.isFlagstone=true;}//look down outdoor
            if(labels.get(m).equals("Walkway")){this.isWalkway=true;}//look down outdoor
            if(labels.get(m).equals("Slate")){this.isSlate=true;}//look down outdoor
            if(labels.get(m).equals("Sidewalk")){this.isSidewalk=true;}//look down outdoor
            if(labels.get(m).equals("Pavement")){this.isPavement=true;}//look down outdoor
            if(labels.get(m).equals("Flooring")){this.isFlooring=true;}//look down inantout
            if(labels.get(m).equals("Water")){this.isWater=true;}
            if(labels.get(m).equals("Food")){this.isFood=true;}
            if(labels.get(m).equals("Drink")){this.isDrink=true;}
            if(labels.get(m).equals("Cutlery")){this.isCultery=true;}
            if(labels.get(m).equals("Beverage")){this.isBeverage=true;}
            if(labels.get(m).equals("Electronics")){this.isElectronics=true;}
            if(labels.get(m).equals("Bag")){this.isBag=true;}
            if(labels.get(m).equals("Furniture")){this.isFurniture=true;}
            if(labels.get(m).equals("Can")){this.isCan=true;}
            if(labels.get(m).equals("Trash Can")){this.isTrashCan=true;}
            if(labels.get(m).equals("Elevator")){this.isElevator=true;}
            if(labels.get(m).equals("Bathroom")){this.isBathroom=true;}
            if(labels.get(m).equals("Towel")){this.isTowel=true;}
            if(labels.get(m).equals("Marker")){this.isMarker=true;}
            if(labels.get(m).equals("Pen")){this.isPen=true;}
            if(labels.get(m).equals("Box")){this.isBox=true;}
            if(labels.get(m).equals("Bottle")){this.isBottle=true;}
            if(labels.get(m).equals("Pot")){this.isPot=true;}
            if(labels.get(m).equals("Wok")){this.isWok=true;}
            if(labels.get(m).equals("Frying Pan")){this.isFryPan=true;}
            if(labels.get(m).equals("Projector")){this.isProjector=true;}
            if(labels.get(m).equals("Knife")){this.isKnife=true;}
            if(labels.get(m).equals("Tree")){this.isTree=true;}
            if(labels.get(m).equals("Cup")){this.isCup=true;}
            if(labels.get(m).equals("Banister")){this.isBanister=true;}
            if(labels.get(m).equals("Tile")){this.isTile=true;}
            if(labels.get(m).equals("Electrical Device")){this.isElectricalDevice=true;}
            if(labels.get(m).equals("Window")){this.isWindow=true;}
            if(labels.get(m).equals("Cooler")){this.isCooler=true;}
            if(labels.get(m).equals("Tape")){this.isTape=true;}
            if(labels.get(m).equals("Book")){this.isBook=true;}
            if(labels.get(m).equals("Scissors")){this.isScissors=true;}
            if(labels.get(m).equals("Adapter")){this.isAdapter=true;}
            if(labels.get(m).equals("Gray")){this.isGray=true;}
            if(labels.get(m).equals("White Board")){this.isWhiteBoard=true;}
            if(labels.get(m).equals("Mannequin")){this.isMannequin=true;}
            if(labels.get(m).equals("Railing")){this.isRailing=true;}
            if(labels.get(m).equals("Urban")){this.isUrban=true;}
            if(labels.get(m).equals("High Rise")){this.isHighRise=true;}
            if(labels.get(m).equals("City")){this.isCity=true;}
            //parentlabelmax[m]=result.getLabels().get(m).getParentLabels().size();
            parentlabelmax=result.getLabels().get(m).getParentLabels().size();
            Log.w("leeeeroy1","child:"+m+labels.get(m)+"\n");
            test+="{\"Name\":"+"\""+labels.get(m)+"\""+",";
            test+="\"Parents\":[";
            for(j=0;j<parentlabelmax;j++)
            {
                parentlabels.put(j,result.getLabels().get(m).getParentLabels().get(j));
                //parentlabels.get(j) =result.getLabels().get(m).getParentLabels().get(j);
                test+="{\"Name\":"+parentlabels.get(j)+"},";
                Log.w("leeeeroy1", "parent:"+String.valueOf(parentlabels.get(j)));


                if(parentlabels.get(j).equals("Food")||parentlabels.get(j).equals("Drink")
                        ||parentlabels.get(j).equals("Cutlery")||parentlabels.get(j).equals("Beverage")||parentlabels.get(j).equals("Pottery")){
                    scene[0][k]=labels.get(m);
                    Log.w("leeeeroy0","eatpar"+ scene[0][k]);
                    Log.w("leeeeroy0", "eatparnum"+k);
                    Log.w("counter", String.valueOf(k));
                    Log.w("counter", "add"+String.valueOf(k));
                }//餐廳

                if(parentlabels.get(j).equals("Corridor")
                        ||parentlabels.get(j).equals("Floor")||parentlabels.get(j).equals("Flooring")||parentlabels.get(j).equals("Transportation")
                        ||parentlabels.get(j).equals("Vehicles")|| parentlabels.get(j).equals("Path")
                        ||parentlabels.get(j).equals("Furniture")||parentlabels.get(j).equals("Box")||parentlabels.get(j).equals("Bag")||parentlabels.get(j).equals("Footwear")
                        ||parentlabels.get(j).equals("Road")||parentlabels.get(j).equals("Pottery")||parentlabels.get(j).equals("Fence")) {
                    scene[1][l]=labels.get(m);
                    Log.w("leeeeroy","walkpar"+ scene[0][k]);
                }//走路外

                if(parentlabels.get(j).equals("Corridor")
                        ||parentlabels.get(j).equals("Floor")||parentlabels.get(j).equals("Flooring")|| parentlabels.get(j).equals("Path")
                        ||parentlabels.get(j).equals("Furniture")||parentlabels.get(j).equals("Box")||parentlabels.get(j).equals("Bag")||parentlabels.get(j).equals("Footwear")
                        ||parentlabels.get(j).equals("Road")||parentlabels.get(j).equals("Pottery")||parentlabels.get(j).equals("Fence")) {
                    scene[2][p]=labels.get(m);
                    Log.w("leeeeroy","walkpar"+ scene[0][k]);
                }//走路內




                    /*
                    if(parentlabels.get(j)[m][j].equals("Shop")||parentlabels.get(j)[m][j].equals("Accessories")||parentlabels.get(j)[m][j].equals("Accessory")
                    ||parentlabels.get(j)[m][j].equals("Clothing")||parentlabels.get(j)[m][j].equals("Bag")) {
                        scene[2][k]=printout[m];
                        k++;
                    }//逛街
                    */
                /*
                String[] SavetypeLabelTable3= {"Furniture","Lamp","Bathroom","Appliance","Electronics","Ceiling Light","Adapter","Electrical Device","Can","File Binder"
                        ,"Clock","Box","Cup","Bag"
                };//add String that has to be Search here
                int SavetypeLabelcount3=SavetypeLabelTable3.length;
                for(int counter=0;counter<SavetypeLabelcount3;counter++) {
                    scene[3][n] = SavetypeLabel(parentlabels.get(j), m, j, SavetypeLabelTable3[counter], scene, 3, n, printout);
                }//回家

                 */

                if(parentlabels.get(j).equals("Furniture")||parentlabels.get(j).equals("Lamp")||parentlabels.get(j).equals("Bathroom")||parentlabels.get(j).equals("Appliance")||parentlabels.get(j).equals("Electronics")||parentlabels.get(j).equals("Ceiling Light")
                        ||parentlabels.get(j).equals("Adapter")||parentlabels.get(j).equals("Electrical Device")
                        ||parentlabels.get(j).equals("Can")||parentlabels.get(j).equals("File Binder")||parentlabels.get(j).equals("Clock")||parentlabels.get(j).equals("Box")
                        ||parentlabels.get(j).equals("Cup")||parentlabels.get(j).equals("Bag")||parentlabels.get(j).equals("Paper Towel")||parentlabels.get(j).equals("Cushion")
                        ||parentlabels.get(j).equals("Text")||parentlabels.get(j).equals("Pot")){
                    scene[3][n]=labels.get(m);
                }//回家
//卫生间

            }
            test+="]}\n";
            Log.w("allscene","eat"+scene[0][k]);
            Log.w("allscene","walk"+scene[1][k]);
            if(scene[0][k]!=null) {
                //not wanted
                String[] NeglectLabelTable0= {"Dish","Meal","Stew","Urban","Dinner","Supper","Lunch","Sweets","Confectionery","Pasta"
                        ,"Vermicelli","Platter","Food","Drinking"
                };//add String that has to be Search here
                int NeglectLabelcount0=NeglectLabelTable0.length;
                for(int counter=0;counter<NeglectLabelcount0;counter++) {
                    scene[0][k] = ReplaceLabel(labels, m, NeglectLabelTable0[counter], scene, 0, k,"");
                }
                //wanted but weird
                String[] SearchedLabelTable0= {"Nuggets","Armchair","Desk","Creme","Pop Bottle"
                };//add String that has to be Search here
                int SearchLabelcount0=SearchedLabelTable0.length;
                String[] ReplaceLabelTable1={"chicken Nuggets","Chair","Table","Cream","Bottle"
                };//*need to be the same length of SearchedWordTable
                for(int counter=0;counter<SearchLabelcount0;counter++) {
                    scene[0][k] = ReplaceLabel(labels, m, SearchedLabelTable0[counter], scene, 0, k, ReplaceLabelTable1[counter]);
                }
                Log.w("allscene", "if!=nullelse" + scene[0][k]);
                if(!scene[0][k].equals("")) {
                    k++;
                }
            }
            if(labels.get(m).equals("Water")){
                scene[0][k]="Water";
                k++;
            }
            //for those are the onlyone child
            Log.w("leeeeroy", "zero if nothing"+String.valueOf(scene[0][k]));
            Log.w("allscene", "before1 "+String.valueOf(k));
            if(scene[1][l]!=null) {
                //special addition
                if((labels.get(m).equals("Train")&&isCorridor)||(labels.get(m).equals("Train Station")&&isCorridor)){
                    scene[1][l] = "";
                }
                //not wanted
                String[] NeglectLabelTable1= {"Tarmac","Asphalt","Transportation","Urban","City","Building","Town","Wheel","Machine","Highway"
                        ,"Tar","Pedestrian","Neighborhood","Vespa","Suburb","Plywood","Furniture","Porch","Wood","Flagstone","Automobile","Bridge"
                        ,"Metropolis","Housing","Architecture","Airport","Sundial","Boat","Barge","Watercraft","Vessel","Vehicle","Train Station"
                        ,"Train","Subway","Aircraft","Takeoff","Tabletop","Reception","Subway","Missile","Rocket","Spaceship","Airplane","Flight"
                };//add String that has to be Search here
                int NeglectLabelcount1=NeglectLabelTable1.length;
                for(int counter=0;counter<NeglectLabelcount1;counter++) {
                    scene[1][l] = ReplaceLabel(labels, m, NeglectLabelTable1[counter], scene, 1, l,"");
                }

                //wanted but weird
                String[] SearchedLabelTable1= {"Person","Armchair","Desk","Bike","Bar Stool","Motor Scooter","Walkway","Pavement","Alley"
                };//add String that has to be Search here
                int SearchLabelcount1=SearchedLabelTable1.length;
                String[] ReplaceLabelTable1={"Person","Chair","Table","Bicycle","Stool","Motorcycle","Sidewalk","Sidewalk","Alleyway"
                };//*need to be the same length of SearchedWordTable
                for(int counter=0;counter<SearchLabelcount1;counter++) {
                    scene[1][l] = ReplaceLabel(labels, m, SearchedLabelTable1[counter], scene, 1, l, ReplaceLabelTable1[counter]);
                }
                //window not parent wont broadcast
                    /*
                    if(printout[m].equals("Window")){
                        scene[1][l]="Window";
                    }
                     */
                Log.w("sceneindex",scene[1][l]);
                Log.w("allscene", "if!=walknullelse" + scene[1][l]);
                if(scene[1][l].equals("Plastic Bag")&&isTrashCan){
                    scene[1][l]="";
                }
                if(!scene[1][l].equals("")) {
                    l++;
                }

            }
            //for those are the onlyone child
            if(labels.get(m).equals("Shelf")){
                scene[1][l]="Shelf";
                l++;
            }
            if(labels.get(m).equals("Elevator")){
                scene[1][l]="Elevator";
                l++;
            }
            if(labels.get(m).equals("Door")){
                //if lidar 5mm Door
                scene[1][l]="Door";
                l++;
            }
            if(labels.get(m).equals("Corridor")){
                scene[1][l]="Corridor";
                l++;
            }
            if(labels.get(m).equals("Tree")){
                scene[1][l]="Tree";
                l++;
            }
            if(labels.get(m).equals("Staircase")) {
                scene[1][l]="Staircase";
                l++;
            }
            if(labels.get(m).equals("Trash Can")) {
                scene[1][l]="Trash Can";
                l++;
            }
            if(labels.get(m).equals("Plant")){
                scene[1][l]="Plant";
                l++;
            }
            if(labels.get(m).equals("Guard Rail")){
                scene[1][l]="Guard Rail";
                l++;
            }
            if(scene[2][p]!=null) {
                //special addition
                if((labels.get(m).equals("Train")&&isCorridor)||(labels.get(m).equals("Train Station")&&isCorridor)){
                    scene[2][p] = "";
                }
                //not wanted
                String[] NeglectLabelTable1= {"Tarmac","Asphalt","Transportation","Urban","City","Building","Town","Wheel","Machine","Highway"
                        ,"Tar","Pedestrian","Neighborhood","Vespa","Suburb","Plywood","Furniture","Porch","Wood","Flagstone","Automobile","Bridge"
                        ,"Metropolis","Housing","Architecture","Airport","Sundial","Boat","Barge","Watercraft","Vessel","Vehicle","Train Station"
                        ,"Train","Subway","Aircraft","Takeoff","Tabletop","Reception","Subway","Missile","Rocket","Spaceship","Airplane","Flight"
                };//add String that has to be Search here
                int NeglectLabelcount1=NeglectLabelTable1.length;
                for(int counter=0;counter<NeglectLabelcount1;counter++) {
                    scene[2][p] = ReplaceLabel(labels, m, NeglectLabelTable1[counter], scene, 2, p,"");
                }

                //wanted but weird
                String[] SearchedLabelTable1= {"Person","Armchair","Desk","Bike","Bar Stool","Motor Scooter","Walkway","Pavement","Alley"
                };//add String that has to be Search here
                int SearchLabelcount1=SearchedLabelTable1.length;
                String[] ReplaceLabelTable1={"Person","Chair","Table","Bicycle","Stool","Motorcycle","Sidewalk","Sidewalk","Alleyway"
                };//*need to be the same length of SearchedWordTable
                for(int counter=0;counter<SearchLabelcount1;counter++) {
                    scene[2][p] = ReplaceLabel(labels, m, SearchedLabelTable1[counter], scene, 2, p, ReplaceLabelTable1[counter]);
                }
                //window not parent wont broadcast
                    /*
                    if(printout[m].equals("Window")){
                        scene[1][l]="Window";
                    }
                     */
                if(scene[2][p].equals("Plastic Bag")&&isTrashCan){
                    scene[2][p]="";
                }
                if(!scene[2][p].equals("")) {
                    p++;
                }
            }
            //for those are the onlyone child
            if(labels.get(m).equals("Shelf")){
                scene[2][p]="Shelf";
                p++;
            }
            if(labels.get(m).equals("Elevator")){
                scene[2][p]="Elevator";
                p++;
            }
            if(labels.get(m).equals("Door")){
                //if lidar 5mm Door
                scene[2][p]="Door";
                p++;
            }
            if(labels.get(m).equals("Corridor")){
                scene[2][p]="Corridor";
                p++;
            }
            if(labels.get(m).equals("Tree")){
                scene[2][p]="Tree";
                p++;
            }
            if(labels.get(m).equals("Staircase")) {
                scene[2][p]="Staircase";
                p++;
            }
            if(labels.get(m).equals("Trash Can")) {
                scene[2][p]="Trash Can";
                p++;
            }
            if(labels.get(m).equals("Plant")){
                scene[2][p]="Plant";
                p++;
            }
            if(labels.get(m).equals("Guard Rail")){
                scene[2][p]="Guard Rail";
                p++;
            }
            if(scene[3][n]!=null) {
                //not wanted
                String[] NeglectLabelTable3= {"Electronics","Hardware","Server","Interior","Label","Boiling","Solar Panels","Texting"
                };//add String that has to be Search here
                int NeglectLabelcount3=NeglectLabelTable3.length;
                for(int counter=0;counter<NeglectLabelcount3;counter++) {
                    scene[3][n] = ReplaceLabel(labels, m, NeglectLabelTable3[counter], scene, 3, n,"");
                }
                //wanted but weird
                String[] SearchedLabelTable3= {"Armchair","Desk","Monitor","Display","Computer","Pc","Computer Keyboard","Adapter","Spray Can"
                        ,"Analog Clock","Digital Clock","Mobile Phone","Window","Potty","Laptop","Clothes Iron","Desktop","Toilet Paper","Bar Stool",
                        "Hand-Held Computer","Measuring Cup","Pop Bottle","Television"
                };//add String that has to be Search here
                int SearchLabelcount3=SearchedLabelTable3.length;
                String[] ReplaceLabelTable3={"Chair","Table","Screen","Screen","personal Computer","personal Computer","Keyboard","Adapter","Can"
                        ,"Clock","Clock","Cell Phone","Window","Toilet","personal Computer","Iron","personal Computer","Tissue","Stool","personal Computer"
                        ,"Cup","Bottle","TV"
                };//*need to be the same length of SearchedWordTable
                for(int counter=0;counter<SearchLabelcount3;counter++) {
                    scene[3][n] = ReplaceLabel(labels, m, SearchedLabelTable3[counter], scene, 3, n, ReplaceLabelTable3[counter]);
                }
                Log.w("allscene", "if!=nullelse" + scene[0][k]);

                if(!scene[3][n].equals("")) {
                    n++;
                }
                //Diaper
                //Tissue
//Reception 接待3
            }
            if(labels.get(m).equals("Sink")) {
                scene[3][n]="Sink";
                n++;
            }
            if(labels.get(m).equals("Pen")){
                scene[3][n]="Pen";
                n++;
            }
            if(labels.get(m).equals("Marker")){
                scene[3][n]="Pen";
                n++;
            }
            if(labels.get(m).equals("Bag")){
                if(!this.isTrashCan) {
                    scene[3][n] = "Backpack";
                    n++;
                }
            }
            if(labels.get(m).equals("Bottle")){
                scene[3][n]="Bottle";
                n++;
            }
            if(labels.get(m).equals("Mirror")){
                scene[3][n]="Mirror";
                n++;
            }
            if(labels.get(m).equals("Wok")){
                scene[3][n]="Wok";
                n++;
            }
            if(labels.get(m).equals("Frying Pan")){
                scene[3][n]="Frying Pan";
                n++;
            }
            if(labels.get(m).equals("Projector")){
                scene[3][n]="Projector";
                n++;
            }
            if(labels.get(m).equals("Knife")){
                scene[3][n]="Knife";
                n++;
            }
            if(labels.get(m).equals("Jar")){
                scene[3][n]="Jar";
                n++;
            }
            if(labels.get(m).equals("Tape")){
                scene[3][n]="Tape";
                n++;
            }
            if(labels.get(m).equals("Book")){
                scene[3][n]="Book";
                n++;
            }
            if(labels.get(m).equals("Scissors")){
                scene[3][n]="Scissors";
                n++;
            }
            if(labels.get(m).equals("Adapter")){
                scene[3][n]="Adapter";
                n++;
            }

            isbox[m]=result.getLabels().get(m).getBoxes().isEmpty();
            if(!isbox[m])
            {
                LabelNumber=m;
                biggest[m]=0;
                boxmax[m]=result.getLabels().get(LabelNumber).getBoxes().size();
                for (i=0;i<boxmax[m];i++) {
                    Anumber[m][i] = PersonCalculate(result, m, i, boxheight[m], boxwidth[m]);
                    if (Anumber[m][i]>biggest[m]){biggest[m]=Anumber[m][i];}
                    if(labels.get(m).equals("Staircase")||labels.get(m).equals("Handrail")) {
                        Xnumber[m][i]=result.getLabels().get(m).getBoxes().get(i).centerX();
                        this.isStairCase=true;
                        this.isHandrail=false;
                        if( Xnumber[m][i]<0.4&& Xnumber[m][i]>=0){
                            this.isStairCaseLeft=true;
                        }
                        else if( Xnumber[m][i]<=0.6&& Xnumber[m][i]>=0.4){
                            this.isStairCaseMiddle=true;
                        }
                        else if( Xnumber[m][i]>0.6&& Xnumber[m][i]<=1){
                            this.isStairCaseRight=true;
                        }
                    }
                }
                if(labels.get(m).equals("Person")){
                    if(biggest[m]>=3.61){//0.33*0.5
                        this.isPerson=true;
                    }else{
                        isPerson=false;
                    }
                    this.distance=((biggest[m]-4.57)/(-0.24))+1;
                    test+="biggestpersonboxsize:"+biggest[m]+" "+"persondistance:"+this.distance+"\n";}
                if(labels.get(m).equals("Car")){
                    this.CarDistance=(((biggest[m]-4.205)*11)/(-0.871))+2;
                    test+="biggestcarboxsize:"+biggest[m]+" "+"cardistance:"+this.distance+"\n";
                    if(this.CarDistance<=0) {
                        this.CarDistance=0;
                    }
                }
            }
            else if(isbox[m])
            {
                Log.w("for","nobox");
            }
        }
        if(this.isMannequin){
            this.isPerson=false;
        }
        /*
        if(k!=0){k--;}
        if(l!=0){l--;}
        if(n!=0){n--;}

         */


        if(this.isFood||this.isDrink||this.isCultery||this.isBeverage||this.isWater) {this.mode=0;
            Log.w("for","eat");}

        if(((this.isFloor)||(this.isFlooring)||(this.isBuilding)||(this.isPath)||(this.isFurniture)||(this.isCan)||(this.isRoad)||this.isBox||this.isTransportation||this.isTree||this.isHandrail||this.isStairCase
                ||this.isStairCaseLeft||this.isStairCaseMiddle||this.isStairCaseRight||this.isTile||this.isBanister
        ||this.isElevator||this.isLobby||this.isCorridor||this.isWalkway||this.isRailing||this.isUrban||this.isHighRise||this.isCity)&&((!this.isBeverage)&&(!this.isIndoors))) {
            this.mode = 1;
            if (scene[1][0] != null) {
                if (scene[1][l] == null) {
                    l--;
                }
                int z = 0;
                for (int count = 0; count < l; count++) {
                    if (scene[1][count].equals("Pillow") && this.isBathroom) {
                        for (int h = count; h < l; h++) {
                            scene[1][h] = scene[1][h + 1];
                        }
                    }
                    if (scene[1][count].equals("Plastic Bag") && this.isTrashCan) {
                        for (int h = count; h < l; h++) {
                            scene[1][h] = scene[1][h + 1];
                        }
                    }
                    if (scene[1][count].equals("Backpack") && this.isTrashCan) {
                        for (int h = count; h < l; h++) {
                            scene[1][h] = scene[1][h + 1];
                        }
                    }
                    if (scene[1][count].equals("Cooler") && this.isTrashCan) {
                        for (int h = count; h < l; h++) {
                            scene[1][h] = scene[1][h + 1];
                        }
                    }
                }
            }
        }

            if(((this.isFloor)||(this.isFlooring)||(this.isBuilding)||(this.isPath)||(this.isFurniture)||(this.isCan)||(this.isRoad)||this.isBox||this.isTree||this.isHandrail||this.isStairCase
                    ||this.isStairCaseLeft||this.isStairCaseMiddle||this.isStairCaseRight||this.isTile||this.isBanister
                    ||this.isElevator||this.isLobby||this.isCorridor||this.isWalkway||this.isRailing||this.isUrban||this.isHighRise||this.isCity)&&(!this.isBeverage)) {
                this.mode = 2;
                if (scene[2][0] != null) {
                    if (scene[2][p] == null) {
                        p--;
                    }
                    for (int count = 0; count < p; count++) {
                        if (scene[2][count].equals("Pillow") && this.isBathroom) {
                            for (int h = count; h < p; h++) {
                                scene[2][h] = scene[2][h + 1];
                            }
                        }
                        if (scene[2][count].equals("Plastic Bag") && this.isTrashCan) {
                            for (int h = count; h < p; h++) {
                                scene[2][h] = scene[2][h + 1];
                            }
                        }
                        if (scene[2][count].equals("Backpack") && this.isTrashCan) {
                            for (int h = count; h < p; h++) {
                                scene[2][h] = scene[2][h + 1];
                            }
                        }
                        if (scene[2][count].equals("Cooler") && this.isTrashCan) {
                            for (int h = count; h < p; h++) {
                                scene[2][h] = scene[2][h + 1];
                            }
                        }
                    }
                }
            }
        if(((this.isIndoors)||(this.isElectronics)||(this.isRoom)||(this.isBag)||(this.isCan)||(this.isTowel)||this.isMarker||this.isPen||this.isText
                ||this.isPot||this.isBottle||this.isWok||this.isFryPan||this.isProjector||this.isKnife||this.isCup||this.isElectricalDevice
        ||this.isTape||this.isBook||this.isScissors||this.isAdapter)&&((!isFloor)&&(!isFlooring)&&(!isTransportation)&&(!isDrink)&&(!isBeverage)&&(!isLobby)&&(!isCorridor)&&(!isElevator)&&(!isUrban)&&(!isHighRise)&&(!isHandrail))) {
            this.mode = 3;
            Log.w("Screen", String.valueOf(scene[3].length));

                Log.w("Screen","count n "+n);
              //  Log.w("Screen",scene[3][n]);
                int z = 0;

        if(scene[3][0]!=null) {
            if(scene[3][n]==null){n--;}
        for (int count = 0; count < n; count++) {
            if (scene[3][count].equals("Pillow") && this.isBathroom) {
                for (int h = count; h < n; h++) {
                    scene[3][h] = scene[3][h + 1];
                }
                if(scene[3][count+1]==null||scene[3][count+1].equals("")){
                    scene[3][count]="";
                }

            }
            if ((scene[3][count].equals("Plastic Bag") && this.isTrashCan) ) {
                for (int h = count; h < n; h++) {
                    scene[3][h] = scene[3][h + 1];
                }
                if (scene[3][count + 1] == null || scene[3][count + 1].equals("")) {
                    scene[3][count] = "";
                }
            }
            if (scene[3][count].equals("Backpack") && this.isTrashCan) {
                    for (int h = count; h < n; h++) {
                        scene[3][h] = scene[3][h + 1];
                    }
                    if(scene[3][count+1]==null||scene[3][count+1].equals("")){
                        scene[3][count]="";
                    }

            }
            if (scene[3][count].equals("Cooler") && this.isTrashCan) {
                for (int h = count; h < n; h++) {
                    scene[3][h] = scene[3][h + 1];
                }
                if(scene[3][count+1]==null||scene[3][count+1].equals("")){
                    scene[3][count]="";
                }


            }
            if (scene[3][count].equals("Screen") && this.isGray) {
                Log.w("Screen", "isingr");
                for (int h = count; h < n; h++) {
                    scene[3][h] = scene[3][h + 1];

                }
                if(scene[3][count+1]==null||scene[3][count+1].equals("")){
                    scene[3][count]="";
                }

            }
            if (scene[3][count].equals("Screen") && this.isWhiteBoard) {
                Log.w("Screen", "isinBoard");
                for (int h = count; h < n; h++) {
                    scene[3][h] = scene[3][h + 1];
                }
                if(scene[3][count+1]==null||scene[3][count+1].equals("")){
                    scene[3][count]="";
                }
            }
            if (scene[3][count].equals("Screen") && this.isWindow) {
                Log.w("Screen", "isinBoard");
                for (int h = count; h < n; h++) {
                    scene[3][h] = scene[3][h + 1];
                }
                if(scene[3][count+1]==null||scene[3][count+1].equals("")){
                    scene[3][count]="";
                }
            }
            if (scene[3][count].equals("Scissors") && this.isElectricalDevice) {
                Log.w("Screen", "isindesk");
                for (int h = count; h < n; h++) {
                    scene[3][h] = scene[3][h + 1];
                }
                if(scene[3][count+1]==null||scene[3][count+1].equals("")){
                    scene[3][count]="";
                }
            }
            if (scene[3][count].equals("Scissors") && this.isElectronics) {
                Log.w("Screen", "isindesk");
                for (int h = count; h < n; h++) {
                    scene[3][h] = scene[3][h + 1];
                }
                if(scene[3][count+1]==null||scene[3][count+1].equals("")){
                    scene[3][count]="";
                }

            }


        }
        }
            Log.w("for", String.valueOf(this.mode));
        }
        test+="mode:"+this.mode+"\n";
        //test+="GPS{latitude,longtutude}:"+this.latitude+","+this.longitude;
        sendMessage(test);
        Log.w("leeeeroy1",String.valueOf(this.mode));
        Log.e("person", String.valueOf(person2));

        if (scene[this.mode][0] == null||(scene[this.mode][0].equalsIgnoreCase(scene[this.mode][1]))||(scene[this.mode][0].equalsIgnoreCase((scene[this.mode][2])))||((scene[this.mode][0].equalsIgnoreCase(scene[this.mode][1]))&&(scene[this.mode][0].equalsIgnoreCase(scene[this.mode][2])))) {
            scene[this.mode][0] = "";
        } else {
            scene[this.mode][0] += ",";
        }
        if (scene[this.mode][1] == null||(scene[this.mode][1].equalsIgnoreCase((scene[this.mode][2])))||((scene[this.mode][0].equalsIgnoreCase(scene[this.mode][1]))&&(scene[this.mode][0].equalsIgnoreCase(scene[this.mode][2])))) {
            scene[this.mode][1] = "";
        } else {
            scene[this.mode][1] += ",";
        }
        if (scene[this.mode][2] == null) {
            scene[this.mode][2] = "";
        }
        output = scene[mode][0] + scene[mode][1] + scene[mode][2];

        Log.w("output","mode="+this.mode);
        Log.w("output",output);
        textTranslate(output);//pass to textTranslate function

    }

    private String ReplaceLabel(ConcurrentHashMap labelMatrix,int numoflabel,String SearchedLabel,String[][] scene,int typeofscence,int numofscence,String SavedLabel){
        if(labelMatrix.get(numoflabel).equals(SearchedLabel)){
            scene[typeofscence][numofscence]=SavedLabel;
        }
        return scene[typeofscence][numofscence];
    }
    private String SavetypeLabel(String[][] ParentlabelMatrix,int numoflabel,int numofparent,String SearchedLabel,String[][] scene,int typeofscence,int numofscence,String[]labelMatrix){
        if(ParentlabelMatrix[numoflabel][numofparent].equals(SearchedLabel)){
            scene[typeofscence][numofscence]=labelMatrix[numoflabel];
        }
        return scene[typeofscence][numofscence];
    }

    private double PersonCalculate(IdentifyLabelsResult result,int labelnumber,int boxnumber,double boxheight,double boxwidth)
    {
        double A=0;
        boxheight=240*result.getLabels().get(labelnumber).getBoxes().get(boxnumber).height();//result from detectlabel  result.get(0)first label  result.get(0).getboxes() bounding box of first object A=.height *.width
        boxwidth=320*result.getLabels().get(labelnumber).getBoxes().get(boxnumber).width();
        A =boxheight*boxwidth;
        A=Math.log10((A));
        return A;

        //result.getLabels().get(0).getBoxes().isEmpty();
    }

    private double comparenumber(double a,double b,double c)
    {
        double max=a;
        if(b>max)
        {
            max=b;
        }
        if(c>max) {
            max = c;
        }
        return max;
    }
    //String[][]
    public  String[][] RemoveMatrix(String[][] modematrix,int mode,int currentcount,int size ) {
        String[][] anotherArray = new String[mode][size];
        if(modematrix==null||currentcount<0||currentcount>size) {return modematrix;}
        else {
            for (int h=0,g=0;h<size;h++) {
                if(h==currentcount){
                    continue;
                }
                anotherArray[mode][g++] = modematrix[mode][h];
            }
            return anotherArray;
        }
    }
    //translate function
    private void textTranslate(String LabelResult) {
        Log.w("text",LabelResult);
        if(Locale.getDefault().toString().equals("zh_TW")||Locale.getDefault().toString().equals("zh_TW_#Hant") ){
            Amplify.Predictions.translateText(
                    LabelResult,
                    LanguageType.ENGLISH,
                    LanguageType.CHINESE_SIMPLIFIED,
                    result -> TextDataHold((TranslateTextResult) result)//pass to TextDataHold function
                    ,
                    error -> Log.e("MyAmplifyApp", "Translation failed", error)
            );
        }
        Log.w("Language",Locale.getDefault().toString());

        if(Locale.getDefault().toString().equals("ja_JP")){
            Amplify.Predictions.translateText(
                    LabelResult,
                    LanguageType.ENGLISH,
                    LanguageType.JAPANESE,
                    result -> TextDataHold((TranslateTextResult) result)//pass to TextDataHold function
                    ,
                    error -> Log.e("MyAmplifyApp", "Translation failed", error)
            );
        }
        //ja_JP

    }

    //This is a function for converting to speech
    //google ttl function
    private void TextDataHold(TranslateTextResult Textdata) {
        SharedPreferences VasPref=getSharedPreferences("com.wusasmart.vas_preferences",0); //switch Vas and Smart Phone
        String SoundDevice=VasPref.getString("SoundDevice",null);
        Log.e("Sound", String.valueOf(SoundDevice));
        try {
            boolean[] isObjectsTable=
                    {       this.isCar
                            ,this.isHandrail
                            ,this.isStairCaseLeft
                            ,this.isStairCaseMiddle
                            ,this.isStairCaseRight
                            ,this.isPerson
                    };//add priortyBroadcastlabel

            int isObjectcount=isObjectsTable.length;

            String[] ReadOutTable=
                    {       "小心有車"
                            ,"警告!前方有扶手"
                            ,"左前方" + String.valueOf(Math.round(distance) )+ "公尺內有樓梯"
                            , "前方" + String.valueOf(Math.round(distance) )+ "公尺內有樓梯"
                            ,"右前方" + String.valueOf(Math.round(distance) )+ "公尺內有樓梯"
                            ,"前方" + String.valueOf(Math.round(distance)) + "公尺內有人"
                    };//*need to be the same length of isObjectsTable

            if(SoundDevice.equals("smart_phone")) {
                for (int counter = 0; counter < isObjectcount; counter++) {
                    ReadOut(isObjectsTable[counter], ReadOutTable[counter]);
                }
            }

            Thread.sleep(50);

            //adjust weird output
            String toSpeak = Textdata.getTranslatedText();

            String[] SearchedWordTable= {"主席", "表","个人电脑","个人计算机","屏幕","鼠标","计算机硬件","可以","文件文件夹","包裹递送","内阁","斑马隧道","塑料袋","酸奶"
            ,"流行瓶","背包","文档","卫生间","台式机","炉灶","煤气灶","荷兰烤箱","勺子","電飯煲","投影仪","拖拉机","生產","预订","磁带","适配器","，"
            };//add String that has to be Search here
            int Searchcount=SearchedWordTable.length;
            String[] ReplaceWordTable={"椅子","桌子","電腦","電腦","螢幕","滑鼠","電腦硬體","罐子","文件夾","包裹","橱柜","斑馬線","塑膠袋","優格"
            ,"寶特瓶","包包","文件","廁所","電腦","爐","瓦斯爐","荷蘭鍋","湯匙","電鍋","投影機","曳引機","產品","書","膠帶","變壓器",""
            };//*need to be the same length of SearchedWordTable

            //adjust weird output
            for(int counter=0;counter<Searchcount;counter++) {
                toSpeak=ReplaceChinese(toSpeak,SearchedWordTable[counter],ReplaceWordTable[counter]);
            }
            MP3save(toSpeak);
            Thread.sleep(50);
            if(SoundDevice.equals("smart_phone")) {
                t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, stringmap);


                while (t1.isSpeaking()) {
                }
                ;
                new Waiter().execute();
                Thread.sleep(50);
            }

            if (isText)
            {
                this.isText=false;
            }

        }catch (InterruptedException | IOException error) {
            Log.e("MyAmplifyApp", "Error writing audio file", error);
        }
        Log.w("text",Textdata.getTranslatedText());
    }

    private String ReplaceChinese(String BroadCastSentence,String SearchedWord,String ReplaceWord){
        //adjust weird output
        if (BroadCastSentence.indexOf(SearchedWord) > -1) {
            BroadCastSentence=BroadCastSentence.replace(SearchedWord,ReplaceWord);
        }
        return BroadCastSentence;
    }

    private void ReadOut(boolean Objects,String ReadSentence) throws InterruptedException {
        if(Objects) {
            Thread.sleep(50);
            t1.speak(ReadSentence, TextToSpeech.QUEUE_FLUSH, stringmap);
            new Waiter().execute();
            while (t1.isSpeaking()){};
            Objects=false;
            Thread.sleep(50);
        }
    }



    class Waiter extends AsyncTask<Void,Void,Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            while (t1.isSpeaking()){

                // try{Thread.sleep(1000);}catch (Exception e){}
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            //TTS has finished speaking. WRITE YOUR CODE HERE
        }
    }

    /**
     * Updates the UI to read/write mode on the document identified by {@code fileId}.
     */
    private void setReadWriteMode(String fileId) {
        //mFileTitleEditText.setEnabled(true);
        //mDocContentEditText.setEnabled(true);
        this.mOpenFileId = fileId;
    }
    // Supposing that your value is an integer declared somewhere as: int myInteger;
    private void sendMessage(String data) {
        // The string "my-message" will be used to filer the intent
        Intent intent = new Intent("testdata");
        // Adding some data
        intent.putExtra("output", data);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
    private void MP3save(String text) throws IOException {
        String state = Environment.getExternalStorageState();
        boolean mExternalStorageWriteable = false;
        boolean mExternalStorageAvailable = false;
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // Can read and write the media
            mExternalStorageAvailable = mExternalStorageWriteable = true;

        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // Can only read the media
            mExternalStorageAvailable = true;
            mExternalStorageWriteable = false;
        } else {
            // Can't read or write
            mExternalStorageAvailable = mExternalStorageWriteable = false;
        }
       // File root = android.os.Environment.getExternalStorageDirectory();
        File MP3dir =new File("/storage/emulated/0/VAS/");
        //File dir = new File(root.getAbsolutePath() + "/download");
        //dir.mkdirs();
        File file = new File(MP3dir, "myData.mp3");
        int test = t1.synthesizeToFile((CharSequence) text, null, file,
                "tts");
        //File MP3READY = new File("/storage/emulated/0/VAS/", "MP3READY.txt");

        Log.w("text","savesuccess");
        File MP3READY = new File("/storage/emulated/0/VAS/", "MP3READY.txt");
        //File MP3READYpath = new File("/storage/emulated/0/VAS/");
        FileOutputStream outStream = new FileOutputStream(MP3READY);
        outStream.write("0".getBytes());
        outStream.close();
    }

}






