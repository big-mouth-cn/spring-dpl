package io.github.bigmouthcn.spring.dpl.hotswap;

import io.github.bigmouthcn.spring.dpl.PluginRuntimeException;

import java.io.IOException;
import java.io.InputStream;
import java.net.FileNameMap;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * @author allen
 * @date 2019/6/27
 * @since 1.0.0
 */
public class PluginResourceURLStreamHandler extends URLStreamHandler {

    private static final String DEFAULT_PROTOCOL = "pluginresource:";
    private static final String DEFAULT_CONTENT_TYPE = "text/plain";

    private final PluginClassLoader pluginClassLoader;

    public PluginResourceURLStreamHandler(PluginClassLoader pluginClassLoader) {
        this.pluginClassLoader = pluginClassLoader;
    }

    @Override
    protected URLConnection openConnection(URL u) throws IOException {
        String protocol = u.getProtocol();
        if (!valid(protocol)) {
            throw new PluginRuntimeException("Invalid protocol, expect " + getProtocol() + ", but " + protocol);
        }
        String resource = u.getPath();

        return new URLConnection(u) {
            @Override
            public void connect() throws IOException {
            }

            @Override
            public String getContentType() {
                FileNameMap fileNameMap = URLConnection.getFileNameMap();
                String contentType = fileNameMap.getContentTypeFor(resource);
                if (null == contentType) {
                    contentType = DEFAULT_CONTENT_TYPE;
                }
                return contentType;
            }

            @Override
            public InputStream getInputStream() throws IOException {
                InputStream is = pluginClassLoader.getByteInputStream(resource);
                if (null == is) {
                    throw new IOException("PluginClassLoader.getByteInputStream() returned null for " + resource);
                }
                return is;
            }
        };
    }

    static String getProtocol() {
        return DEFAULT_PROTOCOL;
    }

    private boolean valid(String protocol) {
        String defaultProtocol = getProtocol();
        return protocol.equals(defaultProtocol.substring(0, defaultProtocol.length() - 1));
    }
}
