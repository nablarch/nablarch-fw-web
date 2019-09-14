package nablarch.fw.web.handler;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Locale;

import nablarch.TestUtil;
import nablarch.core.ThreadContext;
import nablarch.core.log.LogUtil;
import nablarch.core.log.basic.LogLevel;
import nablarch.fw.ExecutionContext;
import nablarch.fw.results.ServiceUnavailable;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpRequestHandler;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.HttpServer;
import nablarch.fw.web.MockHttpRequest;
import nablarch.test.SystemPropertyResource;
import nablarch.test.support.SystemRepositoryResource;

import nablarch.test.support.log.app.OnMemoryLogWriter;
import nablarch.test.support.message.MockStringResourceHolder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

public class HttpErrorHandler_IllegalCaseTest {

    @ClassRule
    public static SystemRepositoryResource repositoryResource = new SystemRepositoryResource(
            "nablarch/fw/web/handler/HttpErrorHandlerIllegalCaseTest.xml");

    private static final String[][] MESSAGES = {
        { "FW000001", "ja", "FW000001メッセージ{0}", "en","FW000001Message{0}"},
        { "FW000002", "ja", "FW000002メッセージ{0}", "en","FW000002Message{0}" },
        { "FW000003", "ja", "FW000003メッセージ{0}" , "en","FW000003Message{0}" },
        { "FW000004", "ja", "FW000004メッセージ{0}", "en","FW000004Message{0}"},
        { "FW000005", "ja", "FW000005メッセージ{0}", "en","FW000005Message{0}" },
        { "FW000006", "ja", "FW000006メッセージ{0}" , "en","FW000006Message{0}" },
        { "FW000007", "ja", "FW000007メッセージ{0}" , "en","FW000007Message{0}" },
        { "FW999999", "ja", "FW999999メッセージ{0}" , "en","FW999999Message{0}"},
        { "ZZ999999", "ja", "ZZ999999メッセージ{0}", "en","ZZ999999Message{0}" },
        { "AP000001", "ja","AP000001メッセージ{0}" , "en","AP000001Message{0}"},
        { "AP000002", "ja","AP000002メッセージ{0}", "en"," AP000002Message{0}" },
        { "AP000003", "ja","AP000003メッセージ{0}", "en","AP000003Message{0}" },
        };

    @Rule
    public final SystemPropertyResource systemPropertyResource = new SystemPropertyResource();

    @BeforeClass
    public static void classSetup() throws Exception {
        repositoryResource.getComponentByType(MockStringResourceHolder.class).setMessages(MESSAGES);
    }

    @AfterClass
    public static void classTeardown() throws Exception {
    }

    @Test
    public void testHandlingOfOutOfMemoryError() {
        HttpServer server = TestUtil.createHttpServer()
            .addHandler(new HttpRequestHandler() {
                public HttpResponse handle(HttpRequest      request,
                                           ExecutionContext context) {
                    throw new OutOfMemoryError();
                }
            }).startLocal();

        HttpResponse res = server.handle(
                new MockHttpRequest("GET / HTTP/1.1")
                , new ExecutionContext()
        );
        assertEquals(500, res.getStatusCode());
        assertTrue(res.getBodyString().contains("<title>A system error occurred.</title>"));
    }

    @Test
    public void testHandlingOfErrorInServletForward() {
        HttpServer server = TestUtil.createHttpServer()
            .setWarBasePath("classpath://nablarch/fw/web/sample/app/")
            .addHandler(new HttpRequestHandler() {
                public HttpResponse handle(HttpRequest      request,
                                           ExecutionContext context) {
                    return new HttpResponse("servlet:///jsp/nullpointer.jsp");
                }
            }).startLocal();

        HttpResponse res = server.handle(
                new MockHttpRequest("GET / HTTP/1.1")
                , new ExecutionContext()
        );
        assertEquals(500, res.getStatusCode());
    }

