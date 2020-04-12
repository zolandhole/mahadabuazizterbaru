package com.mahad.abuaziz;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatSeekBar;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.mahad.abuaziz.services.RekamanService;
import com.mahad.abuaziz.utils.HandlerServer;
import com.mahad.abuaziz.utils.ResponShoutcast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

public class PlayerActivity extends AppCompatActivity {

    private static final String TAG = "PlayerActivity";
    private String judul, upload_date, link, idSong;
    private Button player_btn_play, player_btn_stop, player_btn_pause, player_btn_resume;
    private TextView player_tanggal, player_judul_kajian, player_judul, tv_total_waktu, tv_current_waktu;
    private ProgressBar progress_buffered;
    private AppCompatSeekBar player_seekbar;
    private RelativeLayout control_player;
    private Boolean doubleBackToExitPressedOnce = false;
    private static final String streamingURL = "http://122.248.39.157:8050/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        Intent intent = getIntent();
        idSong = intent.getStringExtra("id");
        judul = intent.getStringExtra("nama");
        upload_date = intent.getStringExtra("upload_date");
        link = intent.getStringExtra("status");

        player_btn_play = findViewById(R.id.player_btn_play);
        player_btn_stop = findViewById(R.id.player_btn_stop);
        player_btn_pause = findViewById(R.id.player_btn_pause);
        player_btn_resume = findViewById(R.id.player_btn_resume);
        player_tanggal = findViewById(R.id.player_tanggal);
        player_judul_kajian = findViewById(R.id.player_judul_kajian);
        player_judul = findViewById(R.id.player_judul);
        player_seekbar = findViewById(R.id.player_seekbar);
        tv_total_waktu = findViewById(R.id.tv_total_waktu);
        tv_current_waktu = findViewById(R.id.current_waktu);
        progress_buffered = findViewById(R.id.tv_buffered);
        control_player = findViewById(R.id.control_player);

