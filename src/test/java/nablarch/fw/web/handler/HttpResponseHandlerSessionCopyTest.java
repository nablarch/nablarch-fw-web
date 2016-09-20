package nablarch.fw.web.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import nablarch.core.ThreadContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import nablarch.common.web.session.SessionManager;
import nablarch.common.web.session.SessionStore;
import nablarch.common.web.session.SessionStoreHandler;
import nablarch.common.web.session.SessionUtil;
import nablarch.common.web.session.store.HiddenStore;
import nablarch.core.repository.ObjectLoader;
import nablarch.core.repository.SystemRepository;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpRequestHandler;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.MockHttpRequest;
import nablarch.fw.web.HttpServer;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * {@link HttpResponseHandler}のセッションコピーのテスト。
 * @author Kiyohito Itoh
 */
public class HttpResponseHandlerSessionCopyTest {

    @After
    public void tearDown(){
        SystemRepository.clear();
    }


    private  SessionStoreHandler handler = new SessionStoreHandler();
    private  SessionManager manager = new SessionManager();
    private  SessionStore store2 = new HiddenStore();


    @Before
    public void setUp() {
        ThreadContext.clear();

        store2.setExpires(900L);
        manager.setAvailableStores(new ArrayList<SessionStore>(){{add(store2);}});
        manager.setDefaultStoreName("hidden");
        handler.setSessionManager(manager);


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
        HttpServer server = new HttpServer()

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
        HttpServer server = new HttpServer()

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

}
