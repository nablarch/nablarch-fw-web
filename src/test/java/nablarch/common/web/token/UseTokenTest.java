package nablarch.common.web.token;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.http.HttpSession;

import org.junit.Test;

import nablarch.fw.ExecutionContext;
import nablarch.fw.Handler;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.HttpServer;
import nablarch.fw.web.MockHttpRequest;
import nablarch.fw.web.servlet.NablarchHttpServletRequestWrapper;
import nablarch.fw.web.servlet.ServletExecutionContext;

/**
 * {@link UseToken}のテストクラス。
 */
public class UseTokenTest {

    /**
     * {@link UseToken.Impl#handle(HttpRequest, ExecutionContext)}のテスト。
     */
    @Test
    public void testUseToken() {

        final AtomicReference<AssertionError> thrownAssertionError = new AtomicReference<AssertionError>();
        final AtomicBoolean assertionCalled = new AtomicBoolean(false);

        HttpServer httpServer = new HttpServer()
                //サーバー処理で発生したAssertionErrorをテスト実行のスレッドに渡すためのハンドラ
                .addHandler(new Handler<HttpRequest, HttpResponse>() {
                    @Override
                    public HttpResponse handle(HttpRequest data, ExecutionContext context) {
                        try {
                            return context.handleNext(data);
                        } catch (AssertionError e) {
                            thrownAssertionError.set(e);
                            //サーバー処理の結果には関心がないので204 No Contentを返しておく
                            return new HttpResponse(204);
                        }
                    }
                })
                //アサーションを行うハンドラ
                .addHandler(new Handler<HttpRequest, HttpResponse>() {
                    @Override
                    public HttpResponse handle(HttpRequest requet, ExecutionContext context) {
                        assertionCalled.set(true);

                        ServletExecutionContext sec = (ServletExecutionContext) context;
                        NablarchHttpServletRequestWrapper request = sec.getServletRequest();
                        HttpSession session = request.getSession().getDelegate();

                        //リクエストとセッションにトークンが登録されていないこと
                        assertThat(request.getAttribute(TokenUtil.KEY_REQUEST_TOKEN), nullValue());
                        assertThat(session.getAttribute(TokenUtil.KEY_SESSION_TOKEN), nullValue());

                        HttpResponse response = context.handleNext(requet);

                        //リクエストとセッションに発行されたトークンが登録されていること
                        assertThat(request.getAttribute(TokenUtil.KEY_REQUEST_TOKEN),
                                notNullValue());
                        assertThat(session.getAttribute(TokenUtil.KEY_SESSION_TOKEN),
                                notNullValue());

                        return response;
                    }
                })
                //テスト対象のインターセプタを適用したアクション
                .addHandler(new Object() {
                    @UseToken
                    public HttpResponse doIndex(HttpRequest request, ExecutionContext context) {
                        return new HttpResponse(204);
                    }
                })
                .startLocal();

        HttpRequest request = new MockHttpRequest("GET /index HTTP/1.1");

        httpServer.handle(request, null);

        assertTrue("Assertions is not called.", assertionCalled.get());

        AssertionError e = thrownAssertionError.get();
        if (e != null) {
            throw e;
        }
    }
}
