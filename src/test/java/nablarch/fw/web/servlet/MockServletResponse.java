package nablarch.fw.web.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

import nablarch.common.web.WebTestUtil;

/**
 * @author Kiyohito Itoh
 */
public class MockServletResponse implements HttpServletResponse {

    private Map<String, List<String>> map = new HashMap<String, List<String>>();

    @Override
    public String getHeader(String s) {
        return null;
    }

    @Override
    public Collection<String> getHeaders(String s) {
        List<String> list = map.get(s);
        if (list == null) {
            list = new ArrayList<String>();
            map.put(s, list);
        }
        return list;
    }

    @Override
    public Collection<String> getHeaderNames() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public void addCookie(Cookie arg0) {
        cookies.add(arg0);
    }
    
    private List<Cookie> cookies = new ArrayList<Cookie>();
    
    public List<Cookie> getCookies() {
        return cookies;
    }
    
    /**
     * {@inheritDoc}
     */
    public void addDateHeader(String arg0, long arg1) {
        getHeaders(arg0).add(String.valueOf(arg1));
    }

    /**
     * {@inheritDoc}
     */
    public void addHeader(String arg0, String arg1) {
        getHeaders(arg0).add(String.valueOf(arg1));
    }

    /**
     * {@inheritDoc}
     */
    public void addIntHeader(String arg0, int arg1) {
        getHeaders(arg0).add(String.valueOf(arg1));
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsHeader(String arg0) {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public String encodeRedirectURL(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     * @deprecated
     */
    public String encodeRedirectUrl(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String encodeURL(String arg0) {
        return arg0 + WebTestUtil.ENCODE_URL_SUFFIX;
    }

    /**
     * {@inheritDoc}
     * @deprecated
     */
    public String encodeUrl(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public void sendError(int arg0) throws IOException {
        // TODO Auto-generated method stub
        
    }

    /**
     * {@inheritDoc}
     */
    public void sendError(int arg0, String arg1) throws IOException {
        // TODO Auto-generated method stub
        
    }

    /**
     * {@inheritDoc}
     */
    public void sendRedirect(String arg0) throws IOException {
        // TODO Auto-generated method stub
        
    }

    /**
     * {@inheritDoc}
     */
    public void setDateHeader(String arg0, long arg1) {
        getHeaders(arg0).add(String.valueOf(arg1));
    }

    /**
     * {@inheritDoc}
     */
    public void setHeader(String arg0, String arg1) {
        getHeaders(arg0).add(String.valueOf(arg1));
    }

    /**
     * {@inheritDoc}
     */
    public void setIntHeader(String arg0, int arg1) {
        getHeaders(arg0).add(String.valueOf(arg1));
    }

    /**
     * {@inheritDoc}
     */
    public void setStatus(int arg0) {
    }

    /**
     * {@inheritDoc}
     * @deprecated
     */
    public void setStatus(int arg0, String arg1) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public int getStatus() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public void flushBuffer() throws IOException {
        // TODO Auto-generated method stub
        
    }

    /**
     * {@inheritDoc}
     */
    public int getBufferSize() {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public String getCharacterEncoding() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String getContentType() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public Locale getLocale() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public ServletOutputStream getOutputStream() throws IOException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public PrintWriter getWriter() throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isCommitted() {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public void reset() {
        // TODO Auto-generated method stub
        
    }

    /**
     * {@inheritDoc}
     */
    public void resetBuffer() {
        // TODO Auto-generated method stub
        
    }

    /**
     * {@inheritDoc}
     */
    public void setBufferSize(int arg0) {
        // TODO Auto-generated method stub
        
    }

    /**
     * {@inheritDoc}
     */
    public void setCharacterEncoding(String arg0) {
        // TODO Auto-generated method stub
        
    }

    /**
     * {@inheritDoc}
     */
    public void setContentLength(int arg0) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setContentLengthLong(long l) {

    }

    /**
     * {@inheritDoc}
     */
    public void setContentType(String arg0) {
        // TODO Auto-generated method stub
        
    }

    /**
     * {@inheritDoc}
     */
    public void setLocale(Locale arg0) {
        // TODO Auto-generated method stub
        
    }

}
