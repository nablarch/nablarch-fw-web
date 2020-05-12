package nablarch.fw.web.handler;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.Cookie;

import nablarch.fw.web.handler.csrf.VerificationFailureHandler;
import org.apache.tools.ant.taskdefs.condition.Http;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import mockit.integration.junit4.JMockit;
import nablarch.common.web.MockHttpSession;
import nablarch.common.web.WebConfig;
import nablarch.common.web.csrf.CsrfTokenUtil;
import nablarch.common.web.session.SessionEntry;
import nablarch.common.web.session.SessionManager;
import nablarch.common.web.session.SessionStore;
import nablarch.common.web.session.SessionStoreHandler;
import nablarch.common.web.session.SessionUtil;
import nablarch.common.web.session.store.HttpSessionStore;
import nablarch.core.date.BasicSystemTimeProvider;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpRequestHandler;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.HttpResponse.Status;
import nablarch.fw.web.MockHttpRequest;
import nablarch.fw.web.handler.csrf.CsrfTokenGenerator;
import nablarch.fw.web.handler.csrf.VerificationTargetMatcher;
import nablarch.fw.web.servlet.MockServletContext;
import nablarch.fw.web.servlet.MockServletRequest;
import nablarch.fw.web.servlet.MockServletResponse;
import nablarch.fw.web.servlet.ServletExecutionContext;
import nablarch.test.support.SystemRepositoryResource;
import nablarch.test.support.log.app.OnMemoryLogWriter;

/**
 * {@link CsrfTokenVerificationHandler}のテストクラス。
 *
 */
@RunWith(JMockit.class)
public class CsrfTokenVerificationHandlerTest {

    @Rule
    public SystemRepositoryResource systemRepositoryResource = new SystemRepositoryResource(null);

    private CsrfTokenVerificationHandler sut;
    private ServletExecutionContext context;
    private SessionStoreHandler sessionStoreHandler;
    private MockServletRequest servletReq;
    private MockHttpSession session;
    private MockServletResponse servletRes;
    private WebConfig webConfig;
    private HttpSessionStore httpSessionStore;
    private InMemoryStore inMemoryStore;

    @Before
    public void init() {

        webConfig = new WebConfig();
        systemRepositoryResource.addComponent("webConfig", webConfig);

        systemRepositoryResource.addComponent("systemTimeProvider", new BasicSystemTimeProvider());

        httpSessionStore = new HttpSessionStore();
        httpSessionStore.setExpires(1L, TimeUnit.HOURS);

        inMemoryStore = new InMemoryStore();
        inMemoryStore.setExpires(1L, TimeUnit.HOURS);

        List<SessionStore> sessionStores = new ArrayList<SessionStore>();
        sessionStores.add(httpSessionStore);
        sessionStores.add(inMemoryStore);

        SessionManager sessionManager = new SessionManager();
        sessionManager.setDefaultStoreName(httpSessionStore.getName());
        sessionManager.setAvailableStores(sessionStores);
        systemRepositoryResource.addComponent("sessionManager", sessionManager);

        sessionStoreHandler = new SessionStoreHandler();
        sessionStoreHandler.setSessionManager(sessionManager);

        sut = new CsrfTokenVerificationHandler();

        session = new MockHttpSession();
        resetRequest();
    }

    private void resetRequest() {
        servletReq = new MockServletRequest();
        servletReq.setSession(session);
        servletRes = new MockServletResponse();
        MockServletContext servletCtx = new MockServletContext();
        context = new ServletExecutionContext(servletReq, servletRes, servletCtx);
        context.addHandler(sessionStoreHandler);
        context.addHandler(sut);

        OnMemoryLogWriter.clear();
    }

    /**
     * CSRFトークンが生成されることをテストする。
     */
    @Test
    public void testGenerateCsrfToken() {
        context.addHandler(new GetCsrfToken());
        HttpResponse resp = context.handleNext(new MockHttpRequest("GET / HTTP/1.1"));

        assertNotNull(resp.getBodyString());
        UUID.fromString(resp.getBodyString());
    }

    /**
     * 生成済みのトークンを読み出せることをテストする。
     */
    @Test
    public void testReadExistingCsrfToken() {
        context.addHandler(new GetCsrfToken());
        HttpResponse resp1 = context.handleNext(new MockHttpRequest("GET / HTTP/1.1"));
        List<Cookie> cookies = servletRes.getCookies();
        String csrfToken = resp1.getBodyString();

        resetRequest();

        servletReq.setCookies(cookies.toArray(new Cookie[0]));
        context.addHandler(new GetCsrfToken());
        HttpResponse resp2 = context.handleNext(new MockHttpRequest("GET / HTTP/1.1"));

        assertEquals(csrfToken, resp2.getBodyString());
    }