    @Test
    public void testHandlingOfStackOverFlowError() {
        HttpServer server = TestUtil.createHttpServer()
            .addHandler(new HttpRequestHandler() {
                public HttpResponse handle(HttpRequest      request,
                                           ExecutionContext context) {
                    throw new StackOverflowError();
                }
            }).startLocal();

        server.getHandlerQueue().add(0, new InternalMonitor());

        HttpResponse res = server.handle(
                new MockHttpRequest("GET / HTTP/1.1")
                , new ExecutionContext()
        );
        assertEquals(500, res.getStatusCode());
        assertEquals(500, InternalMonitor.response.getStatusCode());
    }

    /**
     * Result.InternalError が送出された場合の下記挙動のテスト。
     * 1. 障害ログを出力する。
     * 2. ステータスコード500のレスポンスオブジェクトがリターンされる。
     */
    @Test
    public void testHandlingOfInternalError() throws Exception {
        OnMemoryLogWriter.clear();
        ThreadContext.setLanguage(Locale.JAPANESE);
        System.clearProperty("nablarch.appLog.filePath");
        LogUtil.removeAllObjectsBoundToContextClassLoader();

        HttpServer server = TestUtil.createHttpServer()
            .addHandler(new HttpRequestHandler() {
                public HttpResponse handle(HttpRequest      request,
                                           ExecutionContext context) {
                    throw new nablarch.fw.results.InternalError(
                        LogLevel.FATAL
                      , "FW000004"
                      , new Object[] {"fatal_ex_msg_short_messageOption"}
                    );
                }
            }).startLocal();

        server.getHandlerQueue().add(0, new InternalMonitor());

        HttpResponse res = server.handle(
                new MockHttpRequest("GET / HTTP/1.1")
                , new ExecutionContext()
        );

        assertEquals(500, res.getStatusCode());
        assertEquals(500, InternalMonitor.response.getStatusCode());

        List<String> appLogFile = OnMemoryLogWriter.getMessages("writer.appLog");
        assertThat(
            appLogFile.get(0)
          , containsString("FATAL stdout fail_code = [FW000004] FW000004メッセージfatal_ex_msg_short_messageOption")
        );
    }

    /**
     * Result.ServiceUnAvailableが発生した場合の下記挙動をテストする。
     * 1. 障害ログは出力されない。(デフォルト設定では。)
     * 2. ステータスコード503のレスポンスオブジェクトがリターンされる。
     */
    @Test
    public void testHandlingOfServiceUnavailable() throws Exception {
        OnMemoryLogWriter.clear();
        ThreadContext.setLanguage(Locale.JAPANESE);


        HttpServer server = TestUtil.createHttpServer()
            .addHandler(new HttpRequestHandler() {
                public HttpResponse handle(HttpRequest      request,
                                           ExecutionContext context) {
                    throw new ServiceUnavailable(
                        LogLevel.FATAL
                      , "FW000004"
                      , new Object[] {"fatal_ex_msg_short_messageOption"}
                    );
                }
            }).startLocal();

        server.getHandlerQueue().add(0, new InternalMonitor());

        server.getHandlerOf(HttpResponseHandler.class).setConvertMode("CONVERT_ONLY_400_TO_200");
        HttpResponse res = server.handle(
                new MockHttpRequest("GET / HTTP/1.1")
                , new ExecutionContext()
        );
        assertEquals(503, res.getStatusCode());
        assertEquals(503, InternalMonitor.response.getStatusCode());

        List<String> appLogFile = OnMemoryLogWriter.getMessages("writer.appLog");
        assertEquals(0, appLogFile.size());
    }





    @Test
    public void testHandlingOfThreadDeath() {
        HttpServer server = TestUtil.createHttpServer()
            .addHandler(new HttpRequestHandler() {
                public HttpResponse handle(HttpRequest      request,
                                           ExecutionContext context) {
                    throw new ThreadDeath();
                }
            }).startLocal();

        HttpResponse res = server.handle(
                new MockHttpRequest("GET / HTTP/1.1")
                , new ExecutionContext()
        );
        assertEquals(500, res.getStatusCode());
        assertTrue(res.getBodyString().contains("<title>A system error occurred.</title>"));
    }
}
