package nablarch.common.web.session.integration;

import mockit.Verifications;
import nablarch.core.util.StringUtil;
import nablarch.fw.ExecutionContext;
import nablarch.fw.Handler;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpRequestHandler;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.HttpServer;
import nablarch.fw.web.MockHttpRequest;
import org.junit.rules.ExternalResource;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * {@link HttpServer}を使用したテスト用のリソースクラス。
 * <p/>
 * 本クラスを使用したテストのサンプルソース
 * <pre>
 * public class SampleTest {
 *     @Rule
 *     public HttpServerResource httpServer = new HttpServerResource("classpath://web/bath/path");
 *     @Test
 *     public void test() {
 *         httpServer
 *             // ハンドラの追加。
 *             // HttpServerがデフォルトで設定するハンドラは
 *             // before実行時にクリアしているので、一から設定してください。
 *             .addHandler(new GlobalErrorHandler())
 *             .addHandler(new HttpCharacterEncodingHandler())
 *             .addHandler(new HttpResponseHandler())
 *             // テストハンドラの追加。
 *             // 後続のtestメソッドが呼ばれると、ここに追加したハンドラの順番でリクエストが呼ばれます。
 *             // このハンドラに、テスト対象のAPI呼び出しやアサートを実装してください。
 *             // TestHandlerはデフォルトで下記の実装を持っています。
 *             // 変更したい場合はオーバーライドしてください。
 *             // ・リクエスト時のURL：/
 *             // ・リクエスト時のメソッド：POST
 *             // ・リクエスト時のコンテンツタイプ：application/x-www-form-urlencoded
 *             // ・期待するレスポンスのステータスコード：200
 *             // 期待するレスポンスのステータスコードと、実際のレスポンスメッセージに含まれる
 *             // レスポンスコードが異なる場合はアサーションエラーとなります。
 *             .addTestHandler(new HttpServerResource.TestHandler() {
 *                 @Override
 *                 public HttpResponse handle(final HttpRequest request, final ExecutionContext context) {
 *                     // sut.doSomething() or assertion
 *                 }})
 *             .addTestHandler(･･･)
 *             .addTestHandler(･･･)
 *             // テストの実行。
 *             .test();
 * }
 * </pre>
 * 本クラスを使用する場合は、レスポンスメッセージをキャプチャするために、
 * テストクラスに下記のモックを定義してください。
 * <pre>
 * @Mocked("parse") HttpResponse unused;
 * </pre>
 */
public class HttpServerResource extends ExternalResource {

    private final String warBasePath;
    private HttpServer server;
    private List<TestHandler> testHandlers;

    public HttpServerResource(String warBasePath) {
        this.warBasePath = warBasePath;
    }

    @Override
    protected void before() throws Throwable {
        server = new HttpServer().setWarBasePath(warBasePath).clearHandlers();
        testHandlers = new ArrayList<TestHandler>();
    }

    @Override
    protected void after() {
        server = null;
    }

    public HttpServerResource addHandler(Handler<?, ?> handler) {
        server.addHandler(handler);
        return this;
    }

    public HttpServerResource addTestHandler(TestHandler testHandler) {
        testHandlers.add(testHandler);
        return this;
    }

    public void test() {

        final HttpRequestCreator httpRequestCreator = new HttpRequestCreator();
        final TestHandlerCaller testHandlerCaller = new TestHandlerCaller();

        server.addHandler(testHandlerCaller).startLocal();

        for (TestHandler testHandler : testHandlers) {

            testHandlerCaller.nextHandler = testHandler;

            MockHttpRequest request = httpRequestCreator.create(testHandler);
            System.out.println("*** Request Message ****************************************");
            System.out.println(request);
            System.out.println("************************************************************");

            HttpResponse response = server.handle(request, null);

            new Verifications() {{
                byte[] bytes;
                HttpResponse.parse(bytes = withCapture());
                httpRequestCreator.responseMessage = StringUtil.toString(bytes, Charset.forName("UTF-8"));
            }};

            System.out.println("*** Response Message ***************************************");
            System.out.println(httpRequestCreator.responseMessage);
            System.out.println("************************************************************");

            assertThat(getStatusCode(httpRequestCreator.responseMessage),
                    is(testHandler.getExpectedStatusCode()));
        }
    }

    private static int getStatusCode(String responseMessage) {
        final int begin = responseMessage.indexOf(' ') + 1;
        final String afterStatusCode = responseMessage.substring(begin);
        final int end = afterStatusCode.indexOf(' ');
        return Integer.valueOf(afterStatusCode.substring(0, end));
    }

    private static final class HttpRequestCreator {

        private static final String HTTP_CRLF = "\r\n";
        private String responseMessage = "";
        private String jsessionId = null;

        private MockHttpRequest create(TestHandler testHandler) {

            final MockHttpRequest request = new MockHttpRequest();
            request.setRequestUri(testHandler.getRequestUri());
            request.setMethod(testHandler.getMethod());
            request.getHeaderMap().put("Content-Type", testHandler.getContentType());

            final CookieHeader cookieHeader = new CookieHeader(responseMessage);

            if (jsessionId == null && cookieHeader.contains("JSESSIONID")) {
                // Set-Cookieが初回リクエスト時しか設定されないので、
                // 2回目以降のリクエストでHTTPセッションを維持するために保持。
                jsessionId = cookieHeader.get("JSESSIONID");
            } else if (jsessionId != null && !cookieHeader.contains("JSESSIONID")) {
                // 2回目以降のリクエストだと、レスポンスメッセージからJSESSIONIDを取得できないので
                // 保持しておいたJSESSIONIDを設定。
                cookieHeader.put("JSESSIONID", jsessionId);
            }

            final String cookieHeaderValues = cookieHeader.getCookieHeaderValues("JSESSIONID", "NABLARCH_SID");
            if (StringUtil.hasValue(cookieHeaderValues)) {
                request.getHeaderMap().put("Cookie", cookieHeaderValues);
            }

            final String hiddenStoreValue = getHtmlAttribute("nablarch_hiddenStore");
            if (StringUtil.hasValue(hiddenStoreValue)) {
                request.setParam("nablarch_hiddenStore", hiddenStoreValue);
            }

            return request;
        }

        private String getHtmlAttribute(String name) {
            for (String line : responseMessage.split(HTTP_CRLF)) {
                if (!line.contains("name=\"" + name + '"')) {
                    continue;
                }
                final int valueAttrBegin = line.indexOf("value=\"");
                final int valueBegin = valueAttrBegin + "value=\"".length();
                line = line.substring(valueBegin);
                final int valueEnd = line.indexOf('"');
                return line.substring(0, valueEnd);
            }
            return null;
        }
    }

    private static final class CookieHeader {

        private final Map<String, String> cookieValues;

        private CookieHeader(String responseMessage) {
            // get cookie name/value pairs from Set-Cookie header
            cookieValues = new HashMap<String, String>();
            for (String line : responseMessage.split(HttpRequestCreator.HTTP_CRLF)) {
                if (!line.contains("Set-Cookie:")) {
                    continue;
                }
                final int nameBegin = "Set-Cookie:".length();
                final int nameEnd = line.indexOf('=');
                final int valueBegin = nameEnd + 1;
                final int valueEnd = line.indexOf(';');
                cookieValues.put(line.substring(nameBegin, nameEnd).trim(),
                        line.substring(valueBegin, valueEnd).trim());
            }
        }

        private boolean contains(final String name) {
            return cookieValues.containsKey(name);
        }

        private String get(final String name) {
            return cookieValues.get(name);
        }

        private void put(final String name, final String value) {
            cookieValues.put(name, value);
        }

        private String getCookieHeaderValues(final String... names) {
            final StringBuilder sb = new StringBuilder();
            for (String name : names) {
                if (!cookieValues.containsKey(name)) {
                    continue;
                }
                if (sb.length() != 0) {
                    sb.append("; ");
                }
                sb.append(name).append('=').append(cookieValues.get(name));
            }
            return sb.toString();
        }
    }

    private static final class TestHandlerCaller implements HttpRequestHandler {
        private HttpRequestHandler nextHandler;
        @Override
        public HttpResponse handle(HttpRequest request, ExecutionContext context) {
            return nextHandler.handle(request, context);
        }
    }

    public abstract static class TestHandler implements HttpRequestHandler {
        protected String getRequestUri() {
            return "/";
        }
        protected String getMethod() {
            return "POST";
        }
        protected String getContentType() {
            return "application/x-www-form-urlencoded";
        }
        protected int getExpectedStatusCode() {
            return 200;
        }
    }
}