    /**
     * リクエストパラメータで送信したCSRFトークンの検証が成功することをテストする。
     */
    @Test
    public void testVerifyCsrfTokenWithParameter() {
        context.addHandler(new GetCsrfToken());
        HttpResponse resp1 = context.handleNext(new MockHttpRequest("GET / HTTP/1.1"));
        List<Cookie> cookies = servletRes.getCookies();
        String csrfToken = resp1.getBodyString();

        resetRequest();

        servletReq.setCookies(cookies.toArray(new Cookie[0]));
        context.addHandler(new OK());
        HttpResponse resp2 = context.handleNext(new MockHttpRequest("POST / HTTP/1.1")
                .setParam(webConfig.getCsrfTokenParameterName(), csrfToken));

        assertEquals(OK.RESPONSE, resp2);
    }

    /**
     * リクエストヘッダで送信したCSRFトークンの検証が成功することをテストする。
     */
    @Test
    public void testVerifyCsrfTokenWithHeader() {
        context.addHandler(new GetCsrfToken());
        HttpResponse resp1 = context.handleNext(new MockHttpRequest("GET / HTTP/1.1"));
        List<Cookie> cookies = servletRes.getCookies();
        String csrfToken = resp1.getBodyString();

        resetRequest();

        servletReq.setCookies(cookies.toArray(new Cookie[0]));
        context.addHandler(new OK());
        HttpResponse resp2 = context.handleNext(new MockHttpRequest("POST / HTTP/1.1").setHeaderMap(
                Collections.singletonMap(webConfig.getCsrfTokenHeaderName(), csrfToken)));

        assertEquals(OK.RESPONSE, resp2);
    }

    /**
     * CSRFトークンの検証が失敗することをテストする。
     */
    @Test
    public void testInvalidCsrfToken() {
        context.addHandler(new GetCsrfToken());
        context.handleNext(new MockHttpRequest("GET / HTTP/1.1"));
        List<Cookie> cookies = servletRes.getCookies();

        resetRequest();

        servletReq.setCookies(cookies.toArray(new Cookie[0]));
        context.addHandler(new AlwaysFailure());
        String invalidCsrfToken = "invalid csrf token";

        HttpResponse response = context.handleNext(new MockHttpRequest("POST / HTTP/1.1")
                .setParam(webConfig.getCsrfTokenParameterName(), invalidCsrfToken));
        assertEquals(400, response.getStatusCode());
        assertTrue(response.isBodyEmpty());
    }

    /**
     * リクエストパラメータ名のみが送信されてCSRFトークンの検証が失敗することをテストする。
     */
    @Test
    public void testInvalidCsrfTokenWithHttpRequestParameterNameOnly() {
        context.addHandler(new GetCsrfToken());
        context.handleNext(new MockHttpRequest("GET / HTTP/1.1"));
        List<Cookie> cookies = servletRes.getCookies();

        resetRequest();

        servletReq.setCookies(cookies.toArray(new Cookie[0]));
        context.addHandler(new AlwaysFailure());
        HttpResponse response = context.handleNext(new MockHttpRequest("POST / HTTP/1.1")
                    .setParam(webConfig.getCsrfTokenParameterName(), new String[0]));
        assertEquals(400, response.getStatusCode());
        assertTrue(response.isBodyEmpty());
    }

    /**
     * リクエストパラメータ名をカスタマイズできることをテストする。
     */
    @Test
    public void testCustomizeParameterName() {
        String customizedName = "cutomizedName";
        webConfig.setCsrfTokenParameterName(customizedName);

        context.addHandler(new GetCsrfToken());
        HttpResponse resp1 = context.handleNext(new MockHttpRequest("GET / HTTP/1.1"));
        List<Cookie> cookies = servletRes.getCookies();
        String csrfToken = resp1.getBodyString();

        resetRequest();

        servletReq.setCookies(cookies.toArray(new Cookie[0]));
        context.addHandler(new GetRequestParam(customizedName));
        HttpResponse resp2 = context.handleNext(
                new MockHttpRequest("POST / HTTP/1.1").setParam(customizedName, csrfToken));

        assertEquals(csrfToken, resp2.getBodyString());
    }

