package com.viovie.webkeyboard;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class ConnectListPreferences {
    private static final String CONNECT_IP_LIST = "connectIpList";
    private static final String IP_LIST = "ipList";

    private static SharedPreferences mSharedPreferences;

    private static synchronized SharedPreferences getSharedPreferences(Context context) {
        if (mSharedPreferences == null) {
            mSharedPreferences = context.getSharedPreferences(
                    CONNECT_IP_LIST, Context.MODE_PRIVATE
            );
        }
        return mSharedPreferences;
    }

    public static void saveIp(Context context, String ip) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);

        Collection list = getIpList(context);
        if (list.contains(ip)) {
            return;
        }

        list.add(ip);
        sharedPreferences.edit().putStringSet(IP_LIST, (Set)list).apply();
    }

    public static Collection getIpList(Context context) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);

        Collection list = sharedPreferences.getStringSet(IP_LIST, new HashSet<String>());
        return list;
    }

    public static void clear(Context context) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        sharedPreferences.edit().putStringSet(IP_LIST, new HashSet<String>()).apply();
    }
}
