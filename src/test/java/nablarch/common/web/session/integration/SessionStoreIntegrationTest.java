package nablarch.common.web.session.integration;

import mockit.Expectations;
import mockit.Mocked;
import nablarch.common.web.session.SessionManager;
import nablarch.common.web.session.SessionStoreHandler;
import nablarch.common.web.session.SessionUtil;
import nablarch.common.web.session.store.HiddenStore;
import nablarch.common.web.session.store.HttpSessionStore;
import nablarch.core.ThreadContext;
import nablarch.core.date.BasicSystemTimeProvider;
import nablarch.fw.ExecutionContext;
import nablarch.fw.handler.GlobalErrorHandler;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.handler.HttpCharacterEncodingHandler;
import nablarch.fw.web.handler.HttpResponseHandler;
import nablarch.test.support.SystemRepositoryResource;
import nablarch.test.support.db.helper.DatabaseTestRunner;
import nablarch.test.support.db.helper.VariousDbTestHelper;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;

/**
 * セッションストア機能の結合テスト。
 */
@RunWith(DatabaseTestRunner.class)
public class SessionStoreIntegrationTest {

    @Rule
    public SystemRepositoryResource systemRepository = new SystemRepositoryResource("db-default.xml");

    @Rule
    public HttpServerResource httpServer
            = new HttpServerResource("classpath://nablarch/common/web/session/integration/jsp");

    @BeforeClass
    public static void setUpClass() {
        VariousDbTestHelper.createTable(UserSession.class);
    }

    @AfterClass
    public static void tearDownClass() {
        VariousDbTestHelper.dropTable(UserSession.class);
    }

    @Before
    public void setUp() {
        ThreadContext.clear();

        final HiddenStore hiddenStore = new HiddenStore();
        hiddenStore.setExpires(300L);

        final HttpSessionStore httpSessionStore = new HttpSessionStore();
        httpSessionStore.setExpires(300L);

        final SessionManager sessionManager = new SessionManager();
        sessionManager.setAvailableStores(Arrays.asList(hiddenStore, httpSessionStore));
        sessionManager.setDefaultStoreName("hidden");

        systemRepository.addComponent("sessionManager", sessionManager);
        systemRepository.addComponent("systemTimeProvider", new BasicSystemTimeProvider());

        final SessionStoreHandler sessionStoreHandler = new SessionStoreHandler();
        sessionStoreHandler.setSessionManager(sessionManager);

        httpServer
                .addHandler(new GlobalErrorHandler())
                .addHandler(new HttpCharacterEncodingHandler())
                .addHandler(new HttpResponseHandler())
                .addHandler(sessionStoreHandler);

        final HttpResponse unused = new HttpResponse();
        new Expectations(unused) {
            {
                HttpResponse.parse(anyString);
                minTimes = 0;
                HttpResponse.parse((byte[]) withNotNull());
                minTimes = 0;
            }
        };
    }