    /**
     * リクエストヘッダ名をカスタマイズできることをテストする。
     */
    @Test
    public void testCustomizeHeaderName() {
        String customizedName = "cutomizedName";
        webConfig.setCsrfTokenHeaderName(customizedName);

        context.addHandler(new GetCsrfToken());
        HttpResponse resp1 = context.handleNext(new MockHttpRequest("GET / HTTP/1.1"));
        List<Cookie> cookies = servletRes.getCookies();
        String csrfToken = resp1.getBodyString();

        resetRequest();

        servletReq.setCookies(cookies.toArray(new Cookie[0]));
        context.addHandler(new GetRequestHeader(customizedName));
        HttpResponse resp2 = context.handleNext(new MockHttpRequest("POST / HTTP/1.1")
                .setHeaderMap(Collections.singletonMap(customizedName, csrfToken)));

        assertEquals(csrfToken, resp2.getBodyString());
    }

    /**
     * セッションへ保存するときの名前をカスタマイズできることをテストする。
     */
    @Test
    public void testCustomizeSessionStoredVarName() {
        String customizedName = "cutomizedName";
        webConfig.setCsrfTokenSessionStoredVarName(customizedName);

        context.addHandler(new GetCsrfToken());
        HttpResponse resp1 = context.handleNext(new MockHttpRequest("GET / HTTP/1.1"));
        List<Cookie> cookies = servletRes.getCookies();
        String csrfToken = resp1.getBodyString();

        resetRequest();

        servletReq.setCookies(cookies.toArray(new Cookie[0]));
        context.addHandler(new GetSessionStoredVar(customizedName));
        HttpResponse resp2 = context.handleNext(new MockHttpRequest("POST / HTTP/1.1")
                .setParam(webConfig.getCsrfTokenParameterName(), csrfToken));

        assertEquals(csrfToken, resp2.getBodyString());
    }

    /**
     * {@link CsrfTokenGenerator}の実装を切り替えられることをテストする。
     */
    @Test
    public void testSetCsrfTokenGenerator() {
        final String customizedCsrfToken = "customized csrf token";
        CsrfTokenGenerator csrfTokenGenerator = new CsrfTokenGenerator() {
            @Override
            public String generateToken() {
                return customizedCsrfToken;
            }
        };
        sut.setCsrfTokenGenerator(csrfTokenGenerator);

        context.addHandler(new GetCsrfToken());
        HttpResponse resp = context.handleNext(new MockHttpRequest("GET / HTTP/1.1"));

        assertEquals(customizedCsrfToken, resp.getBodyString());
    }

    /**
     * {@link VerificationTargetMatcher}の実装を切り替えられることをテストする。
     */
    @Test
    public void testSetVerificationTargetMatcher() {

        //必ず対象にならないVerificationTargetMatcherを設定してPOSTリクエストをテストする
        VerificationTargetMatcher verificationTargetMatcher1 = new VerificationTargetMatcher() {
            @Override
            public boolean match(HttpRequest request) {
                return false;
            }
        };
        sut.setVerificationTargetMatcher(verificationTargetMatcher1);

        context.addHandler(new OK());
        HttpResponse resp1 = context
                .handleNext(new MockHttpRequest("POST / HTTP/1.1").setParam("data", "test"));

        assertEquals(OK.RESPONSE, resp1);

        resetRequest();

        //必ず対象になるVerificationTargetMatcherを設定してGETリクエストをテストする
        VerificationTargetMatcher verificationTargetMatcher2 = new VerificationTargetMatcher() {
            @Override
            public boolean match(HttpRequest request) {
                return true;
            }
        };
        sut.setVerificationTargetMatcher(verificationTargetMatcher2);

        context.addHandler(new AlwaysFailure());
        HttpResponse response = context.handleNext(new MockHttpRequest("GET / HTTP/1.1"));
        assertEquals(400, response.getStatusCode());
        assertTrue(response.isBodyEmpty());
    }

    /**
     * CSRFトークンがデフォルトのセッションストアへ保存されることをテストする。
     */
    @Test
    public void testCsrfTokenSavedInDefaultStore() {
        context.addHandler(new OK());
        context.handleNext(new MockHttpRequest("GET / HTTP/1.1"));

        String sessionId = null;
        for (Cookie cookie : servletRes.getCookies()) {
            if (cookie.getName().equals("NABLARCH_SID")) {
                sessionId = cookie.getValue();
                break;
            }
        }
        assertNotNull(sessionId);

        String savedCsrfToken = null;
        List<SessionEntry> entries = httpSessionStore.load(sessionId, context);
        for (SessionEntry entry : entries) {
            if (entry.getKey().equals(webConfig.getCsrfTokenSessionStoredVarName())) {
                savedCsrfToken = (String) entry.getValue();
                break;
            }
        }
        assertNotNull(savedCsrfToken);
    }

