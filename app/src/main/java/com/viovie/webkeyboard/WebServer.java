package com.viovie.webkeyboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;

import androidx.annotation.RawRes;

import com.viovie.webkeyboard.activity.AlertActivity;
import com.viovie.webkeyboard.service.RemoteKeyboardService;
import com.viovie.webkeyboard.task.CtrlInputAction;
import com.viovie.webkeyboard.task.TextInputAction;
import com.viovie.webkeyboard.util.ConnectUtil;
import com.viovie.webkeyboard.util.Logger;

import org.json.JSONException;
import org.json.JSONObject;
import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;

public class WebServer {
    private static Logger logger = Logger.getInstance(WebServer.class);

    private static final String KEYSTORE_PASSWORD = "webkeyboard";

    private final RemoteKeyboardService service;
    private final int port;
    private final boolean isHttps;
    private final String password;

    private ServerSocket serverSocket;
    private ExecutorService executor;
    private volatile boolean isRunning;

    public WebServer(RemoteKeyboardService service, int port) {
        this(service, port, false, null);
    }

    public WebServer(RemoteKeyboardService service, int port, boolean isHttps, String password) {
        this.service = service;
        this.port = port;
        this.isHttps = isHttps;
        this.password = (password != null && !password.trim().isEmpty()) ? password : null;
    }

    public synchronized void start() throws IOException {
        if (isRunning) {
            return;
        }
        if (isHttps) {
            serverSocket = createSSLServerSocket(port);
        } else {
            serverSocket = new ServerSocket(port);
        }
        executor = Executors.newCachedThreadPool();
        isRunning = true;
        executor.submit(this::listenLoop);
        logger.i("WebServer started on port " + port + (isHttps ? " (HTTPS)" : " (HTTP)"));
    }

