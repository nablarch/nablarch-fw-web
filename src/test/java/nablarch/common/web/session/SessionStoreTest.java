package nablarch.common.web.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import mockit.Capturing;
import mockit.Mocked;
import nablarch.common.web.session.encoder.JavaSerializeStateEncoder;
import nablarch.common.web.session.store.HiddenStore;
import nablarch.core.repository.ObjectLoader;
import nablarch.core.repository.SystemRepository;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.servlet.HttpRequestWrapper;
import nablarch.fw.web.servlet.NablarchHttpServletRequestWrapper;
import nablarch.fw.web.servlet.ServletExecutionContext;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author tajima
 *
 */
public class SessionStoreTest {
    @Capturing
    private HttpServletResponse httpResponse;

    @Mocked
    private ServletContext servletContext;

    @Before
    public void setUp() {
        handler = new SessionStoreHandler();
        manager = new SessionManager();
        store   = new HiddenStore();
        store.setParameterName("serialized_session");


        store.setExpires(900L);
        manager.setAvailableStores(new ArrayList(){{add(store);}});
        handler.setSessionManager(manager);
        sessionContent.clear();

        SystemRepository.load(new ObjectLoader() {
            @Override
            public Map<String, Object> load() {
                Map<String, Object> repos = new HashMap<String, Object>();
                repos.put("sessionManager", manager);
                repos.put("sessionStoreHandler", handler);
                return repos;
            }
        });
    }

    @After
    public void tearDown() {
        SystemRepository.clear();
    }

    private static final Map<String, Object> sessionContent = new HashMap<String, Object>();
    private SessionStoreHandler handler;
    private SessionManager manager;
    private HiddenStore store;


    @Test
    public void testThatItOffersFeaturesOfSerializationAndDeserializationToItsSubTypes() {

        HttpServletRequest servletReq = new MockHttpServletRequest().getMockInstance();
        HttpRequest request = new HttpRequestWrapper(
            new NablarchHttpServletRequestWrapper(servletReq)
        );
        ExecutionContext ctx = new ServletExecutionContext(servletReq, httpResponse, servletContext);
        Session session = manager.create(ctx);
        store.save(session.getOrGenerateId(), new ArrayList<SessionEntry>() {{
            add(new SessionEntry("hoge", "hoge_value", store));
            add(new SessionEntry("fuga", "fuga_value", store));
            add(new SessionEntry("piyo", null, store));
            add(new SessionEntry("\uD83C\uDF63\uD83C\uDF63", "\uD83C\uDF7A\uD83C\uDF7A", store));
        }}, ctx);

        final String marshalled = ctx.getRequestScopedVar("serialized_session");

        assertTrue(marshalled.length() > 0);

        servletReq = new MockHttpServletRequest()
                    .setParameterMap(new HashMap<String, String[]>(){{
                         put("serialized_session", new String[]{marshalled});
                     }})
                    .getMockInstance();
        request = new HttpRequestWrapper(
            new NablarchHttpServletRequestWrapper(servletReq)
        );

        ctx = new ServletExecutionContext(servletReq, httpResponse, servletContext);

        List<SessionEntry> unmarshalled = store.load(session.getOrGenerateId(), ctx);
        assertEquals(4, unmarshalled.size());
        assertEquals("hoge", unmarshalled.get(0).getKey());
        assertEquals("hoge_value", unmarshalled.get(0).getValue());
        assertEquals("fuga", unmarshalled.get(1).getKey());
        assertEquals("fuga_value", unmarshalled.get(1).getValue());
        assertEquals("piyo", unmarshalled.get(2).getKey());
        assertEquals("\uD83C\uDF63\uD83C\uDF63", unmarshalled.get(3).getKey());
        assertEquals("\uD83C\uDF7A\uD83C\uDF7A", unmarshalled.get(3).getValue());
        assertNull(unmarshalled.get(2).getValue());
    }

