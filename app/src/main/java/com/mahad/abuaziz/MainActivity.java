package com.mahad.abuaziz;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessaging;
import com.mahad.abuaziz.adapters.AdapterChat;
import com.mahad.abuaziz.models.ModelChat;
import com.mahad.abuaziz.models.ModelHeader;
import com.mahad.abuaziz.models.ModelIklan;
import com.mahad.abuaziz.models.RecyclerViewItem;
import com.mahad.abuaziz.services.RekamanService;
import com.mahad.abuaziz.services.StreamingService;
import com.mahad.abuaziz.utils.DBHandler;
import com.mahad.abuaziz.utils.HandlerServer;
import com.mahad.abuaziz.utils.ResponServer;
import com.mahad.abuaziz.utils.ResponShoutcast;
import com.mahad.abuaziz.utils.ServiceAddress;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String streamingURL = "http://122.248.39.157:8050/";
    private static final String gagalambildata = "Gagal mengambil data ke server, hubungi Administrator, atau klik play tombol diatas untuk menghubungkan ulang";
    private Boolean doubleBackToExitPressedOnce = false;
    private ProgressBar progressBarMain, progressBarPlayer;
    private ImageButton btn_play, btn_stop;
    private TextView tv_error, juduliklan, descriptioniklan, tv_titlekajian, tv_pemateri;
    private LinearLayout linear_kesalahan, kirim_pesan;
    private ScrollView view_offline;
    private RelativeLayout view_online, rl_newmessage;
    private ImageView photoiklan;
    private String ID_LOGIN, token_fcm, JUDUL_KAJIAN, PEMATERI;
    private DBHandler dbHandler;
    private LinearLayoutManager linearLayoutManager;
    private RecyclerView recyclerView;
    private AdapterChat adapterChat;
    private List<RecyclerViewItem> recyclerViewItems;
    private Button btn_send;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressBarMain = findViewById(R.id.main_loading); progressBarPlayer = findViewById(R.id.progressBarPlayer);
        btn_play = findViewById(R.id.btn_player); btn_stop = findViewById(R.id.btn_stop); tv_error = findViewById(R.id.tv_error);
        linear_kesalahan = findViewById(R.id.linear_kesalahan); view_offline = findViewById(R.id.view_offline); view_online = findViewById(R.id.view_sukses);
        photoiklan = findViewById(R.id.photoiklan); juduliklan = findViewById(R.id.juduliklan); descriptioniklan = findViewById(R.id.descriptioniklan);
        Button btn_listkajian = findViewById(R.id.btn_listkajian); kirim_pesan = findViewById(R.id.kirim_pesan); rl_newmessage = findViewById(R.id.rl_newmessage);
        tv_titlekajian = findViewById(R.id.tv_titlekajian); tv_pemateri = findViewById(R.id.tv_pemateri); recyclerView = findViewById(R.id.recycler_chat);
        btn_send = findViewById(R.id.streaming_sendpesan);

        dbHandler = new DBHandler(this);

        btn_play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new ServiceStreaming().execute();
            }
        });
        btn_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btn_stop.setVisibility(View.GONE);
                btn_play.setVisibility(View.GONE);
                progressBarPlayer.setVisibility(View.VISIBLE);
                sendBroadcast(new Intent("exit"));
            }
        });
        btn_listkajian.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { startActivity(new Intent(MainActivity.this, RekamanKajianActivity.class)); }
        });
        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                kirimPesan();
            }
        });
        rl_newmessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recyclerView.smoothScrollToPosition(Objects.requireNonNull(recyclerView.getAdapter()).getItemCount() - 1);
                rl_newmessage.setVisibility(View.GONE);
            }
        });
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int scrollposition = linearLayoutManager.findLastVisibleItemPosition();
                if (scrollposition == Objects.requireNonNull(recyclerView.getAdapter()).getItemCount() - 2){
                    rl_newmessage.setVisibility(View.GONE);
                }
            }
        });

        daftarkanBroadcast();
        generateTokenFCM();
        getIdLogin();
    }

    @Override
    protected void onStart() {
        super.onStart();
        new ServiceStreaming().execute();
    }

    private void getIdLogin() {
        ID_LOGIN = checkUserOnDB();
    }

    private String checkUserOnDB(){
        ArrayList<HashMap<String, String>> userDB = dbHandler.getUser(1);
        for (Map<String, String> map : userDB){
            ID_LOGIN = map.get("id_login");
        }
        return ID_LOGIN;
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            finish();
            sendBroadcast(new Intent("exitrekaman"));
            sendBroadcast(new Intent("exit"));
            return;
        }
        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Tekan lagi untuk keluar aplikasi", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce=false;
            }
        }, 2000);
    }

    private void daftarkanBroadcast() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("mediaplayed");
        filter.addAction("mediastoped");
        filter.addAction("lemot");
        filter.addAction("tidaklemot");
        filter.addAction("streamingError");
        filter.addAction("pausePlayer");
        filter.addAction("errorsenddata");
        filter.addAction("JUDULKAJIAN");
        filter.addAction("PESANUSER");
        filter.addAction("UPDATEIKLAN");
        registerReceiver(broadcastReceiver, filter);
    }

    private void generateTokenFCM() {
        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(new OnSuccessListener<InstanceIdResult>() {
            @Override
            public void onSuccess(InstanceIdResult instanceIdResult) {
                token_fcm = instanceIdResult.getToken();
                Log.e(TAG, "onSuccess: " + token_fcm);
            }
        });
        FirebaseMessaging.getInstance().subscribeToTopic("JUDULKAJIAN");
        FirebaseMessaging.getInstance().subscribeToTopic("PESANUSER");
        FirebaseMessaging.getInstance().subscribeToTopic("UPDATEIKLAN");
    }

    private Boolean apakahKajianSedangDiputar() {
        ActivityManager manager = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
        assert manager != null;
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (StreamingService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private Boolean apakahRekamanSedangDiputar() {
        ActivityManager manager = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
        assert manager != null;
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (RekamanService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @SuppressLint("StaticFieldLeak")
    private class ServiceStreaming extends AsyncTask<Void, Void, String> {

        @Override
        protected void onPreExecute() {
            Log.e(TAG, "onPreExecute: " + streamingURL);
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Void... voids) {
            HandlerServer handlerServer = new HandlerServer(MainActivity.this, streamingURL+"statistics?json=1");
            handlerServer.getStatusServerShoutcast(new ResponShoutcast() {
                @Override
                public void result(JSONObject jsonObject) {
                    updateUIKoneksiKeServerOkLoading();
                    try {
                        String activestreams = jsonObject.getString("activestreams");
                        Log.e(TAG, "result activestreams: "+ activestreams);
                        if (!activestreams.equals("1")){
                            infokanKeUser("Saat ini tidak ada Kajian Online Streaming");
                            layananOffline();
                        } else {
                            Log.e(TAG, "result: Server Aktif lanjutkan ke play Streaming");
                            layananOnline();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void failed(String error) {
                    Log.e(TAG, "failed: Get status Server Streaming = " + error);
                    String pesan = "Coba hubungi kembali dengan menekan tombol play di atas";
                    if (error.contains("com.android.volley.AuthFailureError")){
                        pesan = "Gagal menghubungi Server, hubungi Administrator";
                    } else if (error.contains("com.android.volley.NoConnectionError")){
                        pesan = "Gagal menghubungi Server";
                    }
                    updateUIAdaKesalahan(pesan);
                }
            });
            return null;
        }
    }

    private void infokanKeUser(String message){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void updateUIAdaKesalahan(String pesan) {
        tv_error.setText(pesan);
        linear_kesalahan.setVisibility(View.VISIBLE);
        progressBarMain.setVisibility(View.GONE);
        btn_play.setVisibility(View.VISIBLE);
        btn_stop.setVisibility(View.GONE);
        progressBarPlayer.setVisibility(View.GONE);
        view_online.setVisibility(View.GONE);
        view_offline.setVisibility(View.GONE);
    }

    private void updateUIKoneksiKeServerOkLoading(){
        progressBarPlayer.setVisibility(View.VISIBLE);
        linear_kesalahan.setVisibility(View.GONE);
        progressBarMain.setVisibility(View.VISIBLE);
        btn_play.setVisibility(View.GONE);
        btn_stop.setVisibility(View.GONE);
        view_online.setVisibility(View.GONE);
        view_offline.setVisibility(View.GONE);
    }

    private void updateUIViewOffline(){
        progressBarPlayer.setVisibility(View.GONE);
        linear_kesalahan.setVisibility(View.GONE);
        progressBarMain.setVisibility(View.GONE);
        btn_play.setVisibility(View.VISIBLE);
        btn_stop.setVisibility(View.GONE);
        view_online.setVisibility(View.GONE);
        view_offline.setVisibility(View.VISIBLE);
    }

    private void updateUIViewOnline(){
        linear_kesalahan.setVisibility(View.GONE);
        progressBarMain.setVisibility(View.GONE);
        view_online.setVisibility(View.VISIBLE);
        view_offline.setVisibility(View.GONE);
    }

    private void layananOffline() {
        Log.e(TAG, "layananOffline: ");
        mengambilDataIklanOffline();
    }

    private void mengambilDataIklanOffline() {
        Log.e(TAG, "mengambilDataIklanOffline: dengan ID Login = " + ID_LOGIN);
        List<String> list = new ArrayList<>();
        list.add(ID_LOGIN);
        HandlerServer handlerServer = new HandlerServer(this, ServiceAddress.GETDATAIKLAN);
        synchronized (this){
            handlerServer.sendDataToServer(new ResponServer() {
                @Override
                public void gagal(String result) {
                    updateUIAdaKesalahan("Gagal mengambil Data Offline");
                }

                @Override
                public void berhasil(JSONArray jsonArray) {
                    for (int i = 0; i < jsonArray.length(); i++) {
                        try {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            Glide.with(MainActivity.this).load(jsonObject.getString("photoiklan")).placeholder(R.drawable.button_abu).into(photoiklan);
                            juduliklan.setText(jsonObject.getString("juduliklan"));
                            descriptioniklan.setText(jsonObject.getString("deskripsiiklan"));
                            updateUIViewOffline();
                        } catch (JSONException e) {
                            Log.e(TAG, "berhasil: mengambilDataIklanOffline Exception" + e);
                            updateUIAdaKesalahan("Gagal mengambil Data Offline");
                        }
                    }

                }
            }, list);
        }
    }

    private void layananOnline() {
        if (apakahRekamanSedangDiputar()){
            sendBroadcast(new Intent("exitrekaman"));
        }

        if (!apakahKajianSedangDiputar()){
            progressBarPlayer.setVisibility(View.VISIBLE);
            btn_play.setVisibility(View.GONE);
            btn_stop.setVisibility(View.GONE);
            jalankanStreaming();
        } else {
            progressBarPlayer.setVisibility(View.GONE);
            btn_play.setVisibility(View.GONE);
            btn_stop.setVisibility(View.VISIBLE);
        }

        ID_LOGIN = checkUserOnDB();
        if (ID_LOGIN == null){
            kirim_pesan.setVisibility(View.GONE);
        } else {
            kirim_pesan.setVisibility(View.VISIBLE);
        }
        getJudulKajian();
    }

    private void jalankanStreaming() {
        Bundle bundle = new Bundle();
        bundle.putString("url", streamingURL);
        bundle.putString("name", "MA'HAD ABU AZIZ");
        bundle.putString("judul_kajian", JUDUL_KAJIAN);
        bundle.putString("pemateri", PEMATERI);
        Intent intent = new Intent(MainActivity.this, StreamingService.class);
        intent.putExtras(bundle);
        startService(intent);
    }

    private void getJudulKajian() {
        List<String> list = new ArrayList<>();
        list.add(""); list.add("SURAMPAK");
        HandlerServer handlerServer = new HandlerServer(MainActivity.this, ServiceAddress.INFOKAJIAN);
        synchronized (this) {
            handlerServer.sendDataToServer(new ResponServer() {
                @Override
                public void gagal(String result) {
                    Log.e(TAG, "gagal: GETJUDULKAJIAN: " + result);
                    infokanKeUser(gagalambildata);
                }

                @Override
                public void berhasil(JSONArray jsonArray) {
                    JSONObject object;
                    try {
                        for (int i = 0; i < jsonArray.length(); i++) {
                            object = jsonArray.getJSONObject(i);
                            JUDUL_KAJIAN = object.getString("judul_kajian");
                            PEMATERI = object.getString("pemateri");
                            tv_titlekajian.setText(JUDUL_KAJIAN);
                            tv_pemateri.setText(PEMATERI);
                        }
                    } catch (JSONException e) {
                        infokanKeUser(gagalambildata);
                        e.printStackTrace();
                    }
                    getDataChatting();
                }
            }, list);
        }
    }

    private void getDataChatting() {
        List<String> list = new ArrayList<>();
        list.add(ID_LOGIN);
        HandlerServer handlerServer = new HandlerServer(this, ServiceAddress.GETDATACHAT);
        synchronized (this){
            handlerServer.sendDataToServer(new ResponServer() {
                @Override
                public void gagal(String result) {
                    infokanKeUser(gagalambildata);
                }

                @Override
                public void berhasil(JSONArray jsonArray) {
                    ambilDataHeader(jsonArray);
                }
            }, list);
        }
    }

    private void ambilDataHeader(final JSONArray dataChatting) {
        List<String> list = new ArrayList<>();
        list.add(ID_LOGIN);
        HandlerServer handlerServer = new HandlerServer(this, ServiceAddress.GETPROFILE);
        synchronized (this){
            handlerServer.sendDataToServer(new ResponServer() {
                @Override
                public void gagal(String result) {
                    Log.e(TAG, "gagal getDataIklan: " + result);
                    dbHandler.deleteDB();
                    if (result.contains("User tidak terdaftar")) {
                        ModelHeader dataHeader = new ModelHeader("", "", "");
                        getDataIklan(dataChatting, dataHeader);
                    }
                }

                @Override
                public void berhasil(JSONArray jsonArray) {
                    for (int i = 0; i < jsonArray.length(); i++) {
                        try {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            ModelHeader dataHeader = new ModelHeader(
                                    jsonObject.getString("nama"),
                                    jsonObject.getString("email"),
                                    jsonObject.getString("photo")
                            );
                            getDataIklan(dataChatting, dataHeader);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                }
            }, list);
        }
    }

    private void getDataIklan(final JSONArray dataChatting, final ModelHeader dataHeader) {
        List<String> list = new ArrayList<>();
        list.add("0");
        HandlerServer handlerServer = new HandlerServer(this, ServiceAddress.GETDATAIKLAN);
        synchronized (this){
            handlerServer.sendDataToServer(new ResponServer() {
                @Override
                public void gagal(String result) {
                    Log.e(TAG, "berhasil ambil data iklan");
                    infokanKeUser(gagalambildata);
                }

                @Override
                public void berhasil(JSONArray jsonArray) {
                    for (int i = 0; i < jsonArray.length(); i++) {
                        try {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            ModelIklan dataIklan = new ModelIklan(
                                    jsonObject.getString("photoiklan"),
                                    jsonObject.getString("juduliklan"),
                                    jsonObject.getString("deskripsiiklan")
                            );
                            Log.e(TAG, "berhasil ambil data iklan: " + jsonArray);
                            tampilkanKeRecyclerView(dataChatting, dataHeader, dataIklan);
                        } catch (JSONException e) {
                            infokanKeUser(gagalambildata);
                            e.printStackTrace();
                        }
                    }
                }
            }, list);
        }
    }

    private List<RecyclerViewItem> membuatListData(JSONArray dataChatting, ModelHeader dataHeader, ModelIklan dataIklan) {
        recyclerViewItems = new ArrayList<>();
        recyclerViewItems.add(dataHeader);
        recyclerViewItems.add(dataIklan);

        if (dataChatting != null){
            for (int i = 0; i < dataChatting.length(); i++) {
                try {
                    JSONObject jsonObject = dataChatting.getJSONObject(i);
                    ModelChat modelChat = new ModelChat(
                            jsonObject.getString("id"),
                            jsonObject.getString("pesan"),
                            jsonObject.getString("jam"),
                            jsonObject.getString("photo"),
                            jsonObject.getString("pengirim")
                    );
                    recyclerViewItems.add(modelChat);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        return recyclerViewItems;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void tampilkanKeRecyclerView(JSONArray dataChatting, ModelHeader dataHeader, ModelIklan dataIklan) {
        linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        adapterChat = new AdapterChat(membuatListData(dataChatting, dataHeader, dataIklan), this, dbHandler);
        recyclerView.setAdapter(adapterChat);
        recyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (inputMethodManager != null){
                    inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                }
                return false;
            }
        });
        if (!dataHeader.getNAMAPROFILE().equals("")){
            recyclerView.smoothScrollToPosition(Objects.requireNonNull(recyclerView.getAdapter()).getItemCount() -1);
        }
        recyclerView.setVisibility(View.VISIBLE);
        updateUIViewOnline();
    }

    private void checkStatusServer() {
        Log.e(TAG, "checkStatusServer: YOU ARE HERE");
        HandlerServer handlerServer = new HandlerServer(MainActivity.this, streamingURL+"statistics?json=1");
        handlerServer.getStatusServerShoutcast(new ResponShoutcast() {
            @Override
            public void result(JSONObject jsonObject) {
                try {
                    String activestreams = jsonObject.getString("activestreams");
                    Log.e(TAG, "result: STATUS STRAMING" + activestreams);
                    if (!activestreams.equals("1")){
                        Toast.makeText(MainActivity.this, "Kajian telah berakhir", Toast.LENGTH_LONG).show();
                        view_online.setVisibility(View.GONE);
                        view_offline.setVisibility(View.VISIBLE);
                        btn_stop.setVisibility(View.GONE);
                        progressBarPlayer.setVisibility(View.GONE);
                        btn_play.setVisibility(View.VISIBLE);
                        sendBroadcast(new Intent("exit"));
                        clear();
                    }
                } catch (JSONException e) {
                    updateUIAdaKesalahan(String.valueOf(e));
                    e.printStackTrace();
                }
            }

            @Override
            public void failed(String error) {
                Log.e(TAG, "failed: Get status Server Streaming = " + error);
                String pesan = "Coba hubungi kembali dengan menekan tombol play di atas";
                if (error.contains("com.android.volley.AuthFailureError")){
                    pesan = "Gagal menghubungi Server, hubungi Administrator";
                } else if (error.contains("com.android.volley.NoConnectionError")){
                    pesan = "Gagal menghubungi Server";
                }
                updateUIAdaKesalahan(pesan);
            }
        });
    }

    public void clear() {
        int size = recyclerViewItems.size();
        recyclerViewItems.clear();
        adapterChat.notifyItemRangeRemoved(0, size);
    }

    private void kirimPesan() {
        final EditText editTextPesan = findViewById(R.id.streaming_edittext);
        String pesan = editTextPesan.getText().toString().trim();
        final ProgressBar progressBar_send = findViewById(R.id.progress_bar_send);
        if (!pesan.equals("")){
            btn_send.setVisibility(View.GONE);
            progressBar_send.setVisibility(View.VISIBLE);
            editTextPesan.setEnabled(false);
            List<String> list = new ArrayList<>();
            list.add(ID_LOGIN);
            list.add(pesan);
            HandlerServer handlerServer = new HandlerServer(MainActivity.this, ServiceAddress.TAMBAHCHAT);
            synchronized (this){
                handlerServer.sendDataToServer(new ResponServer() {
                    @Override
                    public void gagal(String result) {
                        Log.e(TAG, "gagal: " + result);
                        infokanKeUser("Gagal Mengirim pesan, silahkan coba lagi");
                        btn_send.setVisibility(View.VISIBLE);
                        progressBar_send.setVisibility(View.GONE);
                        editTextPesan.setEnabled(true);
                    }

                    @Override
                    public void berhasil(JSONArray jsonArray) {
                        Log.e(TAG, "berhasil: " + jsonArray);
                        editTextPesan.setText("");
                        btn_send.setVisibility(View.VISIBLE);
                        progressBar_send.setVisibility(View.GONE);
                        editTextPesan.setEnabled(true);
                        recyclerView.smoothScrollToPosition(Objects.requireNonNull(recyclerView.getAdapter()).getItemCount() - 1);
                    }
                }, list);
            }
        } else {
            Toast.makeText(this, "Isi pesan di kolom", Toast.LENGTH_SHORT).show();
        }
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String param = intent.getAction();
            assert param != null;
            switch (param){
                case "mediaplayed":
                    btn_play.setVisibility(View.GONE);
                    progressBarPlayer.setVisibility(View.GONE);
                    btn_stop.setVisibility(View.VISIBLE);
                    Log.e(TAG, "onReceive: mediaPlayed");
                    break;
                case "mediastoped":
                    btn_stop.setVisibility(View.GONE);
                    progressBarPlayer.setVisibility(View.GONE);
                    btn_play.setVisibility(View.VISIBLE);
                    Log.e(TAG, "onReceive: meidaStoped");
                    break;
                case "lemot":
                    String bufferCode = intent.getStringExtra("lemot");
                    assert bufferCode != null;
                    if (bufferCode.equals("703")){
                        Log.e(TAG, "onReceive: Sedang buffering 703");
                    }
                    if (bufferCode.equals("702")){
                        Log.e(TAG, "onReceive: Buffer Completed 702");
                    }
                    if (bufferCode.equals("701")){
                        Log.e(TAG, "onReceive: Buffer Completed 701");
                    }
                    checkStatusServer();
                    break;
                case "streamingError":
                    Log.e(TAG, "onReceive: ERROR");
                    break;
                case "pausePlayer":
                    Log.e(TAG, "onReceive: Pause Media");
                    break;
                case "errorsenddata":
                    Log.e(TAG, "onReceive: Error SendData Volley");
                    break;
                case "JUDULKAJIAN":
                    tv_titlekajian.setText(intent.getStringExtra("judulKajian"));
                    tv_pemateri.setText(intent.getStringExtra("pemateri"));
                    sendBroadcast(new Intent("exitrekaman"));
                    break;
                case "PESANUSER":
                    Log.e(TAG, "onReceive: " + intent.getStringExtra("pesan"));
                    ModelChat item = new ModelChat(
                            intent.getStringExtra("id"),
                            intent.getStringExtra("pesan"),
                            intent.getStringExtra("jam"),
                            intent.getStringExtra("photo"),
                            intent.getStringExtra("pengirim")
                    );
                    recyclerViewItems.add(item);
                    adapterChat.notifyDataSetChanged();

                    int scrollposition = linearLayoutManager.findLastVisibleItemPosition();
                    if (scrollposition != Objects.requireNonNull(recyclerView.getAdapter()).getItemCount() - 2){
                        rl_newmessage.setVisibility(View.VISIBLE);
                    } else {
                        rl_newmessage.setVisibility(View.GONE);
                        recyclerView.smoothScrollToPosition(Objects.requireNonNull(recyclerView.getAdapter()).getItemCount() - 1);
                    }
                    break;
                case "UPDATEIKLAN":
                    Log.e(TAG, "onReceive: " + intent.getStringExtra("photoiklan"));
                    ModelIklan modelIklan = new ModelIklan(
                            intent.getStringExtra("photoiklan"),
                            intent.getStringExtra("juduliklan"),
                            intent.getStringExtra("deskripsiiklan")
                    );
                    recyclerViewItems.set(1,modelIklan);
                    adapterChat.notifyItemChanged(1);
                    Glide.with(MainActivity.this).load(intent.getStringExtra("photoiklan")).placeholder(R.drawable.button_abu).into(photoiklan);
                    juduliklan.setText(intent.getStringExtra("juduliklan"));
                    descriptioniklan.setText(intent.getStringExtra("deskripsiiklan"));
                    break;
            }
        }
    };
}
