package com.viovie.webkeyboard.activity;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.viovie.webkeyboard.R;
import com.viovie.webkeyboard.util.ConnectUtil;
import com.viovie.webkeyboard.util.Logger;

public class AlertActivity extends Activity implements View.OnClickListener{
    private static Logger logger = Logger.getInstance(AlertActivity.class);
    public static final String INTENT_PARAM_IP = "intentIp";

    private String ip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            ip = bundle.getString(INTENT_PARAM_IP);
            TextView textView = (TextView) findViewById(R.id.text);
            textView.setText(getString(R.string.dialog_connect_message, ip));
        }

        Button yesButton = (Button) findViewById(R.id.yes);
        yesButton.setOnClickListener(this);

        Button noButton = (Button) findViewById(R.id.no);
        noButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.yes: {
                ConnectUtil.getInstance(this).connect(ip);
            }
            case R.id.no: {
                finish();
            }
        }
    }
}
