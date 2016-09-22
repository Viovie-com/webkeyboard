package com.viovie.webkeyboard.util;

import android.util.Log;

import com.viovie.webkeyboard.BuildConfig;

public class Logger {
    private static boolean enable = BuildConfig.DEBUG;

    private String tag;

    private Logger(String tag) {
        this.tag = tag;
    }

    public static synchronized Logger getInstance(Class<?> clazz) {
        String tag = clazz.getName();

        return new Logger(tag);
    }

    public String getTag() {
        return tag;
    }

    public void v(String message) {
        if (enable) {
            Log.v(tag, message);
        }
    }

    public void v(String message, Throwable t) {
        if (enable) {
            Log.v(tag, message, t);
        }
    }

    public void d(String message) {
        if (enable) {
            Log.d(tag, message);
        }
    }

    public void d(String message, Throwable t) {
        if (enable) {
            Log.d(tag, message, t);
        }
    }

    public void i(String message) {
        if (enable) {
            Log.i(tag, message);
        }
    }

    public void i(String message, Throwable t) {
        if (enable) {
            Log.i(tag, message, t);
        }
    }

    public void w(String message) {
        if (enable) {
            Log.w(tag, message);
        }
    }

    public void w(String message, Throwable t) {
        if (enable) {
            Log.w(tag, message, t);
        }
    }

    public void e(String message) {
        if (enable) {
            Log.e(tag, message);
        }
    }

    public void e(String message, Throwable t) {
        if (enable) {
            Log.e(tag, message, t);
        }
    }
}
