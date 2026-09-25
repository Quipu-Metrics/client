package org.quipumetrics.core;

import lombok.RequiredArgsConstructor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.zip.GZIPOutputStream;

@RequiredArgsConstructor
final class Submitter {

    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private static final int CONNECT_TIMEOUT_MILLIS = 5_000;
    private static final int READ_TIMEOUT_MILLIS = 10_000;

    private final String baseUrl;

    /**
     * Sends one submission and forgets it.
     * <p>
     * There is deliberately no retry. If every client retried after an outage,
     * recovery would immediately become a second outage. A lost data point is
     * replaced by the next cycle within 30 minutes.
     */
    void send(String platform, String payload) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(baseUrl + platform).openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        connection.setReadTimeout(READ_TIMEOUT_MILLIS);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Content-Encoding", "gzip");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "Quipu/1");
        connection.setDoOutput(true);

        byte[] compressed = gzip(payload);
        connection.setFixedLengthStreamingMode(compressed.length);
        try (OutputStream out = connection.getOutputStream()) {
            out.write(compressed);
        }

        int status = connection.getResponseCode();
        connection.getInputStream().close();
        if (status != HttpURLConnection.HTTP_ACCEPTED)
            throw new IOException("backend answered " + status);
    }

    private static byte[] gzip(String payload) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(bytes)) {
            gzip.write(payload.getBytes(UTF_8));
        }
        return bytes.toByteArray();
    }
}
