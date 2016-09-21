package com.viovie.webkeyboard;

import android.content.res.Resources;
import android.support.annotation.RawRes;
import android.view.inputmethod.ExtractedTextRequest;

import com.viovie.webkeyboard.service.RemoteKeyboardService;
import com.viovie.webkeyboard.task.CtrlInputAction;
import com.viovie.webkeyboard.task.TextInputAction;

import org.json.JSONException;
import org.json.JSONObject;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class WebServer extends NanoHTTPD {
    private static String index_html = null;
    private static String script_js = null;
    private static String style_css = null;
    private static String msgpack_min_js = null;

    static {
        try {
            index_html = loadLocalFile(R.raw.index);
            script_js = loadLocalFile(R.raw.script);
            style_css = loadLocalFile(R.raw.style);
            msgpack_min_js = loadLocalFile(R.raw.msgpack_min);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private RemoteKeyboardService service;

    public WebServer(RemoteKeyboardService service, int port) {
        super(port);
        this.service = service;
    }

    @Override
    public Response serve(IHTTPSession session) {
        Map<String, String> parms = session.getParms();
        Map<String, String> header = session.getHeaders();
        String uri = session.getUri();

        // Return file
        if (uri.equals("/script.js")) {
            return newFixedLengthResponse(script_js);
        } else if (uri.equals("/msgpack.min.js")) {
            return newFixedLengthResponse(msgpack_min_js);
        } else if (uri.equals("/style.css")) {
            return newFixedLengthResponse(Response.Status.OK, "text/css", style_css);
        }

        if (uri.equals("/key")) {
            if (Method.POST.equals(session.getMethod())) {
                MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(session.getInputStream());
                JSONObject jsonObj = null;
                try {
                    jsonObj = new JSONObject(unpacker.unpackValue().toJson());
                    if (jsonObj.get("mode").equals("D")) {
                        CtrlInputAction cia = new CtrlInputAction(service);
                        cia.keyCode = jsonObj.getInt("code");
                        cia.shiftKey = jsonObj.getBoolean("shift");
                        cia.altKey = jsonObj.getBoolean("alt");
                        cia.ctrlKey = jsonObj.getBoolean("ctrl");
                        ActionRunner actionRunner = new ActionRunner();
                        actionRunner.setAction(cia);
                        service.handler.post(actionRunner);
                        actionRunner.waitResult();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return newFixedLengthResponse(null);
        } else if (uri.equals("/text")) {
            CharSequence txt = service.getCurrentInputConnection().getExtractedText(
                    new ExtractedTextRequest(), 0).text;
            MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
            try {
                packer.packString(txt.toString());
                packer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            InputStream is = new ByteArrayInputStream(packer.toByteArray());
            return newChunkedResponse(Response.Status.OK, "application/x-msgpack", is);
        } else if (uri.equals("/fill")) {
            if (Method.POST.equals(session.getMethod())) {
                TextInputAction tia = new TextInputAction(service);
                MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(session.getInputStream());
                try {
                    tia.text = unpacker.unpackString();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                tia.replace_text = true;
                if (tia.text != null) {
                    ActionRunner actionRunner = new ActionRunner();
                    actionRunner.setAction(tia);
                    service.handler.post(actionRunner);
                    actionRunner.waitResult();
                }
            }
            return newFixedLengthResponse(null);
        } else if (uri.equals("/append")) {
            if (Method.POST.equals(session.getMethod())) {
                TextInputAction tia = new TextInputAction(service);
                MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(session.getInputStream());
                try {
                    tia.text = unpacker.unpackString();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                tia.replace_text = false;
                if (tia.text != null) {
                    ActionRunner actionRunner = new ActionRunner();
                    actionRunner.setAction(tia);
                    service.handler.post(actionRunner);
                    actionRunner.waitResult();
                }
            }
            return newFixedLengthResponse(null);
        }

        // Default return index.html
        return newFixedLengthResponse(index_html);
    }

    private static String loadLocalFile(@RawRes int id) throws IOException {
        InputStream is = Resources.getSystem().openRawResource(id);
        byte[] buffer = new byte[is.available()];
        is.read(buffer);
        is.close();
        return new String(buffer);
    }

    /**
     * Wrapper for InputAction. We cannot post InputActions directly to the
     * messagequeue because we use commitText() and sendKeyEvent(). The later
     * executes asynchronously and hence fast commits (e.g. via copy&paste) result
     * in linebreaks being out of order.
     *
     * @author patrick
     */
    public class ActionRunner implements Runnable {

        private Runnable action;
        private boolean finished;

        public void setAction(Runnable action) {
            this.action = action;
            this.finished = false;
        }

        public void run() {
            action.run();
            synchronized (this) {
                finished = true;
                notify();
            }
        }

        public synchronized void waitResult() {
            while (!finished) {
                try {
                    wait();
                } catch (InterruptedException e) {
                }
            }
        }
    }
}

