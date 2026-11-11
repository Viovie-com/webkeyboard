package com.viovie.webkeyboard;

import com.viovie.webkeyboard.activity.SettingsActivity;

import org.junit.Test;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.Base64;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class OptionsTest {

    @Test
    public void testPortParsing() {
        assertEquals(8080, SettingsActivity.DEFAULT_PORT);

        String portStr = "9090";
        int parsed = Integer.parseInt(portStr);
        assertEquals(9090, parsed);
        assertTrue(parsed >= 1024 && parsed <= 65535);

        String invalidPortStr = "70000";
        int invalidPort = Integer.parseInt(invalidPortStr);
        assertFalse(invalidPort >= 1024 && invalidPort <= 65535);
    }

    @Test
    public void testAuthenticationVerification() {
        String expectedPassword = "secretPassword123";

        // Without password configured, anything passes
        assertTrue(WebServer.isAuthorized(null, null));
        assertTrue(WebServer.isAuthorized(null, ""));
        assertTrue(WebServer.isAuthorized("Basic invalid", ""));

        // With password configured, no header or non-basic header fails
        assertFalse(WebServer.isAuthorized(null, expectedPassword));
        assertFalse(WebServer.isAuthorized("", expectedPassword));
        assertFalse(WebServer.isAuthorized("Bearer token123", expectedPassword));

        // Valid credentials format "user:password"
        String authHeaderWithUser = "Basic " + Base64.getEncoder().encodeToString(
                ("admin:" + expectedPassword).getBytes(StandardCharsets.UTF_8));
        assertTrue(WebServer.isAuthorized(authHeaderWithUser, expectedPassword));

        // Valid credentials format ":password"
        String authHeaderWithoutUser = "Basic " + Base64.getEncoder().encodeToString(
                (":" + expectedPassword).getBytes(StandardCharsets.UTF_8));
        assertTrue(WebServer.isAuthorized(authHeaderWithoutUser, expectedPassword));

        // Valid credentials format "password"
        String authHeaderOnlyPassword = "Basic " + Base64.getEncoder().encodeToString(
                expectedPassword.getBytes(StandardCharsets.UTF_8));
        assertTrue(WebServer.isAuthorized(authHeaderOnlyPassword, expectedPassword));

        // Wrong password fails
        String authHeaderWrong = "Basic " + Base64.getEncoder().encodeToString(
                "admin:wrongPassword".getBytes(StandardCharsets.UTF_8));
        assertFalse(WebServer.isAuthorized(authHeaderWrong, expectedPassword));
    }

    @Test
    public void testSslKeystoreLoading() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream is = new FileInputStream("src/main/res/raw/keystore.p12")) {
            keyStore.load(is, "webkeyboard".toCharArray());
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, "webkeyboard".toCharArray());

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), null, null);

        SSLServerSocketFactory ssf = sslContext.getServerSocketFactory();
        assertNotNull(ssf);

        try (ServerSocket serverSocket = ssf.createServerSocket(0)) {
            assertTrue(serverSocket instanceof SSLServerSocket);
            assertTrue(serverSocket.getLocalPort() > 0);
        }
    }
}
