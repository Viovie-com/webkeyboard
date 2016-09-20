package com.viovie.webkeyboard;

import android.content.Context;
import android.support.annotation.RawRes;
import android.view.inputmethod.ExtractedTextRequest;

import com.viovie.webkeyboard.service.RemoteKeyboardService;
import com.viovie.webkeyboard.task.ActionRunner;
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

    TextInputAction tia = new TextInputAction(RemoteKeyboardService.self);
    CtrlInputAction cia = new CtrlInputAction(RemoteKeyboardService.self);

    String index_html = null;
    String script_js = null;
    String style_css = null;
    String msgpack_min_js = null;

    private Context mContext;

    public WebServer(Context context, int port) throws IOException {
        super(port);
        this.mContext = context;

        index_html = loadLocalFile(R.raw.index);
        script_js = loadLocalFile(R.raw.script);
        style_css = loadLocalFile(R.raw.style);
        msgpack_min_js = loadLocalFile(R.raw.msgpack_min);
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
                        cia.keyCode = jsonObj.getInt("code");
                        cia.shiftKey = jsonObj.getBoolean("shift");
                        cia.altKey = jsonObj.getBoolean("alt");
                        cia.ctrlKey = jsonObj.getBoolean("ctrl");
                        ActionRunner actionRunner = new ActionRunner();
                        actionRunner.setAction(cia);
                        RemoteKeyboardService.self.handler.post(actionRunner);
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
            CharSequence txt = RemoteKeyboardService.self.getCurrentInputConnection().getExtractedText(
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
                    RemoteKeyboardService.self.handler.post(actionRunner);
                    actionRunner.waitResult();
                }
            }
            return newFixedLengthResponse(null);
        } else if (uri.equals("/append")) {
            if (Method.POST.equals(session.getMethod())) {
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
                    RemoteKeyboardService.self.handler.post(actionRunner);
                    actionRunner.waitResult();
                }
            }
            return newFixedLengthResponse(null);
        }

        // Default return index.html
        return newFixedLengthResponse(index_html);
    }

    private String loadLocalFile(@RawRes int id) throws IOException {
        InputStream is = mContext.getResources().openRawResource(id);
        byte[] buffer = new byte[is.available()];
        is.read(buffer);
        is.close();
        return new String(buffer);
    }
}
