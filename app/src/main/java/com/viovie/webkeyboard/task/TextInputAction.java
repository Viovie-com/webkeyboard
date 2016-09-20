package com.viovie.webkeyboard.task;

import android.view.inputmethod.InputConnection;

import com.viovie.webkeyboard.service.RemoteKeyboardService;

/**
 * Commit text into the current editor.
 *
 * @author patrick
 */
public class TextInputAction implements Runnable {

    public String text;
    public boolean replace_text = false;
    private RemoteKeyboardService myService;

    public TextInputAction(RemoteKeyboardService service) {
        this.myService = service;
    }

    @Override
    public void run() {
        InputConnection con = myService.getCurrentInputConnection();
        if (con != null && text != null) {
            if (replace_text) {
                con.performContextMenuAction(android.R.id.selectAll);
                con.commitText("", 1);
            }
            con.commitText(text, text.length());
        }
    }
}