    /**
     * 保存した値を取得できること。
     */
    @Test
    public void testSaveAndGet() {
        httpServer
                .addTestHandler(new HttpServerResource.TestHandler() {
                    @Override
                    public HttpResponse handle(final HttpRequest request, final ExecutionContext context) {

                        final String desc = "値を保存する";
                        // hidden
                        SessionUtil.put(context, "hidden-name1", "hidden-value1");
                        SessionUtil.put(context, "hidden-name2", "hidden-value2");
                        SessionUtil.put(context, "hidden-name3", "hidden-value3");
                        // httpSession
                        SessionUtil.put(context, "httpSession-name1", "httpSession-value1", "httpSession");
                        SessionUtil.put(context, "httpSession-name2", "httpSession-value2", "httpSession");
                        SessionUtil.put(context, "httpSession-name3", "httpSession-value3", "httpSession");

                        final String msg = "保存した値を取得できること";
                        // hidden
                        assertThat(msg, SessionUtil.get(context, "hidden-name1").toString(), is("hidden-value1"));
                        assertThat(msg, SessionUtil.get(context, "hidden-name2").toString(), is("hidden-value2"));
                        assertThat(msg, SessionUtil.get(context, "hidden-name3").toString(), is("hidden-value3"));
                        // httpSession
                        assertThat(msg, SessionUtil.get(context, "httpSession-name1").toString(), is("httpSession-value1"));
                        assertThat(msg, SessionUtil.get(context, "httpSession-name2").toString(), is("httpSession-value2"));
                        assertThat(msg, SessionUtil.get(context, "httpSession-name3").toString(), is("httpSession-value3"));

                        return new HttpResponse("hiddenStore.jsp");
                    }
                })
                .addTestHandler(new HttpServerResource.TestHandler() {
                    @Override
                    public HttpResponse handle(final HttpRequest request, final ExecutionContext context) {

                        final String msg = "前のリクエストで保存した値を取得できること";
                        // hidden
                        assertThat(msg, SessionUtil.get(context, "hidden-name1").toString(), is("hidden-value1"));
                        assertThat(msg, SessionUtil.get(context, "hidden-name2").toString(), is("hidden-value2"));
                        assertThat(msg, SessionUtil.get(context, "hidden-name3").toString(), is("hidden-value3"));
                        // httpSession
                        assertThat(msg, SessionUtil.get(context, "httpSession-name1").toString(), is("httpSession-value1"));
                        assertThat(msg, SessionUtil.get(context, "httpSession-name2").toString(), is("httpSession-value2"));
                        assertThat(msg, SessionUtil.get(context, "httpSession-name3").toString(), is("httpSession-value3"));

                        return new HttpResponse("hiddenStore.jsp");
                    }
                })
                .addTestHandler(new HttpServerResource.TestHandler() {
                    @Override
                    public HttpResponse handle(final HttpRequest request, final ExecutionContext context) {

                        final String msg = "リクエストを跨いでも保存した値を取得できること";
                        // hidden
                        assertThat(msg, SessionUtil.get(context, "hidden-name1").toString(), is("hidden-value1"));
                        assertThat(msg, SessionUtil.get(context, "hidden-name2").toString(), is("hidden-value2"));
                        assertThat(msg, SessionUtil.get(context, "hidden-name3").toString(), is("hidden-value3"));
                        // httpSession
                        assertThat(msg, SessionUtil.get(context, "httpSession-name1").toString(), is("httpSession-value1"));
                        assertThat(msg, SessionUtil.get(context, "httpSession-name2").toString(), is("httpSession-value2"));
                        assertThat(msg, SessionUtil.get(context, "httpSession-name3").toString(), is("httpSession-value3"));

                        return new HttpResponse("hiddenStore.jsp");
                    }
                })
                .addTestHandler(new HttpServerResource.TestHandler() {
                    @Override
                    public HttpResponse handle(final HttpRequest request, final ExecutionContext context) {

                        final String desc = "業務アプリからセッションストアにアクセスしない";

                        return new HttpResponse("hiddenStore.jsp");
                    }
                })
                .addTestHandler(new HttpServerResource.TestHandler() {
                    @Override
                    public HttpResponse handle(final HttpRequest request, final ExecutionContext context) {

                        final String msg = "前のリクエストでアクセスしなくても保存した値を取得できること";
                        // hidden
                        assertThat(msg, SessionUtil.get(context, "hidden-name1").toString(), is("hidden-value1"));
                        assertThat(msg, SessionUtil.get(context, "hidden-name2").toString(), is("hidden-value2"));
                        assertThat(msg, SessionUtil.get(context, "hidden-name3").toString(), is("hidden-value3"));
                        // httpSession
                        assertThat(msg, SessionUtil.get(context, "httpSession-name1").toString(), is("httpSession-value1"));
                        assertThat(msg, SessionUtil.get(context, "httpSession-name2").toString(), is("httpSession-value2"));
                        assertThat(msg, SessionUtil.get(context, "httpSession-name3").toString(), is("httpSession-value3"));

                        return new HttpResponse(200);
                    }
                })
                .test();
    }

