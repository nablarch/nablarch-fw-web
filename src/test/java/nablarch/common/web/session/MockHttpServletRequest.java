package nablarch.common.web.session;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import nablarch.core.util.map.EnumerableIterator;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author tajima
 *
 */
public class MockHttpServletRequest {
    private final Hashtable<String, Object> attributes = new Hashtable<String, Object>();
    private Cookie[] cookies = null;
    private Map<String, String[]> params = new HashMap<String, String[]>();
    private HttpSession session = null;
    public MockHttpServletRequest setCookies(Cookie[] cookies) { this.cookies = cookies; return this;}
    public MockHttpServletRequest setParameterMap(Map<String, String[]> params) { this.params = params; return this;}

    private class MockHttpSession {

        private HttpSession getMockInstance() {
            final HttpSession session = mock(HttpSession.class);
            
            when(session.getAttribute(anyString())).then(context -> sessionContent.get(context.getArgument(0, String.class)));
            
            when(session.getAttributeNames()).then(context -> new EnumerableIterator<>(sessionContent.keySet().iterator()));
            
            doAnswer(context -> sessionContent.put(context.getArgument(0), context.getArgument(1)))
                    .when(session).setAttribute(anyString(), any());
            
            doAnswer(context -> sessionContent.remove(context.getArgument(0, String.class)))
                    .when(session).removeAttribute(anyString());
            
            doAnswer(context -> {
                sessionInvalidateCount++;
                sessionContent.clear();
                MockHttpServletRequest.this.session = null;
                return null;
            }).when(session).invalidate();
            
            return session;
        }
    }

    public static final Map<String, Object> sessionContent = new HashMap<String, Object>();
    public static int sessionInvalidateCount = 0;
    
    public HttpServletRequest getMockInstance() {
        final HttpServletRequest request = mock(HttpServletRequest.class);

        when(request.getRequestURI()).thenReturn("/");
        
        when(request.getContextPath()).thenReturn("");
        
        when(request.getProtocol()).thenReturn("HTTP/1.1");
        
        when(request.getMethod()).thenReturn("GET");
        
        when(request.getParameterMap()).then(context -> this.params);
        
        when(request.getHeaderNames()).thenReturn(new Vector<String>().elements());
        
        when(request.getAttributeNames()).then(context -> attributes.keys());
        
        when(request.getAttribute(anyString())).then(context -> attributes.get(context.getArgument(0, String.class)));
        doAnswer(context -> {
            attributes.put(context.getArgument(0), context.getArgument(1));
            return null;
        }).when(request).setAttribute(anyString(), any());
        
        doAnswer(context -> attributes.remove(context.getArgument(0, String.class)))
                .when(request).removeAttribute(anyString());
        
        when(request.getCookies()).then(context -> this.cookies);
        
        when(request.getSession(anyBoolean())).then(context -> {
            if (context.getArgument(0, Boolean.class) && session == null) {
                session = new MockHttpSession().getMockInstance();
            }
            return session;
        });
        
        return request;
    }
}
