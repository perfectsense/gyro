package beam;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.psddev.dari.util.IoUtils;
import com.psddev.dari.util.StringUtils;

public abstract class BeamStorage {

    /**
     * @param path Can't be {@code null}.
     * @return May be {@code null}.
     */
    public abstract InputStream get(String path) throws IOException;

    public abstract InputStream get(String path, String region) throws IOException;

    /**
     * @param path    Can't be {@code null}.
     * @param content If {@code null}, removes the entry from this storage.
     */
    public abstract void put(String region, String path, InputStream content, String contentType, long length) throws IOException;

    /**
     * Returns the content associated with the given {@code path} as a string.
     *
     * @param path Can't be {@code null}.
     * @return May be {@code null}.
     */
    public String getString(String path) throws IOException {
        return getString(path, null);
    }

    public String getString(String path, String region) throws IOException {
        InputStream input = get(path, region);

        return input != null ? IoUtils.toString(input, StringUtils.UTF_8) : null;
    }

    /**
     * Puts the given {@code content} string into the given {@code path}.
     *
     * @param path    Can't be {@code null}.
     * @param content If {@code null}, removes the entry from this storage.
     */
    public void putString(String path, String content) throws IOException {
        putString(null, path, content);
    }

    public void putString(String region, String path, String content) throws IOException {
        if (content != null) {
            byte[] contentBytes = content.getBytes(StringUtils.UTF_8);
            put(region, path, new ByteArrayInputStream(contentBytes), "text/plain", contentBytes.length);

        } else {
            put(region, path, null, "text/plain", 0);
        }
    }

    public abstract boolean doesExist(String region);
}