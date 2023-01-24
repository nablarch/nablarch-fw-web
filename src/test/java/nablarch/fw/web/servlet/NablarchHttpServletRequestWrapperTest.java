package nablarch.fw.web.servlet;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpSession;

import nablarch.common.web.MockHttpSession;
import nablarch.fw.Result;
import org.junit.Test;

/**
 * {@link NablarchHttpServletRequestWrapper} のテストクラス。
 * 
 * @author Toru Nagashima
 */
public class NablarchHttpServletRequestWrapperTest {
    
    /**
     * {@link NablarchHttpServletRequestWrapper#NablarchHttpServletRequestWrapper(jakarta.servlet.http.HttpServletRequest)} のテスト。
     * ラップするべき情報がラップされていることを確認する。
     */
    @Test
    public void testConstructor() {
        
        MockServletRequest req = new MockServletRequest();
        req.addHeader("header1", "value1");
        req.addHeader("header2", "value2");
        
        req.setAttribute("request1", "request_value1");
        req.setAttribute("request2", "request_value2");
        
        NablarchHttpServletRequestWrapper target = new NablarchHttpServletRequestWrapper(req);
        
        // リクエストは同一インスタンスが設定されていること。
        assertThat(target.getRequest(), is(sameInstance((ServletRequest) req)));
        
        // ヘッダー、パラメータ、リクエストスコープは null でないこと。
        assertThat(target.getHeaderMap(), notNullValue());
        assertThat(target.getParameterMap(), notNullValue());
        assertThat(target.getScope(), notNullValue());
        
        assertThat(target.getHeader("header1"), is((Object) "value1"));
        assertThat(target.getHeader("header2"), is((Object) "value2"));
        assertThat(target.getHeader("header3"), is(nullValue()));
        assertThat(target.getScope().get("request1"), is((Object) "request_value1"));
        assertThat(target.getScope().get("request2"), is((Object) "request_value2"));
        assertThat(target.getScope().get("request3"), is(nullValue()));
    }
    
    /**
     * リクエストスコープに対する情報が設定でき、その情報が取得できることを確認する。
     */
    @Test
    public void testScope() {
        
        MockServletRequest req = new MockServletRequest();
        req.setAttribute("request1", "request_value1");
        req.setAttribute("request2", "request_value2");

        NablarchHttpServletRequestWrapper target = new NablarchHttpServletRequestWrapper(req);
        
        @SuppressWarnings("serial")
        Map<String, Object> scope = new HashMap<String, Object>() {{
            put("request3", "request_value3");
            put("request4", "request_value4");
        }};
        target.setScope(scope);
        
        // set したリクエストスコープの情報が取得できること。
        assertThat(target.getScope(), is(sameInstance(scope)));
        assertThat(target.getScope().get("request1"), is(nullValue()));
        assertThat(target.getScope().get("request2"), is(nullValue()));
        assertThat(target.getScope().get("request3"), is((Object) "request_value3"));
        assertThat(target.getScope().get("request4"), is((Object) "request_value4"));
    }
    
    /**
     * ヘッダーに対する情報が設定でき、その情報が取得できることを確認する。
     */
    @Test
    public void testHeader() {
        
        MockServletRequest req = new MockServletRequest();
        req.addHeader("header1", "value1");
        req.addHeader("header2", "value2");
        
        NablarchHttpServletRequestWrapper target = new NablarchHttpServletRequestWrapper(req);
        
        Enumeration<String> eachHeaderNames = target.getHeaderNames();
        List<String> headerNames = new ArrayList<String>();
        
        while (eachHeaderNames.hasMoreElements()) {
            headerNames.add(eachHeaderNames.nextElement());
        }
        Collections.sort(headerNames);
        assertEquals(2, headerNames.size());
        assertEquals("header1", headerNames.get(0));
        assertEquals("header2", headerNames.get(1));        

        
        @SuppressWarnings("serial")
        Map<String, String> header = new HashMap<String, String>() {{
            put("header3", "value3");
            put("header4", "value4");
        }};
        target.setHeaderMap(header);
        
        // set したヘッダーの情報が取得できること。
        assertThat(target.getHeaderMap(), is(sameInstance(header)));
        assertThat(target.getHeader("header1"), is(nullValue()));
        assertThat(target.getHeader("header2"), is(nullValue()));
        assertThat(target.getHeader("header3"), is((Object) "value3"));
        assertThat(target.getHeader("header4"), is((Object) "value4"));
        
    }

