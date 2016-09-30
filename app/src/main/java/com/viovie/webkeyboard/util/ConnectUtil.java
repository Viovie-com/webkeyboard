package com.viovie.webkeyboard.util;

import android.content.Context;
import android.content.SharedPreferences;

public class ConnectUtil {
    private static Logger logger = Logger.getInstance(ConnectUtil.class);
    private static final String CONNECT_UTIL = "ConnectUtil";
    private static final String CONNECT_IP = "connectIp";

    private static ConnectUtil instance;
    private SharedPreferences sharedPreferences;
    private Context mContext;

    private ConnectUtil(Context context) {
        mContext = context;
        sharedPreferences = mContext.getSharedPreferences(CONNECT_UTIL, Context.MODE_PRIVATE);
    }

    public static synchronized ConnectUtil getInstance(Context context) {
        if (instance == null) {
            instance = new ConnectUtil(context);
        }
        return instance;
    }

    public void connect(String ip) {
        sharedPreferences.edit().putString(CONNECT_IP, ip).apply();
    }

    public boolean isConnect(String ip) {
        String existIp = sharedPreferences.getString(CONNECT_IP, null);
        return existIp != null && existIp.equals(ip);
    }

    public void disconnect() {
        sharedPreferences.edit().putString(CONNECT_IP, null).apply();
    }
}
