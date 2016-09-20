package nablarch.fw.web.post;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.HashMap;

import nablarch.core.ThreadContext;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpErrorResponse;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpRequestHandler;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.MockHttpRequest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * {@link PostResubmitPreventHandler}のテスト。
 * @author Kiyohito Itoh
 */
public class PostResubmitPreventHandlerTest {

    final PostResubmitPreventHandler sut = new PostResubmitPreventHandler();

    private final ExecutionContext context = new ExecutionContext();

    private final TestAction testAction = new TestAction();


    @Before
    public void setUp() throws Exception {
        sut.setForwardPathMapping(new HashMap<String, String>() {{
            put("R", "/error1.jsp");
            put("R1", "/error2.jsp");
            put("WR", "/error3.jsp");
            put("WR1", "/error4.jsp");
            put("WR12", "/error5.jsp");
        }});
        context.addHandler(sut);
        context.addHandler(testAction);
        ThreadContext.clear();
    }

    @After
    public void tearDown() throws Exception {
        ThreadContext.clear();
    }

    private static class TestAction implements HttpRequestHandler {
        boolean isInvoked = false;
        @Override
        public HttpResponse handle(HttpRequest request, ExecutionContext context) {
            isInvoked = true;
            return new HttpResponse(200);
        }
    }

    /**
     * 通常のPOSTリクエストの場合、PRG処理は実行されずに後続のアクションの処理が実行されること。
     */
    @Test
    public void testNormalPostRequest() {

        // prepare
        HttpRequest request = createRequest();

        // execute
        HttpResponse response = context.handleNext(request);

        // assert
        assertThat(response.getStatusCode(), is(200));
        assertThat(testAction.isInvoked, is(true));
        assertThat(context.getSessionScopeMap().isEmpty(), is(true));
    }

    /**
     * 通常のGETリクエストの場合、PRG処理は実行されずに後続のアクションの処理が実行されること。
     */
    @Test
    public void testNormalGetRequest() throws Exception {
        final MockHttpRequest request = new MockHttpRequest();
        request.setMethod("GET");
        final HttpResponse response = context.handleNext(request);

        assertThat(response.getStatusCode(), is(200));
        assertThat(testAction.isInvoked, is(true));
    }

    /**
     * {@link PostResubmitPreventHandler#handle(HttpRequest, ExecutionContext)}のテスト。
     * <p/>
     * POSTの再送信を抑制するリクエストの場合。<br/>
     * ・POSTメソッド
     * ・nablarch_post_resubmit_preventパラメータが存在する。
     * <p/>
     * ・リクエスト情報がセッションスコープに格納されること。
     * ・実行時IDがパラメータに設定され、リダイレクトされること。
     * ・後続ハンドラが呼ばれないこと。
     */
    @Test
    public void testPostRedirectRequest() {

        // prepare
        String executionId = "20140221-1402";
        ThreadContext.setExecutionId(executionId);
        HttpRequest request = createRequest()
                                .setParam("nablarch_post_resubmit_prevent", "true");
        // execute
        HttpResponse response = context.handleNext(request);

        // assert
        PostRequest postRequest = context.getSessionScopedVar("nablarch_post_request_" + executionId);
        assertThat(postRequest, is(notNullValue()));

        assertThat(postRequest.getParamMap(), is(notNullValue()));
        assertThat(postRequest.getParamMap().size(), is(4));
        assertThat(postRequest.getParamMap().get("fruits")[0], is("APPLE"));
        assertThat(postRequest.getParamMap().get("fruits")[1], is("ORANGE"));
        assertThat(postRequest.getParamMap().get("animal")[0], is("DOG"));
        assertThat(postRequest.getParamMap().get("vegetables")[0], is("CARROT"));
        assertThat(postRequest.getParamMap().get("nablarch_post_resubmit_prevent")[0], is("true"));

        assertThat(response.getStatusCode(), is(302));
        assertThat(response.getContentPath().isRedirect(), is(true));
        assertThat(response.getContentPath().getPath(), is("/testapp/TestAction/R12345678?nablarch_post_redirect_id=20140221-1402"));
        assertThat(testAction.isInvoked, is(false));
    }