    /**
     * CSRFトークンが指定されたセッションストアへ保存されることをテストする。
     */
    @Test
    public void testCsrfTokenSavedInSpecifiedStore() {
        webConfig.setCsrfTokenSavedStoreName(inMemoryStore.getName());

        context.addHandler(new OK());
        context.handleNext(new MockHttpRequest("GET / HTTP/1.1"));

        String sessionId = null;
        for (Cookie cookie : servletRes.getCookies()) {
            if (cookie.getName().equals("NABLARCH_SID")) {
                sessionId = cookie.getValue();
                break;
            }
        }
        assertNotNull(sessionId);

        String savedCsrfToken = null;
        List<SessionEntry> entries = inMemoryStore.load(sessionId, context);
        for (SessionEntry entry : entries) {
            if (entry.getKey().equals(webConfig.getCsrfTokenSessionStoredVarName())) {
                savedCsrfToken = (String) entry.getValue();
                break;
            }
        }
        assertNotNull(savedCsrfToken);
    }

    /**
     * リクエストスコープへ特定のキーでBoolean.TRUEを設定すればCSRFトークンが再生成されることをテストする。
     */
    @Test
    public void testRegenerateCsrfToken() {
        context.addHandler(new OK());
        context.handleNext(new MockHttpRequest("GET / HTTP/1.1"));
        List<Cookie> cookies = servletRes.getCookies();
        String csrfToken1 = CsrfTokenUtil.getCsrfToken(context);
        assertNotNull("CSRFトークンを取得できない", csrfToken1);

        resetRequest();

        servletReq.setCookies(cookies.toArray(new Cookie[0]));
        //Boolean.TRUEを設定する
        context.addHandler(new SetRequestScopedVar(
                CsrfTokenVerificationHandler.REQUEST_REGENERATE_KEY, Boolean.TRUE));
        context.handleNext(new MockHttpRequest("GET / HTTP/1.1"));
        String csrfToken2 = CsrfTokenUtil.getCsrfToken(context);
        assertNotNull("再生成後にCSRFトークンを取得できない", csrfToken2);

        assertNotEquals("再生成前後でCSRFトークンが同じ値", csrfToken1, csrfToken2);
    }

    /**
     * リクエストスコープへ特定のキーでBoolean.TRUE以外を設定するとCSRFトークンの再生成は行われないことをテストする。
     */
    @Test
    public void testInvalidRegenerateKey() {
        context.addHandler(new OK());
        context.handleNext(new MockHttpRequest("GET / HTTP/1.1"));
        List<Cookie> cookies = servletRes.getCookies();
        String csrfToken1 = CsrfTokenUtil.getCsrfToken(context);
        assertNotNull("CSRFトークンを取得できない", csrfToken1);

        resetRequest();

        servletReq.setCookies(cookies.toArray(new Cookie[0]));
        //Boolean.TRUEではなくてnew Object()を設定する
        context.addHandler(new SetRequestScopedVar(
                CsrfTokenVerificationHandler.REQUEST_REGENERATE_KEY, new Object()));
        context.handleNext(new MockHttpRequest("GET / HTTP/1.1"));
        String csrfToken2 = CsrfTokenUtil.getCsrfToken(context);

        assertEquals("CSRFトークンが再生成されている", csrfToken1, csrfToken2);

        //警告ログが出力されていることを検証
        OnMemoryLogWriter.assertLogContains("writer.memory",
                "A request scoped variable named 'nablarch_request_for_csrf_token_to_be_regenerated' has unexpected value. 'nablarch_request_for_csrf_token_to_be_regenerated' must be Bolean.TRUE.");
    }

