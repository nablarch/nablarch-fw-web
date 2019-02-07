package nablarch.common.web.session;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.EOFException;
import java.io.NotSerializableException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hamcrest.beans.HasPropertyWithValue;
import org.hamcrest.collection.IsCollectionWithSize;
import org.hamcrest.collection.IsMapContaining;

import nablarch.common.web.session.store.HiddenStore;
import nablarch.common.web.session.store.HiddenStore.HiddenStoreLoadFailedException;
import nablarch.common.web.session.store.HttpSessionStore;
import nablarch.core.repository.ObjectLoader;
import nablarch.core.repository.SystemRepository;
import nablarch.core.util.StringUtil;
import nablarch.fw.ExecutionContext;
import nablarch.fw.Handler;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.servlet.HttpRequestWrapper;
import nablarch.fw.web.servlet.NablarchHttpServletRequestWrapper;
import nablarch.fw.web.servlet.ServletExecutionContext;
import nablarch.test.FixedSystemTimeProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import mockit.Capturing;
import mockit.Mock;
import mockit.Mocked;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;

/**
 * @author Iwauo Tajima
 */
@SuppressWarnings("serial")
@RunWith(JMockit.class)
public class SessionStoreHandler_AlternateFlowTest {

    @Capturing
    private HttpServletResponse httpResponse;

    @Mocked
    private ServletContext servletContext;

    private FixedSystemTimeProvider systemTimeProvider = new FixedSystemTimeProvider() {{
        setFixedDate("20161231235959999");
    }};

    @Before
    public void setUp() {
        handler = new SessionStoreHandler();
        manager = new SessionManager();
        store   = new HttpSessionStore();
        store.setName("test");
        store.setExpires(900L);
        manager.setDefaultStoreName("test");
        manager.setAvailableStores(new ArrayList<SessionStore>(){{add(store);}});
        handler.setSessionManager(manager);
        MockHttpServletRequest.sessionContent.clear();

        SystemRepository.load(new ObjectLoader() {
            @Override
            public Map<String, Object> load() {
                Map<String, Object> repos = new HashMap<String, Object>();
                repos.put("sessionManager", manager);
                repos.put("sessionStoreHandler", handler);
                repos.put("systemTimeProvider", systemTimeProvider);
                return repos;
            }
        });
    }

    @After
    public void tearDown() {
        SystemRepository.clear();
    }

    private SessionStoreHandler handler;
    private SessionManager manager;
    private SessionStore store;

    /**
     * 有効期限がCookieではなくHTTPセッションに保持されていること
     */
    @Test
    public void testMaxAge() {

        manager.setAvailableStores(Arrays.asList(
            manager.getAvailableStores().get(0), // 900L
            new HiddenStore() {{ // 600L
                setName("min_maxAge");
                setExpires(600L);
            }},
            new HiddenStore() { // 1800L but isExtendable:false
                {
                    setName("max_maxAge");
                    setExpires(1800L);
                }
                @Override
                public boolean isExtendable() {
                    return false;
                }
            }
        ));

        HttpServletRequest servletReq = new MockHttpServletRequest()
            .setCookies(new Cookie[]{
                 new Cookie("JSESSION_ID", "hogehoge")
               , new Cookie("NABLARCH_SID", createSessionId())
             })
            .getMockInstance();

        HttpRequest request = new HttpRequestWrapper(
            new NablarchHttpServletRequestWrapper(servletReq)
        );

        final ServletExecutionContext servCtx = new ServletExecutionContext(servletReq, httpResponse, servletContext);

        servCtx.addHandler(handler)
               .addHandler(new Handler<HttpRequest, HttpResponse>() {
                    @Override
                    public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                        SessionUtil.put(ctx, "hoge", "hoge_value");
                        return new HttpResponse(200);
                    }
                })
               .handleNext(request);

        assertThat("システム日時にセッションストアの有効期限(ミリ秒)を加算した値が、HTTPセッションに設定されていること",
                (Long) servCtx.getSessionScopedVar("nablarch_sessionStore_expiration_date"),
                is(systemTimeProvider.getTimestamp().getTime() + 900000L));

