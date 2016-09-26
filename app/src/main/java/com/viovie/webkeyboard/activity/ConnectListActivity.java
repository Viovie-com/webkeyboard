package com.viovie.webkeyboard.activity;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.viovie.webkeyboard.util.ConnectListUtil;
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
                ConnectListUtil.getIpList(this).toArray()
                );
        setListAdapter(mAdapter);
    }

    @Override
    protected void onListItemClick(ListView list, View v, int position, long id) {
        String ip = (String) list.getAdapter().getItem(position);
        if (ConnectListUtil.isBlock(this, ip)) {
            ConnectListUtil.cancelBlockIp(this, ip);
        } else {
            ConnectListUtil.blockIp(this, ip);
        }
    }
}
