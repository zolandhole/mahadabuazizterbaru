package com.mahad.abuaziz.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.mahad.abuaziz.R;
import com.mahad.abuaziz.utils.NotificationReceiver;

import org.json.JSONException;
import org.json.JSONObject;

import static com.mahad.abuaziz.utils.App.CHANNEL_1;

public class MessagingService extends FirebaseMessagingService {

    private static final String TAG = "MessagingService";
    private NotificationManagerCompat notificationManagerCompat;

    @Override
    public void onCreate() {
        notificationManagerCompat = NotificationManagerCompat.from(this);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.e(TAG, "onMessageReceived: " + remoteMessage.getData().toString());
        if (remoteMessage.getData().size() > 0){
            JSONObject jsonObject = new JSONObject(remoteMessage.getData());
            olahDataPushNotification(jsonObject);
        }
    }

    private void olahDataPushNotification(JSONObject jsonObject) {
        try {
            String topic = jsonObject.getString("topic");
            switch (topic) {
                case "JUDULKAJIAN":
                    pengecekanNotifikasi(jsonObject);
                    break;
                case "PESANUSER":
                    Log.e(TAG, "olahDataPushNotification: " + jsonObject);
                    Intent intent = new Intent("PESANUSER");
                    intent.putExtra("id", jsonObject.getString("id"));
                    intent.putExtra("jam", jsonObject.getString("jam"));
                    intent.putExtra("pesan", jsonObject.getString("pesan"));
                    intent.putExtra("photo", jsonObject.getString("photo"));
                    intent.putExtra("pengirim", jsonObject.getString("pengirim"));
                    sendBroadcast(intent);
                    break;
                case "UPDATEIKLAN":
                    Log.e(TAG, "olahDataPushNotification: DATA IKLAN" + jsonObject.getString("photoiklan"));
                    Intent intenIklan = new Intent("UPDATEIKLAN");
                    intenIklan.putExtra("photoiklan", jsonObject.getString("photoiklan"));
                    intenIklan.putExtra("juduliklan", jsonObject.getString("juduliklan"));
                    intenIklan.putExtra("deskripsiiklan", jsonObject.getString("deskripsiiklan"));
                    sendBroadcast(intenIklan);
                    break;
            }
        } catch (JSONException e) {
            Log.e(TAG, "showNotification: CATCH " + e);
            e.printStackTrace();
        }

    }

    private void pengecekanNotifikasi(JSONObject jsonObject) {
        try {
            String judulKajian = jsonObject.getString("judul_kajian");
            String pemateri = jsonObject.getString("pemateri");
            Intent intent = new Intent("JUDULKAJIAN");
            intent.putExtra("judulKajian", judulKajian);
            intent.putExtra("pemateri", pemateri);
            sendBroadcast(intent);
            tampilkanNotifikasi(judulKajian, pemateri);

        } catch (JSONException e) {
            Log.e(TAG, "tampilkanNotifikasi: CATCH " + e);
            e.printStackTrace();
        }
    }

    private void tampilkanNotifikasi(String judulKajian, String pemateri) {
        RemoteViews remoteViewsCollapsed = new RemoteViews(getPackageName(), R.layout.notification_collapsed);
        RemoteViews remoteViewsExpanded = new RemoteViews(getPackageName(), R.layout.notification_expanded);
        Intent intentClick = new Intent(this, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this,0,intentClick, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViewsCollapsed.setTextViewText(R.id.tv_judul_kajian_notif, judulKajian);
        remoteViewsCollapsed.setTextViewText(R.id.tv_pemateri_notif, pemateri);
        remoteViewsCollapsed.setOnClickPendingIntent(R.id.ll_colapsed_layout, pendingIntent);

        remoteViewsExpanded.setTextViewText(R.id.tv_judul_kajian_notif, judulKajian);
        remoteViewsExpanded.setTextViewText(R.id.tv_pemateri_notif, pemateri);
        remoteViewsExpanded.setOnClickPendingIntent(R.id.ll_expanded_layout, pendingIntent);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_1)
                .setSmallIcon(R.drawable.logoabuaziz)
                .setCustomContentView(remoteViewsCollapsed)
                .setCustomBigContentView(remoteViewsExpanded)
                .build();
        notificationManagerCompat.notify(1, notification);
    }

    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
        Log.e(TAG, "onNewToken: " + s);
    }
}
