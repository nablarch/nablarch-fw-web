package nablarch.fw.web.handler;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import javax.servlet.http.HttpServletRequest;

import nablarch.TestUtil;
import nablarch.common.web.session.SessionManager;
import nablarch.common.web.session.SessionStore;
import nablarch.common.web.session.SessionStoreHandler;
import nablarch.common.web.session.SessionUtil;
import nablarch.common.web.session.store.HiddenStore;
import nablarch.core.ThreadContext;
import nablarch.core.repository.ObjectLoader;
import nablarch.core.repository.SystemRepository;
import nablarch.fw.ExecutionContext;
import nablarch.fw.Handler;
import nablarch.fw.Request;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpRequestHandler;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.HttpServer;
import nablarch.fw.web.MockHttpRequest;
import nablarch.fw.web.i18n.ResourcePathRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * {@link HttpResponseHandler}のセッションコピーのテスト。
 * @author Kiyohito Itoh
 */
public class HttpResponseHandlerSessionCopyTest {

    @After
    public void tearDown(){
        SystemRepository.clear();
    }


    private SessionStoreHandler handler = new SessionStoreHandler();
    private SessionManager manager = new SessionManager();
    private SessionStore store2 = new HiddenStore();


    @Before
    public void setUp() {
        store2.setExpires(900L);
        manager.setAvailableStores(new ArrayList<SessionStore>() {{
            add(store2);
        }});
        manager.setDefaultStoreName("hidden");
        handler.setSessionManager(manager);
        ThreadContext.clear();


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
    public static class Parent {
        private String name = "oya";

        private Child child = new Child();

        public String getName() {
            return name;
        }

        public Child getChild() {
            return child;
        }
    }

    public static class Child {
        private String name = "kodomo";

        public String getName() {
            return name;
        }
    }

    /**
     * SessionStoreに設定した値が、JSPで使用できること。
     */
    @Test
    public void testSessionCopy() {
        HttpServer server = TestUtil.createHttpServer()

                .setWarBasePath("classpath://nablarch/fw/web/handler/httpresponsehandler/session")
                .addHandler(new HttpRequestHandler() {
                    @Override
                    public HttpResponse handle(HttpRequest request, ExecutionContext context) {
                        SessionUtil.put(context, "parent", new Parent());
                        SessionUtil.put(context, "string", "moji");
                        SessionUtil.put(context, "integer", 123);
                        // JSPでセッションの値を出力
                        return new HttpResponse("index.jsp");
                    }
                });
        server.startLocal();

        HttpResponse res = server.handle(new MockHttpRequest("GET / HTTP/1.1"), null);
        String bodyString = res.getBodyString();
        Scanner s = new Scanner(bodyString);
        assertThat(s.nextLine(), is("oya"));
        assertThat(s.nextLine(), is("kodomo"));
        assertThat(s.nextLine(), is("moji"));
        assertThat(s.nextLine(), is("123"));
        assertThat(s.hasNextLine(), is(false));
    }

    /**
     * SessionStoreに設定した値で、削除されたもの以外が、JSPで使用できること。
     */
    @Test
    public void testSessionDeleteAndCopy() {
        HttpServer server = TestUtil.createHttpServer()

                .setWarBasePath("classpath://nablarch/fw/web/handler/httpresponsehandler/session")
                .addHandler(new HttpRequestHandler() {
                    @Override
                    public HttpResponse handle(HttpRequest request, ExecutionContext context) {
                        SessionUtil.put(context, "parent", new Parent());
                        SessionUtil.put(context, "string", "moji");
                        SessionUtil.put(context, "integer", 123);
                        SessionUtil.delete(context, "string");
                        // JSPでセッションの値を出力
                        return new HttpResponse("index.jsp");
                    }
                });
        server.startLocal();

        HttpResponse res = server.handle(new MockHttpRequest("GET / HTTP/1.1"), null);
        String bodyString = res.getBodyString();
        Scanner s = new Scanner(bodyString);
        assertThat(s.nextLine(), is("oya"));
        assertThat(s.nextLine(), is("kodomo"));
        assertThat(s.nextLine(), is(""));
        assertThat(s.nextLine(), is("123"));
        assertThat(s.hasNextLine(), is(false));
    }

    /**
     * forward先のパスがnullの場合はコピーされないこと。
     */
    @Test
    public void testForwardPathIsNull() {

        HttpServer server = TestUtil.createHttpServer()

                .setWarBasePath("classpath://nablarch/fw/web/handler/httpresponsehandler/session")
                .addHandler(new HttpRequestHandler() {
                    @Override
                    public HttpResponse handle(HttpRequest request, ExecutionContext context) {
                        SessionUtil.put(context, "parent", new Parent());
                        SessionUtil.put(context, "string", "moji");
                        SessionUtil.put(context, "integer", 123);
                        // JSPでセッションの値を出力
                        return new HttpResponse("dummy.jsp");
                    }
                });

        // forwardPathをnullにするための準備
        HttpResponseHandler sut = null;
        for (Handler handler : server.getHandlerQueue()) {
            if (handler instanceof HttpResponseHandler) {
                sut = HttpResponseHandler.class.cast(handler);
                break;
            }
        }
        sut.setContentPathRule(new ResourcePathRule() {
            @Override
            public String getPathForLanguage(String path, HttpServletRequest request) {
                return null;
            }

            @Override
            protected String createPathForLanguage(String pathFromContextRoot, String language) {
                return null;
            }
        });

        // コピーされていないことをアサートするため、リクエストスコープから取り出すハンドラを準備。
        final Map<String, Object> holder = new HashMap<String, Object>();
        server.addHandler(0, new Handler<Request<?>, Object>() {
            @Override
            public Object handle(Request<?> request, ExecutionContext context) {
                Object result = context.getNextHandler().handle(request, context);
                for (String key : new String[]{"parent", "string", "value"}) {
                    holder.put(key, context.getRequestScopedVar(key));
                }
                return result;
            }
        });

        server.startLocal();
        server.handle(new MockHttpRequest("GET / HTTP/1.1"), null);

        assertThat(holder.get("parent"), is(nullValue()));
        assertThat(holder.get("string"), is(nullValue()));
        assertThat(holder.get("value"), is(nullValue()));
    }

    /**
     * forward先のパスにドットが含まれない場合はコピーされないこと。
     */
    @Test
    public void testForwardPathNotIncludeDot() {

        HttpServer server = TestUtil.createHttpServer()

                .setWarBasePath("classpath://nablarch/fw/web/handler/httpresponsehandler/session")
                .addHandler(new HttpRequestHandler() {
                    @Override
                    public HttpResponse handle(HttpRequest request, ExecutionContext context) {
                        SessionUtil.put(context, "parent", new Parent());
                        SessionUtil.put(context, "string", "moji");
                        SessionUtil.put(context, "integer", 123);
                        // JSPでセッションの値を出力
                        return new HttpResponse("dummy"); // ドットを含まないパス
                    }
                });

        // コピーされていないことをアサートするため、リクエストスコープから取り出すハンドラを準備。
        final Map<String, Object> holder = new HashMap<String, Object>();
        server.addHandler(0, new Handler<Request<?>, Object>() {
            @Override
            public Object handle(Request<?> request, ExecutionContext context) {
                Object result = context.getNextHandler().handle(request, context);
                for (String key : new String[]{"parent", "string", "value"}) {
                    holder.put(key, context.getRequestScopedVar(key));
                }
                return result;
            }
        });

        server.startLocal();
        server.handle(new MockHttpRequest("GET / HTTP/1.1"), null);

        assertThat(holder.get("parent"), is(nullValue()));
        assertThat(holder.get("string"), is(nullValue()));
        assertThat(holder.get("value"), is(nullValue()));
    }

    /**
     * リクエストスコープに同じ名前の変数が存在している場合は、
     * その値のみコピーされないこと。
     */
    @Test
    public void testRequestScopeIncludeSameName() {
        HttpServer server = TestUtil.createHttpServer()

                .setWarBasePath("classpath://nablarch/fw/web/handler/httpresponsehandler/session")
                .addHandler(new HttpRequestHandler() {
                    @Override
                    public HttpResponse handle(HttpRequest request, ExecutionContext context) {

                        SessionUtil.put(context, "parent", new Parent());
                        SessionUtil.put(context, "string", "moji");
                        SessionUtil.put(context, "integer", 123);

                        // リクエストスコープに同じ名前で設定
                        context.setRequestScopedVar("string", "test");

                        return new HttpResponse("index.jsp");
                    }
                });
        server.startLocal();

        HttpResponse res = server.handle(new MockHttpRequest("GET / HTTP/1.1"), null);
        String bodyString = res.getBodyString();
        Scanner s = new Scanner(bodyString);
        assertThat(s.nextLine(), is("oya"));
        assertThat(s.nextLine(), is("kodomo"));
        assertThat(s.nextLine(), is("test")); // "moji"は名前が重複したのでコピーされず、リクエストスコープの値が出力される。
        assertThat(s.nextLine(), is("123"));
        assertThat(s.hasNextLine(), is(false));
    }
}
