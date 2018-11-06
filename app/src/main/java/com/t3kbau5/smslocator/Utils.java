package com.t3kbau5.smslocator;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.telephony.SmsManager;
import android.text.Html;
import android.text.Spanned;
import android.util.Base64;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.Random;

class Utils {
    private static String convertToHex(byte[] data) {
        StringBuilder buf = new StringBuilder();
        for (byte b : data) {
            int halfbyte = (b >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                buf.append((0 <= halfbyte) && (halfbyte <= 9) ? (char) ('0' + halfbyte) : (char) ('a' + (halfbyte - 10)));
                halfbyte = b & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }

    public static String SHA1(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(text.getBytes("iso-8859-1"), 0, text.length());
        byte[] sha1hash = md.digest();
        return convertToHex(sha1hash);
    }

    public static boolean compareToSHA1(String text, String hash) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        String tHash = SHA1(text);
        return tHash.equals(hash);
    }


    private static String formatHtml(String coded) {
        coded = coded.replace("[b]", "<b>");
        coded = coded.replace("[/b]", "</b>");
        coded = coded.replace("[i]", "<i>");
        coded = coded.replace("[/i]", "</i>");
        coded = coded.replace("[u]", "<u>");
        coded = coded.replace("[/u]", "</u>");
        coded = coded.replace("[br]", "<br />");
        coded = coded.replace("\n", "<br />");
        return coded;
    }

    public static Spanned formatAndSpan(String coded) {
        return Html.fromHtml(formatHtml(coded));
    }

    public static void sendSMS(String message, String destination) {
        SmsManager sm = SmsManager.getDefault();
        sm.sendTextMessage(destination, null, message, null, null);
    }

    public static String getProvider(Context context) {
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (Objects.requireNonNull(lm).isProviderEnabled(LocationManager.GPS_PROVIDER))
            return LocationManager.GPS_PROVIDER;
        else if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
            return LocationManager.NETWORK_PROVIDER;
        else return null;
    }

    public static String getProvider(LocationManager lm) {
        if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) return LocationManager.GPS_PROVIDER;
        else if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
            return LocationManager.NETWORK_PROVIDER;
        else return null;
    }

    public static String getStr(Context context, int id) {
        return context.getResources().getString(id);
    }

    public static String randomBase64() {
        byte[] r = new byte[64]; //Means 2048 bit
        Random rand = new Random();
        rand.nextBytes(r);
        return Base64.encodeToString(r, Base64.DEFAULT);
    }

    public static boolean checkAdBlock() {
        BufferedReader in;

        try {
            in = new BufferedReader(new InputStreamReader(
                    new FileInputStream("/etc/hosts")));
            String line;
            while ((line = in.readLine()) != null) {
                if (line.contains("admob")) {
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;

    }

    public static Bundle checkPermissionsGranted(Context context) {
        Bundle b = new Bundle();
        b.putInt(Manifest.permission.ACCESS_FINE_LOCATION, ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION));
        b.putInt(Manifest.permission.ACCESS_COARSE_LOCATION, ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION));
        b.putInt(Manifest.permission.RECEIVE_SMS, ActivityCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS));
        b.putInt(Manifest.permission.SEND_SMS, ActivityCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS));
        b.putInt(Manifest.permission.MODIFY_AUDIO_SETTINGS, ActivityCompat.checkSelfPermission(context, Manifest.permission.MODIFY_AUDIO_SETTINGS));
        b.putInt(Manifest.permission.READ_PHONE_STATE, ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE));

        b.putBoolean("allGranted", b.getInt(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && b.getInt(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED && b.getInt(Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED && b.getInt(Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED && b.getInt(Manifest.permission.MODIFY_AUDIO_SETTINGS) != PackageManager.PERMISSION_GRANTED);

        return b;
    }

    public static boolean isTestDevice(Activity act) {
        String testLabSetting = Settings.System.getString(act.getContentResolver(), "firebase.test.lab");
        return "true".equals(testLabSetting);
    }

    public static boolean internetConnected(Context ctx) {
        ConnectivityManager cm =
                (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = Objects.requireNonNull(cm).getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    public static Bundle personalAdBundle(boolean personal) {
        Bundle extras = new Bundle();
        extras.putString("npa", personal ? "0" : "1");
        return extras;
    }
}
