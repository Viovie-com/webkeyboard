package com.viovie.webkeyboard;

import android.view.inputmethod.InputConnection;

/**
 * Commit text into the current editor.
 *
 * @author patrick
 */
class TextInputAction implements Runnable {

    protected String text;
    protected boolean replace_text = false;
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