    /**
     * 保存した値を削除できること。
     */
    @Test
    public void testDelete() {
        httpServer
                .addTestHandler(new HttpServerResource.TestHandler() {
                    @Override
                    public HttpResponse handle(final HttpRequest request, final ExecutionContext context) {

                        final String desc = "値を保存する";
                        // hidden
                        SessionUtil.put(context, "hidden-name1", "hidden-value1");
                        SessionUtil.put(context, "hidden-name2", "hidden-value2");
                        SessionUtil.put(context, "hidden-name3", "hidden-value3");
                        // httpSession
                        SessionUtil.put(context, "httpSession-name1", "httpSession-value1", "httpSession");
                        SessionUtil.put(context, "httpSession-name2", "httpSession-value2", "httpSession");
                        SessionUtil.put(context, "httpSession-name3", "httpSession-value3", "httpSession");

                        return new HttpResponse("hiddenStore.jsp");
                    }
                })
                .addTestHandler(new HttpServerResource.TestHandler() {
                    @Override
                    public HttpResponse handle(final HttpRequest request, final ExecutionContext context) {

                        final String msg = "前のリクエストで保存した値を取得できること";
                        // hidden
                        assertThat(msg, SessionUtil.get(context, "hidden-name1").toString(), is("hidden-value1"));
                        assertThat(msg, SessionUtil.get(context, "hidden-name2").toString(), is("hidden-value2"));
                        assertThat(msg, SessionUtil.get(context, "hidden-name3").toString(), is("hidden-value3"));
                        // httpSession
                        assertThat(msg, SessionUtil.get(context, "httpSession-name1").toString(), is("httpSession-value1"));
                        assertThat(msg, SessionUtil.get(context, "httpSession-name2").toString(), is("httpSession-value2"));
                        assertThat(msg, SessionUtil.get(context, "httpSession-name3").toString(), is("httpSession-value3"));

                        return new HttpResponse("hiddenStore.jsp");
                    }
                })
                .addTestHandler(new HttpServerResource.TestHandler() {
                    @Override
                    public HttpResponse handle(final HttpRequest request, final ExecutionContext context) {

                        final String desc = "保存した値を適当に削除する";
                        // hidden
                        SessionUtil.delete(context, "hidden-name1");
                        // httpSession
                        SessionUtil.delete(context, "httpSession-name2");

                        final String msg = "削除した値だけが取得できないこと";
                        // hidden
                        assertThat(msg, SessionUtil.orNull(context, "hidden-name1"), is(nullValue()));
                        assertThat(msg, SessionUtil.get(context, "hidden-name2").toString(), is("hidden-value2"));
                        assertThat(msg, SessionUtil.get(context, "hidden-name3").toString(), is("hidden-value3"));
                        // httpSession
                        assertThat(msg, SessionUtil.get(context, "httpSession-name1").toString(), is("httpSession-value1"));
                        assertThat(msg, SessionUtil.orNull(context, "httpSession-name2"), is(nullValue()));
                        assertThat(msg, SessionUtil.get(context, "httpSession-name3").toString(), is("httpSession-value3"));

                        return new HttpResponse("hiddenStore.jsp");
                    }
                })
                .addTestHandler(new HttpServerResource.TestHandler() {
                    @Override
                    public HttpResponse handle(final HttpRequest request, final ExecutionContext context) {

                        final String msg = "前のリクエストで削除した値だけが取得できないこと";
                        // hidden
                        assertThat(msg, SessionUtil.orNull(context, "hidden-name1"), is(nullValue()));
                        assertThat(msg, SessionUtil.get(context, "hidden-name2").toString(), is("hidden-value2"));
                        assertThat(msg, SessionUtil.get(context, "hidden-name3").toString(), is("hidden-value3"));
                        // httpSession
                        assertThat(msg, SessionUtil.get(context, "httpSession-name1").toString(), is("httpSession-value1"));
                        assertThat(msg, SessionUtil.orNull(context, "httpSession-name2"), is(nullValue()));
                        assertThat(msg, SessionUtil.get(context, "httpSession-name3").toString(), is("httpSession-value3"));

                        return new HttpResponse("hiddenStore.jsp");
                    }
                })
                .addTestHandler(new HttpServerResource.TestHandler() {
                    @Override
                    public HttpResponse handle(final HttpRequest request, final ExecutionContext context) {

                        final String desc = "もう1回保存した値を適当に削除する";
                        // hidden
                        SessionUtil.delete(context, "hidden-name3");
                        // httpSession
                        SessionUtil.delete(context, "httpSession-name1");

                        final String msg = "削除した値だけが取得できないこと";
                        // hidden
                        assertThat(msg, SessionUtil.orNull(context, "hidden-name1"), is(nullValue()));
                        assertThat(msg, SessionUtil.get(context, "hidden-name2").toString(), is("hidden-value2"));
                        assertThat(msg, SessionUtil.orNull(context, "hidden-name3"), is(nullValue()));
                        // httpSession
                        assertThat(msg, SessionUtil.orNull(context, "httpSession-name1"), is(nullValue()));
                        assertThat(msg, SessionUtil.orNull(context, "httpSession-name2"), is(nullValue()));
                        assertThat(msg, SessionUtil.get(context, "httpSession-name3").toString(), is("httpSession-value3"));

                        return new HttpResponse("hiddenStore.jsp");
                    }
                })
                .addTestHandler(new HttpServerResource.TestHandler() {
                    @Override
                    public HttpResponse handle(final HttpRequest request, final ExecutionContext context) {

                        final String msg = "前のリクエストまでに削除した値だけが取得できないこと";
                        // hidden
                        assertThat(msg, SessionUtil.orNull(context, "hidden-name1"), is(nullValue()));
                        assertThat(msg, SessionUtil.get(context, "hidden-name2").toString(), is("hidden-value2"));
                        assertThat(msg, SessionUtil.orNull(context, "hidden-name3"), is(nullValue()));
                        // httpSession
                        assertThat(msg, SessionUtil.orNull(context, "httpSession-name1"), is(nullValue()));
                        assertThat(msg, SessionUtil.orNull(context, "httpSession-name2"), is(nullValue()));
                        assertThat(msg, SessionUtil.get(context, "httpSession-name3").toString(), is("httpSession-value3"));

                        return new HttpResponse(200);
                    }
                })
                .test();
    }

