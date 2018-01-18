package nablarch.common.web.handler;

import nablarch.core.util.FileUtil;
import nablarch.fw.web.servlet.NablarchHttpServletRequestWrapper;
import nablarch.test.support.web.servlet.MockServletContext;
import nablarch.test.support.web.servlet.MockServletRequest;
import nablarch.test.support.web.servlet.MockServletResponse;

import javax.el.ELContext;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Kiyohito Itoh
 */
public class MockPageContext extends PageContext {

    private JspWriter w = new MockJspWriter();

    private MockServletRequest request;
    {
        request = new MockServletRequest();
        request.setContextPath(WebTestUtil.CONTEXT_PATH);
        request.setRequestURI(WebTestUtil.CONTEXT_PATH);
        MockHttpSession session = new MockHttpSession();
        session.setServletContext(new MockServletContext() {
            @Override
            public URL getResource(String arg0) throws MalformedURLException {
                return FileUtil.getResourceURL("classpath:" + arg0);
            }
        });
        request.setSession(session);
    }

    private Map<Integer, Map<String, Object>> attributes = new HashMap<Integer, Map<String, Object>>();
    {
        attributes.put(PAGE_SCOPE, new HashMap<String, Object>());
        attributes.put(REQUEST_SCOPE, request.getAttributesMap());
        attributes.put(SESSION_SCOPE, new HashMap<String, Object>());
        attributes.put(APPLICATION_SCOPE, new HashMap<String, Object>());
    }

    private MockServletResponse response = new MockServletResponse();
    private String forwardPath = null;
    private String includePath = null;

    private final NablarchHttpServletRequestWrapper requestWrapper = new NablarchHttpServletRequestWrapper(request);

    private final boolean useRequestWrapper;

    public MockPageContext() {
        this(false);
    }

    public MockPageContext(boolean useRequestWrapper) {
        this.useRequestWrapper = useRequestWrapper;
    }

    public HttpSession getNativeSession() {
        if (requestWrapper == null) {
            return request.getSession();
        }
        NablarchHttpServletRequestWrapper.HttpSessionWrapper sessionWrapper = requestWrapper.getSession(false);
        return sessionWrapper == null ? null : sessionWrapper.getDelegate();
    }

    public String getForwardPath() {
        return forwardPath;
    }

    public String getIncludePath() {
        return includePath;
    }

    public MockServletRequest getMockReq() {
        return request;
    }

    public MockServletResponse getMockRes() {
        return response;
    }

    public Map<String, Object> getAttributes(int scope) {
        return attributes.get(scope);
    }

    public void forward(String relativeUrlPath) throws ServletException, IOException {
        this.forwardPath = relativeUrlPath;
    }

    public Exception getException() {
        throw new UnsupportedOperationException();
    }

    public Object getPage() {
        throw new UnsupportedOperationException();
    }

    public ServletRequest getRequest() {
        return useRequestWrapper ? requestWrapper : request;
    }

    public ServletResponse getResponse() {
        return response;
    }

    public ServletConfig getServletConfig() {
        throw new UnsupportedOperationException();
    }

    public ServletContext getServletContext() {
        throw new UnsupportedOperationException();
    }

    public HttpSession getSession() {
        return request.getSession();
    }

    public void handlePageException(Exception e) throws ServletException, IOException {
        throw new UnsupportedOperationException();
    }

    public void handlePageException(Throwable t) throws ServletException, IOException {
        throw new UnsupportedOperationException();
    }

    public void include(String relativeUrlPath) throws ServletException, IOException {
        include(relativeUrlPath, true);
    }

    public void include(String relativeUrlPath, boolean flush) throws ServletException, IOException {
        this.includePath = relativeUrlPath;
    }

    public void initialize(Servlet servlet, ServletRequest request, ServletResponse response, String errorPageURL, boolean needsSession, int bufferSize, boolean autoFlush)
            throws IOException, IllegalStateException, IllegalArgumentException {
        throw new UnsupportedOperationException();
    }

    public void release() {
        throw new UnsupportedOperationException();
    }