    /**
     * {@link PostResubmitPreventHandler#handle(HttpRequest, ExecutionContext)}のテスト。
     * <p/>
     * POST後のリダイレクトリクエストの場合。<br/>
     * ・GETメソッド
     * ・nablarch_post_redirect_idパラメータが存在する。
     * <p/>
     * ・セッションスコープに格納されたリクエスト情報がリクエストに設定されること。
     * ・セッションスコープからリクエスト情報が削除されること。
     * ・後続ハンドラが呼ばれること。
     */
    @Test
    public void testRedirectRequestGet() {

        // prepare
        String executionId = "20140221-1402";

        MockHttpRequest request = new MockHttpRequest();
        request.setMethod("GET");
        request.setRequestUri("/testapp/TestAction/R12345678");
        request.setHttpVersion("HTTP/1.1");
        request.setHost("www.example.com");
        request.getHeaderMap().put("Content-Type", "text/plain");
        request.setParam("nablarch_post_redirect_id", executionId);

        HttpRequest prevReq = new MockHttpRequest()
                .setParam("param1", "111")
                .setParam("file1", "file111");
        context.setSessionScopedVar("nablarch_post_request_" + executionId, new PostRequest(prevReq));

        // execute
        HttpResponse response = context.handleNext(request);

        // assert
        assertThat(response.getStatusCode(), is(200));
        assertThat(testAction.isInvoked, is(true));

        PostRequest postRequest = context.getSessionScopedVar("nablarch_post_request_" + executionId);
        assertThat(postRequest, is(nullValue()));

        assertThat(request.getParamMap(), is(notNullValue()));
        assertThat(request.getParamMap().size(), is(2));
        assertThat(request.getParamMap().get("param1")[0], is("111"));
        assertThat(request.getParamMap().get("file1")[0], is("file111"));

    }

    /**
     * {@link PostResubmitPreventHandler#handle(HttpRequest, ExecutionContext)}のテスト。
     * <p/>
     * POST後のリダイレクトリクエストの場合。<br/>
     * ・POSTメソッド
     * ・nablarch_post_redirect_idパラメータが存在する。
     * <p/>
     * ・セッションスコープに格納されたリクエスト情報がリクエストに設定されること。
     * ・セッションスコープからリクエスト情報が削除されること。
     * ・後続ハンドラが呼ばれること。
     */
    @Test
    public void testRedirectRequestPost() {

        // prepare
        String executionId = "20140221-1402";

        MockHttpRequest request = new MockHttpRequest();
        request.setMethod("POST");
        request.setRequestUri("/testapp/TestAction/R12345678");
        request.setHttpVersion("HTTP/1.1");
        request.setHost("www.example.com");
        request.getHeaderMap().put("Content-Type", "text/plain");
        request.setParam("nablarch_post_redirect_id", executionId);

        HttpRequest prevReq = new MockHttpRequest()
                .setParam("param1", "111")
                .setParam("file1", "file111");
        context.setSessionScopedVar("nablarch_post_request_" + executionId, new PostRequest(prevReq));

        // execute
        HttpResponse response = context.handleNext(request);

        // assert
        assertThat(response.getStatusCode(), is(200));
        assertThat(testAction.isInvoked, is(true));

        PostRequest postRequest = context.getSessionScopedVar("nablarch_post_request_" + executionId);
        assertThat(postRequest, is(nullValue()));

        assertThat(request.getParamMap(), is(notNullValue()));
        assertThat(request.getParamMap().size(), is(2));
        assertThat(request.getParamMap().get("param1")[0], is("111"));
        assertThat(request.getParamMap().get("file1")[0], is("file111"));

    }

