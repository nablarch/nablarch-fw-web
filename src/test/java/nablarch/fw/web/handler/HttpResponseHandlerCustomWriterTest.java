package nablarch.fw.web.handler;

import junit.framework.AssertionFailedError;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpRequestHandler;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.HttpServer;
import nablarch.fw.web.MockHttpRequest;
import nablarch.fw.web.handler.responsewriter.CustomResponseWriter;
import nablarch.fw.web.servlet.MockServletContext;
import nablarch.fw.web.servlet.MockServletRequest;
import nablarch.fw.web.servlet.MockServletResponse;
import nablarch.fw.web.servlet.ServletExecutionContext;
import nablarch.test.IgnoringLS;
import org.junit.Test;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class HttpResponseHandlerCustomWriterTest {

    /**
     * {@link CustomResponseWriter}が、レスポンスを処理対象と判定しなかった場合、
     * サーブレットフォワードが実行されること
     */
    @Test
    public void testCustomResponseWriter() {
        final WritableMockResponse mockServletResponse = new WritableMockResponse();
        ServletExecutionContext context = new ServletExecutionContext(new MockServletRequest(),
                                                                      mockServletResponse,
                                                                      new MockServletContext());
        // ハンドラ
        HttpResponseHandler httpResponseHandler = new HttpResponseHandler();
        httpResponseHandler.setCustomResponseWriter(new CustomResponseWriter() {
            @Override
            public boolean isResponsibleTo(HttpResponse response, ServletExecutionContext context) {
                return response.getContentPath().getPath().equals("hello.html");
            }

            @Override
            public void writeResponse(HttpResponse response, ServletExecutionContext context) throws ServletException, IOException {
                context.getServletResponse().getWriter().print("Hello World!");
            }
        });
        context.addHandler(httpResponseHandler);
        context.addHandler(new HttpRequestHandler() {
            @Override
            public HttpResponse handle(HttpRequest request, ExecutionContext ctx) {
                return new HttpResponse("hello.html");
            }
        });
        // 実行
        context.handleNext(new MockHttpRequest("GET / HTTP/1.1"));

        // 検証
        String bodyString = mockServletResponse.writer.toString();
        assertThat(bodyString, is("Hello World!"));
    }

    /**
     * {@link CustomResponseWriter}が、レスポンスを処理対象と判定しなかった場合、
     * サーブレットフォワードが実行されること
     */
    @Test
    public void testNoCustomResponseWriter() {
        HttpServer server = new HttpServer();
        // HttpResponseHandlerにCustomResponseWriterを設定する
        HttpResponseHandler sut = server.getHandlerOf(HttpResponseHandler.class);
        sut.setCustomResponseWriter(new CustomResponseWriter() {
            @Override
            public boolean isResponsibleTo(HttpResponse response, ServletExecutionContext context) {
                return false;  // 常にfalseを返却する
            }
            @Override
            public void writeResponse(HttpResponse response, ServletExecutionContext context) throws ServletException, IOException {
                throw new AssertionFailedError(
                        "isResponsibleToがfalseを返却するので、ここには到達しない。");
            }
        });
        server.setWarBasePath("classpath://nablarch/fw/web/handler/httpresponsehandler")
              .addHandler(new HttpRequestHandler() {
                  @Override
                  public HttpResponse handle(HttpRequest request, ExecutionContext context) {
                      return new HttpResponse("index.jsp");  // index.jspにフォワード
                  }
              })
              .startLocal();
        HttpResponse res = server.handle(new MockHttpRequest("GET / HTTP/1.1"), null);
        assertThat("index.jspにサーブレットフォワードされること",
                   res.getBodyString(), is("Hello World!"));
    }

    private static class WritableMockResponse extends MockServletResponse {
        private final StringWriter writer = new StringWriter();
        @Override
        public PrintWriter getWriter() {
            return new PrintWriter(writer);
        }
    }
}
