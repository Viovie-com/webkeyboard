package com.viovie.webkeyboard;

import android.content.SharedPreferences;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.widget.Toast;

/**
 * Models a key event sent from the remote keyboard via the telnet protocol and
 * provides a method for replaying that input on the UI thread (possibly
 * translating it from telnet to android semantics).
 *
 * @author patrick
 */
class CtrlInputAction implements Runnable {

    public static final String TAG = "InputAction";
    public static final String PREF_QUICKLAUNCHER = "pref_quicklauncher";

    /**
     * A control character (anything thats not printable)
     */
    protected int keyCode;
    protected boolean ctrlKey = false;
    protected boolean altKey = false;
    protected boolean shiftKey = false;

    /**
     * For sending raw key presses to the editor
     */
    private RemoteKeyboardService myService;

    public CtrlInputAction(RemoteKeyboardService myService) {
        this.myService = myService;
    }

    // @Override
    public void run() {
        InputConnection con = myService.getCurrentInputConnection();
        if (con == null) {
            return;
        }

        switch (keyCode) {
            case 8: { // Backspace
                typeKey(con, KeyEvent.KEYCODE_DEL);
                break;
            }
            case 13: {
                handleEnterKey(con);
                break;
            }
            case 9: {
                typeKey(con, KeyEvent.KEYCODE_TAB);
                break;
            }
            case 37: {
                if (shiftKey)
                    markText(con, KeyEvent.KEYCODE_DPAD_LEFT);
                else
                    typeKey(con, KeyEvent.KEYCODE_DPAD_LEFT);
                break;
            }
            case 39: {
                if (shiftKey)
                    markText(con, KeyEvent.KEYCODE_DPAD_RIGHT);
                else
                    typeKey(con, KeyEvent.KEYCODE_DPAD_RIGHT);
                break;
            }
            case 38: {
                if (shiftKey)
                    markText(con, KeyEvent.KEYCODE_DPAD_UP);
                else
                    typeKey(con, KeyEvent.KEYCODE_DPAD_UP);
                break;
            }
            case 40: {
                if (shiftKey)
                    markText(con, KeyEvent.KEYCODE_DPAD_DOWN);
                else
                    typeKey(con, KeyEvent.KEYCODE_DPAD_DOWN);
                break;
            }
            case 45: {
                replaceText(con);
                break;
            }
            case 46: { // Del
                if (con.getSelectedText(0) == null) {
                    con.deleteSurroundingText(0, 1);
                } else {
                    typeKey(con, KeyEvent.KEYCODE_DEL);
                }
                break;
            }
            case 36: { // Home
                typeKey(con, KeyEvent.KEYCODE_MOVE_HOME);
                break;
            }
            case 35: { // End
                typeKey(con, KeyEvent.KEYCODE_MOVE_END);
                break;
            }
            case 65: { // CTRL-A
                if (ctrlKey)
                    con.performContextMenuAction(android.R.id.selectAll);
                break;
            }
            case 67: { // CTRL-C
                if (ctrlKey)
                    con.performContextMenuAction(android.R.id.copy);
                break;
            }
            case 76: { // CTRL-L
                if (ctrlKey)
                    con.performEditorAction(EditorInfo.IME_ACTION_SEND);
                break;
            }
            case 27: { // ESC
                typeKey(con, KeyEvent.KEYCODE_BACK);
                break;
            }
            case 70: { // CTRL-F
                if(ctrlKey)
                    typeKey(con, KeyEvent.KEYCODE_SEARCH);
                break;
            }
            case 113: { // F2
                typeKey(con, KeyEvent.KEYCODE_MENU);
                break;
            }
            case 112: { // F1
                typeKey(con, KeyEvent.KEYCODE_HOME);
                break;
            }
            case 86: { // CTRL-V
                if (ctrlKey)
                    con.performContextMenuAction(android.R.id.paste);
                break;
            }
            case 88: { // CTRL-X
                if (ctrlKey)
                    con.performContextMenuAction(android.R.id.cut);
                break;
            }
            case 116: // F5
                typeKey(con, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
                break;
            case 117: // F6
                typeKey(con, KeyEvent.KEYCODE_MEDIA_STOP);
                break;
            case 118: // F7
                typeKey(con, KeyEvent.KEYCODE_MEDIA_PREVIOUS);
                break;
            case 119: // F8
                typeKey(con, KeyEvent.KEYCODE_MEDIA_NEXT);
                break;
            case 120: // F9
                typeKey(con, KeyEvent.KEYCODE_VOLUME_MUTE);
                break;
            case 121: // F10
                typeKey(con, KeyEvent.KEYCODE_VOLUME_DOWN);
                break;
            case 122: // F11
                typeKey(con, KeyEvent.KEYCODE_VOLUME_UP);
                break;
            case 123: // F12
                break;
        }
    }

    /**
     * Mark text using SHIFT+DPAD
     *
     * @param con     input connection
     * @param keycode DPAD keycode
     */
    private void markText(InputConnection con, int keycode) {
        long now = SystemClock.uptimeMillis();
        con.sendKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_SHIFT_LEFT, 0, 0));
        con.sendKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_DOWN, keycode, 0,
                KeyEvent.META_SHIFT_LEFT_ON));
        con.sendKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_UP, keycode, 0,
                KeyEvent.META_SHIFT_LEFT_ON));
        con.sendKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_UP,
                KeyEvent.KEYCODE_SHIFT_LEFT, 0, 0));
    }

    /**
     * Send an down/up event
     *
     * @param key keycode
     * @con connection to sent with
     */
    private void typeKey(InputConnection con, int key) {
        con.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, key));
        con.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, key));
    }

    /**
     * Try to replace the current word with its substitution.
     */
    private void replaceText(InputConnection con) {
        ExtractedText txt = con.getExtractedText(new ExtractedTextRequest(), 0);
        if (txt != null) {
            int end = txt.text.toString().indexOf(" ", txt.selectionEnd);
            if (end == -1) {
                end = txt.text.length();
            }
            int start = txt.text.toString().lastIndexOf(" ", txt.selectionEnd - 1);
            start++;
            String sel = txt.text.subSequence(start, end).toString();
            String rep = myService.replacements.get(sel);
            if (rep != null) {
                con.setComposingRegion(start, end);
                con.setComposingText(rep, 1);
                con.finishComposingText();
            } else {
                String err = myService.getResources().getString(
                        R.string.err_no_replacement, sel);
                Toast.makeText(myService, err, Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Figure out how we are connected to the edittext and what it expects the
     * enter key to do.
     */
    private void handleEnterKey(InputConnection con) {
        EditorInfo ei = myService.getCurrentInputEditorInfo();
        if (ei != null
                && ((ei.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) != EditorInfo.IME_FLAG_NO_ENTER_ACTION)) {
            int[] acts = {EditorInfo.IME_ACTION_DONE, EditorInfo.IME_ACTION_SEARCH,
                    EditorInfo.IME_ACTION_GO, EditorInfo.IME_ACTION_NEXT};

            for (int i : acts) {
                if ((ei.imeOptions & i) == i) {
                    con.performEditorAction(i);
                    return;
                }
            }
        }
        typeKey(con, KeyEvent.KEYCODE_ENTER);
    }
}
