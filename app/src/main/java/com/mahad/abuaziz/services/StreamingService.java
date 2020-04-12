package com.mahad.abuaziz.services;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.mahad.abuaziz.MainActivity;
import com.mahad.abuaziz.R;
import com.mahad.abuaziz.utils.MyReceiver;

import java.io.IOException;
import java.util.Objects;

import static com.mahad.abuaziz.utils.App.CHANNEL_2;

public class StreamingService extends Service implements
        MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnInfoListener, MediaPlayer.OnBufferingUpdateListener
{

    private MediaPlayer mediaPlayer = new MediaPlayer();
    private boolean isPausedCall = false;
    private BroadcastReceiver broadcastReceiver;
    private static final String TAG = "StreamingService";
    private NotificationManagerCompat notificationManagerCompat;

    @Override
    public void onCreate() {
        Log.e(TAG, "onCreate: Called");
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.e(TAG, "onReceive: "+ action);
                assert action != null;
                switch (action){
                    case "stop":
                        pauseMedia();
                        break;
                    case "start":
                        onPrepared(mediaPlayer);
                        break;
                    case "exit":
                        stopMedia();
                        break;
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction("stop");
        filter.addAction("start");
        filter.addAction("exit");
        registerReceiver(broadcastReceiver, filter);

        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);
        mediaPlayer.setOnInfoListener(this);
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.reset();

        AudioManager.OnAudioFocusChangeListener onAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
            @Override
            public void onAudioFocusChange(int focusChange) {
                switch (focusChange){
                    case (AudioManager.AUDIOFOCUS_LOSS):
                        break;
                    case (AudioManager.AUDIOFOCUS_LOSS_TRANSIENT):
                        try {
                            pauseMedia();
                        } catch (IllegalStateException e){
                            e.printStackTrace();
                            Log.e(TAG, "onAudioFocusChange: Catch AUDIOFOCUS_LOSS_TRANSIENT: ", e);
                        }
                        break;
                    case (AudioManager.AUDIOFOCUS_GAIN):
                        onPrepared(mediaPlayer);
                        break;
                    case (AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK):
                        mediaPlayer.setVolume(0.1f, 0.1f);
                        break;
                }
            }
        };

        AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        assert audioManager != null;
        int mediaresult = audioManager.requestAudioFocus(onAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        if (mediaresult == AudioManager.AUDIOFOCUS_REQUEST_GRANTED){
            Log.e(TAG, "onCreate: GRANTED");
        }

        notificationManagerCompat = NotificationManagerCompat.from(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        initIfPhoneCall();
        String nama, judul_kajian, pemateri;
        if (intent!=null){
            Log.e(TAG, "onStartCommand: Streaming Service " + Objects.requireNonNull(intent.getExtras()).getString("url"));
            nama = Objects.requireNonNull(intent.getExtras()).getString("name");
            judul_kajian = intent.getExtras().getString("judul_kajian");
            pemateri = intent.getExtras().getString("pemateri");
            showNotification(nama, judul_kajian, pemateri);
            mediaPlayer.reset();
            if (!mediaPlayer.isPlaying()){
                try {
                    String urlStreaming = intent.getExtras().getString("url");
                    mediaPlayer.setDataSource(urlStreaming);
                    mediaPlayer.prepareAsync();
                } catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
        return START_STICKY;
    }

    private void showNotification(String nama, String judul_kajian, String pemateri){
        Intent intentNotification = new Intent(this, MainActivity.class);
        intentNotification.putExtra("streamingRadio", "streamingRadio");
        intentNotification.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntentOpenApp = PendingIntent.getActivity(this, 0, intentNotification, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intentExit = new Intent(this, MyReceiver.class);
        intentExit.setAction("exit");
        PendingIntent pendingIntentExit = PendingIntent.getBroadcast(this, 12345, intentExit, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_2);
        builder.setSmallIcon(R.drawable.logoabuaziz)
                .setTicker("Mendengarkan " + nama)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentTitle(judul_kajian)
                .setContentText(pemateri)
                .setContentIntent(pendingIntentOpenApp)
                .addAction(android.R.drawable.ic_delete, "HENTIKAN", pendingIntentExit);

        startForeground(111, builder.build());
    }

    private void initIfPhoneCall(){
        Log.e(TAG, "initIfPhoneCall: ");
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            PhoneStateListener phoneStateListener = new PhoneStateListener(){
                @Override
                public void onCallStateChanged(int state, String phoneNumber) {
                    switch (state){
                        case TelephonyManager.CALL_STATE_OFFHOOK:
                        case TelephonyManager.CALL_STATE_RINGING:
                            Log.e(TAG, "onCallStateChanged: ");
                            if (mediaPlayer != null){
                                pauseMedia();
                                isPausedCall = true;
                            }
                            break;
                        case TelephonyManager.CALL_STATE_IDLE:
                            if (mediaPlayer != null){
                                if (isPausedCall){
                                    isPausedCall = false;
//                                    playMedia();
                                    onPrepared(mediaPlayer);
                                }
                            }
                            break;
                    }
                }
            };
            assert telephonyManager != null;
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        } else {
            if (mediaPlayer != null){
//                playMedia();
                onPrepared(mediaPlayer);
            }
        }
    }

    private void playMedia(){
        Log.e(TAG, "playMedia: ");
        if (!mediaPlayer.isPlaying()){
            mediaPlayer.setVolume(1.0f,1.0f);
            mediaPlayer.start();
            Intent intent = new Intent("mediaplayed");
            sendBroadcast(intent);
        }
    }

    private void stopMedia(){
        Log.e(TAG, "stopMedia: ");
        if (mediaPlayer != null){
            Log.e(TAG, "stopMedia: NOT NULL");
            notificationManagerCompat.cancel(1);
            if (mediaPlayer.isPlaying()){
                mediaPlayer.stop();
                Log.e(TAG, "stopMedia: STOP");
                broadcastStopMedia();
            }
            mediaPlayer.reset();
        } else {
            Log.e(TAG, "stopMedia: ELSE");
        }
        stopForeground(true);
        stopSelf();
    }

    private void pauseMedia(){
        Log.e(TAG, "pauseMedia: ");
        if (mediaPlayer.isPlaying()){
            try{
                mediaPlayer.pause();
            } catch (IllegalStateException e){
                Log.e(TAG, "pauseMedia: "+ e);
            }
            sendBroadcast(new Intent("pausePlayer"));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        broadcastStopMedia();
        unregisterReceiver(broadcastReceiver);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.e(TAG, "onBind: StreamingService " + intent);
        sendBroadcast(new Intent("streamingError"));
        return null;
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        Log.e(TAG, "onBufferingUpdate: StreamingService" + mp + " percent: " + percent);
        Intent intent = new Intent("lemot");
        intent.putExtra("lemot", "703");
        sendBroadcast(intent);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.e(TAG, "onCompletion: StreamingService" + mp);
        stopMedia();
        stopSelf();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(TAG, "onError: WHAT: " + what + "EXTRA: " + extra);
        Intent intent = new Intent("streamingError");
        sendBroadcast(intent);
        return false;
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        Log.e(TAG, "onInfo: WHAT: " + what + "EXTRA: " + extra);
        switch (what) {
            case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                Intent intent = new Intent("lemot");
                intent.putExtra("lemot", String.valueOf(what));
                sendBroadcast(intent);
                break;
            case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                Intent intent2 = new Intent("tidaklemot");
                intent2.putExtra("tidaklemot", String.valueOf(what));
                sendBroadcast(intent2);
                break;
        }
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.e(TAG, "onPrepared: " + mp);
        playMedia();
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        Log.e(TAG, "onSeekComplete: StreamingService" + mp);
    }

    private void broadcastStopMedia(){
        Intent intent = new Intent("mediastoped");
        sendBroadcast(intent);
    }
}
