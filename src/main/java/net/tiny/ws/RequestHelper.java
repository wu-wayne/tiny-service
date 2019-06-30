package net.tiny.ws;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;


/**
 * This class provides an helper to understand the Http request
 * The SUN HttpServer is not a real hhtp server. One thing that it does not do is to
 * automatically serve load request for sub elemnts (i.e file) on a html page.
 * This must be handled by the application itself.
 *
 * This class will break up the incomming http request so make it easier to identify
 * request for load of sub elements i.e. from a page
 *
 * Worth to know
 * - when the attribute "Referer" is present in the requestHeader there is a request for load of a sub element,
 *   the "Referer" the refers to the full path host + uri.
 * - If a client on a top level request  load of an html page there will be no "Referer" attribute in the request header
 *   the referens to the html page is passed as parameter.
 * - In normal case when the client is requesting a service the path will not contain any file (?)
 *
 * The whole thing is a bit messy but at least this is an effort to make the analyse a bit less complicated.
 *
 */

public final class RequestHelper {

    private static final String REGEX_REFERER_REQUEST = "(\\w+://\\w+:\\d+)(/(\\w+/)*)(\\w+.?\\w*)";
    private static final Pattern REFERER_REQUEST_PATTERN = Pattern.compile(REGEX_REFERER_REQUEST);

    private static final String REGEX_FILE_REQUEST = "/?(\\w+/)*(\\w+.\\w+)$";
    private static final Pattern FILE_REQUEST_PATTERN = Pattern.compile(REGEX_FILE_REQUEST);

    private static final String REGEX_COOKIE_NAME_VALUE = "^(\\w+)=(.*)$";
    private static final Pattern COOKIE_PATTERN = Pattern.compile(REGEX_COOKIE_NAME_VALUE);

    private final HttpExchange httpExchange;
    private final String requestPath;
    private final String requestMethod;
    private final String requestURI;
    private final String referer;
    private String uriParameters;


    public RequestHelper(HttpExchange he) {
        httpExchange  = he;
        requestPath   = httpExchange.getHttpContext().getPath();
        requestMethod = httpExchange.getRequestMethod();
        referer = (String) httpExchange.getRequestHeaders().getFirst("Referer");
        requestURI = httpExchange.getRequestURI().toString();
        setURIParameters(requestURI);
    }

    public String getMethod() {
        return requestMethod;
    }

    public String getURI() {
        return requestURI;
    }

    public String getContextPath() {
        return httpExchange.getHttpContext().getPath();
    }

    public Headers getHeaders() {
        return httpExchange.getRequestHeaders();
    }

    public String getReferer() {
        return referer;
    }

    public HttpPrincipal getPrincipal() {
        return httpExchange.getPrincipal();
    }

    private Date parseDate(String name) {
        String value = getHeaders().getFirst(name);
        return (null == value) ? null : HttpDateFormat.parse(value);
    }

    public boolean isNotModified(File file) {
        long lastModified = file.lastModified();
        Date date = parseDate("If-Modified-Since");
        if(null != date) {
            return lastModified <= date.getTime();
        }

        date = parseDate("If-Unmodified-Since");
        if(null != date) {
            return lastModified > date.getTime();
        }
        return false;
    }

    /**
     * @deprecated
     */
    public String getIfMatch() {
        return getHeaders().getFirst("If-Match");
    }

    /**
     * @deprecated
     */
    public String getIfNoneMatch() {
        return getHeaders().getFirst("If-None-Match");
    }

    public boolean hasParameters() {
        if ((uriParameters == null) || (uriParameters.isEmpty())) {
            return false;
        }
        return true;
    }

    /**
     * This method will return the any parameter after the path
     * e.g path "/test" uri "/test/foo/fie/bar/" uriParameters "foo/fie/bar"
     * "foo" is returned if index 0 is request
     * @param index
     * @return URI parameter
     */
    public String getParameter(int index) {
        if ((uriParameters == null) || (uriParameters.length() == 0)) {
            return "";
        }
        // "foo/fie/bar" --> "foo","fie","bar"
        String[] items = uriParameters.split("/");
        return (index > items.length) ? "" : items[ index ];
    }

    /**
     * This method will return the all parameters after the path
     * e.g path "/test" uri "/test/api?f=file&t=bar"
     * The return request map is "f=file,t=bar"
     * @return Map of path parameters
     */
    public Map<String, List<String>> getParameters() {
        return getURIParameters(httpExchange.getRequestURI());
    }

