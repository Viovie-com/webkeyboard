package com.viovie.webkeyboard.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.viovie.webkeyboard.R;
import com.viovie.webkeyboard.service.RemoteKeyboardService;
import com.viovie.webkeyboard.util.InternetUtil;
import com.viovie.webkeyboard.util.Logger;

import java.util.Iterator;
import java.util.List;

public class MainActivity extends Activity {
    private static Logger logger = Logger.getInstance(MainActivity.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();

        String ipAddress = InternetUtil.getWifiIpAddress(this);

        TextView instructionText = (TextView) findViewById(R.id.quickinstructions);
        instructionText.setText(getString(R.string.app_quickinstuctions, ipAddress));

        boolean isEnabled = isWebKeyboardEnabled();
        if (!isEnabled) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.dialog_error_not_enabled)
                    .setTitle(R.string.dialog_error_not_enabled_title)
                    .setPositiveButton(android.R.string.yes, dialogOnClick)
                    .setNegativeButton(android.R.string.no, dialogOnClick)
                    .create()
                    .show();
        }

        String inputText = getIntent().getStringExtra(Intent.EXTRA_TEXT);
        if (isEnabled && inputText != null) {
            TextView inputTextView = (TextView) findViewById(R.id.typetest);
            inputTextView.setText(inputText);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.keyboard_select) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showInputMethodPicker();
            }
            return true;
        } else if (id == R.id.keyboard_setting) {
            showKeyboardSetting();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private DialogInterface.OnClickListener dialogOnClick = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            // We are called from the RK is not enabled as IME method.
            if (which == DialogInterface.BUTTON_POSITIVE) {
                showKeyboardSetting();
            }
        }
    };

    private void showKeyboardSetting() {
        startActivity(new Intent(android.provider.Settings.ACTION_INPUT_METHOD_SETTINGS));
    }

    private boolean isWebKeyboardEnabled() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm == null) {
            return false;
        }
        List<InputMethodInfo> enabledList = imm.getEnabledInputMethodList();
        if (enabledList == null) {
            return false;
        }
        Iterator<InputMethodInfo> it = enabledList.iterator();

        boolean available = false;
        while (it.hasNext()) {
            available = it.next()
                    .getServiceName()
                    .equals(RemoteKeyboardService.class.getCanonicalName());
            if (available) {
                break;
            }
        }

        return available;
    }
}
