package net.tiny.ws.client;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import net.tiny.config.JsonParser;
import net.tiny.ws.Callback;

public class SimpleClient {

    public static final String MIME_TYPE_JSON  = "application/json;charset=utf-8";
    public static final String USER_AGENT      = "RestClient Java " + System.getProperty("java.version");
    public static final String BROWSER_AGENT   = "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1)";
    public static final String ACCEPT_MEDIA_TYPE = "application/json, text/*, image/*, audio/*, */*";

    private final Builder builder;
    private int status = -1;
    private byte[] contents = new byte[0];
    private Map<String, List<String>> headers = new HashMap<>();
    private HttpURLConnection connection = null;

    private SimpleClient(Builder builder) {
        this.builder = builder;
    }

    public int getStatus() {
        if(status == -1) {
            throw new IllegalStateException("Not yet connect to server.");
        }
        return status;
    }

    public byte[] getContents() {
        return getContents(false);
    }

    public byte[] getContents(boolean clone) {
        if(contents.length > 0 && clone) {
            byte[] buffer = new byte[contents.length];
            System.arraycopy(contents, 0, buffer, 0, contents.length);
            return buffer;
        }
        return contents;
    }

    public List<String> getHeaders(String name) {
        if (headers.containsKey(name)) {
            return headers.get(name);
        }
        // Capitalization sensitive
        for (String key : headers.keySet()) {
            if (name.equalsIgnoreCase(key))
                return headers.get(key);
        }
        return null;
    }

    public String getHeader(String name) {
        List<String> values = getHeaders(name);
        if (null != values)
            return values.get(0);
        return null;
    }

    public void close() {
        if(connection != null) {
            connection.disconnect();
            connection = null;
        }
    }

    public RequestBuilder request() {
        return request(null);
    }

    public RequestBuilder request(URL url) {
        return new RequestBuilder(this, url);
    }

    public <T> T doGet(String url, Class<T> type) throws IOException {
        byte[] buffer = new RequestBuilder(this, new URL(url)).doGet();
        if(null == buffer)
            return null;
        if (String.class.equals(type)) {
            return type.cast(new String(buffer));
        }
        return JsonParser.unmarshal(new String(buffer), type);
    }

    public String doGet(String url) throws IOException {
        byte[] buffer = new RequestBuilder(this, new URL(url)).doGet();
        if(null == buffer)
            return null;
        return new String(buffer);
    }

    public byte[] doGet(URL url, Consumer<Callback<SimpleClient>> consumer) throws IOException {
        return new RequestBuilder(this, url).doGet(consumer);
    }

    protected void doGet(URL url, Map<String, List<String>> requestHeaders, Consumer<Callback<SimpleClient>> consumer) throws IOException {
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        for (String name : builder.headers.keySet()) {
            connection.setRequestProperty(name, builder.headers.get(name));
        }
        connection.setInstanceFollowRedirects(builder.redirects);

        if (requestHeaders != null && !requestHeaders.isEmpty()) {
            for (String name : requestHeaders.keySet()) {
                List<String> values = requestHeaders.get(name);
                for (String value : values) {
                    connection.setRequestProperty(name, value);
                }
            }
        }

        headers.clear();
        status = connection.getResponseCode();
        final String msg = connection.getResponseMessage();
        headers.putAll(connection.getHeaderFields());
        boolean ok = (status >= 200 && status < 400);
        setContents(connection);

        if (!builder.keepAlive) {
            connection.disconnect();
        }
        if (null != consumer) {
            if (ok) {
                consumer.accept(Callback.succeed(this));
            } else {
                consumer.accept(Callback.failed(String.format("%d %s", status, msg)));
            }
        }
    }

