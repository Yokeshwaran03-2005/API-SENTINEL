package com.apisentinel.gateway;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.util.StreamUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Re-readable HttpServletRequest wrapper that caches the request body in memory.
 * Ensures security inspection can read the payload without exhausting the stream
 * for downstream Spring MVC controllers and message converters.
 */
public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

    private static final int MAX_CACHE_SIZE_BYTES = 1024 * 1024; // 1 MB cap to prevent OOM
    private final byte[] cachedBody;
    private final Charset characterEncoding;

    public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);

        String encoding = request.getCharacterEncoding();
        this.characterEncoding = encoding != null ? Charset.forName(encoding) : StandardCharsets.UTF_8;

        InputStream is = request.getInputStream();
        if (is != null) {
            byte[] rawBytes = StreamUtils.copyToByteArray(is);
            if (rawBytes.length > MAX_CACHE_SIZE_BYTES) {
                // Truncate to maximum cache size
                this.cachedBody = new byte[MAX_CACHE_SIZE_BYTES];
                System.arraycopy(rawBytes, 0, this.cachedBody, 0, MAX_CACHE_SIZE_BYTES);
            } else {
                this.cachedBody = rawBytes;
            }
        } else {
            this.cachedBody = new byte[0];
        }
    }

    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(this.cachedBody);

        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return byteArrayInputStream.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                // Synchronous reading; no listener needed
            }

            @Override
            public int read() {
                return byteArrayInputStream.read();
            }
        };
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), this.characterEncoding));
    }

    public byte[] getCachedBody() {
        return this.cachedBody;
    }

    public String getBodyAsString() {
        if (this.cachedBody.length == 0) {
            return "";
        }
        return new String(this.cachedBody, this.characterEncoding);
    }
}
