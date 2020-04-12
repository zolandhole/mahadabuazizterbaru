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
import android.os.Handler;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.mahad.abuaziz.MainActivity;
import com.mahad.abuaziz.PlayerActivity;
import com.mahad.abuaziz.R;
import com.mahad.abuaziz.utils.MyReceiver;

import java.io.IOException;
import java.util.Objects;

import static com.mahad.abuaziz.utils.App.CHANNEL_2;

public class RekamanService extends Service implements
        MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnInfoListener, MediaPlayer.OnBufferingUpdateListener
{

    private MediaPlayer mediaPlayer = new MediaPlayer();
    private boolean isPausedCall = false;
    private BroadcastReceiver broadcastReceiver;
    private static final String TAG = "RekamanService";
    private NotificationManagerCompat notificationManagerCompat;
    private Handler handler;
    private Runnable runnable;

    @Override
    public void onCreate() {
        Log.e(TAG, "onCreate: Called");
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                assert action != null;
                switch (action){
                    case "stoprekaman":
                        pauseMedia();
                        break;
                    case "startrekaman":
                        onPrepared(mediaPlayer);
                        break;
                    case "exitrekaman":
                        stopMedia();
                        break;
                    case "userSkip":
                        int progress = intent.getIntExtra("progress", 0);
                        updateProgress(progress);
                        break;
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction("stoprekaman");
        filter.addAction("startrekaman");
        filter.addAction("exitrekaman");
        filter.addAction("userSkip");
        registerReceiver(broadcastReceiver, filter);

        handler = new Handler();

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

    private void updateProgress(int progress) {
        handler.removeCallbacks(runnable);
        mediaPlayer.seekTo(progress);
        updateSeekBar();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        initIfPhoneCall();
        String nama, judul_kajian, pemateri;
        if (intent!=null){
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

        Intent intentNotification = new Intent(this, PlayerActivity.class);
        PendingIntent pendingIntentOpenApp = PendingIntent.getActivity(this, 0, intentNotification, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intentExit = new Intent(this, MyReceiver.class);
        intentExit.setAction("exitrekaman");
        PendingIntent pendingIntentExit = PendingIntent.getBroadcast(this, 12345, intentExit, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intentPause = new Intent(this, MyReceiver.class);
        intentPause.setAction("stoprekaman");
        PendingIntent pendingIntentPause = PendingIntent.getBroadcast(this, 12345, intentPause, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intentPlay = new Intent(this, MyReceiver.class);
        intentPlay.setAction("startrekaman");
        PendingIntent pendingIntentPlay = PendingIntent.getBroadcast(this, 12345, intentPlay, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_2);
        builder.setSmallIcon(R.drawable.logoabuaziz)
                .setTicker("Mendengarkan " + nama)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentTitle(judul_kajian)
                .setContentText(pemateri)
                .setContentIntent(pendingIntentOpenApp)
                .addAction(android.R.drawable.ic_delete, "HENTIKAN", pendingIntentExit)
                .addAction(android.R.drawable.ic_media_pause, "JEDA", pendingIntentPause)
                .addAction(android.R.drawable.ic_media_play, "PUTAR", pendingIntentPlay);

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
            Log.e(TAG, "playMedia: CURRENT POSITION" + mediaPlayer.getCurrentPosition());
            updateSeekBar();
            Intent intent = new Intent("mediaplayedrekaman");
            sendBroadcast(intent);
        }
    }

    private void updateSeekBar() {
        int test = mediaPlayer.getCurrentPosition();
        Intent intent = new Intent("UPDATESEEK");
        intent.putExtra("valueseekbar", test);
        sendBroadcast(intent);
        runnable = new Runnable() {
            @Override
            public void run() {
                updateSeekBar();
            }
        };
        handler.postDelayed(runnable, 1000);
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
                handler.removeCallbacks(runnable);
            } catch (IllegalStateException e){
                Log.e(TAG, "pauseMedia: "+ e);
            }
            sendBroadcast(new Intent("pausePlayerrekaman"));
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
        sendBroadcast(new Intent("streamingErrorrekaman"));
        return null;
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        Intent intent = new Intent("buffering");
        intent.putExtra("percent", percent);
        sendBroadcast(intent);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stopMedia();
                stopSelf();
            }
        }, 1000);
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(TAG, "onError: WHAT: " + what + "EXTRA: " + extra);
        Intent intent = new Intent("streamingErrorrekaman");
        sendBroadcast(intent);
        return false;
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        Log.e(TAG, "onInfo: WHAT: " + what + "EXTRA: " + extra);
        sendBroadcast(new Intent("update"));
        switch (what) {
            case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                Intent intent = new Intent("lemotrekaman");
                intent.putExtra("lemot", String.valueOf(what));
                sendBroadcast(intent);
                break;
            case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                Intent intent2 = new Intent("tidaklemotrekaman");
                intent2.putExtra("tidaklemot", String.valueOf(what));
                sendBroadcast(intent2);
                break;
        }
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Intent intent = new Intent("duration");
        intent.putExtra("duration", mediaPlayer.getDuration());
        sendBroadcast(intent);
        playMedia();
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        Log.e(TAG, "onSeekComplete: StreamingService" + mp.getCurrentPosition());
    }

    private void broadcastStopMedia(){
        handler.removeCallbacks(runnable);
        Intent intent = new Intent("mediastopedrekaman");
        sendBroadcast(intent);
    }
}
