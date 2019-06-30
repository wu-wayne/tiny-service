package net.tiny.ws;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

public abstract class BaseWebService implements WebServiceHandler {

    protected static final Logger LOGGER = Logger.getLogger(BaseWebService.class.getName());

    private static final String DEFAULT_ALLOWED_METHODS  = "GET, POST, PUT, DELETE, OPTIONS";
    private static final String GET_ONLY_ALLOWED_METHODS = "GET, OPTIONS";

    abstract protected void execute(HTTP_METHOD method, HttpExchange he) throws IOException;

    protected String path;
    protected List<Filter> filters = new ArrayList<>();
    protected Authenticator auth = null;

    @Override
    public void handle(HttpExchange he) throws IOException {
        try {
            final Headers headers = he.getResponseHeaders();
            final String allowedMethods = getAllowedMethods();
            final HTTP_METHOD method = HTTP_METHOD.valueOf(he.getRequestMethod());
            switch (method) {
            case GET:
            case POST:
            case PUT:
            case DELETE:
                if (isAllowedMethod(method)) {
                    execute(method, he);
                } else {
                    headers.set(HEADER_ALLOW, allowedMethods);
                    he.sendResponseHeaders(HttpURLConnection.HTTP_BAD_METHOD, NO_RESPONSE_LENGTH);
                }
                break;
            case OPTIONS:
                headers.set(HEADER_ALLOW, allowedMethods);
                he.sendResponseHeaders(HttpURLConnection.HTTP_OK, NO_RESPONSE_LENGTH);
                break;
            default:
                headers.set(HEADER_ALLOW, allowedMethods);
                he.sendResponseHeaders(HttpURLConnection.HTTP_BAD_METHOD, NO_RESPONSE_LENGTH);
                break;
            }
        } catch (RuntimeException | IOException ex) {
            he.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, NO_RESPONSE_LENGTH);
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
        } finally {
            he.close();
        }
    }

    protected boolean doGetOnly() {
        return false;
    }
    protected String getAllowedMethods() {
        return doGetOnly() ? GET_ONLY_ALLOWED_METHODS : DEFAULT_ALLOWED_METHODS;
    }

    protected boolean isAllowedMethod(HTTP_METHOD method) {
        return getAllowedMethods().contains(method.name());
    }

    @Override
    public String path() {
        return path;
    }

    @Override
    public WebServiceHandler path(String path) {
        this.path = path;
        return this;
    }
    @Override
    public boolean hasFilters() {
        return !filters.isEmpty();
    }
    @Override
    public List<Filter> getFilters() {
        return filters;
    }

    @Override
    public WebServiceHandler filters(List<Filter> filters) {
        this.filters = filters;
        return this;
    }

    @Override
    public WebServiceHandler filter(Filter filter) {
        if (!filters.contains(filter)) {
            filters.add(filter);
        }
        return this;
    }

    @Override
    public boolean isAuth() {
        return auth != null;
    }

    @Override
    public Authenticator getAuth() {
        return auth;
    }

    @Override
    public WebServiceHandler auth(Authenticator auth) {
        this.auth = auth;
        return this;
    }

    @Override
    public <T> T lookup(Class<T> type) {
        return null; //TODO
    }

    @Override
    public <T> T lookup(String name, Class<T> type) {
        return null; //TODO
    }
}