    public synchronized void stop() {
        isRunning = false;
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                logger.e("Error closing server socket", e);
            }
            serverSocket = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        logger.i("WebServer stopped");
    }

    public int getPort() {
        return port;
    }

    public boolean isHttps() {
        return isHttps;
    }

    private ServerSocket createSSLServerSocket(int port) throws IOException {
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            try (InputStream is = service.getResources().openRawResource(R.raw.keystore)) {
                keyStore.load(is, KEYSTORE_PASSWORD.toCharArray());
            }

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, KEYSTORE_PASSWORD.toCharArray());

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), null, null);

            SSLServerSocketFactory ssf = sslContext.getServerSocketFactory();
            return ssf.createServerSocket(port);
        } catch (Exception e) {
            logger.e("Failed to create SSLServerSocket", e);
            throw new IOException("Failed to create SSLServerSocket: " + e.getMessage(), e);
        }
    }

    private void listenLoop() {
        while (isRunning && serverSocket != null && !serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                if (executor != null && !executor.isShutdown()) {
                    executor.submit(() -> handleClient(socket));
                }
            } catch (IOException e) {
                if (!isRunning) {
                    break;
                }
                logger.e("Error accepting client connection", e);
            }
        }
    }

    private void handleClient(Socket socket) {
        try (socket;
             InputStream rawIn = new BufferedInputStream(socket.getInputStream());
             OutputStream rawOut = new BufferedOutputStream(socket.getOutputStream())) {

            // Read HTTP request line and headers
            HttpRequest request = parseRequest(rawIn, socket.getInetAddress());
            if (request == null) {
                sendResponse(rawOut, 400, "Bad Request", "text/plain; charset=utf-8", "Bad Request".getBytes(StandardCharsets.UTF_8), null);
                return;
            }

            // Check password authentication if configured
            if (password != null) {
                String authHeader = request.headers.get("authorization");
                if (!isAuthorized(authHeader, password)) {
                    Map<String, String> extraHeaders = new HashMap<>();
                    extraHeaders.put("WWW-Authenticate", "Basic realm=\"Web Keyboard\"");
                    sendResponse(rawOut, 401, "Unauthorized", "text/plain; charset=utf-8",
                            "Authentication required.\n".getBytes(StandardCharsets.UTF_8), extraHeaders);
                    return;
                }
            }

            processRequest(request, rawOut);
        } catch (Exception e) {
            logger.e("Error processing client request", e);
        }
    }

    public static boolean isAuthorized(String authHeader, String expectedPassword) {
        if (expectedPassword == null || expectedPassword.isEmpty()) {
            return true;
        }
        if (authHeader == null || !authHeader.toLowerCase(Locale.US).startsWith("basic ")) {
            return false;
        }
        String b64 = authHeader.substring(6).trim();
        try {
            byte[] decodedBytes = decodeBase64(b64);
            if (decodedBytes == null) {
                return false;
            }
            String decoded = new String(decodedBytes, StandardCharsets.UTF_8);
            int colonIndex = decoded.indexOf(':');
            String user;
            String pass;
            if (colonIndex != -1) {
                user = decoded.substring(0, colonIndex);
                pass = decoded.substring(colonIndex + 1);
            } else {
                user = "";
                pass = decoded;
            }
            return expectedPassword.equals(pass) || expectedPassword.equals(user);
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] decodeBase64(String input) {
        if (input == null) {
            return null;
        }
        int len = input.length();
        // Base64 lookup table
        int[] table = new int[128];
        for (int i = 0; i < 128; i++) {
            table[i] = -1;
        }
        for (int i = 'A'; i <= 'Z'; i++) {
            table[i] = i - 'A';
        }
        for (int i = 'a'; i <= 'z'; i++) {
            table[i] = i - 'a' + 26;
        }
        for (int i = '0'; i <= '9'; i++) {
            table[i] = i - '0' + 52;
        }
        table['+'] = 62;
        table['/'] = 63;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int bits = 0;
        int bitCount = 0;
        for (int i = 0; i < len; i++) {
            char c = input.charAt(i);
            if (c == '=') {
                break;
            }
            if (c >= 128 || table[c] < 0) {
                continue; // ignore whitespace or illegal chars
            }
            bits = (bits << 6) | table[c];
            bitCount += 6;
            if (bitCount >= 8) {
                bitCount -= 8;
                out.write((bits >> bitCount) & 0xFF);
            }
        }
        return out.toByteArray();
    }

    private HttpRequest parseRequest(InputStream in, InetAddress clientAddress) throws IOException {
        ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream();
        String requestLine = readLine(in, lineBuffer);
        if (requestLine == null || requestLine.isEmpty()) {
            return null;
        }

        String[] parts = requestLine.split(" ");
        if (parts.length < 2) {
            return null;
        }
        String method = parts[0].toUpperCase(Locale.US);
        String uri = parts[1];
        int queryIndex = uri.indexOf('?');
        if (queryIndex != -1) {
            uri = uri.substring(0, queryIndex);
        }

        Map<String, String> headers = new HashMap<>();
        String headerLine;
        while ((headerLine = readLine(in, lineBuffer)) != null && !headerLine.isEmpty()) {
            int colonIndex = headerLine.indexOf(':');
            if (colonIndex > 0) {
                String name = headerLine.substring(0, colonIndex).trim().toLowerCase(Locale.US);
                String value = headerLine.substring(colonIndex + 1).trim();
                headers.put(name, value);
            }
        }

        byte[] body = new byte[0];
        String contentLengthHeader = headers.get("content-length");
        if (contentLengthHeader != null) {
            try {
                int contentLength = Integer.parseInt(contentLengthHeader);
                if (contentLength > 0) {
                    body = new byte[contentLength];
                    int totalRead = 0;
                    while (totalRead < contentLength) {
                        int read = in.read(body, totalRead, contentLength - totalRead);
                        if (read == -1) {
                            break;
                        }
                        totalRead += read;
                    }
                }
            } catch (NumberFormatException ignored) {
            }
        }

        String ip = headers.get("http-client-ip");
        if (ip == null || ip.isEmpty()) {
            ip = headers.get("x-forwarded-for");
        }
        if (ip == null || ip.isEmpty()) {
            ip = clientAddress != null ? clientAddress.getHostAddress() : "";
        }

        return new HttpRequest(method, uri, headers, body, ip);
    }

    private String readLine(InputStream in, ByteArrayOutputStream buffer) throws IOException {
        buffer.reset();
        int c;
        while ((c = in.read()) != -1) {
            if (c == '\r') {
                continue;
            }
            if (c == '\n') {
                break;
            }
            buffer.write(c);
        }
        if (c == -1 && buffer.size() == 0) {
            return null;
        }
        return buffer.toString(StandardCharsets.UTF_8.name());
    }

    private void processRequest(HttpRequest request, OutputStream out) throws IOException {
        String uri = request.uri;
        String method = request.method;
        String ip = request.clientIp;

        // Static resources
        if ("/script.js".equals(uri)) {
            sendResponse(out, 200, "OK", "application/javascript; charset=utf-8", loadLocalFile(R.raw.script), null);
            return;
        } else if ("/msgpack.min.js".equals(uri)) {
            sendResponse(out, 200, "OK", "application/javascript; charset=utf-8", loadLocalFile(R.raw.msgpack_min), null);
            return;
        } else if ("/style.css".equals(uri)) {
            sendResponse(out, 200, "OK", "text/css; charset=utf-8", loadLocalFile(R.raw.style), null);
            return;
        }

        // Check connection verification
        if ("/check".equals(uri) && "POST".equals(method)) {
            boolean isConnected = ConnectUtil.getInstance(service).isConnect(ip);
            int statusCode = isConnected ? 200 : 406;
            String statusText = isConnected ? "OK" : "Not Acceptable";
            sendResponse(out, statusCode, statusText, "application/json", new byte[0], null);
            return;
        }

        // Connection verification check for other endpoints
        if (!ConnectUtil.getInstance(service).isConnect(ip)) {
            service.handler.post(() -> {
                Intent intent = new Intent(service, AlertActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                Bundle bundle = new Bundle();
                bundle.putString(AlertActivity.INTENT_PARAM_IP, ip);
                intent.putExtras(bundle);
                service.startActivity(intent);
            });

            // Return reload page
            sendResponse(out, 200, "OK", "text/html; charset=utf-8", loadLocalFile(R.raw.reload), null);
            return;
        }

        if ("/key".equals(uri)) {
            if ("POST".equals(method)) {
                MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(request.body);
                try {
                    JSONObject jsonObj = new JSONObject(unpacker.unpackValue().toJson());
                    logger.e("key:" + jsonObj.toString());
                    if ("D".equals(jsonObj.optString("mode"))) {
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
                } catch (IOException | JSONException e) {
                    logger.e("uri key", e);
                }
            }
            sendResponse(out, 200, "OK", "application/octet-stream", new byte[0], null);
            return;
        } else if ("/text".equals(uri)) {
            CharSequence txt = "";
            InputConnection ic = service.getCurrentInputConnection();
            if (ic != null) {
                ExtractedText et = ic.getExtractedText(new ExtractedTextRequest(), 0);
                if (et != null && et.text != null) {
                    txt = et.text;
                }
            }
            MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
            try {
                packer.packString(txt.toString());
                packer.close();
            } catch (IOException e) {
                logger.e("uri text", e);
            }
            byte[] packed = packer.toByteArray();
            sendResponse(out, 200, "OK", "application/x-msgpack", packed, null);
            return;
        } else if ("/fill".equals(uri)) {
            if ("POST".equals(method)) {
                TextInputAction tia = new TextInputAction(service);
                MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(request.body);
                try {
                    tia.text = unpacker.unpackString();
                } catch (IOException e) {
                    logger.e("uri fill", e);
                }
                tia.replace_text = true;
                if (tia.text != null) {
                    ActionRunner actionRunner = new ActionRunner();
                    actionRunner.setAction(tia);
                    service.handler.post(actionRunner);
                    actionRunner.waitResult();
                }
            }
            sendResponse(out, 200, "OK", "application/octet-stream", new byte[0], null);
            return;
        } else if ("/append".equals(uri)) {
            if ("POST".equals(method)) {
                TextInputAction tia = new TextInputAction(service);
                MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(request.body);
                try {
                    tia.text = unpacker.unpackString();
                } catch (IOException e) {
                    logger.e("uri append", e);
                }
                tia.replace_text = false;
                if (tia.text != null) {
                    ActionRunner actionRunner = new ActionRunner();
                    actionRunner.setAction(tia);
                    service.handler.post(actionRunner);
                    actionRunner.waitResult();
                }
            }
            sendResponse(out, 200, "OK", "application/octet-stream", new byte[0], null);
            return;
        }

        // Default: return index.html
        sendResponse(out, 200, "OK", "text/html; charset=utf-8", loadLocalFile(R.raw.index), null);
    }

    private void sendResponse(OutputStream out, int statusCode, String statusText,
                              String contentType, byte[] data, Map<String, String> extraHeaders) throws IOException {
        StringBuilder headers = new StringBuilder();
        headers.append("HTTP/1.1 ").append(statusCode).append(" ").append(statusText).append("\r\n");
        if (contentType != null) {
            headers.append("Content-Type: ").append(contentType).append("\r\n");
        }
        if (extraHeaders != null) {
            for (Map.Entry<String, String> entry : extraHeaders.entrySet()) {
                headers.append(entry.getKey()).append(": ").append(entry.getValue()).append("\r\n");
            }
        }
        headers.append("Content-Length: ").append(data != null ? data.length : 0).append("\r\n");
        headers.append("Connection: close\r\n");
        headers.append("\r\n");

        out.write(headers.toString().getBytes(StandardCharsets.UTF_8));
        if (data != null && data.length > 0) {
            out.write(data);
        }
        out.flush();
    }

    private byte[] loadLocalFile(@RawRes int id) {
        InputStream is = null;
        try {
            is = service.getResources().openRawResource(id);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = is.read(buffer)) != -1) {
                baos.write(buffer, 0, read);
            }
            return baos.toByteArray();
        } catch (IOException e) {
            logger.e("loadLocalFile " + id, e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ex) {
                    logger.e("loadLocalFile close " + id, ex);
                }
            }
        }
        return new byte[0];
    }

    private static class HttpRequest {
        final String method;
        final String uri;
        final Map<String, String> headers;
        final byte[] body;
        final String clientIp;

        HttpRequest(String method, String uri, Map<String, String> headers, byte[] body, String clientIp) {
            this.method = method;
            this.uri = uri;
            this.headers = headers;
            this.body = body;
            this.clientIp = clientIp;
        }
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
                } catch (InterruptedException ignored) {
                }
            }
        }
    }
}
