package nablarch.fw.web.handler;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import nablarch.fw.Handler;
import nablarch.fw.Result.Error;
import nablarch.fw.web.HttpErrorResponse;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.servlet.MockServletRequest;
import nablarch.fw.web.servlet.ServletExecutionContext;

import nablarch.test.support.log.app.OnMemoryLogWriter;
import org.junit.Test;

public class HttpErrorHandlerTest {
    
    private HttpServletRequest createServletRequest(String uri) {
        MockServletRequest request = new MockServletRequest();
        request.setRequestURI(uri);
        request.setContextPath("");
        request.setMethod("POST");
        return request;
    }
    
    private ServletExecutionContext createExecutionContext(HttpServletRequest servletReq) {
        ServletExecutionContext ctx = new ServletExecutionContext(servletReq, null, null);
        ctx.setHandlerQueue(Arrays.asList(new Handler[] {
            new HttpRequestJavaPackageMapping("/", "nablarch.fw.web.handler.test")
        }));
        return ctx;
    }
    
    /**
     * 後続のハンドラで例外が発生しなかった場合は、
     * 後続のハンドラが返したレスポンスがそのまま返されること。
     */
    @Test
    public void testNormal() {
        
        // レスポンスのコンテンツパスが指定された場合
        
        HttpServletRequest servletReq = createServletRequest("/NormalHandler/index.html");
        ServletExecutionContext context = createExecutionContext(servletReq);
        HttpRequest request = context.getHttpRequest();
       
        HttpErrorHandler handler = new HttpErrorHandler();
        
        request.setParam("code", "200");
        request.setParam("path", "/success.jsp");
        
        OnMemoryLogWriter.clear();
        HttpResponse response = handler.handle(request, context);

        assertThat("正常レスポンスなので障害ログは出力されないこと",
                OnMemoryLogWriter.getMessages("writer.appLog").size(), is(0));
        
        assertThat(response.getStatusCode(), is(200));
        assertThat(response.getContentPath().getPath(), is("/success.jsp"));
        
        // レスポンスのコンテンツパスが指定されなかった場合
        
        servletReq = createServletRequest("/NormalHandler/index.html");
        context = createExecutionContext(servletReq);
        request = context.getHttpRequest();
        handler = new HttpErrorHandler();
        
        request.setParam("code", "400");
        
        response = handler.handle(request, context);

        assertThat("400系のエラーなので障害ログは出力されない。",
                OnMemoryLogWriter.getMessages("writer.appLog").size(), is(0));

        assertThat(response.getStatusCode(), is(400));
        assertThat(response.getContentPath(), is(nullValue()));
    }
    
    /**
     * 後続のハンドラで{@link nablarch.fw.NoMoreHandlerException}が発生した場合は、
     * 404エラーがレスポンスされること。
     */
    @Test
    public void testNoMoreHandlerException() {
        
        // デフォルトページを設定しない場合
        
        HttpServletRequest servletReq = createServletRequest("/UnknownHandler/index.html");
        ServletExecutionContext context = new ServletExecutionContext(servletReq, null, null);
        HttpRequest request = context.getHttpRequest();
        HttpErrorHandler handler = new HttpErrorHandler();
        OnMemoryLogWriter.clear();
        
        HttpResponse response = handler.handle(request, context);
        
        assertThat(OnMemoryLogWriter.getMessages("writer.appLog").toString(), containsString("There were no Handlers in handlerQueue. uri = [/UnknownHandler/index.html]"));
        assertThat(response.getStatusCode(), is(404));
        assertThat(response.getContentPath(), is(nullValue()));
        
        // デフォルトページを設定した場合
        
        servletReq = createServletRequest("/UnknownHandler/index.html");
        context = new ServletExecutionContext(servletReq, null, null);
        request = context.getHttpRequest();
        handler = new HttpErrorHandler().setDefaultPage("404", "/test_404.jsp");
        OnMemoryLogWriter.clear();
        
        response = handler.handle(request, context);
        
        assertThat(OnMemoryLogWriter.getMessages("writer.appLog").toString(),
                   containsString("There were no Handlers in handlerQueue. uri = [/UnknownHandler/index.html]"));
        assertThat(response.getStatusCode(), is(404));
        assertThat(response.getContentPath().getPath(), is("/test_404.jsp"));
    }
    
