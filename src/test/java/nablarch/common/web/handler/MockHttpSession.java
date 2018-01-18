package nablarch.common.web.handler;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.servlet.ServletContext;


/**
 * @author Kiyohito Itoh
 */
public class MockHttpSession implements javax.servlet.http.HttpSession {
    
    private String id;
    private ServletContext servletContext;
    private int maxInactiveInterval;

    private Map<String, Object> map = new HashMap<String, Object>();
    
    /**
     * {@inheritDoc}
     */
    public Object getAttribute(String arg0) {
        return map.get(arg0);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Enumeration getAttributeNames() {
        return new Vector(map.keySet()).elements();
    }

    /**
     * {@inheritDoc}
     */
    public long getCreationTime() {
        throw new UnsupportedOperationException();
    }

    public void setId(String id) {
        this.id = id;
    }
    
    /**
     * {@inheritDoc}
     */
    public String getId() {
        return id;
    }

    /**
     * {@inheritDoc}
     */
    public long getLastAccessedTime() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public int getMaxInactiveInterval() {
        return maxInactiveInterval;
    }

    /**
     * {@inheritDoc}
     */
    public ServletContext getServletContext() {
        return servletContext;
    }

    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    /**
     * {@inheritDoc}
     * @deprecated
     */
    public javax.servlet.http.HttpSessionContext getSessionContext() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * @deprecated
     */
    public Object getValue(String arg0) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * @deprecated
     */
    public String[] getValueNames() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public void invalidate() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isNew() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * @deprecated
     */
    public void putValue(String arg0, Object arg1) {
        throw new UnsupportedOperationException();
        
    }

    /**
     * {@inheritDoc}
     */
    public void removeAttribute(String arg0) {
        map.remove(arg0);
    }

    /**
     * {@inheritDoc}
     * @deprecated
     */
    public void removeValue(String arg0) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public void setAttribute(String arg0, Object arg1) {
        map.put(arg0, arg1);
    }

    /**
     * {@inheritDoc}
     */
    public void setMaxInactiveInterval(int maxInactiveInterval) {
        this.maxInactiveInterval = maxInactiveInterval;
    }
}
