package com.viovie.webkeyboard;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 */
@RunWith(AndroidJUnit4.class)
public class ApplicationTest {
    @Test
    public void useAppContext() {
        Context appContext = ApplicationProvider.getApplicationContext();
        assertEquals("com.viovie.webkeyboard", appContext.getPackageName());
    }
}