        new Verifications() {{
            Cookie cookie;
            httpResponse.addCookie(cookie = withCapture());
            assertThat(cookie.getName(), is("NABLARCH_SID"));
            assertThat(cookie.getMaxAge(), is(-1));
            assertThat(cookie.getValue(), is(notNullValue()));
        }};
    }

    /**
     * セッションの有効期限が切れた場合に新規でセッションを生成すること
     */
    @Test
    public void testTimeOut() {

        final String sessionId = createSessionId();

        HttpServletRequest servletReq = new MockHttpServletRequest()
                .setCookies(new Cookie[]{
                        new Cookie("JSESSION_ID", "hogehoge"),
                        new Cookie("NABLARCH_SID", sessionId)
                })
                .getMockInstance();

        HttpRequest request = new HttpRequestWrapper(
                new NablarchHttpServletRequestWrapper(servletReq)
        );

        final ServletExecutionContext servCtx = new ServletExecutionContext(servletReq, httpResponse, servletContext);

        servCtx.setSessionScopedVar("nablarch_sessionStore_expiration_date", systemTimeProvider.getTimestamp().getTime() - 1L);

        store.save(sessionId, new ArrayList<SessionEntry>() {{
            add(new SessionEntry("hoge", "hogeValue", store));
            add(new SessionEntry("fuga", "fugaValue", store));
        }}, servCtx);

        servCtx.addHandler(handler)
                .addHandler(new Handler<HttpRequest, HttpResponse>() {
                    @Override
                    public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                        assertThat(SessionUtil.orNull(ctx, "hoge"), is(nullValue()));
                        assertThat(SessionUtil.orNull(ctx, "fuga"), is(nullValue()));
                        return new HttpResponse(200);
                    }
                })
                .handleNext(request);

        assertThat("セッションを生成していないため、有効期限が更新されていないこと",
                (Long) servCtx.getSessionScopedVar("nablarch_sessionStore_expiration_date"),
                is(systemTimeProvider.getTimestamp().getTime() - 1L));

        // クッキーが生成されていないこと
        new Verifications() {{
            Cookie cookie;
            httpResponse.addCookie(cookie = withCapture());
            times = 0;
        }};
    }

    @Test
    public void testThat_EvenIfSessionTrackingIdIsInvalid_ItContinuesToProcessTheRequestAndCreateNewSession() {

        HttpServletRequest servletReq = new MockHttpServletRequest()
            .setCookies(new Cookie[]{
                 new Cookie("JSESSION_ID", "hogehoge")
             })
            .getMockInstance();

        HttpRequest request = new HttpRequestWrapper(
            new NablarchHttpServletRequestWrapper(servletReq)
        );

        final ServletExecutionContext servCtx = new ServletExecutionContext(servletReq, httpResponse, servletContext);

        HttpResponse res = servCtx.addHandler(handler)
               .addHandler(new Handler<HttpRequest, HttpResponse>() {
                    @Override
                    public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                        SessionUtil.put(ctx, "hoge", "hoge_value");
                        SessionUtil.put(ctx, "fuga", "fuga_value");
                        return new HttpResponse(200, "success");
                    }
                })
               .handleNext(request);

        assertThat("想定したステータスコードのレスポンスが返却されること", res.getStatusCode(), is(200));
        assertThat("想定したコンテンツパスのレスポンスが返却されること", res.getContentPath().getPath(), is("success"));

        assertThat("システム日時にセッションストアの有効期限(ミリ秒)を加算した値が、HTTPセッションに設定されていること",
                (Long) servCtx.getSessionScopedVar("nablarch_sessionStore_expiration_date"),
                is(systemTimeProvider.getTimestamp().getTime() + 900000L));

        final Map<String, String> holder = new HashMap<String, String>();
        new Verifications() {{
            Cookie cookie;
            httpResponse.addCookie(cookie = withCapture());
            assertThat(cookie.getName(), is("NABLARCH_SID"));
            assertThat(cookie.getMaxAge(), is(-1));
            assertThat(cookie.getValue(), is(notNullValue()));
            holder.put("sessionId", cookie.getValue());
        }};

        List<SessionEntry> entries = store.load(holder.get("sessionId"), servCtx);
        assertThat(entries.size(), is(2));
        assertThat((String) find(entries, "hoge").getValue(), is("hoge_value"));
        assertThat((String) find(entries, "fuga").getValue(), is("fuga_value"));
    }

    private SessionEntry find(List<SessionEntry> entries, String byKey) {
        for (SessionEntry entry : entries) {
            if (byKey.equals(entry.getKey())) {
                return entry;
            }
        }
        fail("SessionEntry not found. key =[" + byKey + "]");
        return null;
    }

    @Test
    public void testThatItRestoresSessionStateFromStoreIfThereIsTrackingIdInCookie() {
        final String sessionId = createSessionId();

        HttpServletRequest servletReq = new MockHttpServletRequest()
            .setCookies(new Cookie[]{
                    new Cookie("JSESSION_ID", "hogehoge"),
                    new Cookie("NABLARCH_SID", sessionId)
            })
            .getMockInstance();

        HttpRequest request = new HttpRequestWrapper(
            new NablarchHttpServletRequestWrapper(servletReq)
        );

        final ServletExecutionContext servCtx = new ServletExecutionContext(servletReq, httpResponse, servletContext);

        servCtx.setSessionScopedVar("nablarch_sessionStore_expiration_date", systemTimeProvider.getTimestamp().getTime());

        store.save(sessionId, new ArrayList<SessionEntry>() {{
            add(new SessionEntry("hoge", "hogeValue", store));
            add(new SessionEntry("fuga", "fugaValue", store));
        }}, servCtx);

        HttpResponse res = servCtx.addHandler(handler)
           .addHandler(new Handler<HttpRequest, HttpResponse>() {
                @Override
                public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                    assertThat((String) SessionUtil.get(ctx, "hoge"), is("hogeValue"));
                    assertThat((String) SessionUtil.get(ctx, "fuga"), is("fugaValue"));
                    return new HttpResponse(200, "success");
                }
            })
           .handleNext(request);

        assertThat("想定したステータスコードのレスポンスが返却されること", res.getStatusCode(), is(200));
        assertThat("想定したコンテンツパスのレスポンスが返却されること", res.getContentPath().getPath(), is("success"));

        assertThat("システム日時にセッションストアの有効期限(ミリ秒)を加算した値が、HTTPセッションに設定されていること",
                (Long) servCtx.getSessionScopedVar("nablarch_sessionStore_expiration_date"),
                is(systemTimeProvider.getTimestamp().getTime() + 900000L));

        new Verifications() {{
            Cookie cookie;
            httpResponse.addCookie(cookie = withCapture());
            assertThat(cookie.getName(), is("NABLARCH_SID"));
            assertThat(cookie.getMaxAge(), is(-1));
        }};

        List<SessionEntry> entries = store.load(sessionId, servCtx);
        assertThat(entries.size(), is(2));
        assertThat((String) find(entries, "hoge").getValue(), is("hogeValue"));
        assertThat((String) find(entries, "fuga").getValue(), is("fugaValue"));
    }
    
    /**
     * load時にHttpSessionがなければ生成されないこと。
     */
    @Test
    public void testDoNotCreateSessionInLoad() {
        final SessionStore hidden = new HiddenStore();
        hidden.setExpires(900L);
        final SessionStore httpSession = new HttpSessionStore();
        httpSession.setExpires(900L);
        manager.setAvailableStores(new ArrayList<SessionStore>() {{
            add(hidden);
            add(httpSession);
        }});

        final String sessionId = createSessionId();

        HttpServletRequest servletReq = new MockHttpServletRequest(){
            @Mock
            public HttpSession getSession() {
                fail("セッションを生成してはダメ");
                return null;
            }
            @Mock 
            public HttpSession getSession(boolean b) { 
                assertThat("セッションを生成してはダメ", b, is(false));
                return null;
            }
        }
            .setCookies(new Cookie[]{new Cookie("NABLARCH_SID", sessionId)})
            .getMockInstance();

        HttpRequest request = new HttpRequestWrapper(
            new NablarchHttpServletRequestWrapper(servletReq) {
                @Override
                public HttpSessionWrapper getSession(boolean create) {
                    assertThat("セッション生成フラグはfalseでないとだめ", create, is(false));
                    return null; // ないのでnull
                }
            }
        );

        final ServletExecutionContext servCtx = new ServletExecutionContext(servletReq, httpResponse, servletContext);

        hidden.save(sessionId, new ArrayList<SessionEntry>() {{
            add(new SessionEntry("hoge", "hogeValue", hidden));
            add(new SessionEntry("fuga", "fugaValue", hidden));
        }}, servCtx);

        servCtx.addHandler(handler)
               .addHandler(new Handler<HttpRequest, HttpResponse>() {
                    @Override
                    public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                        return new HttpResponse(200);
                    }
                })
               .handleNext(request);
    }

    @Test
    public void testThatItRaisesExceptionWhenFailedToSerializeSessionContent() {
        manager.setAvailableStores(new ArrayList<SessionStore>(){{add(new HiddenStore());}});
        manager.setDefaultStoreName("hidden");
        HttpServletRequest httpRequest = new MockHttpServletRequest().getMockInstance();
        HttpRequest request = new HttpRequestWrapper(
            new NablarchHttpServletRequestWrapper(httpRequest)
        );

        try {
            new ServletExecutionContext(httpRequest, httpResponse, servletContext)
               .addHandler(handler)
               .addHandler(new Handler<HttpRequest, HttpResponse>() {
                    @Override
                    public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                        SessionUtil.put(ctx, "notSerializable", new Thread());
                        return new HttpResponse(200);
                    }
                })
               .handleNext(request);

            fail();

        } catch (Throwable e) {
            assertThat(
                "セッション内容の保存に失敗した場合は実行時例外を送出する。"
              , e, instanceOf(EncodeException.class)
            );
            assertThat(
                "セッションの保存に失敗した起因例外をチェインする。"
              , e.getCause(), instanceOf(NotSerializableException.class)
            );
        }
    }

    /**
     * HiddenStoreのロードに失敗した場合は、400が返されること。
     */
    @Test
    public void testTamperingDetectedForHiddenStore() {

        manager.setAvailableStores(new ArrayList<SessionStore>(){{add(new HiddenStore());}});
        final String sessionId = createSessionId();

        HttpServletRequest httpRequest = new MockHttpServletRequest()
            .setCookies(new Cookie[]{
                    new Cookie("JSESSION_ID", "hogehoge"),
                    new Cookie("NABLARCH_SID", sessionId)
            })
            .getMockInstance();

        HttpRequest request = new HttpRequestWrapper(new NablarchHttpServletRequestWrapper(httpRequest));

        ServletExecutionContext servCtx = (ServletExecutionContext)
                new ServletExecutionContext(httpRequest, httpResponse, servletContext)
                        .addHandler(handler)
                        .addHandler(new Handler<HttpRequest, HttpResponse>() {
                            @Override
                            public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                                return new HttpResponse(200);
                            }
                        });
        servCtx.getHttpRequest().setParam("nablarch_hiddenStore", "invalid-data");
        servCtx.setSessionScopedVar("nablarch_sessionStore_expiration_date", systemTimeProvider.getTimestamp().getTime());

        HttpResponse res = servCtx.handleNext(request);

        assertThat("セッションストアの有効期限(ミリ秒)が変更されていないこと",
                (Long) servCtx.getSessionScopedVar("nablarch_sessionStore_expiration_date"),
                is(systemTimeProvider.getTimestamp().getTime()));
        
        assertThat("セッション内容のロードに失敗した場合は改竄の可能性があるので400を返す。", res.getStatusCode(), is(400));
        assertThat("セッションの保存に失敗した起因例外をコンテキストにセットする。", servCtx.getException() instanceof HiddenStoreLoadFailedException, is(true));
    }

    /**
     * SessionStoreのロードに失敗した場合は、発生した例外がそのまま返されること。
     */
    @Test
    public void testTamperingDetectedForSessionStore() {

        final String sessionId = createSessionId();

        HttpServletRequest httpRequest = new MockHttpServletRequest()
            .setCookies(new Cookie[]{
                    new Cookie("JSESSION_ID", "hogehoge"),
                    new Cookie("NABLARCH_SID", sessionId)
            })
            .getMockInstance();

        HttpRequest request = new HttpRequestWrapper(new NablarchHttpServletRequestWrapper(httpRequest));

        ServletExecutionContext servCtx = (ServletExecutionContext)
                new ServletExecutionContext(httpRequest, httpResponse, servletContext)
                        .addHandler(handler)
                        .addHandler(new Handler<HttpRequest, HttpResponse>() {
                            @Override
                            public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                                return new HttpResponse(200);
                            }
                        });

        servCtx.setSessionScopedVar(sessionId, "invalid-data");
        servCtx.setSessionScopedVar("nablarch_sessionStore_expiration_date", systemTimeProvider.getTimestamp().getTime());

        try {
            servCtx.handleNext(request);
            fail();
        } catch (Throwable e) {
            assertThat("セッションストアの有効期限(ミリ秒)が変更されていないこと",
                    (Long) servCtx.getSessionScopedVar("nablarch_sessionStore_expiration_date"),
                    is(systemTimeProvider.getTimestamp().getTime()));
            assertThat("セッション内容の保存に失敗した場合は実行時例外を送出する。", e, instanceOf(EncodeException.class));
            assertThat("セッションの保存に失敗した起因例外をチェインする。", e.getCause(), instanceOf(EOFException.class));
        }
    }

    @Test
    public void testRaisesErrorWhenItHasNotAnySessionStoresOrMakingItsSessionStoresEmpty() {
        SessionManager manager = new SessionManager();
        try {
            manager.getAvailableStores();
            fail();
        } catch (Throwable e) {
            assertThat(e, instanceOf(IllegalStateException.class));
        }
        try {
            manager.getDefaultStore();
            fail();
        } catch (Throwable e) {
            assertThat(e, instanceOf(IllegalStateException.class));
        }
        try {
            manager.setAvailableStores(null);
        } catch (Throwable e) {
            assertThat(e, instanceOf(IllegalArgumentException.class));
        }
        try {
            manager.setAvailableStores(Arrays.asList(new SessionStore[]{}));
        } catch (Throwable e) {
            assertThat(e, instanceOf(IllegalArgumentException.class));
        }
    }

    @Test
    public void testThatItCanManageMultipleSessionStores() {

        HttpServletRequest servletReq = new MockHttpServletRequest().getMockInstance();
        HttpRequest request = new HttpRequestWrapper(
            new NablarchHttpServletRequestWrapper(servletReq)
        );
        final ServletExecutionContext servCtx = new ServletExecutionContext(servletReq, httpResponse, servletContext);

        final HiddenStore hiddenStore = new HiddenStore();
        hiddenStore.setParameterName("hiddenStore");
        hiddenStore.setExpires(900L);

        manager.setAvailableStores(new ArrayList<SessionStore>() {{
            add(store);
            add(hiddenStore);
        }});

        final List<Boolean> runAction = new ArrayList<Boolean>();

        servCtx.addHandler(handler)
           .addHandler(new Handler<HttpRequest, HttpResponse>() {
                @Override
                public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                    SessionUtil.put(ctx, "var_on_defaultStore", "value_on_defaultStore");
                    SessionUtil.put(ctx, "var_on_hiddenStore", "value_on_hiddenStore", "hidden");
                    runAction.add(true);
                    return new HttpResponse(200);
                }
            })
           .handleNext(request);

        assertThat(runAction.size(), is(1));

        assertThat((HiddenStore) manager.findSessionStore("hidden"), sameInstance(hiddenStore));
        try {
            manager.findSessionStore("notExists");
            fail("must be thrown IllegalStateException.");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), is("not found session store. storeName=[notExists]"));
        }
        assertThat(manager.getDefaultStore(), sameInstance(store));

        final Map<String, String> holder = new HashMap<String, String>();
        new Verifications() {{
            Cookie cookie;
            httpResponse.addCookie(cookie = withCapture());
            assertThat(cookie.getName(), is("NABLARCH_SID"));
            assertThat(cookie.getMaxAge(), is(-1));
            assertThat(cookie.getValue(), is(notNullValue()));
            holder.put("sessionId", cookie.getValue());
        }};

        List<SessionEntry> entries = store.load(holder.get("sessionId"), servCtx);
        assertThat(entries.size(), is(1));
        assertThat(entries.get(0).getKey(), is("var_on_defaultStore"));
    }

    /**
     * Action内でinvalidateして、putすると、SIDが変わること。
     * セッションIDがクッキーに設定されている場合。
     */
    @Test
    public void testRegenerateSessionIdWhenPutAfterInvalidateInActionWithSessionId() {
        final String sessionId = createSessionId();

        HttpServletRequest servletReq = new MockHttpServletRequest()
            .setCookies(new Cookie[]{
                    new Cookie("JSESSION_ID", "hogehoge"),
                    new Cookie("NABLARCH_SID", sessionId)
            })
            .getMockInstance();

        HttpRequest request = new HttpRequestWrapper(
            new NablarchHttpServletRequestWrapper(servletReq)
        );

        final ServletExecutionContext servCtx = new ServletExecutionContext(servletReq, httpResponse, servletContext);

        servCtx.setSessionScopedVar("nablarch_sessionStore_expiration_date", systemTimeProvider.getTimestamp().getTime());

        store.save(sessionId, new ArrayList<SessionEntry>() {{
            add(new SessionEntry("hoge", "hogeValue", store));
            add(new SessionEntry("fuga", "fugaValue", store));
        }}, servCtx);

        HttpResponse res = servCtx.addHandler(handler)
               .addHandler(new Handler<HttpRequest, HttpResponse>() {
                   @Override
                   public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                       SessionUtil.invalidate(ctx);
                       SessionUtil.put(ctx, "test-var", "test-value");
                       return new HttpResponse(200, "success");
                   }
               })
               .handleNext(request);

        assertThat("想定したステータスコードのレスポンスが返却されること", res.getStatusCode(), is(200));
        assertThat("想定したコンテンツパスのレスポンスが返却されること", res.getContentPath().getPath(), is("success"));

        assertThat("システム日時にセッションストアの有効期限(ミリ秒)を加算した値が、HTTPセッションに設定されていること",
                (Long) servCtx.getSessionScopedVar("nablarch_sessionStore_expiration_date"),
                is(systemTimeProvider.getTimestamp().getTime() + 900000L));

        final Map<String, String> holder = new HashMap<String, String>();
        new Verifications() {{
            Cookie cookie;
            httpResponse.addCookie(cookie = withCapture());
            assertThat(cookie.getName(), is("NABLARCH_SID"));
            assertThat(cookie.getMaxAge(), is(-1));
            assertThat(cookie.getValue(), is(notNullValue()));
            holder.put("sessionIdAfterInvalidate", cookie.getValue());
        }};

        List<SessionEntry> entries = store.load(sessionId, servCtx);
        assertThat(entries.size(), is(0));

        entries = store.load(holder.get("sessionIdAfterInvalidate"), servCtx);
        assertThat(entries.size(), is(1));
        assertThat(entries.get(0).getKey(), is("test-var"));
    }

    /**
     * Action内でinvalidateして、putすると、SIDが変わること。
     * セッションIDがクッキーに設定されていない場合。
     */
    @Test
    public void testRegenerateSessionIdWhenPutAfterInvalidateInActionWithoutSessionId() {

        HttpServletRequest servletReq = new MockHttpServletRequest().getMockInstance();

        HttpRequest request = new HttpRequestWrapper(
            new NablarchHttpServletRequestWrapper(servletReq)
        );

        final ServletExecutionContext servCtx = new ServletExecutionContext(servletReq, httpResponse, servletContext);

        HttpResponse res = servCtx.addHandler(handler)
               .addHandler(new Handler<HttpRequest, HttpResponse>() {
                    @Override
                    public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                        SessionUtil.invalidate(ctx);
                        SessionUtil.put(ctx, "test-var", "test-value");
                        return new HttpResponse(200, "success");
                    }
                })
               .handleNext(request);

        assertThat("想定したステータスコードのレスポンスが返却されること", res.getStatusCode(), is(200));
        assertThat("想定したコンテンツパスのレスポンスが返却されること", res.getContentPath().getPath(), is("success"));

        final Map<String, String> holder = new HashMap<String, String>();
        new Verifications() {{
            Cookie cookie;
            httpResponse.addCookie(cookie = withCapture());
            assertThat(cookie.getName(), is("NABLARCH_SID"));
            assertThat(cookie.getMaxAge(), is(-1));
            assertThat(cookie.getValue(), is(notNullValue()));
            holder.put("sessionIdAfterInvalidate", cookie.getValue());
        }};

        List<SessionEntry> entries = store.load(holder.get("sessionIdAfterInvalidate"), servCtx);
        assertThat(entries.size(), is(1));
        assertThat(entries.get(0).getKey(), is("test-var"));
    }
    
    private String createSessionId() {
    	String uuid = UUID.randomUUID().toString();
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.reset();
	    	md.update(StringUtil.getBytes(uuid, Charset.forName("UTF-8")));
	    	
	    	byte[] digest = md.digest();
	    	
	    	StringBuilder sb = new StringBuilder();
	    	int length = digest.length;
	    	for (int i = 0; i < length; i++) {
	    		int b = (0xFF & digest[i]);
	    	  	if (b < 16) {
	    	  		sb.append("0");
	    	  	}
	    	    sb.append(Integer.toHexString(b));
	    	}
	    	return uuid + sb;
		} catch (NoSuchAlgorithmException e) {
			// NOP
		}
		return null;
    }

    /**
     * 不具合の解消を確認するテスト。
     *
     * {@link HiddenStore}を使用しても、Action内でinvalidateしてputすると、
     * put後の値が設定されること。
     *
     * HttpSessionストア、Hiddenストアを使用。
     */
    @Test
    public void testRegenerateSessionIdWhenPutAfterInvalidateInActionWithSessionIdUsingHiddenStore() {

        final SessionStore httpSessionStore = store;

        final String parameterName = "test-hidden-store";
        final HiddenStore hiddenStore = new HiddenStore();
        hiddenStore.setParameterName(parameterName);
        hiddenStore.setExpires(900L);

        manager.setAvailableStores(Arrays.asList(hiddenStore, httpSessionStore));

        final String sessionId = createSessionId();

        final HttpServletRequest servletReq = new MockHttpServletRequest()
                .setCookies(new Cookie[]{
                        new Cookie("JSESSION_ID", "hogehoge"),
                        new Cookie("NABLARCH_SID", sessionId)
                })
                .getMockInstance();

        final HttpRequest request = new HttpRequestWrapper(
                new NablarchHttpServletRequestWrapper(servletReq)
        );

        final ServletExecutionContext servCtx = new ServletExecutionContext(
                servletReq, httpResponse, servletContext);

        servCtx.setSessionScopedVar("nablarch_sessionStore_expiration_date", systemTimeProvider.getTimestamp().getTime());

        httpSessionStore.save(sessionId, new ArrayList<SessionEntry>() {{
            add(new SessionEntry("test1-key", "test1-value", httpSessionStore));
        }}, servCtx);
        hiddenStore.save(sessionId, new ArrayList<SessionEntry>() {{
            add(new SessionEntry("test2-key", "test2-value", hiddenStore));
        }}, servCtx);

        HttpResponse res = servCtx.addHandler(handler)
                .addHandler(new Handler<HttpRequest, HttpResponse>() {
                    @Override
                    public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                        SessionUtil.invalidate(ctx);
                        SessionUtil.put(ctx, "test3-key", "test3-value", "test");
                        SessionUtil.put(ctx, "test4-key", "test4-value", "hidden");
                        return new HttpResponse(200, "success");
                    }
                })
                .handleNext(request);

        assertThat("想定したステータスコードのレスポンスが返却されること", res.getStatusCode(), is(200));
        assertThat("想定したコンテンツパスのレスポンスが返却されること", res.getContentPath().getPath(), is("success"));

        final Map<String, String> holder = new HashMap<String, String>();
        new Verifications() {{
            Cookie cookie;
            httpResponse.addCookie(cookie = withCapture());
            assertThat(cookie.getName(), is("NABLARCH_SID"));
            assertThat(cookie.getMaxAge(), is(-1));
            assertThat(cookie.getValue(), is(notNullValue()));
            holder.put("sessionIdAfterInvalidate", cookie.getValue());
        }};

        assertThat(httpSessionStore.load(sessionId, servCtx).size(), is(0));

        final List<SessionEntry> entriesInHttpSession
                = httpSessionStore.load(holder.get("sessionIdAfterInvalidate"), servCtx);
        assertThat(entriesInHttpSession.size(), is(1));
        assertThat(entriesInHttpSession.get(0).getKey(), is("test3-key"));
        assertThat((String) entriesInHttpSession.get(0).getValue(), is("test3-value"));

        assertThat(servCtx.getRequestScopedVar(parameterName), is(notNullValue()));
        servCtx.getHttpRequest().setParam(parameterName, servCtx.getRequestScopedVar(parameterName).toString());
        final List<SessionEntry> entriesInHidden
                = hiddenStore.load(holder.get("sessionIdAfterInvalidate"), servCtx);
        assertThat(entriesInHidden.size(), is(1));
        assertThat(entriesInHidden.get(0).getKey(), is("test4-key"));
        assertThat((String) entriesInHidden.get(0).getValue(), is("test4-value"));
    }

    /**
     * HttpSessionのみが存在している状態(SessionStoreは存在していない)で、
     * invalidateをした場合HttpSessionがinvalidateされること。
     * @throws Exception
     */
    @Test
    public void containsHttpSessionWhenInvalidateSessionStore() throws Exception {

        // -------------------------------------------------- setup
        final SessionStore hiddenStore = new HiddenStore();
        hiddenStore.setExpires(5L, TimeUnit.MINUTES);
        manager.setAvailableStores(Collections.singletonList(hiddenStore));

        // HttpSessionを持つリクエストを生成
        // NABLARCH_SIDは持たない(セッションストアに情報はない)
        final HttpServletRequest servletReq = new MockHttpServletRequest().getMockInstance();
        servletReq.getSession(true);

        final HttpRequest request = new HttpRequestWrapper(
                new NablarchHttpServletRequestWrapper(servletReq)
        );

        final ServletExecutionContext servCtx = new ServletExecutionContext(servletReq, httpResponse, servletContext);
        MockHttpServletRequest.sessionInvalidateCount = 0;

        HttpResponse res = servCtx.addHandler(handler)
                                  .addHandler(new Handler<HttpRequest, HttpResponse>() {
                                      @Override
                                      public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                                          // セッションを破棄
                                          SessionUtil.invalidate(ctx);
                                          SessionUtil.put(ctx, "key", "value", "hidden");
                                          return new HttpResponse(200, "success");
                                      }
                                  })
                                  .handleNext(request);

        assertThat("想定したステータスコードのレスポンスが返却されること", res.getStatusCode(), is(200));
        assertThat("想定したコンテンツパスのレスポンスが返却されること", res.getContentPath()
                                                   .getPath(), is("success"));
        assertThat("HttpSessionがinvalidateされること", MockHttpServletRequest.sessionInvalidateCount, is(1));
        assertThat("セッションの有効期間を示す値がセッションに設定されること",
                MockHttpServletRequest.sessionContent, IsMapContaining.hasKey("nablarch_sessionStore_expiration_date"));
    }

    /**
     * サーバサイド処理中にセッションが破棄された場合は、
     * セッションストアに対する保存が行われないこと。
     */
    @Test
    public void invalidatedHttpSession_notSaveSessionStore() throws Exception {

        // -------------------------------------------------- setup
        final SessionStore hiddenStore = new HiddenStore();
        hiddenStore.setExpires(5L, TimeUnit.MINUTES);
        manager.setAvailableStores(Collections.singletonList(hiddenStore));

        final String sessionId = createSessionId();
        final HttpServletRequest servletReq = new MockHttpServletRequest()
                .setCookies(new Cookie[] {
                        new Cookie("NABLARCH_SID", sessionId)
                })
                .getMockInstance();
        // セッションを生成してセッションの有効期間を一日後に
        servletReq.getSession(true)
                  .setAttribute("nablarch_sessionStore_expiration_date", systemTimeProvider.getDate().getTime() + TimeUnit.DAYS.toMillis(1));

        final HttpRequest request = new HttpRequestWrapper(
                new NablarchHttpServletRequestWrapper(servletReq)
        );

        final ServletExecutionContext servCtx = new ServletExecutionContext(servletReq, httpResponse, servletContext);

        HttpResponse res = servCtx.addHandler(handler)
                                  .addHandler(new Handler<HttpRequest, HttpResponse>() {
                                      @Override
                                      public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                                          // ほかスレッドでセッションが破棄されたことを再現させるために
                                          // セッションストアではなく直接HttpSessionを無効化する。
                                          servCtx.getNativeHttpSession(false).invalidate();
                                          
                                          SessionUtil.put(ctx, "key", "value", "hidden");
                                          return new HttpResponse(200, "success");
                                      }
                                  })
                                  .handleNext(request);

        assertThat("想定したステータスコードのレスポンスが返却されること", res.getStatusCode(), is(200));
        assertThat("想定したコンテンツパスのレスポンスが返却されること", res.getContentPath().getPath(), is("success"));
        assertThat("セッションストアには値が保存されないこと", servCtx.getRequestScopedVar("nablarch_hiddenStore"), nullValue());
        
    }
    
    /**
     * アクション内で追加したセッションデータを即削除した場合、
     * その値はセッションに追加されないこと。
     * 
     * @throws Exception
     */
    @Test
    public void addAndRemoveSessionEntryInAction() throws Exception {

        // -------------------------------------------------- setup
        final SessionStore hiddenStore = new HiddenStore();
        hiddenStore.setExpires(5L, TimeUnit.MINUTES);
        manager.setAvailableStores(Collections.singletonList(hiddenStore));

        // HttpSessionを持つリクエストを生成
        // NABLARCH_SIDは持たない(セッションストアに情報はない)
        final String sessionId = createSessionId();
        final HttpServletRequest servletReq = new MockHttpServletRequest()
                .setCookies(new Cookie[] {
                        new Cookie("NABLARCH_SID", sessionId)
                })
                .getMockInstance();
        servletReq.getSession(true);

        final HttpRequest request = new HttpRequestWrapper(
                new NablarchHttpServletRequestWrapper(servletReq)
        );

        final ServletExecutionContext servCtx = new ServletExecutionContext(servletReq, httpResponse, servletContext);


        HttpResponse res = servCtx.addHandler(handler)
                                  .addHandler(new Handler<HttpRequest, HttpResponse>() {
                                      @Override
                                      public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                                          // セッションに値を追加
                                          SessionUtil.put(ctx, "key1", "value1", "hidden");
                                          SessionUtil.put(ctx, "key2", "value2", "hidden");

                                          // key1は即削除
                                          SessionUtil.delete(ctx, "key1");

                                          return new HttpResponse(200, "success");
                                      }
                                  })
                                  .handleNext(request);

        assertThat("想定したステータスコードのレスポンスが返却されること", res.getStatusCode(), is(200));
        assertThat("想定したコンテンツパスのレスポンスが返却されること", res.getContentPath()
                                                   .getPath(), is("success"));
        final String storeValue = servCtx.getRequestScopedVar("nablarch_hiddenStore");
        servCtx.getHttpRequest().setParam("nablarch_hiddenStore", storeValue);
        // SessionStoreHandlerが設定するSessionIdの取得
        final List<Cookie> cookies = new ArrayList<Cookie>();
        new Verifications() {{
            httpResponse.addCookie(withCapture(cookies));
            assertNotNull("cookieが設定されていること", getSessionId(cookies));
        }};
        final List<SessionEntry> sessionEntryList = hiddenStore.load(getSessionId(cookies), servCtx);
        assertThat("key1は即削除しているので追加されているエントリーは1つだけ", sessionEntryList, IsCollectionWithSize.hasSize(1));
        assertThat("追加されているのはkey2のセッション情報のみ", sessionEntryList.get(0), allOf(
                HasPropertyWithValue.hasProperty("key", is("key2")),
                HasPropertyWithValue.hasProperty("value", is("value2"))
        ));
    }

    /**
     * CookieからセッションIDを取得する。
     *
     * @param cookies Cookieのリスト
     * @return SessionID
     */
    private String getSessionId(List<Cookie> cookies) {
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals("NABLARCH_SID")) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
