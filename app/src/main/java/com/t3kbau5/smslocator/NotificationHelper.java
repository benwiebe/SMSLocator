package com.t3kbau5.smslocator;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Random;

/**
 * Created by Ben on 2018-04-16.
 */

public class NotificationHelper {

    private static final String CHANNEL_ID_SMS = "sms";
    private static final int NOTIF_ID_SMS = 8675309;

    private static NotificationChannel createNotifChannel(Context context, String id, String name, int importance, boolean vibration, boolean bypassDnd, boolean showBadge) {
        NotificationChannel channel = null;
        if(Build.VERSION.SDK_INT >= 26) {
            channel = new NotificationChannel(id, name, importance);
            channel.setBypassDnd(bypassDnd);
            channel.enableVibration(vibration);
            channel.setShowBadge(showBadge);
            NotificationManager notifManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notifManager.createNotificationChannel(channel);
        }
        return channel;
    }

    public static void showNotif(Context context, Notification notification, int id) {
        NotificationManager notifManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notifManager.notify(id, notification);
    }

    public static void showNotif(Context context, String title, String message, int smallIcon, int largeIcon, Intent resultIntent, int number, int priority, NotificationChannel channel, int id) {
        Bitmap largeIconB = BitmapFactory.decodeResource(context.getResources(), largeIcon);

        Notification.Builder mBuilder;
        if(Build.VERSION.SDK_INT >= 26)
            mBuilder = new Notification.Builder(context, channel.getId());
        else
            mBuilder = new Notification.Builder(context);

        mBuilder.setSmallIcon(smallIcon)
                .setContentTitle(title)
                .setContentText(message)
                .setLargeIcon(largeIconB)
                .setNumber(number);
        if(Build.VERSION.SDK_INT >= 16)
            mBuilder.setPriority(priority);

        mBuilder.setContentIntent(PendingIntent.getActivity(context, 0, resultIntent, PendingIntent.FLAG_ONE_SHOT));
        if(Build.VERSION.SDK_INT < 16)
            showNotif(context, mBuilder.getNotification(), id);
        else
            showNotif(context, mBuilder.build(), id);
    }

    public static void showMessageNotif(Context context, String destination, String contents){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        int displayNum = prefs.getInt("notifDisplayNum", 0);
        displayNum++;
        prefs.edit().putInt("notifDisplayNum", displayNum).apply();

        Intent intent = new Intent(context, Interactions.class);

        //todo: configure importance
        NotificationChannel channel = createNotifChannel(context, CHANNEL_ID_SMS, Utils.getStr(context, R.string.notif_channel_name_sms), NotificationManager.IMPORTANCE_HIGH, true, false, true);

        showNotif(context,
                    Utils.getStr(context, R.string.notif_sent_title),
                    Utils.getStr(context, R.string.notif_sent_body) + destination,
                    R.drawable.icon_notif, R.drawable.ic_launcher,
                    intent, displayNum,
                    Integer.parseInt(prefs.getString("notif_priority", "0")),
                    channel,
                    NOTIF_ID_SMS);
    }

    public static void cancelNotif(Context context, int id){
        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(id);
    }

    public static void clearMessageNotif(Context context) {
        cancelNotif(context, NOTIF_ID_SMS);
    }

}
