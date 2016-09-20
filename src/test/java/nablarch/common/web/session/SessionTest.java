package nablarch.common.web.session;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.is;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import mockit.Capturing;
import mockit.Mocked;
import nablarch.common.web.session.store.HttpSessionStore;
import nablarch.core.repository.ObjectLoader;
import nablarch.core.repository.SystemRepository;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.servlet.ServletExecutionContext;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author tajima
 *
 */
public class SessionTest {

    @Capturing
    private HttpServletResponse httpResponse;

    @Mocked
    private ServletContext servletContext;

    @SuppressWarnings("serial")
    @Before
    public void setUp() {
        handler = new SessionStoreHandler();
        manager = new SessionManager();
        store   = new HttpSessionStore();
        store.setExpires(900L);
        manager.setAvailableStores(new ArrayList<SessionStore>(){{add(store);}});
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
    private SessionStore store;

    @Test
    public void testThatItGeneratesRandomSessionIdAtFirstAccessAndReturnsItInSubsequentAccess() {

        HttpServletRequest servletReq = new MockHttpServletRequest().getMockInstance();

        ExecutionContext ctx = new ServletExecutionContext(servletReq, httpResponse, servletContext);

        Session session = manager.create(ctx);
        assertNull(session.getId());
        String generatedId = session.getOrGenerateId().toString();
        assertTrue(generatedId.length() > 0);
        assertEquals(generatedId, session.getId().toString());
        assertEquals(generatedId, session.getOrGenerateId().toString());
    }

    @Test
    public void testThatItHasFundamentalInterfaceForManagingSessionEntries() {
        HttpServletRequest servletReq = new MockHttpServletRequest().getMockInstance();

        ExecutionContext ctx = new ServletExecutionContext(servletReq, httpResponse, servletContext);

        Session session = manager.create(ctx);

        session.put("hoge", "hoge_value", store.getName());
        session.put("fuga", "fuga_value", store.getName());
        session.put("piyo", "must_be_lost", "unknown");

        assertEquals("hoge_value", session.get("hoge"));
        assertEquals("fuga_value", session.get("fuga", String.class));
        assertNull(session.get("piyo"));

        session.delete("hoge");

        assertNull(session.get("hoge"));

        session.deleteAll();

        assertNull(session.get("fuga"));
    }
    
    @Test
    public void testLoad_InvalidSessionId() {
        HttpServletRequest servletReq = new MockHttpServletRequest().getMockInstance();

        ExecutionContext ctx = new ServletExecutionContext(servletReq, httpResponse, servletContext);

        Session session = manager.create(ctx);

        session.put("hoge", "hoge_value", store.getName());

        session.save();
        String sessionId = session.getId();

        session = manager.create(ctx);

        // 不正なセッションIDでロード
        session.load("aaa");

        assertThat("セッションストアに保存した値を取得できないこと", session.get("hoge"), is(nullValue()));

        // 正しいセッションIDでロード
        session.load(sessionId);

        assertThat("セッションストアに保存した値を取得できること", (String) session.get("hoge"), is("hoge_value"));
    }

    @Test
    public void testInvalidate() {

        HttpServletRequest servletReq = new MockHttpServletRequest().getMockInstance();
        ExecutionContext ctx = new ServletExecutionContext(servletReq, httpResponse, servletContext);
        Session session = manager.create(ctx);

        assertNull(session.getId());

        session.invalidate();

        assertNull(session.getId());

        String id = session.getOrGenerateId();

        assertEquals(id, session.getId());

        session.invalidate();

        assertNull(session.getId());
    }
}
