package nablarch.fw.web.servlet;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MockServletContext implements ServletContext {

    public Object getAttribute(String arg0) {
        throw new UnsupportedOperationException();
    }

    public Enumeration getAttributeNames() {
        throw new UnsupportedOperationException();
    }

    public ServletContext getContext(String arg0) {
        throw new UnsupportedOperationException();
    }

    public String getContextPath() {
        throw new UnsupportedOperationException();
    }

    private Map<String, String> initParams = new HashMap<String, String>();
    
    public Map<String, String> getInitParams() {
        return initParams;
    }
    
    public String getInitParameter(String arg0) {
        return initParams.get(arg0);
    }

    public Enumeration getInitParameterNames() {
        throw new UnsupportedOperationException();
    }

    public int getMajorVersion() {
        throw new UnsupportedOperationException();
    }

    public String getMimeType(String arg0) {
        throw new UnsupportedOperationException();
    }

    public int getMinorVersion() {
        throw new UnsupportedOperationException();
    }

    public RequestDispatcher getNamedDispatcher(String arg0) {
        throw new UnsupportedOperationException();
    }

    public String getRealPath(String arg0) {
        throw new UnsupportedOperationException();
    }

    public RequestDispatcher getRequestDispatcher(String arg0) {
        throw new UnsupportedOperationException();
    }

    public URL getResource(String arg0) throws MalformedURLException {
        throw new UnsupportedOperationException();
    }

    public InputStream getResourceAsStream(String arg0) {
        throw new UnsupportedOperationException();
    }

    public Set getResourcePaths(String arg0) {
        throw new UnsupportedOperationException();
    }

    public String getServerInfo() {
        throw new UnsupportedOperationException();
    }

    public Servlet getServlet(String arg0) throws ServletException {
        throw new UnsupportedOperationException();
    }

    public String getServletContextName() {
        throw new UnsupportedOperationException();
    }

    public Enumeration getServletNames() {
        throw new UnsupportedOperationException();
    }

    public Enumeration getServlets() {
        throw new UnsupportedOperationException();
    }

    public void log(String arg0) {
        throw new UnsupportedOperationException();
    }

    public void log(Exception arg0, String arg1) {
        throw new UnsupportedOperationException();
    }

    public void log(String arg0, Throwable arg1) {
        throw new UnsupportedOperationException();
    }

    public void removeAttribute(String arg0) {
        throw new UnsupportedOperationException();
    }

    public void setAttribute(String arg0, Object arg1) {
        throw new UnsupportedOperationException();
    }
}