    @Test
    public void testUsingAlternateEncoder() {

        assertTrue(
            "明示的に設定しない場合はJavaのSerializationによるエンコーダを使用する。"
          , manager.getDefaultEncoder() instanceof JavaSerializeStateEncoder
        );

        manager.setDefaultEncoder(new JavaSerializeStateEncoder());

        HttpServletRequest servletReq = new MockHttpServletRequest().getMockInstance();
        HttpRequest request = new HttpRequestWrapper(
            new NablarchHttpServletRequestWrapper(servletReq)
        );
        ExecutionContext ctx = new ServletExecutionContext(servletReq, httpResponse, servletContext);
        Session session = manager.create(ctx);
        store.save(session.getOrGenerateId(), new ArrayList<SessionEntry>() {{
            add(new SessionEntry("hoge", "hoge_value", store));
            add(new SessionEntry("fuga", "fuga_value", store));
        }}, ctx);

        final String marshalled = ctx.getRequestScopedVar("serialized_session");

        assertTrue(marshalled.length() > 0);

        servletReq = new MockHttpServletRequest()
                    .setParameterMap(new HashMap<String, String[]>(){{
                         put("serialized_session", new String[]{marshalled});
                     }})
                    .getMockInstance();
        request = new HttpRequestWrapper(
            new NablarchHttpServletRequestWrapper(servletReq)
        );

        ctx = new ServletExecutionContext(servletReq, httpResponse, servletContext);

        List<SessionEntry> unmarshalled = store.load(session.getOrGenerateId(), ctx);
        assertEquals(2, unmarshalled.size());
        assertEquals("hoge", unmarshalled.get(0).getKey());
        assertEquals("hoge_value", unmarshalled.get(0).getValue());
        assertEquals("fuga", unmarshalled.get(1).getKey());
        assertEquals("fuga_value", unmarshalled.get(1).getValue());
    }


    private static class HogeBean {
        private String stringValue = null;
        public String getStringValue() {
            return stringValue;
        }
        public void setStringValue(String stringValue) {
            this.stringValue = stringValue;
        }

        private Integer intValue = null;
        public Integer getIntValue() {
            return intValue;
        }
        public void setIntValue(Integer intValue) {
            this.intValue = intValue;
        }

        public String getReadonlyValue() {
            return "readonly";
        }

        private String writeonly = null;
        public void setWriteonlyValue(String val) {
            this.writeonly = val;
        }
    }

    @Test
    public void testThatIfAnEntryWrapsAnJavaBeansItsPropertiesCanBeAccessedThroughMapInterface() {
        HogeBean bean = new HogeBean();
        SessionEntry entry = new SessionEntry("hoge", bean, store);
        bean.setStringValue("str\uD83C\uDF7A\uD83C\uDF7A");
        bean.setIntValue(100);

        assertTrue(!entry.isEmpty());
        assertEquals(3, entry.size());
        assertTrue(entry.containsKey("stringValue"));
        assertTrue(entry.containsValue("str\uD83C\uDF7A\uD83C\uDF7A"));
        assertEquals("str\uD83C\uDF7A\uD83C\uDF7A", entry.get("stringValue"));
        assertEquals(100, entry.get("intValue"));
        assertEquals("readonly", entry.get("readonlyValue"));
        assertNull(entry.get("writeonlyValue"));
        assertEquals(3, entry.keySet().size());
        assertEquals(3, entry.values().size());
        assertEquals(3, entry.entrySet().size());

        bean.setStringValue(null);
        entry = new SessionEntry("hoge", bean, store);
        assertNull(entry.get("stringValue"));


        try {
            entry.clear();
        } catch (Throwable e) {
            assertTrue(e instanceof UnsupportedOperationException);
        }

        try {
            entry.put("hoge", "fuga");
        } catch (Throwable e) {
            assertTrue(e instanceof UnsupportedOperationException);
        }

        try {
            entry.putAll(new HashMap());
        } catch (Throwable e) {
            assertTrue(e instanceof UnsupportedOperationException);
        }

        try {
            entry.remove("hoge");
        } catch (Throwable e) {
            assertTrue(e instanceof UnsupportedOperationException);
        }

        long now = System.currentTimeMillis();

        entry = new SessionEntry("hoge", new Timestamp(now), store);
        assertEquals(now, entry.get("time"));

        entry = new SessionEntry("hoge", "fuga", store);
        assertEquals(new String("fuga".getBytes()), new String((byte[])entry.get("bytes")));

        entry = new SessionEntry("hoge", new Integer(13), store);
        assertTrue(entry.isEmpty());

        entry = new SessionEntry("hoge", null, store);
        assertTrue(entry.isEmpty());
    }

    private static class ErrornousBean {
        public String getValue() {
            throw new IllegalStateException();
        }
    }

    @Test
    public void testThatAnEntryWrapsAJavaBeanWhosePropertyRaisesAnExceptionIsSeenAsAnEmptyMap() {
        SessionEntry entry = new SessionEntry("hoge", new ErrornousBean(), store);
        assertEquals(0, entry.size());
        assertTrue(entry.isEmpty());
    }
}