    /**
     * いきなりinvalidateした後、そもそも値を保存していないので、
     * 当然、値が取得できないこと。
     */
    @Test
    public void testInvalidate() {
        httpServer
                .addTestHandler(new HttpServerResource.TestHandler() {
                    @Override
                    public HttpResponse handle(final HttpRequest request, final ExecutionContext context) {

                        final String desc = "いきなりinvalidateする";
                        SessionUtil.invalidate(context);

                        final String msg = "いきなりinvalidateしたので値が取得できないこと";
                        // hidden
                        assertThat(msg, SessionUtil.orNull(context, "hidden-name1"), is(nullValue()));
                        assertThat(msg, SessionUtil.orNull(context, "hidden-name2"), is(nullValue()));
                        assertThat(msg, SessionUtil.orNull(context, "hidden-name3"), is(nullValue()));
                        // httpSession
                        assertThat(msg, SessionUtil.orNull(context, "httpSession-name1"), is(nullValue()));
                        assertThat(msg, SessionUtil.orNull(context, "httpSession-name2"), is(nullValue()));
                        assertThat(msg, SessionUtil.orNull(context, "httpSession-name3"), is(nullValue()));

                        return new HttpResponse("hiddenStore.jsp");
                    }
                })
                .addTestHandler(new HttpServerResource.TestHandler() {
                    @Override
                    public HttpResponse handle(final HttpRequest request, final ExecutionContext context) {

                        final String msg = "前のリクエストでinvalidateしたので値が取得できないこと";
                        // hidden
                        assertThat(msg, SessionUtil.orNull(context, "hidden-name1"), is(nullValue()));
                        assertThat(msg, SessionUtil.orNull(context, "hidden-name2"), is(nullValue()));
                        assertThat(msg, SessionUtil.orNull(context, "hidden-name3"), is(nullValue()));
                        // httpSession
                        assertThat(msg, SessionUtil.orNull(context, "httpSession-name1"), is(nullValue()));
                        assertThat(msg, SessionUtil.orNull(context, "httpSession-name2"), is(nullValue()));
                        assertThat(msg, SessionUtil.orNull(context, "httpSession-name3"), is(nullValue()));

                        return new HttpResponse(200);
                    }
                })
                .test();
    }