    /**
     * リクエストパラメータに対する情報が設定でき、その情報が取得できることを確認する。
     */
    @Test
    public void testParameter() {
        
        MockServletRequest req = new MockServletRequest();
        NablarchHttpServletRequestWrapper target = new NablarchHttpServletRequestWrapper(req);
        
        @SuppressWarnings("serial")
        Map<String, String[]> params = new LinkedHashMap<String, String[]>() {{
            put("param1", new String[] {"param_value1"});
            put("param2", new String[] {"param_value2", "param_value3"});
            put("param4", new String[0]);
        }};
        target.setParamMap(params);
        
        // set したパラメータの情報が取得できること。
        assertThat(target.getParameterMap(), is(sameInstance(params)));
        assertThat(target.getParameter("param1"), is("param_value1"));
        assertThat(target.getParameter("param2"), is("param_value2"));
        assertThat(target.getParameter("param3"), is(nullValue()));
        assertThat(target.getParameter("param4"), is(nullValue()));
        
        Enumeration<String> eachParamNames = target.getParameterNames();
        assertEquals("param1", eachParamNames.nextElement());
        assertEquals("param2", eachParamNames.nextElement());
        assertEquals("param4", eachParamNames.nextElement());
        assertFalse(eachParamNames.hasMoreElements());
        
        String[] values = target.getParameterValues("param2");
        assertEquals(2, values.length);
        assertEquals("param_value2", values[0]);
        assertEquals("param_value3", values[1]);      
        
        // ポストパラメータの読み込み時に実行時エラーが送出される場合。
        // (Weblogicで発生。)
        req = new MockServletRequest() {
            public Map<String, String[]> getParameterMap() {
                throw new RuntimeException("An IOError occured."); 
            }
        };
        target = new NablarchHttpServletRequestWrapper(req);
        
        try {
            target.getParameter("hoge");
            fail();
        } catch (Throwable e) {
            assertEquals(
                NablarchHttpServletRequestWrapper.PostParameterReadError.class,
                e.getClass()
            );
            assertEquals(
                "An IOError occured.",
                e.getCause().getMessage()
            );
            assertEquals(400, ((Result) e).getStatusCode());
        }
    }
    
    /**
     * セッションの取得が正しく行えることを確認する。
     */
    @Test
    public void testGetSession() {
        
        // NablarchHttpServletRequestWrapper 生成時には HttpSession が生成されていないケース。
        MockServletRequest req = new MockServletRequest() {
            @Override
            public HttpSession getSession(boolean create) {
                if (create) {
                    if (getSession() == null) {
                        setSession(new MockHttpSession());
                    }
                }
                return getSession();
            }
        };
        NablarchHttpServletRequestWrapper target = new NablarchHttpServletRequestWrapper(req);
        assertThat(target.getSession(false), is(nullValue()));
        assertThat(target.getSession(true), is(notNullValue()));

        // NablarchHttpServletRequestWrapper 生成時に HttpSession が生成されているケース。
        req = new MockServletRequest();
        req.setSession(new MockHttpSession());
        target = new NablarchHttpServletRequestWrapper(req);
        
        assertThat(target.getSession(false), is(notNullValue()));
        assertThat(target.getSession(true), is(notNullValue()));
    }
    
    @Test
    public void testSessionAccess() {
        HttpSession session = new NablarchHttpServletRequestWrapper.HttpSessionWrapper(
            new MockHttpSession() {
                public long getCreationTime() {
                    return 1111;
                }
                public long getLastAccessedTime() {
                    return 2222;
                }
                public boolean isNew() {
                    return false;
                }
            }
        );
        session.setAttribute("attr1", "value1");
        session.setAttribute("attr2", "value2");
        session.setMaxInactiveInterval(3333);

        assertEquals("value1", session.getAttribute("attr1"));
        assertEquals("value2", session.getAttribute("attr2"));
        assertNull(session.getAttribute("attr3"));
        
        
        Enumeration<String> eachAttrNames = session.getAttributeNames();
        List<String> attrNames = new ArrayList<String>();
        while (eachAttrNames.hasMoreElements()) {
            attrNames.add(eachAttrNames.nextElement());
        }
        Collections.sort(attrNames);
        
        assertEquals(2, attrNames.size());
        assertEquals("attr1", attrNames.get(0));
        assertEquals("attr2", attrNames.get(1));

        attrNames = Collections.list(session.getAttributeNames());
        Collections.sort(attrNames);        
        assertEquals(2, attrNames.size());
        assertEquals("attr1", attrNames.get(0));
        assertEquals("attr2", attrNames.get(1));
        
        assertEquals(1111, session.getCreationTime());
        assertEquals(2222, session.getLastAccessedTime());
        assertEquals(3333, session.getMaxInactiveInterval());
        assertFalse(session.isNew());

        session.removeAttribute("attr1");
        assertNull(session.getAttribute("attr1"));
        
        session.removeAttribute("attr2");
        assertNull(session.getAttribute("attr2"));
    }

}