    public Object findAttribute(String name) {
        throw new UnsupportedOperationException();
    }

    public Object getAttribute(String name) {
        return getAttributes(PAGE_SCOPE).get(name);
    }

    public Object getAttribute(String name, int scope) {
        Map<String, Object> attributes = getAttributes(scope);
        return attributes != null ? attributes.get(name) : null;
    }

    public Enumeration<String> getAttributeNamesInScope(int scope) {
        throw new UnsupportedOperationException();
    }

    public int getAttributesScope(String name) {
        throw new UnsupportedOperationException();
    }

    public ELContext getELContext() {
        throw new UnsupportedOperationException();
    }
    /** @deprecated */
    public javax.servlet.jsp.el.ExpressionEvaluator getExpressionEvaluator() {
        throw new UnsupportedOperationException();
    }

    public void setOut(JspWriter w) {
        this.w = w;
    }

    public JspWriter getOut() {
        return w;
    }
    /** @deprecated */
    public javax.servlet.jsp.el.VariableResolver getVariableResolver() {
        throw new UnsupportedOperationException();
    }

    public void removeAttribute(String name) {
        throw new UnsupportedOperationException();
    }

    public void removeAttribute(String name, int scope) {
        getAttributes(scope).remove(name);
    }

    public void setAttribute(String name, Object value) {
        getAttributes(PAGE_SCOPE).put(name, value);
    }

    public void setAttribute(String name, Object value, int scope) {
        getAttributes(scope).put(name, value);
    }

    public static class MockJspWriter extends JspWriter {

        private StringBuilder sb = new StringBuilder();

        public MockJspWriter() {
            super(512, true);
        }

        public String getOutput() {
            return sb.toString();
        }

        public void clearOutput() {
            sb = new StringBuilder();
        }

        public void clear() throws IOException {
            throw new UnsupportedOperationException();
        }

        public void clearBuffer() throws IOException {
            throw new UnsupportedOperationException();
        }

        public void close() throws IOException {
            throw new UnsupportedOperationException();
        }

        public void flush() throws IOException {
            throw new UnsupportedOperationException();
        }

        public int getRemaining() {
            throw new UnsupportedOperationException();
        }

        public void newLine() throws IOException {
            throw new UnsupportedOperationException();
        }

        public void print(boolean b) throws IOException {
            throw new UnsupportedOperationException();
        }

        public void print(char c) throws IOException {
            throw new UnsupportedOperationException();
        }

        public void print(int i) throws IOException {
            throw new UnsupportedOperationException();
        }

        public void print(long l) throws IOException {
            throw new UnsupportedOperationException();
        }

        public void print(float f) throws IOException {
            throw new UnsupportedOperationException();
        }

        public void print(double d) throws IOException {
            throw new UnsupportedOperationException();
        }

        public void print(char[] c) throws IOException {
            throw new UnsupportedOperationException();
        }

        public void print(String s) throws IOException {
            sb.append(s);
        }

        public void print(Object o) throws IOException {
            throw new UnsupportedOperationException();
        }

        public void println() throws IOException {
            throw new UnsupportedOperationException();
        }

        public void println(boolean b) throws IOException {
            throw new UnsupportedOperationException();
        }

        public void println(char c) throws IOException {
            throw new UnsupportedOperationException();
        }

        public void println(int i) throws IOException {
            throw new UnsupportedOperationException();
        }

        public void println(long l) throws IOException {
            throw new UnsupportedOperationException();
        }

        public void println(float f) throws IOException {
            throw new UnsupportedOperationException();
        }

        public void println(double d) throws IOException {
            throw new UnsupportedOperationException();
        }

        public void println(char[] c) throws IOException {
            throw new UnsupportedOperationException();
        }

        public void println(String s) throws IOException {
            throw new UnsupportedOperationException();
        }

        public void println(Object o) throws IOException {
            throw new UnsupportedOperationException();
        }

        public void write(char[] cbuf, int off, int len) throws IOException {
            throw new UnsupportedOperationException();
        }
    }
}