    /**
     * invalidate前に保存した値が取得できないこと。
     */
    @Test
    public void testInvalidateBeforeSave() {
        final Map<String, String> beforeCookie = new HashMap<String, String>();
        httpServer
                .addTestHandler(new HttpServerResource.TestHandler() {
                    @Override
                    public HttpResponse handle(final HttpRequest request, final ExecutionContext context) {

                        final String desc = "値を保存する";
                        // hidden
                        SessionUtil.put(context, "hidden-name1", "hidden-value1");
                        SessionUtil.put(context, "hidden-name2", "hidden-value2");
                        SessionUtil.put(context, "hidden-name3", "hidden-value3");
                        // httpSession
                        SessionUtil.put(context, "httpSession-name1", "httpSession-value1", "httpSession");
                        SessionUtil.put(context, "httpSession-name2", "httpSession-value2", "httpSession");
                        SessionUtil.put(context, "httpSession-name3", "httpSession-value3", "httpSession");

                        return new HttpResponse("hiddenStore.jsp");
                    }
                })
                .addTestHandler(new HttpServerResource.TestHandler() {
                    @Override
                    public HttpResponse handle(final HttpRequest request, final ExecutionContext context) {

                        final String msg = "前のリクエストで保存した値を取得できること";
                        // hidden
                        assertThat(msg, SessionUtil.get(context, "hidden-name1").toString(), is("hidden-value1"));
                        assertThat(msg, SessionUtil.get(context, "hidden-name2").toString(), is("hidden-value2"));
                        assertThat(msg, SessionUtil.get(context, "hidden-name3").toString(), is("hidden-value3"));
                        // httpSession
                        assertThat(msg, SessionUtil.get(context, "httpSession-name1").toString(), is("httpSession-value1"));
                        assertThat(msg, SessionUtil.get(context, "httpSession-name2").toString(), is("httpSession-value2"));
                        assertThat(msg, SessionUtil.get(context, "httpSession-name3").toString(), is("httpSession-value3"));

                        // 後続のリクエストでの確認用にJSESSIONID、NABLARCH_SIDをMAPに詰める。
                        beforeCookie.put("JSESSIONID", request.getCookie().get("JSESSIONID"));
                        beforeCookie.put("NABLARCH_SID", request.getCookie().get("NABLARCH_SID"));

                        return new HttpResponse("hiddenStore.jsp");
                    }
                })
                .addTestHandler(new HttpServerResource.TestHandler() {
                    @Override
                    public HttpResponse handle(final HttpRequest request, final ExecutionContext context) {

                        final String msg = "invalidate前なので保存した値を取得できること";
                        // hidden
                        assertThat(msg, SessionUtil.get(context, "hidden-name1").toString(), is("hidden-value1"));
                        assertThat(msg, SessionUtil.get(context, "hidden-name2").toString(), is("hidden-value2"));
                        assertThat(msg, SessionUtil.get(context, "hidden-name3").toString(), is("hidden-value3"));
                        // httpSession
                        assertThat(msg, SessionUtil.get(context, "httpSession-name1").toString(), is("httpSession-value1"));
                        assertThat(msg, SessionUtil.get(context, "httpSession-name2").toString(), is("httpSession-value2"));
                        assertThat(msg, SessionUtil.get(context, "httpSession-name3").toString(), is("httpSession-value3"));

                        final String beforeDesc = "invalidate直前に値を保存する";
                        // hidden
                        SessionUtil.put(context, "hidden-name4", "hidden-value4");
                        // httpSession
                        SessionUtil.put(context, "httpSession-name4", "httpSession-value4", "httpSession");

                        final String beforeMsg = "invalidate直前なので保存した値を取得できること";
                        // hidden
                        assertThat(msg, SessionUtil.get(context, "hidden-name4").toString(), is("hidden-value4"));
                        // httpSession
                        assertThat(msg, SessionUtil.get(context, "httpSession-name4").toString(), is("httpSession-value4"));

                        final String beforeSidMsg = "invalidate前なのでJSESSIONID、NABLARCH_SIDが前リクエストから変更されていないこと";
                        assertThat(beforeSidMsg, request.getCookie().get("JSESSIONID"), is(beforeCookie.get("JSESSIONID")));
                        assertThat(beforeSidMsg, request.getCookie().get("NABLARCH_SID"), is(beforeCookie.get("NABLARCH_SID")));

                        final String desc = "invalidateする";
                        SessionUtil.invalidate(context);

                        final String afterMsg = "invalidate後なので値が取得できないこと";
                        // hidden
                        assertThat(afterMsg, SessionUtil.orNull(context, "hidden-name1"), is(nullValue()));
                        assertThat(afterMsg, SessionUtil.orNull(context, "hidden-name2"), is(nullValue()));
                        assertThat(afterMsg, SessionUtil.orNull(context, "hidden-name3"), is(nullValue()));
                        assertThat(afterMsg, SessionUtil.orNull(context, "hidden-name4"), is(nullValue()));
                        // httpSession
                        assertThat(afterMsg, SessionUtil.orNull(context, "httpSession-name1"), is(nullValue()));
                        assertThat(afterMsg, SessionUtil.orNull(context, "httpSession-name2"), is(nullValue()));
                        assertThat(afterMsg, SessionUtil.orNull(context, "httpSession-name3"), is(nullValue()));
                        assertThat(afterMsg, SessionUtil.orNull(context, "httpSession-name4"), is(nullValue()));

                        return new HttpResponse("hiddenStore.jsp");
                    }
                })
                .addTestHandler(new HttpServerResource.TestHandler() {
                    @Override
                    public HttpResponse handle(final HttpRequest request, final ExecutionContext context) {

                        final String msg = "前のリクエストでinvalidateしたので値が取得できないこと";
                        // hidden
                        assertThat(msg, SessionUtil.orNull(context, "hidden-name1"), is(nullValue()));
                        assertThat(msg, SessionUtil.orNull(context, "hidden-name2"), is(nullValue()));
                        assertThat(msg, SessionUtil.orNull(context, "hidden-name3"), is(nullValue()));
                        assertThat(msg, SessionUtil.orNull(context, "hidden-name4"), is(nullValue()));
                        // httpSession
                        assertThat(msg, SessionUtil.orNull(context, "httpSession-name1"), is(nullValue()));
                        assertThat(msg, SessionUtil.orNull(context, "httpSession-name2"), is(nullValue()));
                        assertThat(msg, SessionUtil.orNull(context, "httpSession-name3"), is(nullValue()));
                        assertThat(msg, SessionUtil.orNull(context, "httpSession-name4"), is(nullValue()));

                        final String sidMsg = "前のリクエストでinvalidateしたのでJSESSIONID、NABLARCH_SIDが変更されていること";
                        // nablarch-testing-jetty9ではLazySessionInvalidationFilterのため、このアサートはコメントアウトする。
                        // assertThat(sidMsg, request.getCookie().get("JSESSIONID"), not(beforeCookie.get("JSESSIONID")));
                        assertThat(sidMsg, request.getCookie().get("NABLARCH_SID"), not(beforeCookie.get("NABLARCH_SID")));

                        return new HttpResponse(200);
                    }
                })
                .test();
    }

