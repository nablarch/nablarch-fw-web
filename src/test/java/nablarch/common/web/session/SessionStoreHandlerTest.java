package nablarch.common.web.session;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import nablarch.common.web.session.store.HiddenStore;
import nablarch.core.repository.ObjectLoader;
import nablarch.core.repository.SystemRepository;
import nablarch.fw.ExecutionContext;
import nablarch.fw.Handler;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.servlet.HttpRequestWrapper;
import nablarch.fw.web.servlet.NablarchHttpServletRequestWrapper;
import nablarch.fw.web.servlet.ServletExecutionContext;
import nablarch.test.FixedSystemTimeProvider;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * @author kawasima
 * @author tajima
 */
public class SessionStoreHandlerTest {

    private HttpServletRequest httpRequest;

    private final HttpServletResponse httpResponse = mock(HttpServletResponse.class);

    private final ServletContext servletContext = mock(ServletContext.class);

    private FixedSystemTimeProvider systemTimeProvider = new FixedSystemTimeProvider() {{
        setFixedDate("20161231235959999");
    }};

    private static class UserDto implements Serializable {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @Before
    public void setUp() {
        SystemRepository.load(new ObjectLoader() {
            @Override
            public Map<String, Object> load() {
                Map<String, Object> repos = new HashMap<String, Object>();
                SessionManager sessionManager = new SessionManager();
                List<SessionStore> sessionStores = new ArrayList<SessionStore>();
                HiddenStore hiddenStore = new HiddenStore();
                hiddenStore.setName("test");
                hiddenStore.setParameterName("hidden-dayo");
                hiddenStore.setExpires(900L);
                sessionStores.add(hiddenStore);
                sessionManager.setDefaultStoreName("test");
                sessionManager.setAvailableStores(sessionStores);

                SessionStoreHandler sessionStoreHandler = new SessionStoreHandler();
                sessionStoreHandler.setSessionManager(sessionManager);
                sessionStoreHandler.setCookieDomain("www.example.com");
                sessionStoreHandler.setCookiePath("/app");
                sessionStoreHandler.setCookieName("SESSION_TRACKING_ID");
                sessionStoreHandler.setCookieSecure(true);
                sessionStoreHandler.setExpiration(new HttpSessionManagedExpiration());

                repos.put("sessionManager", sessionManager);
                repos.put("sessionStoreHandler", sessionStoreHandler);
                repos.put("systemTimeProvider", systemTimeProvider);

                return repos;
            }
        });

        httpRequest = new MockHttpServletRequest()
                .setCookies(new Cookie[]{ new Cookie("JSESSION_ID", "hogehoge")})
                .getMockInstance();
    }

    @Test
    public void testCreateSession() {
        final SessionStoreHandler handler = SystemRepository.get("sessionStoreHandler");

        HttpRequest request = new HttpRequestWrapper(
                new NablarchHttpServletRequestWrapper(httpRequest));


        // アクション的なハンドラを追加し、その中でUserDtoをセッションに入れる。
        ExecutionContext ctx = new ServletExecutionContext(httpRequest, httpResponse, servletContext)
                .addHandler(new Handler<HttpRequest, HttpResponse>() {
                    @Override
                    public HttpResponse handle(HttpRequest request, ExecutionContext context) {
                        UserDto userDto = new UserDto();
                        SessionUtil.put(context, "userDto", userDto);
                        return new HttpResponse("/");
                    }
                });

        handler.handle(request, ctx);

        assertThat("システム日時にセッションストアの有効期限(ミリ秒)を加算した値が、HTTPセッションに設定されていること",
                (Long) ctx.getSessionScopedVar("nablarch_sessionStore_expiration_date"),
                is(systemTimeProvider.getTimestamp().getTime() + 900000L));

        final ArgumentCaptor<Cookie> captor = ArgumentCaptor.forClass(Cookie.class);
        verify(httpResponse, atLeastOnce()).addCookie(captor.capture());

        final Cookie cookie = captor.getValue();
        assertThat(cookie.getDomain(), is("www.example.com"));
        assertThat(cookie.getName(), is("SESSION_TRACKING_ID"));
        assertThat(cookie.getPath(), is("/app"));
        assertThat(cookie.getMaxAge(), is(-1));
        assertThat(cookie.getSecure(), is(true));
    }

    @Test
    public void testNotCreateSession() {
        final SessionStoreHandler handler = SystemRepository.get("sessionStoreHandler");
        HttpRequest request = new HttpRequestWrapper(
                new NablarchHttpServletRequestWrapper(httpRequest));

        // アクションは何もしない。
        ExecutionContext ctx = new ServletExecutionContext(httpRequest, httpResponse, servletContext)
                .addHandler(new Handler<HttpRequest, HttpResponse>() {
                    @Override
                    public HttpResponse handle(HttpRequest request, ExecutionContext context) {
                        return new HttpResponse("/");
                    }
                });

        handler.handle(request, ctx);
        
        verify(httpResponse, never()).addCookie(isNull());
    }

    @Test
    public void testSaveSessionStoreIdWhenIdIsAvailable() {
        SessionStoreHandler handler = SystemRepository.get("sessionStoreHandler");
        String sessionStoreId = "test-session-store-id";

        httpRequest = new MockHttpServletRequest()
                .setCookies(new Cookie[]{ new Cookie("SESSION_TRACKING_ID", sessionStoreId)})
                .getMockInstance();

        ExecutionContext context = new ServletExecutionContext(httpRequest, httpResponse, servletContext)
                .addHandler(new NoopHandler());
        context.setSessionScopedVar("nablarch_sessionStore_expiration_date", System.currentTimeMillis());

        handler.handle(null, context);

        assertThat(InternalSessionUtil.getId(context), is(sessionStoreId));
    }

    @Test
    public void testSaveSessionStoreIdWhenIdIsUnavailable() {
        SessionStoreHandler handler = SystemRepository.get("sessionStoreHandler");

        ExecutionContext context = new ServletExecutionContext(httpRequest, httpResponse, servletContext)
                .addHandler(new NoopHandler());

        handler.handle(null, context);

        assertThat(InternalSessionUtil.getId(context), is(nullValue()));
    }

    /**
     * 何も処理をしないハンドラ。
     */
    private static class NoopHandler implements Handler<Object, Object> {
        @Override
        public Object handle(Object o, ExecutionContext context) {
            return null;
        }
    }
}