        player_btn_play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playRekaman();
            }
        });
        player_btn_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRekaman();
            }
        });
        player_btn_pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pauseRekaman();
            }
        });
        player_btn_resume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { sendBroadcast(new Intent("startrekaman"));
            }
        });

        daftarkanBroadcast();
        playRekaman();
        Log.e(TAG, "onCreate: " + judul + upload_date + link);
    }

    @Override
    protected void onResume() {
        super.onResume();
        cekStatusServer();
    }

    private void cekStatusServer() {
        HandlerServer handlerServer = new HandlerServer(this, streamingURL+"statistics?json=1");
        handlerServer.getStatusServerShoutcast(new ResponShoutcast() {
            @Override
            public void result(JSONObject jsonObject) {
                try {
                    String activestreams = jsonObject.getString("activestreams");
                    if (activestreams.equals("1")){
                        sendBroadcast(new Intent("exitrekaman"));
                        finish();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void failed(String error) {
                Log.e(TAG, "failed: Get status Server Streaming = " + error);
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            startActivity(new Intent(this, RekamanKajianActivity.class));
            finish();
            sendBroadcast(new Intent("exitrekaman"));
            sendBroadcast(new Intent("exit"));
            return;
        }
        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Tekan sekali lagi untuk kembali, rekaman kajian dihentikan", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce=false;
            }
        }, 2000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
    }


    private void daftarkanBroadcast() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("mediaplayedrekaman");
        filter.addAction("pausePlayerrekaman");
        filter.addAction("mediastopedrekaman");
        filter.addAction("duration");
        filter.addAction("UPDATESEEK");
        filter.addAction("buffering");
        filter.addAction("update");
        filter.addAction("JUDULKAJIAN");
        registerReceiver(broadcastReceiver, filter);
    }

    @SuppressLint("SetTextI18n")
    private void playRekaman() {
        player_tanggal.setText("Dirilis: " + upload_date);
        String tv_judul = judul.replace("_", " ");
        tv_judul = tv_judul.replace("-", " ");
        player_judul_kajian.setText(tv_judul);
        Bundle bundle = new Bundle();
        bundle.putString("idsong", idSong);
        bundle.putString("url", link);
        bundle.putString("name", "MA'HAD ABU AZIZ");
        bundle.putString("judul_kajian", judul);
        bundle.putString("pemateri", "Rekaman Kajian");
        Intent intent = new Intent(PlayerActivity.this, RekamanService.class);
        intent.putExtras(bundle);
        startService(intent);
    }

    private void stopRekaman() {
        Intent intent = new Intent("exitrekaman");
        sendBroadcast(intent);
    }

    private void pauseRekaman(){
        Intent intent = new Intent("stoprekaman");
        sendBroadcast(intent);
    }

    private void played(){
        control_player.setVisibility(View.VISIBLE);
        player_btn_play.setVisibility(View.GONE);
        player_btn_stop.setVisibility(View.VISIBLE);
        player_btn_pause.setVisibility(View.VISIBLE);
        player_btn_resume.setVisibility(View.GONE);
        player_judul.setText(R.string.sedang_diputar);
    }

    private void updateSeekBar(int valueseekbar) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            player_seekbar.setProgress(valueseekbar, true);
        } else {
            player_seekbar.setProgress(valueseekbar);
        }
        @SuppressLint("DefaultLocale") String currentwaktu = String.format(
                "%d:%d",
                TimeUnit.MILLISECONDS.toMinutes(valueseekbar),
                TimeUnit.MILLISECONDS.toSeconds(valueseekbar) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(valueseekbar)));
        tv_current_waktu.setText(currentwaktu);
        player_seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser){
                    Intent intent = new Intent("userSkip");
                    intent.putExtra("progress", progress);
                    sendBroadcast(intent);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @SuppressLint("SetTextI18n")
        @Override
        public void onReceive(Context context, Intent intent) {
            String params = intent.getAction();
            assert params != null;
            switch (params){
                case "mediaplayedrekaman":
                    played();
                    break;
                case "mediastopedrekaman":
                    player_btn_play.setVisibility(View.VISIBLE);
                    player_btn_stop.setVisibility(View.GONE);
                    player_btn_pause.setVisibility(View.GONE);
                    player_btn_resume.setVisibility(View.GONE);
                    player_judul.setText(R.string.dihentikan);
                    player_seekbar.setProgress(0);
                    break;
                case "pausePlayerrekaman":
                    player_btn_play.setVisibility(View.GONE);
                    player_btn_stop.setVisibility(View.VISIBLE);
                    player_btn_pause.setVisibility(View.GONE);
                    player_btn_resume.setVisibility(View.VISIBLE);
                    player_judul.setText(R.string.dijeda);
                    break;
                case "duration":
                    int maxSeek = intent.getIntExtra("duration", 0);
                    player_seekbar.setMax(intent.getIntExtra("duration", 0) - 1000);
                    @SuppressLint("DefaultLocale") String totalwaktu = String.format(
                            "%d:%d",
                            TimeUnit.MILLISECONDS.toMinutes(maxSeek),
                            TimeUnit.MILLISECONDS.toSeconds(maxSeek) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(maxSeek)));
                    tv_total_waktu.setText(totalwaktu);
                    break;
                case "UPDATESEEK":
                    updateSeekBar(intent.getIntExtra("valueseekbar", 0));
                    break;
                case "buffering":
                    if (intent.getIntExtra("percent", 0) >= 1){
                        control_player.setVisibility(View.VISIBLE);
                        progress_buffered.setVisibility(View.GONE);
                    } else {
                        control_player.setVisibility(View.GONE);
                    }
                    break;
                case "update":
                    progress_buffered.setVisibility(View.VISIBLE);
                    break;
                case "JUDULKAJIAN":
                    Log.e(TAG, "onReceive: BROADCAST");
                    sendBroadcast(new Intent("exitrekaman"));
                    finish();
                    break;
            }
        }
    };
}
