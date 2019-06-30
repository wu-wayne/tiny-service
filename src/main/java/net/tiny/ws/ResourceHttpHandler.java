package net.tiny.ws;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;

import net.tiny.ws.cache.FileContentCache;

/**
 * HTTP (HyperText Transfer Protocol) Handler
 * @see http://www.tohoho-web.com/ex/http.htm
 */
public class ResourceHttpHandler extends BaseWebService {

    private Map<String, String> resources = null;
    private FileContentCache contentCache = null;
    private List<String> paths = new ArrayList<>();
    private int cacheSize = -1;
    private long maxAge = 86400L; //1 day
    private String serverName = DEFALUT_SERVER_NAME;

    public WebServiceHandler setPaths(List<String> paths) {
        this.paths = paths;
        return this;
    }

    @Override
    protected boolean doGetOnly() {
        return true;
    }

    @Override
    protected void execute(HTTP_METHOD method, HttpExchange he) throws IOException {
        // Go GET method only
        final RequestHelper request = HttpHandlerHelper.getRequestHelper(he);
        final String uri = he.getRequestURI().getPath();
        final String context = uri.substring(1, uri.indexOf("/", 1));
        final String realPath = mapping(context) + uri.substring(uri.indexOf("/", 1));

        final File doc = new File(realPath);
        byte[] buffer;
        int statCode = HttpURLConnection.HTTP_OK;

        final ResponseHeaderHelper header = HttpHandlerHelper.getHeaderHelper(he);
        if (!doc.exists() || !doc.isFile()) {
            header.setContentType(MIME_TYPE.HTML);
            buffer = NOT_FOUND;
            statCode = HttpURLConnection.HTTP_NOT_FOUND;
        } else {
            try {
                if (request.isNotModified(doc)) {
                    buffer = new byte[0];
                    header.set("Server", serverName);
                    header.set("Connection", "Keep-Alive");
                    statCode = HttpURLConnection.HTTP_NOT_MODIFIED;
                } else {
                    header.setContentType(doc);
                    header.setLastModified(doc);
                    header.set("Server", serverName);
                    header.set("Access-Control-Allow-Origin", "*");
                    header.set("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept");
                    header.set("Access-Control-Allow-Methods", getAllowedMethods());
                    header.set("Connection", "Keep-Alive");
                    header.set("Keep-Alive", "timeout=10, max=1000");
                    header.set("Cache-Control", "max-age=" + maxAge); //"max-age=0" 86400:1 day

                    buffer = getCacheableContents(doc);
                    statCode = HttpURLConnection.HTTP_OK;
                }
            } catch (IOException e) {
                header.setContentType(MIME_TYPE.HTML);
                buffer = SERVER_ERROR;
                statCode = HttpURLConnection.HTTP_INTERNAL_ERROR;
            }
        }
        if (buffer.length > 0) {
            header.setContentLength(buffer.length);
            he.sendResponseHeaders(statCode, buffer.length);
            he.getResponseBody().write(buffer);
        } else {
            he.sendResponseHeaders(statCode, -1);
        }
    }

    private byte[] getCacheableContents(File file) throws IOException {
        if(contentCache == null && cacheSize > 0) {
            // Cache max files
            contentCache = new FileContentCache(cacheSize);
        }
        if (contentCache != null) {
            return contentCache.get(file.getAbsolutePath());
        } else {
            return Files.readAllBytes(file.toPath());
        }
    }

    private String mapping(String context) {
        if (resources == null) {
            resources = new HashMap<>();
            for (String res : paths) {
                String[] array = res.split(":");
                if (array.length > 1) {
                    resources.put(array[0], array[1]);
                }
            }
        }
        return resources.get(context);
    }

}