    /**
     *  {@link VerificationFailureHandler}の実装を切り替えられることをテストする。
     */
    @Test
    public void testSetVerificationFailureHandler() {

        VerificationFailureHandler handler = new VerificationFailureHandler() {
            @Override
            public HttpResponse handle(
                    HttpRequest request, ExecutionContext context,
                    String userSentToken, String sessionAssociatedToken) {
                assertNotNull(request);
                assertNotNull(context);
                return new HttpResponse(200).write(userSentToken + sessionAssociatedToken);
            }
        };
        sut.setVerificationFailureHandler(handler);

        context.addHandler(new GetCsrfToken());
        HttpResponse response1 = context.handleNext(new MockHttpRequest("GET / HTTP/1.1"));
        List<Cookie> cookies = servletRes.getCookies();

        resetRequest();

        servletReq.setCookies(cookies.toArray(new Cookie[0]));
        context.addHandler(new AlwaysFailure());
        String invalidCsrfToken = "invalid csrf token";
        String expectedBodyString = invalidCsrfToken + response1.getBodyString();

        HttpResponse response2 = context.handleNext(new MockHttpRequest("POST / HTTP/1.1")
                .setParam(webConfig.getCsrfTokenParameterName(), invalidCsrfToken));
        assertEquals(200, response2.getStatusCode());
        assertEquals(expectedBodyString, response2.getBodyString());
    }

    private static class GetCsrfToken implements HttpRequestHandler {
        @Override
        public HttpResponse handle(HttpRequest request, ExecutionContext context) {
            String csrfToken = CsrfTokenUtil.getCsrfToken(context);
            assertNotNull("CSRFトークンがnull", csrfToken);
            return new HttpResponse(Status.OK.getStatusCode()).write(csrfToken);
        }
    }

    private static class GetRequestParam implements HttpRequestHandler {

        private String name;

        public GetRequestParam(String name) {
            this.name = name;
        }

        @Override
        public HttpResponse handle(HttpRequest request, ExecutionContext context) {
            String[] params = request.getParam(name);
            assertNotNull("リクエストパラメータ" + name + "がnull", params);
            assertEquals("リクエストパラメータ" + name + "のサイズが1ではない", 1, params.length);
            return new HttpResponse(Status.OK.getStatusCode()).write(params[0]);
        }
    }

    private static class GetRequestHeader implements HttpRequestHandler {

        private String name;

        public GetRequestHeader(String name) {
            this.name = name;
        }

        @Override
        public HttpResponse handle(HttpRequest request, ExecutionContext context) {
            String header = request.getHeader(name);
            assertNotNull("リクエストヘッダ" + name + "がnull", header);
            return new HttpResponse(Status.OK.getStatusCode()).write(header);
        }
    }

    private static class GetSessionStoredVar implements HttpRequestHandler {

        private String name;

        public GetSessionStoredVar(String name) {
            this.name = name;
        }

        @Override
        public HttpResponse handle(HttpRequest request, ExecutionContext context) {
            String value = SessionUtil.orNull(context, name);
            assertNotNull("セッションストアの値" + name + "がnull", value);
            return new HttpResponse(Status.OK.getStatusCode()).write(value);
        }
    }

    private static class OK implements HttpRequestHandler {

        static final HttpResponse RESPONSE = new HttpResponse();

        @Override
        public HttpResponse handle(HttpRequest request, ExecutionContext context) {
            return RESPONSE;
        }
    }

    private static class AlwaysFailure implements HttpRequestHandler {
        @Override
        public HttpResponse handle(HttpRequest request, ExecutionContext context) {
            fail("ここに到達してはいけない");
            return null;
        }
    }

    private static class SetRequestScopedVar implements HttpRequestHandler {

        private String name;
        private Object value;

        public SetRequestScopedVar(String name, Object value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public HttpResponse handle(HttpRequest request, ExecutionContext context) {
            context.setRequestScopedVar(name, value);
            return new HttpResponse();
        }
    }

    private static class InMemoryStore extends SessionStore {

        private final Map<String, List<SessionEntry>> data = new HashMap<String, List<SessionEntry>>();

        public InMemoryStore() {
            super("inMemory");
        }

        @Override
        public List<SessionEntry> load(String sessionId, ExecutionContext executionContext) {
            List<SessionEntry> entries = data.get(sessionId);
            if (entries != null) {
                return entries;
            }
            return Collections.emptyList();
        }

        @Override
        public void save(String sessionId, List<SessionEntry> entries,
                ExecutionContext executionContext) {
            data.put(sessionId, entries);
        }

        @Override
        public void delete(String sessionId, ExecutionContext executionContext) {
            data.remove(sessionId);
        }

        @Override
        public void invalidate(String sessionId, ExecutionContext executionContext) {
            data.remove(sessionId);
        }
    }
}
