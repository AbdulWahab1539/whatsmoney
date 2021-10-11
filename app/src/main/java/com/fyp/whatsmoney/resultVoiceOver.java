package com.fyp.whatsmoney;

import android.content.Intent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class resultVoiceOver extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private TextToSpeech tts;
    String str;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate (savedInstanceState);
        setContentView (R.layout.activity_result_voice_over);

        tts = new TextToSpeech(this, this);

        Bundle getResult = getIntent ().getExtras ();
        String result = getResult.getString ("result");
        str = result.replaceAll("\\[", "").replaceAll("\\]","");
        TextView resultTextView  = findViewById (R.id.textView);
        resultTextView.setText (str);
        Log.i("msg",str);

    }
    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(getApplicationContext(), "Language not supported", Toast.LENGTH_SHORT).show();
            } else {
                if(str.contains ("Not a note")){
                    speak("The Captures Image is not a note!! Going Back to camera");
                }
                speak("You Have " + str + "!Going Back to camera");
            }
        } else {
            Toast.makeText(getApplicationContext(), "Init failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void speak(String message) {
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener () {
            @Override
            public void onStart(String s) {
            }

            @Override
            public void onDone(String s) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        Intent Intent = new Intent(resultVoiceOver.this, Camera.class);
                        startActivity(Intent);
                        finish();

                    }
                });


            }

            @Override
            public void onError(String s) {
            }
        });
        Bundle params = new Bundle();
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "");

        tts.speak(message, TextToSpeech.QUEUE_FLUSH, params, "Dummy String");

    }
    @Override
    protected void onPause() {
        finish();
        System.exit(0);
        tts.shutdown();
        super.onPause();
    }

}