    /**
     * 後続のハンドラで{@link Error}が発生した場合は、
     * {@link Error}に応じたレスポンスされること。
     */
    @Test
    public void testResultError() {
        
        HttpServletRequest servletReq = createServletRequest("/ResultErrorHandler/index.html");
        ServletExecutionContext context = createExecutionContext(servletReq);
        HttpRequest request = context.getHttpRequest();
        HttpErrorHandler handler = new HttpErrorHandler();
        
        OnMemoryLogWriter.clear();
        HttpResponse response = handler.handle(request, context);

        assertThat(response.getStatusCode(), is(500));
        assertThat(response.getContentPath(), is(nullValue()));
        
        // 期待するメッセージがログ出力せれていることをアサート
        List<String> messages = OnMemoryLogWriter.getMessages("writer.appLog");
        assertThat("出力される障害ログは1つであること", messages.size(), is(1));
        assertThat("FATALログが出力されていること", messages.get(0), containsString("FATAL"));
        assertThat("InternalErrorが出力されていること", messages.get(0), containsString("[500 InternalError]"));
        assertThat("デフォルトのログメッセージが出力されていること", messages.get(0), containsString("The request could not be processed due to a unexpected condition. please contact our support team if you need."));
    }

    /**
     * 後続のハンドラで{@link nablarch.fw.Result.Error}が発生した場合のテスト。
     * デフォルト設定での障害通知ログ出力確認を行う。
     * 
     * デフォルト設定では、500~599のステータスコードの場合に障害通知が出力される。
     * ただし、503については障害通知ログは出力されない。
     */
    @Test
    public void testWriterFailureLogFromDefaultSetting() throws NoSuchFieldException, IllegalAccessException {

        HttpErrorHandler handler = new HttpErrorHandler();
        handler.setDefaultPages(new HashMap<String, String>() {
            {
                put("1..", "/jsp/systemError.jsp");
                put("2..", "/jsp/systemError.jsp");
                put("3..", "/jsp/systemError.jsp");
                put("4..", "/jsp/systemError.jsp");
                put("5..", "/jsp/systemError.jsp");
                put("6..", "/jsp/systemError.jsp");
                put("7..", "/jsp/systemError.jsp");
                put("8..", "/jsp/systemError.jsp");
                put("9..", "/jsp/systemError.jsp");
                put("503", "/jsp/userError.jsp");
            }
        });

        Field codeField = HttpResponse.Status.class.getDeclaredField("code");
        codeField.setAccessible(true);
        for (HttpResponse.Status status : HttpResponse.Status.values()) {
            int statusCode = (Integer) codeField.get(status);
            System.out.println("################################################## " + statusCode + " ##################################################");
            HttpServletRequest servletReq = createServletRequest("/StatusTestHandler/index.html");
            ServletExecutionContext context = createExecutionContext(servletReq);
            HttpRequest request = context.getHttpRequest();
            request.getParamMap().put("statusCode", new String[]{String.valueOf(statusCode)});

            OnMemoryLogWriter.clear();
            HttpResponse response = handler.handle(request, context);

            assertThat(response.getStatusCode(), is(statusCode));
            if (statusCode == 503) {
                assertThat(response.getContentPath().getPath(), is("/jsp/userError.jsp"));
            } else {
                assertThat(response.getContentPath().getPath(), is("/jsp/systemError.jsp"));
            }

            List<String> messages = OnMemoryLogWriter.getMessages("writer.appLog");
            if (statusCode >= 500 && statusCode <= 599 && statusCode != 503) {
                // ステータスコードが503以外の場合は、障害通知ログが出力されていること。
                assertThat("出力される障害ログは1つであること", messages.size(), is(1));
                assertThat("FATALログが出力されていること", messages.get(0), containsString("FATAL"));
                assertThat("Errorが出力されていること", messages.get(0), containsString("Error"));
            } else {
                assertThat("障害通知ログは出力されないこと", messages.size(), is(0));
            }
        }
    }