    public boolean isFileRequest() {
        Matcher matcher = FILE_REQUEST_PATTERN.matcher( uriParameters );
        return matcher.matches();
    }

    public String getRequestedPath() {
        if (isFileRequest()) {
            int i = uriParameters.lastIndexOf("/");
            return (i < 0) ? "" : uriParameters.substring(0,i);
        }
        return null;
    }

    public String getRequestedFilename() {
        if (isFileRequest()) {
           int i = uriParameters.lastIndexOf("/");
           return (i < 0) ? uriParameters : uriParameters.substring(i+1);
        }
        return null;
    }

    public String getRequestedFileExtenstion() {
        if (isFileRequest()) {
           String fnam = getRequestedFilename();
           int i = fnam.lastIndexOf(".");
           if (i < 0) {
               return null;
           }
           if ((i + 1) >= fnam.length()) {
               return null;
           }
           return fnam.substring(i+1);
        }
        return null;
    }

    public boolean isReferensRequest() {
        return (referer != null);
    }

    /**
     * Search and retreive a cookie from a HTTP request context
     * @param key, The cookie name to search for
     * @param pReturnJustValue, return just the cookie value or the name + value i.e. "foo=bar;fie;etc";
     * @return
     */
    public String getCookie(String key, boolean justValue) {
        Iterator<Map.Entry<String, List<String>>> it =
                httpExchange.getRequestHeaders().entrySet().iterator();
        while( it.hasNext()) {
            Map.Entry<String, List<String>> entry = it.next();
            if(entry.getKey().toLowerCase().contentEquals("cookie")){
                String result = getCookieFromSearchString(key, entry.getValue().get(0));
                if(result != null) {
                    if (justValue) {
                        Matcher m = COOKIE_PATTERN.matcher(result);
                        if ((m.matches()) && (m.groupCount() == 2)) {
                            return m.group(2);
                        } else {
                            return result;
                        }
                    }
                }
                return result;
            }
        }
        return null;
    }

    private Map<String, List<String>> getURIParameters(final URI requestUri) {
        final Map<String, List<String>> requestParameters = new LinkedHashMap<>();
        final String requestQuery = requestUri.getRawQuery();
        if (requestQuery != null) {
            final String[] rawRequestParameters = requestQuery.split("[&;]", -1);
            for (final String rawRequestParameter : rawRequestParameters) {
                final String[] requestParameter = rawRequestParameter.split("=", 2);
                final String requestParameterName = decodeUrlComponent(requestParameter[0]);
                requestParameters.putIfAbsent(requestParameterName, new ArrayList<>());
                final String requestParameterValue = requestParameter.length > 1 ? decodeUrlComponent(requestParameter[1]) : null;
                requestParameters.get(requestParameterName).add(requestParameterValue);
            }
        }
        return requestParameters;
    }

    private String decodeUrlComponent(final String urlComponent) {
        try {
            return URLDecoder.decode(urlComponent, StandardCharsets.UTF_8.name());
        } catch (final UnsupportedEncodingException ex) {
            throw new InternalError(ex);
        }
    }

    private String getCookieFromSearchString(String pStartKey, String pWholeCookie) {
        if (pWholeCookie.contains(";")) {
            String data[] = pWholeCookie.split(";");
            for (int i = 0; i < data.length; i++) {
                if (data[i].trim().startsWith(pStartKey)) {
                    return data[i].trim();
                }
            }
        } else if (pWholeCookie.startsWith(pStartKey)) {
            return pWholeCookie;
        }
        return null;
    }

    private void setURIParameters( String uri ) {
        if (referer == null) {
            // easy case no sub item relative path
            int i = uri.indexOf( requestPath );
            if (i >= 0) {
                uriParameters = (uri.length() > requestPath.length()) ? uri.substring( i + requestPath.length() + 1 ) :
                    uri.substring( i + requestPath.length());
            } else {
                uriParameters = "";
            }
        } else {
            Matcher m = REFERER_REQUEST_PATTERN.matcher( referer );
            if (m.matches()) {
                String tRelPath = m.group(2);
                uriParameters = uri.substring(tRelPath.length());
            }
        }
     }

    @Override
    public String toString() {
        String rssid = getCookie("RSSID", true);
        return String.format("method : %s path: %s uri: %s params: %s referer: % RSSID: %s",
                requestMethod,
                requestPath,
                requestURI,
                uriParameters,
                referer,
                rssid);
    }
}
