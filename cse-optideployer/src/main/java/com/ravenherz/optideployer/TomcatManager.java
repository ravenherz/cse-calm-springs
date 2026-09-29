package com.ravenherz.optideployer;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;

public final class TomcatManager implements ManagerClient {

    private static final int CONNECT_TIMEOUT_MS = 30_000;
    private static final int READ_TIMEOUT_MS = 15 * 60 * 1000;

    private final OptiConfig config;

    public TomcatManager(OptiConfig config) {
        this.config = config;
    }

    static String deployUrl(String managerUrl, String context, Path war) {
        String base = managerUrl;
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (!base.endsWith("/deploy")) {
            base = base + "/deploy";
        }
        return base
                + "?path=" + URLEncoder.encode(context, StandardCharsets.UTF_8)
                + "&update=true&war=" + URLEncoder.encode(war.toUri().toASCIIString(), StandardCharsets.UTF_8);
    }

    @Override
    public String deploy(Path warFile) throws IOException {
        String url = deployUrl(config.managerUrl(), config.context(), warFile);
        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        acceptLoopbackCertificate(connection, url);
        connection.setRequestMethod("GET");
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        String token = Base64.getEncoder().encodeToString(
                (config.username() + ":" + config.password()).getBytes(StandardCharsets.UTF_8));
        connection.setRequestProperty("Authorization", "Basic " + token);
        int status = connection.getResponseCode();
        InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
        String body = "";
        if (stream != null) {
            try (stream) {
                body = new String(stream.readAllBytes(), StandardCharsets.UTF_8).trim();
            }
        }
        if (body.length() > 500) {
            body = body.substring(0, 500);
        }
        if (status != 200 || !body.startsWith("OK")) {
            throw new IOException("Tomcat manager " + status + ": " + body);
        }
        return body;
    }

    static boolean loopback(String host) {
        return "127.0.0.1".equals(host) || "localhost".equalsIgnoreCase(host) || "::1".equals(host);
    }

    /**
     * A loopback manager URL still presents the public certificate. Trust the certificate,
     * and skip the name check, because the connection never leaves this machine.
     */
    static void acceptLoopbackCertificate(HttpURLConnection connection, String url) {
        if (!(connection instanceof HttpsURLConnection https) || !loopback(URI.create(url).getHost())) {
            return;
        }
        https.setSSLSocketFactory(new LoopbackSockets((SSLSocketFactory) SSLSocketFactory.getDefault()));
        HostnameVerifier allow = (name, session) -> true;
        https.setHostnameVerifier(allow);
    }

    private static final class LoopbackSockets extends SSLSocketFactory {

        private final SSLSocketFactory delegate;

        private LoopbackSockets(SSLSocketFactory delegate) {
            this.delegate = delegate;
        }

        private static Socket relax(Socket socket) {
            if (socket instanceof SSLSocket ssl) {
                SSLParameters parameters = ssl.getSSLParameters();
                parameters.setEndpointIdentificationAlgorithm(null);
                ssl.setSSLParameters(parameters);
            }
            return socket;
        }

        @Override
        public String[] getDefaultCipherSuites() {
            return delegate.getDefaultCipherSuites();
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return delegate.getSupportedCipherSuites();
        }

        @Override
        public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
            return relax(delegate.createSocket(socket, host, port, autoClose));
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException {
            return relax(delegate.createSocket(host, port));
        }

        @Override
        public Socket createSocket(String host, int port, InetAddress local, int localPort) throws IOException {
            return relax(delegate.createSocket(host, port, local, localPort));
        }

        @Override
        public Socket createSocket(InetAddress host, int port) throws IOException {
            return relax(delegate.createSocket(host, port));
        }

        @Override
        public Socket createSocket(InetAddress host, int port, InetAddress local, int localPort) throws IOException {
            return relax(delegate.createSocket(host, port, local, localPort));
        }
    }
}
