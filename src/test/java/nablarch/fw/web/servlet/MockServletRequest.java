package nablarch.fw.web.servlet;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.*;

/**
 * @author Kiyohito Itoh
 */
@SuppressWarnings("rawtypes")
public class MockServletRequest implements HttpServletRequest {

    private HttpSession session;
    private StringBuffer requestUrl;
    private int serverPort;
    private String method = "GET";
    private String remoteAddr;
    private String remoteHost;
    private String characterEncoding;
    private String queryString;
    
    private Map<String, String[]> parameterMap = new HashMap<String, String[]>();
    private Map<String, Object> attributesMap = new HashMap<String, Object>();
    {
        attributesMap.put("nablarch_params", parameterMap);
    }
    public Map<String, String[]> getParams() {
        return parameterMap;
    }
    
    public Map<String, Object> getAttributesMap() {
        return attributesMap;
    }
    
    private String contextPath = "/";
    
    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }
    
    /**
     * {@inheritDoc}
     */
    public Object getAttribute(String arg0) {
        return attributesMap.get(arg0);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Enumeration getAttributeNames() {
        return new Vector(attributesMap.keySet()).elements();
    }

    /**
     * {@inheritDoc}
     */
    public String getCharacterEncoding() {
        return characterEncoding;
    }

    /**
     * {@inheritDoc}
     */
    public int getContentLength() {
        return contentLength;
    }

    @Override
    public long getContentLengthLong() {
        return 0;
    }

    private int contentLength = -1;

    public void setContentLength(int contentLength) {
        this.contentLength = contentLength;
    }

    /**
     * {@inheritDoc}
     */
    public String getContentType() {
        return contentType;
    }

    private String contentType = "";

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * {@inheritDoc}
     */
    public ServletInputStream getInputStream() throws IOException {
        if (inputStream == null) {
            throw new UnsupportedOperationException();
        }
        return inputStream;
    }

    private ServletInputStream inputStream = null;

    public void setInputStream(final InputStream in) {
        this.inputStream = new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return false;
            }

            @Override
            public boolean isReady() {
                return false;
            }

            @Override
            public void setReadListener(ReadListener readListener) {

            }

            @Override
            public int read() throws IOException {
                return in.read();
            }
            @Override
            public int available() throws IOException {
                return in.available();
            }
        };
    }
    /**
     * {@inheritDoc}
     */
    public String getLocalAddr() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public String getLocalName() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public int getLocalPort() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServletContext getServletContext() {
        return null;
    }

    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        return null;
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
        return null;
    }

    @Override
    public boolean isAsyncStarted() {
        return false;
    }

    @Override
    public boolean isAsyncSupported() {
        return false;
    }

    @Override
    public AsyncContext getAsyncContext() {
        return null;
    }

    @Override
    public DispatcherType getDispatcherType() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public Locale getLocale() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public Enumeration getLocales() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public String getParameter(String arg0) {
        if (!parameterMap.containsKey(arg0)) {
            return null;
        }
        return parameterMap.get(arg0)[0];
    }

    /**
     * {@inheritDoc}
     */
    public Map getParameterMap() {
        return parameterMap;
    }

    /**
     * {@inheritDoc}
     */
    public Enumeration getParameterNames() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public String[] getParameterValues(String arg0) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public String getProtocol() {
        return "HTTP/1.1";
    }

    /**
     * {@inheritDoc}
     */
    public BufferedReader getReader() throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * @deprecated
     */
    public String getRealPath(String arg0) {
        throw new UnsupportedOperationException();
    }

    public void setRemoteAddr(String remoteAddr) {
        this.remoteAddr = remoteAddr;
    }
    
    /**
     * {@inheritDoc}
     */
    public String getRemoteAddr() {
        return remoteAddr;
    }

    public void setRemoteHost(String remoteHost) {
        this.remoteHost = remoteHost;
    }
    
    /**
     * {@inheritDoc}
     */
    public String getRemoteHost() {
        return remoteHost;
    }

    /**
     * {@inheritDoc}
     */
    public int getRemotePort() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public RequestDispatcher getRequestDispatcher(String arg0) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public String getScheme() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public String getServerName() {
        throw new UnsupportedOperationException();
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }
    
    /**
     * {@inheritDoc}
     */
    public int getServerPort() {
        return serverPort;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isSecure() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public void removeAttribute(String key) {
        attributesMap.remove(key);
    }

    /**
     * {@inheritDoc}
     */
    public void setAttribute(String arg0, Object arg1) {
        attributesMap.put(arg0, arg1);
    }

    /**
     * {@inheritDoc}
     */
    public void setCharacterEncoding(String arg0) throws UnsupportedEncodingException {
        this.characterEncoding = arg0;
    }

    /**
     * {@inheritDoc}
     */
    public String getAuthType() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public String getContextPath() {
        return contextPath;
    }

    /**
     * {@inheritDoc}
     */
    public Cookie[] getCookies() {
        return cookies;
    }
    
    private Cookie[] cookies;
    
    public void setCookies(Cookie... cookies) {
        this.cookies = cookies;
    }

    /**
     * {@inheritDoc}
     */
    public long getDateHeader(String arg0) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public String getHeader(String arg0) {
        return headers.get(arg0);
    }
    
    public void addHeader(String name, String value) {
        headers.put(name, value);
    }
    
    private Map<String, String> headers = new HashMap<String, String>();
    
    /**
     * {@inheritDoc}
     */
    public Enumeration getHeaderNames() {
        return new Vector<String>(headers.keySet()).elements();
    }

    /**
     * {@inheritDoc}
     */
    public Enumeration getHeaders(String arg0) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public int getIntHeader(String arg0) {
        throw new UnsupportedOperationException();
    }

    public void setMethod(String method) {
        this.method = method;
    }
    
    /**
     * {@inheritDoc}
     */
    public String getMethod() {
        return method;
    }

    /**
     * {@inheritDoc}
     */
    public String getPathInfo() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public String getPathTranslated() {
        throw new UnsupportedOperationException();
    }

    public void setQueryString(String queryString) {
        this.queryString = queryString;
    }

    /**
     * {@inheritDoc}
     */
    public String getQueryString() {
        return queryString;
    }

    /**
     * {@inheritDoc}
     */
    public String getRemoteUser() {
        throw new UnsupportedOperationException();
    }

    private String uri = "/";
    public void setRequestURI(String uri) {
        this.uri = uri;
    }
    /**
     * {@inheritDoc}
     */
    public String getRequestURI() {
        return uri;
    }

    public void setRequestUrl(String requestUrl) {
        this.requestUrl = new StringBuffer(requestUrl);
    }
    
    /**
     * {@inheritDoc}
     */
    public StringBuffer getRequestURL() {
        return requestUrl;
    }

    /**
     * {@inheritDoc}
     */
    public String getRequestedSessionId() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public String getServletPath() {
        throw new UnsupportedOperationException();
    }

    public void setSession(HttpSession session) {
        this.session = session;
    }
    
    /**
     * {@inheritDoc}
     */
    public HttpSession getSession() {
        return session;
    }

    @Override
    public String changeSessionId() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public HttpSession getSession(boolean arg0) {
        return session;
    }

    /**
     * {@inheritDoc}
     */
    public Principal getUserPrincipal() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isRequestedSessionIdFromCookie() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isRequestedSessionIdFromURL() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * @deprecated
     */
    public boolean isRequestedSessionIdFromUrl() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean authenticate(HttpServletResponse httpServletResponse) throws IOException, ServletException {
        return false;
    }

    @Override
    public void login(String s, String s1) throws ServletException {

    }

    @Override
    public void logout() throws ServletException {

    }

    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        return null;
    }

    @Override
    public Part getPart(String s) throws IOException, ServletException {
        return null;
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> aClass) throws IOException, ServletException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isRequestedSessionIdValid() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isUserInRole(String arg0) {
        throw new UnsupportedOperationException();
    }
}
