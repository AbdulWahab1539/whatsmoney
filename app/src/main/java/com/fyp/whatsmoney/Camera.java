package com.fyp.whatsmoney;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.wonderkiln.camerakit.CameraKitError;
import com.wonderkiln.camerakit.CameraKitEvent;
import com.wonderkiln.camerakit.CameraKitEventListener;
import com.wonderkiln.camerakit.CameraKitImage;
import com.wonderkiln.camerakit.CameraKitVideo;
import com.wonderkiln.camerakit.CameraView;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class Camera extends AppCompatActivity implements TextToSpeech.OnInitListener{

    private static final String MODEL_PATH = "notes.tflite";
    private static final boolean QUANT = false;
    private static final String LABEL_PATH = "labelscash.txt";
    private static final int INPUT_SIZE = 200;

    private TextToSpeech tts;
    private SpeechRecognizer speechRecognition;
    private static final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 1;

    private Classifier classifier;

    private Executor executor = Executors.newSingleThreadExecutor();
    private Button btnDetectObject;
    private CameraView cameraView;
    private Button micButton;

    Bitmap bitmap;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        cameraView = findViewById(R.id.cameraView);
       // btnToggleCamera = findViewById(R.id.btnToggleCamera);
        btnDetectObject = findViewById(R.id.btnDetectObject);
        micButton = findViewById (R.id.micButton);
        //TextToSpeech Initializer
        tts = new TextToSpeech(this, this);
        initializeSpeechRecognizer();

        cameraView.addCameraKitListener(new CameraKitEventListener() {
            @Override
            public void onEvent(CameraKitEvent cameraKitEvent) {

            }

            @Override
            public void onError(CameraKitError cameraKitError) {

            }

            @Override
            public void onImage(CameraKitImage cameraKitImage) {

                bitmap = cameraKitImage.getBitmap();
                bitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false);
                final List<Classifier.Recognition> results = classifier.recognizeImage(bitmap);

                Intent produceResult = new Intent (getApplicationContext (), resultVoiceOver.class);
                produceResult.putExtra ("result", results.toString ());
                produceResult.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity (produceResult);
                finishActivity ();
            }

            @Override
            public void onVideo(CameraKitVideo cameraKitVideo) {

            }
        });

        btnDetectObject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraView.captureImage();
                cameraView.stop ();
                speak("Image Captured! Please wait while the image is being recognized.");
                btnDetectObject.setVisibility (View.INVISIBLE);
                micButton.setVisibility (View.INVISIBLE);
            }
        });
        micButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                        startListening();
            }
        });

        initTensorFlowAndLoadModel();
    }//End of OnCreate

    @Override
    protected void onResume() {
        super.onResume();
        cameraView.start();
        tts = new TextToSpeech (getApplicationContext (),this) ;
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraView.stop();
        tts.stop ();
        tts.shutdown ();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        tts.shutdown ();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                classifier.close();
            }
        });
    }
   /* public void Clicked(View view){
                startListening();
    }*/

    private void initTensorFlowAndLoadModel() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    classifier = TensorFlowImageClassifier.create(
                            getAssets(),
                            MODEL_PATH,
                            LABEL_PATH,
                            INPUT_SIZE,
                            QUANT);
                } catch (final Exception e) {
                    throw new RuntimeException("Error initializing TensorFlow!", e);
                }
            }
        });
    }

    boolean doubleBackToExitPressedOnce = false;
    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();
        speak("Please press back again to exit");

        new Handler ().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce=false;
            }
        }, 2000);
    }//End of Back Button Method

    public void finishActivity(){
        this.finish ();
    }

    //TextToSpeech Implementation
    private void processResult(String result_message) {
        result_message = result_message.toLowerCase();
        Log.i("msg",result_message);
                switch (result_message) {
                    case "snap":
                    case "take":
                    case "capture":
                    case "picture":
                        btnDetectObject.callOnClick ();
                        break;
                    case "close":
                    case "finish":
                    case "exit":
                    case "stop":
                        new CountDownTimer (2500, 2500) {

                            public void onTick(long millisUntilFinished) {
                                speak ("Good Bye! Looking forward to help you out.");
                            }

                            public void onFinish() {
                                finish ();
                            }
                        }.start ();
                        break;
                    default:
                        speak ("Sorry! I didn't recognized that");
                        break;
                }

            if (result_message.contains ("finish")) {
                finish ();
            }
            if (result_message.contains ("exit")) {
                speak("Bye!") ;
                finish ();
            }
        }

    private void startListening() {
        if (ContextCompat.checkSelfPermission(Camera.this,
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(Camera.this,
                    Manifest.permission.RECORD_AUDIO)) {
                Toast.makeText (this, "permission granted",Toast.LENGTH_SHORT).show ();
            } else {
                ActivityCompat.requestPermissions(Camera.this,
                        new String[]{Manifest.permission.RECORD_AUDIO}, MY_PERMISSIONS_REQUEST_RECORD_AUDIO);
            }
        } else {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
            speechRecognition.startListening(intent);
        }
    }

    private void speak(String message) {

        Bundle params = new Bundle();
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "");
        tts.speak(message, TextToSpeech.QUEUE_FLUSH, params, null);
    }

    private void initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognition = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognition.setRecognitionListener(new RecognitionListener () {
                @Override
                public void onReadyForSpeech(Bundle params) {

                }

                @Override
                public void onBeginningOfSpeech() {

                }

                @Override
                public void onRmsChanged(float rmsdB) {

                }

                @Override
                public void onBufferReceived(byte[] buffer) {

                }

                @Override
                public void onEndOfSpeech() {

                }

                @Override
                public void onError(int error) {

                }

                @Override
                public void onResults(Bundle results) {
                    List<String> result_arr = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    assert result_arr != null;
                    processResult(result_arr.get(0));
                }

                @Override
                public void onPartialResults(Bundle partialResults) {

                }

                @Override
                public void onEvent(int eventType, Bundle params) {

                }
            });
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                //Toast.makeText(getApplicationContext(), "Language not supported", Toast.LENGTH_SHORT).show();
                speak ("Your Language is not supported.");
            } else {
                    speak ("Welcome! Hold the device steady and speak 'snap' to take the picture.");
                    btnDetectObject.setVisibility (View.VISIBLE);
                    micButton.setVisibility (View.VISIBLE);
            }
        } else {
            Toast.makeText(getApplicationContext(), "Init failed", Toast.LENGTH_SHORT).show();
            speak ("Error! Initialization Failed.");
        }
    }//end of TextToSpeech
}
