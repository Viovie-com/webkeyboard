package com.viovie.webkeyboard.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class ConnectListUtil {
    private static final String CONNECT_IP_LIST = "connectIpList";
    private static final String IP_LIST = "ipList";
    private static final String BLOCK_IP_LIST = "blockIpList";

    private static SharedPreferences mSharedPreferences;

    private static synchronized SharedPreferences getSharedPreferences(Context context) {
        if (mSharedPreferences == null) {
            mSharedPreferences = context.getSharedPreferences(
                    CONNECT_IP_LIST, Context.MODE_PRIVATE
            );
        }
        return mSharedPreferences;
    }

    public static Collection getIpList(Context context) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);

        Collection list = sharedPreferences.getStringSet(IP_LIST, new HashSet<String>());
        return list;
    }

    public static void saveIp(Context context, String ip) {
        Collection list = getIpList(context);
        if (list.contains(ip)) {
            return;
        }

        list.add(ip);
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        sharedPreferences.edit().putStringSet(IP_LIST, (Set)list).apply();
    }

    public static void clear(Context context) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        sharedPreferences.edit()
                .putStringSet(IP_LIST, new HashSet<String>())
                .putStringSet(BLOCK_IP_LIST, new HashSet<String>())
                .apply();
    }

    public static Collection getBlockIpList(Context context) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);

        Collection list = sharedPreferences.getStringSet(BLOCK_IP_LIST, new HashSet<String>());
        return list;
    }

    public static void blockIp(Context context, String ip) {
        if (isBlock(context, ip)) return;

        Collection list = getBlockIpList(context);
        list.add(ip);
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        sharedPreferences.edit().putStringSet(BLOCK_IP_LIST, (Set)list).apply();
    }

    public static void cancelBlockIp(Context context, String ip) {
        if (!isBlock(context, ip)) return;

        Collection list = getBlockIpList(context);
        list.remove(ip);
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        sharedPreferences.edit().putStringSet(BLOCK_IP_LIST, (Set)list).apply();
    }

    public static boolean isBlock(Context context, String ip) {
        return getBlockIpList(context).contains(ip);
    }
}