    /**
     * 後続のハンドラで{@link nablarch.fw.Result.Error}が発生した場合のテスト。
     * 任意の設定を行った場合のログ出力テスト。
     *
     * ここでは、障害通知ログの出力対象のステータスコードを200と503としているため、
     * その2つのステータスコードのみログ出力されることを確認する。
     */
    @Test
    public void testWriterFailureLogFromOtherSetting() throws NoSuchFieldException, IllegalAccessException {

        HttpErrorHandler handler = new HttpErrorHandler();
        handler.setWriteFailureLogPattern("200|503");
        handler.setDefaultPages(new HashMap<String, String>() {
            {
                put("1..", "/jsp/systemError.jsp");
                put("2..", "/jsp/systemError.jsp");
                put("3..", "/jsp/systemError.jsp");
                put("4..", "/jsp/systemError.jsp");
                put("5..", "/jsp/systemError.jsp");
                put("6..", "/jsp/systemError.jsp");
                put("7..", "/jsp/systemError.jsp");
                put("8..", "/jsp/systemError.jsp");
                put("9..", "/jsp/systemError.jsp");
                put("503", "/jsp/userError.jsp");
            }
        });

        Field codeField = HttpResponse.Status.class.getDeclaredField("code");
        codeField.setAccessible(true);
        for (HttpResponse.Status status : HttpResponse.Status.values()) {
            int statusCode = (Integer) codeField.get(status);
            System.out.println("################################################## " + statusCode + " ##################################################");
            HttpServletRequest servletReq = createServletRequest("/StatusTestHandler/index.html");
            ServletExecutionContext context = createExecutionContext(servletReq);
            HttpRequest request = context.getHttpRequest();
            request.getParamMap().put("statusCode", new String[]{String.valueOf(statusCode)});

            OnMemoryLogWriter.clear();
            HttpResponse response = handler.handle(request, context);

            assertThat(response.getStatusCode(), is(statusCode));
            if (statusCode == 503) {
                assertThat(response.getContentPath().getPath(), is("/jsp/userError.jsp"));
            } else {
                assertThat(response.getContentPath().getPath(), is("/jsp/systemError.jsp"));
            }

            List<String> messages = OnMemoryLogWriter.getMessages("writer.appLog");
            if (statusCode == 200 || statusCode == 503) {
                // ステータスコードが503以外の場合は、障害通知ログが出力されていること。
                assertThat("出力される障害ログは1つであること", messages.size(), is(1));
                assertThat("FATALログが出力されていること", messages.get(0), containsString("FATAL"));
                assertThat("Errorが出力されていること", messages.get(0), containsString("Error"));
            } else {
                assertThat("障害通知ログは出力されないこと", messages.size(), is(0));
            }
        }
    }

    /**
     * 後続のハンドラで{@link HttpErrorResponse}が発生した場合は、
     * {@link HttpErrorResponse}に応じたレスポンスされること。
     */
    @Test
    public void testHttpErrorResponse() {
        
        HttpServletRequest servletReq = createServletRequest("/HttpErrorResponseHandler/index.html");
        ServletExecutionContext context = createExecutionContext(servletReq);
        HttpRequest request = context.getHttpRequest();
        HttpErrorHandler handler = new HttpErrorHandler();

        OnMemoryLogWriter.clear();
        HttpResponse response = handler.handle(request, context);
        assertThat("アプリケーションで明示的にエラーレスポンスを設定した場合は、ログ出力されないこと",
                OnMemoryLogWriter.getMessages("writer.appLog").size(), is(0));

        assertThat(response.getStatusCode(), is(400));
        assertThat(response.getContentPath().getPath(), is("/test_400.jsp"));
    }
    
    /**
     * 後続のハンドラで{@link RuntimeException}が発生した場合は、
     * 500エラーがレスポンスされること。
     */
    @Test
    public void testRuntimeExceptionException() {
        
        HttpServletRequest servletReq = createServletRequest("/RuntimeExceptionHandler/index.html");
        ServletExecutionContext context = createExecutionContext(servletReq);
        HttpRequest request = context.getHttpRequest();
        HttpErrorHandler handler = new HttpErrorHandler();
        OnMemoryLogWriter.clear();
        
        HttpResponse response = handler.handle(request, context);

        assertThat(OnMemoryLogWriter.getMessages("writer.appLog").toString(),
                   containsString("FATAL stdout fail_code = [MSG99999]"));
        assertThat(response.getStatusCode(), is(500));
        assertThat(response.getContentPath(), is(nullValue()));
    }
    
    /**
     * 後続のハンドラで{@link StackOverflowError}が発生した場合は、
     * 500エラーがレスポンスされること。
     */
    @Test
    public void testStackOverflowError() {
        
        HttpServletRequest servletReq = createServletRequest("/StackOverflowErrorHandler/index.html");
        ServletExecutionContext context = createExecutionContext(servletReq);
        HttpRequest request = context.getHttpRequest();
        HttpErrorHandler handler = new HttpErrorHandler();
        OnMemoryLogWriter.clear();
        
        HttpResponse response = handler.handle(request, context);

        assertThat(OnMemoryLogWriter.getMessages("writer.appLog").toString(),
                   containsString("FATAL stdout fail_code = [MSG99999]"));
        assertThat(response.getStatusCode(), is(500));
        assertThat(response.getContentPath(), is(nullValue()));
    }
    
