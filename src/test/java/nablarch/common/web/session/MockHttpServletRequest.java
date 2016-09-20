package nablarch.common.web.session;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import mockit.Mock;
import mockit.MockUp;
import nablarch.core.util.map.EnumerableIterator;

/**
 * @author tajima
 *
 */
public class MockHttpServletRequest extends MockUp<HttpServletRequest> {
    private final Hashtable<String, Object> attributes = new Hashtable<String, Object>();
    private Cookie[] cookies = null;
    private Map<String, String[]> params = new HashMap<String, String[]>();
    private HttpSession session = null;

    @Mock public String getRequestURI() { return "/"; }
    @Mock public String getContextPath() { return ""; }
    @Mock public String getProtocol() { return "HTTP/1.1"; }
    @Mock public String getMethod() { return "GET"; }
    @Mock public Map<String, String[]> getParameterMap() { return this.params; }
    @Mock public Enumeration getHeaderNames() { return new Vector().elements(); }
    @Mock public Enumeration getAttributeNames() { return attributes.keys(); }
    @Mock public Object getAttribute(String name) { return attributes.get(name); }
    @Mock public void setAttribute(String name, Object value) { attributes.put(name, value); }
    @Mock public void removeAttribute(String name) { attributes.remove(name); }
    @Mock public Cookie[] getCookies() { return this.cookies; }
    @Mock public HttpSession getSession(boolean b) {
        if (b && session == null) {
            session = new MockHttpSession().getMockInstance();
        }
        return session;
    }
    public MockHttpServletRequest setCookies(Cookie[] cookies) { this.cookies = cookies; return this;}
    public MockHttpServletRequest setParameterMap(Map<String, String[]> params) { this.params = params; return this;}

    private class MockHttpSession extends MockUp<HttpSession> {
        @Mock public Object getAttribute(String name) { return sessionContent.get(name); }
        @Mock public Enumeration<String> getAttributeNames() { return new EnumerableIterator<String>(sessionContent.keySet().iterator()); }
        @Mock public void setAttribute(String name, Object value) { sessionContent.put(name, value); }
        @Mock public void removeAttribute(String name) { sessionContent.remove(name); }
        @Mock public Object getValue(String name) { return getAttribute(name); }
        @Mock public void invalidate() {
            sessionInvalidateCount++;
            sessionContent.clear();
            session = null;
        }
    }

    public static final Map<String, Object> sessionContent = new HashMap<String, Object>();
    public static int sessionInvalidateCount = 0;
}
