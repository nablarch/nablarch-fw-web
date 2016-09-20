package nablarch.common.web.session;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import mockit.Capturing;
import mockit.Mocked;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
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
import org.junit.runner.RunWith;

/**
 * @author kawasima
 * @author tajima
 */
@RunWith(JMockit.class)
public class SessionStoreHandlerTest {

    private HttpServletRequest httpRequest;

    @Capturing
    private HttpServletResponse httpResponse;

    @Mocked
    private ServletContext servletContext;

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

        new Verifications() {
            {
                Cookie cookie;
                httpResponse.addCookie(cookie = withCapture());
                assertThat(cookie.getDomain(), is("www.example.com"));
                assertThat(cookie.getName(), is("SESSION_TRACKING_ID"));
                assertThat(cookie.getPath(), is("/app"));
                assertThat(cookie.getMaxAge(), is(-1));
                assertThat(cookie.getSecure(), is(true));
            }
        };
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
        
        new Verifications() {
            {
                Cookie cookie = null;
                httpResponse.addCookie(cookie); times = 0;
            }
        };
    }
}