    /**
     * 後続のハンドラで{@link VirtualMachineError}が発生した場合は、
     * 再スローする。
     */
    @Test
    public void testVirtualMachineError() {
        
        HttpServletRequest servletReq = createServletRequest("/VirtualMachineErrorHandler/index.html");
        ServletExecutionContext context = createExecutionContext(servletReq);
        HttpRequest request = context.getHttpRequest();
        HttpErrorHandler handler = new HttpErrorHandler();
        
        try {
            handler.handle(request, context);
            fail("後続のハンドラでVirtualMachineErrorが発生した場合");
        } catch (VirtualMachineError e) {
            // success
        }
    }
    
    /**
     * 後続のハンドラで{@link ThreadDeath}が発生した場合は、
     * 再スローする。
     */
    @Test
    public void testThreadDeathError() {
        
        HttpServletRequest servletReq = createServletRequest("/ThreadDeathHandler/index.html");
        ServletExecutionContext context = createExecutionContext(servletReq);
        HttpRequest request = context.getHttpRequest();
        HttpErrorHandler handler = new HttpErrorHandler();
        
        try {
            handler.handle(request, context);
            fail("後続のハンドラでThreadDeathが発生した場合");
        } catch (ThreadDeath e) {
            // success
        }
    }
    
    /**
     * 後続のハンドラで{@link java.lang.Error}が発生した場合は、
     * 500エラーがレスポンスされること。
     */
    @Test
    public void testError() {
        
        HttpServletRequest servletReq = createServletRequest("/ErrorHandler/index.html");
        ServletExecutionContext context = createExecutionContext(servletReq);
        HttpRequest request = context.getHttpRequest();
        HttpErrorHandler handler = new HttpErrorHandler();
        OnMemoryLogWriter.clear();
        
        HttpResponse response = handler.handle(request, context);

        assertThat(OnMemoryLogWriter.getMessages("writer.appLog").toString(),
                   containsString("FATAL stdout fail_code = [MSG99999]"));
        assertThat(response.getStatusCode(), is(500));
        assertThat(response.getContentPath(), is(nullValue()));
    }
    
    /**
     * リポジトリの設定で指定されたデフォルトページが使用されること。
     */
    @Test
    public void testDefaultPagesSetting() {
        
        SystemRepository.clear();
        String path = "classpath:nablarch/fw/web/handler/http-error-handler-test.xml";
        SystemRepository.load(new DiContainer(new XmlComponentDefinitionLoader(path)));
        
        // 200
        
        HttpServletRequest servletReq = createServletRequest("/NormalHandler/index.html");
        ServletExecutionContext context = createExecutionContext(servletReq);
        HttpRequest request = context.getHttpRequest();
        HttpErrorHandler handler = SystemRepository.get("errorHandler");
        
        request.setParam("code", "200");
        
        HttpResponse response = handler.handle(request, context);
        
        assertThat(response.getStatusCode(), is(200));
        assertNull(response.getContentPath());
        
        // 404
        
        servletReq = createServletRequest("/NormalHandler/index.html");
        context = createExecutionContext(servletReq);
        request = context.getHttpRequest();
        
        request.setParam("code", "404");
        
        response = handler.handle(request, context);
        
        assertThat(response.getStatusCode(), is(404));
        assertThat(response.getContentPath().getPath(), is("/PAGE_NOT_FOUND_ERROR.jsp"));
        
        // 403
        
        servletReq = createServletRequest("/NormalHandler/index.html");
        context = createExecutionContext(servletReq);
        request = context.getHttpRequest();
        
        request.setParam("code", "403");
        
        response = handler.handle(request, context);
        
        assertThat(response.getStatusCode(), is(403));
        assertThat(response.getContentPath().getPath(), is("/PERMISSION-ERROR.jsp"));
        
        
        // 400系汎用エラー(ワイルドカード指定)
        
        servletReq = createServletRequest("/NormalHandler/index.html");
        context = createExecutionContext(servletReq);
        request = context.getHttpRequest();
        
        request.setParam("code", "409");
        
        response = handler.handle(request, context);
        
        assertThat(response.getStatusCode(), is(409));
        assertThat(response.getContentPath().getPath(), is("/USER_ERROR.jsp"));
        
    }
}