    /**
     * 2回目のGETリクエストの場合(セッション上にリクエストパラメータが保持されていない場合)は、設定したパスに遷移すること。
     *
     * ※ {@link HttpErrorResponse}が送出され、遷移先のパスがリクエストIDにマッチするものになっていること
     */
    @Test
    public void testRedirectOfSecondTime() throws Exception {

        ThreadContext.setRequestId("WR12345678");

        final MockHttpRequest mockRequest = new MockHttpRequest();
        mockRequest.setMethod("GET");
        mockRequest.setParam("nablarch_post_redirect_id", "testRedirectRequestToDouble");

        try {
            context.handleNext(mockRequest);
            fail("ここは通過しない");
        } catch (HttpErrorResponse e) {
            assertThat("BadRequestである", e.getResponse().getStatusCode(), is(400));
            assertThat("もっともマッチしたものが長いJSPが遷移先になる", e.getResponse().getContentPath().getPath(), is("/error5.jsp"));
            assertThat("アクションに処理は到達しない", testAction.isInvoked, is(false));
        }

        // -------------------------------------------------- リクエストIDを変更
        ThreadContext.setRequestId("WR1345678");
        context.clearHandlers().addHandler(sut).addHandler(testAction);
        context.getSessionScopeMap().clear();
        mockRequest.setParam("nablarch_post_redirect_id", "testRedirectRequestToDouble");
        try {
            context.handleNext(mockRequest);
            fail("ここは通過しない");
        } catch (HttpErrorResponse e) {
            assertThat("BadRequestである", e.getResponse().getStatusCode(), is(400));
            assertThat("もっともマッチしたものが長いJSPが遷移先になる", e.getResponse().getContentPath().getPath(), is("/error4.jsp"));
            assertThat("アクションに処理は到達しない", testAction.isInvoked, is(false));
        }
    }

    /**
     * 2回目のGETリクエストの場合で(セッション上にリクエストパラメータが保持されていない場合)、遷移先のパスが存在しない場合は例外が送出されること。
     */
    @Test
    public void testRedirectOfSecondTime_notFoundForwardPath() throws Exception {
        ThreadContext.setRequestId("users");

        final MockHttpRequest mockRequest = new MockHttpRequest();
        mockRequest.setMethod("GET");
        mockRequest.setParam("nablarch_post_redirect_id", "testRedirectRequestToDouble");

        try {
            context.handleNext(mockRequest);
            fail("ここは通過しない");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("forward path not found in path mapping. request id=[users]"));
        }
    }

    /**
     * クエリーパラメータに対応するセッション情報が存在しない場合、BadRequestが返却されること。
     */
    @Test
    public void testInvalidRequest_InvalidQueryParam() {

        MockHttpRequest request = new MockHttpRequest();
        request.setMethod("GET").setParam("nablarch_post_redirect_id", "dummy");

        ThreadContext.setRequestId("R_____");
        try {
            context.handleNext(request);
            fail("ここは通過しない");
        } catch (HttpErrorResponse e) {
            assertThat(e.getResponse().getStatusCode(), is(400));
            assertThat(e.getResponse().getContentPath().getPath(), is("/error1.jsp"));
            assertThat(testAction.isInvoked, is(false));
        }
    }

    /**
     * クエリーパラメータが空文字列の場合、BadRequestが返却されること。
     */
    @Test
    public void testInvalidRequest_EmptyQueryParam() throws Exception {

        ThreadContext.setRequestId("R1_____");
        MockHttpRequest request = new MockHttpRequest();
        request.setMethod("GET").setParam("nablarch_post_redirect_id", "");

        try {
            context.handleNext(request);
            fail("ここは通過しない");
        } catch (HttpErrorResponse e) {
            assertThat(e.getResponse().getStatusCode(), is(400));
            assertThat(e.getResponse().getContentPath().getPath(), is("/error2.jsp"));
            assertThat(testAction.isInvoked, is(false));
        }

    }

    private HttpRequest createRequest() {

        final MockHttpRequest request = new MockHttpRequest();
        request.setMethod("POST");
        request.setRequestUri("/testapp/TestAction/R12345678");
        request.setHttpVersion("HTTP/1.1");
        request.setHost("www.example.com");
        request.getHeaderMap().put("Content-Type", "text/plain");
        request.setParam("fruits", "APPLE", "ORANGE");
        request.setParam("animal", "DOG");
        request.setParam("vegetables", "CARROT");
        return request;
    }
}
