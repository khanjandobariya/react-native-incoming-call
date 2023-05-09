package com.incomingcall;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.view.View;
import android.os.Vibrator;
import android.content.Context;

import java.util.Timer;
import java.util.TimerTask;
import android.app.Activity;
import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;


public class UnlockScreenActivity extends AppCompatActivity implements UnlockScreenActivityInterface {

    private static final String TAG = "MessagingService";
    private TextView tvName;
    private TextView tvInfo;
    private Integer timeout = 0;
    private String uuid = "";
    static boolean active = false;
    private static Vibrator vibrator;
    private static Ringtone ringtone;
    private static Activity fa;
    private Timer timer;
    private Button button;
    static UnlockScreenActivity instance;

    private  String UName = "";
    private  String UChannel = "";


    public static UnlockScreenActivity getInstance() {
      return instance;
    }


    @Override
    public void onStart() {
        super.onStart();
        if (this.timeout > 0) {
              timer = new Timer();
              timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    // this code will be executed after timeout seconds
                    dismissIncoming();
                }
            }, timeout);
        }
        active = true;
        instance = this;
    }

    @Override
    public void onStop() {
        super.onStop();
        active = false;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        fa = this;
        setContentView(R.layout.activity_call_incoming);
        button = (Button) findViewById(R.id.avatar);
        tvName = findViewById(R.id.tvName);
        tvInfo = findViewById(R.id.tvInfo);


        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            if (bundle.containsKey("uuid")) {
                uuid = bundle.getString("uuid");
            }
            if (bundle.containsKey("channelname")) {
                String name = bundle.getString("channelname");
                UChannel = name;
                String str = name.toString();

                String[] strArray = str.split(" ");
                StringBuilder builder = new StringBuilder();

//First name
                if (strArray.length > 0){
                    builder.append(strArray[0], 0, 1);
                }
//Middle name
                if (strArray.length > 1){
                    builder.append(strArray[1], 0, 1);
                }
//Surname
                if (strArray.length > 2){
                    builder.append(strArray[2], 0, 1);
                }

                button.setText(builder.toString());
            }
            if (bundle.containsKey("name")) {
                String name = bundle.getString("name");
                UName = name;
            }
            if (bundle.containsKey("info")) {
                String info = bundle.getString("info");
                tvInfo.setText(info);
            }

            if (bundle.containsKey("timeout")) {
                this.timeout = bundle.getInt("timeout");
            }
            else this.timeout = 0;
            tvName.setText(UChannel.concat(UName));
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        startRinging();


        LottieAnimationView acceptCallBtn = findViewById(R.id.ivAcceptCall);
        acceptCallBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    stopRinging();
                    acceptDialing();
                } catch (Exception e) {
                    WritableMap params = Arguments.createMap();
                    params.putString("message", e.getMessage());
                    sendEvent("error", params);
                    dismissDialing();
                }
            }
        });

        AnimateImage rejectCallBtn = findViewById(R.id.ivDeclineCall);
        rejectCallBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopRinging();
                dismissDialing();
            }
        });

    }

    @Override
    public void onBackPressed() {
        // Dont back
    }

    public void dismissIncoming() {
        stopRinging();
        dismissDialing();
    }

    private void startRinging() {
      long[] pattern = {0, 1000, 800};
      vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
      int ringerMode = ((AudioManager) getSystemService(Context.AUDIO_SERVICE)).getRingerMode();
      if(ringerMode == AudioManager.RINGER_MODE_SILENT) return;

      if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        VibrationEffect vibe = VibrationEffect.createWaveform(pattern, 2);
        vibrator.vibrate(vibe);
      }else{
        vibrator.vibrate(pattern, 0);
      }
      if(ringerMode == AudioManager.RINGER_MODE_VIBRATE) return;

      ringtone = RingtoneManager.getRingtone(this, RingtoneManager.getActualDefaultRingtoneUri(getApplicationContext(), RingtoneManager.TYPE_RINGTONE));
      ringtone.play();
    }

    private void stopRinging() {
      if (vibrator != null){
        vibrator.cancel();
      }
      int ringerMode = ((AudioManager) getSystemService(Context.AUDIO_SERVICE)).getRingerMode();
      if(ringerMode != AudioManager.RINGER_MODE_NORMAL) return;
      ringtone.stop();
    }

    private void acceptDialing() {
        WritableMap params = Arguments.createMap();
        params.putBoolean("accept", true);
        params.putString("uuid", uuid);
        params.putString("name", UName);
        params.putString("channel", UChannel);
        if (timer != null){
          timer.cancel();
        }
        if (!IncomingCallModule.reactContext.hasCurrentActivity()) {
            params.putBoolean("isHeadless", true);
        }
        KeyguardManager mKeyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);

        if (mKeyguardManager.isDeviceLocked()) {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mKeyguardManager.requestDismissKeyguard(this, new KeyguardManager.KeyguardDismissCallback() {
              @Override
              public void onDismissSucceeded() {
                super.onDismissSucceeded();
              }
            });
          }
        }

        sendEvent("answerCall", params);
        finish();
    }

    private void dismissDialing() {
        WritableMap params = Arguments.createMap();
        params.putBoolean("accept", false);
        params.putString("uuid", uuid);
        params.putString("name", UName);
        params.putString("channel", UChannel);
        if (timer != null) {
          timer.cancel();
        }
        if (!IncomingCallModule.reactContext.hasCurrentActivity()) {
            params.putBoolean("isHeadless", true);
        }

        sendEvent("endCall", params);

        finish();
    }

    @Override
    public void onConnected() {
        Log.d(TAG, "onConnected: ");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

            }
        });
    }

    @Override
    public void onDisconnected() {
        Log.d(TAG, "onDisconnected: ");

    }

    @Override
    public void onConnectFailure() {
        Log.d(TAG, "onConnectFailure: ");

    }

    @Override
    public void onIncoming(ReadableMap params) {
        Log.d(TAG, "onIncoming: ");
    }

    private void sendEvent(String eventName, WritableMap params) {
        IncomingCallModule.sendEvents(eventName,params);
    }
}
