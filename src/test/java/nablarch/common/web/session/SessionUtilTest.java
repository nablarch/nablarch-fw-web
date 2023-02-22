package nablarch.common.web.session;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletResponse;

import nablarch.common.web.session.store.HiddenStore;
import nablarch.common.web.session.store.HttpSessionStore;
import nablarch.core.repository.ObjectLoader;
import nablarch.core.repository.SystemRepository;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.servlet.ServletExecutionContext;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import mockit.Capturing;
import mockit.Mocked;

/**
 * {@link SessionUtil}のテスト
 * 
 * @author Naoki Yamamoto
 */
public class SessionUtilTest {

    @Capturing
    private HttpServletResponse httpResponse;

    @Mocked
    private ServletContext servletContext;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    
    private static final Map<String, Object> sessionContent = new HashMap<String, Object>();
    private static SessionStoreHandler handler;
    private static SessionManager manager;
    private static SessionStore store1;
    private static SessionStore store2;
    private ExecutionContext ctx;
    
    @SuppressWarnings("serial")
    @BeforeClass
    public static void setUpClass() {
        handler = new SessionStoreHandler();
        manager = new SessionManager();
        store1 = new HttpSessionStore();
        store1.setExpires(900L);
        store2 = new HiddenStore();
        store2.setExpires(900L);
        manager.setAvailableStores(new ArrayList<SessionStore>(){{add(store1);add(store2);}});
        manager.setDefaultStoreName("httpSession");
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
    
    @Before
    public void setUp() {
        ctx = new ServletExecutionContext(new MockHttpServletRequest().getMockInstance(), httpResponse, servletContext);
        ctx.setSessionStoredVar("test", new SessionEntry("test", "12345", store1));
    }

    @AfterClass
    public static void tearDownClass() {
        SystemRepository.clear();
    }

    @Test
    public void testGet() {
        assertThat((String) SessionUtil.get(ctx, "test"), is("12345"));
    }
    
    @Test
    public void testGetForNotFound() {
        expectedException.expect(SessionKeyNotFoundException.class);
        expectedException.expectMessage("specified key was not found in session store. key: aaa");
        SessionUtil.get(ctx, "aaa");
    }
    
    @Test
    public void testOrNull() {
        assertThat((String) SessionUtil.orNull(ctx, "test"), is("12345"));
    }
    
    @Test
    public void testOrNullForNotFound() {
        assertThat(SessionUtil.orNull(ctx, "aaa"), is(nullValue()));
    }
    
    @Test
    public void testOr() {
        assertThat((String) SessionUtil.or(ctx, "test", "default"), is("12345"));
    }
    
    @Test
    public void testOrForNotFound() {
        assertThat(SessionUtil.or(ctx, "aaa", "default"), is("default"));
    }
    
    @Test
    public void testPut() {
        
        assertThat(ctx.getSessionStoredVar("put"), is(nullValue()));
        SessionUtil.put(ctx, "put", "value");
        assertThat(ctx.getSessionStoredVar("put"), is(notNullValue()));
        assertThat(ctx.getSessionStoredVar("put"), instanceOf(SessionEntry.class));
    }
    
    @Test
    public void testPutStoreName() {
        assertThat(ctx.getSessionStoredVar("put"), is(nullValue()));
        SessionUtil.put(ctx, "put", "value", "hidden");
        assertThat(ctx.getSessionStoredVar("put"), is(notNullValue()));
        assertThat(ctx.getSessionStoredVar("put"), instanceOf(SessionEntry.class));
    }
    
    @Test
    public void testDelete() {
        assertThat("セッションにはSessionEntryが格納されていること",
                ctx.getSessionStoredVar("test"),
                is(instanceOf(SessionEntry.class)));

        // 削除
        String result = SessionUtil.delete(ctx, "test");
        Assert.assertThat("削除したセッションの値が戻されること", result, is("12345"));

        assertThat("削除後は、セッションスコープにはセッションの値が設定されていること",
                (String) ctx.getSessionStoredVar("test"), is("12345"));

        assertThat("削除済みの場合でも例外は発生しない。また戻り値はnullとなること",
                SessionUtil.delete(ctx, "test"), is(nullValue()));
        assertThat("deleteでは何も実行されないので、値もそのまま", (String) ctx.getSessionStoredVar("test"), is("12345"));

        // 存在しないキー名を指定してもエラーとはならない
        assertThat(SessionUtil.delete(ctx, "not_found"), is(nullValue()));
    }
    
    @Test
    public void testInvalidate() {
        ctx.setSessionStoredVar("put", new SessionEntry("put", "value", store1));
        assertThat(ctx.getSessionStoredVar("test"), is(notNullValue()));
        assertThat(ctx.getSessionStoredVar("put"), is(notNullValue()));
        SessionUtil.invalidate(ctx);
        assertThat((String) ctx.getSessionStoredVar("test"), is("12345"));
        assertThat((String) ctx.getSessionStoredVar("put"), is("value"));
        
        assertThat((Boolean) ctx.getSessionStoredVar(SessionStoreHandler.IS_INVALIDATED_KEY), is(Boolean.TRUE));
    }
    
    @Test
    public void testOverwrite() {

        // リクエストスコープに同一名称のパラメータをセットしても、セッションストアが上書きされないこと。
        ctx.setRequestScopedVar("test", new SessionEntry("test", "AAAAA", store1));
        assertThat((String) SessionUtil.get(ctx, "test"), is("12345"));
        
        // セッションストアに同一名称のパラメータをセットしても、リクエストスコープが上書きされないこと。
        SessionUtil.put(ctx, "test", "BBBBB");
        SessionEntry test = ctx.getRequestScopedVar("test");
        assertThat((String) test.getValue(), is("AAAAA"));
    }

    @Test
    public void testChangeId() {
        SessionUtil.put(ctx, "foo", "FOO");
        SessionUtil.put(ctx, "bar", "BAR");

        SessionUtil.changeId(ctx);

        // invalidate のマークは設定されているが、セッションストアの値がそのまま維持されていること
        assertThat(SessionUtil.<String>get(ctx, "foo"), is("FOO"));
        assertThat(SessionUtil.<String>get(ctx, "bar"), is("BAR"));

        assertThat((Boolean) ctx.getSessionStoredVar(SessionStoreHandler.IS_INVALIDATED_KEY), is(Boolean.TRUE));
    }
}