    protected void doPost(URL url, Map<String, List<String>> requestHeaders, Consumer<Callback<SimpleClient>> consumer) throws IOException {
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        for (String name : builder.headers.keySet()) {
            connection.setRequestProperty(name, builder.headers.get(name));
        }
        connection.setInstanceFollowRedirects(builder.redirects);
        if (requestHeaders != null && !requestHeaders.isEmpty()) {
            for (String name : requestHeaders.keySet()) {
                List<String> values = requestHeaders.get(name);
                for (String value : values) {
                    connection.setRequestProperty(name, value);
                }
            }
        }

        //TODO POST data
        headers.clear();
        status = connection.getResponseCode();
        final String msg = connection.getResponseMessage();
        headers.putAll(connection.getHeaderFields());
        boolean ok = (status >=200 && status <=303);
        setContents(connection);

        if (!builder.keepAlive) {
            connection.disconnect();
        }
        if (null != consumer) {
            if (ok) {
                consumer.accept(Callback.succeed(this));
            } else {
                consumer.accept(Callback.failed(String.format("%d %s", status, msg)));
            }
        }
    }

    private void setContents(HttpURLConnection conn) throws IOException {
        int size = conn.getContentLength();
        boolean has = false;
        if(size > 0) {
            has = true;
        }
        if (!has) {
            has = (null != conn.getContentType());
        }
        if (!has) {
            contents = new byte[0];
            return;
        }
        BufferedInputStream bis = new BufferedInputStream(conn.getInputStream());
        contents = getContent(size, bis);
        bis.close();
    }

    private byte[] getContent(int contentLength, InputStream in) throws IOException {
        ByteArrayOutputStream contentBuffer = new ByteArrayOutputStream();
        byte readBuf[] = new byte[contentLength];
        int readLen = 0;
        while((readLen = in.read(readBuf)) > 0 ) {
            contentBuffer.write(readBuf, 0, readLen);
            contentBuffer.flush();
        }
        return contentBuffer.toByteArray();
    }

    public static class RequestBuilder {
        SimpleClient  client;
        boolean ssl = false;
        String protocol = "http";
        String host = "localhost";
        String path = "/";
        String query = null;
        int port = 80;
        Consumer<Callback<SimpleClient>> consumer;
        Map<String, List<String>> headers = new HashMap<>();

        public RequestBuilder(SimpleClient c, URL url) {
            client = c;
            if(url != null) {
                protocol = url.getProtocol();
                ssl = protocol.endsWith("s");
                host = url.getHost();
                port = url.getPort();
                path = url.getPath();
                query = url.getQuery();
            }
        }

        public RequestBuilder host(String h) {
            host = h;
            return this;
        }
        public RequestBuilder port(int p) {
            port = p;
            return this;
        }
        public RequestBuilder path(String p) {
            path = p;
            return this;
        }
        public RequestBuilder query(String q) {
            query = q;
            return this;
        }
        public RequestBuilder header(String name, String value) {
            List<String> values = headers.get(name);
            if(null == values) {
                values = new ArrayList<>();
                values.add(value);
                headers.put(name, values);
            } else if (!values.contains(value)) {
                values.add(value);
            }
            return this;
        }

        public byte[] doGet() throws IOException {
            return doGet(null);
        }

        public byte[] doGet(Consumer<Callback<SimpleClient>> consumer) throws IOException {
            String url;
            if (query == null) {
                url = String.format("%s://%s:%d%s", protocol, host, port, path);
            } else {
                url = String.format("%s://%s:%d%s?%s", protocol, host, port, path, query);
            }
            client.doGet(new URL(url), headers, consumer);
            return client.getContents(true);
        }
    }

    public static class Builder {
        Map<String, String> headers = new HashMap<>();
        boolean redirects = false;
        boolean keepAlive = false;

        public Builder() {
            header("User-Agent", USER_AGENT);
            header("Accept", ACCEPT_MEDIA_TYPE);
        }

        public Builder redirects(boolean enable) {
            redirects = enable;
            return this;
        }


        public Builder userAgent(String ua) {
            header("User-Agent", ua);
            return this;
        }

        public Builder keepAlive(boolean enable) {
            if (enable) {
                header("Connection", "Keep-Alive");
                keepAlive = true;
            }
            return this;
        }

        public Builder accept(String mediaType) {
            header("Accept", mediaType);
            return this;
        }

        public Builder header(String name, String value) {
               headers.put(name, value);
            return this;
        }

        public SimpleClient build() {
            return new SimpleClient(this);
        }
    }
}
