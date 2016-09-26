package com.viovie.webkeyboard.activity;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;

import com.viovie.webkeyboard.ConnectListPreferences;
import com.viovie.webkeyboard.util.Logger;

public class ConnectListActivity extends ListActivity {
    private static Logger logger = Logger.getInstance(ConnectListActivity.class);

    // This is the Adapter being used to display the list's data
    ArrayAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create a progress bar to display while the list loads
        ProgressBar progressBar = new ProgressBar(this);
        progressBar.setIndeterminate(true);
        getListView().setEmptyView(progressBar);

        // Must add the progress bar to the root of the layout
        ViewGroup root = (ViewGroup) findViewById(android.R.id.content);
        root.addView(progressBar);

        // Create an empty adapter we will use to display the loaded data.
        // We pass null for the cursor, then update it in onLoadFinished()
        mAdapter = new ArrayAdapter(this,
                android.R.layout.simple_list_item_1,
                ConnectListPreferences.getIpList(this).toArray()
                );
        setListAdapter(mAdapter);
    }
}
