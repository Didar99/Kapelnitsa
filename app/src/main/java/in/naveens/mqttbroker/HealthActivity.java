package in.naveens.mqttbroker;

import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;
import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;
import android.widget.Toast;
import com.gelitenight.waveview.library.WaveView;
import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class HealthActivity extends Activity implements MqttCallback {

    private static final String TAG = "MainActivity";
    private final String pubTopic = "action";
    MqttAndroidClient client;
    TextView textView;
    String globalIP, level;

    WaveView waveView;
    private AnimatorSet mAnimatorSet;
    private int mBorderColor = Color.parseColor("#FFFFFF");
    private int mBorderWidth = 1;
    List<Animator> animators = new ArrayList<>();
    float waterLevel, waterLevelLast;
    boolean notify;
    Vibrator vibrator;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_health);

        textView = findViewById(R.id.textView);
        waveView = findViewById(R.id.wave);
        notify = PrefConfig.loadNotify(getApplicationContext());

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        initAnimation();

        setIP();

        //MQTTConnect options : setting version to MQTT 3.1.1
        MqttConnectOptions options = new MqttConnectOptions();
        options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1);
        options.setUserName("admin");
        options.setPassword("".toCharArray());

        //Below code binds MainActivity to Paho Android Service via provided MqttAndroidClient
        // client interface
        //Todo : Check why it wasn't connecting to test.mosquitto.org. Isn't that a public broker.
        //Todo : .check why client.subscribe was throwing NullPointerException  even on doing subToken.waitForCompletion()  for Async                  connection estabishment. and why it worked on subscribing from within client.connect’s onSuccess(). SO

        String mqttUrl = "tcp://" + globalIP + ":1883";
        String clientId = MqttClient.generateClientId();
        client =
                new MqttAndroidClient(getApplicationContext(), mqttUrl,
                        clientId);
        try {
            IMqttToken token = client.connect(options);
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // We are connected
                    Log.d(TAG, "onSuccess");
                    Toast.makeText(getApplicationContext(), "Üstünlikli baglanyldy", Toast.LENGTH_SHORT).show();
                    //Subscribing to a topic car/gpio/status on broker mqtt.flespi.io
                    client.setCallback(HealthActivity.this);
                    final String subTopic = "status";
                    int qos = 1;
                    try {
                        IMqttToken subToken = client.subscribe(subTopic, qos);
                        subToken.setActionCallback(new IMqttActionListener() {
                            @Override
                            public void onSuccess(IMqttToken asyncActionToken) {
                                // successfully subscribed
//                                Toast.makeText(getActivity(), "Successfully subscribed to: " + subTopic, Toast.LENGTH_SHORT).show();
//                                pubAllStatus();

//                                dialog.dismiss();
                            }
                            @Override
                            public void onFailure(IMqttToken asyncActionToken,
                                                  Throwable exception) {
                                // The subscription could not be performed, maybe the user was not
                                // authorized to subscribe on the specified topic e.g. using wildcards
                                Toast.makeText(getApplicationContext(), "Serwere baglanylmady" + subTopic, Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (MqttException e) {
                        e.printStackTrace();
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Something went wrong e.g. connection timeout or firewall problems
                    Log.d(TAG, "onFailure");
//                            Toast.makeText(getActivity(), "Connection failed", Toast.LENGTH_SHORT).show();
//                            Snackbar.make(requireActivity().findViewById(R.id.constraint), R.string.no_internet, Snackbar.LENGTH_SHORT).show();
                    Toast.makeText(getApplicationContext(), "Internet baglanyşygy ýok", Toast.LENGTH_SHORT).show();
//                    dialog.dismiss();
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void connectionLost(Throwable cause) {}

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {

//        Log.d("door",message.toString());
        if(message.toString().equals("101")){
            fullDegree();
        } else if (message.toString().equals("100")){
            emptyDegree();
        } else if(message.toString().equals("201")) {
            fullDegree2();
        } else if (message.toString().equals("200")) {
            emptyDegree2();
        } else if (message.toString().equals("301")) {
            fullDegree3();
        }else if (message.toString().equals("300")) {
            emptyDegree3();
        } else if (message.toString().equals("401")) {
            fullDegree4();
        } else if (message.toString().equals("400")) {
            emptyDegree4();
        }
//        Toast.makeText(getActivity(), "Topic: "+topic+"\nMessage: "+message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {}

    public void pubAllStatus() {
        String payload = "1";
        byte[] encodedPayload = new byte[0];
        try {
            encodedPayload = payload.getBytes(StandardCharsets.UTF_8);
            MqttMessage message = new MqttMessage(encodedPayload);
            client.publish(pubTopic, message);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void fullDegree() {
        textView.setText("25%");
        level = "25%";
        textView.setTextColor(getResources().getColor(R.color.blue_500));
        waterLevelLast = waterLevel;
        waterLevel = 0.2f;
        liquidDegree();
        waveStart();
        Log.e("DEGREE => ", "FULL");
        if (!notify) {
            createNotification(getApplicationContext());
        }
    }
    public void fullDegree2() {
        textView.setText("45%");
        level = "45%";
        textView.setTextColor(getResources().getColor(R.color.blue_500));
        waterLevelLast = waterLevel;
        waterLevel = 0.4f;
        liquidDegree();
        waveStart();
        Log.e("DEGREE 2 => ", "FULL");
        if (!notify) {
            createNotification(getApplicationContext());
        }
    }
    public void fullDegree3() {
        textView.setText("65%");
        level = "65%";
        textView.setTextColor(getResources().getColor(R.color.white));
        waterLevelLast = waterLevel;
        waterLevel = 0.6f;
        liquidDegree();
        waveStart();
        Log.e("DEGREE 3 => ", "FULL");
        if (!notify) {
            createNotification(getApplicationContext());
        }
    }
    public void fullDegree4() {
        textView.setText("85%");
        level = "85%";
        textView.setTextColor(getResources().getColor(R.color.white));
        waterLevelLast = waterLevel;
        waterLevel = 0.8f;
        liquidDegree();
        waveStart();
        Log.e("DEGREE 4 => ", "FULL");
        if (!notify) {
            createNotification(getApplicationContext());
        }
    }
    public void emptyDegree() {
        textView.setText("15%");
        level = "15%";
        textView.setTextColor(getResources().getColor(R.color.blue_500));
        waterLevelLast = waterLevel;
        waterLevel = 0.1f;
        liquidDegree();
        waveStart();
        Log.e("DEGREE => ", "EMPTY");
        if (!notify) {
            createNotification(getApplicationContext());
        }
    }
    public void emptyDegree2() {
        textView.setText("35%");
        level = "35%";
        textView.setTextColor(getResources().getColor(R.color.blue_500));
        waterLevelLast = waterLevel;
        waterLevel = 0.3f;
        liquidDegree();
        waveStart();
        Log.e("DEGREE 2 => ", "EMPTY");
        if (!notify) {
            createNotification(getApplicationContext());
        }
    }
    public void emptyDegree3() {
        textView.setText("55%");
        level = "55%";
        textView.setTextColor(getResources().getColor(R.color.white));
        waterLevelLast = waterLevel;
        waterLevel = 0.5f;
        liquidDegree();
        waveStart();
        Log.e("DEGREE 3 => ", "EMPTY");
        if (!notify) {
            createNotification(getApplicationContext());
        }
    }
    public void emptyDegree4() {
        textView.setText("75%");
        level = "75%";
        textView.setTextColor(getResources().getColor(R.color.white));
        waterLevelLast = waterLevel;
        waterLevel = 0.7f;
        liquidDegree();
        waveStart();
        Log.e("DEGREE 4 => ", "EMPTY");
        if (!notify) {
            createNotification(getApplicationContext());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        waveCancel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        waveStart();
    }

    public void waveStart() {
        waveView.setShowWave(true);
        if (mAnimatorSet != null) {
            mAnimatorSet.start();
        }
    }

    public void waveCancel() {
        if (mAnimatorSet != null) {
//            mAnimatorSet.cancel();
            mAnimatorSet.end();
        }
    }


    private void initAnimation() {
        // horizontal animation.
        // wave waves infinitely.
        ObjectAnimator waveShiftAnim = ObjectAnimator.ofFloat(
                waveView, "waveShiftRatio", 0f, 1f);
        waveShiftAnim.setRepeatCount(ValueAnimator.INFINITE);
        waveShiftAnim.setDuration(1000);
        waveShiftAnim.setInterpolator(new LinearInterpolator());
        animators.add(waveShiftAnim);

        // vertical animation.
        // water level increases from 0 to center of WaveView
        ObjectAnimator waterLevelAnim = ObjectAnimator.ofFloat(
                waveView, "waterLevelRatio", 0.0f, 0.0f);
        waterLevelAnim.setDuration(10000);
        waterLevelAnim.setInterpolator(new DecelerateInterpolator());
        animators.add(waterLevelAnim);

        // amplitude animation.
        // wave grows big then grows small, repeatedly
        ObjectAnimator amplitudeAnim = ObjectAnimator.ofFloat(
                waveView, "amplitudeRatio", 0.0001f, 0.05f);
        amplitudeAnim.setRepeatCount(ValueAnimator.INFINITE);
        amplitudeAnim.setRepeatMode(ValueAnimator.REVERSE);
        amplitudeAnim.setDuration(5000);
        amplitudeAnim.setInterpolator(new LinearInterpolator());
        animators.add(amplitudeAnim);

        mAnimatorSet = new AnimatorSet();
        mAnimatorSet.playTogether(animators);


        waveView.setBorder(mBorderWidth, mBorderColor);
        waveView.setShapeType(WaveView.ShapeType.SQUARE);
        waveView.setWaveColor(
                Color.parseColor("#6600B8D4"),
                Color.parseColor("#660091EA"));
    }

    public void liquidDegree() {
        // vertical animation.
        // water level increases from 0 to center of WaveView
        ObjectAnimator waterLevelAnim = ObjectAnimator.ofFloat(
                waveView, "waterLevelRatio", waterLevelLast, waterLevel);
        waterLevelAnim.setDuration(10000);
        waterLevelAnim.setInterpolator(new DecelerateInterpolator());
        animators.add(waterLevelAnim);

        mAnimatorSet = new AnimatorSet();
        mAnimatorSet.playTogether(animators);
    }
    private void setIP() {
        SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(this).edit();
        prefs.putString(getString(R.string.mqtt_host), Utils.getIPAddress(true));
        prefs.apply();
        globalIP = Utils.getIPAddress(true);
    }


    public void createNotification(Context mContext) {
        NotificationManager mNotificationManager;

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(mContext.getApplicationContext(), "notify_001");
        Intent ii = new Intent(mContext.getApplicationContext(), HealthActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, ii, 0);

        NotificationCompat.BigTextStyle bigText = new NotificationCompat.BigTextStyle();
        bigText.bigText("Damjalaýyn sanjymyň derejesi pes: " + level);
        bigText.setBigContentTitle("Duýduryş");
        bigText.setSummaryText("Häzirki wagt");

        mBuilder.setContentIntent(pendingIntent);
        mBuilder.setSmallIcon(android.R.drawable.ic_dialog_info);
        mBuilder.setContentTitle("Duýduryş");
        mBuilder.setContentText("Damjalaýyn sanjymyň derejesi pes: " + level);
        mBuilder.setPriority(Notification.PRIORITY_MAX);
        mBuilder.setStyle(bigText);

        mNotificationManager =
                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

// === Removed some obsoletes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            String channelId = "Your_channel_id";
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_HIGH);
            mNotificationManager.createNotificationChannel(channel);
            mBuilder.setChannelId(channelId);
        }

        mNotificationManager.notify(0, mBuilder.build());

        // Vibrate for 500 milliseconds
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(2000, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            //deprecated in API 26
            vibrator.vibrate(2000);
        }
    }
}