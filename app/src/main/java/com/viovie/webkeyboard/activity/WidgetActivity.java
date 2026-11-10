package com.viovie.webkeyboard.activity;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.inputmethod.InputMethodManager;

public class WidgetActivity extends Activity {

    public WidgetActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final InputMethodManager service = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (service != null) {
            service.showInputMethodPicker();
        }
        finish();
    }

}
