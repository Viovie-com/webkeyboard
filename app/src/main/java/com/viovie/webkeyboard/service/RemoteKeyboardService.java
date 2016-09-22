package com.viovie.webkeyboard.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.inputmethodservice.KeyboardView.OnKeyboardActionListener;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

import com.viovie.webkeyboard.R;
import com.viovie.webkeyboard.WebServer;
import com.viovie.webkeyboard.activity.MainActivity;
import com.viovie.webkeyboard.util.InternetUtil;
import com.viovie.webkeyboard.util.Logger;

import java.io.IOException;
import java.net.InetAddress;

public class RemoteKeyboardService extends InputMethodService implements
        OnKeyboardActionListener {
    private static Logger logger = Logger.getInstance(RemoteKeyboardService.class);

    /**
     * For referencing our notification in the notification area.
     */
    public static final int NOTIFICATION = 42;

    /**
     * For posting InputActions on the UI thread.
     */
    public Handler handler;

    private WebServer webServer;

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();

        try {
            webServer = new WebServer(this, 8080);
            webServer.start();
            updateNotification(null);
        } catch (IOException e) {
            logger.e("new WebServer exception", e);
        }
    }

    @Override
    public boolean onEvaluateFullscreenMode() {
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(this);
        return p.getBoolean("pref_fullscreen", false);
    }

    @Override
    public View onCreateInputView() {
        KeyboardView ret = new KeyboardView(this, null);
        ret.setKeyboard(new Keyboard(this, R.xml.keyboarddef));
        ret.setOnKeyboardActionListener(this);
        ret.setPreviewEnabled(false);
        return ret;
    }

    @Override
    public void onInitializeInterface() {
        super.onInitializeInterface();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        webServer.stop();
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION);
    }

    @Override
    public void onPress(int primaryCode) {
        switch (primaryCode) {
            case 0: {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showInputMethodPicker();
                break;
            }
        }
    }

    @Override
    public void onRelease(int primaryCode) {
    }

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
    }

    @Override
    public void onText(CharSequence text) {
    }

    @Override
    public void swipeLeft() {
    }

    @Override
    public void swipeRight() {
    }

    @Override
    public void swipeDown() {
    }

    @Override
    public void swipeUp() {
    }

    /**
     * Update the message in the notification area
     *
     * @param remote the remote host we are connected to or null if not connected.
     */
    protected void updateNotification(InetAddress remote) {
        String title = getString(R.string.notification_title);
        String content = null;
        if (remote == null) {
            String ipAddress = InternetUtil.getWifiIpAddress(this);
            content = getString(R.string.notification_waiting, "" + ipAddress);
        } else {
            content = getString(R.string.notification_peer, remote.getHostName());
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder
                .setContentText(content)
                .setContentTitle(title)
                .setOngoing(true)
                .setContentIntent(
                        PendingIntent.getActivity(this, 0, new Intent(this,
                                MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT))
                .setSmallIcon(R.drawable.ic_stat_service);
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION, builder.build());
    }
}