    /**
     * invalidate後に保存した値が取得できること。
     */
    @Test
    public void testInvalidateAfterSave() {
        final Map<String, String> beforeCookie = new HashMap<String, String>();
        httpServer
                .addTestHandler(new HttpServerResource.TestHandler() {
                    @Override
                    public HttpResponse handle(final HttpRequest request, final ExecutionContext context) {

                        final String desc = "値を保存する";
                        // hidden
                        SessionUtil.put(context, "hidden-name1", "hidden-value1");
                        SessionUtil.put(context, "hidden-name2", "hidden-value2");
                        SessionUtil.put(context, "hidden-name3", "hidden-value3");
                        // httpSession
                        SessionUtil.put(context, "httpSession-name1", "httpSession-value1", "httpSession");
                        SessionUtil.put(context, "httpSession-name2", "httpSession-value2", "httpSession");
                        SessionUtil.put(context, "httpSession-name3", "httpSession-value3", "httpSession");

                        return new HttpResponse("hiddenStore.jsp");
                    }
                })
                .addTestHandler(new HttpServerResource.TestHandler() {
                    @Override
                    public HttpResponse handle(final HttpRequest request, final ExecutionContext context) {

                        final String msg = "前のリクエストで保存した値を取得できること";
                        // hidden
                        assertThat(msg, SessionUtil.get(context, "hidden-name1").toString(), is("hidden-value1"));
                        assertThat(msg, SessionUtil.get(context, "hidden-name2").toString(), is("hidden-value2"));
                        assertThat(msg, SessionUtil.get(context, "hidden-name3").toString(), is("hidden-value3"));
                        // httpSession
                        assertThat(msg, SessionUtil.get(context, "httpSession-name1").toString(), is("httpSession-value1"));
                        assertThat(msg, SessionUtil.get(context, "httpSession-name2").toString(), is("httpSession-value2"));
                        assertThat(msg, SessionUtil.get(context, "httpSession-name3").toString(), is("httpSession-value3"));

                        // 後続のリクエストでの確認用にJSESSIONID、NABLARCH_SIDをMAPに詰める。
                        beforeCookie.put("JSESSIONID", request.getCookie().get("JSESSIONID"));
                        beforeCookie.put("NABLARCH_SID", request.getCookie().get("NABLARCH_SID"));

                        return new HttpResponse("hiddenStore.jsp");
                    }
                })
                .addTestHandler(new HttpServerResource.TestHandler() {
                    @Override
                    public HttpResponse handle(final HttpRequest request, final ExecutionContext context) {

                        final String msg = "invalidate前なので保存した値を取得できること";
                        // hidden
                        assertThat(msg, SessionUtil.get(context, "hidden-name1").toString(), is("hidden-value1"));
                        assertThat(msg, SessionUtil.get(context, "hidden-name2").toString(), is("hidden-value2"));
                        assertThat(msg, SessionUtil.get(context, "hidden-name3").toString(), is("hidden-value3"));
                        // httpSession
                        assertThat(msg, SessionUtil.get(context, "httpSession-name1").toString(), is("httpSession-value1"));
                        assertThat(msg, SessionUtil.get(context, "httpSession-name2").toString(), is("httpSession-value2"));
                        assertThat(msg, SessionUtil.get(context, "httpSession-name3").toString(), is("httpSession-value3"));

                        final String beforeSidMsg = "invalidate前なのでJSESSIONID、NABLARCH_SIDが前リクエストから変更されていないこと";
                        assertThat(beforeSidMsg, request.getCookie().get("JSESSIONID"), is(beforeCookie.get("JSESSIONID")));
                        assertThat(beforeSidMsg, request.getCookie().get("NABLARCH_SID"), is(beforeCookie.get("NABLARCH_SID")));

                        final String desc = "invalidateして、すぐに値を保存する";
                        SessionUtil.invalidate(context);
                        // hidden
                        SessionUtil.put(context, "hidden-name4", "hidden-value4");
                        // httpSession
                        SessionUtil.put(context, "httpSession-name4", "httpSession-value4", "httpSession");

                        final String afterMsg = "invalidate後に保存した値のみ取得できること";
                        // hidden
                        assertThat(afterMsg, SessionUtil.orNull(context, "hidden-name1"), is(nullValue()));
                        assertThat(afterMsg, SessionUtil.orNull(context, "hidden-name2"), is(nullValue()));
                        assertThat(afterMsg, SessionUtil.orNull(context, "hidden-name3"), is(nullValue()));
                        assertThat(afterMsg, SessionUtil.get(context, "hidden-name4").toString(), is("hidden-value4"));
                        // httpSession
                        assertThat(afterMsg, SessionUtil.orNull(context, "httpSession-name1"), is(nullValue()));
                        assertThat(afterMsg, SessionUtil.orNull(context, "httpSession-name2"), is(nullValue()));
                        assertThat(afterMsg, SessionUtil.orNull(context, "httpSession-name3"), is(nullValue()));
                        assertThat(afterMsg, SessionUtil.get(context, "httpSession-name4").toString(), is("httpSession-value4"));

                        return new HttpResponse("hiddenStore.jsp");
                    }
                })
                .addTestHandler(new HttpServerResource.TestHandler() {
                    @Override
                    public HttpResponse handle(final HttpRequest request, final ExecutionContext context) {
                        final String msg = "前のリクエストでinvalidate後に保存した値のみ取得できること";
                        // hidden
                        assertThat(msg, SessionUtil.orNull(context, "hidden-name1"), is(nullValue()));
                        assertThat(msg, SessionUtil.orNull(context, "hidden-name2"), is(nullValue()));
                        assertThat(msg, SessionUtil.orNull(context, "hidden-name3"), is(nullValue()));
                        // nablarch-testing-jetty9ではLazySessionInvalidationFilterのため、このアサートはコメントアウトする。
                        // assertThat(msg, SessionUtil.get(context, "hidden-name4").toString(), is("hidden-value4"));
                        // httpSession
                        assertThat(msg, SessionUtil.orNull(context, "httpSession-name1"), is(nullValue()));
                        assertThat(msg, SessionUtil.orNull(context, "httpSession-name2"), is(nullValue()));
                        assertThat(msg, SessionUtil.orNull(context, "httpSession-name3"), is(nullValue()));
                        // nablarch-testing-jetty9ではLazySessionInvalidationFilterのため、このアサートはコメントアウトする。
                        // assertThat(msg, SessionUtil.get(context, "httpSession-name4").toString(), is("httpSession-value4"));

                        final String sidMsg = "前のリクエストでinvalidateしたのでJSESSIONID、NABLARCH_SIDが変更されていること";
                        // nablarch-testing-jetty9ではLazySessionInvalidationFilterのため、このアサートはコメントアウトする。
                        // assertThat(sidMsg, request.getCookie().get("JSESSIONID"), not(beforeCookie.get("JSESSIONID")));
                        assertThat(sidMsg, request.getCookie().get("NABLARCH_SID"), not(beforeCookie.get("NABLARCH_SID")));

                        return new HttpResponse(200);
                    }
                })
                .test();
    }

