package nablarch.fw.web.handler;

import static nablarch.test.support.tool.Hereis.string;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import nablarch.core.ThreadContext;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpRequestHandler;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.MockHttpRequest;
import nablarch.fw.web.interceptor.ErrorOnSessionWriteConflict;
import nablarch.test.core.log.LogVerifier;
import nablarch.test.support.SystemRepositoryResource;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

@Ignore
public class SessionConcurrentAccessHandlerTest {

    @Rule
    public SystemRepositoryResource repositoryResource = new SystemRepositoryResource("db-default.xml");
    
    public static final class Events {

        CountDownLatch startGate;
        CountDownLatch allRequestsAccepted;
        CountDownLatch oneOfRequestsCompleted;
        CountDownLatch allRequestsCompleted;
        CountDownLatch goalGate;

        Events() {
            reset();
        }

        void reset() {
            startGate              = new CountDownLatch(1);
            allRequestsAccepted    = new CountDownLatch(2);
            oneOfRequestsCompleted = new CountDownLatch(1);
            allRequestsCompleted   = new CountDownLatch(2);
            goalGate               = new CountDownLatch(1);
        }
    };
    private final Events events = new Events();


    private ExecutionContext
    createContext(SessionConcurrentAccessHandler handler, Map session) {
        return new ExecutionContext()
              .addHandler(
                   new HttpErrorHandler()
                      .setDefaultPage("409", "/sessionWriteConflict.html")
               )
              .addHandler(handler)
              /**
               * A RequestHandler that initializes session variables.
               */
              .addHandler("//*", new HttpRequestHandler() {
                   @Override
                public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                       try {
                           events.allRequestsAccepted.countDown();
                           events.startGate.await();
                           return ctx.handleNext(req);
                       } catch (InterruptedException e) {
                           throw new RuntimeException(e);
                       }
                   }
               })
              .addHandler("/node1/*", new HttpRequestHandler() {
                   @Override
                public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                       try {
                           Thread thread = Thread.currentThread();
                           Thread.sleep(20);
                           String var1 = (String) ctx.getSessionScopedVar("var1");
                           Thread.sleep(20);
                           String var2 = (String) ctx.getSessionScopedVar("var2");
                           Thread.sleep(20);
                           ctx.setSessionScopedVar("var1", var1 + thread.getName());
                           Thread.sleep(20);
                           ctx.setSessionScopedVar("var2", var2 + thread.getName());
                           Thread.sleep(20);

                           completedThreads.add(thread);

                           events.oneOfRequestsCompleted.countDown();
                           events.allRequestsCompleted.countDown();
                           events.goalGate.await();
                           return new HttpResponse(201);
                       } catch (InterruptedException e) {
                           throw new RuntimeException(e);
                       }
                   }
               })
              .addHandler("/node2/*", new HttpRequestHandler() {
                   @Override
                public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                       try {
                           Thread thread = Thread.currentThread();
                           Thread.sleep(20);
                           String var1 = (String) ctx.getSessionScopedVar("var1");
                           Thread.sleep(20);
                           String var2 = (String) ctx.getSessionScopedVar("var2");
                           Thread.sleep(20);
                           ctx.setSessionScopedVar("var1", var1 + thread.getName());
                           Thread.sleep(20);
                           ctx.setSessionScopedVar("var2", var2 + thread.getName());
                           Thread.sleep(20);

                           completedThreads.add(thread);

                           events.oneOfRequestsCompleted.countDown();
                           events.allRequestsCompleted.countDown();
                           events.goalGate.await();
                           return new HttpResponse(201);
                       } catch (InterruptedException e) {
                           throw new RuntimeException(e);
                       }
                   }
               })
              .addHandler("/node1e/*", new HttpRequestHandler() {
                   @Override
                @ErrorOnSessionWriteConflict //セッション書込みに失敗した場合はステータスコード409(Conflicted)を返す。
                   public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                       try {
                           Thread thread = Thread.currentThread();
                           Thread.sleep(20);
                           String var1 = (String) ctx.getSessionScopedVar("var1");
                           Thread.sleep(20);
                           String var2 = (String) ctx.getSessionScopedVar("var2");
                           Thread.sleep(20);
                           ctx.setSessionScopedVar("var1", var1 + thread.getName());
                           Thread.sleep(20);
                           ctx.setSessionScopedVar("var2", var2 + thread.getName());
                           Thread.sleep(20);

                           completedThreads.add(thread);

                           events.oneOfRequestsCompleted.countDown();
                           events.allRequestsCompleted.countDown();
                           events.goalGate.await();
                           Thread.sleep(20);
                           return new HttpResponse(201);
                       } catch (InterruptedException e) {
                           throw new RuntimeException(e);
                       }
                   }
               })
              .addHandler("/node2e/*", new HttpRequestHandler() {
                   @Override
                @ErrorOnSessionWriteConflict
                   public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                       try {
                           Thread thread = Thread.currentThread();
                           Thread.sleep(20);
                           String var1 = (String) ctx.getSessionScopedVar("var1");
                           Thread.sleep(20);
                           String var2 = (String) ctx.getSessionScopedVar("var2");
                           Thread.sleep(20);
                           ctx.setSessionScopedVar("var1", var1 + thread.getName());
                           Thread.sleep(20);
                           ctx.setSessionScopedVar("var2", var2 + thread.getName());
                           Thread.sleep(20);

                           completedThreads.add(thread);

                           events.oneOfRequestsCompleted.countDown();
                           events.allRequestsCompleted.countDown();
                           events.goalGate.await();
                           return new HttpResponse(201);
                       } catch (InterruptedException e) {
                           throw new RuntimeException(e);
                       }
                   }
               })
              .addHandler("/node3e/*", new HttpRequestHandler() {
                  @Override
                  @ErrorOnSessionWriteConflict
                  public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                      ctx.setSessionScopedVar("var1", new Thread());  // シリアライズ時にエラーが発生する。
                      return new HttpResponse(201);
                  }
              })
              .addHandler("/node1t/*", new HttpRequestHandler() {
                  @Override
                  @ErrorOnSessionWriteConflict //セッション書込みに失敗した場合はステータスコード409(Conflicted)を返す。
                  public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                      try {
                          Thread thread = Thread.currentThread();
                          Thread.sleep(20);
                          String var1 = (String) ctx.getSessionScopedVar("var1");
                          Thread.sleep(20);
                          String var2 = (String) ctx.getSessionScopedVar("var2");
                          Thread.sleep(20);
                          ctx.setSessionScopedVar(SESSION_TOKEN_KEY, var1 + thread.getName());
                          Thread.sleep(20);
                          ctx.setSessionScopedVar(SESSION_TOKEN_KEY, var2 + thread.getName());
                          Thread.sleep(20);
                          completedThreads.add(thread);

                          events.oneOfRequestsCompleted.countDown();
                          events.allRequestsCompleted.countDown();
                          events.goalGate.await();
                          Thread.sleep(20);
                          return new HttpResponse(201);
                      } catch (InterruptedException e) {
                          throw new RuntimeException(e);
                      }
                  }
              })
              .addHandler("/node2t/*", new HttpRequestHandler() {
                  @Override
                  @ErrorOnSessionWriteConflict
                  public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
                      try {
                          Thread thread = Thread.currentThread();
                          Thread.sleep(20);
                          String var1 = (String) ctx.getSessionScopedVar("var1");
                          Thread.sleep(20);
                          String var2 = (String) ctx.getSessionScopedVar("var2");
                          Thread.sleep(20);
                          ctx.setSessionScopedVar(SESSION_TOKEN_KEY, var1 + thread.getName());
                          Thread.sleep(20);
                          ctx.setSessionScopedVar(SESSION_TOKEN_KEY, var2 + thread.getName());
                          Thread.sleep(20);
                          completedThreads.add(thread);

                          events.oneOfRequestsCompleted.countDown();
                          events.allRequestsCompleted.countDown();
                          events.goalGate.await();
                          Thread.sleep(20);
                          return new HttpResponse(201);
                      } catch (InterruptedException e) {
                          throw new RuntimeException(e);
                      }
                  }
              });
    }

    private final List<Thread> completedThreads = new ArrayList<Thread>();
    private static final List<HttpResponse> responseHolder = new ArrayList<HttpResponse>();

    class Task implements Runnable {
        final ExecutionContext    ctx;
        final Map<String, Object> session;
        final String              nodeToAccess;

        Task(SessionConcurrentAccessHandler handler,
             Map<String, Object>            session,
             String                         nodeToAccess)
        {
            this.ctx          = createContext(handler, session);
            this.session      = session;
            this.nodeToAccess = nodeToAccess;
        }

        @Override
        public void run() {
            HttpRequest req = new MockHttpRequest(string(nodeToAccess));
            /*****************************************
            GET /${nodeToAccess}/test HTTP/1.1
            ******************************************/
            ExecutionContext context = new ExecutionContext(ctx)
                                      .setSessionScopeMap(session);
            HttpResponse res = context.handleNext(req);
            responseHolder.add(res);
            assertTrue(res.getStatusCode() == 201 ||  res.getStatusCode() == 409);
        }
    }

    /**
    public void testConcurrentAccessPolicy_SERIALIZE() throws Exception {
        sessionManager.setConcurrentAccessPolicy(
            ConcurrentAccessPolicy.SERIALIZE
        );

        final Map<String, Object> session = new ConcurrentHashMap<String, Object>();

        Thread requester1 = new Thread(
            new Task(server, session, "node1"),
            "requester1"
        );

        Thread requester2 = new Thread(
            new Task(server, session, "node2"),
            "requester2"
        );

        requester1.start();
        requester2.start();

        allRequestsAccepted.await();
        startGate.countDown();
        oneOfRequestsCompleted.await();
        assertEquals(1, completedThreads.size());
        Thread winner = completedThreads.get(0);
        assertEquals("var2@root" + winner.getName(), session.get("/var2"));

        goalGate.countDown();
        allRequestsCompleted.await();

        assertEquals(2, completedThreads.size());
        Thread looser = completedThreads.get(1);
        assertEquals("var2@root" + winner.getName() + looser.getName(), session.get("/var2"));
    }
    */

    /**
     * セッション並行アクセスポリシーMANUALを指定した際に、IllegalArgumentExceptionが送出されることを検証。(異常系)
     *
     * 補足：<br>
     * セッション並行アクセスポリシーMANUALは、1.4.3まで存在していたポリシーである。<br>
     * 1.5.0で廃止となった。
     *
     */
    @Test(expected = IllegalArgumentException.class)
    public void testConcurrentAccessPolicy_MANUAL() throws Exception {
        responseHolder.clear();
        completedThreads.clear();
        events.reset();

        new SessionConcurrentAccessHandler().setConcurrentAccessPolicy("MANUAL");
    }

    /**
     * セッション並行アクセスポリシーSERIALIZEを指定した際に、IllegalArgumentExceptionが送出されることを検証。(異常系)
     *
     * 補足：<br>
     * セッション並行アクセスポリシーSERIALIZEは、1.4.3まで存在していたポリシーである。<br>
     * 1.5.0で廃止となった。
     *
     */
    @Test(expected = IllegalArgumentException.class)
    public void testConcurrentAccessPolicy_SERIALIZE() throws Exception {
        responseHolder.clear();
        completedThreads.clear();
        events.reset();

        new SessionConcurrentAccessHandler().setConcurrentAccessPolicy("SERIALIZE");
    }

    /**
     * セッション並行アクセスポリシーCONCURRENTで正常にセッションスコープ変数の更新が可能で
     * あることを検証。(正常系)
     */
    @Test
    public void testConcurrentAccessPolicy_CONCURRENT() throws Exception {
        responseHolder.clear();
        completedThreads.clear();
        events.reset();

        SessionConcurrentAccessHandler handler = new SessionConcurrentAccessHandler()
                                                .setConcurrentAccessPolicy("CONCURRENT");

        final Map<String, Object> session = new ConcurrentHashMap<String, Object>();

        session.put("var1", "initial");

        Thread requester1 = new Thread(
            new Task(handler, session, "node1"),
            "requester1"
        );

        Thread requester2 = new Thread(
            new Task(handler, session, "node2"),
            "requester2"
        );
        events.startGate.countDown();
        events.goalGate.countDown();

        requester1.start();
        requester1.join();

        assertEquals("initial" + "requester1", session.get("var1").toString());

        requester2.start();
        requester2.join();

        assertEquals("initial" + "requester1" + "requester2", session.get("var1").toString());

        assertEquals(2, completedThreads.size());
    }

    private static final String SESSION_TOKEN_KEY = "/nablarch_session_token";

    /**
     * セッション並行アクセスポリシーCONCURRENTで、
     * nablarch_session_tokenが同期処理の対象外であることの検証。(正常系)
     */
    @Test
    public void testConcurrentAccessPolicy_CONCURRENT_sessionToken() throws Exception {

        responseHolder.clear();
        completedThreads.clear();
        events.reset();

        SessionConcurrentAccessHandler handler = new SessionConcurrentAccessHandler()
                .setConcurrentAccessPolicy("CONCURRENT");

        final Map<String, Object> session = new ConcurrentHashMap<String, Object>();
        session.put("var1", "initial");
        session.put("var2", "initial");

        Thread requester1t = new Thread(
                new Task(handler, session, "node1t"),
                "requester1t"
        );

        Thread requester2t = new Thread(
                new Task(handler, session, "node2t"),
                "requester2t"
        );

        requester1t.start();
        requester2t.start();

        events.allRequestsAccepted.await();
        events.startGate.countDown();
        events.allRequestsCompleted.await();
        assertEquals(2, completedThreads.size());

        assertEquals(2, session.size());

        events.goalGate.countDown();

        requester1t.join();
        requester2t.join();

        assertNull(session.get(SESSION_TOKEN_KEY)); // 同期処理の対象外
    }

    /**
     * セッション変数の更新内容を反映させた時点で、既に並行スレッドによる変更が反映されていた
     * 場合に、実行時例外が送出されることを検証する。
     */
    @Test
    public void testConcurrentAccessPolicy_ErrorOnSessionWriteConfilict() throws Exception {

        ThreadContext.setLanguage(Locale.ENGLISH);


        LogVerifier.clear();
        LogVerifier.setExpectedLogMessages(new ArrayList<Map<String, String>>() {{
            add(new HashMap<String, String>() {{
                put("logLevel", "INFO");
                put("message1", "Could not apply modification of session scope variables because another concurrent thread has already applied its modification.");
            }});
        }});

        responseHolder.clear();
        completedThreads.clear();
        events.reset();

        SessionConcurrentAccessHandler handler = new SessionConcurrentAccessHandler()
                                                .setConcurrentAccessPolicy("CONCURRENT");


        handler.setConflictWarningMessageId("10001");

        final Map<String, Object> session = new ConcurrentHashMap<String, Object>();

        Thread requester1e = new Thread(
            new Task(handler, session, "node1e"),
            "requester1e"
        );

        Thread requester2e = new Thread(
            new Task(handler, session, "node2e"),
            "requester2e"
        );

        requester1e.start();
        requester2e.start();

        events.allRequestsAccepted.await();
        events.startGate.countDown();
        events.allRequestsCompleted.await();
        assertEquals(2, completedThreads.size());

        assertEquals(0, session.size()); //まだ書き出されていない。

        events.goalGate.countDown();

        requester1e.join();
        requester2e.join();

        LogVerifier.verify("並行アクセスエラーのログがでる");
    }

    /**
     * セッションのシリアライズに失敗した場合は、実行時例外(IllegalStateException)を送出する。
     * → 通常はGlobalErrorHandlerによってFATALログが出力される。
     */
    @Test
    public void testConcurrentAccessPolicy_SerializerError() throws Exception {
        LogVerifier.clear();
        LogVerifier.setExpectedLogMessages(new ArrayList<Map<String, String>>() {{
            add(new HashMap<String, String>() {{
                put("logLevel", "FATAL");
                put("message1", "Could not apply modification of session scope variables because could not take snapshot of current session scope.");
            }});
        }});
        responseHolder.clear();
        completedThreads.clear();
        events.reset();
        events.startGate.countDown();

        SessionConcurrentAccessHandler handler = new SessionConcurrentAccessHandler()
                                                .setConcurrentAccessPolicy("CONCURRENT");

        final Map<String, Object> session = new ConcurrentHashMap<String, Object>();

        Thread requester = new Thread(
            new Task(handler, session, "node3e") {
                @Override
                public void run() {
                    HttpRequest req = new MockHttpRequest(string(nodeToAccess));
                    /*****************************************
                    GET /${nodeToAccess}/test HTTP/1.1
                    ******************************************/
                    ExecutionContext context = new ExecutionContext(ctx)
                                              .setSessionScopeMap(session);
                    HttpResponse res = context.handleNext(req);
                    responseHolder.add(res);
                    assertEquals(500, res.getStatusCode());
                }
            }
          , "requester"
        );

        requester.start();
        requester.join();

        LogVerifier.verify("書き込み失敗のログがでる");
    }
}
