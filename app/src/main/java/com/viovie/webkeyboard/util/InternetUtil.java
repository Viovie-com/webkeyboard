package com.viovie.webkeyboard.util;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.List;

public class InternetUtil {
    public static String getWifiIpAddress(Context context) {
        String ipAddress = null;
        try {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                if (wifiInfo != null) {
                    int ip = wifiInfo.getIpAddress();
                    if (ip != 0) {
                        // Convert little-endian to big-endian if needed
                        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
                            ip = Integer.reverseBytes(ip);
                        }

                        byte[] ipByteArray = BigInteger.valueOf(ip).toByteArray();

                        try {
                            ipAddress = InetAddress.getByAddress(ipByteArray).getHostAddress();
                        } catch (UnknownHostException ex) {
                            ipAddress = null;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }

        if (ipAddress == null || "0.0.0.0".equals(ipAddress)) {
            ipAddress = getNetworkInterfaceIpAddress();
        }

        return ipAddress;
    }

    private static String getNetworkInterfaceIpAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            // First check wifi / wlan interfaces
            for (NetworkInterface intf : interfaces) {
                if (intf.getName().toLowerCase().contains("wlan")) {
                    for (InetAddress addr : Collections.list(intf.getInetAddresses())) {
                        if (!addr.isLoopbackAddress() && !addr.isLinkLocalAddress() && addr.getAddress().length == 4) {
                            return addr.getHostAddress();
                        }
                    }
                }
            }
            // Fallback to any non-loopback IPv4 interface
            for (NetworkInterface intf : interfaces) {
                for (InetAddress addr : Collections.list(intf.getInetAddresses())) {
                    if (!addr.isLoopbackAddress() && !addr.isLinkLocalAddress() && addr.getAddress().length == 4) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