    /**
     * リダイレクト後に保存した値を取得できること。
     */
    @Test
    public void testRedirect() {
        httpServer
                .addTestHandler(new HttpServerResource.TestHandler() {
                    @Override
                    protected int getExpectedStatusCode() {
                        return 302;
                    }
                    @Override
                    public HttpResponse handle(final HttpRequest request, final ExecutionContext context) {

                        final String desc = "値を保存する";
                        // hidden
                        SessionUtil.put(context, "hidden-name1", "hidden-value1");
                        SessionUtil.put(context, "hidden-name2", "hidden-value2");
                        SessionUtil.put(context, "hidden-name3", "hidden-value3");
                        // httpSession
                        SessionUtil.put(context, "httpSession-name1", "httpSession-value1", "httpSession");
                        SessionUtil.put(context, "httpSession-name2", "httpSession-value2", "httpSession");
                        SessionUtil.put(context, "httpSession-name3", "httpSession-value3", "httpSession");

                        return new HttpResponse("redirect://redirect-test");
                    }
                })
                .addTestHandler(new HttpServerResource.TestHandler() {
                    @Override
                    protected String getMethod() {
                        return "GET";
                    }
                    @Override
                    public HttpResponse handle(final HttpRequest request, final ExecutionContext context) {

                        assertThat("リダイレクトなのでGETであること", request.getMethod(), is("GET"));

                        final String msg = "HiddenStore以外は前のリクエストで保存した値を取得できること";
                        // hidden
                        assertThat(msg, SessionUtil.orNull(context, "hidden-name1"), is(nullValue()));
                        assertThat(msg, SessionUtil.orNull(context, "hidden-name2"), is(nullValue()));
                        assertThat(msg, SessionUtil.orNull(context, "hidden-name3"), is(nullValue()));
                        // httpSession
                        assertThat(msg, SessionUtil.get(context, "httpSession-name1").toString(), is("httpSession-value1"));
                        assertThat(msg, SessionUtil.get(context, "httpSession-name2").toString(), is("httpSession-value2"));
                        assertThat(msg, SessionUtil.get(context, "httpSession-name3").toString(), is("httpSession-value3"));

                        return new HttpResponse(200);
                    }
                })
                .test();
    }

    @Entity
    @Table(name = "USER_SESSION")
    public static class UserSession {

        @Id
        @Column(name = "SESSION_ID", nullable = false)
        public String sessionId;

        @Lob
        @Column(name = "SESSION_OBJECT")
        public byte[] sessionObject;

        @Column(name = "EXPIRATION_DATETIME")
        public Timestamp expirationDatetime;

        public UserSession() {}

        public UserSession(String sessionId, byte[] sessionObject, Timestamp expirationDatetime) {
            this.sessionId = sessionId;
            this.sessionObject = sessionObject;
            this.expirationDatetime = expirationDatetime;
        }
    }
}
